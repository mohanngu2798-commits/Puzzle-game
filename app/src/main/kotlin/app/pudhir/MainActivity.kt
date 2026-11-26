package app.pudhir

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.pudhir.data.Progress
import app.pudhir.ui.PudhirApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val progress = Progress(applicationContext)
        setContent { PudhirApp(progress) }
    }
}
