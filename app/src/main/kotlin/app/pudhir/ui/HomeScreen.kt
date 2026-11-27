package app.pudhir.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pudhir.data.Progress
import app.pudhir.games.GameInfo
import app.pudhir.games.Games
import app.pudhir.games.Shelf
import java.time.LocalTime

@Composable
fun HomeScreen(progress: Progress, onPlay: (String, Boolean) -> Unit, onStats: () -> Unit) {
    val p = LocalPalette.current
    var settings by remember { mutableStateOf(false) }
    val dailies = Games.all.filter { !it.endless }
    val doneToday = dailies.count { progress.dailyDone(it.id) }

    LazyColumn(
        Modifier.fillMaxWidth().statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                LogoMark(30.dp)
                Spacer(Modifier.width(10.dp))
                Text("Pudhir", style = MaterialTheme.typography.headlineSmall, color = p.ink, modifier = Modifier.weight(1f))
                Pill("🔥 ${progress.streak()}", lighten(Color(0xFFF26B3A), 0.82f), Color(0xFFB4441C), 32.dp)
                Spacer(Modifier.width(4.dp))
                IconCircle(Icons.Rounded.BarChart, "Stats", onStats)
                IconCircle(Icons.Rounded.Settings, "Settings") { settings = true }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(greeting(), style = MaterialTheme.typography.displaySmall, color = p.ink)
                Text("Sharpen your mind, one puzzle a day.", style = MaterialTheme.typography.bodyLarge, color = p.muted)
            }
        }
        item { TodayCard(doneToday, dailies.size) }
        item { SectionTitle("Today's puzzles") }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(dailies, key = { it.id }) { g -> DailyChip(g, progress.dailyDone(g.id)) { onPlay(g.id, true) } }
            }
        }
        for (shelf in Shelf.entries) {
            val games = Games.all.filter { it.shelf == shelf }
            item { SectionTitle(shelf.title) }
            items(games.chunked(2)) { row ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    for ((i, g) in row.withIndex()) {
                        Box(Modifier.weight(1f).padding(6.dp)) {
                            GameCard(g, progress, Games.all.indexOf(g)) { onPlay(g.id, false) }
                        }
                        if (row.size == 1 && i == 0) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        item { Spacer(Modifier.navigationBarsPadding()) }
    }

    if (settings) SettingsDialog(progress) { settings = false }
}

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

@Composable
fun LogoMark(size: androidx.compose.ui.unit.Dp) {
    // Same mark as the app icon: plum tile with four puzzle tiles, one still to solve.
    androidx.compose.foundation.Canvas(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.26f))
            .background(Brush.radialGradient(listOf(Color(0xFF4A2C7A), Color(0xFF160C2E)), center = androidx.compose.ui.geometry.Offset.Zero, radius = size.value * 3f)),
    ) {
        val u = this.size.width / 108f
        val gold = Brush.linearGradient(listOf(Color(0xFFFFE8A3), Color(0xFFF2C14E), Color(0xFFC98A1B)))
        val coral = Brush.linearGradient(listOf(Color(0xFFFFB08A), Color(0xFFF0553A)))
        androidx.compose.ui.graphics.drawscope.rotate(45f) {
            val r = androidx.compose.ui.geometry.CornerRadius(5f * u)
            fun tile(x: Float, y: Float, b: Brush, outline: Boolean = false) = drawRoundRect(
                b, androidx.compose.ui.geometry.Offset(x * u, y * u), androidx.compose.ui.geometry.Size(18 * u, 18 * u), r,
                style = if (outline) androidx.compose.ui.graphics.drawscope.Stroke(3.2f * u) else androidx.compose.ui.graphics.drawscope.Fill,
            )
            tile(31f, 31f, coral); tile(57f, 31f, gold); tile(31f, 57f, gold); tile(57f, 57f, gold, true)
        }
    }
}

@Composable
private fun IconCircle(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(Modifier.size(44.dp).clip(CircleShape).bouncyClick(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, label, tint = p.ink)
    }
}

@Composable
private fun TodayCard(done: Int, total: Int) {
    val frac by animateFloatAsState(done / total.toFloat(), tween(700), label = "today")
    Box(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(26.dp), ambientColor = Color(0x55F26B3A), spotColor = Color(0x55F26B3A))
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFF26B3A), Color(0xFFE5487A))))
            .padding(20.dp),
    ) {
        Column {
            Text("DAILY CHALLENGE", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                if (done == total) "All done for today!" else "$done of $total solved today",
                color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text("New puzzles every midnight. Keep your streak alive.", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Color.White.copy(alpha = 0.3f))) {
                Box(Modifier.fillMaxWidth(frac).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(Color.White))
            }
        }
    }
}

@Composable
private fun DailyChip(g: GameInfo, done: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    Column(
        Modifier
            .width(96.dp)
            .shadow(3.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(p.surface)
            .bouncyClick(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            GlyphTile(g, 52.dp)
            if (done) Box(
                Modifier.align(Alignment.BottomEnd).size(22.dp).clip(CircleShape).background(p.good).border(2.dp, p.surface, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(14.dp)) }
        }
        Spacer(Modifier.height(8.dp))
        Text(g.name, style = MaterialTheme.typography.labelMedium, color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun GlyphTile(g: GameInfo, size: androidx.compose.ui.unit.Dp) {
    val p = LocalPalette.current
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.3f)).background(if (p.dark) darken(g.accent, 0.55f) else lighten(g.accent, 0.84f)),
        contentAlignment = Alignment.Center,
    ) { Text(g.glyph, fontSize = (size.value * 0.5f).sp) }
}

@Composable
private fun GameCard(g: GameInfo, progress: Progress, index: Int, onClick: () -> Unit) {
    val p = LocalPalette.current
    Column(
        Modifier
            .fillMaxWidth()
            .popIn(60 * (index % 8))
            .shadow(4.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(p.surface)
            .bouncyClick(onClick = onClick)
            .padding(16.dp),
    ) {
        GlyphTile(g, 56.dp)
        Spacer(Modifier.height(12.dp))
        Text(g.name, style = MaterialTheme.typography.titleMedium, color = p.ink, maxLines = 1)
        Text(g.tagline, style = MaterialTheme.typography.bodyMedium, color = p.muted, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(10.dp))
        if (g.endless) Pill("Best ${progress.bestScore(g.id)}", lighten(g.accent, 0.8f), darken(g.accent, 0.35f))
        else Pill("Level ${progress.level(g.id)}", lighten(g.accent, 0.8f), darken(g.accent, 0.35f))
    }
}

@Composable
private fun SettingsDialog(progress: Progress, onClose: () -> Unit) {
    val p = LocalPalette.current
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = { TextButton(onClick = onClose) { Text("Done") } },
        title = { Text("Settings") },
        text = {
            Column {
                Text("Theme", style = MaterialTheme.typography.titleMedium, color = p.ink)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("System", "Light", "Dark").forEachIndexed { i, label ->
                        val on = progress.theme == i
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (on) Color(0xFFF26B3A) else p.surfaceAlt)
                                .bouncyClick { progress.theme = i }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) { Text(label, color = if (on) Color.White else p.ink, fontWeight = FontWeight.SemiBold) }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Vibration", style = MaterialTheme.typography.titleMedium, color = p.ink, modifier = Modifier.weight(1f))
                    Switch(checked = progress.haptics, onCheckedChange = { progress.haptics = it })
                }
                Spacer(Modifier.height(12.dp))
                Text("Pudhir works fully offline. Your progress stays on this phone.", color = p.muted, fontSize = 13.sp)
            }
        },
    )
}
