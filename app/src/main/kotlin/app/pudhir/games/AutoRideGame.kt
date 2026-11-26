package app.pudhir.games

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
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pudhir.core.AutoRidePuzzle
import app.pudhir.ui.ActionChip
import app.pudhir.ui.ActionRow
import app.pudhir.ui.LocalPalette
import app.pudhir.ui.lighten
import kotlin.math.abs

@Composable
fun AutoRideGame(s: PlaySession) = Generated(s, { AutoRidePuzzle.generate(s.seed, s.difficulty) }) { RideBoard(s, it) }

@Composable
private fun RideBoard(s: PlaySession, puzzle: AutoRidePuzzle) {
    val p = LocalPalette.current
    val n = puzzle.size
    val start = puzzle.stops.entries.first { it.value == 1 }.key
    val path = remember { mutableStateListOf(start) }
    var hints by remember { mutableIntStateOf(0) }
    var solved by remember { mutableStateOf(false) }
    val measurer = rememberTextMeasurer()

    fun nextStopNeeded(): Int = 1 + path.count { puzzle.stops.containsKey(it) }

    fun extend(q: Int) {
        if (solved) return
        val idx = path.indexOf(q)
        if (idx >= 0) { while (path.size > idx + 1) path.removeAt(path.lastIndex); return }
        val last = path.last()
        val adj = (abs(q / n - last / n) + abs(q % n - last % n)) == 1
        if (!adj) return
        val stop = puzzle.stops[q]
        if (stop != null && stop != nextStopNeeded()) return
        path.add(q); s.haptic(stop != null)
        if (path.size == n * n && nextStopNeeded() > puzzle.lastStop && path.last() == puzzle.route.last()) { solved = true; s.onSolved(hints) }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Next stop: ${minOf(nextStopNeeded(), puzzle.lastStop)} · ${path.size} of ${n * n} squares", color = p.muted)
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            val side = minOf(maxWidth, maxHeight)
            Box(Modifier.size(side).shadow(6.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp)).background(p.surface).padding(10.dp)) {
                Canvas(
                    Modifier.fillMaxSize().pointerInput(Unit) {
                        fun cellAt(o: Offset): Int? {
                            val cs = size.width / n.toFloat()
                            val r = (o.y / cs).toInt(); val c = (o.x / cs).toInt()
                            return if (o.x >= 0 && o.y >= 0 && r in 0 until n && c in 0 until n) r * n + c else null
                        }
                        detectDragGestures(
                            onDragStart = { o -> cellAt(o)?.let { if (it in path) extend(it) } },
                            onDrag = { ch, _ -> cellAt(ch.position)?.let { extend(it) } },
                        )
                    },
                ) {
                    val cs = size.width / n
                    for (r in 0 until n) for (c in 0 until n) {
                        val on = r * n + c in path
                        drawRoundRect(
                            if (on) lighten(s.accent, if (p.dark) 0.15f else 0.8f) else p.surfaceAlt,
                            Offset(c * cs + cs * 0.06f, r * cs + cs * 0.06f), Size(cs * 0.88f, cs * 0.88f), CornerRadius(cs * 0.22f),
                        )
                    }
                    if (path.size > 1) {
                        val line = Path()
                        path.forEachIndexed { i, q ->
                            val x = (q % n) * cs + cs / 2; val y = (q / n) * cs + cs / 2
                            if (i == 0) line.moveTo(x, y) else line.lineTo(x, y)
                        }
                        drawPath(line, s.accent, style = Stroke(cs * 0.3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                    for ((q, num) in puzzle.stops) {
                        val centre = Offset((q % n) * cs + cs / 2, (q / n) * cs + cs / 2)
                        drawCircle(p.ink, cs * 0.3f, centre)
                        val l = measurer.measure("$num", TextStyle(fontSize = (cs / density * 0.3f).sp, fontWeight = FontWeight.ExtraBold, color = Color.White))
                        drawText(l, topLeft = Offset(centre.x - l.size.width / 2, centre.y - l.size.height / 2))
                    }
                    // The auto at the head of the route.
                    val head = path.last()
                    drawCircle(Color(0xFFF5B83D), cs * 0.16f, Offset((head % n) * cs + cs / 2, (head / n) * cs + cs / 2))
                }
            }
        }
        ActionRow {
            ActionChip(Icons.AutoMirrored.Rounded.Undo, "Back", path.size > 1 && !solved) { path.removeAt(path.lastIndex) }
            ActionChip(Icons.Rounded.RestartAlt, "Restart", !solved) { path.clear(); path.add(start) }
            ActionChip(Icons.Rounded.Lightbulb, "Hint", !solved) {
                s.useHint {
                    hints++
                    var k = 0
                    while (k < path.size && path[k] == puzzle.route[k]) k++
                    while (path.size > k) path.removeAt(path.lastIndex)
                    // Drive on to the next stop along the true route.
                    var i = path.size
                    while (i < puzzle.route.size) { extend(puzzle.route[i]); if (puzzle.stops.containsKey(puzzle.route[i])) break; i++ }
                }
            }
        }
    }
}
