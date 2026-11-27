package app.pudhir.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pudhir.core.TapCricket
import app.pudhir.ui.LocalPalette
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private const val RUNUP = 1.1f
private const val RESULT = 1.6f

/** The batters who take turns at the crease, one per wicket. */
private data class Batter(val name: String, val shirt: Color, val hair: Color, val band: Color)
private val BATTERS = listOf(
    Batter("Arjun", Color(0xFF2F6FE0), Color(0xFF1E1A2E), Color(0xFFE5484D)),
    Batter("Nila", Color(0xFF2BA36B), Color(0xFF6A2C8C), Color(0xFFF5B83D)),
    Batter("Veera", Color(0xFFF26B3A), Color(0xFFF2D06B), Color(0xFF2F6FE0)),
)
private val SKIN = Color(0xFFF7D2B0)

@Composable
fun TapCricketGame(s: PlaySession) {
    val p = LocalPalette.current
    val game = remember { TapCricket(s.seed, s.difficulty) }
    val still = LocalInspectionMode.current
    var phase by remember { mutableIntStateOf(if (still) 1 else 0) } // 0 run-up, 1 ball in the air, 2 result
    var clock by remember { mutableFloatStateOf(if (still) 0.7f else 0f) }
    var delivery by remember { mutableStateOf(game.next()) }
    var swingAt by remember { mutableStateOf<Float?>(null) }
    var shot by remember { mutableStateOf<TapCricket.Shot?>(null) }
    var finished by remember { mutableStateOf(false) }
    var swingT by remember { mutableFloatStateOf(-1f) }
    var last by remember { mutableStateOf<TapCricket.Shot?>(null) }
    val measurer = rememberTextMeasurer()

    fun settle(error: Float?) {
        val r = game.judge(delivery, error)
        shot = r; last = r; phase = 2; clock = 0f
        s.haptic(r.runs >= 4 || r.out)
    }

    if (!still) LaunchedEffect(Unit) {
        var prev = androidx.compose.runtime.withFrameNanos { it }
        while (!finished) {
            val now = androidx.compose.runtime.withFrameNanos { it }
            val dt = ((now - prev) / 1e9f).coerceAtMost(0.05f); prev = now
            clock += dt
            if (swingAt != null) swingT += dt
            when (phase) {
                0 -> if (clock >= RUNUP) { phase = 1; clock = 0f }
                1 -> {
                    val sw = swingAt
                    if (sw != null) { if (clock >= maxOf(sw, delivery.travel)) settle(sw - delivery.travel) }
                    else if (clock >= delivery.travel + 0.24f) settle(null)
                }
                2 -> if (clock >= RESULT) {
                    if (game.isOver()) {
                        finished = true
                        s.onGameOver(game.runs, "All out for ${game.runs} in ${game.over}.${game.ballInOver} overs")
                    } else {
                        delivery = game.next(); swingAt = null; swingT = -1f; shot = null; phase = 0; clock = 0f
                    }
                }
            }
        }
    }

    val batter = BATTERS[game.wickets.coerceAtMost(BATTERS.size - 1)]
    val bowler = delivery.bowler

    Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("${game.runs}/${game.wickets}", color = p.ink, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
            Text("  Over ${game.over}.${game.ballInOver}", color = p.muted, modifier = Modifier.weight(1f))
            Text(
                if (game.streak >= 3) "🔥 On fire ×${game.streak}" else "${3 - game.wickets} wickets left",
                color = if (game.streak >= 3) Color(0xFFEF6B3A) else p.muted, fontWeight = FontWeight.Bold,
            )
        }
        Text("${bowler.name} bowls ${bowler.style}" + if (delivery.slower && phase == 1) " · slower ball!" else "", color = p.muted, fontSize = 13.sp)
        Canvas(
            Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp).clip(RoundedCornerShape(24.dp))
                .pointerInput(Unit) {
                    detectTapGestures {
                        if (phase == 1 && swingAt == null && clock > 0.12f) {
                            swingAt = clock; swingT = 0f; s.haptic(false)
                            if (clock >= delivery.travel) settle(clock - delivery.travel)
                        }
                    }
                },
        ) {
            val w = size.width; val h = size.height
            // Sky, stands and field.
            drawRect(Brush.verticalGradient(listOf(Color(0xFF5B4BC4), Color(0xFFE88BB0), Color(0xFFFFC98A))), size = Size(w, h * 0.2f))
            drawCircle(Brush.radialGradient(listOf(Color(0xCCFFF6D8), Color(0x00FFF6D8)), Offset(w * 0.78f, h * 0.06f), w * 0.12f), w * 0.12f, Offset(w * 0.78f, h * 0.06f))
            for (x in listOf(0.06f, 0.94f)) {
                drawLine(Color(0xFF3A3550), Offset(w * x, h * 0.2f), Offset(w * x, h * 0.03f), 5f)
                drawRoundRect(Color(0xFFFFFBEA), Offset(w * x - w * 0.045f, h * 0.015f), Size(w * 0.09f, h * 0.03f), CornerRadius(6f))
                drawCircle(Brush.radialGradient(listOf(Color(0x88FFFBEA), Color(0x00FFFBEA)), Offset(w * x, h * 0.03f), w * 0.2f), w * 0.2f, Offset(w * x, h * 0.03f))
            }
            for (i in 0 until 46) {
                val x = (i * 0.618f % 1f) * w; val y = h * (0.12f + (i % 4) * 0.02f)
                val hop = if (shot?.let { it.runs >= 4 } == true) abs(sin(clock * 14f + i)) * 6f else 0f
                drawCircle(listOf(Color(0xFFE5484D), Color(0xFFF5B83D), Color(0xFF2F6FE0), Color(0xFF9B5DE5), Color.White)[i % 5], w * 0.016f, Offset(x, y - hop))
            }
            drawRect(Color(0xFF3FAE5A), Offset(0f, h * 0.2f), Size(w, h * 0.8f))
            for (k in 0 until 8) drawRect(Color(0xFF47B862), Offset(0f, h * (0.2f + k * 0.1f)), Size(w, h * 0.05f))
            val topY = h * 0.3f; val batY = h * 0.84f
            val pitch = Path().apply {
                moveTo(w * 0.42f, topY); lineTo(w * 0.58f, topY); lineTo(w * 0.7f, h * 0.95f); lineTo(w * 0.3f, h * 0.95f); close()
            }
            drawPath(pitch, Color(0xFFE2C48E))
            drawLine(Color.White, Offset(w * 0.4f, topY + h * 0.03f), Offset(w * 0.6f, topY + h * 0.03f), 3f)
            drawLine(Color.White, Offset(w * 0.31f, batY - h * 0.02f), Offset(w * 0.69f, batY - h * 0.02f), 4f)
            // Far stumps.
            for (k in -1..1) drawLine(Color(0xFFFFF2D6), Offset(w / 2 + k * w * 0.012f, topY + h * 0.03f), Offset(w / 2 + k * w * 0.012f, topY - h * 0.01f), 4f)

            // The bowler: runs in, then bowls and reacts.
            val bowlerMood = when { shot == null -> 0; shot!!.out -> 1; shot!!.runs >= 4 -> -1; else -> 0 }
            val runT = if (phase == 0) (clock / RUNUP).coerceIn(0f, 1f) else 1f
            val bowlerY = h * (0.14f + 0.16f * runT)
            val bob = if (phase == 0) abs(sin(clock * 16f)) * h * 0.012f else 0f
            val armAngle = if (phase == 0 && runT > 0.75f) (runT - 0.75f) / 0.25f * 300f else if (phase == 0) 0f else 300f
            chibi(
                Offset(w / 2, bowlerY - bob), w * (0.045f + 0.02f * runT), SKIN, Color(bowler.hair), Color(bowler.colour), Color.White,
                bowlerMood, armAngle, bat = false, cheer = bowlerMood == 1 && phase == 2, t = clock,
            )

            // The batter at the bottom crease.
            val batterMood = when { shot == null -> 0; shot!!.out -> -1; shot!!.runs >= 4 -> 1; else -> 0 }
            val swingAge = swingT
            val batAngle = when {
                swingAge < 0f -> -35f
                swingAge < 0.18f -> -35f + swingAge / 0.18f * 150f
                else -> 115f
            }
            val batterAt = Offset(w * 0.38f, batY - h * 0.02f)
            val stumpsX = w / 2
            // Near stumps (fly apart when bowled).
            val bowled = shot == TapCricket.Shot.Bowled && phase == 2
            for (k in -1..1) {
                val fall = if (bowled) clock * (k * 60f + 20f) else 0f
                rotate(fall, Offset(stumpsX + k * w * 0.02f, batY + h * 0.05f)) {
                    drawLine(Color(0xFFFFF2D6), Offset(stumpsX + k * w * 0.02f, batY + h * 0.05f), Offset(stumpsX + k * w * 0.02f, batY - h * 0.05f), 7f, StrokeCap.Round)
                }
            }
            chibi(batterAt, w * 0.085f, SKIN, batter.hair, batter.shirt, batter.band, batterMood, batAngle, bat = true, cheer = batterMood == 1 && phase == 2, t = clock)

            // The ball.
            if (phase == 1 || phase == 2) {
                val travel = delivery.travel
                val contact = Offset(w / 2 + delivery.drift * w * 0.08f, batY - h * 0.04f)
                val ball: Offset; val r: Float
                if (phase == 1 || shot == null || shot!!.runs == 0 && !shot!!.out || shot == TapCricket.Shot.Bowled) {
                    val u = if (phase == 1) clock / travel else 1f + clock / travel
                    val ground = topY + (batY - topY) * u
                    val bounce = 0.62f
                    val lift = if (u < bounce) (1 - u / bounce) * h * 0.08f else sin(((u - bounce) / (1.4f - bounce)).coerceIn(0f, 1f) * PI.toFloat()) * h * 0.05f
                    ball = Offset(w / 2 + delivery.drift * w * 0.08f * u * u, ground - lift - h * 0.02f)
                    r = w * (0.012f + 0.014f * u.coerceAtMost(1.3f))
                    if (u < 1.4f) drawCircle(Color(0x33000000), r, Offset(ball.x, ground))
                } else {
                    val q = clock
                    val (vx, vy, g) = when (shot!!) {
                        TapCricket.Shot.Six -> Triple(w * 0.35f, -h * 1.5f, h * 0.9f)
                        TapCricket.Shot.Four -> Triple(-w * 0.9f, -h * 0.35f, h * 0.1f)
                        TapCricket.Shot.Two -> Triple(w * 0.5f, -h * 0.4f, h * 0.3f)
                        TapCricket.Shot.Caught -> Triple(w * 0.05f, -h * 1.1f, h * 1.4f)
                        else -> Triple(-w * 0.25f, -h * 0.3f, h * 0.3f)
                    }
                    ball = Offset(contact.x + vx * q, contact.y + vy * q + g * q * q)
                    r = w * if (shot == TapCricket.Shot.Six) (0.026f + 0.02f * q) else 0.024f
                }
                drawCircle(Color(0xFFD7263D), r, ball)
                drawArc(Color.White, -60f, 120f, false, Offset(ball.x - r * 0.6f, ball.y - r * 0.6f), Size(r * 1.2f, r * 1.2f), style = Stroke(r * 0.18f))
                drawCircle(Color(0x88FFFFFF), r * 0.3f, Offset(ball.x - r * 0.35f, ball.y - r * 0.35f))
            }

            // Timing helper ring on easy levels.
            if (phase == 1 && s.difficulty == app.pudhir.core.Difficulty.Easy) {
                val u = (clock / delivery.travel).coerceIn(0f, 1f)
                drawCircle(Color(0x88FFFFFF), w * (0.16f - 0.12f * u), Offset(w / 2, batY - h * 0.04f), style = Stroke(4f))
            }

            // Anime speed lines on a boundary.
            if (phase == 2 && (shot == TapCricket.Shot.Six || shot == TapCricket.Shot.Four) && clock < 0.9f) {
                val centre = Offset(w / 2, h * 0.45f)
                for (k in 0 until 28) {
                    val a = k / 28f * 2 * PI.toFloat() + clock
                    val dir = Offset(kotlin.math.cos(a), sin(a))
                    drawLine(Color.White.copy(alpha = 0.55f * (1 - clock / 0.9f)), centre + dir * (w * 0.35f), centre + dir * (w * (0.5f + (k % 3) * 0.12f)), 3f + (k % 2) * 3f, StrokeCap.Round)
                }
            }
            // Big result text.
            val r = shot
            if (phase == 2 && r != null) {
                val pop = (clock / 0.25f).coerceAtMost(1f)
                val size = (if (r.runs >= 4 || r.out) 46f else 28f) * (0.6f + 0.4f * pop)
                val colour = when { r.out -> Color(0xFFE5484D); r == TapCricket.Shot.Six -> Color(0xFFF5B83D); r == TapCricket.Shot.Four -> Color(0xFF2F6FE0); else -> Color.White }
                val l = measurer.measure(r.label, TextStyle(fontSize = size.sp, fontWeight = FontWeight.Black, color = colour))
                val at = Offset(w / 2 - l.size.width / 2, h * 0.45f - l.size.height / 2)
                val shadow = measurer.measure(r.label, TextStyle(fontSize = size.sp, fontWeight = FontWeight.Black, color = Color(0x66000000)))
                drawText(shadow, topLeft = at + Offset(4f, 4f)); drawText(l, topLeft = at)
            } else if (phase == 0) {
                val l = measurer.measure("Tap when the ball reaches the bat", TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White))
                drawText(l, topLeft = Offset(w / 2 - l.size.width / 2, h * 0.47f))
            }
        }
        Text(
            last?.let { "Last ball: ${it.label} · ${game.sixes} sixes" } ?: "${batter.name} is on strike. Perfect timing hits a six!",
            color = p.muted, fontSize = 13.sp,
        )
    }
}

/**
 * A chibi cricketer: big head, spiky anime hair, huge shiny eyes. [mood] 1 happy, -1 sad, 0 focused.
 * [arm] is the bowling arm or bat angle in degrees.
 */
private fun DrawScope.chibi(
    c: Offset, u: Float, skin: Color, hair: Color, shirt: Color, band: Color, mood: Int, arm: Float,
    bat: Boolean, cheer: Boolean, t: Float,
) {
    val jump = if (cheer) abs(sin(t * 12f)) * u * 0.5f else 0f
    val o = Offset(c.x, c.y - jump)
    // Legs and shoes.
    for (k in listOf(-1, 1)) {
        drawRoundRect(Color(0xFFF4F4F4), Offset(o.x + k * u * 0.35f - u * 0.17f, o.y + u * 1.6f), Size(u * 0.34f, u * 0.8f), CornerRadius(u * 0.15f))
        drawRoundRect(Color(0xFF2B2440), Offset(o.x + k * u * 0.35f - u * 0.24f, o.y + u * 2.3f), Size(u * 0.48f, u * 0.22f), CornerRadius(u * 0.11f))
    }
    // Body, with a cel-shaded side and an ink outline.
    val ink = Color(0xFF2B2440)
    val line = Stroke(u * 0.07f)
    drawRoundRect(shirt, Offset(o.x - u * 0.7f, o.y + u * 0.75f), Size(u * 1.4f, u * 1.05f), CornerRadius(u * 0.4f))
    drawRoundRect(app.pudhir.ui.darken(shirt, 0.25f), Offset(o.x + u * 0.25f, o.y + u * 0.75f), Size(u * 0.45f, u * 1.05f), CornerRadius(u * 0.4f))
    drawRoundRect(ink, Offset(o.x - u * 0.7f, o.y + u * 0.75f), Size(u * 1.4f, u * 1.05f), CornerRadius(u * 0.4f), style = line)
    drawLine(Color.White, Offset(o.x - u * 0.2f, o.y + u * 0.85f), Offset(o.x + u * 0.2f, o.y + u * 0.85f), u * 0.12f, StrokeCap.Round)
    // Arm (bowling arm or bat).
    val shoulder = Offset(o.x + u * 0.6f, o.y + u * 1.0f)
    val armUp = if (cheer) -150f else arm
    rotate(armUp, shoulder) {
        drawLine(skin, shoulder, shoulder + Offset(0f, u * 0.75f), u * 0.26f, StrokeCap.Round)
        if (bat) {
            drawLine(Color(0xFF7A4B2A), shoulder + Offset(0f, u * 0.7f), shoulder + Offset(0f, u * 1.05f), u * 0.14f, StrokeCap.Round)
            drawRoundRect(Color(0xFFEBC98A), shoulder + Offset(-u * 0.16f, u * 1.0f), Size(u * 0.32f, u * 1.5f), CornerRadius(u * 0.14f))
        } else if (arm < 250f) {
            drawCircle(Color(0xFFD7263D), u * 0.17f, shoulder + Offset(0f, u * 0.85f))
        }
    }
    drawLine(skin, Offset(o.x - u * 0.6f, o.y + u * 1.0f), Offset(o.x - u * 0.75f, o.y + u * 1.6f), u * 0.26f, StrokeCap.Round)
    // Head.
    drawCircle(hair, u * 1.06f, o + Offset(0f, -u * 0.05f))
    drawCircle(ink, u * 1.1f, o + Offset(0f, -u * 0.05f), style = line)
    drawCircle(skin, u, o + Offset(0f, u * 0.08f))
    // Soft cel shadow on the cheek and chin.
    drawArc(app.pudhir.ui.darken(skin, 0.12f), -20f, 130f, false, o + Offset(-u * 0.92f, -u * 0.84f), Size(u * 1.84f, u * 1.84f), style = Stroke(u * 0.16f))
    // Spiky hair on top, and a fringe.
    val spikes = Path().apply {
        moveTo(o.x - u * 1.05f, o.y - u * 0.1f)
        val tips = listOf(-0.95f to -0.95f, -0.45f to -1.5f, 0.1f to -1.25f, 0.55f to -1.55f, 1.0f to -0.85f)
        var bx = -0.8f
        for ((tx, ty) in tips) { lineTo(o.x + tx * u, o.y + ty * u); bx += 0.45f; lineTo(o.x + bx * u, o.y - u * 0.75f) }
        lineTo(o.x + u * 1.05f, o.y - u * 0.1f)
        lineTo(o.x + u * 0.6f, o.y - u * 0.25f); lineTo(o.x + u * 0.35f, o.y - u * 0.55f)
        lineTo(o.x + u * 0.1f, o.y - u * 0.2f); lineTo(o.x - u * 0.2f, o.y - u * 0.55f)
        lineTo(o.x - u * 0.45f, o.y - u * 0.2f); lineTo(o.x - u * 0.7f, o.y - u * 0.5f)
        close()
    }
    drawPath(spikes, hair)
    drawPath(spikes, ink, style = Stroke(u * 0.06f))
    // Glossy hair highlight.
    drawArc(Color.White.copy(alpha = 0.35f), 200f, 70f, false, o + Offset(-u * 0.75f, -u * 0.95f), Size(u * 1.3f, u * 1.1f), style = Stroke(u * 0.12f, cap = StrokeCap.Round))
    drawRect(band, Offset(o.x - u * 0.98f, o.y - u * 0.5f), Size(u * 1.96f, u * 0.18f))
    // Eyes.
    for (k in listOf(-1, 1)) {
        val e = o + Offset(k * u * 0.4f, u * 0.2f)
        when (mood) {
            1 -> drawArc(ink, 200f, 140f, false, e - Offset(u * 0.18f, u * 0.1f), Size(u * 0.36f, u * 0.3f), style = Stroke(u * 0.1f, cap = StrokeCap.Round))
            -1 -> drawArc(ink, 20f, 140f, false, e - Offset(u * 0.18f, u * 0.2f), Size(u * 0.36f, u * 0.3f), style = Stroke(u * 0.1f, cap = StrokeCap.Round))
            else -> {
                drawOval(ink, e - Offset(u * 0.15f, u * 0.22f), Size(u * 0.3f, u * 0.44f))
                drawOval(Brush.verticalGradient(listOf(app.pudhir.ui.darken(shirt, 0.3f), app.pudhir.ui.lighten(shirt, 0.35f)), e.y - u * 0.12f, e.y + u * 0.2f), e - Offset(u * 0.11f, u * 0.1f), Size(u * 0.22f, u * 0.3f))
                drawLine(ink, e + Offset(-u * 0.2f, -u * 0.24f), e + Offset(u * 0.18f, -u * 0.27f), u * 0.07f, StrokeCap.Round)
                drawCircle(Color.White, u * 0.07f, e + Offset(-u * 0.05f, -u * 0.1f))
                drawCircle(Color.White, u * 0.035f, e + Offset(u * 0.06f, u * 0.1f))
            }
        }
    }
    // Blush and mouth.
    for (k in listOf(-1, 1)) drawOval(Color(0x66FF7A8A), o + Offset(k * u * 0.62f - u * 0.14f, u * 0.48f), Size(u * 0.28f, u * 0.14f))
    when (mood) {
        1 -> drawArc(Color(0xFFB8324A), 0f, 180f, true, o + Offset(-u * 0.2f, u * 0.5f), Size(u * 0.4f, u * 0.32f))
        -1 -> drawArc(ink, 200f, 140f, false, o + Offset(-u * 0.15f, u * 0.62f), Size(u * 0.3f, u * 0.2f), style = Stroke(u * 0.07f, cap = StrokeCap.Round))
        else -> drawLine(ink, o + Offset(-u * 0.1f, u * 0.62f), o + Offset(u * 0.1f, u * 0.62f), u * 0.07f, StrokeCap.Round)
    }
}
