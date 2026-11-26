package app.pudhir.core

import kotlin.random.Random

data class Plate(val x: Int, val y: Int, val w: Int, val h: Int, val layer: Int, val tint: Int)
data class Bolt(val x: Float, val y: Float, val plate: Int, val colour: Int)

/**
 * Bolt Bazaar: unscrew uncovered bolts. Each bolt flies into the open box of its colour;
 * otherwise it waits in a 5-slot tray. A box takes 3 bolts, then the next box arrives.
 * A plate drops away once all its bolts are out. Fill the tray and you lose.
 * Colours follow a removal order found while generating, so every board can be cleared.
 */
class BoltBazaar(val plates: List<Plate>, val bolts: List<Bolt>, val boxQueue: List<Int>, val width: Int, val height: Int) {
    val boltIn = BooleanArray(bolts.size) { true }
    val plateIn = BooleanArray(plates.size) { true }
    /** Open boxes: colour and how many bolts they hold. */
    val boxes = ArrayList<IntArray>()
    var nextBox = 0; private set
    val tray = ArrayList<Int>()

    init { repeat(OPEN_BOXES) { openNextBox() } }

    private fun openNextBox() {
        if (nextBox < boxQueue.size) { boxes += intArrayOf(boxQueue[nextBox], 0); nextBox++ }
    }

    fun isFree(b: Int): Boolean {
        if (!boltIn[b]) return false
        val bolt = bolts[b]
        val own = plates[bolt.plate].layer
        for (p in plates.indices) {
            if (!plateIn[p] || p == bolt.plate) continue
            val pl = plates[p]
            if (pl.layer > own && bolt.x > pl.x && bolt.x < pl.x + pl.w && bolt.y > pl.y && bolt.y < pl.y + pl.h) return false
        }
        return true
    }

    sealed interface Result
    data class ToBox(val box: Int, val dropped: List<Int>, val boxDone: Boolean) : Result
    data class ToTray(val dropped: List<Int>) : Result
    data object Blocked : Result

    fun unscrew(b: Int): Result {
        if (!isFree(b)) return Blocked
        val colour = bolts[b].colour
        val box = boxes.indexOfFirst { it[0] == colour && it[1] < 3 }
        if (box == -1 && tray.size >= TRAY) return Blocked
        boltIn[b] = false
        val dropped = dropEmptyPlates()
        if (box == -1) { tray += b; return ToTray(dropped) }
        boxes[box][1]++
        val done = settleBoxes()
        return ToBox(box, dropped, done)
    }

    /** Closes full boxes, opens new ones and moves waiting tray bolts in. Returns true if a box closed. */
    private fun settleBoxes(): Boolean {
        var closedAny = false
        var changed = true
        while (changed) {
            changed = false
            val full = boxes.indexOfFirst { it[1] >= 3 }
            if (full != -1) { boxes.removeAt(full); openNextBox(); closedAny = true; changed = true }
            for (t in tray.toList()) {
                val box = boxes.indexOfFirst { it[0] == bolts[t].colour && it[1] < 3 }
                if (box != -1) { tray.remove(t); boxes[box][1]++; changed = true }
            }
        }
        return closedAny
    }

    private fun dropEmptyPlates(): List<Int> {
        val out = ArrayList<Int>()
        for (p in plates.indices) if (plateIn[p] && bolts.indices.none { boltIn[it] && bolts[it].plate == p }) { plateIn[p] = false; out += p }
        return out
    }

    fun isWon() = boltIn.none { it } && tray.isEmpty()
    fun isLost() = tray.size >= TRAY && bolts.indices.filter { isFree(it) }.none { b -> boxes.any { it[0] == bolts[b].colour && it[1] < 3 } }

    fun hint(): Int? {
        val free = bolts.indices.filter { isFree(it) }
        return free.firstOrNull { b -> boxes.any { it[0] == bolts[b].colour && it[1] < 3 } } ?: free.firstOrNull()
    }

    companion object {
        const val TRAY = 5
        const val OPEN_BOXES = 2
        const val COLOURS = 8

        fun generate(seed: Long, difficulty: Difficulty): BoltBazaar {
            val rng = Random(seed)
            val w = 6; val h = 8
            val (plateCount, colours) = when (difficulty) {
                Difficulty.Easy -> 5 to 4
                Difficulty.Medium -> 8 to 5
                Difficulty.Hard -> 11 to 7
                Difficulty.Expert -> 14 to COLOURS
            }
            while (true) {
                val plates = ArrayList<Plate>()
                val bolts = ArrayList<Triple<Float, Float, Int>>()
                val used = HashSet<Pair<Float, Float>>()
                for (i in 0 until plateCount) {
                    val pw = 2 + rng.nextInt(3); val ph = 2 + rng.nextInt(3)
                    val px = rng.nextInt(w - pw + 1); val py = rng.nextInt(h - ph + 1)
                    plates += Plate(px, py, pw, ph, i, rng.nextInt(6))
                    // Bolts sit near the plate's corners.
                    val corners = mutableListOf(
                        px + 0.5f to py + 0.5f, px + pw - 0.5f to py + 0.5f,
                        px + 0.5f to py + ph - 0.5f, px + pw - 0.5f to py + ph - 0.5f,
                    ).filter { it !in used }.toMutableList().shuffled(rng)
                    val k = minOf(corners.size, 2 + rng.nextInt(2))
                    for (j in 0 until k) { bolts += Triple(corners[j].first, corners[j].second, i); used += corners[j] }
                }
                if (plates.indices.any { p -> bolts.none { it.third == p } }) continue
                // Trim to a multiple of 3 by dropping bolts from plates with more than one.
                while (bolts.size % 3 != 0) {
                    val idx = bolts.indices.firstOrNull { i -> bolts.count { it.third == bolts[i].third } > 1 } ?: break
                    bolts.removeAt(idx)
                }
                if (bolts.size % 3 != 0) continue
                val draft = BoltBazaar(plates, bolts.map { Bolt(it.first, it.second, it.third, 0) }, emptyList(), w, h)
                // Find a removal order, then colour bolts in triples along it.
                val order = ArrayList<Int>()
                var ok = true
                while (draft.boltIn.any { it }) {
                    val free = draft.bolts.indices.filter { draft.isFree(it) }
                    if (free.isEmpty()) { ok = false; break }
                    val b = free[rng.nextInt(free.size)]
                    draft.boltIn[b] = false; draft.dropEmptyPlates(); order += b
                }
                if (!ok) continue
                val colourOf = IntArray(bolts.size)
                val queue = ArrayList<Int>()
                var last = -1
                for (g in 0 until order.size / 3) {
                    var c = rng.nextInt(colours)
                    if (c == last && colours > 1) c = (c + 1) % colours
                    last = c
                    queue += c
                    for (k in 0 until 3) colourOf[order[g * 3 + k]] = c
                }
                val finalBolts = bolts.mapIndexed { i, t -> Bolt(t.first, t.second, t.third, colourOf[i]) }
                return BoltBazaar(plates, finalBolts, queue, w, h)
            }
        }
    }
}
