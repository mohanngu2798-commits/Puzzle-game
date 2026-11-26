package app.pudhir.games

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pudhir.core.KolamPuzzle
import app.pudhir.ui.ActionChip
import app.pudhir.ui.ActionRow
import app.pudhir.ui.LocalPalette
import app.pudhir.ui.lighten
import kotlin.math.abs

@Composable
fun KolamGame(s: PlaySession) {
    val p = LocalPalette.current
    val puzzle = remember { KolamPuzzle.generate(s.seed, s.difficulty) }
    val R = puzzle.rows; val C = puzzle.cols
    // Edge states: 0 empty, 1 line, 2 crossed out.
    val h = remember { mutableStateListOf(*Array((R + 1) * C) { 0 }) }
    val v = remember { mutableStateListOf(*Array(R * (C + 1)) { 0 }) }
    val history = remember { mutableStateListOf<Pair<Int, Int>>() } // signed edge id, old state
    var hints by remember { mutableIntStateOf(0) }
    var solved by remember { mutableStateOf(false) }
    val bloom = remember { Animatable(0f) }
    val measurer = rememberTextMeasurer()

    fun hLine(r: Int, c: Int) = h[r * C + c] == 1
    fun vLine(r: Int, c: Int) = v[r * (C + 1) + c] == 1
    fun sides(r: Int, c: Int) = listOf(hLine(r, c), hLine(r + 1, c), vLine(r, c), vLine(r, c + 1)).count { it }

    fun check() {
        for (r in 0..R) for (c in 0 until C) if (hLine(r, c) != puzzle.hEdge(r, c)) return
        for (r in 0 until R) for (c in 0..C) if (vLine(r, c) != puzzle.vEdge(r, c)) return
        solved = true
    }

    LaunchedEffect(solved) {
        if (solved) { bloom.animateTo(1f, tween(900)); s.onSolved(hints) }
    }

    fun toggle(isH: Boolean, idx: Int, to: Int? = null) {
        if (solved) return
        val list = if (isH) h else v
        val old = list[idx]
        list[idx] = to ?: ((old + 1) % 3)
        history += (if (isH) idx else -(idx + 1)) to old
        s.haptic(false)
        check()
    }

    // Fix one wrong edge, or draw one missing loop edge.
    fun kolamHint() {
        hints++
        for (r in 0..R) for (c in 0 until C) if (hLine(r, c) && !puzzle.hEdge(r, c)) { toggle(true, r * C + c, 2); return }
        for (r in 0 until R) for (c in 0..C) if (vLine(r, c) && !puzzle.vEdge(r, c)) { toggle(false, r * (C + 1) + c, 2); return }
        for (r in 0..R) for (c in 0 until C) if (!hLine(r, c) && puzzle.hEdge(r, c)) { toggle(true, r * C + c, 1); return }
        for (r in 0 until R) for (c in 0..C) if (!vLine(r, c) && puzzle.vEdge(r, c)) { toggle(false, r * (C + 1) + c, 1); return }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            val side = minOf(maxWidth, maxHeight)
            Box(Modifier.size(side).shadow(6.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp)).background(p.surface)) {
                Canvas(
                    Modifier.fillMaxSize().padding(18.dp).pointerInput(Unit) {
                        detectTapGestures { pos ->
                            val cell = size.width / C.toFloat()
                            val gx = pos.x / cell; val gy = pos.y / cell
                            // Nearest horizontal edge vs nearest vertical edge.
                            val hr = Math.round(gy); val hc = gx.toInt().coerceIn(0, C - 1)
                            val vr = gy.toInt().coerceIn(0, R - 1); val vc = Math.round(gx)
                            val dh = abs(gy - hr); val dv = abs(gx - vc)
                            if (dh < dv && hr in 0..R) toggle(true, hr * C + hc)
                            else if (vc in 0..C) toggle(false, vr * (C + 1) + vc)
                        }
                    },
                ) {
                    val cell = size.width / C
                    val stroke = cell * 0.13f
                    // Bloom: inside of the loop fills with colour when solved.
                    if (bloom.value > 0f) {
                        for (r in 0 until R) for (c in 0 until C) if (puzzle.inside[r * C + c]) {
                            val inset = cell * 0.5f * (1 - bloom.value)
                            drawRect(lighten(s.accent, 0.75f).copy(alpha = bloom.value), Offset(c * cell + inset, r * cell + inset), Size(cell - 2 * inset, cell - 2 * inset))
                        }
                    }
                    // Clues.
                    for (r in 0 until R) for (c in 0 until C) {
                        val clue = puzzle.clues[r * C + c]
                        if (clue < 0) continue
                        val n = sides(r, c)
                        val col = when { n == clue -> p.good; n > clue -> p.bad; else -> p.ink }
                        val layout = measurer.measure("$clue", TextStyle(fontSize = (cell / density * 0.42f).sp, fontWeight = FontWeight.Bold, color = col))
                        drawText(layout, topLeft = Offset(c * cell + (cell - layout.size.width) / 2, r * cell + (cell - layout.size.height) / 2))
                    }
                    val lineCol = if (solved) s.accent else p.ink
                    for (r in 0..R) for (c in 0 until C) {
                        val st = h[r * C + c]
                        val a = Offset(c * cell, r * cell); val b = Offset((c + 1) * cell, r * cell)
                        if (st == 1) drawLine(lineCol, a, b, stroke, StrokeCap.Round)
                        if (st == 2) drawCross(Offset((a.x + b.x) / 2, a.y), cell * 0.08f, p.muted)
                    }
                    for (r in 0 until R) for (c in 0..C) {
                        val st = v[r * (C + 1) + c]
                        val a = Offset(c * cell, r * cell); val b = Offset(c * cell, (r + 1) * cell)
                        if (st == 1) drawLine(lineCol, a, b, stroke, StrokeCap.Round)
                        if (st == 2) drawCross(Offset(a.x, (a.y + b.y) / 2), cell * 0.08f, p.muted)
                    }
                    for (r in 0..R) for (c in 0..C) drawCircle(if (solved) s.accent else p.ink, cell * 0.075f, Offset(c * cell, r * cell))
                }
            }
        }
        ActionRow {
            ActionChip(Icons.AutoMirrored.Rounded.Undo, "Undo", history.isNotEmpty() && !solved) {
                val (id, old) = history.removeAt(history.lastIndex)
                if (id >= 0) h[id] = old else v[-id - 1] = old
            }
            ActionChip(Icons.Rounded.RestartAlt, "Restart", !solved) {
                for (i in h.indices) h[i] = 0
                for (i in v.indices) v[i] = 0
                history.clear()
            }
            ActionChip(Icons.Rounded.Lightbulb, "Hint", !solved) { s.useHint { kolamHint() } }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCross(c: Offset, r: Float, col: androidx.compose.ui.graphics.Color) {
    drawLine(col, Offset(c.x - r, c.y - r), Offset(c.x + r, c.y + r), r * 0.5f, StrokeCap.Round)
    drawLine(col, Offset(c.x - r, c.y + r), Offset(c.x + r, c.y - r), r * 0.5f, StrokeCap.Round)
}
