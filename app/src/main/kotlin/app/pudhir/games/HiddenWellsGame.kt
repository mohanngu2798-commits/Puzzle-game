package app.pudhir.games

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.TouchApp
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pudhir.core.HiddenWells
import app.pudhir.ui.ActionChip
import app.pudhir.ui.ActionRow
import app.pudhir.ui.LocalPalette
import app.pudhir.ui.lighten
import app.pudhir.ui.shake

private val NumberColours = listOf(
    Color(0xFF3A86F2), Color(0xFF2BA36B), Color(0xFFE5484D), Color(0xFF6A4C93),
    Color(0xFFB5763B), Color(0xFF1FB5C9), Color(0xFF1C1B22), Color(0xFF77727F),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HiddenWellsGame(s: PlaySession) {
    val p = LocalPalette.current
    val spec = remember { HiddenWells.specFor(s.difficulty) }
    var board by remember { mutableStateOf<HiddenWells?>(null) }
    val open = remember { BooleanArray(spec.rows * spec.cols) }
    val flags = remember { BooleanArray(spec.rows * spec.cols) }
    var version by remember { mutableIntStateOf(0) }
    var flagMode by remember { mutableStateOf(false) }
    var hints by remember { mutableIntStateOf(0) }
    var lost by remember { mutableIntStateOf(-1) }
    var shakeKey by remember { mutableIntStateOf(0) }

    fun reveal(pos: Int) {
        if (lost >= 0) return
        if (board == null) board = HiddenWells.generate(s.seed, s.difficulty, pos)
        val b = board!!
        if (flags[pos] || open[pos]) return
        if (!b.open(pos, open)) {
            lost = pos; shakeKey++; s.haptic(true); version++
            s.onGameOver(null, "You found a hidden well!")
            return
        }
        s.haptic(false); version++
        if (b.isCleared(open)) s.onSolved(hints)
    }

    fun toggleFlag(pos: Int) {
        if (open[pos] || lost >= 0) return
        flags[pos] = !flags[pos]; s.haptic(true); version++
    }

    Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        val flagsUsed = flags.count { it }.also { version }
        Text("${spec.wells - flagsUsed} wells hidden · " + if (board == null) "Tap any square to start" else "Long-press to flag", color = p.muted)
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            val cs = minOf(maxWidth / spec.cols, maxHeight / spec.rows)
            Column(Modifier.shake(shakeKey).clip(RoundedCornerShape(16.dp)).background(p.surfaceAlt).padding(3.dp)) {
                for (r in 0 until spec.rows) Row {
                    for (c in 0 until spec.cols) {
                        val pos = r * spec.cols + c
                        val isOpen = open[pos].also { version }
                        val flip by animateFloatAsState(if (isOpen) 1f else 0f, tween(220, delayMillis = 0), label = "flip")
                        val b = board
                        Box(
                            Modifier.size(cs).padding(1.5.dp)
                                .graphicsLayer { rotationY = (1 - flip) * 0f; scaleX = if (isOpen) 0.9f + 0.1f * flip else 1f; scaleY = scaleX }
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        lost == pos -> p.bad
                                        isOpen -> p.surface
                                        else -> lighten(Color(0xFF2BA36B), if (p.dark) 0.05f else 0.55f)
                                    },
                                )
                                .combinedClickable(
                                    onClick = { if (flagMode) toggleFlag(pos) else reveal(pos) },
                                    onLongClick = { toggleFlag(pos) },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            when {
                                lost >= 0 && b != null && b.wells[pos] -> Text("💧", fontSize = (cs.value * 0.5f).sp)
                                flags[pos] -> Text("🚩", fontSize = (cs.value * 0.45f).sp)
                                isOpen && b != null && b.counts[pos] > 0 -> Text(
                                    "${b.counts[pos]}", fontWeight = FontWeight.ExtraBold, fontSize = (cs.value * 0.48f).sp,
                                    color = NumberColours[b.counts[pos] - 1],
                                )
                            }
                        }
                    }
                }
            }
        }
        ActionRow {
            ActionChip(if (flagMode) Icons.Rounded.Flag else Icons.Rounded.TouchApp, if (flagMode) "Flagging" else "Digging") { flagMode = !flagMode }
            ActionChip(Icons.Rounded.Lightbulb, "Hint", board != null && lost < 0) {
                s.useHint { board?.hint(open, flags)?.let { hints++; flags[it] = false; reveal(it) } }
            }
        }
    }
}
