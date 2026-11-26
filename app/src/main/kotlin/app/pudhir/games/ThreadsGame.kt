package app.pudhir.games

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import app.pudhir.core.Cell
import app.pudhir.core.ThreadsPuzzle
import app.pudhir.ui.ActionChip
import app.pudhir.ui.ActionRow
import app.pudhir.ui.LocalPalette
import app.pudhir.ui.PieceColours
import kotlin.math.abs

@Composable
fun ThreadsGame(s: PlaySession) {
    val p = LocalPalette.current
    val puzzle = remember { ThreadsPuzzle.generate(s.seed, s.difficulty) }
    val n = puzzle.size
    val paths: List<SnapshotStateList<Cell>> = remember { List(puzzle.colours) { mutableStateListOf() } }
    var active by remember { mutableIntStateOf(-1) }
    var hints by remember { mutableIntStateOf(0) }
    var solved by remember { mutableStateOf(false) }
    val pulse = remember { Animatable(1f) }

    fun endpointColour(c: Cell): Int = puzzle.endpoints.indexOfFirst { it.first == c || it.second == c }
    fun ownerOf(c: Cell): Int = paths.indexOfFirst { c in it }
    fun complete(k: Int): Boolean {
        val path = paths[k]
        if (path.size < 2) return false
        val (a, b) = puzzle.endpoints[k]
        return setOf(path.first(), path.last()) == setOf(a, b)
    }
    fun filled() = paths.sumOf { it.size }

    fun check() {
        if (paths.indices.all { complete(it) } && ThreadsPuzzle.isSolved(puzzle, paths.map { it.toList() })) solved = true
    }

    LaunchedEffect(solved) {
        if (solved) { pulse.snapTo(1.25f); pulse.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 300f)); s.onSolved(hints) }
    }

    fun cut(k: Int, at: Cell) {
        val path = paths[k]
        val i = path.indexOf(at)
        if (i >= 0) while (path.size > i) path.removeAt(path.lastIndex)
    }

    fun enter(c: Cell) {
        if (active < 0 || solved) return
        val path = paths[active]
        if (path.isEmpty()) return
        val last = path.last()
        if (c == last) return
        val idx = path.indexOf(c)
        if (idx >= 0) { while (path.size > idx + 1) path.removeAt(path.lastIndex); return }
        if (abs(c.r - last.r) + abs(c.c - last.c) != 1) return
        if (complete(active)) return
        val ep = endpointColour(c)
        if (ep >= 0 && ep != active) return
        val owner = ownerOf(c)
        if (owner >= 0 && owner != active) cut(owner, c)
        path.add(c)
        s.haptic(false)
        if (complete(active)) s.haptic(true)
    }

    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "${paths.indices.count { complete(it) }} of ${puzzle.colours} threads · ${filled() * 100 / (n * n)}% filled",
            color = p.muted, modifier = Modifier.padding(bottom = 8.dp),
        )
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            val side = minOf(maxWidth, maxHeight)
            Box(Modifier.size(side).shadow(6.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp)).background(p.surface)) {
                Canvas(
                    Modifier.fillMaxSize().padding(10.dp).pointerInput(Unit) {
                        fun cellAt(o: Offset): Cell? {
                            val cs = size.width / n.toFloat()
                            val r = (o.y / cs).toInt(); val c = (o.x / cs).toInt()
                            return if (r in 0 until n && c in 0 until n && o.x >= 0 && o.y >= 0) Cell(r, c) else null
                        }
                        detectDragGestures(
                            onDragStart = { o ->
                                val c = cellAt(o) ?: return@detectDragGestures
                                val ep = endpointColour(c)
                                if (ep >= 0) {
                                    active = ep
                                    paths[ep].clear(); paths[ep].add(c)
                                    s.haptic(false)
                                } else {
                                    val owner = ownerOf(c)
                                    if (owner >= 0) { active = owner; cut(owner, c); paths[owner].add(c) } else active = -1
                                }
                            },
                            onDrag = { change, _ -> cellAt(change.position)?.let { enter(it) } },
                            onDragEnd = { active = -1; check() },
                            onDragCancel = { active = -1 },
                        )
                    },
                ) {
                    val cs = size.width / n
                    // Grid.
                    for (i in 0..n) {
                        drawLine(p.line, Offset(i * cs, 0f), Offset(i * cs, size.height), 1.dp.toPx())
                        drawLine(p.line, Offset(0f, i * cs), Offset(size.width, i * cs), 1.dp.toPx())
                    }
                    // Tinted floor under each thread.
                    for ((k, path) in paths.withIndex()) for (c in path) {
                        drawRect(PieceColours[k].copy(alpha = if (p.dark) 0.22f else 0.16f), Offset(c.c * cs, c.r * cs), Size(cs, cs))
                    }
                    for ((k, path) in paths.withIndex()) {
                        if (path.size < 2) continue
                        val line = Path()
                        path.forEachIndexed { i, c ->
                            val x = c.c * cs + cs / 2; val y = c.r * cs + cs / 2
                            if (i == 0) line.moveTo(x, y) else line.lineTo(x, y)
                        }
                        drawPath(line, PieceColours[k], style = Stroke(cs * 0.32f * pulse.value, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                    for ((k, e) in puzzle.endpoints.withIndex()) for (c in listOf(e.first, e.second)) {
                        val centre = Offset(c.c * cs + cs / 2, c.r * cs + cs / 2)
                        drawCircle(PieceColours[k], cs * 0.36f * pulse.value, centre)
                        if (complete(k)) drawCircle(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f), cs * 0.12f, centre)
                    }
                }
            }
        }
        ActionRow {
            ActionChip(Icons.Rounded.RestartAlt, "Restart", !solved) { paths.forEach { it.clear() } }
            ActionChip(Icons.Rounded.Lightbulb, "Hint", !solved) {
                s.useHint {
                    // Lay down the true thread for the first colour that is not already right.
                    val k = puzzle.solutionPaths.indices.firstOrNull { paths[it].toList() != puzzle.solutionPaths[it] && paths[it].toList() != puzzle.solutionPaths[it].reversed() }
                    if (k != null) {
                        hints++
                        val target = puzzle.solutionPaths[k]
                        for (j in paths.indices) if (j != k) target.firstOrNull { it in paths[j] }?.let { cut(j, it) }
                        paths[k].clear(); paths[k].addAll(target)
                        s.haptic(true)
                        check()
                    }
                }
            }
        }
    }
}
