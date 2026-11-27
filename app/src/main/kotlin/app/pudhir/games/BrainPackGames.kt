package app.pudhir.games

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pudhir.core.Deepam
import app.pudhir.core.Jodi
import app.pudhir.core.Nagaram
import app.pudhir.core.WaterChannels
import app.pudhir.ui.ActionChip
import app.pudhir.ui.ActionRow
import app.pudhir.ui.LocalPalette
import app.pudhir.ui.darken
import app.pudhir.ui.jellyClick
import app.pudhir.ui.lighten
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------------------------------------------------------------- Deepam

@Composable
fun DeepamGame(s: PlaySession) {
    val p = LocalPalette.current
    val game = remember { Deepam(s.seed, s.difficulty) }
    var version by remember { mutableIntStateOf(0) }
    var hint by remember { mutableIntStateOf(-1) }
    var hints by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        version.let { Text("${game.lit.count { it }} of ${game.lit.size} lamps lit · ${game.moves} taps", color = p.muted) }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
            val cell = minOf(maxWidth, maxHeight) / game.size
            Box(Modifier.size(cell * game.size).shadow(8.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp)).background(Color(0xFF1E1530))) {
                version.let {
                    for (i in game.lit.indices) {
                        val on = game.lit[i]
                        val glow by animateFloatAsState(if (on) 1f else 0f, spring(dampingRatio = 0.5f, stiffness = 300f), label = "lamp")
                        Box(
                            Modifier.offset(cell * (i % game.size), cell * (i / game.size)).size(cell).padding(cell * 0.08f)
                                .jellyClick(!done) {
                                    game.tap(i); hint = -1; version++; s.haptic(false)
                                    if (game.isSolved()) { done = true; s.onSolved(hints) }
                                }
                                .border(if (hint == i) 3.dp else 0.dp, Color(0xFFF5B83D), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Canvas(Modifier.fillMaxSize()) {
                                val w = size.width
                                if (glow > 0f) drawCircle(Brush.radialGradient(listOf(Color(0xFFFFD36B).copy(alpha = 0.7f * glow), Color.Transparent)), w * 0.55f, Offset(w / 2, w * 0.42f))
                                // The clay lamp.
                                drawArc(Color(0xFFB5652B), 0f, 180f, true, Offset(w * 0.18f, w * 0.38f), androidx.compose.ui.geometry.Size(w * 0.64f, w * 0.4f))
                                drawCircle(Color(0xFF8A4A1C), w * 0.05f, Offset(w * 0.78f, w * 0.58f))
                                if (glow > 0.05f) {
                                    val flame = androidx.compose.ui.graphics.Path().apply {
                                        moveTo(w * 0.5f, w * (0.56f - 0.3f * glow)); quadraticBezierTo(w * 0.62f, w * 0.48f, w * 0.5f, w * 0.57f); quadraticBezierTo(w * 0.38f, w * 0.48f, w * 0.5f, w * (0.56f - 0.3f * glow))
                                    }
                                    drawPath(flame, Brush.verticalGradient(listOf(Color(0xFFFFF3B0), Color(0xFFFF8A1E))))
                                }
                            }
                        }
                    }
                }
            }
        }
        Text("Tap a lamp to flip it and its neighbours. Light them all.", color = p.muted, fontSize = 13.sp)
        ActionRow { ActionChip(Icons.Rounded.Lightbulb, "Hint", !done) { s.useHint { hints++; hint = game.hint() ?: -1 } } }
    }
}

// ---------------------------------------------------------------- Jodi

private val JodiFaces = listOf("🪔", "🥭", "🌺", "🐘", "🦚", "🍵", "🥥", "🌶️", "🛕", "🪁", "🐄", "🍌", "🌼", "🐅", "🥁", "🎋")

@Composable
fun JodiGame(s: PlaySession) {
    val p = LocalPalette.current
    val game = remember { Jodi(s.seed, s.difficulty) }
    val open = remember { mutableStateListOf<Int>() }
    var version by remember { mutableIntStateOf(0) }
    var peek by remember { mutableStateOf(false) }
    var hints by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        version.let { Text("${game.matched.count { it } / 2} of ${game.cards.size / 2} pairs · ${game.flips} tries", color = p.muted) }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
            val cell = minOf(maxWidth / game.cols, maxHeight / game.rows)
            Box(Modifier.size(cell * game.cols, cell * game.rows)) {
                version.let {
                    for (i in game.cards.indices) {
                        val faceUp = peek || game.matched[i] || i in open
                        val turn by animateFloatAsState(if (faceUp) 180f else 0f, tween(320), label = "flip")
                        Box(
                            Modifier.offset(cell * (i % game.cols), cell * (i / game.cols)).size(cell).padding(4.dp)
                                .jellyClick(!busy && !faceUp) {
                                    open += i; s.haptic(false); version++
                                    if (open.size == 2) {
                                        busy = true
                                        val (a, b) = open[0] to open[1]
                                        scope.launch {
                                            delay(if (game.cards[a] == game.cards[b]) 250 else 750)
                                            if (game.check(a, b)) s.haptic(true)
                                            open.clear(); version++; busy = false
                                            if (game.isSolved()) s.onSolved(hints)
                                        }
                                    }
                                }
                                .graphicsLayer { rotationY = turn; cameraDistance = 12f * density }
                                .shadow(3.dp, RoundedCornerShape(14.dp)).clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (turn > 90f) (if (game.matched[i]) Brush.linearGradient(listOf(Color(0xFFE6F7EC), Color(0xFFC9EBD5))) else Brush.linearGradient(listOf(Color.White, Color(0xFFF3EDE5))))
                                    else Brush.linearGradient(listOf(s.accent, darken(s.accent, 0.3f))),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (turn > 90f) Text(JodiFaces[game.cards[i] % JodiFaces.size], fontSize = (cell.value * 0.42f).sp, modifier = Modifier.graphicsLayer { rotationY = 180f })
                            else Text("✦", color = Color.White.copy(alpha = 0.7f), fontSize = (cell.value * 0.3f).sp)
                        }
                    }
                }
            }
        }
        ActionRow {
            ActionChip(Icons.Rounded.Visibility, "Peek", !busy && !peek) {
                s.useHint { hints++; peek = true; scope.launch { delay(1200); peek = false } }
            }
        }
    }
}

// ---------------------------------------------------------------- Nagaram

@Composable
fun NagaramGame(s: PlaySession) {
    val p = LocalPalette.current
    val game = remember { Nagaram(s.seed, s.difficulty) }
    var version by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        version.let { Text("${game.moves} moves · put the town back in order", color = p.muted) }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
            val n = game.size
            val cell = minOf(maxWidth, maxHeight) / n
            Box(Modifier.size(cell * n).shadow(8.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp)).background(if (p.dark) Color(0xFF26222B) else Color(0xFFEDE4D8))) {
                version.let {
                    for (v in 1 until n * n) key(v) {
                        val at = game.tiles.indexOf(v)
                        val x by animateFloatAsState((at % n).toFloat(), spring(dampingRatio = 0.7f, stiffness = 700f), label = "x")
                        val y by animateFloatAsState((at / n).toFloat(), spring(dampingRatio = 0.7f, stiffness = 700f), label = "y")
                        val home = at == v - 1
                        // Colour bands by row of the solved picture: a sunset over a temple town.
                        val row = (v - 1) / n
                        val band = listOf(Color(0xFFF26B3A), Color(0xFFF2994A), Color(0xFFE5487A), Color(0xFF9B5DE5), Color(0xFF4C6EF5))[row % 5]
                        Box(
                            Modifier.offset(cell * x, cell * y).size(cell).padding(3.dp)
                                .jellyClick(!done) {
                                    if (game.slide(game.tiles.indexOf(v))) {
                                        version++; s.haptic(false)
                                        if (game.isSolved()) { done = true; s.onSolved(0) }
                                    } else s.haptic(true)
                                }
                                .shadow(3.dp, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(lighten(band, 0.25f), band)))
                                .border(if (home) 2.dp else 0.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center,
                        ) { Text("$v", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = (cell.value * 0.36f).sp) }
                    }
                }
            }
        }
        Text("Tap a tile next to the gap, or in its row or column, to slide it.", color = p.muted, fontSize = 13.sp)
    }
}

// ---------------------------------------------------------------- Water Channels

@Composable
fun WaterChannelsGame(s: PlaySession) {
    val p = LocalPalette.current
    val game = remember { WaterChannels(s.seed, s.difficulty) }
    var version by remember { mutableIntStateOf(0) }
    var hint by remember { mutableIntStateOf(-1) }
    var hints by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    val turns = remember { mutableStateListOf(*IntArray(game.tiles.size).toTypedArray()) }
    Column(Modifier.fillMaxSize().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        val wet = version.let { game.watered() }
        Text("${wet.count { it }} of ${wet.size} fields watered", color = p.muted)
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
            val n = game.size
            val cell = minOf(maxWidth, maxHeight) / n
            Box(Modifier.size(cell * n).shadow(8.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp)).background(Color(0xFF7FB069))) {
                for (i in game.tiles.indices) {
                    val angle by animateFloatAsState(turns[i] * 90f, spring(dampingRatio = 0.55f, stiffness = 400f), label = "rot")
                    val base = version.let { game.tiles[i] }
                    // Draw the tile's current shape un-rotated, then let the layer spin it; settle when the spin ends.
                    Box(
                        Modifier.offset(cell * (i % n), cell * (i / n)).size(cell)
                            .jellyClick(!done) {
                                game.rotate(i); turns[i] = turns[i] + 1; hint = -1; version++; s.haptic(false)
                                if (game.isSolved()) { done = true; s.onSolved(hints) }
                            }
                            .border(if (hint == i) 3.dp else 0.5.dp, if (hint == i) Color(0xFFF5B83D) else Color(0x22000000)),
                    ) {
                        val shown = remember(i) { mutableIntStateOf(game.tiles[i]) }
                        Canvas(Modifier.fillMaxSize().graphicsLayer { rotationZ = angle - turns[i] * 90f }) {
                            val w = size.width; val c = Offset(w / 2, w / 2)
                            val water = if (wet[i]) Color(0xFF3AA0F2) else Color(0xFF8A6A3A)
                            val bank = if (wet[i]) Color(0xFF1C6FB8) else Color(0xFF6B4F2A)
                            val ends = listOf(1 to Offset(w / 2, 0f), 2 to Offset(w, w / 2), 4 to Offset(w / 2, w), 8 to Offset(0f, w / 2))
                            for ((bit, e) in ends) if (base and bit != 0) {
                                drawLine(bank, c, e, w * 0.34f, StrokeCap.Butt)
                                drawLine(water, c, e, w * 0.22f, StrokeCap.Butt)
                            }
                            drawCircle(bank, w * 0.17f, c); drawCircle(water, w * 0.11f, c)
                            if (i == game.source) { drawCircle(Color(0xFF5A4636), w * 0.26f, c); drawCircle(Color(0xFF3AA0F2), w * 0.17f, c) }
                        }
                        shown.intValue = base
                    }
                }
            }
        }
        Text("Tap channels to turn them. Water every field from the well.", color = p.muted, fontSize = 13.sp)
        ActionRow { ActionChip(Icons.Rounded.Lightbulb, "Hint", !done) { s.useHint { hints++; hint = game.hint() ?: -1 } } }
    }
}
