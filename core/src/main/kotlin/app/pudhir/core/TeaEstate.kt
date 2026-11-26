package app.pudhir.core

import kotlin.math.abs
import kotlin.random.Random

/**
 * Tea Estate: plant exactly one tea bush in every row, every column and every plot.
 * No two bushes may touch, not even diagonally. Exactly one solution.
 * regions[r*n+c] is the plot number of a cell; solution[r] is the bush column in row r.
 */
class TeaEstatePuzzle(val size: Int, val regions: IntArray, val solution: IntArray) {
    companion object {
        fun sizeFor(d: Difficulty) = when (d) {
            Difficulty.Easy -> 5; Difficulty.Medium -> 6; Difficulty.Hard -> 7; Difficulty.Expert -> 8
        }

        fun generate(seed: Long, difficulty: Difficulty): TeaEstatePuzzle {
            val n = sizeFor(difficulty)
            val rng = Random(seed)
            while (true) {
                val stars = placeStars(n, rng) ?: continue
                val regions = growRegions(n, stars, rng)
                // Nudge plot borders until the answer is unique.
                var attempts = 0
                while (attempts < 300) {
                    val sols = solve(n, regions, 2)
                    if (sols.size == 1) return TeaEstatePuzzle(n, regions, stars)
                    // Move a random border cell (not a star) into a neighbouring plot.
                    val p = rng.nextInt(n * n)
                    val r = p / n; val c = p % n
                    if (stars[r] == c) { attempts++; continue }
                    val nb = Cell(r, c).neighbours4().filter { it.r in 0 until n && it.c in 0 until n && regions[it.r * n + it.c] != regions[p] }
                    if (nb.isEmpty()) { attempts++; continue }
                    val to = regions[nb[rng.nextInt(nb.size)].let { it.r * n + it.c }]
                    val old = regions[p]
                    regions[p] = to
                    if (!connected(n, regions, old)) regions[p] = old
                    attempts++
                }
            }
        }

        private fun placeStars(n: Int, rng: Random): IntArray? {
            val cols = IntArray(n) { -1 }
            val used = BooleanArray(n)
            fun rec(r: Int): Boolean {
                if (r == n) return true
                for (c in (0 until n).toMutableList().shuffled(rng)) {
                    if (used[c] || (r > 0 && abs(cols[r - 1] - c) <= 1)) continue
                    cols[r] = c; used[c] = true
                    if (rec(r + 1)) return true
                    used[c] = false
                }
                return false
            }
            return if (rec(0)) cols else null
        }

        private fun growRegions(n: Int, stars: IntArray, rng: Random): IntArray {
            val reg = IntArray(n * n) { -1 }
            val frontier = ArrayList<Int>()
            for (r in 0 until n) { reg[r * n + stars[r]] = r; frontier += r * n + stars[r] }
            while (frontier.isNotEmpty()) {
                val i = rng.nextInt(frontier.size)
                val p = frontier[i]
                val free = Cell(p / n, p % n).neighbours4().filter { it.r in 0 until n && it.c in 0 until n && reg[it.r * n + it.c] == -1 }
                if (free.isEmpty()) { frontier.removeAt(i); continue }
                val q = free[rng.nextInt(free.size)].let { it.r * n + it.c }
                reg[q] = reg[p]; frontier += q
            }
            return reg
        }

        private fun connected(n: Int, reg: IntArray, id: Int): Boolean {
            val cells = reg.indices.filter { reg[it] == id }
            if (cells.isEmpty()) return false
            val seen = HashSet<Int>(); val stack = ArrayDeque(listOf(cells[0])); seen += cells[0]
            while (stack.isNotEmpty()) {
                val p = stack.removeLast()
                for (nb in Cell(p / n, p % n).neighbours4()) {
                    if (nb.r !in 0 until n || nb.c !in 0 until n) continue
                    val q = nb.r * n + nb.c
                    if (reg[q] == id && seen.add(q)) stack.addLast(q)
                }
            }
            return seen.size == cells.size
        }

        /** All solutions up to [limit]; each is the bush column per row. */
        fun solve(n: Int, reg: IntArray, limit: Int): List<IntArray> {
            val out = ArrayList<IntArray>()
            val cols = IntArray(n); val colUsed = BooleanArray(n); val regUsed = BooleanArray(n)
            fun rec(r: Int) {
                if (out.size >= limit) return
                if (r == n) { out += cols.copyOf(); return }
                for (c in 0 until n) {
                    val g = reg[r * n + c]
                    if (colUsed[c] || regUsed[g] || (r > 0 && abs(cols[r - 1] - c) <= 1)) continue
                    cols[r] = c; colUsed[c] = true; regUsed[g] = true
                    rec(r + 1)
                    colUsed[c] = false; regUsed[g] = false
                }
            }
            rec(0)
            return out
        }
    }
}
