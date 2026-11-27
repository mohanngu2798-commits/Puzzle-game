package app.pudhir.games

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pudhir.core.Difficulty
import app.pudhir.core.Pallanguzhi
import app.pudhir.ui.ActionChip
import app.pudhir.ui.ActionRow
import app.pudhir.ui.LocalPalette
import app.pudhir.ui.jellyClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

private val Wood = Color(0xFF8B5A2B)
private val WoodDark = Color(0xFF5E3A17)
private val PitColour = Color(0xFF4A2C12)

fun aiDepth(d: Difficulty) = when (d) { Difficulty.Easy -> 1; Difficulty.Medium -> 2; Difficulty.Hard -> 3; Difficulty.Expert -> 4 }

@Composable
fun PallanguzhiGame(s: PlaySession) {
    val p = LocalPalette.current
    val game = remember { Pallanguzhi().also { it.recordTrace = true } }
    // What the board shows; it steps through each seed as it is sown.
    var shown by remember { mutableStateOf(game.pits.copyOf()) }
    var active by remember { mutableIntStateOf(-1) }
    var busy by remember { mutableStateOf(false) }
    var hintPit by remember { mutableIntStateOf(-1) }
    var hints by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("Your turn: tap a pit on your side") }
    var version by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    suspend fun animate(before: IntArray) {
        val pathIt = game.lastPath.iterator()
        shown = before
        for (snap in game.lastTrace) {
            // Highlight the pit that changed.
            active = (0 until 14).firstOrNull { snap[it] != shown[it] } ?: active
            shown = snap
            s.haptic(false)
            delay(150)
        }
        if (pathIt.hasNext()) Unit
        active = -1
        shown = game.pits.copyOf()
        version++
    }

    fun finish() {
        val w = game.winner()
        if (w == 0) s.onSolved(hints)
        else s.onGameOver(null, if (w == 1) "The phone wins ${game.store[1]} to ${game.store[0]}" else "It's a draw, ${game.store[0]} each")
    }

    suspend fun phoneTurn() {
        while (!game.isOver() && game.turn == 1) {
            message = "The phone is thinking…"
            delay(500)
            val m = withContext(Dispatchers.Default) {
                val c = game.copy()
                if (s.difficulty == Difficulty.Easy && Random.nextInt(3) == 0) c.legal().random() else c.bestMove(aiDepth(s.difficulty))
            } ?: break
            val before = game.pits.copyOf()
            game.play(m)
            message = if (game.lastCapture >= 0) "The phone captured seeds" else "The phone sowed"
            animate(before)
        }
    }

    fun tap(pit: Int) {
        if (busy || game.turn != 0 || game.owner(pit) != 0 || game.pits[pit] == 0) { if (!busy) s.haptic(true); return }
        busy = true; hintPit = -1
        scope.launch {
            val before = game.pits.copyOf()
            game.play(pit)
            animate(before)
            if (game.lastCapture >= 0) { message = "You captured seeds!"; s.haptic(true) }
            if (!game.isOver()) phoneTurn()
            if (game.isOver()) { delay(400); finish() } else message = "Your turn: tap a pit on your side"
            busy = false
        }
    }

    Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, color = p.muted)
        Spacer(Modifier.height(10.dp))
        version.let { Score("Phone", game.store[1], Color(0xFF3A86F2)) }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            val pit = minOf((maxWidth - 32.dp) / 7, maxHeight / 3.2f)
            Column(
                Modifier.shadow(10.dp, RoundedCornerShape(28.dp)).clip(RoundedCornerShape(28.dp))
                    .background(Brush.verticalGradient(listOf(Wood, WoodDark))).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Phone's row, shown right to left so sowing runs anticlockwise.
                Row { for (i in 13 downTo 7) Pit(shown[i], pit, i == active, false, false) {} }
                Box(Modifier.fillMaxWidth().height(2.dp).background(Color.White.copy(alpha = 0.15f)))
                Row { for (i in 0..6) Pit(shown[i], pit, i == active, i == hintPit, !busy && game.turn == 0 && shown[i] > 0) { tap(i) } }
            }
        }
        version.let { Score("You", game.store[0], s.accent) }
        ActionRow {
            ActionChip(Icons.Rounded.Lightbulb, "Hint", !busy && game.turn == 0) {
                s.useHint { hints++; hintPit = game.copy().bestMove(4) ?: -1 }
            }
        }
    }
}

@Composable
private fun Score(who: String, n: Int, c: Color) {
    val p = LocalPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(c))
        Spacer(Modifier.width(6.dp))
        Text("$who  ", color = p.muted)
        Text("$n seeds", color = p.ink, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
    }
}

@Composable
private fun Pit(count: Int, size: Dp, active: Boolean, hinted: Boolean, playable: Boolean, onTap: () -> Unit) {
    val glow by animateFloatAsState(if (active) 1f else 0f, spring(stiffness = 600f), label = "pit")
    Box(
        Modifier.size(size).padding(3.dp).jellyClick(playable) { onTap() }
            .graphicsLayer { scaleX = 1f + glow * 0.08f; scaleY = scaleX }
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(PitColour, Color(0xFF2E1A0A))))
            .border(if (hinted) 3.dp else if (active) 2.dp else 0.dp, if (hinted) Color(0xFFF5B83D) else Color(0xFFFFE2B8).copy(alpha = glow), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        // Seeds as little cowries, up to 12 drawn; the number shows the exact count.
        Canvas(Modifier.fillMaxSize().padding(size * 0.14f)) {
            val r = this.size.minDimension
            val rnd = Random(count * 7 + 3)
            for (k in 0 until minOf(count, 12)) {
                val a = rnd.nextFloat() * 6.283f; val d = rnd.nextFloat() * r * 0.3f
                val c = Offset(center.x + d * kotlin.math.cos(a), center.y + d * kotlin.math.sin(a))
                drawOval(Color(0xFFF7E7C6), topLeft = c - Offset(r * 0.09f, r * 0.065f), size = androidx.compose.ui.geometry.Size(r * 0.18f, r * 0.13f))
            }
        }
        if (count > 0) Text("$count", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = (size.value * 0.26f).sp,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = size * 0.1f))
    }
}
