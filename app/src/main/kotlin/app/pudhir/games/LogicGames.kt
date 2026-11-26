package app.pudhir.games

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import app.pudhir.core.BloomPuzzle
import app.pudhir.core.TeaEstatePuzzle
import app.pudhir.ui.ActionChip
import app.pudhir.ui.ActionRow
import app.pudhir.ui.LocalPalette
import app.pudhir.ui.darken
import app.pudhir.ui.lighten
import app.pudhir.ui.shake
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** A five-petal flower drawn in [colour], used by Bloom and Tea Estate. */
fun DrawScope.flower(centre: Offset, radius: Float, petal: Color, heart: Color) {
    for (k in 0 until 5) {
        val a = k * 2 * Math.PI / 5 - Math.PI / 2
        drawCircle(petal, radius * 0.42f, Offset(centre.x + cos(a).toFloat() * radius * 0.5f, centre.y + sin(a).toFloat() * radius * 0.5f))
    }
    drawCircle(heart, radius * 0.32f, centre)
}

private val Jasmine = Color(0xFFFFFFFF)
private val Marigold = Color(0xFFF5A623)

/** Square board with cells laid out by the caller; shared look for the logic games. */
@Composable
fun LogicBoard(cells: Int, shakeKey: Int, content: @Composable (cellSize: androidx.compose.ui.unit.Dp) -> Unit) {
    val p = LocalPalette.current
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, maxHeight)
        Box(Modifier.size(side).shake(shakeKey).shadow(6.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp)).background(p.surface).padding(6.dp)) {
            content((side - 12.dp) / cells)
        }
    }
}

@Composable
fun BloomGame(s: PlaySession) = Generated(s, { BloomPuzzle.generate(s.seed, s.difficulty) }) { puzzle -> BloomBoard(s, puzzle) }

@Composable
private fun BloomBoard(s: PlaySession, puzzle: BloomPuzzle) {
    val p = LocalPalette.current
    val n = puzzle.size
    val g = remember { mutableStateListOf(*puzzle.givens.toTypedArray()) }
    val history = remember { mutableStateListOf<Pair<Int, Int>>() }
    var hints by remember { mutableIntStateOf(0) }
    var shakeKey by remember { mutableIntStateOf(0) }

    fun broken(pos: Int): Boolean {
        val v = g[pos]; if (v == 0) return false
        val r = pos / n; val c = pos % n
        for (s0 in -2..0) {
            val c0 = c + s0
            if (c0 >= 0 && c0 + 2 < n && (0..2).all { g[r * n + c0 + it] == v }) return true
            val r0 = r + s0
            if (r0 >= 0 && r0 + 2 < n && (0..2).all { g[(r0 + it) * n + c] == v }) return true
        }
        if ((0 until n).count { g[r * n + it] == v } > n / 2) return true
        if ((0 until n).count { g[it * n + c] == v } > n / 2) return true
        return false
    }

    fun set(pos: Int, v: Int) {
        history += pos to g[pos]; g[pos] = v; s.haptic(false)
        if (broken(pos)) { shakeKey++; s.haptic(true) }
        if ((0 until n * n).all { g[it] == puzzle.solution[it] }) s.onSolved(hints)
    }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Equal jasmine and marigold in every line. Never three in a row.", color = p.muted)
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            LogicBoard(n, shakeKey) { cs ->
                Column {
                    for (r in 0 until n) Row {
                        for (c in 0 until n) {
                            val pos = r * n + c
                            val v = g[pos]
                            val given = puzzle.givens[pos] != 0
                            val pop by animateFloatAsState(if (v != 0) 1f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 600f), label = "pop")
                            val bad = broken(pos)
                            Box(
                                Modifier.size(cs).padding(2.dp).clip(RoundedCornerShape(10.dp))
                                    .background(if (bad) lighten(p.bad, 0.8f) else if (given) p.surfaceAlt else lighten(Color(0xFF6BBF59), if (p.dark) 0.1f else 0.85f))
                                    .clickable(enabled = !given) { set(pos, (v + 1) % 3) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Canvas(Modifier.fillMaxSize().graphicsLayer { scaleX = pop; scaleY = pop }) {
                                    if (v == 1) { drawCircle(Color(0x22000000), size.minDimension * 0.36f, center + Offset(0f, 2f)); flower(center, size.minDimension * 0.4f, Jasmine, Color(0xFFF7D774)) }
                                    if (v == 2) flower(center, size.minDimension * 0.4f, Marigold, darken(Marigold, 0.3f))
                                }
                            }
                        }
                    }
                }
            }
        }
        ActionRow {
            ActionChip(Icons.AutoMirrored.Rounded.Undo, "Undo", history.isNotEmpty()) { val (pos, v) = history.removeAt(history.lastIndex); g[pos] = v }
            ActionChip(Icons.Rounded.RestartAlt, "Restart") { for (i in 0 until n * n) g[i] = puzzle.givens[i]; history.clear() }
            ActionChip(Icons.Rounded.Lightbulb, "Hint") {
                s.useHint {
                    val pos = (0 until n * n).firstOrNull { g[it] != 0 && g[it] != puzzle.solution[it] } ?: (0 until n * n).firstOrNull { g[it] == 0 }
                    if (pos != null) { hints++; set(pos, puzzle.solution[pos]) }
                }
            }
        }
    }
}

private val PlotColours = listOf(
    Color(0xFFCDE7B0), Color(0xFFF9D8A7), Color(0xFFBFD7EA), Color(0xFFF4C2C2), Color(0xFFD9C8F0),
    Color(0xFFFFF1A8), Color(0xFFB8E0D2), Color(0xFFE8D5B7), Color(0xFFC9D6DF),
)

@Composable
fun TeaEstateGame(s: PlaySession) = Generated(s, { TeaEstatePuzzle.generate(s.seed, s.difficulty) }) { puzzle -> TeaBoard(s, puzzle) }

@Composable
private fun TeaBoard(s: PlaySession, puzzle: TeaEstatePuzzle) {
    val p = LocalPalette.current
    val n = puzzle.size
    // 0 empty, 1 marked as no bush, 2 bush.
    val g = remember { mutableStateListOf(*Array(n * n) { 0 }) }
    val history = remember { mutableStateListOf<Pair<Int, Int>>() }
    var hints by remember { mutableIntStateOf(0) }
    var shakeKey by remember { mutableIntStateOf(0) }

    fun clash(pos: Int): Boolean {
        if (g[pos] != 2) return false
        val r = pos / n; val c = pos % n
        for (q in 0 until n * n) {
            if (q == pos || g[q] != 2) continue
            val rr = q / n; val cc = q % n
            if (rr == r || cc == c || puzzle.regions[q] == puzzle.regions[pos] || (abs(rr - r) <= 1 && abs(cc - c) <= 1)) return true
        }
        return false
    }

    fun set(pos: Int, v: Int) {
        history += pos to g[pos]; g[pos] = v
        s.haptic(v == 2)
        if (clash(pos)) shakeKey++
        val bushes = (0 until n * n).filter { g[it] == 2 }
        if (bushes.size == n && bushes.all { puzzle.solution[it / n] == it % n }) s.onSolved(hints)
    }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("One bush per row, column and plot. Bushes never touch.", color = p.muted, modifier = Modifier.padding(8.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            LogicBoard(n, shakeKey) { cs ->
                Column {
                    for (r in 0 until n) Row {
                        for (c in 0 until n) {
                            val pos = r * n + c
                            val reg = puzzle.regions[pos]
                            val base = PlotColours[reg % PlotColours.size].let { if (p.dark) darken(it, 0.45f) else it }
                            val v = g[pos]
                            val pop by animateFloatAsState(if (v == 2) 1f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 600f), label = "bush")
                            // Thick borders between different plots.
                            val top = r == 0 || puzzle.regions[pos - n] != reg
                            val left = c == 0 || puzzle.regions[pos - 1] != reg
                            val bottom = r == n - 1 || puzzle.regions[pos + n] != reg
                            val right = c == n - 1 || puzzle.regions[pos + 1] != reg
                            Box(
                                Modifier.size(cs).background(base).clickable { set(pos, (v + 1) % 3) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Canvas(Modifier.fillMaxSize()) {
                                    val w = 2.5.dp.toPx(); val thin = 0.5.dp.toPx()
                                    val line = p.ink.copy(alpha = 0.7f); val faint = p.ink.copy(alpha = 0.12f)
                                    drawLine(if (top) line else faint, Offset(0f, 0f), Offset(size.width, 0f), if (top) w else thin)
                                    drawLine(if (left) line else faint, Offset(0f, 0f), Offset(0f, size.height), if (left) w else thin)
                                    if (bottom) drawLine(line, Offset(0f, size.height), Offset(size.width, size.height), w)
                                    if (right) drawLine(line, Offset(size.width, 0f), Offset(size.width, size.height), w)
                                    if (v == 1) drawCircle(p.ink.copy(alpha = 0.35f), size.minDimension * 0.07f, center)
                                    if (pop > 0f) {
                                        val rad = size.minDimension * 0.36f * pop
                                        val leaf = if (clash(pos)) p.bad else Color(0xFF2E8B57)
                                        drawCircle(darken(leaf, 0.2f), rad, center + Offset(0f, rad * 0.15f))
                                        drawCircle(leaf, rad * 0.9f, center)
                                        drawCircle(lighten(leaf, 0.35f), rad * 0.35f, center + Offset(-rad * 0.3f, -rad * 0.3f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Text("Tap once to mark empty, twice to plant a bush.", color = p.muted, modifier = Modifier.padding(top = 4.dp))
        ActionRow {
            ActionChip(Icons.AutoMirrored.Rounded.Undo, "Undo", history.isNotEmpty()) { val (pos, v) = history.removeAt(history.lastIndex); g[pos] = v }
            ActionChip(Icons.Rounded.RestartAlt, "Restart") { for (i in 0 until n * n) g[i] = 0; history.clear() }
            ActionChip(Icons.Rounded.Lightbulb, "Hint") {
                s.useHint {
                    // Remove a wrong bush, else plant a right one.
                    val wrong = (0 until n * n).firstOrNull { g[it] == 2 && puzzle.solution[it / n] != it % n }
                    val pos = wrong ?: (0 until n).map { it * n + puzzle.solution[it] }.firstOrNull { g[it] != 2 }
                    if (pos != null) { hints++; set(pos, if (wrong != null) 1 else 2) }
                }
            }
        }
    }
}
