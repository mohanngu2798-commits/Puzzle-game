package app.pudhir.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Square
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pudhir.core.HandloomPuzzle
import app.pudhir.ui.ActionChip
import app.pudhir.ui.ActionRow
import app.pudhir.ui.LocalPalette
import app.pudhir.ui.lighten

@Composable
fun HandloomGame(s: PlaySession) = Generated(s, { HandloomPuzzle.generate(s.seed, s.difficulty) }) { HandloomBoard(s, it) }

@Composable
private fun HandloomBoard(s: PlaySession, puzzle: HandloomPuzzle) {
    val p = LocalPalette.current
    val n = puzzle.rows
    val g = remember { mutableStateListOf(*Array(n * n) { 0 }) } // 0 empty, 1 woven, 2 crossed
    val history = remember { mutableStateListOf<List<Pair<Int, Int>>>() }
    var crossMode by remember { mutableStateOf(false) }
    var hints by remember { mutableIntStateOf(0) }
    var solved by remember { mutableStateOf(false) }
    val measurer = rememberTextMeasurer()
    val maxClue = maxOf(puzzle.rowClues.maxOf { it.size }, puzzle.colClues.maxOf { it.size })
    val clueCells = maxClue * 0.55f + 0.3f

    fun check() {
        if (!solved && (0 until n * n).all { (g[it] == 1) == puzzle.solution[it] }) { solved = true; s.onSolved(hints) }
    }

    fun lineDone(clue: List<Int>, cells: List<Int>) = HandloomPuzzle.runs(cells.map { g[it] == 1 }) == clue

    Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            val side = minOf(maxWidth, maxHeight)
            Box(Modifier.size(side).shadow(6.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp)).background(p.surface).padding(8.dp)) {
                val stroke = remember { mutableListOf<Pair<Int, Int>>() }
                var paintValue by remember { mutableIntStateOf(1) }
                Canvas(
                    Modifier.fillMaxSize()
                        .pointerInput(Unit) {
                            fun cellAt(o: Offset): Int? {
                                val cs = size.width / (n + clueCells)
                                val c = ((o.x - clueCells * cs) / cs).toInt(); val r = ((o.y - clueCells * cs) / cs).toInt()
                                return if (o.x >= clueCells * cs && o.y >= clueCells * cs && r in 0 until n && c in 0 until n) r * n + c else null
                            }
                            fun paint(pos: Int) {
                                if (g[pos] == paintValue) return
                                stroke.add(pos to g[pos]); g[pos] = paintValue; s.haptic(false)
                            }
                            detectDragGestures(
                                onDragStart = { o ->
                                    val pos = cellAt(o) ?: return@detectDragGestures
                                    val want = if (crossMode) 2 else 1
                                    paintValue = if (g[pos] == want) 0 else want
                                    stroke.clear(); paint(pos)
                                },
                                onDrag = { ch, _ -> cellAt(ch.position)?.let { paint(it) } },
                                onDragEnd = { if (stroke.isNotEmpty()) history += stroke.toList(); check() },
                            )
                        }
                        .pointerInput(Unit) {
                            detectTapGestures { o ->
                                val cs = size.width / (n + clueCells)
                                val c = ((o.x - clueCells * cs) / cs).toInt(); val r = ((o.y - clueCells * cs) / cs).toInt()
                                if (o.x < clueCells * cs || o.y < clueCells * cs || r !in 0 until n || c !in 0 until n) return@detectTapGestures
                                val pos = r * n + c
                                val want = if (crossMode) 2 else 1
                                history += listOf(pos to g[pos])
                                g[pos] = if (g[pos] == want) 0 else want
                                s.haptic(false); check()
                            }
                        },
                ) {
                    val cs = size.width / (n + clueCells)
                    val ox = clueCells * cs; val oy = clueCells * cs
                    val fs = (cs / density * 0.42f).coerceAtMost(15f).sp
                    // Clues.
                    for (r in 0 until n) {
                        val done = lineDone(puzzle.rowClues[r], (0 until n).map { r * n + it })
                        val text = puzzle.rowClues[r].joinToString(" ")
                        val l = measurer.measure(text, TextStyle(fontSize = fs, fontWeight = FontWeight.Bold, color = if (done) p.muted.copy(alpha = 0.5f) else p.ink))
                        drawText(l, topLeft = Offset(ox - l.size.width - cs * 0.2f, oy + r * cs + (cs - l.size.height) / 2))
                    }
                    for (c in 0 until n) {
                        val done = lineDone(puzzle.colClues[c], (0 until n).map { it * n + c })
                        puzzle.colClues[c].reversed().forEachIndexed { k, v ->
                            val l = measurer.measure("$v", TextStyle(fontSize = fs, fontWeight = FontWeight.Bold, color = if (done) p.muted.copy(alpha = 0.5f) else p.ink))
                            drawText(l, topLeft = Offset(ox + c * cs + (cs - l.size.width) / 2, oy - (k + 1) * cs * 0.55f - cs * 0.05f))
                        }
                    }
                    // Cells.
                    for (r in 0 until n) for (c in 0 until n) {
                        val v = g[r * n + c]
                        val o = Offset(ox + c * cs, oy + r * cs)
                        if (v == 1) {
                            // A woven square: warp and weft stripes.
                            drawRoundRect(s.accent, Offset(o.x + 1.5f, o.y + 1.5f), Size(cs - 3f, cs - 3f), CornerRadius(cs * 0.12f))
                            drawLine(lighten(s.accent, 0.3f), Offset(o.x + cs * 0.25f, o.y + cs * 0.2f), Offset(o.x + cs * 0.25f, o.y + cs * 0.8f), cs * 0.08f, StrokeCap.Round)
                            drawLine(lighten(s.accent, 0.3f), Offset(o.x + cs * 0.6f, o.y + cs * 0.2f), Offset(o.x + cs * 0.6f, o.y + cs * 0.8f), cs * 0.08f, StrokeCap.Round)
                        } else if (v == 2) {
                            val m = cs * 0.3f
                            drawLine(p.muted, Offset(o.x + m, o.y + m), Offset(o.x + cs - m, o.y + cs - m), cs * 0.06f, StrokeCap.Round)
                            drawLine(p.muted, Offset(o.x + cs - m, o.y + m), Offset(o.x + m, o.y + cs - m), cs * 0.06f, StrokeCap.Round)
                        }
                    }
                    // Grid with a stronger line every five squares.
                    for (i in 0..n) {
                        val strong = i % 5 == 0 || i == n
                        val col = if (strong) p.ink.copy(alpha = 0.6f) else p.line
                        val w = if (strong) 1.6.dp.toPx() else 0.8.dp.toPx()
                        drawLine(col, Offset(ox + i * cs, oy), Offset(ox + i * cs, oy + n * cs), w)
                        drawLine(col, Offset(ox, oy + i * cs), Offset(ox + n * cs, oy + i * cs), w)
                    }
                }
            }
        }
        ActionRow {
            ActionChip(Icons.AutoMirrored.Rounded.Undo, "Undo", history.isNotEmpty()) {
                val last = history.removeAt(history.lastIndex)
                for ((pos, v) in last.reversed()) g[pos] = v
            }
            ActionChip(if (crossMode) Icons.Rounded.Close else Icons.Rounded.Square, if (crossMode) "Mark ✕" else "Weave") { crossMode = !crossMode }
            ActionChip(Icons.Rounded.Lightbulb, "Hint") {
                s.useHint {
                    val pos = (0 until n * n).firstOrNull { (g[it] == 1) != puzzle.solution[it] && (puzzle.solution[it] || g[it] == 1) }
                    if (pos != null) { hints++; history += listOf(pos to g[pos]); g[pos] = if (puzzle.solution[pos]) 1 else 2; check() }
                }
            }
        }
    }
}
