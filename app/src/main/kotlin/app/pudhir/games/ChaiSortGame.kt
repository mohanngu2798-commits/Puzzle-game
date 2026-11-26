package app.pudhir.games

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import app.pudhir.core.ChaiSort
import app.pudhir.ui.ActionChip
import app.pudhir.ui.ActionRow
import app.pudhir.ui.LocalPalette
import app.pudhir.ui.PieceColours
import app.pudhir.ui.bouncyClick
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChaiSortGame(s: PlaySession) = Generated(s, { ChaiSort.generate(s.seed, s.difficulty) }) { ChaiBoard(s, it) }

private class Pour(val from: Int, val to: Int, val colour: Int)

@Composable
private fun ChaiBoard(s: PlaySession, start: ChaiSort) {
    val p = LocalPalette.current
    var game by remember { mutableStateOf(start.copy()) }
    var version by remember { mutableIntStateOf(0) }
    val history = remember { mutableStateListOf<ChaiSort>() }
    var selected by remember { mutableIntStateOf(-1) }
    var hintMove by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var hints by remember { mutableIntStateOf(0) }
    var wobble by remember { mutableIntStateOf(-1) }
    var wobbleKey by remember { mutableIntStateOf(0) }
    var pour by remember { mutableStateOf<Pour?>(null) }
    var streaming by remember { mutableStateOf(false) }
    val travel = remember { Animatable(0f) }
    val tilt = remember { Animatable(0f) }
    val positions = remember { mutableStateMapOf<Int, Offset>() }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val count = game.glasses.size
    val perRow = if (count <= 6) count else (count + 1) / 2
    val glassW = if (perRow > 6) 40.dp else 50.dp
    val glassH = if (perRow > 6) 150.dp else 172.dp
    val wPx = with(density) { glassW.toPx() }
    val hPx = with(density) { glassH.toPx() }

    fun complete(i: Int) = game.glasses[i].let { it.size == game.capacity && it.all { c -> c == it[0] } }

    fun tap(i: Int) {
        if (pour != null || game.isSolved()) return
        if (selected == -1) {
            if (game.glasses[i].isNotEmpty() && !complete(i)) { selected = i; s.haptic(false) }
            return
        }
        if (selected == i) { selected = -1; return }
        val from = selected
        if (!game.canPour(from, i)) {
            wobble = i; wobbleKey++; s.haptic(true)
            return
        }
        val move = Pour(from, i, game.glasses[from].last())
        pour = move
        hintMove = null
        scope.launch {
            // Lift, glide over the target, tilt, pour, then return.
            travel.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
            tilt.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
            history += game.copy()
            streaming = true
            game.pour(from, i)
            version++
            s.haptic(false)
            delay(460)
            streaming = false
            tilt.animateTo(0f, tween(200))
            selected = -1
            travel.animateTo(0f, tween(300, easing = FastOutSlowInEasing))
            pour = null
            if (complete(i)) s.haptic(true)
            if (game.isSolved()) { delay(250); s.onSolved(hints) }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        version.let {
            val done = (0 until count).count { complete(it) }
            Text("$done of ${ChaiSort.coloursFor(s.difficulty)} glasses sorted", color = p.muted)
        }
        Box(Modifier.fillMaxWidth().weight(1f).onGloballyPositioned { origin = it.positionInRoot() }) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
                version.let { _ ->
                    for (row in (0 until count).chunked(perRow)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            for (i in row) {
                                val lift by animateFloatAsState(
                                    if (selected == i && pour == null) -26f else 0f,
                                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 500f), label = "lift",
                                )
                                val mine = pour?.takeIf { it.from == i }
                                val target = mine?.let { positions[it.to] }
                                val here = positions[i]
                                Box(
                                    Modifier
                                        .zIndex(if (mine != null) 2f else 0f)
                                        .onGloballyPositioned { positions[i] = it.positionInRoot() - origin }
                                        .graphicsLayer {
                                            var tx = 0f; var ty = lift.dp.toPx(); var rot = 0f
                                            if (mine != null && target != null && here != null) {
                                                val toRight = target.x >= here.x
                                                // Mouth of the tilted glass sits just above the target's rim.
                                                val dx = target.x - here.x + (if (toRight) -wPx * 0.62f else wPx * 0.62f)
                                                val dy = target.y - here.y - hPx * 0.62f
                                                tx = dx * travel.value
                                                ty = ty * (1 - travel.value) + dy * travel.value
                                                rot = tilt.value * (if (toRight) 72f else -72f)
                                            }
                                            translationX = tx; translationY = ty; rotationZ = rot
                                        },
                                ) {
                                    Glass(
                                        units = game.glasses[i].toList(), capacity = game.capacity,
                                        highlight = hintMove?.let { it.first == i || it.second == i } ?: false,
                                        complete = complete(i),
                                        wobbleKey = if (wobble == i) wobbleKey else 0,
                                        modifier = Modifier.width(glassW).height(glassH).bouncyClick { tap(i) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            // The stream of tea falling into the target glass.
            val pr = pour
            val target = pr?.let { positions[it.to] }
            if (streaming && pr != null && target != null) {
                val streamLen by animateFloatAsState(1f, tween(140), label = "stream")
                Canvas(Modifier.fillMaxSize().zIndex(3f)) {
                    val x = target.x + wPx / 2
                    val top = target.y - hPx * 0.14f
                    val bottom = target.y + hPx * 0.9f
                    drawLine(PieceColours[pr.colour], Offset(x, top), Offset(x, top + (bottom - top) * streamLen), wPx * 0.16f, StrokeCap.Round)
                }
            }
        }
        ActionRow {
            ActionChip(Icons.AutoMirrored.Rounded.Undo, "Undo", history.isNotEmpty() && pour == null) {
                game = history.removeAt(history.lastIndex); version++; selected = -1
            }
            ActionChip(Icons.Rounded.RestartAlt, "Restart", pour == null) { history.clear(); game = start.copy(); version++; selected = -1 }
            ActionChip(Icons.Rounded.Lightbulb, "Hint", pour == null) {
                s.useHint {
                    val moves = game.solve()
                    if (moves != null && moves.isNotEmpty()) { hintMove = moves.first(); selected = moves.first().first; hints++ }
                    else { history.clear(); game = start.copy(); version++; hintMove = null } // dead end: rewind
                }
            }
        }
    }
}

@Composable
private fun Glass(units: List<Int>, capacity: Int, highlight: Boolean, complete: Boolean, wobbleKey: Int, modifier: Modifier) {
    val p = LocalPalette.current
    val level by animateFloatAsState(units.size.toFloat(), tween(440, easing = FastOutSlowInEasing), label = "level")
    // Sorted glasses pop and glow.
    val pop = remember { Animatable(1f) }
    val glow by animateFloatAsState(if (complete) 1f else 0f, tween(400), label = "glow")
    LaunchedEffect(complete) {
        if (complete) { delay(350); pop.snapTo(1.12f); pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 400f)) }
    }
    // A small "no" wiggle for a pour that is not allowed.
    val wig = remember { Animatable(0f) }
    LaunchedEffect(wobbleKey) {
        if (wobbleKey == 0) return@LaunchedEffect
        for (v in listOf(6f, -5f, 3f, 0f)) wig.animateTo(v, tween(60))
    }
    Box(modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value; rotationZ = wig.value }) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            val glass = Path().apply {
                addRoundRect(RoundRect(0f, 0f, w, h, CornerRadius(w * 0.12f), CornerRadius(w * 0.12f), CornerRadius(w * 0.5f), CornerRadius(w * 0.5f)))
            }
            if (glow > 0f) {
                val gold = Color(0xFFF5B83D)
                drawRoundRect(gold.copy(alpha = 0.25f * glow), Offset(-8f, -8f), Size(w + 16f, h + 16f), CornerRadius(w * 0.5f))
            }
            drawPath(glass, if (p.dark) p.surfaceAlt else Color(0xFFF7F3EE))
            clipPath(glass) {
                val unitH = (h - 10.dp.toPx()) / capacity
                val shown = level.coerceAtMost(capacity.toFloat())
                for (k in units.indices) {
                    val portion = (shown - k).coerceIn(0f, 1f)
                    if (portion <= 0f) break
                    val top = h - (k + portion) * unitH
                    drawRect(PieceColours[units[k]], Offset(0f, top), Size(w, unitH * portion + 1f))
                }
                // Departing tea (level higher than units) keeps the colour of the old top while draining.
                if (shown > units.size && units.size < capacity) {
                    val k = units.size
                    val portion = (shown - k).coerceIn(0f, 1f)
                    val top = h - (k + portion) * unitH
                    drawRect(Color.White.copy(alpha = 0.0f), Offset(0f, top), Size(w, unitH * portion))
                }
                // Glass shine.
                drawRect(Color.White.copy(alpha = 0.22f), Offset(w * 0.14f, h * 0.06f), Size(w * 0.1f, h * 0.86f))
            }
            drawPath(
                glass,
                when { highlight -> Color(0xFFF5B83D); complete -> Color(0xFF2BA36B); else -> p.line },
                style = Stroke(if (highlight || complete) 3.5.dp.toPx() else 2.dp.toPx()),
            )
        }
        if (complete) Box(
            Modifier.align(Alignment.TopCenter).offset(y = (-14).dp).size(24.dp).graphicsLayer { scaleX = glow; scaleY = glow }
                .clip(CircleShape).background(Color(0xFF2BA36B)),
            contentAlignment = Alignment.Center,
        ) { Text("✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
    }
}
