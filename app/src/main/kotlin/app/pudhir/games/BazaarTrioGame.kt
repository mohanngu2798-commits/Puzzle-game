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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pudhir.core.BazaarTrio
import app.pudhir.ui.ActionChip
import app.pudhir.ui.ActionRow
import app.pudhir.ui.LocalPalette
import app.pudhir.ui.bouncyClick
import app.pudhir.ui.lighten
import app.pudhir.ui.popIn
import app.pudhir.ui.shake

val BazaarFaces = listOf(
    "🍎", "🍊", "🍇", "🥕", "🌽", "🍆", "🥭", "🍌", "🍋", "🍉",
    "🥥", "🌶️", "🧅", "🍓", "🍍", "🥝", "🫑", "🍐", "🥒", "🍒",
)

@Composable
fun BazaarTrioGame(s: PlaySession) {
    var attempt by remember { mutableIntStateOf(0) }
    key(attempt) { TrioBoard(s, onRestart = { attempt++ }) }
}

@Composable
private fun TrioBoard(s: PlaySession, onRestart: () -> Unit) {
    val p = LocalPalette.current
    val game = remember { BazaarTrio.generate(s.seed, s.difficulty) }
    var version by remember { mutableIntStateOf(0) }
    var hints by remember { mutableIntStateOf(0) }
    var hintTile by remember { mutableIntStateOf(-1) }
    var shakeKey by remember { mutableIntStateOf(0) }

    fun tap(i: Int) {
        if (!game.isFree(i)) { s.haptic(true); return }
        val cleared = game.take(i)
        hintTile = -1
        version++
        s.haptic(cleared.isNotEmpty())
        when {
            game.isWon() -> s.onSolved(hints)
            game.isLost() -> { shakeKey++; s.onGameOver(null, "Basket is full!") }
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${game.present.count { it }.also { version }} items on the table", color = p.muted, modifier = Modifier.padding(top = 4.dp))
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            val tile = minOf(maxWidth / game.width.toFloat(), maxHeight / game.height.toFloat())
            Box(Modifier.size(tile * game.width, tile * game.height)) {
                val order = game.tiles.indices.sortedBy { game.tiles[it].layer }
                for (i in order) {
                    val t = game.tiles[i]
                    val present = game.present[i].also { version }
                    val gone by animateFloatAsState(if (present) 1f else 0f, spring(stiffness = 700f), label = "gone")
                    if (gone <= 0.02f) continue
                    val free = game.isFree(i).also { version }
                    Box(
                        Modifier
                            .offset(x = tile * t.x, y = tile * t.y)
                            .size(tile)
                            .padding(2.dp)
                            .graphicsLayer { scaleX = gone; scaleY = gone; alpha = gone }
                            .shadow(if (free) 4.dp else 1.dp, RoundedCornerShape(14.dp))
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (free) p.surface else if (p.dark) Color(0xFF2B2832) else Color(0xFFE4DDD3))
                            .border(if (hintTile == i) 3.dp else 1.dp, if (hintTile == i) Color(0xFFF5B83D) else p.line, RoundedCornerShape(14.dp))
                            .bouncyClick { tap(i) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(BazaarFaces[t.face % BazaarFaces.size], fontSize = (tile.value * 0.5f).sp, modifier = Modifier.graphicsLayer { alpha = if (free) 1f else 0.4f })
                    }
                }
            }
        }
        // The basket.
        Row(
            Modifier.fillMaxWidth().shake(shakeKey).shadow(4.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp))
                .background(lighten(s.accent, if (p.dark) 0.1f else 0.85f)).padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            version.let {
                for (k in 0 until BazaarTrio.CAPACITY) {
                    val idx = game.basket.getOrNull(k)
                    Box(
                        Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(p.surface.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (idx != null) key(idx) {
                            Text(BazaarFaces[game.tiles[idx].face % BazaarFaces.size], fontSize = 22.sp, modifier = Modifier.popIn())
                        }
                    }
                }
            }
        }
        ActionRow {
            ActionChip(Icons.AutoMirrored.Rounded.Undo, "Undo") { if (game.undo()) version++ else s.haptic(true) }
            ActionChip(Icons.Rounded.RestartAlt, "Restart", onClick = onRestart)
            ActionChip(Icons.Rounded.Lightbulb, "Hint") { s.useHint { hintTile = game.hint() ?: -1; hints++ } }
        }
    }
}
