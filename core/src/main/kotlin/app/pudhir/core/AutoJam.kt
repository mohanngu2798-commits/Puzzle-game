package app.pudhir.core

import kotlin.random.Random

data class Vehicle(val row: Int, val col: Int, val len: Int, val horizontal: Boolean) {
    fun cells(): List<Cell> = (0 until len).map { if (horizontal) Cell(row, col + it) else Cell(row + it, col) }
}

/**
 * Auto Jam: slide the cars and lorries along their lanes so the golden auto
 * (vehicle 0) can drive out of the gate on the right of its row.
 */
class AutoJam(val vehicles: MutableList<Vehicle>, val size: Int = 6) {
    val exitRow = vehicles[0].row

    fun copy() = AutoJam(vehicles.toMutableList(), size)

    private fun occupied(except: Int = -1): BooleanArray {
        val g = BooleanArray(size * size)
        for ((i, v) in vehicles.withIndex()) if (i != except) for (c in v.cells()) g[c.r * size + c.c] = true
        return g
    }

    /** How far vehicle [i] can move: (most negative, most positive). */
    fun range(i: Int): Pair<Int, Int> {
        val g = occupied(i)
        val v = vehicles[i]
        var lo = 0; var hi = 0
        if (v.horizontal) {
            while (v.col + lo - 1 >= 0 && !g[v.row * size + v.col + lo - 1]) lo--
            while (v.col + v.len + hi < size && !g[v.row * size + v.col + v.len + hi]) hi++
        } else {
            while (v.row + lo - 1 >= 0 && !g[(v.row + lo - 1) * size + v.col]) lo--
            while (v.row + v.len + hi < size && !g[(v.row + v.len + hi) * size + v.col]) hi++
        }
        return lo to hi
    }

    fun move(i: Int, delta: Int): Boolean {
        val (lo, hi) = range(i)
        if (delta == 0 || delta < lo || delta > hi) return false
        val v = vehicles[i]
        vehicles[i] = if (v.horizontal) v.copy(col = v.col + delta) else v.copy(row = v.row + delta)
        return true
    }

    fun isSolved(): Boolean {
        val v = vehicles[0]
        return v.col + v.len == size
    }

    private fun key(): String = vehicles.joinToString(",") { "${it.row}${it.col}" }

    /** Shortest solution as (vehicle, delta) moves, or null when there is none. */
    fun solve(limit: Int = 30_000): List<Pair<Int, Int>>? {
        val start = key()
        val prev = HashMap<String, Triple<String, Int, Int>>()
        val states = HashMap<String, List<Vehicle>>()
        states[start] = vehicles.toList()
        val queue = ArrayDeque(listOf(start))
        prev[start] = Triple("", -1, 0)
        while (queue.isNotEmpty()) {
            if (prev.size > limit) return null
            val k = queue.removeFirst()
            val st = AutoJam(states[k]!!.toMutableList(), size)
            if (st.vehicles[0].col + st.vehicles[0].len == size) {
                val path = ArrayList<Pair<Int, Int>>()
                var cur = k
                while (true) { val (pk, vi, d) = prev[cur]!!; if (vi < 0) break; path += vi to d; cur = pk }
                return path.reversed()
            }
            for (i in st.vehicles.indices) {
                val (lo, hi) = st.range(i)
                for (d in lo..hi) {
                    if (d == 0) continue
                    val nx = st.copy(); nx.move(i, d)
                    val nk = nx.key()
                    if (nk in prev) continue
                    prev[nk] = Triple(k, i, d); states[nk] = nx.vehicles.toList(); queue.addLast(nk)
                }
            }
        }
        return null
    }

    companion object {
        fun movesFor(d: Difficulty) = when (d) {
            Difficulty.Easy -> 4..8; Difficulty.Medium -> 8..13; Difficulty.Hard -> 12..20; Difficulty.Expert -> 16..60
        }

        fun generate(seed: Long, difficulty: Difficulty): AutoJam {
            val rng = Random(seed)
            val want = movesFor(difficulty)
            var best: AutoJam? = null; var bestMoves = -1
            repeat(150) {
                val game = randomBoard(rng, 9 + rng.nextInt(5)) ?: return@repeat
                val sol = game.solve() ?: return@repeat
                val m = sol.size
                if (m in want) return game
                // Keep the closest to the wanted range.
                val score = if (m < want.first) m else want.last * 2 - m
                if (score > bestMoves) { bestMoves = score; best = game }
            }
            return best ?: generate(seed + 1, Difficulty.Easy)
        }

        private fun randomBoard(rng: Random, count: Int): AutoJam? {
            val size = 6
            val list = mutableListOf(Vehicle(2, rng.nextInt(3), 2, true))
            val grid = BooleanArray(36)
            for (c in list[0].cells()) grid[c.r * size + c.c] = true
            var tries = 0
            while (list.size < count && tries < 300) {
                tries++
                val horizontal = rng.nextBoolean()
                val len = if (rng.nextInt(4) == 0) 3 else 2
                val r = rng.nextInt(if (horizontal) size else size - len + 1)
                val c = rng.nextInt(if (horizontal) size - len + 1 else size)
                if (horizontal && r == 2) continue // nothing else drives in the exit lane sideways
                val v = Vehicle(r, c, len, horizontal)
                if (v.cells().any { grid[it.r * size + it.c] }) continue
                list += v
                for (cell in v.cells()) grid[cell.r * size + cell.c] = true
            }
            val g = AutoJam(list, size)
            return if (g.vehicles[0].col + 2 == size) null else g
        }
    }
}
