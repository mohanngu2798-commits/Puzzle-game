package app.pudhir.games

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import app.pudhir.core.ChaiSort
import app.pudhir.ui.ActionChip
import app.pudhir.ui.ActionRow
import app.pudhir.ui.LocalPalette
import app.pudhir.ui.PieceColours
import app.pudhir.ui.bouncyClick
import app.pudhir.ui.shake

@Composable
fun ChaiSortGame(s: PlaySession) {
    val p = LocalPalette.current
    val start = remember { ChaiSort.generate(s.seed, s.difficulty) }
    var game by remember { mutableStateOf(start.copy()) }
    var version by remember { mutableIntStateOf(0) }
    val history = remember { mutableStateListOf<ChaiSort>() }
    var selected by remember { mutableIntStateOf(-1) }
    var hintMove by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var hints by remember { mutableIntStateOf(0) }
    var shakeGlass by remember { mutableIntStateOf(-1) }
    var shakeKey by remember { mutableIntStateOf(0) }

    fun tap(i: Int) {
        if (game.isSolved()) return
        if (selected == -1) {
            if (game.glasses[i].isNotEmpty()) { selected = i; s.haptic(false) }
            return
        }
        if (selected == i) { selected = -1; return }
        if (game.canPour(selected, i)) {
            history += game.copy()
            game.pour(selected, i)
            version++
            hintMove = null
            s.haptic(false)
            selected = -1
            if (game.isSolved()) s.onSolved(hints)
        } else {
            shakeGlass = i; shakeKey++; s.haptic(true)
            selected = if (game.glasses[i].isNotEmpty()) i else -1
        }
    }

    val count = game.glasses.size
    val perRow = if (count <= 6) count else (count + 1) / 2
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Make every glass a single colour", color = p.muted)
        Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.SpaceEvenly) {
            version.let { _ ->
                for (row in (0 until count).chunked(perRow)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        for (i in row) {
                            val lift by animateFloatAsState(if (selected == i) -28f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 500f), label = "lift")
                            val highlight = hintMove?.let { it.first == i || it.second == i } ?: false
                            Glass(
                                units = game.glasses[i].toList(), capacity = game.capacity, highlight = highlight,
                                modifier = Modifier
                                    .width(if (perRow > 6) 40.dp else 52.dp)
                                    .height(if (perRow > 6) 150.dp else 176.dp)
                                    .graphicsLayer { translationY = lift * density }
                                    .then(if (shakeGlass == i) Modifier.shake(shakeKey) else Modifier)
                                    .bouncyClick { tap(i) },
                            )
                        }
                    }
                }
            }
        }
        ActionRow {
            ActionChip(Icons.AutoMirrored.Rounded.Undo, "Undo", history.isNotEmpty()) {
                game = history.removeAt(history.lastIndex); version++; selected = -1
            }
            ActionChip(Icons.Rounded.RestartAlt, "Restart") { history.clear(); game = start.copy(); version++; selected = -1 }
            ActionChip(Icons.Rounded.Lightbulb, "Hint") {
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
private fun Glass(units: List<Int>, capacity: Int, highlight: Boolean, modifier: Modifier) {
    val p = LocalPalette.current
    val level by animateFloatAsState(units.size.toFloat(), tween(320), label = "level")
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            val glass = Path().apply {
                addRoundRect(RoundRect(0f, 0f, w, h, CornerRadius(w * 0.18f), CornerRadius(w * 0.18f), CornerRadius(w * 0.5f), CornerRadius(w * 0.5f)))
            }
            drawPath(glass, if (p.dark) p.surfaceAlt else androidx.compose.ui.graphics.Color(0xFFF7F3EE))
            clipPath(glass) {
                val unitH = (h - 8.dp.toPx()) / capacity
                val shown = level.coerceAtMost(capacity.toFloat())
                // Draw whole units, then the partial top unit while pouring.
                for (k in units.indices) {
                    val portion = (shown - k).coerceIn(0f, 1f)
                    if (portion <= 0f) break
                    val top = h - (k + portion) * unitH
                    drawRect(PieceColours[units[k]], Offset(0f, top), Size(w, unitH * portion + 1f))
                    drawRect(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.18f), Offset(w * 0.14f, top), Size(w * 0.12f, unitH * portion))
                }
            }
            drawPath(glass, if (highlight) androidx.compose.ui.graphics.Color(0xFFF5B83D) else p.line, style = Stroke(if (highlight) 4.dp.toPx() else 2.dp.toPx()))
        }
    }
    Spacer(Modifier.width(2.dp))
}
