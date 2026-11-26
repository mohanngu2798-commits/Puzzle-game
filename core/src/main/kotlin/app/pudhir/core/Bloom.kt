package app.pudhir.core

import kotlin.random.Random

/**
 * Bloom: fill the garden with jasmine (1) and marigold (2).
 * Each row and column holds equal numbers of both, no three of a kind sit in a line,
 * and no two rows (or two columns) are identical. Exactly one solution.
 */
class BloomPuzzle(val size: Int, val givens: IntArray, val solution: IntArray) {
    companion object {
        fun sizeFor(d: Difficulty) = when (d) {
            Difficulty.Easy -> 6; Difficulty.Medium -> 6; Difficulty.Hard -> 8; Difficulty.Expert -> 10
        }

        fun generate(seed: Long, difficulty: Difficulty): BloomPuzzle {
            val n = sizeFor(difficulty)
            val rng = Random(seed)
            val full = IntArray(n * n)
            check(fill(full, n, 0, rng))
            val puzzle = full.copyOf()
            val order = (0 until n * n).toMutableList().shuffled(rng)
            val keepMin = when (difficulty) {
                Difficulty.Easy -> n * n * 45 / 100
                Difficulty.Medium -> n * n * 30 / 100
                Difficulty.Hard -> n * n * 28 / 100
                Difficulty.Expert -> n * n * 24 / 100
            }
            var shown = n * n
            for (p in order) {
                if (shown <= keepMin) break
                val v = puzzle[p]
                puzzle[p] = 0
                if (count(puzzle.copyOf(), n, 2) != 1) puzzle[p] = v else shown--
            }
            return BloomPuzzle(n, puzzle, full)
        }

        private fun fill(g: IntArray, n: Int, pos: Int, rng: Random): Boolean {
            if (pos == n * n) return valid(g, n, true)
            val first = 1 + rng.nextInt(2)
            for (v in intArrayOf(first, 3 - first)) {
                g[pos] = v
                if (okAt(g, n, pos) && fill(g, n, pos + 1, rng)) return true
            }
            g[pos] = 0
            return false
        }

        /** Local rules around [pos], assuming cells after pos may still be empty. */
        fun okAt(g: IntArray, n: Int, pos: Int): Boolean {
            val r = pos / n; val c = pos % n
            val v = g[pos]
            if (c >= 2 && g[pos - 1] == v && g[pos - 2] == v) return false
            if (r >= 2 && g[pos - n] == v && g[pos - 2 * n] == v) return false
            val rowCount = (0 until n).count { g[r * n + it] == v }
            val colCount = (0 until n).count { g[it * n + c] == v }
            if (rowCount > n / 2 || colCount > n / 2) return false
            // A finished row must not repeat an earlier row.
            if (c == n - 1) for (rr in 0 until r) if ((0 until n).all { g[rr * n + it] == g[r * n + it] }) return false
            if (r == n - 1) for (cc in 0 until c) if ((0 until n).all { g[it * n + cc] == g[it * n + c] }) return false
            return true
        }

        /** Whole-board check; with [complete] every cell must be filled. */
        fun valid(g: IntArray, n: Int, complete: Boolean): Boolean {
            if (complete && g.any { it == 0 }) return false
            for (r in 0 until n) for (c in 0 until n) {
                val v = g[r * n + c]; if (v == 0) continue
                if (c + 2 < n && g[r * n + c + 1] == v && g[r * n + c + 2] == v) return false
                if (r + 2 < n && g[(r + 1) * n + c] == v && g[(r + 2) * n + c] == v) return false
            }
            for (i in 0 until n) for (v in 1..2) {
                if ((0 until n).count { g[i * n + it] == v } > n / 2) return false
                if ((0 until n).count { g[it * n + i] == v } > n / 2) return false
            }
            if (complete) {
                val rows = (0 until n).map { r -> (0 until n).map { g[r * n + it] } }
                val cols = (0 until n).map { c -> (0 until n).map { g[it * n + c] } }
                if (rows.toSet().size != n || cols.toSet().size != n) return false
            }
            return true
        }

        /** Counts solutions up to [limit] by filling empty cells in order. */
        fun count(g: IntArray, n: Int, limit: Int): Int {
            val empties = g.indices.filter { g[it] == 0 }
            var found = 0
            fun rec(k: Int) {
                if (found >= limit) return
                if (k == empties.size) { if (valid(g, n, true)) found++; return }
                val pos = empties[k]
                for (v in 1..2) {
                    g[pos] = v
                    if (localOk(g, n, pos)) rec(k + 1)
                    g[pos] = 0
                    if (found >= limit) return
                }
            }
            rec(0)
            return found
        }

        private fun localOk(g: IntArray, n: Int, pos: Int): Boolean {
            val r = pos / n; val c = pos % n; val v = g[pos]
            for (s in -2..0) {
                val c0 = c + s
                if (c0 >= 0 && c0 + 2 < n && (0..2).all { g[r * n + c0 + it] == v }) return false
                val r0 = r + s
                if (r0 >= 0 && r0 + 2 < n && (0..2).all { g[(r0 + it) * n + c] == v }) return false
            }
            if ((0 until n).count { g[r * n + it] == v } > n / 2) return false
            if ((0 until n).count { g[it * n + c] == v } > n / 2) return false
            return true
        }
    }
}
