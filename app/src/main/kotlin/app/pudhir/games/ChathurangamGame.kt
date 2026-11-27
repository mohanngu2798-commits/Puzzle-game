package app.pudhir.games

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pudhir.core.Chathurangam
import app.pudhir.core.Chathurangam.Kind
import app.pudhir.core.Difficulty
import app.pudhir.core.Riddles
import app.pudhir.ui.ActionChip
import app.pudhir.ui.ActionRow
import app.pudhir.ui.LocalPalette
import app.pudhir.ui.jellyClick
import app.pudhir.ui.popIn
import app.pudhir.ui.shake
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun glyph(k: Kind) = when (k) {
    Kind.Raja -> "👑"; Kind.Mantri -> "📜"; Kind.Gaja -> "🐘"; Kind.Ashva -> "🐎"; Kind.Ratha -> "🏰"; Kind.Padati -> "🛡️"
}

@Composable
fun ChathurangamGame(s: PlaySession) {
    val p = LocalPalette.current
    val game = remember { Chathurangam() }
    var version by remember { mutableIntStateOf(0) }
    var selected by remember { mutableIntStateOf(-1) }
    var busy by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf<Chathurangam.Move?>(null) }
    var hints by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("Your move. Tap a piece to see where it can go.") }
    val scope = rememberCoroutineScope()
    val depth = when (s.difficulty) { Difficulty.Easy -> 1; Difficulty.Medium -> 2; Difficulty.Hard -> 3; Difficulty.Expert -> 4 }

    fun end(): Boolean = when (game.result()) {
        0 -> { s.onSolved(hints); true }
        1 -> { s.onGameOver(null, if (game.inCheck(0)) "Checkmate. The phone wins" else "The phone captured your army"); true }
        2 -> { s.onGameOver(null, "It's a draw"); true }
        else -> false
    }

    fun phone() {
        busy = true
        scope.launch {
            delay(350)
            val m = withContext(Dispatchers.Default) { game.bestMove(depth) }
            if (m != null) {
                val took = game.play(m)
                s.haptic(took != null)
                message = when {
                    game.inCheck(0) -> "Your Raja is under attack!"
                    took != null -> "The phone took your ${took.kind.label}"
                    else -> "Your move"
                }
            }
            version++
            end(); busy = false
        }
    }

    fun tap(sq: Int) {
        if (busy || game.turn != 0) return
        val piece = game.board[sq]
        if (piece != null && piece.side == 0) { selected = sq; s.haptic(false); return }
        val m = game.moves().firstOrNull { it.from == selected && it.to == sq } ?: run { if (selected >= 0) s.haptic(true); return }
        val took = game.play(m)
        selected = -1; hint = null; version++
        s.haptic(took != null)
        message = if (took != null) "You took the phone's ${took.kind.label}!" else "The phone is thinking…"
        if (!end()) phone()
    }

    Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        version.let { Text(message, color = p.ink, fontWeight = FontWeight.SemiBold) }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            val side = minOf(maxWidth, maxHeight)
            val cell = side / 8
            Box(Modifier.size(side).shadow(8.dp, RoundedCornerShape(14.dp)).clip(RoundedCornerShape(14.dp))) {
                Canvas(Modifier.fillMaxSize()) {
                    val cs = size.width / 8
                    for (r in 0..7) for (c in 0..7) {
                        drawRect(if ((r + c) % 2 == 0) Color(0xFFF3E3C3) else Color(0xFFC9A06A), Offset(c * cs, r * cs), Size(cs, cs))
                    }
                    // The crossed squares of the old ashtapada board.
                    for ((r, c) in listOf(0 to 0, 0 to 7, 7 to 0, 7 to 7, 3 to 3, 3 to 4, 4 to 3, 4 to 4)) {
                        val o = Offset(c * cs, r * cs); val m = cs * 0.2f
                        drawLine(Color(0x55803A1A), o + Offset(m, m), o + Offset(cs - m, cs - m), 3f)
                        drawLine(Color(0x55803A1A), o + Offset(cs - m, m), o + Offset(m, cs - m), 3f)
                    }
                }
                version.let {
                    val targets = if (selected >= 0) game.moves().filter { it.from == selected }.map { it.to }.toSet() else emptySet()
                    val last = game.lastMove
                    for (sq in 0 until 64) {
                        val r = sq / 8; val c = sq % 8
                        val ring = when {
                            hint != null && (hint!!.from == sq || hint!!.to == sq) -> Color(0xFFF5B83D)
                            sq == selected -> s.accent
                            last != null && (last.from == sq || last.to == sq) -> Color(0x993A86F2)
                            else -> null
                        }
                        Box(
                            Modifier.offset(cell * c, cell * r).size(cell).jellyClick(!busy) { tap(sq) }
                                .border(if (ring != null) 3.dp else 0.dp, ring ?: Color.Transparent),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (sq in targets) Box(Modifier.size(cell * 0.28f).clip(CircleShape).background(s.accent.copy(alpha = 0.55f)))
                            val piece = game.board[sq]
                            if (piece != null) Box(
                                Modifier.size(cell * 0.82f).shadow(3.dp, CircleShape).clip(CircleShape)
                                    .background(Brush.radialGradient(if (piece.side == 0) listOf(Color(0xFFFFC2A3), Color(0xFFE8562C)) else listOf(Color(0xFFB8D4FF), Color(0xFF2F6FD6)))),
                                contentAlignment = Alignment.Center,
                            ) { Text(glyph(piece.kind), fontSize = (cell.value * 0.44f).sp) }
                        }
                    }
                }
            }
        }
        Text("👑 Raja  📜 Mantri  🐘 Gaja  🐎 Ashva  🏰 Ratha  🛡️ Soldier", color = p.muted, fontSize = 12.sp)
        ActionRow {
            ActionChip(Icons.Rounded.Lightbulb, "Hint", !busy) { s.useHint { hints++; hint = game.bestMove(3); selected = hint?.from ?: -1 } }
        }
    }
}

@Composable
fun VidukathaiGame(s: PlaySession) {
    val p = LocalPalette.current
    val count = when (s.difficulty) { Difficulty.Easy -> 4; Difficulty.Medium -> 5; Difficulty.Hard -> 6; Difficulty.Expert -> 8 }
    val round = remember { Riddles.round(s.seed, count) }
    var index by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(2) }
    var picked by remember { mutableStateOf<String?>(null) }
    var shakeKey by remember { mutableIntStateOf(0) }
    var hidden by remember { mutableStateOf(setOf<String>()) }
    var hints by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val (riddle, choices) = round[index]

    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Riddle ${index + 1} of $count  ·  " + "❤️".repeat(lives), color = p.muted)
        Spacer(Modifier.height(18.dp))
        key(index) {
            Box(
                Modifier.fillMaxWidth().popIn().shadow(6.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF4A2C7A), Color(0xFF7B3F8C)))).padding(24.dp),
            ) {
                Text("“${riddle.text}”", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, lineHeight = 30.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("What am I?", color = p.muted, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        Column(Modifier.fillMaxWidth().weight(1f).shake(shakeKey), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for ((k, ch) in choices.withIndex()) {
                if (ch in hidden) continue
                val correct = ch == riddle.answer
                val state = picked
                val bg = when {
                    state == null -> p.surface
                    correct -> Color(0xFF2BA36B)
                    ch == state -> Color(0xFFD62839)
                    else -> p.surface
                }
                key(index, ch) {
                    Box(
                        Modifier.fillMaxWidth().popIn(k * 60).jellyClick(state == null) {
                            picked = ch
                            if (correct) s.haptic(false) else { s.haptic(true); shakeKey++; lives-- }
                            scope.launch {
                                delay(900)
                                when {
                                    lives < 0 -> s.onGameOver(null, "Out of hearts. The answer was ${riddle.answer}")
                                    index == count - 1 -> s.onSolved(hints + (2 - lives))
                                    else -> { index++; picked = null; hidden = emptySet() }
                                }
                            }
                        }.shadow(3.dp, RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp)).background(bg).padding(vertical = 16.dp, horizontal = 18.dp),
                    ) {
                        Text(ch, color = if (state != null && (correct || ch == state)) Color.White else p.ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        ActionRow {
            ActionChip(Icons.Rounded.Lightbulb, "Hint", picked == null && hidden.size < 2) {
                s.useHint { hints++; hidden = hidden + choices.filter { it != riddle.answer && it !in hidden }.take(1) }
            }
        }
    }
}
