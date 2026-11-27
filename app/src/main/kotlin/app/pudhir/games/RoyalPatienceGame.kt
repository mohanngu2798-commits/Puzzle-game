package app.pudhir.games

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pudhir.core.Card
import app.pudhir.core.RoyalPatience
import app.pudhir.ui.ActionChip
import app.pudhir.ui.ActionRow
import app.pudhir.ui.LocalPalette
import app.pudhir.ui.jellyClick
import app.pudhir.ui.darken
import app.pudhir.ui.shake
import kotlinx.coroutines.delay

private val SuitGlyph = listOf("♠", "♥", "♦", "♣")
private fun rankLabel(r: Int) = when (r) { 1 -> "A"; 11 -> "J"; 12 -> "Q"; 13 -> "K"; else -> "$r" }

@Composable
private fun PlayingCard(card: Card?, w: Dp, h: Dp, accent: Color, highlight: Boolean = false, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(w * 0.12f)
    when {
        card == null -> Box(modifier.size(w, h).clip(shape).border(1.5.dp, p.line, shape))
        !card.up -> Box(
            modifier.size(w, h).shadow(2.dp, shape).clip(shape)
                .background(Brush.linearGradient(listOf(accent, darken(accent, 0.3f))))
                .padding(w * 0.08f).clip(RoundedCornerShape(w * 0.08f)).border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(w * 0.08f)),
        )
        else -> {
            val ink = if (card.red) Color(0xFFD62839) else Color(0xFF1C1B22)
            Box(
                modifier.size(w, h).shadow(2.dp, shape).clip(shape).background(Color.White)
                    .border(if (highlight) 2.5.dp else 0.5.dp, if (highlight) Color(0xFFF5B83D) else Color(0xFFDDD6CC), shape),
            ) {
                Column(Modifier.padding(start = w * 0.08f, top = w * 0.02f)) {
                    Text(rankLabel(card.rank), color = ink, fontWeight = FontWeight.ExtraBold, fontSize = (w.value * 0.3f).sp)
                    Text(SuitGlyph[card.suit], color = ink, fontSize = (w.value * 0.26f).sp, modifier = Modifier.offset(y = (-w.value * 0.08f).dp))
                }
                Text(SuitGlyph[card.suit], color = ink, fontSize = (w.value * 0.5f).sp, modifier = Modifier.align(Alignment.BottomEnd).padding(end = w * 0.08f))
            }
        }
    }
}

@Composable
fun RoyalPatienceGame(s: PlaySession) {
    val p = LocalPalette.current
    val game = remember { RoyalPatience.generate(s.seed, s.difficulty) }
    var version by remember { mutableIntStateOf(0) }
    var hint by remember { mutableStateOf<Pair<RoyalPatience.Source, Int>?>(null) }
    var hints by remember { mutableIntStateOf(0) }
    var shakeKey by remember { mutableIntStateOf(0) }
    var finishing by remember { mutableStateOf(false) }

    fun after(ok: Boolean) {
        hint = null
        if (!ok) { shakeKey++; s.haptic(true); return }
        s.haptic(false); version++
        if (game.isWon()) s.onSolved(hints) else if (game.canAutoFinish()) finishing = true
    }

    LaunchedEffect(finishing) {
        if (!finishing) return@LaunchedEffect
        while (game.autoFinishStep()) { version++; delay(90) }
        if (game.isWon()) s.onSolved(hints)
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text((if (game.drawCount == 3) "Draw three" else "Draw one") + " · ${game.moves.also { version }} moves", color = p.muted)
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).padding(top = 8.dp).shake(shakeKey)) {
            val gap = 5.dp
            val cw = (maxWidth - gap * 8) / 7
            val ch = cw * 1.42f
            val faceDownStep = ch * 0.14f
            version.let {
                // Stock.
                Box(Modifier.offset(gap, 0.dp).jellyClick { game.draw(); after(true) }) {
                    PlayingCard(if (game.stock.isEmpty()) null else Card(0, 1, false), cw, ch, s.accent)
                    if (game.stock.isEmpty()) Text("↻", fontSize = 22.sp, color = p.muted, modifier = Modifier.align(Alignment.Center))
                }
                // Waste: the last up to three cards fanned; only the top one plays.
                val shown = game.waste.takeLast(if (game.drawCount == 3) 3 else 1)
                shown.forEachIndexed { k, card ->
                    val top = k == shown.lastIndex
                    PlayingCard(
                        card, cw, ch, s.accent, highlight = top && hint?.first == RoyalPatience.Waste,
                        modifier = Modifier.offset(gap * 2 + cw + cw * 0.28f * k, 0.dp).then(if (top) Modifier.jellyClick { after(game.autoMove(RoyalPatience.Waste)) } else Modifier),
                    )
                }
                // Foundations.
                for (f in 0 until 4) {
                    val x = gap * (4 + f) + cw * (3 + f)
                    Box(Modifier.offset(x, 0.dp)) {
                        PlayingCard(game.foundations[f].lastOrNull(), cw, ch, s.accent)
                        if (game.foundations[f].isEmpty()) Text("A", color = p.line, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
                    }
                }
                // Tableau.
                val tableTop = ch + 14.dp
                for (col in 0 until 7) {
                    val x = gap + (cw + gap) * col
                    val pile = game.tableau[col]
                    if (pile.isEmpty()) PlayingCard(null, cw, ch, s.accent, modifier = Modifier.offset(x, tableTop))
                    var y = tableTop
                    val upStep = if (pile.count { it.up } > 7) ch * 0.2f else ch * 0.28f
                    pile.forEachIndexed { idx, card ->
                        key(card.suit * 13 + card.rank) {
                            val ay by animateDpAsState(y, spring(stiffness = 700f), label = "y")
                            val ax by animateDpAsState(x, spring(stiffness = 700f), label = "x")
                            val isHint = hint?.first.let { it is RoyalPatience.Pile && it.pile == col && it.index == idx }
                            PlayingCard(
                                card, cw, ch, s.accent, highlight = isHint,
                                modifier = Modifier.offset(ax, ay).then(
                                    if (card.up) Modifier.jellyClick { after(game.autoMove(RoyalPatience.Pile(col, idx))) } else Modifier,
                                ),
                            )
                        }
                        y += if (card.up) upStep else faceDownStep
                    }
                }
            }
        }
        ActionRow {
            ActionChip(Icons.AutoMirrored.Rounded.Undo, "Undo", game.canUndo().also { version } && !finishing) { if (game.undo()) { version++; hint = null } }
            ActionChip(Icons.Rounded.Lightbulb, "Hint", !finishing) {
                s.useHint {
                    hints++
                    hint = game.hint()
                    if (hint == null) { game.draw(); version++ } // nothing to move: draw for the player
                }
            }
        }
    }
}
