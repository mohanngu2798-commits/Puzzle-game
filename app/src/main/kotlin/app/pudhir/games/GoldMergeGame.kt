package app.pudhir.games

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pudhir.core.Dir
import app.pudhir.core.GoldMerge
import app.pudhir.ui.ActionChip
import app.pudhir.ui.ActionRow
import app.pudhir.ui.LocalPalette
import app.pudhir.ui.darken
import app.pudhir.ui.lighten
import app.pudhir.ui.popIn
import kotlin.math.abs
import kotlin.math.ln

private fun coinColour(v: Int): Color {
    val step = (ln(v.toDouble()) / ln(2.0)).toInt()
    val ramp = listOf(
        Color(0xFFE8DCC8), Color(0xFFE9C9A0), Color(0xFFEFB57A), Color(0xFFF29D5C), Color(0xFFF2804A),
        Color(0xFFEA6A3F), Color(0xFFE9C46A), Color(0xFFF2C94C), Color(0xFFF5B83D), Color(0xFFE9A21A), Color(0xFFD4AF37),
    )
    return ramp[(step - 1).coerceIn(0, ramp.size - 1)]
}

@Composable
fun GoldMergeGame(s: PlaySession) {
    var attempt by remember { mutableIntStateOf(0) }
    key(attempt) { MergeBoard(s) { attempt++ } }
}

@Composable
private fun MergeBoard(s: PlaySession, onRestart: () -> Unit) {
    val p = LocalPalette.current
    val target = remember { GoldMerge.targetFor(s.difficulty) }
    val game = remember { GoldMerge(s.seed, target = target) }
    var version by remember { mutableIntStateOf(0) }

    fun swipe(d: Dir) {
        if (game.slide(d).isEmpty()) return
        version++
        s.haptic(false)
        if (game.isWon()) s.onSolved(0)
        else if (!game.canMove()) s.onGameOver(game.score, "No moves left")
    }

    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        version.let {
            Text("${game.score}", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = p.ink)
            Text("Merge coins up to $target · best coin ${game.best()}", color = p.muted)
        }
        Spacer(Modifier.height(12.dp))
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            val side = minOf(maxWidth, maxHeight)
            val gap = 8.dp
            val cell: Dp = (side - gap * (game.size + 1)) / game.size
            Box(
                Modifier.size(side).shadow(6.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp))
                    .background(if (p.dark) Color(0xFF2A2620) else Color(0xFFEFE6D8))
                    .pointerInput(Unit) {
                        var total = androidx.compose.ui.geometry.Offset.Zero
                        detectDragGestures(
                            onDragStart = { total = androidx.compose.ui.geometry.Offset.Zero },
                            onDrag = { ch, d -> total += d; ch.consume() },
                            onDragEnd = {
                                if (maxOf(abs(total.x), abs(total.y)) > 40f) {
                                    swipe(if (abs(total.x) > abs(total.y)) (if (total.x > 0) Dir.Right else Dir.Left) else (if (total.y > 0) Dir.Down else Dir.Up))
                                }
                            },
                        )
                    },
            ) {
                for (r in 0 until game.size) for (c in 0 until game.size) {
                    Box(
                        Modifier.offset(gap + (cell + gap) * c, gap + (cell + gap) * r).size(cell).clip(CircleShape)
                            .background(if (p.dark) Color(0xFF3A342B) else Color(0xFFE3D7C4)),
                    )
                }
                version.let {
                    for (pos in 0 until game.size * game.size) {
                        val v = game.values[pos]; if (v == 0) continue
                        val id = game.ids[pos]
                        key(id) {
                            val x by animateDpAsState(gap + (cell + gap) * (pos % game.size), spring(stiffness = 900f), label = "x")
                            val y by animateDpAsState(gap + (cell + gap) * (pos / game.size), spring(stiffness = 900f), label = "y")
                            val bounce = remember { Animatable(1f) }
                            LaunchedEffect(v) { if (v > 4) { bounce.snapTo(1.18f); bounce.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 500f)) } }
                            val col = coinColour(v)
                            Box(
                                Modifier.offset(x, y).size(cell).popIn()
                                    .graphicsLayer { scaleX = bounce.value; scaleY = bounce.value }
                                    .shadow(4.dp, CircleShape).clip(CircleShape)
                                    .background(Brush.radialGradient(listOf(lighten(col, 0.4f), col, darken(col, 0.15f)))),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "$v", fontWeight = FontWeight.ExtraBold,
                                    fontSize = (cell.value * if (v >= 1000) 0.26f else if (v >= 100) 0.32f else 0.4f).sp,
                                    color = if (v >= 8) Color.White else darken(col, 0.6f),
                                )
                            }
                        }
                    }
                }
            }
        }
        Text("Swipe to slide all coins", color = p.muted, modifier = Modifier.padding(top = 8.dp))
        ActionRow { ActionChip(Icons.Rounded.RestartAlt, "Restart", onClick = onRestart) }
    }
}
