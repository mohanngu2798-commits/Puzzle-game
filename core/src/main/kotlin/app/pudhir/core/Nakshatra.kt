package app.pudhir.core

/**
 * Nakshatra, the five-pointed star puzzle. The star has ten points: five tips and five inner crossings,
 * joined by five straight lines of four points each.
 * Each move starts on an empty point, passes over the next point along a line, and puts a coin on
 * the point after it, which must be empty. Place [goal] coins to win.
 */
class Nakshatra(val goal: Int) {
    val coins = BooleanArray(10)
    val placed get() = coins.count { it }
    private val history = ArrayList<Int>()

    /** Legal moves as (start, over, land). */
    fun moves(): List<Triple<Int, Int, Int>> = JUMPS.filter { (a, _, c) -> !coins[a] && !coins[c] }

    fun play(start: Int, land: Int): Boolean {
        val m = moves().firstOrNull { it.first == start && it.third == land } ?: return false
        coins[m.third] = true; history += m.third
        return true
    }

    fun undo(): Boolean { val i = history.removeLastOrNull() ?: return false; coins[i] = false; return true }
    fun restart() { coins.fill(false); history.clear() }
    fun isWon() = placed >= goal
    fun isStuck() = !isWon() && moves().isEmpty()

    /** A move that still leads to the goal, if any. */
    fun hint(): Triple<Int, Int, Int>? {
        for (m in moves()) {
            coins[m.third] = true
            val ok = solvable()
            coins[m.third] = false
            if (ok) return m
        }
        return null
    }

    private fun solvable(): Boolean {
        if (placed >= goal) return true
        for (m in moves()) {
            coins[m.third] = true
            val ok = solvable()
            coins[m.third] = false
            if (ok) return true
        }
        return false
    }

    companion object {
        /** Tips 0..4 (clockwise from the top), inner points 5..9; inner 5+k sits between tips k and k+1. */
        val LINES: List<List<Int>> = (0 until 5).map { k ->
            // Line from tip k to tip k+2 passes inner points k+5 and (k+1)%5+5.
            listOf(k, 5 + k, 5 + (k + 1) % 5, (k + 2) % 5)
        }
        val JUMPS: List<Triple<Int, Int, Int>> = LINES.flatMap { l ->
            listOf(Triple(l[0], l[1], l[2]), Triple(l[1], l[2], l[3]), Triple(l[2], l[1], l[0]), Triple(l[3], l[2], l[1]))
        }
    }
}
