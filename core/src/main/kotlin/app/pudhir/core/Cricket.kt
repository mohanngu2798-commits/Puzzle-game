package app.pudhir.core

import kotlin.math.abs
import kotlin.random.Random

/** A cartoon bowler: how fast they bowl, how much the ball drifts, and how much the pace varies. */
data class Bowler(val name: String, val style: String, val travel: Float, val drift: Float, val vary: Float, val colour: Long, val hair: Long)

/**
 * Tap Cricket: bowlers come in one after another; tap to swing as the ball reaches the bat.
 * Better timing scores more. Three wickets and the innings ends.
 */
class TapCricket(seed: Long, val difficulty: Difficulty) {
    enum class Shot(val runs: Int, val label: String, val out: Boolean = false) {
        Six(6, "SIX!"), Four(4, "FOUR!"), Two(2, "Two runs"), One(1, "Single"), Dot(0, "Dot ball"),
        Caught(0, "Caught!", true), Bowled(0, "Bowled!", true),
    }

    /** One ball: seconds from release to the bat, sideways drift (-1..1), and whether it would hit the stumps. */
    data class Delivery(val bowler: Bowler, val travel: Float, val drift: Float, val onStumps: Boolean, val slower: Boolean)

    private val rng = Random(seed)
    var runs = 0; private set
    var wickets = 0; private set
    var balls = 0; private set
    var streak = 0; private set
    var sixes = 0; private set
    val maxWickets = 3
    private var bowlerIndex = 0

    /** How forgiving the timing window is, in seconds (half-width of a perfect hit). */
    val perfect = when (difficulty) { Difficulty.Easy -> 0.055f; Difficulty.Medium -> 0.042f; Difficulty.Hard -> 0.034f; Difficulty.Expert -> 0.027f }
    private val pace = when (difficulty) { Difficulty.Easy -> 1.18f; Difficulty.Medium -> 1f; Difficulty.Hard -> 0.88f; Difficulty.Expert -> 0.78f }

    /** Bowler for the current over (changes every six balls, and gets tougher as the innings goes). */
    val bowler: Bowler get() = BOWLERS[bowlerIndex % BOWLERS.size]
    val over get() = balls / 6
    val ballInOver get() = balls % 6

    fun next(): Delivery {
        bowlerIndex = over
        val b = bowler
        val tough = 1f - minOf(over, 8) * 0.025f
        val slower = rng.nextFloat() < b.vary
        val t = b.travel * pace * tough * (if (slower) 1.35f else 1f) * (0.94f + rng.nextFloat() * 0.12f)
        return Delivery(b, t, (rng.nextFloat() * 2 - 1) * b.drift, rng.nextFloat() < 0.6f, slower)
    }

    /** Result of a swing [error] seconds off (negative = early); null error means no swing. */
    fun judge(d: Delivery, error: Float?): Shot {
        val shot = when {
            error == null -> if (d.onStumps) Shot.Bowled else Shot.Dot
            else -> {
                val e = abs(error) / perfect
                when {
                    e <= 1f -> Shot.Six
                    e <= 2f -> Shot.Four
                    e <= 3f -> Shot.Two
                    e <= 4.2f -> if (error < 0) Shot.Caught else Shot.One
                    e <= 6f -> if (d.onStumps) Shot.Bowled else Shot.Dot
                    else -> if (d.onStumps) Shot.Bowled else Shot.Dot
                }
            }
        }
        balls++
        runs += shot.runs
        if (shot == Shot.Six) sixes++
        if (shot.out) { wickets++; streak = 0 } else if (shot.runs >= 4) streak++ else streak = 0
        return shot
    }

    fun isOver() = wickets >= maxWickets

    companion object {
        val BOWLERS = listOf(
            Bowler("Kiki", "gentle medium pace", 1.25f, 0.2f, 0.05f, 0xFF4FB3E8, 0xFF2B2440),
            Bowler("Mango Bolt", "quick and straight", 1.0f, 0.1f, 0.1f, 0xFFF5B83D, 0xFFE0503A),
            Bowler("Spinny Ravi", "loopy spin", 1.35f, 0.55f, 0.15f, 0xFF62C370, 0xFF3A2A20),
            Bowler("Thunder Meena", "fast, with a slower ball", 0.85f, 0.2f, 0.3f, 0xFFE05A8A, 0xFF5B2A86),
            Bowler("Captain Chakra", "swing and mystery", 0.95f, 0.45f, 0.25f, 0xFF8E6CE0, 0xFFF2F2F2),
            Bowler("Rocket Raju", "the fastest of them all", 0.72f, 0.15f, 0.2f, 0xFFEF6B3A, 0xFF1C1C1C),
        )
    }
}
