package app.pudhir.core

import kotlin.random.Random

enum class Dir(val dr: Int, val dc: Int) { Up(-1, 0), Down(1, 0), Left(0, -1), Right(0, 1) }

/** A tile movement for animation: from cell to cell, with the value it ends up as. */
data class TileMove(val id: Int, val from: Cell, val to: Cell, val value: Int, val merged: Boolean)

/**
 * Gold Merge: slide the coins; two equal coins merge into one worth double.
 * Reach the gold target for the level. Runs out of moves and the round ends.
 */
class GoldMerge(seed: Long, val size: Int = 4, val target: Int = 2048) {
    private val rng = Random(seed)
    val values = IntArray(size * size)
    val ids = IntArray(size * size)
    private var nextId = 1
    var score = 0; private set
    var lastSpawn = -1; private set

    init { spawn(); spawn() }

    private fun spawn() {
        val empty = values.indices.filter { values[it] == 0 }
        if (empty.isEmpty()) return
        val p = empty[rng.nextInt(empty.size)]
        values[p] = if (rng.nextInt(10) == 0) 4 else 2
        ids[p] = nextId++
        lastSpawn = p
    }

    /** Slides in [d]. Returns the tile moves, or empty if nothing moved. */
    fun slide(d: Dir): List<TileMove> {
        val moves = ArrayList<TileMove>()
        val newV = IntArray(size * size)
        val newId = IntArray(size * size)
        var moved = false
        for (line in 0 until size) {
            // Cells of this line ordered from the edge we slide towards.
            val cells = (0 until size).map { k ->
                when (d) {
                    Dir.Left -> Cell(line, k); Dir.Right -> Cell(line, size - 1 - k)
                    Dir.Up -> Cell(k, line); Dir.Down -> Cell(size - 1 - k, line)
                }
            }
            var write = 0
            var lastMergeable = -1 // index into cells of the last placed tile that may still merge
            for (cell in cells) {
                val p = cell.r * size + cell.c
                val v = values[p]
                if (v == 0) continue
                if (lastMergeable >= 0) {
                    val tc = cells[lastMergeable]; val tp = tc.r * size + tc.c
                    if (newV[tp] == v) {
                        newV[tp] = v * 2; score += v * 2
                        moves += TileMove(ids[p], cell, tc, v * 2, true)
                        moved = true
                        lastMergeable = -1
                        continue
                    }
                }
                val tc = cells[write]; val tp = tc.r * size + tc.c
                newV[tp] = v; newId[tp] = ids[p]
                if (tc != cell) moved = true
                moves += TileMove(ids[p], cell, tc, v, false)
                lastMergeable = write
                write++
            }
        }
        if (!moved) return emptyList()
        newV.copyInto(values); newId.copyInto(ids)
        spawn()
        return moves
    }

    fun best() = values.max()
    fun isWon() = best() >= target
    fun canMove(): Boolean {
        if (values.any { it == 0 }) return true
        for (r in 0 until size) for (c in 0 until size) {
            val v = values[r * size + c]
            if (c + 1 < size && values[r * size + c + 1] == v) return true
            if (r + 1 < size && values[(r + 1) * size + c] == v) return true
        }
        return false
    }

    companion object {
        fun targetFor(d: Difficulty) = when (d) {
            Difficulty.Easy -> 256; Difficulty.Medium -> 512; Difficulty.Hard -> 1024; Difficulty.Expert -> 2048
        }
    }
}
