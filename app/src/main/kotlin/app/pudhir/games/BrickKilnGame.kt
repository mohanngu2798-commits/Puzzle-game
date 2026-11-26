package app.pudhir.games

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pudhir.core.BrickKiln
import app.pudhir.core.Shape
import app.pudhir.ui.LocalPalette
import app.pudhir.ui.darken
import app.pudhir.ui.lighten
import kotlin.math.roundToInt
import androidx.compose.runtime.rememberCoroutineScope
import app.pudhir.ui.popIn
import kotlinx.coroutines.launch

private val KilnColours = listOf(
    Color(0xFFE76F51), Color(0xFFF4A261), Color(0xFFE9C46A), Color(0xFF2A9D8F), Color(0xFF3A86F2), Color(0xFF9B5DE5),
)

fun DrawScope.brick(col: Color, topLeft: Offset, side: Float, alpha: Float = 1f) {
    val inset = side * 0.05f
    val s = side - inset * 2
    val o = Offset(topLeft.x + inset, topLeft.y + inset)
    drawRoundRect(darken(col, 0.25f).copy(alpha = alpha), o, Size(s, s), CornerRadius(s * 0.2f))
    drawRoundRect(col.copy(alpha = alpha), o, Size(s, s * 0.88f), CornerRadius(s * 0.2f))
    drawRoundRect(lighten(col, 0.35f).copy(alpha = alpha * 0.8f), Offset(o.x + s * 0.15f, o.y + s * 0.12f), Size(s * 0.7f, s * 0.14f), CornerRadius(s * 0.07f))
}

@Composable
fun BrickKilnGame(s: PlaySession) {
    val p = LocalPalette.current
    val game = remember { BrickKiln(s.seed) }
    var version by remember { mutableIntStateOf(0) }
    var boardOrigin by remember { mutableStateOf(Offset.Zero) }
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }
    var cellPx by remember { mutableFloatStateOf(1f) }
    var dragSlot by remember { mutableIntStateOf(-1) }
    var finger by remember { mutableStateOf(Offset.Zero) }
    val trayOrigins = remember { Array(3) { Offset.Zero } }
    val flash = remember { Animatable(0f) }
    var flashCells by remember { mutableStateOf<List<Int>>(emptyList()) }
    var flashColour by remember { mutableIntStateOf(0) }
    var comboText by remember { mutableStateOf("") }
    var comboKey by remember { mutableIntStateOf(0) }
    val placeAnim = remember { Animatable(1f) }
    var placedCells by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var flashOrigin by remember { mutableStateOf(Offset.Zero) }
    var returning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val lift = with(density) { 70.dp.toPx() }

    val score by animateFloatAsState(game.score.toFloat().also { version }, tween(400), label = "score")

    LaunchedEffect(flashCells) {
        if (flashCells.isNotEmpty()) { flash.snapTo(0f); flash.animateTo(1f, tween(650)) }
    }

    /** Top-left board cell for the dragged piece, if it is over the board. */
    fun target(shape: Shape): Pair<Int, Int>? {
        val topLeft = Offset(finger.x - shape.width * cellPx / 2, finger.y - lift - shape.height * cellPx / 2)
        val c = ((topLeft.x - boardOrigin.x) / cellPx).roundToInt()
        val r = ((topLeft.y - boardOrigin.y) / cellPx).roundToInt()
        return if (game.fits(shape, r, c)) r to c else null
    }

    Box(Modifier.fillMaxSize().onGloballyPositioned { rootOrigin = it.positionInRoot() }) {
        Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(score.roundToInt().toString(), fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = p.ink)
            androidx.compose.runtime.key(comboKey) {
                Text(
                    if (comboText.isNotEmpty()) comboText else "Fill rows and columns to fire them",
                    color = if (comboText.isNotEmpty()) s.accent else p.muted,
                    fontWeight = if (comboText.isNotEmpty()) FontWeight.ExtraBold else FontWeight.SemiBold,
                    fontSize = if (comboText.isNotEmpty()) 18.sp else 14.sp,
                    modifier = if (comboText.isNotEmpty()) Modifier.popIn() else Modifier,
                )
            }
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                val side = minOf(maxWidth, maxHeight)
                Box(
                    Modifier.size(side).shadow(6.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp))
                        .background(if (p.dark) Color(0xFF26222B) else Color(0xFFF3ECE4)).padding(8.dp),
                ) {
                    Canvas(Modifier.fillMaxSize().onGloballyPositioned { boardOrigin = it.positionInRoot(); cellPx = it.size.width / game.size.toFloat() }) {
                        version
                        val cs = size.width / game.size
                        for (r in 0 until game.size) for (c in 0 until game.size) {
                            val v = game.board[r * game.size + c]
                            val o = Offset(c * cs, r * cs)
                            if (v < 0) drawRoundRect(if (p.dark) Color(0xFF332E39) else Color(0xFFE8DFD5), Offset(o.x + cs * 0.06f, o.y + cs * 0.06f), Size(cs * 0.88f, cs * 0.88f), CornerRadius(cs * 0.18f))
                            else if ((r * game.size + c) in placedCells) {
                                // Freshly placed bricks settle in with a little squash.
                                val k = placeAnim.value
                                val sz = cs * k
                                brick(KilnColours[v], Offset(o.x + (cs - sz) / 2, o.y + (cs - sz) / 2), sz)
                            } else brick(KilnColours[v], o, cs)
                        }
                        // Ghost preview of where the dragged piece lands.
                        val shape = if (dragSlot >= 0) game.tray[dragSlot] else null
                        if (shape != null && !returning) target(shape)?.let { (r, c) ->
                            for (cell in shape.cells) brick(KilnColours[shape.colour], Offset((c + cell.c) * cs, (r + cell.r) * cs), cs, 0.35f)
                            // Glow the lines this drop would fire.
                            val filled = BooleanArray(game.size * game.size) { game.board[it] >= 0 }
                            for (cell in shape.cells) filled[(r + cell.r) * game.size + c + cell.c] = true
                            val glow = lightenGlow(KilnColours[shape.colour])
                            for (rr in 0 until game.size) if ((0 until game.size).all { filled[rr * game.size + it] }) {
                                drawRoundRect(glow, Offset(0f, rr * cs), Size(size.width, cs), CornerRadius(cs * 0.2f))
                            }
                            for (cc in 0 until game.size) if ((0 until game.size).all { filled[it * game.size + cc] }) {
                                drawRoundRect(glow, Offset(cc * cs, 0f), Size(cs, size.height), CornerRadius(cs * 0.2f))
                            }
                        }
                        // Fired bricks sweep away outward from where the piece landed.
                        if (flash.value in 0.0001f..0.9999f) for (q in flashCells) {
                            val cx = (q % game.size) + 0.5f; val cy = (q / game.size) + 0.5f
                            val d = kotlin.math.hypot(cx - flashOrigin.x, cy - flashOrigin.y)
                            val t = ((flash.value - d * 0.05f) / 0.45f).coerceIn(0f, 1f)
                            if (t >= 1f) continue
                            val sz = cs * (1f - t) * (1f + 0.25f * t)
                            val o = Offset(cx * cs - sz / 2, cy * cs - sz / 2 - t * cs * 0.4f)
                            if (t <= 0f) brick(KilnColours[flashColour], Offset((q % game.size) * cs, (q / game.size) * cs), cs)
                            else drawRoundRect(lighten(KilnColours[flashColour], 0.55f).copy(alpha = 1f - t), o, Size(sz, sz), CornerRadius(sz * 0.2f))
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                for (slot in 0 until 3) {
                    val shape = game.tray[slot].also { version }
                    val appear by animateFloatAsState(if (shape != null) 1f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 400f), label = "tray")
                    Box(
                        Modifier.size(100.dp)
                            .onGloballyPositioned { trayOrigins[slot] = it.positionInRoot() }
                            .graphicsLayer { scaleX = appear; scaleY = appear; alpha = if (dragSlot == slot) 0f else 1f }
                            .pointerInput(slot) {
                                detectDragGestures(
                                    onDragStart = { o -> if (game.tray[slot] != null && !returning) { dragSlot = slot; finger = trayOrigins[slot] + o; s.haptic(false) } },
                                    onDrag = { change, delta -> if (dragSlot == slot) { finger += delta; change.consume() } },
                                    onDragEnd = {
                                        val sh = game.tray[slot]
                                        val t = if (sh != null && dragSlot == slot) target(sh) else null
                                        if (sh != null && t != null) {
                                            dragSlot = -1
                                            val before = game.score
                                            game.place(slot, t.first, t.second)
                                            placedCells = sh.cells.map { (t.first + it.r) * game.size + t.second + it.c }.toSet() - game.lastCleared.toSet()
                                            scope.launch { placeAnim.snapTo(0.7f); placeAnim.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 700f)) }
                                            s.haptic(false)
                                            if (game.lastCleared.isNotEmpty()) {
                                                flashColour = sh.colour; flashCells = game.lastCleared.toList()
                                                flashOrigin = Offset(t.second + sh.width / 2f, t.first + sh.height / 2f)
                                                comboText = if (game.combo > 1) "Combo ×${game.combo}!  +${game.score - before}" else "Fired!  +${game.score - before}"
                                                comboKey++
                                                s.haptic(true)
                                            } else comboText = ""
                                            version++
                                            if (game.isGameOver()) scope.launch { kotlinx.coroutines.delay(700); s.onGameOver(game.score, "Kiln is full!") }
                                        } else if (dragSlot == slot) {
                                            // Not a valid spot: glide back to the tray.
                                            returning = true
                                            val from = finger
                                            val home = trayOrigins[slot] + Offset(50f * density.density, 50f * density.density + lift)
                                            scope.launch {
                                                val a = Animatable(0f)
                                                a.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 600f)) { finger = from + (home - from) * value }
                                                dragSlot = -1; returning = false
                                            }
                                            s.haptic(true)
                                        }
                                    },
                                    onDragCancel = { if (!returning) dragSlot = -1 },
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (shape != null) Canvas(Modifier.size(90.dp)) {
                            val cs = minOf(size.width / 5, size.height / 5)
                            val ox = (size.width - shape.width * cs) / 2; val oy = (size.height - shape.height * cs) / 2
                            val dim = !game.canPlaceAnywhere(shape)
                            for (cell in shape.cells) brick(if (dim) p.muted else KilnColours[shape.colour], Offset(ox + cell.c * cs, oy + cell.r * cs), cs)
                        }
                    }
                }
            }
        }
        // The piece follows the finger at full board size, lifted above it.
        val shape = if (dragSlot >= 0) game.tray[dragSlot] else null
        if (shape != null) {
            val w = shape.width * cellPx; val h = shape.height * cellPx
            val pos = Offset(finger.x - w / 2, finger.y - lift - h / 2) - rootOrigin
            Canvas(
                Modifier.offset { IntOffset(pos.x.roundToInt(), pos.y.roundToInt()) }
                    .size(with(density) { w.toDp() }, with(density) { h.toDp() }),
            ) { for (cell in shape.cells) brick(KilnColours[shape.colour], Offset(cell.c * cellPx, cell.r * cellPx), cellPx) }
        }
    }
}

private fun lightenGlow(c: Color) = lighten(c, 0.6f).copy(alpha = 0.45f)
