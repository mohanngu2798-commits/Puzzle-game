package app.pudhir.core

import kotlin.random.Random

/**
 * Godown: you are the warehouse keeper. Push every crate onto a marked spot.
 * Crates can be pushed, never pulled, and only one at a time.
 * Levels are built by pulling crates backwards off their spots, so every level is solvable,
 * and the generator keeps the forward solution for hints.
 */
class Godown(
    val rows: Int, val cols: Int,
    val walls: BooleanArray, val goals: BooleanArray,
    startBoxes: Set<Int>, startPlayer: Int,
    val solution: List<Dir>,
) {
    val boxes = startBoxes.toMutableSet()
    var player = startPlayer; private set
    val history = ArrayList<Triple<Dir, Boolean, Int>>() // direction, pushed a crate, player before
    private val initialBoxes = startBoxes.toSet()
    private val initialPlayer = startPlayer

    private fun step(p: Int, d: Dir): Int? {
        val r = p / cols + d.dr; val c = p % cols + d.dc
        if (r !in 0 until rows || c !in 0 until cols) return null
        val q = r * cols + c
        return if (walls[q]) null else q
    }

    /** Moves the keeper; returns 0 if blocked, 1 walked, 2 pushed. */
    fun move(d: Dir): Int {
        val next = step(player, d) ?: return 0
        if (next in boxes) {
            val beyond = step(next, d) ?: return 0
            if (beyond in boxes) return 0
            boxes.remove(next); boxes.add(beyond)
            history += Triple(d, true, player); player = next
            return 2
        }
        history += Triple(d, false, player); player = next
        return 1
    }

    fun undo(): Boolean {
        val (d, pushed, before) = history.removeLastOrNull() ?: return false
        if (pushed) {
            val boxNow = step(player, d)!!
            boxes.remove(boxNow); boxes.add(player)
        }
        player = before
        return true
    }

    fun restart() {
        boxes.clear(); boxes.addAll(initialBoxes); player = initialPlayer; history.clear()
    }

    fun isSolved() = boxes.all { goals[it] }

    /** Next move along the stored solution; undoes back to the solution path first if needed. */
    fun hint(): Dir? {
        while (history.size > solution.size || history.indices.any { history[it].first != solution[it] }) {
            if (!undo()) break
        }
        return solution.getOrNull(history.size)
    }

    companion object {
        fun spec(d: Difficulty) = when (d) {
            Difficulty.Easy -> Triple(7, 7, 2)
            Difficulty.Medium -> Triple(8, 7, 3)
            Difficulty.Hard -> Triple(9, 8, 3)
            Difficulty.Expert -> Triple(10, 8, 4)
        }

        fun generate(seed: Long, difficulty: Difficulty): Godown {
            val rng = Random(seed)
            val (rows, cols, crates) = spec(difficulty)
            var best: Godown? = null; var bestScore = -1
            repeat(60) {
                val g = attempt(rows, cols, crates, rng, difficulty) ?: return@repeat
                val score = g.solution.size + 6 * g.boxes.count { b -> !g.goals[b] }
                if (score > bestScore) { bestScore = score; best = g }
            }
            return best ?: generate(seed + 1, difficulty)
        }

        private fun attempt(rows: Int, cols: Int, crates: Int, rng: Random, d: Difficulty): Godown? {
            val walls = BooleanArray(rows * cols) { p -> p / cols == 0 || p / cols == rows - 1 || p % cols == 0 || p % cols == cols - 1 }
            // A few inner pillars and wall stubs.
            repeat((rows * cols) / 9) {
                val p = rng.nextInt(rows * cols)
                walls[p] = true
            }
            val floor = (0 until rows * cols).filter { !walls[it] }
            if (!connected(rows, cols, walls, floor)) return null
            // Goals away from corners so crates can leave them.
            val goalCandidates = floor.filter { p -> Dir.entries.count { dd -> openAt(rows, cols, walls, p, dd) } >= 3 }.toMutableList().shuffled(rng)
            if (goalCandidates.size < crates + 2) return null
            val goals = BooleanArray(rows * cols)
            val boxes = HashSet<Int>()
            for (k in 0 until crates) { goals[goalCandidates[k]] = true; boxes += goalCandidates[k] }
            val free = floor.filter { it !in boxes }
            var player = free[rng.nextInt(free.size)]
            // Reverse play: walk and pull crates.
            data class Rev(val from: Int, val to: Int, val pulled: Boolean)
            val rev = ArrayList<Rev>()
            val steps = when (d) { Difficulty.Easy -> 60; Difficulty.Medium -> 110; Difficulty.Hard -> 170; Difficulty.Expert -> 240 }
            repeat(steps) {
                val dirs = Dir.entries.shuffled(rng)
                for (dd in dirs) {
                    val to = neighbour(rows, cols, player, dd) ?: continue
                    if (walls[to] || to in boxes) continue
                    // Pull a crate standing on the opposite side, most of the time.
                    val behind = neighbour(rows, cols, player, opposite(dd))
                    val pull = behind != null && behind in boxes && rng.nextInt(10) < 7
                    if (pull) { boxes.remove(behind!!); boxes.add(player) }
                    rev += Rev(player, to, pull)
                    player = to
                    break
                }
            }
            if (boxes.all { goals[it] }) return null
            if (boxes.count { !goals[it] } < maxOf(1, crates - 1)) return null
            // Forward solution: replay the reverse steps backwards.
            val forward = rev.reversed().map { r -> dirBetween(cols, r.to, r.from) }
            // Trim pointless walking at the end (after the last push the puzzle is already solved).
            val g = Godown(rows, cols, walls, goals, boxes.toSet(), player, forward)
            val sim = Godown(rows, cols, walls, goals, boxes.toSet(), player, forward)
            var solvedAt = -1
            for ((i, dd) in forward.withIndex()) { sim.move(dd); if (sim.isSolved()) { solvedAt = i; break } }
            if (solvedAt < 0) return null
            return Godown(rows, cols, walls, goals, g.boxes.toSet(), player, forward.subList(0, solvedAt + 1).toList())
        }

        private fun opposite(d: Dir) = when (d) { Dir.Up -> Dir.Down; Dir.Down -> Dir.Up; Dir.Left -> Dir.Right; Dir.Right -> Dir.Left }
        private fun neighbour(rows: Int, cols: Int, p: Int, d: Dir): Int? {
            val r = p / cols + d.dr; val c = p % cols + d.dc
            return if (r in 0 until rows && c in 0 until cols) r * cols + c else null
        }
        private fun openAt(rows: Int, cols: Int, walls: BooleanArray, p: Int, d: Dir) = neighbour(rows, cols, p, d)?.let { !walls[it] } ?: false
        private fun dirBetween(cols: Int, a: Int, b: Int): Dir = when (b - a) { -cols -> Dir.Up; cols -> Dir.Down; -1 -> Dir.Left; else -> Dir.Right }
        private fun connected(rows: Int, cols: Int, walls: BooleanArray, floor: List<Int>): Boolean {
            if (floor.isEmpty()) return false
            val seen = HashSet<Int>(); val st = ArrayDeque(listOf(floor[0])); seen += floor[0]
            while (st.isNotEmpty()) {
                val p = st.removeLast()
                for (d in Dir.entries) { val q = neighbour(rows, cols, p, d) ?: continue; if (!walls[q] && seen.add(q)) st.addLast(q) }
            }
            return seen.size == floor.size
        }
    }
}
