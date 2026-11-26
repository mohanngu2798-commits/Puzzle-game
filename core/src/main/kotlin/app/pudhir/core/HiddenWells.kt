package app.pudhir.core

import kotlin.random.Random

/**
 * Hidden Wells: open every safe square without falling into a hidden well.
 * A number shows how many wells touch that square. Boards are generated after the
 * first tap and checked to be solvable by logic alone, so you never have to guess.
 */
class HiddenWells(val rows: Int, val cols: Int, val wells: BooleanArray) {
    val counts = IntArray(rows * cols) { p -> neighbours(p).count { wells[it] } }

    fun neighbours(p: Int): List<Int> {
        val r = p / cols; val c = p % cols
        val out = ArrayList<Int>(8)
        for (dr in -1..1) for (dc in -1..1) {
            if (dr == 0 && dc == 0) continue
            val rr = r + dr; val cc = c + dc
            if (rr in 0 until rows && cc in 0 until cols) out += rr * cols + cc
        }
        return out
    }

    /** Opens [p] (flood-filling zeros) into [open]. Returns false if it was a well. */
    fun open(p: Int, open: BooleanArray): Boolean {
        if (wells[p]) return false
        val stack = ArrayDeque(listOf(p))
        while (stack.isNotEmpty()) {
            val q = stack.removeLast()
            if (open[q]) continue
            open[q] = true
            if (counts[q] == 0) for (nb in neighbours(q)) if (!open[nb] && !wells[nb]) stack.addLast(nb)
        }
        return true
    }

    fun isCleared(open: BooleanArray) = (0 until rows * cols).all { open[it] || wells[it] }

    /** A safe unopened square that logic proves safe from what is open, or any safe square. */
    fun hint(open: BooleanArray, flags: BooleanArray): Int? {
        val known = deduce(open.copyOf())
        return (0 until rows * cols).firstOrNull { !open[it] && known.first[it] }
            ?: (0 until rows * cols).firstOrNull { !open[it] && !wells[it] }
    }

    /** Applies logic rules; returns (proven safe, proven well). */
    private fun deduce(open: BooleanArray): Pair<BooleanArray, BooleanArray> {
        val n = rows * cols
        val safe = BooleanArray(n); val mine = BooleanArray(n)
        var changed = true
        while (changed) {
            changed = false
            val constraints = ArrayList<Pair<Set<Int>, Int>>()
            for (p in 0 until n) {
                if (!open[p] || counts[p] == 0) continue
                val unknown = neighbours(p).filter { !open[it] && !mine[it] && !safe[it] }.toSet()
                val need = counts[p] - neighbours(p).count { mine[it] }
                if (unknown.isEmpty()) continue
                constraints += unknown to need
            }
            for ((u, need) in constraints) {
                if (need == 0) for (q in u) { safe[q] = true; changed = true }
                else if (need == u.size) for (q in u) { mine[q] = true; changed = true }
            }
            if (!changed) {
                // Subset rule: if A ⊆ B then B \ A holds needB - needA wells.
                outer@ for (i in constraints.indices) for (j in constraints.indices) {
                    if (i == j) continue
                    val (a, na) = constraints[i]; val (b, nb) = constraints[j]
                    if (a.size < b.size && b.containsAll(a)) {
                        val rest = b - a
                        if (nb - na == 0) { rest.forEach { safe[it] = true }; changed = true; break@outer }
                        if (nb - na == rest.size) { rest.forEach { mine[it] = true }; changed = true; break@outer }
                    }
                }
            }
            if (changed) for (p in 0 until n) if (safe[p] && !open[p] && !wells[p]) open(p, open)
        }
        return safe to mine
    }

    fun solvableFrom(first: Int): Boolean {
        val open = BooleanArray(rows * cols)
        open(first, open)
        deduce(open)
        return isCleared(open)
    }

    companion object {
        data class Spec(val rows: Int, val cols: Int, val wells: Int)

        fun specFor(d: Difficulty) = when (d) {
            Difficulty.Easy -> Spec(9, 8, 10)
            Difficulty.Medium -> Spec(10, 9, 15)
            Difficulty.Hard -> Spec(12, 10, 24)
            Difficulty.Expert -> Spec(14, 10, 30)
        }

        fun generate(seed: Long, difficulty: Difficulty, first: Int): HiddenWells {
            val (rows, cols, wanted) = specFor(difficulty)
            val rng = Random(seed * 31 + first)
            val fr = first / cols; val fc = first % cols
            var count = wanted
            while (true) {
                // If a board is stubborn, use one fewer well rather than ship a guess.
                repeat(800) {
                    val cells = (0 until rows * cols).filter { kotlin.math.abs(it / cols - fr) > 1 || kotlin.math.abs(it % cols - fc) > 1 }.toMutableList().shuffled(rng)
                    val wells = BooleanArray(rows * cols)
                    for (k in 0 until count) wells[cells[k]] = true
                    val board = HiddenWells(rows, cols, wells)
                    if (board.solvableFrom(first)) return board
                }
                count--
            }
        }
    }
}
