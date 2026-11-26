package app.pudhir.core

import kotlin.random.Random

/**
 * Chai Sort: pour coloured tea between glasses until every glass holds one colour.
 * You can pour onto an empty glass or onto the same colour, while there is room.
 */
class ChaiSort(val capacity: Int, glasses: List<List<Int>>) {
    val glasses: List<MutableList<Int>> = glasses.map { it.toMutableList() }

    fun copy() = ChaiSort(capacity, glasses)

    fun canPour(from: Int, to: Int): Boolean {
        if (from == to) return false
        val a = glasses[from]; val b = glasses[to]
        if (a.isEmpty() || b.size >= capacity) return false
        if (b.isNotEmpty() && b.last() != a.last()) return false
        // Pointless: moving a complete single-colour glass into an empty one.
        if (b.isEmpty() && a.all { it == a[0] }) return false
        return true
    }

    /** Pours and returns how many units moved (0 if not allowed). */
    fun pour(from: Int, to: Int): Int {
        if (!canPour(from, to)) return 0
        val a = glasses[from]; val b = glasses[to]
        val colour = a.last()
        var moved = 0
        while (a.isNotEmpty() && a.last() == colour && b.size < capacity) { b.add(a.removeAt(a.lastIndex)); moved++ }
        return moved
    }

    fun isSolved() = glasses.all { g -> g.isEmpty() || (g.size == capacity && g.all { it == g[0] }) }

    private fun key(): String = glasses.map { it.joinToString(",") }.sorted().joinToString("|")

    /** Finds a sequence of pours that solves the current position, or null. */
    fun solve(budget: Int = 150_000): List<Pair<Int, Int>>? {
        val seen = HashSet<String>()
        val moves = ArrayList<Pair<Int, Int>>()
        var nodes = 0
        fun dfs(s: ChaiSort): Boolean {
            if (s.isSolved()) return true
            if (++nodes > budget) return false
            if (!seen.add(s.key())) return false
            val n = s.glasses.size
            for (i in 0 until n) for (j in 0 until n) {
                if (!s.canPour(i, j)) continue
                val nx = s.copy()
                nx.pour(i, j)
                moves += i to j
                if (dfs(nx)) return true
                moves.removeAt(moves.lastIndex)
            }
            return false
        }
        return if (dfs(copy())) moves else null
    }

    companion object {
        fun coloursFor(d: Difficulty) = when (d) {
            Difficulty.Easy -> 4; Difficulty.Medium -> 6; Difficulty.Hard -> 8; Difficulty.Expert -> 10
        }

        fun generate(seed: Long, difficulty: Difficulty): ChaiSort {
            val rng = Random(seed)
            val k = coloursFor(difficulty)
            val cap = 4
            while (true) {
                val units = (0 until k).flatMap { c -> List(cap) { c } }.toMutableList().shuffled(rng)
                val glasses = (0 until k).map { g -> units.subList(g * cap, g * cap + cap).toList() } + listOf(emptyList(), emptyList())
                val game = ChaiSort(cap, glasses)
                // Avoid glasses that start already sorted or nearly sorted.
                if (game.glasses.any { g -> g.isNotEmpty() && g.all { it == g[0] } }) continue
                if (game.glasses.take(k).any { g -> g[1] == g[2] && g[2] == g[3] }) continue
                if (game.solve() != null) return game
            }
        }
    }
}
