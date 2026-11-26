package app.pudhir.core

import kotlin.random.Random

/**
 * Clockwork: a board of geared clocks. Tapping a gear turns its hand a quarter turn forward,
 * and every gear meshed with it turns a quarter turn back (meshed gears spin the other way).
 * Set every hand to twelve.
 *
 * Puzzles are made by scrambling a solved board with taps, so each one is solvable. Because
 * taps add up in any order, we always know a set of taps that solves the current board,
 * which powers the hint.
 */
class Clockwork private constructor(
    val rows: Int,
    val cols: Int,
    /** Hand positions per full turn (4 = quarter turns). */
    val steps: Int,
    /** Meshed gear pairs (orthogonal neighbours). */
    val links: List<Pair<Int, Int>>,
    private val start: IntArray,
    private val startNeed: IntArray,
) {
    val size get() = rows * cols
    val dial = start.copyOf()
    /** How many more taps each gear needs in one known solution. */
    private val need = startNeed.copyOf()
    private val history = ArrayList<Int>()
    private val meshed: List<List<Int>> = List(rows * cols) { i -> links.mapNotNull { (a, b) -> if (a == i) b else if (b == i) a else null } }

    val moves get() = history.size
    /** Fewest taps the board was scrambled with: a fair target. */
    val par: Int = startNeed.sum()

    fun neighbours(i: Int): List<Int> = meshed[i]

    fun history(): List<Int> = history

    fun tap(i: Int) {
        turn(i, 1); need[i] = (need[i] - 1 + steps) % steps; history += i
    }

    fun undo(): Boolean {
        val i = history.removeLastOrNull() ?: return false
        turn(i, -1); need[i] = (need[i] + 1) % steps
        return true
    }

    fun restart() {
        start.copyInto(dial); startNeed.copyInto(need); history.clear()
    }

    fun isSolved() = dial.all { it == 0 }

    /** A gear worth tapping next, or null when solved. */
    fun hint(): Int? = need.indices.firstOrNull { need[it] > 0 }

    private fun turn(i: Int, by: Int) {
        dial[i] = (dial[i] + by + steps) % steps
        for (j in meshed[i]) dial[j] = (dial[j] - by + steps * 2) % steps
    }

    companion object {
        fun generate(seed: Long, difficulty: Difficulty): Clockwork {
            val rng = Random(seed)
            val (rows, cols, scrambled) = when (difficulty) {
                Difficulty.Easy -> Triple(3, 3, 3)
                Difficulty.Medium -> Triple(4, 3, 5)
                Difficulty.Hard -> Triple(4, 4, 8)
                Difficulty.Expert -> Triple(5, 4, 11)
            }
            val steps = 4
            val n = rows * cols
            while (true) {
                val links = meshLinks(rows, cols, rng, extra = if (difficulty <= Difficulty.Medium) 0.25 else 0.4)
                val taps = IntArray(n)
                for (i in (0 until n).shuffled(rng).take(scrambled)) taps[i] = 1 + rng.nextInt(steps - 1)
                val dial = IntArray(n)
                for (i in 0 until n) repeat(taps[i]) {
                    dial[i] = (dial[i] + 1) % steps
                    for ((a, b) in links) {
                        if (a == i) dial[b] = (dial[b] + steps - 1) % steps
                        if (b == i) dial[a] = (dial[a] + steps - 1) % steps
                    }
                }
                // Too many hands already home makes a dull board.
                if (dial.count { it == 0 } > n / 2) continue
                val need = IntArray(n) { (steps - taps[it]) % steps }
                return Clockwork(rows, cols, steps, links, dial, need)
            }
        }

        /** A random spanning tree over the grid (every gear is meshed) plus some extra meshes. */
        private fun meshLinks(rows: Int, cols: Int, rng: Random, extra: Double): List<Pair<Int, Int>> {
            val n = rows * cols
            fun adj(i: Int) = buildList {
                val r = i / cols; val c = i % cols
                if (r > 0) add(i - cols); if (r < rows - 1) add(i + cols)
                if (c > 0) add(i - 1); if (c < cols - 1) add(i + 1)
            }
            val seen = BooleanArray(n); val out = LinkedHashSet<Pair<Int, Int>>()
            val stack = ArrayDeque(listOf(rng.nextInt(n))); seen[stack.first()] = true
            while (stack.isNotEmpty()) {
                val cur = stack.last()
                val next = adj(cur).filter { !seen[it] }
                if (next.isEmpty()) { stack.removeLast(); continue }
                val nx = next[rng.nextInt(next.size)]
                seen[nx] = true; out += minOf(cur, nx) to maxOf(cur, nx); stack.addLast(nx)
            }
            for (i in 0 until n) for (j in adj(i)) if (j > i && (i to j) !in out && rng.nextDouble() < extra) out += i to j
            return out.toList()
        }
    }
}
