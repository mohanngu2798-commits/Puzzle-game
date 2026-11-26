package app.pudhir.games

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import app.pudhir.ui.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Builds a puzzle off the main thread; shows a gentle loader while it is being made. */
@Composable
fun <T : Any> Generated(s: PlaySession, make: () -> T, content: @Composable (T) -> Unit) {
    val value by produceState<T?>(null, s.seed, s.difficulty) {
        value = withContext(Dispatchers.Default) { make() }
    }
    val v = value
    if (v == null) PreparingPuzzle(s) else content(v)
}

@Composable
fun PreparingPuzzle(s: PlaySession) {
    val p = LocalPalette.current
    val t = rememberInfiniteTransition(label = "load")
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (i in 0 until 3) {
                val y by t.animateFloat(0f, -12f, infiniteRepeatable(tween(420, delayMillis = i * 120), RepeatMode.Reverse), label = "dot$i")
                Box(Modifier.size(14.dp).graphicsLayer { translationY = y * density }.clip(CircleShape).background(s.accent))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Preparing your puzzle…", color = p.muted)
    }
}
