package app.pudhir.core

import kotlin.random.Random

/**
 * Handloom: number clues beside each row and column give the runs of woven squares,
 * in order. Weave the hidden pattern. Every puzzle can be solved line by line, without guessing.
 */
class HandloomPuzzle(val rows: Int, val cols: Int, val solution: BooleanArray) {
    val rowClues: List<List<Int>> = (0 until rows).map { r -> runs((0 until cols).map { solution[r * cols + it] }) }
    val colClues: List<List<Int>> = (0 until cols).map { c -> runs((0 until rows).map { solution[it * cols + c] }) }

    companion object {
        fun sizeFor(d: Difficulty) = when (d) {
            Difficulty.Easy -> 5; Difficulty.Medium -> 8; Difficulty.Hard -> 10; Difficulty.Expert -> 12
        }

        fun runs(line: List<Boolean>): List<Int> {
            val out = ArrayList<Int>(); var k = 0
            for (b in line) { if (b) k++ else if (k > 0) { out += k; k = 0 } }
            if (k > 0) out += k
            return out
        }

        fun generate(seed: Long, difficulty: Difficulty): HandloomPuzzle {
            val n = sizeFor(difficulty)
            val rng = Random(seed)
            while (true) {
                val density = 0.5f + rng.nextFloat() * 0.15f
                // Mirror half the time, so the woven picture looks like a motif.
                val mirror = rng.nextBoolean()
                val g = BooleanArray(n * n)
                for (r in 0 until n) for (c in 0 until n) {
                    g[r * n + c] = if (mirror && c >= (n + 1) / 2) g[r * n + (n - 1 - c)] else rng.nextFloat() < density
                }
                val p = HandloomPuzzle(n, n, g)
                if (p.rowClues.any { it.isEmpty() } || p.colClues.any { it.isEmpty() }) continue
                if (lineSolvable(p)) return p
            }
        }

        /** Solves with line logic alone; true when every cell gets decided. */
        fun lineSolvable(p: HandloomPuzzle): Boolean {
            val state = IntArray(p.rows * p.cols) // 0 unknown, 1 filled, 2 empty
            var changed = true
            while (changed) {
                changed = false
                for (r in 0 until p.rows) {
                    val idx = (0 until p.cols).map { r * p.cols + it }
                    val res = solveLine(p.rowClues[r], idx.map { state[it] }.toIntArray()) ?: return false
                    for ((k, i) in idx.withIndex()) if (state[i] == 0 && res[k] != 0) { state[i] = res[k]; changed = true }
                }
                for (c in 0 until p.cols) {
                    val idx = (0 until p.rows).map { it * p.cols + c }
                    val res = solveLine(p.colClues[c], idx.map { state[it] }.toIntArray()) ?: return false
                    for ((k, i) in idx.withIndex()) if (state[i] == 0 && res[k] != 0) { state[i] = res[k]; changed = true }
                }
            }
            return state.none { it == 0 }
        }

        /** Cells forced in every arrangement of [clue] consistent with [known]; null if none fits. */
        fun solveLine(clue: List<Int>, known: IntArray): IntArray? {
            val n = known.size
            val canFill = BooleanArray(n); val canEmpty = BooleanArray(n)
            val cur = IntArray(n) { 2 }
            var any = false
            fun place(k: Int, start: Int) {
                if (k == clue.size) {
                    for (i in start until n) if (known[i] == 1) return
                    any = true
                    for (i in 0 until n) { val v = if (i < start) cur[i] else 2; if (v == 1) canFill[i] = true else canEmpty[i] = true }
                    return
                }
                val len = clue[k]
                val remaining = clue.drop(k + 1).sum() + (clue.size - k - 1)
                for (s in start..n - len - remaining) {
                    // Cells from start to s-1 are empty.
                    if ((start until s).any { known[it] == 1 }) break
                    if ((s until s + len).any { known[it] == 2 }) continue
                    if (s + len < n && known[s + len] == 1) continue
                    for (i in start until s) cur[i] = 2
                    for (i in s until s + len) cur[i] = 1
                    if (s + len < n) cur[s + len] = 2
                    place(k + 1, minOf(n, s + len + 1))
                }
            }
            place(0, 0)
            if (!any) return null
            return IntArray(n) { if (canFill[it] && !canEmpty[it]) 1 else if (canEmpty[it] && !canFill[it]) 2 else 0 }
        }
    }
}
