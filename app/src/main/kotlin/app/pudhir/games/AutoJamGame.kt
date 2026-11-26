package app.pudhir.games

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pudhir.core.AutoJam
import app.pudhir.core.Vehicle
import app.pudhir.ui.ActionChip
import app.pudhir.ui.ActionRow
import app.pudhir.ui.LocalPalette
import app.pudhir.ui.PieceColours
import app.pudhir.ui.darken
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun AutoJamGame(s: PlaySession) = Generated(s, { AutoJam.generate(s.seed, s.difficulty) }) { JamBoard(s, it) }

@Composable
private fun JamBoard(s: PlaySession, start: AutoJam) {
    val p = LocalPalette.current
    val game = remember { start.copy() }
    var version by remember { mutableIntStateOf(0) }
    val history = remember { mutableStateListOf<List<Vehicle>>() }
    var hint by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var hints by remember { mutableIntStateOf(0) }
    var won by remember { mutableStateOf(false) }
    val drive = remember { Animatable(0f) }
    val n = game.size

    LaunchedEffect(won) {
        if (won) { drive.animateTo(1f, tween(500)); s.onSolved(hints) }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Free the golden auto · ${history.size.also { version }} moves", color = p.muted)
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            val side = minOf(maxWidth, maxHeight) - 20.dp
            val cell = side / n
            val cellPx = with(LocalDensity.current) { cell.toPx() }
            Box(Modifier.size(side + 20.dp)) {
                Box(Modifier.offset(10.dp, 10.dp).size(side).shadow(6.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp)).background(if (p.dark) Color(0xFF2B2A30) else Color(0xFFE9E4DC)))
                // Exit gate.
                Box(Modifier.offset(side + 6.dp, 10.dp + cell * game.exitRow + cell * 0.15f).size(14.dp, cell * 0.7f).clip(RoundedCornerShape(6.dp)).background(Color(0xFFF5B83D)))
                version.let {
                    for ((i, v) in game.vehicles.withIndex()) key(i) {
                        val shift = remember { Animatable(0f) }
                        val scope = rememberCoroutineScope()
                        val col = if (i == 0) Color(0xFFF5B83D) else PieceColours[(i * 3) % PieceColours.size]
                        val isHint = hint?.first == i
                        val w = if (v.horizontal) cell * v.len else cell
                        val h = if (v.horizontal) cell else cell * v.len
                        Box(
                            Modifier
                                .offset(10.dp + cell * v.col, 10.dp + cell * v.row)
                                .size(w, h)
                                .graphicsLayer {
                                    if (v.horizontal) translationX = shift.value + (if (i == 0) drive.value * cellPx * 3 else 0f) else translationY = shift.value
                                    alpha = if (i == 0) 1f - drive.value * 0.8f else 1f
                                }
                                .padding(4.dp)
                                .shadow(4.dp, RoundedCornerShape(14.dp)).clip(RoundedCornerShape(14.dp))
                                .background(col)
                                .border(if (isHint) 3.dp else 0.dp, if (isHint) Color.White else Color.Transparent, RoundedCornerShape(14.dp))
                                .pointerInput(i, version) {
                                    detectDragGestures(
                                        onDrag = { ch, d ->
                                            ch.consume()
                                            val (lo, hi) = game.range(i)
                                            val delta = if (v.horizontal) d.x else d.y
                                            scope.launch { shift.snapTo((shift.value + delta).coerceIn(lo * cellPx, hi * cellPx)) }
                                        },
                                        onDragEnd = {
                                            val steps = (shift.value / cellPx).roundToInt()
                                            scope.launch {
                                                shift.animateTo(steps * cellPx, spring(dampingRatio = 0.6f, stiffness = 700f))
                                                if (steps != 0) {
                                                    history += game.vehicles.toList()
                                                    game.move(i, steps)
                                                    shift.snapTo(0f)
                                                    hint = null
                                                    s.haptic(false)
                                                    version++
                                                    if (game.isSolved()) { s.haptic(true); won = true }
                                                }
                                            }
                                        },
                                    )
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            // Roof and windows.
                            Box(Modifier.fillMaxSize().padding(if (v.horizontal) 8.dp else 6.dp).clip(RoundedCornerShape(10.dp)).background(darken(col, 0.12f)))
                            Text(if (i == 0) "🛺" else if (v.len == 3) "🚚" else "🚗", fontSize = (cell.value * 0.42f).sp)
                        }
                    }
                }
            }
        }
        ActionRow {
            ActionChip(Icons.AutoMirrored.Rounded.Undo, "Undo", history.isNotEmpty() && !won) {
                val last = history.removeAt(history.lastIndex)
                for (k in last.indices) game.vehicles[k] = last[k]
                version++
            }
            ActionChip(Icons.Rounded.RestartAlt, "Restart", !won) {
                for (k in start.vehicles.indices) game.vehicles[k] = start.vehicles[k]
                history.clear(); version++
            }
            ActionChip(Icons.Rounded.Lightbulb, "Hint", !won) {
                s.useHint { hint = game.solve()?.firstOrNull(); hints++ }
            }
        }
    }
}
