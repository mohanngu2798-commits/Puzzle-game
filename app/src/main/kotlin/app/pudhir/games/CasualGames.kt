package app.pudhir.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pudhir.core.CarromFlick
import app.pudhir.core.Difficulty
import app.pudhir.core.ExpressSnake
import app.pudhir.core.HoliPop
import app.pudhir.core.FruitMerge
import app.pudhir.core.RangoliPaint
import app.pudhir.ui.ActionChip
import app.pudhir.ui.ActionRow
import app.pudhir.ui.LocalPalette
import app.pudhir.ui.darken
import app.pudhir.ui.jellyClick
import app.pudhir.ui.lighten
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt

/** A box of the given height/width ratio, as large as fits. */
@Composable
private fun Fit(ratio: Float, modifier: Modifier, content: @Composable (Dp) -> Unit) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val w: Dp = minOf(maxWidth, maxHeight / ratio)
        Box(Modifier.size(w, w * ratio)) { content(w) }
    }
}

/** Runs [step] every frame with the elapsed seconds, except in previews. */
@Composable
private fun FrameLoop(running: Boolean, step: (Float) -> Unit) {
    if (LocalInspectionMode.current) return
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        var prev = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            step(((now - prev) / 1e9f).coerceAtMost(0.04f)); prev = now
        }
    }
}

/** The fruit ladder, smallest to biggest: name, skin colour, darker shade. */
private data class Fruit(val name: String, val skin: Color, val shade: Color)
private val FRUITS = listOf(
    Fruit("Jamun", Color(0xFF6B3FA0), Color(0xFF3E2163)),
    Fruit("Lime", Color(0xFF9ACD32), Color(0xFF5E8A1E)),
    Fruit("Guava", Color(0xFFB5D96A), Color(0xFF7FA23E)),
    Fruit("Chikoo", Color(0xFFB07A4F), Color(0xFF7A5030)),
    Fruit("Orange", Color(0xFFFF9A2E), Color(0xFFD9681A)),
    Fruit("Pomegranate", Color(0xFFE0394E), Color(0xFFA51F35)),
    Fruit("Mango", Color(0xFFFFC233), Color(0xFFF08A1C)),
    Fruit("Coconut", Color(0xFF8A5A3B), Color(0xFF5A3822)),
    Fruit("Papaya", Color(0xFFFFA347), Color(0xFF8FBF4A)),
    Fruit("Jackfruit", Color(0xFFC9C34A), Color(0xFF8E8A2A)),
    Fruit("Watermelon", Color(0xFF3FA34D), Color(0xFF1F6B2C)),
)

/** Draws a cute fruit with a face. [cheer] 0..1 makes it beam (just merged). */
private fun DrawScope.fruit(c: Offset, r: Float, kind: Int, cheer: Float = 0f, squash: Float = 0f) {
    val f = FRUITS[kind.coerceIn(0, FRUITS.size - 1)]
    val ink = Color(0xFF4A2A1E)
    val sx = 1f + squash; val sy = 1f - squash
    val body = Size(r * 2 * sx, r * 2 * sy)
    val tl = Offset(c.x - r * sx, c.y - r * sy + r * squash)
    val centre = Offset(c.x, c.y + r * squash)
    // Leaf or crown first, so the body overlaps it.
    when (kind) {
        2, 4, 6, 8 -> {
            drawLine(Color(0xFF6B4A2A), centre + Offset(0f, -r * 0.9f), centre + Offset(r * 0.1f, -r * 1.15f), r * 0.1f, StrokeCap.Round)
            drawOval(Color(0xFF4FAE4A), centre + Offset(r * 0.05f, -r * 1.25f), Size(r * 0.55f, r * 0.28f))
        }
        5 -> for (k in -1..1) drawLine(f.shade, centre + Offset(k * r * 0.12f, -r * 0.85f), centre + Offset(k * r * 0.22f, -r * 1.12f), r * 0.12f, StrokeCap.Round)
    }
    drawOval(f.shade, tl, body)
    drawOval(Brush.radialGradient(listOf(lighten(f.skin, 0.4f), f.skin, f.shade), centre - Offset(r * 0.35f, r * 0.4f), r * 1.5f), tl + Offset(r * 0.04f, r * 0.04f), Size(body.width - r * 0.08f, body.height - r * 0.08f))
    // Fruit details.
    when (kind) {
        2 -> for (k in 0 until 6) drawCircle(f.shade.copy(alpha = 0.5f), r * 0.05f, centre + Offset(kotlin.math.cos(k * 1.1f) * r * 0.6f, kotlin.math.sin(k * 1.1f) * r * 0.55f))
        7 -> for (k in 0 until 7) { val a = k * 0.9f; drawLine(Color(0x55FFFFFF), centre + Offset(kotlin.math.cos(a) * r * 0.3f, kotlin.math.sin(a) * r * 0.3f), centre + Offset(kotlin.math.cos(a) * r * 0.75f, kotlin.math.sin(a) * r * 0.75f), r * 0.04f) }
        9 -> for (yy in -3..3) for (xx in -3..3) if (xx * xx + yy * yy < 11) drawCircle(f.shade.copy(alpha = 0.45f), r * 0.045f, centre + Offset(xx * r * 0.24f + (yy % 2) * r * 0.12f, yy * r * 0.24f))
        10 -> for (k in -3..3) drawArc(f.shade, -90f + k * 2f, 180f, false, centre - Offset(r * (0.25f + abs(k) * 0.02f) * (4 - abs(k)) / 4f, r * 0.98f), Size(r * 0.5f * (4 - abs(k)) / 2f, r * 1.96f), style = Stroke(r * 0.1f))
        8 -> drawOval(Color(0x3384C04A), tl + Offset(r * 0.1f, r * 0.1f), Size(body.width * 0.45f, body.height * 0.8f))
        else -> Unit
    }
    drawOval(ink.copy(alpha = 0.85f), tl, body, style = Stroke(r * 0.06f))
    // Shine.
    drawOval(Color.White.copy(alpha = 0.55f), centre + Offset(-r * 0.62f, -r * 0.62f), Size(r * 0.38f, r * 0.24f))
    // Face.
    val face = centre + Offset(0f, r * 0.12f)
    val eye = r * 0.11f
    for (k in listOf(-1, 1)) {
        val e = face + Offset(k * r * 0.3f, -r * 0.05f)
        if (cheer > 0.3f) drawArc(ink, 200f, 140f, false, e - Offset(eye, eye * 0.6f), Size(eye * 2, eye * 1.4f), style = Stroke(r * 0.06f, cap = StrokeCap.Round))
        else { drawCircle(ink, eye, e); drawCircle(Color.White, eye * 0.38f, e + Offset(-eye * 0.3f, -eye * 0.35f)) }
        drawOval(Color(0x66FF6F8A), face + Offset(k * r * 0.48f - r * 0.1f, r * 0.08f), Size(r * 0.2f, r * 0.1f))
    }
    if (cheer > 0.3f) drawArc(Color(0xFFB8324A), 0f, 180f, true, face + Offset(-r * 0.14f, r * 0.04f), Size(r * 0.28f, r * 0.24f))
    else drawArc(ink, 20f, 140f, false, face + Offset(-r * 0.1f, -r * 0.02f), Size(r * 0.2f, r * 0.14f), style = Stroke(r * 0.05f, cap = StrokeCap.Round))
}

@Composable
fun FruitBazaarGame(s: PlaySession) {
    val p = LocalPalette.current
    val game = remember { FruitMerge(s.seed, s.difficulty) }
    var frame by remember { mutableIntStateOf(0) }
    var aim by remember { mutableFloatStateOf(0.5f) }
    var cool by remember { mutableFloatStateOf(0f) }
    var over by remember { mutableStateOf(false) }
    val pops = remember { mutableStateListOf<Triple<Offset, Int, Float>>() }
    FrameLoop(!over) { dt ->
        game.step(dt); cool -= dt; frame++
        for (i in pops.indices.reversed()) { val (o, k, t) = pops[i]; if (t + dt > 0.5f) pops.removeAt(i) else pops[i] = Triple(o, k, t + dt) }
        if (game.merges.isNotEmpty()) {
            s.haptic(game.merges.any { it.third >= 5 })
            for ((x, y, k) in game.merges) pops += Triple(Offset(x, y), k, 0f)
            game.merges.clear()
        }
        if (!over && game.isWon()) { over = true; s.haptic(true); s.onSolved(0) }
        else if (!over && game.isOver()) { over = true; s.onGameOver(game.score, "The basket overflowed!") }
    }
    fun drop() { if (cool <= 0f && !over) { game.drop(aim); cool = 0.5f; s.haptic(false) } }
    Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        frame.let {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${game.score}", color = p.ink, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
                Text("  Grow a ${FRUITS[game.goal].name}!", color = p.muted, modifier = Modifier.weight(1f))
                Text("Next", color = p.muted, fontSize = 12.sp)
                Canvas(Modifier.size(34.dp)) { fruit(center, size.width * 0.36f, game.next) }
            }
        }
        // The fruit ladder, lit up to the biggest fruit grown so far.
        Canvas(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(30.dp)) {
            frame.let { }
            val n = game.goal + 1; val step = size.width / n
            for (k in 0 until n) {
                val c = Offset(step * (k + 0.5f), size.height / 2)
                if (k > game.best && k > 4) drawCircle(p.line, size.height * 0.38f, c) else fruit(c, size.height * (0.24f + 0.02f * minOf(k, 7)), k)
            }
        }
        Fit(game.height, Modifier.fillMaxWidth().weight(1f).padding(vertical = 6.dp)) {
            Canvas(
                Modifier.fillMaxSize().shadow(8.dp, RoundedCornerShape(26.dp)).clip(RoundedCornerShape(26.dp)).background(Color(0xFFF6E3B8))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { aim = (it.x / size.width).coerceIn(0f, 1f) },
                            onDrag = { ch, _ -> aim = (ch.position.x / size.width).coerceIn(0f, 1f) },
                            onDragEnd = { drop() },
                        )
                    }
                    .pointerInput(Unit) { detectTapGestures { aim = (it.x / size.width).coerceIn(0f, 1f); drop() } },
            ) {
                frame.let { }
                val u = size.width
                // Basket: warm wood with woven slats.
                drawRect(Brush.verticalGradient(listOf(Color(0xFFFFF4DA), Color(0xFFF1D79E))))
                for (k in 1 until 7) drawLine(Color(0x22A0702A), Offset(0f, size.height * k / 7f), Offset(u, size.height * k / 7f), 3f)
                drawRect(Color(0xFFC98E4A), Offset(0f, size.height - u * 0.025f), Size(u, u * 0.025f))
                drawLine(Color(0xAAE5484D), Offset(0f, game.line * u), Offset(u, game.line * u), 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 10f)))
                val r = game.radius(game.current) * u
                val ax = aim.coerceIn(r / u, 1 - r / u) * u
                drawLine(Color(0x55A0702A), Offset(ax, r * 2), Offset(ax, size.height), 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 10f)))
                if (cool <= 0f) fruit(Offset(ax, r + 8f), r, game.current)
                for (b in game.balls) {
                    val pop = (b.age / 0.18f).coerceAtMost(1f)
                    val wob = if (b.age < 0.5f) kotlin.math.sin(b.age * 30f) * 0.08f * (1 - b.age / 0.5f) else 0f
                    fruit(Offset(b.x * u, b.y * u), b.r * u * (0.6f + 0.4f * pop), b.kind, cheer = if (b.age < 0.8f && b.kind > 0) 1f else 0f, squash = wob)
                }
                for ((o, k, t) in pops) {
                    val c = Offset(o.x * u, o.y * u)
                    for (i in 0 until 10) {
                        val a = i * 0.628f
                        drawCircle(FRUITS[k].skin.copy(alpha = 1 - t / 0.5f), u * 0.012f, c + Offset(kotlin.math.cos(a), kotlin.math.sin(a)) * (game.radius(k) * u + t * u * 0.25f))
                    }
                }
            }
        }
        Text("Drag and let go to drop. Two of the same fruit merge into a bigger one.", color = p.muted, fontSize = 13.sp)
    }
}

private val HOLI = listOf(Color(0xFFE5484D), Color(0xFFF5B83D), Color(0xFF2BA36B), Color(0xFF2F6FE0), Color(0xFFE05A8A), Color(0xFF9B5DE5))

@Composable
fun HoliPopGame(s: PlaySession) {
    val p = LocalPalette.current
    val game = remember { HoliPop(s.seed, s.difficulty) }
    var version by remember { mutableIntStateOf(0) }
    var angle by remember { mutableStateOf<Float?>(null) }
    var done by remember { mutableStateOf(false) }
    val r = game.r
    val sy = game.rows * r * sqrt(3f) + r * 2.5f
    val ratio = sy + r * 2
    Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        version.let { Text("Shots ${game.shots} · score ${game.score}", color = p.muted) }
        Fit(ratio, Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp)) {
            Canvas(
                Modifier.fillMaxSize().shadow(6.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp)).background(p.surface)
                    .pointerInput(Unit) {
                        fun aimAt(o: Offset) {
                            val u = size.width
                            val a = atan2(o.y / u - sy, o.x / u - 0.5f)
                            angle = a.coerceIn(-PI.toFloat() + 0.18f, -0.18f)
                        }
                        detectDragGestures(
                            onDragStart = { aimAt(it) },
                            onDrag = { ch, _ -> aimAt(ch.position) },
                            onDragEnd = {
                                val a = angle ?: return@detectDragGestures
                                if (done) return@detectDragGestures
                                val (popped, dropped) = game.fire(0.5f, sy, a)
                                angle = null; version++
                                s.haptic(popped.isNotEmpty())
                                if (game.isWon()) { done = true; s.onSolved(0) } else if (game.isLost()) { done = true; s.onGameOver(game.score, "The colours reached the bottom!") }
                                if (dropped.size > 4) s.haptic(true)
                            },
                        )
                    },
            ) {
                version.let { }
                val u = size.width
                drawRect(Brush.verticalGradient(listOf(lighten(HOLI[3], if (p.dark) 0.05f else 0.85f), p.surface)))
                drawLine(p.bad.copy(alpha = 0.4f), Offset(0f, (game.rows - 2) * r * sqrt(3f) * u), Offset(u, (game.rows - 2) * r * sqrt(3f) * u), 2f)
                for (rr in 0 until game.rows) for (c in 0 until game.cols) {
                    val v = game.grid[rr][c]; if (v < 0) continue
                    val (x, y) = game.centre(rr, c)
                    balloon(Offset(x * u, y * u), r * u * 0.95f, HOLI[v])
                }
                angle?.let { a ->
                    val (pts, _) = game.trace(0.5f, sy, a)
                    for (i in 1 until pts.size) drawLine(p.muted.copy(alpha = 0.6f), Offset(pts[i - 1].first * u, pts[i - 1].second * u), Offset(pts[i].first * u, pts[i].second * u), 4f, StrokeCap.Round, PathEffect.dashPathEffect(floatArrayOf(8f, 12f)))
                }
                balloon(Offset(0.5f * u, sy * u), r * u, HOLI[game.loaded])
                balloon(Offset(0.68f * u, sy * u + r * u * 0.3f), r * u * 0.7f, HOLI[game.next])
            }
        }
        ActionRow { ActionChip(Icons.Rounded.SwapHoriz, "Swap colour", !done) { game.swap(); version++ } }
    }
}

private fun DrawScope.balloon(c: Offset, r: Float, col: Color) {
    drawCircle(darken(col, 0.2f), r, c)
    drawCircle(Brush.radialGradient(listOf(lighten(col, 0.5f), col), c - Offset(r * 0.35f, r * 0.35f), r), r * 0.9f, c)
    drawCircle(Color.White.copy(alpha = 0.6f), r * 0.2f, c - Offset(r * 0.35f, r * 0.4f))
}

@Composable
fun ExpressSnakeGame(s: PlaySession) {
    val p = LocalPalette.current
    val game = remember { ExpressSnake(s.seed) }
    var version by remember { mutableIntStateOf(0) }
    var acc by remember { mutableFloatStateOf(0f) }
    val speed = when (s.difficulty) { Difficulty.Easy -> 0.2f; Difficulty.Medium -> 0.15f; Difficulty.Hard -> 0.11f; Difficulty.Expert -> 0.085f }
    FrameLoop(!game.dead) { dt ->
        acc += dt
        val gap = (speed - game.score / 10 * 0.002f).coerceAtLeast(0.06f)
        if (acc >= gap) {
            acc = 0f
            if (game.tick()) s.haptic(false)
            version++
            if (game.dead) { s.haptic(true); s.onGameOver(game.score, "The express crashed!") }
        }
    }
    val measurer = rememberTextMeasurer()
    Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        version.let { Text("Passengers ${game.score / 10} · carriages ${game.body.size - 1}", color = p.muted) }
        Fit(game.h / game.w.toFloat(), Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp)) {
            Canvas(
                Modifier.fillMaxSize().shadow(6.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp)).background(Color(0xFF8BC77A))
                    .pointerInput(Unit) {
                        var sum = Offset.Zero
                        detectDragGestures(onDragStart = { sum = Offset.Zero }) { ch, d ->
                            sum += d
                            if (hypot(sum.x, sum.y) > 24f) {
                                game.turn(if (abs(sum.x) > abs(sum.y)) (if (sum.x > 0) 1 else 3) else (if (sum.y > 0) 2 else 0))
                                sum = Offset.Zero
                            }
                            ch.consume()
                        }
                    },
            ) {
                version.let { }
                val cs = size.width / game.w
                for (r in 0 until game.h) for (c in 0 until game.w) if ((r + c) % 2 == 0) drawRect(Color(0xFF93CF82), Offset(c * cs, r * cs), Size(cs, cs))
                val f = game.food
                val l = measurer.measure("🙋", TextStyle(fontSize = (cs / density * 0.75f).sp))
                drawText(l, topLeft = Offset((f % game.w) * cs + cs / 2 - l.size.width / 2, (f / game.w) * cs + cs / 2 - l.size.height / 2))
                game.body.forEachIndexed { i, q ->
                    val o = Offset((q % game.w) * cs, (q / game.w) * cs)
                    val col = if (i == 0) Color(0xFFE5484D) else if (i % 2 == 0) Color(0xFF2F6FE0) else Color(0xFFF5B83D)
                    drawRoundRect(darken(col, 0.2f), o + Offset(cs * 0.06f, cs * 0.1f), Size(cs * 0.88f, cs * 0.86f), CornerRadius(cs * 0.25f))
                    drawRoundRect(col, o + Offset(cs * 0.06f, cs * 0.04f), Size(cs * 0.88f, cs * 0.82f), CornerRadius(cs * 0.25f))
                    if (i == 0) {
                        drawCircle(Color(0xFFFFF2B0), cs * 0.16f, o + Offset(cs / 2, cs / 2))
                    } else {
                        drawRoundRect(Color(0xFFCFEFFF), o + Offset(cs * 0.25f, cs * 0.25f), Size(cs * 0.5f, cs * 0.36f), CornerRadius(cs * 0.08f))
                    }
                }
            }
        }
        Text("Swipe to steer. Each passenger adds a carriage. Don't hit the edge or your own train.", color = p.muted, fontSize = 13.sp)
    }
}

private val RANGOLI = listOf(Color(0xFFE5484D), Color(0xFFF5B83D), Color(0xFF2BA36B), Color(0xFF2F6FE0), Color(0xFFE05A8A), Color(0xFF9B5DE5))

@Composable
fun RangoliPaintGame(s: PlaySession) {
    val p = LocalPalette.current
    val game = remember { RangoliPaint(s.seed, s.difficulty) }
    var version by remember { mutableIntStateOf(0) }
    var brush by remember { mutableIntStateOf(0) }
    var hints by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    val measurer = rememberTextMeasurer()
    fun paint(i: Int, c: Int) {
        if (done || game.painted[i] >= 0) return
        if (game.paint(i, c)) { version++; s.haptic(false); if (game.isDone()) { done = true; s.haptic(true); s.onSolved(hints) } } else s.haptic(true)
    }
    Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        version.let { Text("${game.painted.count { it >= 0 }} of ${game.regions.size} painted", color = p.muted) }
        Fit(1f, Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp)) {
            Canvas(
                Modifier.fillMaxSize().pointerInput(Unit) {
                    detectTapGestures { o ->
                        val half = size.width / 2f
                        game.regionAt((o.x - half) / half, (o.y - half) / half)?.let { paint(it, brush) }
                    }
                },
            ) {
                version.let { }
                val half = size.width / 2; val ringW = half / game.rings
                val centre = Offset(half, half)
                game.regions.forEachIndexed { i, rg ->
                    val painted = game.painted[i] >= 0
                    val col = if (painted) RANGOLI[rg.colour] else if (rg.colour == brush) lighten(RANGOLI[brush], 0.75f) else p.surfaceAlt
                    if (rg.ring == 0) {
                        drawCircle(col, ringW, centre)
                        drawCircle(p.line, ringW, centre, style = Stroke(3f))
                    } else {
                        val mid = ringW * (rg.ring + 0.5f)
                        val sweep = 360f / rg.sectors
                        val start = -90f + rg.sector * sweep
                        drawArc(col, start, sweep, false, centre - Offset(mid, mid), Size(mid * 2, mid * 2), style = Stroke(ringW * 0.96f))
                        // Petal outline.
                        drawArc(p.line, start, sweep, true, centre - Offset(ringW * (rg.ring + 1), ringW * (rg.ring + 1)), Size(ringW * (rg.ring + 1) * 2, ringW * (rg.ring + 1) * 2), style = Stroke(2f))
                    }
                    if (!painted) {
                        val mid = if (rg.ring == 0) 0f else ringW * (rg.ring + 0.5f)
                        val a = Math.toRadians((-90f + (rg.sector + 0.5f) * 360f / rg.sectors).toDouble())
                        val at = centre + Offset((kotlin.math.cos(a) * mid).toFloat(), (kotlin.math.sin(a) * mid).toFloat())
                        val l = measurer.measure("${rg.colour + 1}", TextStyle(fontSize = (ringW / density * 0.3f).coerceIn(9f, 18f).sp, fontWeight = FontWeight.Bold, color = p.muted))
                        drawText(l, topLeft = at - Offset(l.size.width / 2f, l.size.height / 2f))
                    }
                }
                drawCircle(p.line, half, centre, style = Stroke(3f))
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            RANGOLI.forEachIndexed { i, c ->
                val left = version.let { game.regions.indices.count { game.regions[it].colour == i && game.painted[it] < 0 } }
                Box(
                    Modifier.size(if (brush == i) 50.dp else 42.dp).jellyClick { brush = i }.clip(CircleShape).background(c),
                    contentAlignment = Alignment.Center,
                ) { Text(if (left == 0) "✓" else "${i + 1}", color = Color.White, fontWeight = FontWeight.ExtraBold) }
            }
        }
        ActionRow {
            ActionChip(Icons.Rounded.Lightbulb, "Hint", !done) {
                s.useHint {
                    hints++
                    val i = game.regions.indices.firstOrNull { game.painted[it] < 0 } ?: return@useHint
                    brush = game.regions[i].colour; paint(i, brush)
                }
            }
        }
    }
}

@Composable
fun CarromFlickGame(s: PlaySession) {
    val p = LocalPalette.current
    val game = remember { CarromFlick(s.seed, s.difficulty) }
    var version by remember { mutableIntStateOf(0) }
    var strikerX by remember { mutableFloatStateOf(0.5f) }
    var pull by remember { mutableStateOf<Offset?>(null) }
    var done by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    FrameLoop(running) { dt ->
        val got = game.step(dt)
        if (got.isNotEmpty()) s.haptic(true)
        version++
        if (!game.moving) {
            running = false
            game.striker.x = strikerX; game.striker.y = 0.82f
            if (game.isWon()) { done = true; s.onSolved(0) } else if (game.isLost()) { done = true; s.onGameOver(null, "Out of shots with ${game.coins.size} coins left") }
        }
    }
    Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        version.let { Text("${game.pocketed} of ${game.total} pocketed · ${game.shotsLeft} shots left", color = p.muted) }
        Fit(1f, Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp)) {
            Canvas(
                Modifier.fillMaxSize().shadow(8.dp, RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp)).background(Color(0xFF7A4B2A))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { pull = Offset.Zero },
                            onDrag = { ch, d -> pull = (pull ?: Offset.Zero) + d / size.width.toFloat(); ch.consume() },
                            onDragEnd = {
                                val v = pull; pull = null
                                if (v == null || running || done || hypot(v.x, v.y) < 0.03f) return@detectDragGestures
                                // Pull back like a slingshot: the striker flies the other way.
                                val a = atan2(-v.y, -v.x)
                                game.shoot(strikerX, a, (hypot(v.x, v.y) * 7f).coerceAtMost(3.2f))
                                running = true; s.haptic(false)
                            },
                        )
                    },
            ) {
                version.let { }
                val u = size.width; val m = u * 0.04f
                drawRoundRect(Color(0xFFF2D9A8), Offset(m, m), Size(u - 2 * m, u - 2 * m), CornerRadius(8f))
                for ((px, py) in CarromFlick.POCKETS) drawCircle(Color(0xFF2B1A10), game.pocketR * u, Offset(px * u, py * u))
                drawCircle(Color(0xFFB8324A), u * 0.13f, Offset(u / 2, u / 2), style = Stroke(3f))
                drawCircle(Color(0xFFB8324A), u * 0.04f, Offset(u / 2, u / 2), style = Stroke(2f))
                drawLine(Color(0xFF6B3A20), Offset(u * 0.2f, u * 0.82f), Offset(u * 0.8f, u * 0.82f), 3f)
                for (c in game.coins) {
                    val col = when (c.kind) { 0 -> Color(0xFFD7263D); 1 -> Color(0xFFFFF6E0); else -> Color(0xFF2B2440) }
                    drawCircle(darken(col, 0.3f), c.r * u, Offset(c.x * u, c.y * u))
                    drawCircle(col, c.r * u * 0.85f, Offset(c.x * u, c.y * u))
                    drawCircle(darken(col, 0.15f), c.r * u * 0.5f, Offset(c.x * u, c.y * u), style = Stroke(2f))
                }
                val st = if (running) Offset(game.striker.x * u, game.striker.y * u) else Offset(strikerX * u, 0.82f * u)
                pull?.let { v ->
                    val aim = st - v * u * 1.6f
                    drawLine(Color.White.copy(alpha = 0.8f), st, aim, 4f, StrokeCap.Round, PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                }
                drawCircle(Color(0xFF2F6FE0), game.striker.r * u, st)
                drawCircle(Color(0xFFCFE3FF), game.striker.r * u * 0.6f, st)
            }
        }
        Slider(strikerX, { strikerX = it.coerceIn(0.2f, 0.8f); if (!running) { game.striker.x = strikerX } }, valueRange = 0.2f..0.8f, enabled = !running && !done)
        Text("Slide to place the striker, then pull back and let go to shoot. The red queen counts too.", color = p.muted, fontSize = 13.sp)
    }
}
