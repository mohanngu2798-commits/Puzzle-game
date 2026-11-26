package app.pudhir.games

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pudhir.core.MithaiMatch
import app.pudhir.ui.ActionChip
import app.pudhir.ui.ActionRow
import app.pudhir.ui.LocalPalette
import app.pudhir.ui.lighten
import app.pudhir.ui.popIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

val Sweets = listOf("🍬", "🍩", "🍪", "🧁", "🍫", "🍭")

@Composable
fun MithaiMatchGame(s: PlaySession) {
    val p = LocalPalette.current
    val game = remember { MithaiMatch(s.seed, s.difficulty) }
    var version by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var selected by remember { mutableIntStateOf(-1) }
    var clearing by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var hint by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var hints by remember { mutableIntStateOf(0) }
    var chainText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val n = game.size

    fun trySwap(a: Int, b: Int) {
        if (busy || !game.adjacent(a, b)) return
        if (!game.swap(a, b)) { s.haptic(true); return }
        busy = true; selected = -1; hint = null
        version++
        scope.launch {
            delay(180)
            var chain = 0
            while (true) {
                val m = game.findMatches()
                if (m.isEmpty()) break
                chain++
                clearing = m; s.haptic(chain > 1); delay(220)
                game.step(); clearing = emptySet(); version++
                chainText = if (chain > 1) "Sweet chain ×$chain!" else ""
                delay(260)
            }
            if (game.hint() == null) { game.shuffle(); version++ }
            busy = false
            when {
                game.isWon() -> s.onSolved(hints)
                game.isLost() -> s.onGameOver(game.score, "Out of moves")
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        version.let {
            val frac by animateFloatAsState((game.score / game.target.toFloat()).coerceAtMost(1f), tween(500), label = "goal")
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${game.score} / ${game.target}", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = p.ink)
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(p.surfaceAlt)) {
                        Box(Modifier.fillMaxWidth(frac).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(s.accent))
                    }
                }
                Spacer(Modifier.size(16.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${game.movesLeft}", fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = s.accent)
                    Text("moves", color = p.muted, fontSize = 12.sp)
                }
            }
            Text(chainText, color = s.accent, fontWeight = FontWeight.Bold, modifier = Modifier.height(24.dp).padding(top = 4.dp))
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            val side = minOf(maxWidth, maxHeight)
            val cell = side / n
            Box(
                Modifier.size(side).shadow(6.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp))
                    .background(lighten(s.accent, if (p.dark) 0.05f else 0.88f))
                    .pointerInput(Unit) {
                        var start = -1; var total = Offset.Zero
                        detectDragGestures(
                            onDragStart = { o -> val cs = size.width / n.toFloat(); start = (o.y / cs).toInt().coerceIn(0, n - 1) * n + (o.x / cs).toInt().coerceIn(0, n - 1); total = Offset.Zero },
                            onDrag = { ch, d -> total += d; ch.consume() },
                            onDragEnd = {
                                if (start >= 0 && maxOf(abs(total.x), abs(total.y)) > 24f) {
                                    val r = start / n; val c = start % n
                                    val to = if (abs(total.x) > abs(total.y)) (if (total.x > 0 && c < n - 1) start + 1 else if (total.x < 0 && c > 0) start - 1 else -1)
                                    else (if (total.y > 0 && r < n - 1) start + n else if (total.y < 0 && r > 0) start - n else -1)
                                    if (to >= 0) trySwap(start, to)
                                }
                            },
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { o ->
                            val cs = size.width / n.toFloat()
                            val pos = (o.y / cs).toInt().coerceIn(0, n - 1) * n + (o.x / cs).toInt().coerceIn(0, n - 1)
                            if (selected >= 0 && game.adjacent(selected, pos)) trySwap(selected, pos) else selected = if (selected == pos) -1 else pos
                        }
                    },
            ) {
                version.let {
                    for (pos in 0 until n * n) {
                        val kind = game.board[pos]
                        if (kind < 0) continue
                        key(game.ids[pos]) {
                            val x by animateDpAsState(cell * (pos % n), spring(stiffness = 700f), label = "x")
                            val y by animateDpAsState(cell * (pos / n), spring(dampingRatio = 0.65f, stiffness = 420f), label = "y")
                            val going = pos in clearing
                            val gone by animateFloatAsState(if (going) 0f else 1f, tween(200), label = "gone")
                            val sel = pos == selected || hint?.let { it.first == pos || it.second == pos } == true
                            Box(
                                Modifier.offset(x, y).size(cell).padding(2.dp).popIn()
                                    .graphicsLayer { scaleX = gone * (if (sel) 1.1f else 1f); scaleY = scaleX; alpha = gone }
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (sel) p.surface else Color.Transparent)
                                    .border(if (sel) 2.dp else 0.dp, if (sel) s.accent else Color.Transparent, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center,
                            ) { Text(Sweets[kind % Sweets.size], fontSize = (cell.value * 0.6f).sp) }
                        }
                    }
                }
            }
        }
        ActionRow {
            ActionChip(Icons.Rounded.Shuffle, "Shuffle", !busy) { game.shuffle(); version++; s.haptic(true) }
            ActionChip(Icons.Rounded.Lightbulb, "Hint", !busy) { s.useHint { hint = game.hint(); hints++ } }
        }
    }
}
