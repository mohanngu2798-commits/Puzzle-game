package app.pudhir.games

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pudhir.core.AaduPuli
import app.pudhir.core.AaduPuli.Piece
import app.pudhir.ui.ActionChip
import app.pudhir.ui.ActionRow
import app.pudhir.ui.LocalPalette
import app.pudhir.ui.jellyClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AaduPuliGame(s: PlaySession) {
    val p = LocalPalette.current
    val game = remember { AaduPuli.create() }
    var version by remember { mutableIntStateOf(0) }
    var selected by remember { mutableIntStateOf(-1) }
    var busy by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf<AaduPuli.Move?>(null) }
    var hints by remember { mutableIntStateOf(0) }
    var lastTiger by remember { mutableStateOf<AaduPuli.Move?>(null) }
    val scope = rememberCoroutineScope()

    fun end(): Boolean {
        if (game.goatsWin()) { s.onSolved(hints); return true }
        if (game.tigersWin()) { s.onGameOver(null, "The tigers caught ${game.captured} goats"); return true }
        if (game.goatsTurn && game.goatMoves().isEmpty()) { s.onGameOver(null, "Your goats are stuck"); return true }
        return false
    }

    fun afterGoat() {
        busy = true; hint = null; selected = -1; version++
        scope.launch {
            if (!end()) {
                delay(450)
                val m = withContext(Dispatchers.Default) { game.copy().bestMove(aiDepth(s.difficulty) + 1) }
                if (m != null) {
                    game.apply(m); lastTiger = m
                    s.haptic(m is AaduPuli.Jump)
                }
                version++
                end()
            }
            busy = false
        }
    }

    fun tap(i: Int) {
        if (busy || !game.goatsTurn) return
        val moves = game.goatMoves()
        if (game.goatsToPlace > 0) {
            val m = moves.firstOrNull { it is AaduPuli.Place && it.at == i } ?: run { s.haptic(true); return }
            game.apply(m); s.haptic(false); afterGoat(); return
        }
        if (game.board[i] == Piece.Goat) { selected = if (selected == i) -1 else i; s.haptic(false); return }
        val m = moves.firstOrNull { it is AaduPuli.Step && it.from == selected && it.to == i }
        if (m == null) { s.haptic(true); return }
        game.apply(m); s.haptic(false); afterGoat()
    }

    Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        version.let {
            val phase = if (game.goatsToPlace > 0) "Place a goat · ${game.goatsToPlace} left" else "Move a goat to trap the tigers"
            Text(phase, color = p.ink, fontWeight = FontWeight.SemiBold)
            Text("Goats caught: ${game.captured} of ${game.captureToWin}", color = if (game.captured > 3) p.bad else p.muted, fontSize = 13.sp)
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
            val w = minOf(maxWidth, maxHeight / 1.02f)
            val h = w * 1.0f
            val dot = w * 0.11f
            Box(
                Modifier.size(w, h).shadow(8.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp))
                    .background(if (p.dark) Color(0xFF2A2418) else Color(0xFFF4E6C8)),
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val line = if (p.dark) Color(0xFF8C7A55) else Color(0xFF9C7B45)
                    for ((a, b) in game.edges) {
                        val pa = game.points[a]; val pb = game.points[b]
                        drawLine(line, Offset(pa.first * size.width, pa.second * size.height), Offset(pb.first * size.width, pb.second * size.height), 3.dp.toPx(), StrokeCap.Round)
                    }
                }
                version.let {
                    val targets = if (selected >= 0) game.goatMoves().filterIsInstance<AaduPuli.Step>().filter { it.from == selected }.map { it.to }.toSet() else emptySet()
                    for (i in game.points.indices) {
                        val (x, y) = game.points[i]
                        val piece = game.board[i]
                        val isHint = hint.let { (it is AaduPuli.Place && it.at == i) || (it is AaduPuli.Step && (it.from == i || it.to == i)) }
                        val fresh = lastTiger.let { (it is AaduPuli.Step && it.to == i) || (it is AaduPuli.Jump && it.to == i) }
                        val pop by animateFloatAsState(if (selected == i) 1.15f else 1f, spring(dampingRatio = 0.4f, stiffness = 500f), label = "sel")
                        Box(
                            Modifier.offset(w * x - dot / 2, h * y - dot / 2).size(dot)
                                .jellyClick(!busy) { tap(i) }
                                .graphicsLayer { scaleX = pop; scaleY = pop }
                                .clip(CircleShape)
                                .background(
                                    when (piece) {
                                        Piece.Empty -> if (i in targets) s.accent.copy(alpha = 0.35f) else (if (p.dark) Color(0xFF4A4030) else Color(0xFFE4CFA4))
                                        Piece.Goat -> Color.White
                                        Piece.Tiger -> Color(0xFFF5A623)
                                    },
                                )
                                .border(
                                    if (isHint || selected == i || fresh) 3.dp else 1.dp,
                                    when { isHint -> Color(0xFFF5B83D); selected == i -> s.accent; fresh -> Color(0xFFD62839); else -> Color.Black.copy(alpha = 0.15f) },
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (piece != Piece.Empty) Text(if (piece == Piece.Goat) "🐐" else "🐯", fontSize = (dot.value * 0.55f).sp)
                        }
                    }
                }
            }
        }
        Text("Tigers jump over goats to catch them. Block every tiger to win.", color = p.muted, fontSize = 13.sp)
        ActionRow {
            ActionChip(Icons.Rounded.Lightbulb, "Hint", !busy) { s.useHint { hints++; hint = game.copy().bestMove(3) } }
        }
    }
}
