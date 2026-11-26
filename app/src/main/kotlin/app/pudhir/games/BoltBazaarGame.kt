package app.pudhir.games

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pudhir.core.BoltBazaar
import app.pudhir.ui.ActionChip
import app.pudhir.ui.ActionRow
import app.pudhir.ui.LocalPalette
import app.pudhir.ui.PieceColours
import app.pudhir.ui.bouncyClick
import app.pudhir.ui.darken
import app.pudhir.ui.lighten
import app.pudhir.ui.popIn
import app.pudhir.ui.shake

private val PlateTints = listOf(
    Color(0xFFBFD7EA), Color(0xFFF2D0A9), Color(0xFFD7C5E8), Color(0xFFC8E6C9), Color(0xFFF6C6C6), Color(0xFFE8E2C8),
)

@Composable
fun BoltBazaarGame(s: PlaySession) {
    var attempt by remember { mutableIntStateOf(0) }
    key(attempt) { BoltBoard(s) { attempt++ } }
}

@Composable
private fun Bolt(colour: Color, size: Dp, dim: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).shadow(if (dim) 0.dp else 3.dp, CircleShape).clip(CircleShape)
            .background(Brush.radialGradient(listOf(lighten(colour, 0.35f), colour, darken(colour, 0.2f))))
            .graphicsLayer { alpha = if (dim) 0.55f else 1f },
        contentAlignment = Alignment.Center,
    ) {
        // Screw slot.
        Box(Modifier.width(size * 0.5f).height(size * 0.12f).clip(RoundedCornerShape(50)).background(darken(colour, 0.45f)))
    }
}

@Composable
private fun BoltBoard(s: PlaySession, onRestart: () -> Unit) {
    val p = LocalPalette.current
    val game = remember { BoltBazaar.generate(s.seed, s.difficulty) }
    var version by remember { mutableIntStateOf(0) }
    var hints by remember { mutableIntStateOf(0) }
    var hintBolt by remember { mutableIntStateOf(-1) }
    var shakeKey by remember { mutableIntStateOf(0) }

    fun tap(b: Int) {
        when (val r = game.unscrew(b)) {
            BoltBazaar.Blocked -> { shakeKey++; s.haptic(true) }
            is BoltBazaar.ToBox -> { s.haptic(r.boxDone || r.dropped.isNotEmpty()) }
            is BoltBazaar.ToTray -> { s.haptic(false) }
        }
        hintBolt = -1
        version++
        if (game.isWon()) s.onSolved(hints) else if (game.isLost()) s.onGameOver(null, "The tray is full!")
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        version.let {
            // Open colour boxes.
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (k in 0 until BoltBazaar.OPEN_BOXES) {
                    val box = game.boxes.getOrNull(k)
                    key(game.nextBox, k) {
                        Row(
                            Modifier.weight(1f).height(58.dp).popIn().shadow(4.dp, RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp))
                                .background(if (box != null) lighten(PieceColours[box[0]], 0.72f) else p.surfaceAlt).padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (box != null) for (j in 0 until 3) {
                                if (j < box[1]) Bolt(PieceColours[box[0]], 30.dp, false, Modifier.popIn())
                                else Box(Modifier.size(30.dp).clip(CircleShape).border(2.dp, darken(PieceColours[box[0]], 0.1f), CircleShape))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            // Waiting tray.
            Row(
                Modifier.fillMaxWidth().shake(shakeKey).clip(RoundedCornerShape(16.dp)).background(p.surfaceAlt).padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                for (k in 0 until BoltBazaar.TRAY) {
                    val b = game.tray.getOrNull(k)
                    Box(Modifier.size(30.dp).clip(CircleShape).background(p.surface), contentAlignment = Alignment.Center) {
                        if (b != null) key(b) { Bolt(PieceColours[game.bolts[b].colour], 28.dp, false, Modifier.popIn()) }
                    }
                }
            }
            Text("${game.boxQueue.size - game.nextBox} boxes still to fill", color = p.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            val unit = minOf(maxWidth / game.width.toFloat(), maxHeight / game.height.toFloat())
            Box(Modifier.size(unit * game.width, unit * game.height).clip(RoundedCornerShape(20.dp)).background(if (p.dark) Color(0xFF24212A) else Color(0xFFF1ECE6))) {
                for (pi in game.plates.indices.sortedBy { game.plates[it].layer }) {
                    val pl = game.plates[pi]
                    val inBoard = game.plateIn[pi].also { version }
                    val fall by animateFloatAsState(if (inBoard) 0f else 1f, tween(450), label = "fall")
                    if (fall >= 0.99f) continue
                    Box(
                        Modifier.offset(unit * pl.x, unit * pl.y).size(unit * pl.w, unit * pl.h).padding(3.dp)
                            .graphicsLayer { translationY = fall * 600f; rotationZ = fall * 25f; alpha = 1f - fall }
                            .shadow(4.dp, RoundedCornerShape(14.dp)).clip(RoundedCornerShape(14.dp))
                            .background(PlateTints[pl.tint].copy(alpha = 0.93f))
                            .border(1.5.dp, darken(PlateTints[pl.tint], 0.15f), RoundedCornerShape(14.dp)),
                    )
                    for (b in game.bolts.indices) {
                        val bolt = game.bolts[b]
                        if (bolt.plate != pi) continue
                        val there = game.boltIn[b].also { version }
                        val out by animateFloatAsState(if (there) 0f else 1f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = 500f), label = "out")
                        if (out >= 0.99f) continue
                        val free = game.isFree(b).also { version }
                        val size = unit * 0.62f
                        Bolt(
                            PieceColours[bolt.colour], size, !free,
                            Modifier.offset(unit * bolt.x - size / 2, unit * bolt.y - size / 2)
                                .graphicsLayer { scaleX = 1 + out * 0.6f; scaleY = 1 + out * 0.6f; alpha = 1 - out; rotationZ = out * 360f }
                                .then(if (hintBolt == b) Modifier.border(3.dp, Color(0xFFF5B83D), CircleShape) else Modifier)
                                .bouncyClick { tap(b) },
                        )
                    }
                }
            }
        }
        ActionRow {
            ActionChip(Icons.Rounded.RestartAlt, "Restart", onClick = onRestart)
            ActionChip(Icons.Rounded.Lightbulb, "Hint") { s.useHint { hintBolt = game.hint() ?: -1; hints++ } }
        }
    }
}
