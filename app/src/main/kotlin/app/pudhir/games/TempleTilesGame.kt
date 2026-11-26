package app.pudhir.games

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pudhir.core.TempleTiles
import app.pudhir.ui.ActionChip
import app.pudhir.ui.ActionRow
import app.pudhir.ui.LocalPalette
import app.pudhir.ui.bouncyClick
import app.pudhir.ui.shake
import kotlin.random.Random

val TempleFaces = listOf(
    "🪔", "🌺", "🌼", "🌸", "🪷", "🥭", "🍌", "🍋", "🍉", "🥥", "🌶️", "🧄",
    "🍵", "☕", "🥛", "🍯", "🍚", "🍛", "🐘", "🦚", "🐅", "🐄", "🐒", "🦜",
    "🐟", "🪁", "🥁", "🐚", "🛕", "🏺", "💎", "👑", "📿", "🔔",
)

@Composable
fun TempleTilesGame(s: PlaySession) {
    val p = LocalPalette.current
    val game = remember { TempleTiles.generate(s.seed, s.difficulty) }
    var version by remember { mutableIntStateOf(0) }
    var selected by remember { mutableIntStateOf(-1) }
    var hint by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var hints by remember { mutableIntStateOf(0) }
    var shakeKey by remember { mutableIntStateOf(0) }
    var shakeTile by remember { mutableIntStateOf(-1) }
    val rng = remember { Random(s.seed xor 0x5DEECE66DL) }

    fun tap(i: Int) {
        if (!game.isFree(i)) { shakeTile = i; shakeKey++; s.haptic(true); return }
        if (selected == -1 || selected == i) { selected = if (selected == i) -1 else i; s.haptic(false); return }
        if (game.tryMatch(selected, i)) {
            selected = -1; hint = null; version++; s.haptic(true)
            if (game.isCleared()) s.onSolved(hints)
        } else {
            shakeTile = i; shakeKey++; selected = i; s.haptic(false)
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        version.let {
            val movesLeft = game.hint() != null
            Text(
                "${game.remaining()} tiles left · " + if (movesLeft || game.isCleared()) "${game.layoutName}" else "No pairs free, shuffle!",
                color = if (movesLeft || game.isCleared()) p.muted else p.bad, modifier = Modifier.padding(top = 4.dp),
            )
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            val tileW: Dp = minOf(maxWidth / (game.cols + 0.6f), maxHeight / ((game.rows + 0.6f) * 1.28f))
            val tileH = tileW * 1.28f
            val depth = tileW * 0.12f
            val boardW = tileW * game.cols + depth * game.layers
            val boardH = tileH * game.rows + depth * game.layers
            Box(Modifier.size(boardW, boardH)) {
                val order = game.positions.indices.sortedWith(compareBy({ game.positions[it].layer }, { game.positions[it].r }, { -game.positions[it].c }))
                for (i in order) {
                    val pos = game.positions[i]
                    val present = game.present[i].also { version }
                    val gone by animateFloatAsState(if (present) 1f else 0f, tween(260), label = "gone")
                    if (gone <= 0.01f) continue
                    val free = game.isFree(i).also { version }
                    val sel = selected == i
                    val isHint = hint?.let { it.first == i || it.second == i } ?: false
                    val lift by animateFloatAsState(if (sel) 1f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 600f), label = "lift")
                    Box(
                        Modifier
                            .offset(x = tileW * pos.c + depth * (game.layers - pos.layer), y = tileH * pos.r + depth * (game.layers - pos.layer) - depth * pos.layer * 0.2f)
                            .size(tileW, tileH)
                            .graphicsLayer {
                                scaleX = gone * (1 + lift * 0.06f); scaleY = gone * (1 + lift * 0.06f); alpha = gone
                                translationY = -lift * 6.dp.toPx()
                            }
                            .then(if (shakeTile == i) Modifier.shake(shakeKey) else Modifier)
                            .shadow(if (free) 3.dp else 1.dp, RoundedCornerShape(tileW * 0.18f))
                            .clip(RoundedCornerShape(tileW * 0.18f))
                            .background(Color(0xFFD9CBB5))
                            .padding(bottom = depth * 0.6f, end = depth * 0.3f)
                            .clip(RoundedCornerShape(tileW * 0.16f))
                            .background(if (free) Color(0xFFFFFCF6) else Color(0xFFEDE6DA))
                            .border(
                                if (sel || isHint) 2.5.dp else 0.dp,
                                if (sel) s.accent else if (isHint) Color(0xFFF5B83D) else Color.Transparent,
                                RoundedCornerShape(tileW * 0.16f),
                            )
                            .bouncyClick { tap(i) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            TempleFaces[game.faces[i] % TempleFaces.size],
                            fontSize = (tileW.value * 0.56f).sp,
                            modifier = Modifier.graphicsLayer { alpha = if (free) 1f else 0.55f },
                        )
                    }
                }
            }
        }
        ActionRow {
            ActionChip(Icons.AutoMirrored.Rounded.Undo, "Undo") { if (game.undo() != null) { version++; selected = -1 } }
            ActionChip(Icons.Rounded.Shuffle, "Shuffle") { game.shuffle(rng); version++; selected = -1; hint = null; s.haptic(true) }
            ActionChip(Icons.Rounded.Lightbulb, "Hint") {
                s.useHint { hint = game.hint(); if (hint != null) hints++ else { game.shuffle(rng); version++ } }
            }
        }
    }
}
