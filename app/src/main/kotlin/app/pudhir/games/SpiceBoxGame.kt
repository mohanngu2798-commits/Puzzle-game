package app.pudhir.games

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lightbulb
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pudhir.core.SudokuPuzzle
import app.pudhir.ui.ActionChip
import app.pudhir.ui.ActionRow
import app.pudhir.ui.LocalPalette
import app.pudhir.ui.bouncyClick
import app.pudhir.ui.jellyClick
import app.pudhir.ui.lighten
import app.pudhir.ui.shake

@Composable
fun SpiceBoxGame(s: PlaySession) {
    val p = LocalPalette.current
    val puzzle = remember { SudokuPuzzle.generate(s.seed, s.difficulty) }
    val values = remember { mutableStateListOf(*puzzle.givens.toTypedArray()) }
    val notes = remember { mutableStateListOf(*Array(81) { 0 }) }
    val history = remember { mutableStateListOf<Triple<Int, Int, Int>>() } // pos, oldValue, oldNotes
    var selected by remember { mutableIntStateOf(-1) }
    var noteMode by remember { mutableStateOf(false) }
    var hints by remember { mutableIntStateOf(0) }
    var mistakes by remember { mutableIntStateOf(0) }
    var shakeKey by remember { mutableIntStateOf(0) }

    fun checkWin() { if ((0 until 81).all { values[it] == puzzle.solution[it] }) s.onSolved(hints + mistakes / 2) }

    fun enter(d: Int) {
        val pos = selected
        if (pos < 0 || puzzle.givens[pos] != 0) return
        if (noteMode) {
            history += Triple(pos, values[pos], notes[pos])
            notes[pos] = notes[pos] xor (1 shl d)
            s.haptic(false)
            return
        }
        history += Triple(pos, values[pos], notes[pos])
        values[pos] = if (values[pos] == d) 0 else d
        notes[pos] = 0
        if (values[pos] != 0 && values[pos] != puzzle.solution[pos]) { mistakes++; shakeKey++; s.haptic(true) }
        else {
            s.haptic(false)
            // Clear this digit from notes in the same row, column and box.
            val r = pos / 9; val c = pos % 9
            for (q in 0 until 81) {
                val rr = q / 9; val cc = q % 9
                if (rr == r || cc == c || (rr / 3 == r / 3 && cc / 3 == c / 3)) notes[q] = notes[q] and (1 shl d).inv()
            }
        }
        checkWin()
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(8.dp))
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            val side = minOf(maxWidth, maxHeight)
            Box(
                Modifier.size(side).shake(shakeKey).shadow(6.dp, RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp)).background(p.surface),
            ) {
                val selR = if (selected >= 0) selected / 9 else -1
                val selC = if (selected >= 0) selected % 9 else -1
                val selV = if (selected >= 0) values[selected] else 0
                Column(Modifier.fillMaxSize()) {
                    for (r in 0 until 9) Row(Modifier.weight(1f)) {
                        for (c in 0 until 9) {
                            val pos = r * 9 + c
                            val v = values[pos]
                            val given = puzzle.givens[pos] != 0
                            val related = selected >= 0 && (r == selR || c == selC || (r / 3 == selR / 3 && c / 3 == selC / 3))
                            val bg = when {
                                pos == selected -> lighten(s.accent, if (p.dark) 0.1f else 0.6f)
                                selV != 0 && v == selV -> lighten(s.accent, if (p.dark) 0.3f else 0.78f)
                                related -> if (p.dark) p.surfaceAlt else lighten(s.accent, 0.92f)
                                else -> Color.Transparent
                            }
                            val scale by animateFloatAsState(if (v != 0) 1f else 0.4f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 500f), label = "pop")
                            Box(
                                Modifier.weight(1f).fillMaxSize().background(bg).clickable { selected = pos },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (v != 0) {
                                    val wrong = v != puzzle.solution[pos]
                                    Text(
                                        "$v",
                                        fontSize = 22.sp,
                                        fontWeight = if (given) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = when { given -> p.ink; wrong -> p.bad; else -> s.accent },
                                        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
                                    )
                                } else if (notes[pos] != 0) {
                                    Column(Modifier.fillMaxSize().padding(2.dp)) {
                                        for (nr in 0 until 3) Row(Modifier.weight(1f)) {
                                            for (nc in 0 until 3) {
                                                val d = nr * 3 + nc + 1
                                                Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                                                    if (notes[pos] and (1 shl d) != 0) Text("$d", fontSize = 9.sp, color = p.muted)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // Grid lines on top.
                Canvas(Modifier.fillMaxSize()) {
                    val cell = size.width / 9
                    for (i in 1 until 9) {
                        val thick = i % 3 == 0
                        val col = if (thick) p.ink.copy(alpha = 0.55f) else p.line
                        val w = if (thick) 2.dp.toPx() else 1.dp.toPx()
                        drawLine(col, Offset(i * cell, 0f), Offset(i * cell, size.height), w)
                        drawLine(col, Offset(0f, i * cell), Offset(size.width, i * cell), w)
                    }
                }
            }
        }
        ActionRow {
            ActionChip(Icons.AutoMirrored.Rounded.Undo, "Undo", history.isNotEmpty()) {
                val (pos, v, n) = history.removeAt(history.lastIndex)
                values[pos] = v; notes[pos] = n
            }
            ActionChip(Icons.Rounded.Backspace, "Erase") {
                if (selected >= 0 && puzzle.givens[selected] == 0) {
                    history += Triple(selected, values[selected], notes[selected]); values[selected] = 0; notes[selected] = 0
                }
            }
            ActionChip(Icons.Rounded.Edit, if (noteMode) "Notes on" else "Notes", badge = if (noteMode) "✓" else null) { noteMode = !noteMode }
            ActionChip(Icons.Rounded.Lightbulb, "Hint") { s.useHint {
                val target = if (selected >= 0 && values[selected] != puzzle.solution[selected]) selected
                else (0 until 81).firstOrNull { values[it] != puzzle.solution[it] } ?: -1
                if (target >= 0) {
                    history += Triple(target, values[target], notes[target])
                    values[target] = puzzle.solution[target]; notes[target] = 0; selected = target; hints++
                    s.haptic(false); checkWin()
                }
            } }
        }
        // Number pad with how many of each digit are still to place.
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (d in 1..9) {
                val left = 9 - values.count { it == d }
                Column(
                    Modifier.weight(1f).aspectRatio(0.72f).jellyClick(left > 0) { enter(d) }.shadow(2.dp, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp))
                        .background(if (left == 0) p.surfaceAlt else p.surface),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                ) {
                    Text("$d", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = if (left == 0) p.muted else s.accent)
                    Text("$left", fontSize = 10.sp, color = p.muted)
                }
            }
        }
    }
}
