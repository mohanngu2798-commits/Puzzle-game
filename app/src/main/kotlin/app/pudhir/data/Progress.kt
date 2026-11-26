package app.pudhir.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDate

/** Everything the player has achieved, stored on the device only. */
class Progress(context: Context) {
    private val prefs = context.getSharedPreferences("pudhir", Context.MODE_PRIVATE)

    /** Bumped on every write so Compose screens re-read. */
    var version by mutableIntStateOf(0)
        private set

    private fun edit(block: android.content.SharedPreferences.Editor.() -> Unit) {
        prefs.edit().apply(block).apply()
        version++
    }

    fun today(): Long = LocalDate.now().toEpochDay()

    fun level(game: String): Int { version; return prefs.getInt("level_$game", 1) }
    fun wins(game: String): Int { version; return prefs.getInt("wins_$game", 0) }
    fun stars(game: String): Int { version; return prefs.getInt("stars_$game", 0) }
    fun bestTime(game: String): Int { version; return prefs.getInt("best_$game", 0) }
    fun bestScore(game: String): Int { version; return prefs.getInt("score_$game", 0) }
    fun dailyDone(game: String): Boolean { version; return prefs.getLong("daily_$game", -1) == today() }
    fun totalWins(): Int { version; return prefs.all.filterKeys { it.startsWith("wins_") }.values.sumOf { (it as? Int) ?: 0 } }
    fun totalStars(): Int { version; return prefs.all.filterKeys { it.startsWith("stars_") }.values.sumOf { (it as? Int) ?: 0 } }

    /** Current daily streak: days in a row with at least one daily puzzle solved. */
    fun streak(): Int {
        version
        val last = prefs.getLong("streak_day", -10)
        return if (last >= today() - 1) prefs.getInt("streak", 0) else 0
    }

    fun bestStreak(): Int { version; return prefs.getInt("best_streak", 0) }

    fun recordWin(game: String, daily: Boolean, seconds: Int, stars: Int) {
        edit {
            putInt("wins_$game", prefs.getInt("wins_$game", 0) + 1)
            putInt("stars_$game", prefs.getInt("stars_$game", 0) + stars)
            putInt("wallet", prefs.getInt("wallet", 3) + stars)
            val best = prefs.getInt("best_$game", 0)
            if (seconds > 0 && (best == 0 || seconds < best)) putInt("best_$game", seconds)
            if (daily) {
                putLong("daily_$game", today())
                val last = prefs.getLong("streak_day", -10)
                if (last != today()) {
                    val s = if (last == today() - 1) prefs.getInt("streak", 0) + 1 else 1
                    putInt("streak", s)
                    putLong("streak_day", today())
                    if (s > prefs.getInt("best_streak", 0)) putInt("best_streak", s)
                }
            } else {
                putInt("level_$game", prefs.getInt("level_$game", 1) + 1)
            }
        }
    }

    /** Spendable stars: everyone starts with 3 bonus stars. */
    fun wallet(): Int { version; return prefs.getInt("wallet", 3) }

    fun spend(n: Int): Boolean {
        val w = prefs.getInt("wallet", 3)
        if (w < n) return false
        edit { putInt("wallet", w - n) }
        return true
    }

    fun skipLevel(game: String) = edit { putInt("level_$game", prefs.getInt("level_$game", 1) + 1) }

    fun recordScore(game: String, score: Int) {
        if (score > prefs.getInt("score_$game", 0)) edit { putInt("score_$game", score) }
    }

    var theme: Int
        get() { version; return prefs.getInt("theme", 0) } // 0 system, 1 light, 2 dark
        set(v) = edit { putInt("theme", v) }

    var haptics: Boolean
        get() { version; return prefs.getBoolean("haptics", true) }
        set(v) = edit { putBoolean("haptics", v) }
}
