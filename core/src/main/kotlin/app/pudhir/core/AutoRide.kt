package app.pudhir.core

import kotlin.random.Random

/**
 * Auto Ride: drive one route that visits every square exactly once,
 * starting at stop 1 and passing the numbered stops in order. Exactly one route works.
 * [stops] maps cell index to stop number (1-based); [route] is the solution.
 */
class AutoRidePuzzle(val size: Int, val stops: Map<Int, Int>, val route: List<Int>) {
    val lastStop get() = stops.values.max()

    companion object {
        fun sizeFor(d: Difficulty) = when (d) {
            Difficulty.Easy -> 5; Difficulty.Medium -> 6; Difficulty.Hard -> 7; Difficulty.Expert -> 7
        }

        fun generate(seed: Long, difficulty: Difficulty): AutoRidePuzzle {
            val n = sizeFor(difficulty)
            val rng = Random(seed)
            while (true) {
                val path = ThreadsPuzzle.hamiltonian(n, rng).map { it.r * n + it.c }
                // Start with stops every few squares, then remove stops while the route stays unique.
                val step = when (difficulty) { Difficulty.Easy -> 3; Difficulty.Medium -> 4; Difficulty.Hard -> 4; Difficulty.Expert -> 5 }
                val idx = sortedSetOf(0, path.size - 1)
                var i = step
                while (i < path.size - 1) { idx += i; i += step + rng.nextInt(2) }
                if (count(n, path, idx.toList(), 2) != 1) continue
                for (k in idx.toList().filter { it != 0 && it != path.size - 1 }.shuffled(rng)) {
                    idx.remove(k)
                    if (count(n, path, idx.toList(), 2) != 1) idx.add(k)
                }
                val stops = idx.toList().withIndex().associate { (num, at) -> path[at] to num + 1 }
                return AutoRidePuzzle(n, stops, path)
            }
        }

        /** Counts routes through stops (given as path indices into [path]) up to [limit]. */
        fun count(n: Int, path: List<Int>, stopIdx: List<Int>, limit: Int): Int {
            val stopAt = HashMap<Int, Int>() // cell -> order
            stopIdx.forEachIndexed { order, at -> stopAt[path[at]] = order }
            val total = n * n
            val visited = BooleanArray(total)
            var found = 0
            var nodes = 0
            fun neighbours(p: Int): List<Int> {
                val r = p / n; val c = p % n
                val out = ArrayList<Int>(4)
                if (r > 0) out += p - n; if (r < n - 1) out += p + n; if (c > 0) out += p - 1; if (c < n - 1) out += p + 1
                return out
            }
            fun reachableOk(head: Int, left: Int): Boolean {
                // All unvisited cells must be connected to the head.
                if (left == 0) return true
                val seen = BooleanArray(total); val st = ArrayDeque<Int>()
                var k = 0
                for (q in neighbours(head)) if (!visited[q]) { seen[q] = true; st.addLast(q) }
                while (st.isNotEmpty()) { val q = st.removeLast(); k++; for (w in neighbours(q)) if (!visited[w] && !seen[w]) { seen[w] = true; st.addLast(w) } }
                return k == left
            }
            fun rec(p: Int, depth: Int, next: Int) {
                if (found >= limit || ++nodes > 120_000) { if (nodes > 120_000) found = limit; return }
                if (depth == total) { if (next == stopIdx.size) found++; return }
                if (!reachableOk(p, total - depth)) return
                for (q in neighbours(p)) {
                    if (visited[q]) continue
                    val o = stopAt[q]
                    if (o != null && o != next) continue
                    if (o == null && depth + 1 == total) continue
                    visited[q] = true
                    rec(q, depth + 1, if (o != null) next + 1 else next)
                    visited[q] = false
                    if (found >= limit) return
                }
            }
            val start = path[stopIdx[0]]
            visited[start] = true
            rec(start, 1, 1)
            return found
        }
    }
}
