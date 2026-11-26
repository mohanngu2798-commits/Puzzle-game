package app.pudhir.core

import kotlin.random.Random

/**
 * Mithai Match: swap two neighbouring sweets to line up three or more of a kind.
 * Reach the target score before you run out of moves. Four in a row clears the whole line.
 */
class MithaiMatch(seed: Long, val difficulty: Difficulty, val size: Int = 8) {
    private val rng = Random(seed)
    val kinds = when (difficulty) { Difficulty.Easy -> 5; else -> 6 }
    val board = IntArray(size * size)
    val ids = IntArray(size * size)
    private var nextId = 1
    var score = 0; private set
    var movesLeft = when (difficulty) { Difficulty.Easy -> 22; Difficulty.Medium -> 20; Difficulty.Hard -> 18; Difficulty.Expert -> 16 }; private set
    val target = when (difficulty) { Difficulty.Easy -> 1200; Difficulty.Medium -> 2200; Difficulty.Hard -> 3200; Difficulty.Expert -> 4200 }
    private var chain = 0

    init {
        do {
            for (p in board.indices) { board[p] = rng.nextInt(kinds); ids[p] = nextId++ }
        } while (findMatches().isNotEmpty() || hint() == null)
    }

    fun adjacent(a: Int, b: Int) = (a / size == b / size && kotlin.math.abs(a - b) == 1) || kotlin.math.abs(a - b) == size

    private fun swapRaw(a: Int, b: Int) {
        val t = board[a]; board[a] = board[b]; board[b] = t
        val i = ids[a]; ids[a] = ids[b]; ids[b] = i
    }

    /** Swaps if it makes a match; returns false (and leaves the board) otherwise. */
    fun swap(a: Int, b: Int): Boolean {
        if (!adjacent(a, b) || movesLeft <= 0) return false
        swapRaw(a, b)
        if (findMatches().isEmpty()) { swapRaw(a, b); return false }
        movesLeft--
        chain = 0
        return true
    }

    fun findMatches(): Set<Int> {
        val out = HashSet<Int>()
        for (r in 0 until size) {
            var c = 0
            while (c < size) {
                var e = c
                while (e + 1 < size && board[r * size + e + 1] == board[r * size + c]) e++
                val len = e - c + 1
                if (len >= 3) {
                    if (len >= 4) for (k in 0 until size) out += r * size + k else for (k in c..e) out += r * size + k
                }
                c = e + 1
            }
        }
        for (c in 0 until size) {
            var r = 0
            while (r < size) {
                var e = r
                while (e + 1 < size && board[(e + 1) * size + c] == board[r * size + c]) e++
                val len = e - r + 1
                if (len >= 3) {
                    if (len >= 4) for (k in 0 until size) out += k * size + c else for (k in r..e) out += k * size + c
                }
                r = e + 1
            }
        }
        return out
    }

    /** One cascade step: clears matches, drops sweets, refills. Returns cleared cells (empty when settled). */
    fun step(): Set<Int> {
        val m = findMatches()
        if (m.isEmpty()) return m
        chain++
        score += m.size * 20 * chain
        for (p in m) board[p] = -1
        for (c in 0 until size) {
            var write = size - 1
            for (r in size - 1 downTo 0) {
                val p = r * size + c
                if (board[p] >= 0) {
                    val q = write * size + c
                    board[q] = board[p]; ids[q] = ids[p]
                    write--
                }
            }
            for (r in write downTo 0) { board[r * size + c] = rng.nextInt(kinds); ids[r * size + c] = nextId++ }
        }
        return m
    }

    fun hint(): Pair<Int, Int>? {
        for (p in board.indices) for (q in listOf(p + 1, p + size)) {
            if (q >= board.size || !adjacent(p, q)) continue
            swapRaw(p, q)
            val ok = findMatches().isNotEmpty()
            swapRaw(p, q)
            if (ok) return p to q
        }
        return null
    }

    fun shuffle() {
        do {
            val vals = board.toMutableList().shuffled(rng)
            for (p in board.indices) board[p] = vals[p]
        } while (findMatches().isNotEmpty() || hint() == null)
    }

    fun isWon() = score >= target
    fun isLost() = movesLeft <= 0 && !isWon()
}
