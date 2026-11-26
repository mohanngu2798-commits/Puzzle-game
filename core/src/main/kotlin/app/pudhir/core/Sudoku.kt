package app.pudhir.core

import kotlin.random.Random

/**
 * Spice Box: classic 9x9 sudoku. Cells hold 1..9 (0 = empty).
 * Every generated puzzle has exactly one solution.
 */
class SudokuPuzzle(val givens: IntArray, val solution: IntArray) {
    companion object {
        const val N = 9

        fun generate(seed: Long, difficulty: Difficulty): SudokuPuzzle {
            val rng = Random(seed)
            val full = IntArray(81)
            fillRandom(full, 0, rng)
            val puzzle = full.copyOf()
            val target = when (difficulty) {
                Difficulty.Easy -> 40
                Difficulty.Medium -> 33
                Difficulty.Hard -> 28
                Difficulty.Expert -> 24
            }
            // Remove cells in symmetric pairs while the puzzle stays unique.
            val order = (0 until 41).toMutableList().shuffled(rng)
            var clues = 81
            for (i in order) {
                if (clues <= target) break
                val j = 80 - i
                val a = puzzle[i]
                val b = puzzle[j]
                puzzle[i] = 0; puzzle[j] = 0
                if (countSolutions(puzzle.copyOf(), 2) != 1) {
                    puzzle[i] = a; puzzle[j] = b
                } else {
                    clues -= if (i == j) 1 else 2
                }
            }
            return SudokuPuzzle(puzzle, full)
        }

        private fun fillRandom(g: IntArray, pos: Int, rng: Random): Boolean {
            if (pos == 81) return true
            val digits = (1..9).toMutableList().shuffled(rng)
            for (d in digits) {
                if (canPlace(g, pos, d)) {
                    g[pos] = d
                    if (fillRandom(g, pos + 1, rng)) return true
                    g[pos] = 0
                }
            }
            return false
        }

        fun canPlace(g: IntArray, pos: Int, d: Int): Boolean {
            val r = pos / 9; val c = pos % 9
            for (k in 0 until 9) {
                if (g[r * 9 + k] == d && r * 9 + k != pos) return false
                if (g[k * 9 + c] == d && k * 9 + c != pos) return false
            }
            val br = r / 3 * 3; val bc = c / 3 * 3
            for (rr in br until br + 3) for (cc in bc until bc + 3) {
                val p = rr * 9 + cc
                if (p != pos && g[p] == d) return false
            }
            return true
        }

        /** Counts solutions up to [limit], choosing the most constrained cell first. */
        fun countSolutions(g: IntArray, limit: Int): Int {
            var best = -1; var bestCount = 10; var bestMask = 0
            for (p in 0 until 81) {
                if (g[p] != 0) continue
                var mask = 0; var cnt = 0
                for (d in 1..9) if (canPlace(g, p, d)) { mask = mask or (1 shl d); cnt++ }
                if (cnt == 0) return 0
                if (cnt < bestCount) { bestCount = cnt; best = p; bestMask = mask; if (cnt == 1) break }
            }
            if (best == -1) return 1
            var total = 0
            for (d in 1..9) {
                if (bestMask and (1 shl d) == 0) continue
                g[best] = d
                total += countSolutions(g, limit - total)
                g[best] = 0
                if (total >= limit) break
            }
            return total
        }
    }
}
