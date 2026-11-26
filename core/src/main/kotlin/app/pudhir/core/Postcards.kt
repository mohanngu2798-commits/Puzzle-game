package app.pudhir.core

import kotlin.random.Random

/**
 * Postcards: a painted postcard is cut into tiles and shuffled.
 * Swap any two tiles until the picture is whole. Tiles in the right place lock in.
 * at[slot] = which piece sits in that slot; piece k belongs in slot k.
 */
class Postcards(val rows: Int, val cols: Int, val scene: Int, val at: IntArray) {
    var swaps = 0; private set

    fun locked(slot: Int) = at[slot] == slot

    fun swap(a: Int, b: Int): Boolean {
        if (a == b || locked(a) || locked(b)) return false
        val t = at[a]; at[a] = at[b]; at[b] = t
        swaps++
        return true
    }

    fun isSolved() = at.indices.all { at[it] == it }

    /** Puts one piece into its home slot (swapping out whatever is there). */
    fun hint(): Pair<Int, Int>? {
        val slot = at.indices.firstOrNull { !locked(it) } ?: return null
        val from = at.indexOf(slot)
        return from to slot
    }

    companion object {
        const val SCENES = 8
        fun spec(d: Difficulty) = when (d) {
            Difficulty.Easy -> 4 to 3; Difficulty.Medium -> 5 to 4; Difficulty.Hard -> 6 to 5; Difficulty.Expert -> 7 to 6
        }

        fun generate(seed: Long, difficulty: Difficulty): Postcards {
            val rng = Random(seed)
            val (rows, cols) = spec(difficulty)
            val n = rows * cols
            // Shuffle so that no piece starts at home.
            val perm = IntArray(n) { it }
            do {
                for (i in n - 1 downTo 1) { val j = rng.nextInt(i + 1); val t = perm[i]; perm[i] = perm[j]; perm[j] = t }
            } while (perm.indices.count { perm[it] == it } > n / 10)
            return Postcards(rows, cols, rng.nextInt(SCENES), perm)
        }
    }
}
