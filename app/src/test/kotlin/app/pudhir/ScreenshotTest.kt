package app.pudhir

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.pudhir.core.Difficulty
import app.pudhir.core.Seeds
import app.pudhir.data.Progress
import app.pudhir.games.Games
import app.pudhir.games.PlaySession
import app.pudhir.ui.HomeScreen
import app.pudhir.ui.LocalPalette
import app.pudhir.ui.PudhirTheme
import app.pudhir.ui.WinOverlay
import org.junit.Rule
import org.junit.Test

/** Screenshots of every screen, recorded in CI with `recordPaparazziDebug`. */
class ScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5, maxPercentDifference = 100.0)

    @Composable
    private fun Frame(dark: Boolean = false, content: @Composable () -> Unit) {
        PudhirTheme(if (dark) 2 else 1) {
            Box(Modifier.fillMaxSize().background(LocalPalette.current.bg)) { content() }
        }
    }

    @Test fun home() = paparazzi.snapshot { Frame { HomeScreen(Progress(paparazzi.context), { _, _ -> }, {}) } }

    @Test fun homeDark() = paparazzi.snapshot { Frame(dark = true) { HomeScreen(Progress(paparazzi.context), { _, _ -> }, {}) } }

    @Test fun win() = paparazzi.snapshot {
        Frame { WinOverlay("Brilliant!", 3, "Solved in 1:42", Games.all[0].accent, "Next level", {}, {}) }
    }

    @Test fun allGames() {
        for (g in Games.all) {
            paparazzi.snapshot(name = g.id) {
                Frame {
                    Column {
                        Text(g.name)
                        g.content(
                            PlaySession(g.id, Seeds.level(g.id, 12), Difficulty.Medium, g.accent, {}, { _, _ -> }, {}, { it() }),
                        )
                    }
                }
            }
        }
    }
}
