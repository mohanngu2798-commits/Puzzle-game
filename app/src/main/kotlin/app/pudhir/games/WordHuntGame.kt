package app.pudhir.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pudhir.core.PlacedWord
import app.pudhir.core.WordHunt
import app.pudhir.ui.ActionChip
import app.pudhir.ui.ActionRow
import app.pudhir.ui.LocalPalette
import app.pudhir.ui.PieceColours
import app.pudhir.ui.lighten
import app.pudhir.ui.popIn
import kotlin.math.abs
import kotlin.math.max

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordHuntGame(s: PlaySession) {
    val p = LocalPalette.current
    val game = remember { WordHunt.generate(s.seed, s.difficulty) }
    val n = game.size
    val found = remember { mutableStateListOf<PlacedWord>() }
    var dragFrom by remember { mutableIntStateOf(-1) }
    var dragTo by remember { mutableIntStateOf(-1) }
    var hints by remember { mutableIntStateOf(0) }
    var flash by remember { mutableStateOf<PlacedWord?>(null) }
    val measurer = rememberTextMeasurer()

    /** Snaps the drag end to the nearest straight line (8 directions) from the start. */
    fun snap(from: Int, to: Int): Int {
        val r0 = from / n; val c0 = from % n; val dr = to / n - r0; val dc = to % n - c0
        val len = max(abs(dr), abs(dc))
        if (len == 0) return from
        val sr = Integer.signum(if (abs(dr) * 2 >= len) dr else 0); val sc = Integer.signum(if (abs(dc) * 2 >= len) dc else 0)
        var k = len
        while (k > 0 && (r0 + sr * k !in 0 until n || c0 + sc * k !in 0 until n)) k--
        return (r0 + sr * k) * n + (c0 + sc * k)
    }

    Column(Modifier.fillMaxSize().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Theme: ${game.theme}", color = s.accent, fontWeight = FontWeight.Bold)
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            val side = minOf(maxWidth, maxHeight)
            Box(Modifier.size(side).shadow(6.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp)).background(p.surface).padding(8.dp)) {
                Canvas(
                    Modifier.fillMaxSize().pointerInput(Unit) {
                        fun cellAt(o: Offset): Int {
                            val cs = size.width / n.toFloat()
                            return (o.y / cs).toInt().coerceIn(0, n - 1) * n + (o.x / cs).toInt().coerceIn(0, n - 1)
                        }
                        detectDragGestures(
                            onDragStart = { o -> dragFrom = cellAt(o); dragTo = dragFrom; s.haptic(false) },
                            onDrag = { ch, _ -> val t = snap(dragFrom, cellAt(ch.position)); if (t != dragTo) { dragTo = t; s.haptic(false) } },
                            onDragEnd = {
                                val w = game.match(dragFrom, dragTo)
                                if (w != null && w !in found) {
                                    found += w; flash = w; s.haptic(true)
                                    if (found.size == game.words.size) s.onSolved(hints)
                                }
                                dragFrom = -1; dragTo = -1
                            },
                            onDragCancel = { dragFrom = -1; dragTo = -1 },
                        )
                    },
                ) {
                    val cs = size.width / n
                    fun centre(q: Int) = Offset((q % n) * cs + cs / 2, (q / n) * cs + cs / 2)
                    for ((k, w) in found.withIndex()) {
                        val cells = w.cells(n)
                        drawLine(PieceColours[k % PieceColours.size].copy(alpha = 0.35f), centre(cells.first()), centre(cells.last()), cs * 0.78f, StrokeCap.Round)
                    }
                    if (dragFrom >= 0) drawLine(s.accent.copy(alpha = 0.45f), centre(dragFrom), centre(dragTo), cs * 0.78f, StrokeCap.Round)
                    val style = TextStyle(fontSize = (cs / density * 0.5f).sp, fontWeight = FontWeight.Bold, color = p.ink)
                    for (q in 0 until n * n) {
                        val l = measurer.measure(game.grid[q].toString(), style)
                        val c = centre(q)
                        drawText(l, topLeft = Offset(c.x - l.size.width / 2, c.y - l.size.height / 2))
                    }
                }
            }
        }
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (w in game.words) {
                val done = w in found
                val k = found.indexOf(w)
                Box(
                    Modifier.then(if (flash == w) Modifier.popIn() else Modifier).clip(RoundedCornerShape(50))
                        .background(if (done) lighten(PieceColours[k % PieceColours.size], 0.6f) else p.surfaceAlt)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(w.word, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (done) p.muted else p.ink, textDecoration = if (done) TextDecoration.LineThrough else null)
                }
            }
        }
        ActionRow {
            ActionChip(Icons.Rounded.Lightbulb, "Hint") {
                s.useHint {
                    val w = game.words.firstOrNull { it !in found }
                    if (w != null) { hints++; found += w; flash = w; if (found.size == game.words.size) s.onSolved(hints) }
                }
            }
        }
    }
}
