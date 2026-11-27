package app.pudhir.games

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pudhir.core.Paramapadham
import app.pudhir.ui.LocalPalette
import app.pudhir.ui.PrimaryButton
import app.pudhir.ui.jellyClick
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private fun squareCentre(sq: Int, cell: Float): Offset {
    val i = (sq - 1).coerceAtLeast(0)
    val row = i / 10
    val col = if (row % 2 == 0) i % 10 else 9 - i % 10
    return Offset((col + 0.5f) * cell, (9 - row + 0.5f) * cell)
}

@Composable
fun ParamapadhamGame(s: PlaySession) {
    val p = LocalPalette.current
    val game = remember { Paramapadham(s.seed, s.difficulty) }
    var face by remember { mutableIntStateOf(1) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Roll the dice to start") }
    // Animated square positions (fractional while walking).
    val walk = remember { arrayOf(Animatable(0f), Animatable(0f)) }
    val dieSpin = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val colours = listOf(s.accent, Color(0xFF3A86F2))

    suspend fun doRoll() {
        // Tumble the die.
        scope.launch { dieSpin.snapTo(0f); dieSpin.animateTo(1f, tween(520)) }
        repeat(7) { face = 1 + (it * 5 + face) % 6; delay(70) }
        val r = game.roll()
        face = r.die
        s.haptic(false)
        val who = if (r.player == 0) "You" else "The phone"
        val a = walk[r.player]
        val from = a.value.roundToInt()
        if (r.walkedTo == from) message = "$who rolled ${r.die}, too far for Vaikuntam"
        for (sq in from + 1..r.walkedTo) { a.animateTo(sq.toFloat(), spring(dampingRatio = 0.7f, stiffness = 900f)); delay(60) }
        if (r.endedAt != r.walkedTo) {
            val up = r.endedAt > r.walkedTo
            message = if (up) "$who climbed a ladder to ${r.endedAt}!" else "$who met a snake, down to ${r.endedAt}"
            s.haptic(true)
            delay(200)
            a.animateTo(r.endedAt.toFloat(), tween(650))
        } else if (r.walkedTo != from) message = "$who rolled ${r.die}" + if (r.again) ", roll again!" else ""
    }

    suspend fun phoneLoop() {
        while (game.winner() < 0 && game.turn == 1) { delay(650); doRoll() }
    }

    fun tapRoll() {
        if (busy || game.turn != 0 || game.winner() >= 0) return
        busy = true
        scope.launch {
            doRoll()
            if (game.winner() < 0) phoneLoop()
            delay(300)
            when (game.winner()) {
                0 -> s.onSolved(0)
                1 -> s.onGameOver(null, "The phone reached Vaikuntam first")
                else -> message = if (message.startsWith("You")) message else "Your turn"
            }
            busy = false
        }
    }

    Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, color = p.ink, fontWeight = FontWeight.SemiBold)
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            val side = minOf(maxWidth, maxHeight)
            Box(Modifier.size(side).shadow(8.dp, RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp))) {
                Canvas(Modifier.fillMaxSize()) {
                    val cs = size.width / 10
                    val tones = listOf(Color(0xFFFFF4E0), Color(0xFFFFE3C2), Color(0xFFE9F5E1), Color(0xFFE3EEFB))
                    for (sq in 1..100) {
                        val c = squareCentre(sq, cs)
                        drawRect(tones[(sq + (sq - 1) / 10) % 4], c - Offset(cs / 2, cs / 2), androidx.compose.ui.geometry.Size(cs, cs))
                    }
                    // Vaikuntam glow on 100.
                    drawCircle(Color(0xFFF5B83D).copy(alpha = 0.5f), cs * 0.45f, squareCentre(100, cs))
                    for ((foot, top) in game.ladders) {
                        val a = squareCentre(foot, cs); val b = squareCentre(top, cs)
                        val d = b - a; val len = d.getDistance(); val n = Offset(-d.y / len, d.x / len) * (cs * 0.18f)
                        val brown = Color(0xFF9C6B30)
                        drawLine(brown, a + n, b + n, cs * 0.08f, StrokeCap.Round); drawLine(brown, a - n, b - n, cs * 0.08f, StrokeCap.Round)
                        val rungs = (len / (cs * 0.45f)).toInt()
                        for (k in 1 until rungs) { val t = a + d * (k / rungs.toFloat()); drawLine(brown, t + n, t - n, cs * 0.06f) }
                    }
                    for ((head, tail) in game.snakes) {
                        val a = squareCentre(head, cs); val b = squareCentre(tail, cs)
                        val d = b - a; val len = d.getDistance(); val n = Offset(-d.y / len, d.x / len) * (cs * 0.55f)
                        val path = Path().apply {
                            moveTo(a.x, a.y)
                            cubicTo((a + d * 0.33f + n).x, (a + d * 0.33f + n).y, (a + d * 0.66f - n).x, (a + d * 0.66f - n).y, b.x, b.y)
                        }
                        drawPath(path, Color(0xFF1F8A4C), style = Stroke(cs * 0.2f, cap = StrokeCap.Round))
                        drawPath(path, Color(0xFF7BD389), style = Stroke(cs * 0.07f, cap = StrokeCap.Round))
                        drawCircle(Color(0xFF1F8A4C), cs * 0.17f, a)
                        drawCircle(Color.White, cs * 0.04f, a + Offset(-cs * 0.06f, -cs * 0.04f)); drawCircle(Color.White, cs * 0.04f, a + Offset(cs * 0.06f, -cs * 0.04f))
                    }
                }
                // Square numbers.
                val cellDp = side / 10
                for (sq in listOf(1, 100)) {
                    val c = squareCentre(sq, 1f)
                    Text(if (sq == 100) "🛕" else "1", fontSize = 12.sp, color = Color(0xFF6B5A44),
                        modifier = Modifier.offset(cellDp * (c.x - 0.45f), cellDp * (c.y - 0.48f)))
                }
                // Tokens.
                for (k in 0..1) {
                    val v = walk[k].value
                    val lo = v.toInt().coerceAtLeast(0); val f = v - lo
                    val c0 = if (lo == 0) Offset(-0.3f, 9.5f) else squareCentre(lo, 1f)
                    val c1 = squareCentre(lo + 1, 1f)
                    val c = c0 + (c1 - c0) * f
                    val hop = kotlin.math.sin(f * Math.PI).toFloat() * 0.35f
                    val tok = cellDp * 0.56f
                    Box(
                        Modifier.offset { IntOffset(((c.x * cellDp.toPx()) - tok.toPx() / 2 + (if (k == 0) -4 else 4) * density).roundToInt(), ((c.y - hop) * cellDp.toPx() - tok.toPx() / 2).roundToInt()) }
                            .size(tok).shadow(4.dp, CircleShape).clip(CircleShape).background(colours[k]),
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                Modifier.size(64.dp).graphicsLayer { rotationZ = dieSpin.value * 360f }.shadow(6.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp)).background(Color.White).jellyClick(!busy && game.turn == 0) { tapRoll() },
                contentAlignment = Alignment.Center,
            ) { DieFace(face) }
            Column {
                for (k in 0..1) Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(colours[k]))
                    Spacer(Modifier.width(6.dp))
                    Text((if (k == 0) "You" else "Phone") + " · square ${walk[k].value.roundToInt()}", color = p.ink)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        PrimaryButton(if (busy) "Wait…" else "Roll", s.accent, enabled = !busy && game.turn == 0) { tapRoll() }
    }
}

@Composable
fun DieFace(n: Int) {
    val spots = when (n) {
        1 -> listOf(1 to 1); 2 -> listOf(0 to 0, 2 to 2); 3 -> listOf(0 to 0, 1 to 1, 2 to 2)
        4 -> listOf(0 to 0, 0 to 2, 2 to 0, 2 to 2); 5 -> listOf(0 to 0, 0 to 2, 1 to 1, 2 to 0, 2 to 2)
        else -> listOf(0 to 0, 0 to 2, 1 to 0, 1 to 2, 2 to 0, 2 to 2)
    }
    Canvas(Modifier.size(44.dp)) {
        val c = size.width / 3
        for ((r, col) in spots) drawCircle(if (n == 1) Color(0xFFD62839) else Color(0xFF1C1B22), c * 0.28f, Offset((col + 0.5f) * c, (r + 0.5f) * c))
    }
}
