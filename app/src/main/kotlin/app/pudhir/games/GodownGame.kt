package app.pudhir.games

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pudhir.core.Dir
import app.pudhir.core.Godown
import app.pudhir.ui.ActionChip
import app.pudhir.ui.ActionRow
import app.pudhir.ui.LocalPalette
import app.pudhir.ui.darken
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun GodownGame(s: PlaySession) {
    val p = LocalPalette.current
    val game = remember { Godown.generate(s.seed, s.difficulty) }
    var version by remember { mutableIntStateOf(0) }
    val crates = remember { mutableStateListOf(*game.boxes.toTypedArray()) } // stable order for animation
    var hints by remember { mutableIntStateOf(0) }
    var hintDir by remember { mutableStateOf<Dir?>(null) }
    var walking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun sync() {
        // Keep each crate's identity: replace only the one that moved.
        val now = game.boxes.toMutableSet()
        val gone = crates.indices.filter { crates[it] !in now }
        val added = (now - crates.toSet()).toMutableList()
        for (i in gone) if (added.isNotEmpty()) crates[i] = added.removeAt(0)
        version++
    }

    fun step(d: Dir) {
        val r = game.move(d)
        if (r == 0) { s.haptic(true); return }
        hintDir = null
        s.haptic(r == 2)
        sync()
        if (game.isSolved()) s.onSolved(hints)
    }

    /** Shortest walk (without pushing) to [target]. */
    fun walkTo(target: Int): List<Dir>? {
        val prev = HashMap<Int, Pair<Int, Dir>>()
        val q = ArrayDeque(listOf(game.player)); val seen = hashSetOf(game.player)
        while (q.isNotEmpty()) {
            val cur = q.removeFirst()
            if (cur == target) {
                val out = ArrayList<Dir>(); var c = cur
                while (c != game.player) { val (pc, d) = prev[c]!!; out += d; c = pc }
                return out.reversed()
            }
            for (d in Dir.entries) {
                val r = cur / game.cols + d.dr; val c = cur % game.cols + d.dc
                if (r !in 0 until game.rows || c !in 0 until game.cols) continue
                val nx = r * game.cols + c
                if (game.walls[nx] || nx in game.boxes || !seen.add(nx)) continue
                prev[nx] = cur to d; q.addLast(nx)
            }
        }
        return null
    }

    Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        version.let { Text("${game.boxes.count { game.goals[it] }} of ${game.boxes.size} crates in place · ${game.history.size} steps", color = p.muted) }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            val cell = minOf(maxWidth / game.cols, maxHeight / game.rows)
            Box(
                Modifier.size(cell * game.cols, cell * game.rows)
                    .pointerInput(Unit) {
                        var total = Offset.Zero
                        detectDragGestures(
                            onDragStart = { total = Offset.Zero },
                            onDrag = { ch, d -> total += d; ch.consume() },
                            onDragEnd = {
                                if (!walking && maxOf(abs(total.x), abs(total.y)) > 30f)
                                    step(if (abs(total.x) > abs(total.y)) (if (total.x > 0) Dir.Right else Dir.Left) else (if (total.y > 0) Dir.Down else Dir.Up))
                            },
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { o ->
                            if (walking) return@detectTapGestures
                            val cs = size.width / game.cols.toFloat()
                            val pos = (o.y / cs).toInt().coerceIn(0, game.rows - 1) * game.cols + (o.x / cs).toInt().coerceIn(0, game.cols - 1)
                            val pr = game.player / game.cols; val pc = game.player % game.cols
                            val dr = pos / game.cols - pr; val dc = pos % game.cols - pc
                            if (abs(dr) + abs(dc) == 1) { step(Dir.entries.first { it.dr == dr && it.dc == dc }); return@detectTapGestures }
                            val path = walkTo(pos) ?: return@detectTapGestures
                            walking = true
                            scope.launch { for (d in path) { step(d); delay(90) }; walking = false }
                        }
                    },
            ) {
                for (q in 0 until game.rows * game.cols) {
                    val x = cell * (q % game.cols); val y = cell * (q / game.cols)
                    if (game.walls[q]) {
                        Box(Modifier.offset(x, y).size(cell).padding(1.dp).clip(RoundedCornerShape(8.dp)).background(if (p.dark) Color(0xFF4A4250) else Color(0xFF8A7F74)))
                    } else {
                        Box(Modifier.offset(x, y).size(cell).background(if (p.dark) Color(0xFF26232A) else Color(0xFFF3EDE5)))
                        if (game.goals[q]) Box(Modifier.offset(x, y).size(cell).padding(cell * 0.3f).clip(CircleShape).border(3.dp, s.accent, CircleShape))
                    }
                }
                version.let {
                    for ((k, c) in crates.withIndex()) key(k) {
                        val cx by animateDpAsState(cell * (c % game.cols), spring(stiffness = 900f), label = "cx")
                        val cy by animateDpAsState(cell * (c / game.cols), spring(stiffness = 900f), label = "cy")
                        val home = game.goals[c]
                        Box(
                            Modifier.offset(cx, cy).size(cell).padding(cell * 0.08f).shadow(3.dp, RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp))
                                .background(if (home) Color(0xFF2BA36B) else Color(0xFFC98B4E))
                                .border(2.dp, darken(if (home) Color(0xFF2BA36B) else Color(0xFFC98B4E), 0.25f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center,
                        ) { Text(if (home) "✓" else "📦", fontSize = (cell.value * 0.42f).sp, color = Color.White) }
                    }
                    val px by animateDpAsState(cell * (game.player % game.cols), spring(stiffness = 900f), label = "px")
                    val py by animateDpAsState(cell * (game.player / game.cols), spring(stiffness = 900f), label = "py")
                    Box(Modifier.offset(px, py).size(cell), contentAlignment = Alignment.Center) {
                        Text("👷", fontSize = (cell.value * 0.6f).sp)
                        hintDir?.let { d ->
                            Text(
                                when (d) { Dir.Up -> "⬆️"; Dir.Down -> "⬇️"; Dir.Left -> "⬅️"; Dir.Right -> "➡️" },
                                fontSize = (cell.value * 0.4f).sp,
                                modifier = Modifier.offset(cell * 0.6f * d.dc, cell * 0.6f * d.dr),
                            )
                        }
                    }
                }
            }
        }
        Text("Swipe to step, or tap a square to walk there", color = p.muted, fontSize = 13.sp)
        ActionRow {
            ActionChip(Icons.AutoMirrored.Rounded.Undo, "Undo", !walking) { if (game.undo()) sync() }
            ActionChip(Icons.Rounded.RestartAlt, "Restart", !walking) { game.restart(); crates.clear(); crates.addAll(game.boxes); version++ }
            ActionChip(Icons.Rounded.Lightbulb, "Hint", !walking) { s.useHint { hintDir = game.hint(); hints++; crates.clear(); crates.addAll(game.boxes); version++ } }
        }
    }
}
