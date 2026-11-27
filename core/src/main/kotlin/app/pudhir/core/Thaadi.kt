package app.pudhir.core

/**
 * Thaadi, the mill game carved on old temple floors: three nested squares joined at their midpoints, 24 points.
 * Each side has nine pieces. Take turns placing them; then take turns sliding one to a free neighbour.
 * Three of yours in a straight line (a mill) lets you remove one enemy piece that is not in a mill
 * (unless all of them are). With only three pieces left you may jump anywhere.
 * A player wins when the other is down to two pieces or cannot move.
 */
class Thaadi {
    /** -1 empty, 0 you, 1 the phone. */
    val board = IntArray(24) { -1 }
    val inHand = intArrayOf(9, 9)
    var turn = 0
        private set
    /** The player to move must now remove an enemy piece. */
    var removing = false
        private set

    sealed interface Move
    data class Place(val at: Int) : Move
    data class Slide(val from: Int, val to: Int) : Move
    data class Remove(val at: Int) : Move

    fun count(p: Int) = board.count { it == p } + inHand[p]
    fun onBoard(p: Int) = board.count { it == p }

    fun inMill(i: Int): Boolean {
        val p = board[i]; if (p < 0) return false
        return MILLS.any { m -> i in m && m.all { board[it] == p } }
    }

    fun moves(): List<Move> {
        val me = turn
        if (removing) {
            val foe = (0 until 24).filter { board[it] == 1 - me }
            val free = foe.filter { !inMill(it) }
            return (free.ifEmpty { foe }).map { Remove(it) }
        }
        if (inHand[me] > 0) return (0 until 24).filter { board[it] < 0 }.map { Place(it) }
        val flying = onBoard(me) == 3
        val out = ArrayList<Move>()
        for (i in 0 until 24) if (board[i] == me) {
            val dests = if (flying) (0 until 24).filter { board[it] < 0 } else ADJ[i].filter { board[it] < 0 }
            for (d in dests) out += Slide(i, d)
        }
        return out
    }

    fun winner(): Int {
        for (p in 0..1) if (inHand[p] == 0 && count(p) < 3) return 1 - p
        if (!removing && moves().isEmpty()) return 1 - turn
        return -1
    }

    fun apply(m: Move) {
        val me = turn
        when (m) {
            is Remove -> { board[m.at] = -1; removing = false; turn = 1 - me; return }
            is Place -> { board[m.at] = me; inHand[me]-- ; if (inMill(m.at)) { removing = true; return } }
            is Slide -> { board[m.to] = me; board[m.from] = -1; if (inMill(m.to)) { removing = true; return } }
        }
        turn = 1 - me
    }

    fun copy() = Thaadi().also { board.copyInto(it.board); inHand.copyInto(it.inHand); it.turn = turn; it.removing = removing }

    fun bestMove(depth: Int): Move? {
        val me = turn
        var best: Move? = null; var bestScore = Int.MIN_VALUE
        for (m in moves()) {
            val c = copy(); c.apply(m)
            val s = search(c, depth - 1, me, Int.MIN_VALUE, Int.MAX_VALUE)
            if (s > bestScore) { bestScore = s; best = m }
        }
        return best
    }

    private fun search(g: Thaadi, depth: Int, me: Int, alpha0: Int, beta0: Int): Int {
        val w = g.winner()
        if (w == me) return 100_000 + depth
        if (w == 1 - me) return -100_000 - depth
        if (depth <= 0) return eval(g, me)
        var alpha = alpha0; var beta = beta0
        val maximise = g.turn == me
        var best = if (maximise) Int.MIN_VALUE else Int.MAX_VALUE
        for (m in g.moves()) {
            val c = g.copy(); c.apply(m)
            val s = search(c, depth - 1, me, alpha, beta)
            if (maximise) { best = maxOf(best, s); alpha = maxOf(alpha, s) } else { best = minOf(best, s); beta = minOf(beta, s) }
            if (beta <= alpha) break
        }
        return best
    }

    private fun eval(g: Thaadi, me: Int): Int {
        fun mobility(p: Int) = (0 until 24).count { i -> g.board[i] == p && ADJ[i].any { g.board[it] < 0 } }
        fun twos(p: Int) = MILLS.count { m -> m.count { g.board[it] == p } == 2 && m.any { g.board[it] < 0 } }
        return (g.count(me) - g.count(1 - me)) * 100 + (mobility(me) - mobility(1 - me)) * 4 + (twos(me) - twos(1 - me)) * 15
    }

    companion object {
        /** Points: ring 0 (outer) 0..7, ring 1 8..15, ring 2 (inner) 16..23; each ring clockwise from top-left. */
        fun xy(i: Int): Pair<Float, Float> {
            val ring = i / 8; val k = i % 8
            val inset = ring / 6f + 0.05f
            val a = inset; val b = 0.5f; val c = 1f - inset
            return when (k) { 0 -> a to a; 1 -> b to a; 2 -> c to a; 3 -> c to b; 4 -> c to c; 5 -> b to c; 6 -> a to c; else -> a to b }
        }
        val ADJ: List<List<Int>> = List(24) { i ->
            val ring = i / 8; val k = i % 8
            val out = mutableListOf(ring * 8 + (k + 1) % 8, ring * 8 + (k + 7) % 8)
            if (k % 2 == 1) { if (ring > 0) out += i - 8; if (ring < 2) out += i + 8 }
            out
        }
        val MILLS: List<List<Int>> = buildList {
            for (r in 0..2) for (s in 0..3) add(listOf(r * 8 + 2 * s, r * 8 + 2 * s + 1, r * 8 + (2 * s + 2) % 8))
            for (k in listOf(1, 3, 5, 7)) add(listOf(k, k + 8, k + 16))
        }
    }
}
