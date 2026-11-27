package app.pudhir.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import app.pudhir.core.Seeds
import app.pudhir.core.Difficulty
import app.pudhir.data.Progress
import app.pudhir.games.Games
import app.pudhir.games.PlaySession
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter

const val HINT_COST = 1
const val SKIP_COST = 3

private sealed interface Outcome {
    data class Won(val stars: Int, val seconds: Int) : Outcome
    data class Over(val score: Int?, val message: String) : Outcome
}

@Composable
fun PlayScreen(progress: Progress, gameId: String, daily: Boolean, onHome: () -> Unit) {
    val game = Games.byId(gameId)
    val p = LocalPalette.current
    val haptic = Haptics()
    var round by remember { mutableIntStateOf(0) }
    var outcome by remember { mutableStateOf<Outcome?>(null) }
    var seconds by remember { mutableIntStateOf(0) }
    var adOffer by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    var showRules by remember { mutableStateOf(progress.wins(gameId) == 0) }
    // Levels ask for a difficulty first; daily puzzles and endless games don't.
    val picks = !daily && !game.endless
    // Asked each time the game is opened (not before every level); the last choice is pre-selected.
    var choosing by remember { mutableStateOf(picks) }
    val chosen = progress.difficulty(gameId)

    val level = remember(round, chosen) { progress.level(gameId) }
    val difficulty = if (daily || !picks) Difficulty.Medium else Difficulty.entries[chosen.coerceIn(0, 3)]
    // Each difficulty has its own run of levels; the original series stays as Medium.
    val seed = when {
        daily -> Seeds.daily(gameId, progress.today())
        difficulty == Difficulty.Medium -> Seeds.level(gameId, level)
        else -> Seeds.level(gameId + "/" + difficulty.name, level)
    }

    LaunchedEffect(round, outcome) {
        if (outcome != null) return@LaunchedEffect
        while (true) { delay(1000); if (!showRules && !choosing) seconds++ }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).bouncyClick(onClick = onHome), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = p.ink)
            }
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text(game.name, style = MaterialTheme.typography.titleLarge, color = p.ink)
                val sub = when {
                    daily -> "Daily · " + LocalDate.now().format(DateTimeFormatter.ofPattern("d MMM"))
                    game.endless -> "Best ${progress.bestScore(gameId)}"
                    game.versus -> "Game $level · ${difficulty.label}"
                    else -> "Level $level · ${difficulty.label}"
                }
                if (picks) Text(
                    "$sub ▾", style = MaterialTheme.typography.labelMedium, color = game.accent, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.bouncyClick { choosing = true },
                ) else Text(sub, style = MaterialTheme.typography.labelMedium, color = p.muted)
            }
            if (!game.endless) Pill("⏱ ${formatTime(seconds)}", p.surfaceAlt, p.ink, 30.dp)
            Spacer(Modifier.width(6.dp))
            Pill("⭐ ${progress.wallet()}", lighten(p.gold, 0.7f), darken(p.gold, 0.5f), 30.dp)
            if (!game.endless && !daily && !game.versus) Box(
                Modifier.size(40.dp).clip(CircleShape).bouncyClick {
                    val skip: () -> Unit = { progress.skipLevel(gameId); seconds = 0; round += 1 }
                    if (progress.spend(SKIP_COST)) skip() else adOffer = "a free skip" to skip
                },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.SkipNext, "Skip level for $SKIP_COST stars", tint = p.ink) }
            Box(Modifier.size(40.dp).clip(CircleShape).bouncyClick { showRules = true }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Info, "How to play", tint = p.ink)
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (choosing) return@Box // nothing to play until a difficulty is picked
            key(gameId, daily, level, round, chosen) {
                game.content(
                    PlaySession(
                        gameId = gameId, seed = seed, difficulty = difficulty, accent = game.accent,
                        onSolved = { hints ->
                            if (outcome == null) {
                                val stars = when { hints == 0 -> 3; hints <= 2 -> 2; else -> 1 }
                                progress.recordWin(gameId, daily, seconds, stars)
                                haptic(true)
                                outcome = Outcome.Won(stars, seconds)
                            }
                        },
                        onGameOver = { score, message ->
                            if (outcome == null) {
                                if (score != null) progress.recordScore(gameId, score)
                                haptic(true)
                                outcome = Outcome.Over(score, message)
                            }
                        },
                        haptic = { strong -> if (progress.haptics) haptic(strong) },
                        useHint = { grant ->
                            if (progress.spend(HINT_COST)) grant() else adOffer = "a free hint" to grant
                        },
                    ),
                )
            }
        }
    }

    adOffer?.let { (what, grant) ->
        AlertDialog(
            onDismissRequest = { adOffer = null },
            icon = { Text("⭐", fontSize = 36.sp) },
            title = { Text("Out of stars") },
            text = { Text("Solve levels to earn stars, or watch a short video for $what.", color = p.ink) },
            confirmButton = {
                // Ads are not wired yet: in test builds the video is skipped and the reward is given.
                TextButton(onClick = { adOffer = null; grant() }) { Text("Watch video", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { adOffer = null }) { Text("Not now") } },
        )
    }

    when (val o = outcome) {
        is Outcome.Won -> WinOverlay(
            title = if (daily) "Daily solved!" else if (game.versus) "You win!" else listOf("Brilliant!", "Sharp mind!", "Well played!", "Superb!")[level % 4],
            stars = o.stars,
            detail = (if (game.versus) "Won in " else "Solved in ") + formatTime(o.seconds) + if (daily) " · Streak ${progress.streak()} 🔥" else "",
            accent = game.accent,
            nextLabel = if (daily) "Play levels" else if (game.versus) "Play again" else "Next level",
            onNext = { outcome = null; seconds = 0; if (daily) onHome() else round++ },
            onHome = onHome,
        )
        is Outcome.Over -> WinOverlay(
            title = o.message,
            stars = 0,
            detail = if (o.score != null) "Score ${o.score} · Best ${progress.bestScore(gameId)}" else "Give it another go.",
            accent = game.accent,
            nextLabel = "Try again",
            onNext = { outcome = null; seconds = 0; round++ },
            onHome = onHome,
        )
        null -> {}
    }

    if (choosing) DifficultyPicker(
        game = game,
        current = chosen,
        levelFor = { d -> progress.levelAt(gameId, d) },
        onPick = { d -> progress.setDifficulty(gameId, d); seconds = 0; choosing = false },
        onDismiss = { if (chosen < 0) onHome() else choosing = false },
    )

    if (showRules && !choosing) AlertDialog(
        onDismissRequest = { showRules = false },
        confirmButton = { TextButton(onClick = { showRules = false }) { Text("Let's play", fontWeight = FontWeight.Bold) } },
        icon = { GlyphTile(game, 56.dp) },
        title = { Text(game.name) },
        text = {
            Column {
                for (r in game.rules) {
                    Row(Modifier.padding(vertical = 4.dp)) {
                        Box(Modifier.padding(top = 7.dp).size(6.dp).clip(CircleShape).background(game.accent))
                        Spacer(Modifier.width(10.dp))
                        Text(r, fontSize = 15.sp, color = p.ink)
                    }
                }
            }
        },
    )
}

private val DifficultyNotes = listOf(
    "Easy" to "Small boards, gentle start",
    "Medium" to "A steady challenge",
    "Hard" to "Bigger boards, fewer clues",
    "Expert" to "For the sharpest minds",
)

@Composable
private fun DifficultyPicker(
    game: app.pudhir.games.GameInfo,
    current: Int,
    levelFor: (Int) -> Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val p = LocalPalette.current
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (current < 0) "Back" else "Keep ${DifficultyNotes[current].first}") } },
        icon = { GlyphTile(game, 56.dp) },
        title = { Text("Choose your level") },
        text = {
            Column {
                DifficultyNotes.forEachIndexed { i, (name, note) ->
                    val on = i == current
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 5.dp).popIn(i * 60)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                            .background(if (on) lighten(game.accent, if (p.dark) 0.1f else 0.82f) else p.surfaceAlt)
                            .bouncyClick { onPick(i) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Filled dots show how hard it is.
                        Row {
                            for (k in 0 until 4) Box(
                                Modifier.padding(end = 3.dp).size(8.dp).clip(CircleShape)
                                    .background(if (k <= i) game.accent else p.line),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(name, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = if (on && p.dark) Color.White else p.ink)
                            Text(note, fontSize = 13.sp, color = if (on && p.dark) Color.White.copy(alpha = 0.8f) else p.muted)
                        }
                        val lv = levelFor(i)
                        if (lv > 1) Text("Level $lv", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = darken(game.accent, 0.2f))
                    }
                }
            }
        },
    )
}
