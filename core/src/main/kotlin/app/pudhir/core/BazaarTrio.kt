package app.pudhir.core

import kotlin.math.abs
import kotlin.random.Random

/** One item on the bazaar table. x,y are in tile units (half steps allowed). */
data class TrioTile(val x: Float, val y: Float, val layer: Int, val face: Int)

/**
 * Bazaar Trio: tap uncovered items to move them into a 7-slot basket.
 * Three of a kind in the basket are sold and vanish. Fill the basket and you lose.
 * Faces are assigned backwards from a full clearance, so every table is clearable.
 */
class BazaarTrio(val tiles: List<TrioTile>, val width: Int, val height: Int) {
    val present = BooleanArray(tiles.size) { true }
    val basket = ArrayList<Int>() // tile indices, grouped by face
    private val undoStack = ArrayList<Pair<Int, List<Int>>>()

    fun isCovered(i: Int): Boolean {
        val t = tiles[i]
        for (j in tiles.indices) {
            if (!present[j] || j == i) continue
            val o = tiles[j]
            if (o.layer > t.layer && abs(o.x - t.x) < 1f && abs(o.y - t.y) < 1f) return true
        }
        return false
    }

    fun isFree(i: Int) = present[i] && !isCovered(i)

    /** Moves a tile to the basket. Returns indices that were cleared as a triple (empty if none). */
    fun take(i: Int): List<Int> {
        if (!isFree(i) || basket.size >= CAPACITY) return emptyList()
        present[i] = false
        val face = tiles[i].face
        val pos = basket.indexOfLast { tiles[it].face == face }.let { if (it == -1) basket.size else it + 1 }
        val before = basket.toList()
        basket.add(pos, i)
        val same = basket.filter { tiles[it].face == face }
        var cleared = emptyList<Int>()
        if (same.size >= 3) { cleared = same.take(3); basket.removeAll(cleared.toSet()) }
        undoStack += i to before
        return cleared
    }

    fun undo(): Boolean {
        val (i, before) = undoStack.removeLastOrNull() ?: return false
        // Undo is only allowed when the last move did not sell a triple.
        if (basket.size != before.size + 1) { undoStack += i to before; return false }
        basket.clear(); basket.addAll(before)
        present[i] = true
        return true
    }

    fun isWon() = present.none { it } && basket.isEmpty()
    fun isLost() = basket.size >= CAPACITY

    /** Suggest a free tile whose face is already in the basket, else any free tile. */
    fun hint(): Int? {
        val free = tiles.indices.filter { isFree(it) }
        val counts = basket.groupingBy { tiles[it].face }.eachCount()
        return free.maxByOrNull { (counts[tiles[it].face] ?: 0) * 10 + free.count { f -> tiles[f].face == tiles[it].face } }
    }

    companion object {
        const val CAPACITY = 7
        const val FACE_KINDS = 20

        fun generate(seed: Long, difficulty: Difficulty): BazaarTrio {
            val rng = Random(seed)
            val (count, kinds, layers) = when (difficulty) {
                Difficulty.Easy -> Triple(27, 6, 3)
                Difficulty.Medium -> Triple(42, 9, 4)
                Difficulty.Hard -> Triple(60, 12, 5)
                Difficulty.Expert -> Triple(75, 15, 6)
            }
            val w = 7; val h = 8
            while (true) {
                val spots = ArrayList<Triple<Float, Float, Int>>()
                for (l in 0 until layers) {
                    val off = if (l % 2 == 1) 0.5f else 0f
                    val cells = ArrayList<Pair<Float, Float>>()
                    for (r in 0 until h - (if (off > 0) 1 else 0)) for (c in 0 until w - (if (off > 0) 1 else 0)) cells += (c + off) to (r + off)
                    cells.shuffle(rng)
                    val per = count / layers + (if (l < count % layers) 1 else 0)
                    for (k in 0 until minOf(per, cells.size)) spots += Triple(cells[k].first, cells[k].second, l)
                }
                if (spots.size != count) continue
                val draft = spots.map { TrioTile(it.first, it.second, it.third, -1) }
                val faces = assignBackwards(draft, kinds, rng) ?: continue
                return BazaarTrio(draft.mapIndexed { i, t -> t.copy(face = faces[i]) }, w, h)
            }
        }

        private fun assignBackwards(tiles: List<TrioTile>, kinds: Int, rng: Random): IntArray? {
            val game = BazaarTrio(tiles, 0, 0)
            val faces = IntArray(tiles.size) { -1 }
            val faceOrder = (0 until tiles.size / 3).map { it % kinds }.toMutableList().shuffled(rng)
            var f = 0
            while (game.present.any { it }) {
                val free = tiles.indices.filter { game.isFree(it) }.toMutableList()
                if (free.size < 3) return null
                free.shuffle(rng)
                for (k in 0 until 3) { faces[free[k]] = faceOrder[f]; game.present[free[k]] = false }
                f++
            }
            return faces
        }
    }
}
