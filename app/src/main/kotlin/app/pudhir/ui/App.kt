package app.pudhir.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.pudhir.data.Progress

sealed interface Screen {
    data object Home : Screen
    data object Stats : Screen
    data class Play(val gameId: String, val daily: Boolean) : Screen
}

@Composable
fun PudhirApp(progress: Progress) {
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    PudhirTheme(progress.theme) {
        val p = LocalPalette.current
        BackHandler(enabled = screen != Screen.Home) { screen = Screen.Home }
        Box(Modifier.fillMaxSize().background(p.bg).liquidTouch(if (p.dark) androidx.compose.ui.graphics.Color(0xFF9CCBFF) else androidx.compose.ui.graphics.Color(0xFF3D8BD9))) {
            AnimatedContent(
                targetState = screen,
                transitionSpec = {
                    if (targetState == Screen.Home) {
                        (fadeIn(tween(220)) + scaleIn(tween(260), initialScale = 0.96f)) togetherWith
                            (slideOutHorizontally(tween(260)) { it / 3 } + fadeOut(tween(200)))
                    } else {
                        (slideInHorizontally(tween(300)) { it / 3 } + fadeIn(tween(260))) togetherWith
                            (fadeOut(tween(200)) + scaleOut(tween(260), targetScale = 0.96f))
                    }
                },
                label = "nav",
            ) { s ->
                when (s) {
                    Screen.Home -> HomeScreen(progress, onPlay = { id, daily -> screen = Screen.Play(id, daily) }, onStats = { screen = Screen.Stats })
                    Screen.Stats -> StatsScreen(progress, onBack = { screen = Screen.Home })
                    is Screen.Play -> PlayScreen(progress, s.gameId, s.daily, onHome = { screen = Screen.Home })
                }
            }
        }
    }
}
