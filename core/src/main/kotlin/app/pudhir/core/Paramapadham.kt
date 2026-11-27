package app.pudhir.core

import kotlin.random.Random

/**
 * Paramapadham (snakes and ladders) on squares 1..100, you against the phone.
 * Roll the die and move; a ladder's foot lifts you to its top, a snake's head sends you to its tail.
 * You must land exactly on 100 to reach Vaikuntam; a roll that overshoots is lost.
 * Rolling a 6 gives another roll.
 */
class Paramapadham(seed: Long, difficulty: Difficulty) {
    private val rng = Random(seed)
    /** head -> tail. */
    val snakes: Map<Int, Int>
    /** foot -> top. */
    val ladders: Map<Int, Int>
    val pos = intArrayOf(0, 0)
    var turn = 0
        private set
    var lastRoll = 0
        private set

    init {
        val (nSnakes, nLadders) = when (difficulty) {
            Difficulty.Easy -> 5 to 8
            Difficulty.Medium -> 7 to 7
            Difficulty.Hard -> 9 to 6
            Difficulty.Expert -> 11 to 5
        }
        val used = HashSet<Int>().apply { add(1); add(100) }
        val s = LinkedHashMap<Int, Int>(); val l = LinkedHashMap<Int, Int>()
        while (s.size < nSnakes) {
            val head = 20 + rng.nextInt(79) // 20..98
            val tail = (head - 10 - rng.nextInt(minOf(40, head - 11))).coerceAtLeast(2)
            if (head in used || tail in used || row(head) == row(tail)) continue
            s[head] = tail; used += head; used += tail
        }
        while (l.size < nLadders) {
            val foot = 2 + rng.nextInt(75)
            val top = (foot + 10 + rng.nextInt(30)).coerceAtMost(98)
            if (foot in used || top in used || row(foot) == row(top)) continue
            l[foot] = top; used += foot; used += top
        }
        snakes = s; ladders = l
    }

    private fun row(sq: Int) = (sq - 1) / 10

    fun winner() = when { pos[0] == 100 -> 0; pos[1] == 100 -> 1; else -> -1 }

    /** Result of one roll: where the piece walked to, and where it ended after any snake or ladder. */
    data class Roll(val player: Int, val die: Int, val walkedTo: Int, val endedAt: Int, val again: Boolean)

    fun roll(): Roll {
        val p = turn
        val die = 1 + rng.nextInt(6)
        lastRoll = die
        val target = pos[p] + die
        val walked = if (target > 100) pos[p] else target
        val ended = snakes[walked] ?: ladders[walked] ?: walked
        pos[p] = ended
        val again = die == 6 && ended != 100
        if (!again) turn = 1 - turn
        return Roll(p, die, walked, ended, again)
    }
}
