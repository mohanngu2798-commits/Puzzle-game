package app.pudhir.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Jelly press: squashes while held (a little wider than tall, like pressing water)
 * and wobbles back with a soft overshoot when released.
 */
fun Modifier.bouncyClick(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val sx = remember { Animatable(1f) }
    val sy = remember { Animatable(1f) }
    LaunchedEffect(pressed, enabled) {
        if (!enabled) { sx.snapTo(1f); sy.snapTo(1f); return@LaunchedEffect }
        if (pressed) coroutineScope {
            launch { sx.animateTo(0.95f, spring(dampingRatio = 0.6f, stiffness = 900f)) }
            sy.animateTo(0.89f, spring(dampingRatio = 0.6f, stiffness = 900f))
        } else coroutineScope {
            launch { sx.animateTo(1f, spring(dampingRatio = 0.28f, stiffness = 420f)) }
            sy.animateTo(1f, spring(dampingRatio = 0.24f, stiffness = 380f))
        }
    }
    this
        .graphicsLayer { scaleX = sx.value; scaleY = sy.value }
        .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
}

/**
 * Makes a whole screen feel liquid: every touch sends out a soft ripple from the finger,
 * and the screen gives a tiny jelly wobble. It only watches touches and never consumes them,
 * so every button and game board underneath works exactly as before.
 */
fun Modifier.liquidTouch(tint: Color): Modifier = composed {
    val wobble = remember { Animatable(0f) }
    val ripples = remember { androidx.compose.runtime.mutableStateListOf<Ripple>() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var pivot by remember { mutableStateOf(Offset.Zero) }
    this
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val e = awaitPointerEvent(PointerEventPass.Initial)
                    for (ch in e.changes) if (ch.changedToDownIgnoreConsumed()) {
                        val at = ch.position
                        pivot = at
                        val r = Ripple(at, Animatable(0f))
                        ripples += r
                        if (ripples.size > 4) ripples.removeAt(0)
                        scope.launch {
                            r.t.animateTo(1f, tween(620, easing = androidx.compose.animation.core.FastOutSlowInEasing))
                            ripples.remove(r)
                        }
                        scope.launch {
                            wobble.snapTo(1f)
                            wobble.animateTo(0f, spring(dampingRatio = 0.22f, stiffness = 260f))
                        }
                    }
                }
            }
        }
        .graphicsLayer {
            // A gentle squeeze toward the finger that settles with a few soft wobbles.
            val k = wobble.value * 0.006f
            scaleX = 1f - k
            scaleY = 1f - k * 0.6f
            if (size.width > 0f) transformOrigin = TransformOrigin(
                (pivot.x / size.width).coerceIn(0f, 1f), (pivot.y / size.height).coerceIn(0f, 1f),
            )
        }
        .drawWithContent {
            drawContent()
            for (r in ripples) {
                val t = r.t.value
                val radius = 18.dp.toPx() + t * 90.dp.toPx()
                val a = (1f - t)
                drawCircle(tint.copy(alpha = 0.16f * a), radius * 0.55f, r.at)
                drawCircle(tint.copy(alpha = 0.28f * a), radius, r.at, style = Stroke(width = (2.5f * (1f - t) + 0.5f).dp.toPx()))
                drawCircle(tint.copy(alpha = 0.14f * a), radius * 0.7f, r.at, style = Stroke(width = 1.5.dp.toPx()))
            }
        }
}

class Ripple(val at: Offset, val t: Animatable<Float, androidx.compose.animation.core.AnimationVector1D>)

/** Horizontal shake whenever [trigger] changes (used for wrong moves). */
fun Modifier.shake(trigger: Int): Modifier = composed {
    val x = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        for (v in listOf(14f, -12f, 9f, -6f, 3f, 0f)) x.animateTo(v, tween(45))
    }
    this.graphicsLayer { translationX = x.value }
}

/** Pops in with a bouncy scale the first time it appears. */
fun Modifier.popIn(delayMs: Int = 0): Modifier = composed {
    // Previews and screenshots show the settled look.
    if (androidx.compose.ui.platform.LocalInspectionMode.current) return@composed this
    val s = remember { Animatable(0.6f) }
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMs.toLong())
        kotlinx.coroutines.coroutineScope {
            launch { a.animateTo(1f, tween(180)) }
            s.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 380f))
        }
    }
    this.graphicsLayer { scaleX = s.value; scaleY = s.value; alpha = a.value }
}

@Composable
fun Haptics(): (Boolean) -> Unit {
    val h = LocalHapticFeedback.current
    return { strong -> h.performHapticFeedback(if (strong) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove) }
}

@Composable
fun PrimaryButton(text: String, colour: Color, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    // A chunky 3D button: a darker base under a raised face, like a key press.
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val drop by animateFloatAsState(if (pressed) 4f else 0f, spring(stiffness = Spring.StiffnessHigh), label = "drop")
    val face = if (enabled) colour else colour.copy(alpha = 0.4f)
    Box(
        modifier
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(darken(face, 0.22f))
            .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .graphicsLayer { translationY = drop * density }
                .clip(RoundedCornerShape(16.dp))
                .background(face),
            contentAlignment = Alignment.Center,
        ) {
            Text(text.uppercase(), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, letterSpacing = 0.8.sp)
        }
    }
}

@Composable
fun SoftButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(
        modifier
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, p.line, RoundedCornerShape(16.dp))
            .background(p.surface)
            .bouncyClick(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text.uppercase(), color = p.ink, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, letterSpacing = 0.8.sp) }
}

/** Round icon action under the board: undo, hint, restart... */
@Composable
fun ActionChip(icon: ImageVector, label: String, enabled: Boolean = true, badge: String? = null, onClick: () -> Unit) {
    val p = LocalPalette.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp)) {
        Box {
            Box(
                Modifier
                    .size(52.dp)
                    .shadow(if (enabled) 3.dp else 0.dp, CircleShape)
                    .clip(CircleShape)
                    .background(p.surface)
                    .bouncyClick(enabled, onClick),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, label, tint = if (enabled) p.ink else p.muted.copy(alpha = 0.5f)) }
            if (badge != null) Box(
                Modifier.align(Alignment.TopEnd).size(20.dp).clip(CircleShape).background(Color(0xFFF26B3A)),
                contentAlignment = Alignment.Center,
            ) { Text(badge, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = androidx.compose.material3.MaterialTheme.typography.labelMedium, color = p.muted)
    }
}

@Composable
fun ActionRow(content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) { content() }
}

fun darken(c: Color, f: Float) = Color(c.red * (1 - f), c.green * (1 - f), c.blue * (1 - f), c.alpha)
fun lighten(c: Color, f: Float) = Color(c.red + (1 - c.red) * f, c.green + (1 - c.green) * f, c.blue + (1 - c.blue) * f, c.alpha)

fun formatTime(s: Int): String = "%d:%02d".format(s / 60, s % 60)

/** Celebration card with popping stars and a confetti burst. */
@Composable
fun WinOverlay(
    title: String,
    stars: Int,
    detail: String,
    accent: Color,
    nextLabel: String,
    onNext: () -> Unit,
    onHome: () -> Unit,
) {
    val p = LocalPalette.current
    val haptic = Haptics()
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
        Confetti()
        Column(
            Modifier
                .padding(28.dp)
                .popIn()
                .shadow(16.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(p.surface)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
                for (i in 0 until 3) {
                    val on = i < stars
                    val big = i == 1
                    Icon(
                        Icons.Rounded.Star, null,
                        tint = if (on) p.gold else p.line,
                        modifier = Modifier.size(if (big) 64.dp else 48.dp).popIn(250 + i * 180),
                    )
                }
            }
            LaunchedEffect(Unit) { for (i in 0 until stars) { kotlinx.coroutines.delay(250L + i * 180); haptic(false) } }
            Spacer(Modifier.height(12.dp))
            Text(title, style = androidx.compose.material3.MaterialTheme.typography.headlineSmall, color = p.ink, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(detail, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, color = p.muted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            PrimaryButton(nextLabel, accent, Modifier.fillMaxWidth(), onClick = onNext)
            Spacer(Modifier.height(10.dp))
            SoftButton("Home", Modifier.fillMaxWidth(), onClick = onHome)
        }
    }
}

@Composable
fun Confetti() {
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) { t.animateTo(1f, tween(1600, easing = LinearEasing)) }
    val bits = remember {
        val r = Random(7)
        List(70) { Triple(r.nextFloat() * 6.283f, 0.45f + r.nextFloat() * 0.6f, PieceColours[r.nextInt(6)]) }
    }
    Canvas(Modifier.fillMaxSize()) {
        val v = t.value
        if (v >= 1f) return@Canvas
        val cx = size.width / 2; val cy = size.height * 0.42f
        for ((i, b) in bits.withIndex()) {
            val (ang, speed, col) = b
            val d = speed * size.minDimension * 0.75f * (1 - (1 - v) * (1 - v))
            val x = cx + cos(ang) * d
            val y = cy + sin(ang) * d + v * v * size.height * 0.35f
            val s = (if (i % 2 == 0) 9f else 6f) * density
            drawRect(col.copy(alpha = 1f - v), topLeft = Offset(x, y), size = androidx.compose.ui.geometry.Size(s, s * 1.6f))
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    val p = LocalPalette.current
    Text(text, style = androidx.compose.material3.MaterialTheme.typography.titleLarge, color = p.ink, modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 12.dp))
}

@Composable
fun Pill(text: String, colour: Color, textColour: Color = Color.White, height: Dp = 26.dp) {
    Box(Modifier.height(height).clip(RoundedCornerShape(50)).background(colour).padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
        Text(text, color = textColour, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
