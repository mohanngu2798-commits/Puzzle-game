package app.pudhir.core

import kotlin.random.Random

/**
 * Kolam: draw one closed loop along the dot grid. A number in a cell says how many
 * of that cell's four sides the loop uses. Every puzzle has exactly one loop.
 *
 * Internally the loop is the border of an "inside" region of cells.
 * clues[r*cols+c] is -1 when the cell shows no number.
 */
class KolamPuzzle(val rows: Int, val cols: Int, val clues: IntArray, val inside: BooleanArray) {

    /** Horizontal edge above row r (r in 0..rows) at column c is on the loop. */
    fun hEdge(r: Int, c: Int): Boolean = isIn(r - 1, c) != isIn(r, c)
    /** Vertical edge left of column c (c in 0..cols) at row r is on the loop. */
    fun vEdge(r: Int, c: Int): Boolean = isIn(r, c - 1) != isIn(r, c)

    private fun isIn(r: Int, c: Int) = r in 0 until rows && c in 0 until cols && inside[r * cols + c]

    companion object {
        fun sizeFor(d: Difficulty) = when (d) {
            Difficulty.Easy -> 5; Difficulty.Medium -> 6; Difficulty.Hard -> 7; Difficulty.Expert -> 8
        }

        fun generate(seed: Long, difficulty: Difficulty): KolamPuzzle {
            val n = sizeFor(difficulty)
            val rng = Random(seed)
            while (true) {
                val inside = growRegion(n, n, rng)
                val full = IntArray(n * n) { countSides(inside, n, n, it / n, it % n) }
                val clues = full.copyOf()
                val order = (0 until n * n).toMutableList().shuffled(rng)
                val keepAtLeast = when (difficulty) {
                    Difficulty.Easy -> n * n * 60 / 100
                    Difficulty.Medium -> n * n * 50 / 100
                    Difficulty.Hard -> n * n * 42 / 100
                    Difficulty.Expert -> n * n * 36 / 100
                }
                var shown = n * n
                for (p in order) {
                    if (shown <= keepAtLeast) break
                    val v = clues[p]
                    clues[p] = -1
                    if (Solver(n, n, clues).count(2) != 1) clues[p] = v else shown--
                }
                if (Solver(n, n, clues).count(2) == 1) return KolamPuzzle(n, n, clues, inside)
            }
        }

        fun countSides(inside: BooleanArray, rows: Int, cols: Int, r: Int, c: Int): Int {
            fun inn(rr: Int, cc: Int) = rr in 0 until rows && cc in 0 until cols && inside[rr * cols + cc]
            val me = inn(r, c)
            var k = 0
            if (inn(r - 1, c) != me) k++
            if (inn(r + 1, c) != me) k++
            if (inn(r, c - 1) != me) k++
            if (inn(r, c + 1) != me) k++
            return k
        }

        private fun growRegion(rows: Int, cols: Int, rng: Random): BooleanArray {
            val inside = BooleanArray(rows * cols)
            val target = rows * cols * (45 + rng.nextInt(15)) / 100
            inside[rng.nextInt(rows) * cols + rng.nextInt(cols)] = true
            var size = 1
            var stuck = 0
            while (size < target && stuck < 400) {
                val frontier = (0 until rows * cols).filter { p ->
                    !inside[p] && Cell(p / cols, p % cols).neighbours4().any { it.r in 0 until rows && it.c in 0 until cols && inside[it.r * cols + it.c] }
                }
                if (frontier.isEmpty()) break
                val p = frontier[rng.nextInt(frontier.size)]
                inside[p] = true
                if (validShape(inside, rows, cols)) size++ else { inside[p] = false; stuck++ }
            }
            return inside
        }

        /** Outside connected to the border, inside connected, and no pinch points. */
        fun validShape(inside: BooleanArray, rows: Int, cols: Int): Boolean {
            fun inn(r: Int, c: Int) = r in 0 until rows && c in 0 until cols && inside[r * cols + c]
            // No 2x2 checkerboard (loop touching itself at a dot), including the frame outside the grid.
            for (r in -1 until rows) for (c in -1 until cols) {
                val a = inn(r, c); val b = inn(r, c + 1); val d = inn(r + 1, c); val e = inn(r + 1, c + 1)
                if (a == e && b == d && a != b) return false
            }
            // Inside connected.
            val start = inside.indexOfFirst { it }
            if (start < 0) return false
            if (flood(inside, rows, cols, listOf(start), true) != inside.count { it }) return false
            // Every outside cell reaches the border.
            val border = (0 until rows * cols).filter { p ->
                !inside[p] && (p / cols == 0 || p / cols == rows - 1 || p % cols == 0 || p % cols == cols - 1)
            }
            return flood(inside, rows, cols, border, false) == inside.count { !it }
        }

        private fun flood(inside: BooleanArray, rows: Int, cols: Int, starts: List<Int>, value: Boolean): Int {
            val seen = BooleanArray(rows * cols)
            val stack = ArrayDeque<Int>()
            for (s in starts) { seen[s] = true; stack.addLast(s) }
            var n = 0
            while (stack.isNotEmpty()) {
                val p = stack.removeLast(); n++
                val r = p / cols; val c = p % cols
                for (nb in Cell(r, c).neighbours4()) {
                    if (nb.r !in 0 until rows || nb.c !in 0 until cols) continue
                    val q = nb.r * cols + nb.c
                    if (!seen[q] && inside[q] == value) { seen[q] = true; stack.addLast(q) }
                }
            }
            return n
        }
    }

    /** Counts loops matching the clues (by inside/outside colouring), with a work budget. */
    class Solver(private val rows: Int, private val cols: Int, private val clues: IntArray) {
        private val state = IntArray(rows * cols) { -1 } // -1 unknown, 0 out, 1 in
        private var nodes = 0
        private val budget = 400_000
        private var found = 0
        private var limit = 2

        fun count(limit: Int): Int {
            this.limit = limit
            found = 0; nodes = 0
            search(0)
            return if (nodes > budget) limit else found
        }

        private fun v(r: Int, c: Int): Int = if (r in 0 until rows && c in 0 until cols) state[r * cols + c] else 0

        private fun clueOk(r: Int, c: Int): Boolean {
            if (r !in 0 until rows || c !in 0 until cols) return true
            val clue = clues[r * cols + c]
            if (clue < 0) return true
            val me = v(r, c)
            val ns = intArrayOf(v(r - 1, c), v(r + 1, c), v(r, c - 1), v(r, c + 1))
            if (me == -1) {
                // Either colour must remain possible.
                return okFor(0, ns, clue) || okFor(1, ns, clue)
            }
            return okFor(me, ns, clue)
        }

        private fun okFor(me: Int, ns: IntArray, clue: Int): Boolean {
            var diff = 0; var unk = 0
            for (x in ns) if (x == -1) unk++ else if (x != me) diff++
            return diff <= clue && clue <= diff + unk
        }

        private fun search(p: Int) {
            if (found >= limit || nodes > budget) return
            nodes++
            if (p == rows * cols) {
                val inside = BooleanArray(rows * cols) { state[it] == 1 }
                if (validShape(inside, rows, cols)) found++
                return
            }
            val r = p / cols; val c = p % cols
            for (value in 0..1) {
                state[p] = value
                if (clueOk(r, c) && clueOk(r - 1, c) && clueOk(r, c - 1) && clueOk(r, c + 1) && clueOk(r + 1, c) && noPinch(r, c)) {
                    search(p + 1)
                }
                state[p] = -1
                if (found >= limit || nodes > budget) return
            }
        }

        private fun noPinch(r: Int, c: Int): Boolean {
            // Check the 2x2 block whose bottom-right is (r,c), plus frame blocks completed by this cell.
            fun chk(r0: Int, c0: Int): Boolean {
                val a = v(r0, c0); val b = v(r0, c0 + 1); val d = v(r0 + 1, c0); val e = v(r0 + 1, c0 + 1)
                if (a == -1 || b == -1 || d == -1 || e == -1) return true
                return !(a == e && b == d && a != b)
            }
            return chk(r - 1, c - 1) && (c != cols - 1 || chk(r - 1, c)) && (r != rows - 1 || chk(r, c - 1)) &&
                (r != rows - 1 || c != cols - 1 || chk(r, c))
        }
    }
}
