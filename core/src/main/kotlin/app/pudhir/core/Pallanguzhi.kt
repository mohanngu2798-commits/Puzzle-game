package app.pudhir.core

/**
 * Pallanguzhi, the Tamil seed-sowing game, for two players (0 = you, 1 = the phone).
 * Pits 0..6 are player 0's row (left to right), pits 7..13 are player 1's row (right to left),
 * so sowing goes 0,1,...,13,0 anticlockwise around the board.
 *
 * A move: take every seed from one of your non-empty pits and sow them one by one into the following pits.
 * - If the pit after the last seed has seeds, pick those up and keep sowing.
 * - If it is empty, you capture the seeds in the pit after it (the turn ends).
 * - Two empty pits in a row: the turn ends with nothing captured.
 * - Whenever sowing makes a pit hold exactly four seeds, the owner of that pit takes them at once (a "pasu").
 * The game ends when the player to move has no seeds in their row; the rest go to the other side.
 * Most seeds wins.
 */
class Pallanguzhi(val seedsPerPit: Int = 5) {
    val pits = IntArray(14) { seedsPerPit }
    val store = IntArray(2)
    var turn = 0
        private set
    /** Pits touched by the last move in order, for animation; the capture pit comes last when there was one. */
    var lastPath: List<Int> = emptyList()
        private set
    var lastCapture = -1
        private set
    /** Board after every seed dropped or taken during the last move, for step-by-step animation. */
    var lastTrace: List<IntArray> = emptyList()
        private set
    var recordTrace = false

    fun owner(pit: Int) = if (pit < 7) 0 else 1
    fun legal(player: Int = turn): List<Int> = (0 until 14).filter { owner(it) == player && pits[it] > 0 }
    fun isOver() = legal(turn).isEmpty()
    fun total(player: Int) = store[player]

    fun winner(): Int = when {
        store[0] > store[1] -> 0
        store[1] > store[0] -> 1
        else -> -1
    }

    fun copy(): Pallanguzhi = Pallanguzhi(seedsPerPit).also {
        pits.copyInto(it.pits); store.copyInto(it.store); it.turn = turn
    }

    fun play(pit: Int) {
        require(owner(pit) == turn && pits[pit] > 0)
        val path = ArrayList<Int>()
        val trace = ArrayList<IntArray>()
        fun snap() { if (recordTrace) trace += pits.copyOf() }
        lastCapture = -1
        var hand = pits[pit]; pits[pit] = 0
        snap()
        var at = pit
        var guard = 0
        while (true) {
            while (hand > 0) {
                at = (at + 1) % 14
                pits[at]++; hand--; path += at
                snap()
                if (pits[at] == 4) { store[owner(at)] += 4; pits[at] = 0; snap() }
            }
            val next = (at + 1) % 14
            if (pits[next] > 0 && guard++ < 200) {
                hand = pits[next]; pits[next] = 0; at = next
                snap()
                continue
            }
            val after = (next + 1) % 14
            if (pits[next] == 0 && pits[after] > 0) {
                store[turn] += pits[after]; pits[after] = 0; lastCapture = after
                snap()
            }
            break
        }
        lastPath = path
        lastTrace = trace
        turn = 1 - turn
        if (isOver()) {
            // Whoever still has seeds keeps them.
            for (i in 0 until 14) { store[owner(i)] += pits[i]; pits[i] = 0 }
        }
    }

    /** Best pit for the player to move, looking [depth] moves ahead. */
    fun bestMove(depth: Int): Int? {
        val me = turn
        var best: Int? = null; var bestScore = Int.MIN_VALUE
        for (m in legal()) {
            val c = copy(); c.play(m)
            val s = search(c, depth - 1, me)
            if (s > bestScore) { bestScore = s; best = m }
        }
        return best
    }

    private fun search(g: Pallanguzhi, depth: Int, me: Int): Int {
        if (depth <= 0 || g.isOver()) {
            val onBoard = (0 until 14).sumOf { if (g.owner(it) == me) g.pits[it] else -g.pits[it] }
            return (g.store[me] - g.store[1 - me]) * 4 + onBoard
        }
        val scores = g.legal().map { m -> val c = g.copy(); c.play(m); search(c, depth - 1, me) }
        return if (g.turn == me) scores.max() else scores.min()
    }
}
