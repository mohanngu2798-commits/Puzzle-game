package app.pudhir.games

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pudhir.core.Clockwork
import app.pudhir.ui.ActionChip
import app.pudhir.ui.ActionRow
import app.pudhir.ui.LocalPalette
import app.pudhir.ui.bouncyClick
import app.pudhir.ui.jellyClick
import app.pudhir.ui.darken
import app.pudhir.ui.lighten
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ClockworkGame(s: PlaySession) {
    val p = LocalPalette.current
    val game = remember { Clockwork.generate(s.seed, s.difficulty) }
    var version by remember { mutableIntStateOf(0) }
    var hints by remember { mutableIntStateOf(0) }
    var hintGear by remember { mutableStateOf<Int?>(null) }
    var done by remember { mutableStateOf(false) }
    // Unwrapped turn counts, so a hand always animates the way it actually turned.
    val turns = remember { mutableStateListOf(*game.dial.toTypedArray()) }

    fun spin(i: Int, by: Int) {
        turns[i] += by
        for (j in game.neighbours(i)) turns[j] -= by
    }

    fun tap(i: Int) {
        if (done) return
        game.tap(i); spin(i, 1); hintGear = null; version++
        s.haptic(false)
        if (game.isSolved()) { done = true; s.haptic(true); s.onSolved(hints) }
    }

    Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        version.let {
            Text("${game.dial.count { it == 0 }} of ${game.size} clocks at twelve · ${game.moves} turns", color = p.muted)
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            val cell: Dp = minOf(maxWidth / game.cols, maxHeight / game.rows)
            Box(Modifier.size(cell * game.cols, cell * game.rows)) {
                // Brass rods between meshed gears.
                Canvas(Modifier.fillMaxSize()) {
                    val cs = size.width / game.cols
                    fun centre(i: Int) = Offset((i % game.cols + 0.5f) * cs, (i / game.cols + 0.5f) * cs)
                    for ((a, b) in game.links) {
                        drawLine(darken(s.accent, 0.35f).copy(alpha = 0.55f), centre(a), centre(b), cs * 0.16f, StrokeCap.Round)
                        drawLine(lighten(s.accent, 0.35f), centre(a), centre(b), cs * 0.07f, StrokeCap.Round)
                    }
                }
                for (i in 0 until game.size) {
                    val angle by animateFloatAsState(turns[i] * 90f, spring(dampingRatio = 0.42f, stiffness = 300f), label = "turn")
                    val home = (version.let { game.dial[i] }) == 0
                    val face by animateColorAsState(if (home) Color(0xFF2BA36B) else if (p.dark) Color(0xFF2C2832) else Color.White, label = "face")
                    Box(
                        Modifier.offset(cell * (i % game.cols), cell * (i / game.cols)).size(cell).padding(cell * 0.04f)
                            .jellyClick { tap(i) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Gear(angle, face, home, hintGear == i, s.accent, p.dark)
                    }
                }
            }
        }
        Text("A gear turns forward. Gears joined to it turn back.", color = p.muted, fontSize = 13.sp)
        ActionRow {
            ActionChip(Icons.AutoMirrored.Rounded.Undo, "Undo", !done) {
                val i = game.history().lastOrNull()
                if (i != null && game.undo()) { spin(i, -1); hintGear = null; version++ }
            }
            ActionChip(Icons.Rounded.RestartAlt, "Restart", !done) {
                while (true) { val i = game.history().lastOrNull() ?: break; game.undo(); spin(i, -1) }
                hintGear = null; version++
            }
            ActionChip(Icons.Rounded.Lightbulb, "Hint", !done) { s.useHint { hints++; hintGear = game.hint() } }
        }
    }
}

@Composable
private fun Gear(angle: Float, face: Color, home: Boolean, hinted: Boolean, accent: Color, dark: Boolean) {
    Canvas(Modifier.fillMaxSize()) {
        val r = size.minDimension / 2
        val c = center
        val metal = if (hinted) Color(0xFFF5B83D) else accent
        // Toothed rim, turning with the hand.
        rotate(angle, c) {
            val teeth = 12
            val path = Path()
            for (k in 0 until teeth * 2) {
                val a0 = Math.toRadians((k * 180.0 / teeth) - 90).toFloat()
                val a1 = Math.toRadians(((k + 1) * 180.0 / teeth) - 90).toFloat()
                val rr = if (k % 2 == 0) r else r * 0.86f
                val p0 = Offset(c.x + rr * cos(a0), c.y + rr * sin(a0))
                val p1 = Offset(c.x + rr * cos(a1), c.y + rr * sin(a1))
                if (k == 0) path.moveTo(p0.x, p0.y) else path.lineTo(p0.x, p0.y)
                path.lineTo(p1.x, p1.y)
            }
            path.close()
            drawPath(path, darken(metal, 0.15f))
        }
        drawCircle(lighten(metal, 0.2f), r * 0.8f, c)
        drawCircle(face, r * 0.68f, c)
        // Hour ticks, twelve highlighted.
        for (k in 0 until 4) {
            val a = Math.toRadians(k * 90.0 - 90).toFloat()
            val from = Offset(c.x + r * 0.54f * cos(a), c.y + r * 0.54f * sin(a))
            val to = Offset(c.x + r * 0.64f * cos(a), c.y + r * 0.64f * sin(a))
            val tick = if (k == 0) (if (home) Color.White else accent) else (if (home) Color.White.copy(alpha = 0.6f) else Color(0xFFB9B1A8))
            drawLine(tick, from, to, r * (if (k == 0) 0.1f else 0.06f), StrokeCap.Round)
        }
        // The hand.
        rotate(angle, c) {
            val ink = if (home) Color.White else if (dark) Color(0xFFF1ECE6) else Color(0xFF1C1B22)
            drawLine(ink, c, Offset(c.x, c.y - r * 0.46f), r * 0.11f, StrokeCap.Round)
        }
        drawCircle(if (home) Color.White else accent, r * 0.1f, c)
        if (hinted) drawCircle(Color(0xFFF5B83D), r * 0.98f, c, style = Stroke(r * 0.08f))
    }
}
