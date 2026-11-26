package app.pudhir.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class Palette(
    val bg: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val good: Color,
    val bad: Color,
    val gold: Color,
    val dark: Boolean,
)

val LightPalette = Palette(
    bg = Color(0xFFFAF8F5), surface = Color.White, surfaceAlt = Color(0xFFF2EEE8),
    ink = Color(0xFF1C1B22), muted = Color(0xFF77727F), line = Color(0xFFE4DED6),
    good = Color(0xFF2BA36B), bad = Color(0xFFE5484D), gold = Color(0xFFF5B83D), dark = false,
)

val DarkPalette = Palette(
    bg = Color(0xFF121116), surface = Color(0xFF1E1C24), surfaceAlt = Color(0xFF2A2731),
    ink = Color(0xFFF2EFEA), muted = Color(0xFFA29DAA), line = Color(0xFF34313B),
    good = Color(0xFF3CC283), bad = Color(0xFFFF6B6F), gold = Color(0xFFFFC54D), dark = true,
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

/** Bright, friendly colours used for pieces across games (tea, bolts, threads...). */
val PieceColours = listOf(
    Color(0xFFF26B3A), Color(0xFF3A86F2), Color(0xFF2BB673), Color(0xFFF5B83D),
    Color(0xFF9B5DE5), Color(0xFFE5487A), Color(0xFF1FB5C9), Color(0xFF8A5A3B),
    Color(0xFF6C7A89), Color(0xFFB7D334),
)

private val Display = FontFamily.SansSerif

val PudhirType = Typography(
    displaySmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = Display, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Display, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 0.3.sp),
)

@Composable
fun PudhirTheme(mode: Int, content: @Composable () -> Unit) {
    val dark = when (mode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    val p = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) darkColorScheme(background = p.bg, surface = p.surface, onSurface = p.ink, onBackground = p.ink, primary = Color(0xFFF26B3A))
    else lightColorScheme(background = p.bg, surface = p.surface, onSurface = p.ink, onBackground = p.ink, primary = Color(0xFFF26B3A))
    androidx.compose.runtime.CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, typography = PudhirType, content = content)
    }
}
