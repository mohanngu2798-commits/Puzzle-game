package app.pudhir.core

import kotlin.random.Random

data class TilePos(val layer: Int, val r: Int, val c: Int)

/**
 * Temple Tiles: mahjong solitaire. Match two free tiles with the same symbol.
 * A tile is free when nothing sits on top of it and its left or right side is open.
 * Deals are built backwards from a full clearance, so every deal can be solved.
 */
class TempleTiles(val layoutName: String, val positions: List<TilePos>, val faces: IntArray) {
    val present = BooleanArray(positions.size) { true }
    private val at = HashMap<TilePos, Int>().apply { positions.forEachIndexed { i, p -> put(p, i) } }
    private val history = ArrayList<Pair<Int, Int>>()
    val rows = positions.maxOf { it.r } + 1
    val cols = positions.maxOf { it.c } + 1
    val layers = positions.maxOf { it.layer } + 1

    private fun occupied(p: TilePos) = at[p]?.let { present[it] } ?: false

    fun isFree(i: Int): Boolean {
        if (!present[i]) return false
        val p = positions[i]
        if (occupied(TilePos(p.layer + 1, p.r, p.c))) return false
        val left = occupied(TilePos(p.layer, p.r, p.c - 1))
        val right = occupied(TilePos(p.layer, p.r, p.c + 1))
        return !left || !right
    }

    fun tryMatch(a: Int, b: Int): Boolean {
        if (a == b || !isFree(a) || !isFree(b) || faces[a] != faces[b]) return false
        present[a] = false; present[b] = false
        history += a to b
        return true
    }

    fun undo(): Pair<Int, Int>? {
        val last = history.removeLastOrNull() ?: return null
        present[last.first] = true; present[last.second] = true
        return last
    }

    fun remaining() = present.count { it }
    fun isCleared() = remaining() == 0

    fun hint(): Pair<Int, Int>? {
        val free = positions.indices.filter { isFree(it) }
        for (i in free.indices) for (j in i + 1 until free.size) if (faces[free[i]] == faces[free[j]]) return free[i] to free[j]
        return null
    }

    /** Re-deals the remaining tiles so the board can still be cleared. */
    fun shuffle(rng: Random) {
        val idx = positions.indices.filter { present[it] }
        val pool = idx.map { faces[it] }.sorted()
        val pairs = pool.chunked(2).map { it[0] }.toMutableList().shuffled(rng)
        repeat(200) {
            val assigned = dealBackwards(idx, pairs, rng)
            if (assigned != null) { for ((k, v) in assigned) faces[k] = v; history.clear(); return }
        }
    }

    private fun dealBackwards(idx: List<Int>, pairFaces: List<Int>, rng: Random): Map<Int, Int>? {
        val saved = present.copyOf()
        val out = HashMap<Int, Int>()
        var f = 0
        while (true) {
            val free = idx.filter { present[it] && isFree(it) }
            if (free.isEmpty()) break
            if (free.size < 2) { saved.copyInto(present); return null }
            val a = free[rng.nextInt(free.size)]
            var b: Int
            do { b = free[rng.nextInt(free.size)] } while (b == a)
            out[a] = pairFaces[f]; out[b] = pairFaces[f]; f++
            present[a] = false; present[b] = false
        }
        val ok = idx.none { present[it] }
        saved.copyInto(present)
        return if (ok) out else null
    }

    companion object {
        const val FACE_KINDS = 34

        /** Layer masks, bottom layer first. '#' = tile. */
        val LAYOUTS: List<Pair<String, List<List<String>>>> = listOf(
            "Lotus" to listOf(
                listOf("..####..", ".######.", "########", "########", "########", "########", ".######.", "..####.."),
                listOf("........", "..####..", ".######.", ".######.", ".######.", ".######.", "..####..", "........"),
                listOf("........", "........", "...##...", "..####..", "..####..", "...##...", "........", "........"),
            ),
            "Gopuram" to listOf(
                listOf("########", "########", ".######.", ".######.", "..####..", "..####..", "########", "########"),
                listOf("........", ".######.", ".######.", "..####..", "..####..", "...##...", ".######.", "........"),
                listOf("........", "........", "..####..", "...##...", "...##...", "........", "...##...", "........"),
            ),
            "Diamond" to listOf(
                listOf("...##...", "..####..", ".######.", "########", "########", ".######.", "..####..", "...##..."),
                listOf("........", "...##...", "..####..", ".######.", ".######.", "..####..", "...##...", "........"),
                listOf("........", "........", "...##...", "..####..", "..####..", "...##...", "........", "........"),
                listOf("........", "........", "........", "...##...", "...##...", "........", "........", "........"),
            ),
            "Courtyard" to listOf(
                listOf("########", "#......#", "#.####.#", "#.####.#", "#.####.#", "#.####.#", "#......#", "########"),
                listOf("........", "........", "..####..", "..#..#..", "..#..#..", "..####..", "........", "........"),
                listOf("##....##", "........", "........", "...##...", "...##...", "........", "........", "##....##"),
            ),
        )

        fun layoutFor(d: Difficulty, rng: Random): Int = when (d) {
            Difficulty.Easy -> 3
            Difficulty.Medium -> rng.nextInt(2) * 2 + 1 // Gopuram or Courtyard
            else -> rng.nextInt(LAYOUTS.size)
        }

        fun positionsOf(masks: List<List<String>>): List<TilePos> {
            val out = ArrayList<TilePos>()
            masks.forEachIndexed { l, rows -> rows.forEachIndexed { r, s -> s.forEachIndexed { c, ch -> if (ch == '#') out += TilePos(l, r, c) } } }
            // A tile can only sit on a tile.
            val set = out.toHashSet()
            return out.filter { it.layer == 0 || TilePos(it.layer - 1, it.r, it.c) in set }
        }

        fun generate(seed: Long, difficulty: Difficulty): TempleTiles {
            val rng = Random(seed)
            val li = layoutFor(difficulty, rng)
            var pos = positionsOf(LAYOUTS[li].second)
            if (pos.size % 2 == 1) pos = pos.dropLast(1)
            val kinds = when (difficulty) {
                Difficulty.Easy -> 12; Difficulty.Medium -> 18; Difficulty.Hard -> 26; Difficulty.Expert -> FACE_KINDS
            }
            while (true) {
                val pairFaces = (0 until pos.size / 2).map { it % kinds }.toMutableList().shuffled(rng)
                val game = TempleTiles(LAYOUTS[li].first, pos, IntArray(pos.size))
                val dealt = game.dealBackwards(pos.indices.toList(), pairFaces, rng) ?: continue
                for ((k, v) in dealt) game.faces[k] = v
                return game
            }
        }
    }
}
