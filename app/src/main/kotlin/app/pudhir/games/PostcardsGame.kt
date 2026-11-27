package app.pudhir.games

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import app.pudhir.core.Postcards
import app.pudhir.ui.ActionChip
import app.pudhir.ui.ActionRow
import app.pudhir.ui.LocalPalette
import app.pudhir.ui.bouncyClick
import app.pudhir.ui.jellyClick
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

val SceneNames = listOf("Temple at dusk", "Backwaters", "Moonlit peaks", "Mandala", "Desert dunes", "Lighthouse night", "Tea hills", "Lotus pond")

/** Paints one of the postcard scenes at full [w] x [h]. */
fun DrawScope.drawScene(scene: Int, w: Float, h: Float) {
    fun hills(baseY: Float, amp: Float, col: Color, phase: Float, waves: Float = 2f) {
        val path = Path().apply {
            moveTo(0f, h)
            var x = 0f
            while (x <= w) { lineTo(x, baseY + sin(x / w * Math.PI.toFloat() * waves + phase) * amp); x += w / 40 }
            lineTo(w, h); close()
        }
        drawPath(path, col)
    }
    when (scene) {
        0 -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF2D1B4E), Color(0xFFE5487A), Color(0xFFF7B267)), 0f, h * 0.7f), size = Size(w, h))
            drawCircle(Color(0xFFFFE29A), w * 0.14f, Offset(w * 0.68f, h * 0.48f))
            hills(h * 0.68f, h * 0.04f, Color(0xFF6B2D5C), 0.5f)
            // Temple tower silhouette.
            val t = Path().apply {
                moveTo(w * 0.22f, h * 0.75f); lineTo(w * 0.26f, h * 0.38f); lineTo(w * 0.3f, h * 0.3f); lineTo(w * 0.34f, h * 0.38f); lineTo(w * 0.38f, h * 0.75f); close()
            }
            drawPath(t, Color(0xFF2A1033))
            drawCircle(Color(0xFF2A1033), w * 0.015f, Offset(w * 0.3f, h * 0.28f))
            hills(h * 0.82f, h * 0.03f, Color(0xFF2A1033), 1.4f, 3f)
        }
        1 -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF9AD0EC), Color(0xFFE8F6EF)), 0f, h * 0.55f), size = Size(w, h))
            drawRect(Brush.verticalGradient(listOf(Color(0xFF3FA796), Color(0xFF1B6B73)), h * 0.55f, h), Offset(0f, h * 0.55f), Size(w, h * 0.45f))
            for (k in 0 until 4) {
                val x = w * (0.1f + k * 0.26f); val top = h * (0.25f + (k % 2) * 0.06f)
                drawLine(Color(0xFF5B4636), Offset(x, h * 0.58f), Offset(x + w * 0.04f, top), w * 0.018f)
                for (a in 0 until 6) {
                    val ang = a * Math.PI / 3
                    drawLine(Color(0xFF2E7D32), Offset(x + w * 0.04f, top), Offset(x + w * 0.04f + cos(ang).toFloat() * w * 0.09f, top + sin(ang).toFloat() * h * 0.04f), w * 0.02f)
                }
            }
            val boat = Path().apply { moveTo(w * 0.35f, h * 0.72f); lineTo(w * 0.7f, h * 0.72f); lineTo(w * 0.62f, h * 0.78f); lineTo(w * 0.42f, h * 0.78f); close() }
            drawPath(boat, Color(0xFF5B3A29))
            drawRect(Color(0xFFF2C94C), Offset(w * 0.47f, h * 0.66f), Size(w * 0.12f, h * 0.06f))
        }
        2 -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF0B1D3A), Color(0xFF3A5A8C))), size = Size(w, h))
            val rng = Random(2); repeat(40) { drawCircle(Color.White.copy(alpha = 0.7f), w * 0.004f, Offset(rng.nextFloat() * w, rng.nextFloat() * h * 0.5f)) }
            drawCircle(Color(0xFFF4F1DE), w * 0.1f, Offset(w * 0.25f, h * 0.2f))
            val m = Path().apply { moveTo(0f, h * 0.75f); lineTo(w * 0.3f, h * 0.35f); lineTo(w * 0.55f, h * 0.65f); lineTo(w * 0.78f, h * 0.3f); lineTo(w, h * 0.6f); lineTo(w, h); lineTo(0f, h); close() }
            drawPath(m, Color(0xFF52648C))
            val snow = Path().apply { moveTo(w * 0.24f, h * 0.43f); lineTo(w * 0.3f, h * 0.35f); lineTo(w * 0.36f, h * 0.43f); close(); moveTo(w * 0.72f, h * 0.38f); lineTo(w * 0.78f, h * 0.3f); lineTo(w * 0.84f, h * 0.38f); close() }
            drawPath(snow, Color.White)
            hills(h * 0.85f, h * 0.03f, Color(0xFF243B55), 0.3f, 3f)
        }
        3 -> {
            drawRect(Color(0xFF3B0D3F), size = Size(w, h))
            val c = Offset(w / 2, h / 2)
            val cols = listOf(Color(0xFFF26B3A), Color(0xFFF5B83D), Color(0xFFE5487A), Color(0xFF2BB673), Color(0xFF3A86F2), Color(0xFFFFFFFF))
            for (ring in 6 downTo 1) {
                val rad = w * 0.075f * ring
                val petals = 6 + ring * 2
                for (k in 0 until petals) {
                    val a = k * 2 * Math.PI / petals + ring * 0.3
                    drawCircle(cols[ring % cols.size], rad * 0.28f, Offset(c.x + cos(a).toFloat() * rad, c.y + sin(a).toFloat() * rad))
                }
            }
            drawCircle(Color(0xFFF5B83D), w * 0.06f, c)
        }
        4 -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFFFDE2A6), Color(0xFFF7A35C))), size = Size(w, h))
            drawCircle(Color(0xFFFFF3D6), w * 0.12f, Offset(w * 0.75f, h * 0.22f))
            hills(h * 0.55f, h * 0.05f, Color(0xFFE9A05B), 0f, 1.5f)
            hills(h * 0.68f, h * 0.06f, Color(0xFFD9843F), 1.5f, 2f)
            hills(h * 0.82f, h * 0.04f, Color(0xFFB8652B), 3f, 2.5f)
            // Camel caravan silhouettes.
            for (k in 0 until 3) {
                val x = w * (0.18f + k * 0.12f); val y = h * 0.6f
                drawCircle(Color(0xFF5A3420), w * 0.03f, Offset(x, y))
                drawRect(Color(0xFF5A3420), Offset(x - w * 0.035f, y), Size(w * 0.07f, h * 0.02f))
                drawLine(Color(0xFF5A3420), Offset(x + w * 0.035f, y), Offset(x + w * 0.05f, y - h * 0.03f), w * 0.01f)
            }
        }
        5 -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF14213D), Color(0xFF274C77))), size = Size(w, h))
            val rng = Random(5); repeat(50) { drawCircle(Color.White.copy(alpha = rng.nextFloat()), w * 0.004f, Offset(rng.nextFloat() * w, rng.nextFloat() * h * 0.6f)) }
            val beam = Path().apply { moveTo(w * 0.62f, h * 0.3f); lineTo(0f, h * 0.15f); lineTo(0f, h * 0.35f); close() }
            drawPath(beam, Color(0x55FFF3B0))
            drawRect(Color(0xFFF4F1DE), Offset(w * 0.58f, h * 0.3f), Size(w * 0.08f, h * 0.4f))
            for (k in 0 until 4) drawRect(Color(0xFFE5484D), Offset(w * 0.58f, h * (0.36f + k * 0.09f)), Size(w * 0.08f, h * 0.035f))
            drawCircle(Color(0xFFFFE29A), w * 0.04f, Offset(w * 0.62f, h * 0.29f))
            hills(h * 0.72f, h * 0.02f, Color(0xFF3D3B30), 0.4f, 1f)
            drawRect(Brush.verticalGradient(listOf(Color(0xFF1D3557), Color(0xFF0B132B)), h * 0.78f, h), Offset(0f, h * 0.78f), Size(w, h * 0.22f))
        }
        6 -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFFCDEBF5), Color(0xFFFFFFFF)), 0f, h * 0.4f), size = Size(w, h))
            val greens = listOf(Color(0xFF9CCC65), Color(0xFF7CB342), Color(0xFF558B2F), Color(0xFF33691E))
            for ((k, g) in greens.withIndex()) hills(h * (0.35f + k * 0.15f), h * 0.05f, g, k * 1.2f, 1.5f + k * 0.3f)
            for (row in 0 until 10) for (k in 0 until 12) {
                val y = h * (0.42f + row * 0.06f)
                drawCircle(Color(0x33204010), w * 0.018f, Offset(w * (k / 11f) + (row % 2) * w * 0.04f, y))
            }
            drawCircle(Color(0xFFFFD166), w * 0.08f, Offset(w * 0.8f, h * 0.14f))
        }
        else -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF7FC8A9), Color(0xFF2D6A4F))), size = Size(w, h))
            val rng = Random(7)
            repeat(9) {
                val c = Offset(rng.nextFloat() * w, h * 0.2f + rng.nextFloat() * h * 0.75f)
                drawCircle(Color(0xFF40916C), w * 0.09f, c)
                drawCircle(Color(0xFF52B788), w * 0.07f, c)
            }
            repeat(4) {
                val c = Offset(w * (0.15f + it * 0.24f), h * (0.3f + (it % 2) * 0.35f))
                for (k in 0 until 8) {
                    val a = k * Math.PI / 4
                    drawCircle(Color(0xFFF8A5C2), w * 0.035f, Offset(c.x + cos(a).toFloat() * w * 0.04f, c.y + sin(a).toFloat() * w * 0.04f))
                }
                drawCircle(Color(0xFFFFE066), w * 0.025f, c)
            }
        }
    }
}

@Composable
fun PostcardsGame(s: PlaySession) {
    val p = LocalPalette.current
    val game = remember { Postcards.generate(s.seed, s.difficulty) }
    var version by remember { mutableIntStateOf(0) }
    var selected by remember { mutableIntStateOf(-1) }
    var peek by remember { mutableStateOf(false) }
    var hints by remember { mutableIntStateOf(0) }

    fun tap(slot: Int) {
        if (game.locked(slot)) { s.haptic(true); return }
        if (selected == -1) { selected = slot; s.haptic(false); return }
        if (selected == slot) { selected = -1; return }
        game.swap(selected, slot); selected = -1; version++
        s.haptic(game.locked(slot))
        if (game.isSolved()) s.onSolved(hints)
    }

    Column(Modifier.fillMaxSize().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        version.let { Text("${SceneNames[game.scene]} · ${game.at.indices.count { game.locked(it) }} of ${game.at.size} in place", color = p.muted) }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            val pw = minOf(maxWidth / game.cols, maxHeight / game.rows)
            val ph = pw
            Box(Modifier.size(pw * game.cols, ph * game.rows).shadow(8.dp, RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp)).background(p.surface)) {
                if (peek) {
                    Canvas(Modifier.fillMaxSize()) { drawScene(game.scene, size.width, size.height) }
                } else version.let {
                    for (slot in game.at.indices) {
                        val piece = game.at[slot]
                        key(piece) {
                            val x by animateDpAsState(pw * (slot % game.cols), spring(dampingRatio = 0.7f, stiffness = 500f), label = "x")
                            val y by animateDpAsState(ph * (slot / game.cols), spring(dampingRatio = 0.7f, stiffness = 500f), label = "y")
                            val lift by animateFloatAsState(if (selected == slot) 1f else 0f, spring(stiffness = 600f), label = "lift")
                            val locked = game.locked(slot)
                            Box(
                                Modifier.offset(x, y).size(pw, ph).jellyClick { tap(slot) }
                                    .graphicsLayer { scaleX = 1f - lift * 0.06f; scaleY = scaleX }
                                    .then(if (locked) Modifier else Modifier.padding(1.dp).border(if (selected == slot) 3.dp else 0.5.dp, if (selected == slot) s.accent else Color.White.copy(alpha = 0.7f))),
                            ) {
                                Canvas(Modifier.fillMaxSize()) {
                                    val fullW = size.width * game.cols; val fullH = size.height * game.rows
                                    clipRect {
                                        translate(-size.width * (piece % game.cols), -size.height * (piece / game.cols)) { drawScene(game.scene, fullW, fullH) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        ActionRow {
            ActionChip(Icons.Rounded.Image, if (peek) "Hide" else "Peek") { peek = !peek }
            ActionChip(Icons.Rounded.Lightbulb, "Hint") {
                s.useHint {
                    game.hint()?.let { (a, b) -> hints++; game.swap(a, b); version++; s.haptic(true); if (game.isSolved()) s.onSolved(hints) }
                }
            }
        }
    }
}
