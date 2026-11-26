package app.pudhir.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pudhir.data.Progress
import app.pudhir.games.Games

@Composable
fun StatsScreen(progress: Progress, onBack: () -> Unit) {
    val p = LocalPalette.current
    LazyColumn(Modifier.fillMaxWidth().statusBarsPadding(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(CircleShape).bouncyClick(onClick = onBack), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = p.ink)
                }
                Text("Your progress", style = MaterialTheme.typography.headlineSmall, color = p.ink)
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                StatBox("🔥", "${progress.streak()}", "Day streak", Modifier.weight(1f))
                StatBox("🏆", "${progress.bestStreak()}", "Best streak", Modifier.weight(1f))
                StatBox("⭐", "${progress.totalStars()}", "Stars", Modifier.weight(1f))
            }
        }
        item { SectionTitle("By game") }
        items(Games.all) { g ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(18.dp)).background(p.surface).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlyphTile(g, 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(g.name, style = MaterialTheme.typography.titleMedium, color = p.ink)
                    val line = if (g.endless) "Best score ${progress.bestScore(g.id)}"
                    else "Solved ${progress.wins(g.id)} · Best time " + (progress.bestTime(g.id).takeIf { it > 0 }?.let { formatTime(it) } ?: "-")
                    Text(line, style = MaterialTheme.typography.bodyMedium, color = p.muted)
                }
                if (!g.endless) Text("⭐ ${progress.stars(g.id)}", color = p.ink, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StatBox(emoji: String, value: String, label: String, modifier: Modifier) {
    val p = LocalPalette.current
    Column(
        modifier.padding(6.dp).shadow(3.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp)).background(p.surface).padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 22.sp)
        Text(value, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = p.ink)
        Text(label, style = MaterialTheme.typography.labelMedium, color = p.muted)
    }
}
