package app.pudhir.core

/**
 * Pathinaaru Kaai (sixteen soldiers). A 5x5 grid with diagonals and a triangle on the top and bottom: 37 points.
 * Each side has 16 soldiers; the middle row starts empty.
 * A soldier steps to a free neighbouring point along a line, or jumps over an enemy soldier to the free
 * point straight beyond it, capturing it, and may keep jumping. Take every enemy soldier, or leave them no move, to win.
 * You are 0 (bottom), the phone is 1 (top).
 */
class PathinaaruKaai {
    val board = IntArray(N) { -1 }
    var turn = 0
        private set
    /** While a soldier is mid-way through a chain of jumps, only it may move (and only by jumping). */
    var chaining = -1
        private set

    sealed interface Move { val from: Int; val to: Int }
    data class Step(override val from: Int, override val to: Int) : Move
    data class Jump(override val from: Int, val over: Int, override val to: Int) : Move

    init {
        for (i in 0 until N) {
            val (x, y) = XY[i]
            if (y < 2f) board[i] = 1
            if (y > 2f) board[i] = 0
        }
    }

    fun count(p: Int) = board.count { it == p }

    fun moves(): List<Move> {
        val me = turn
        val out = ArrayList<Move>()
        for (i in 0 until N) {
            if (board[i] != me || (chaining >= 0 && i != chaining)) continue
            if (chaining < 0) for (n in ADJ[i]) if (board[n] < 0) out += Step(i, n)
            for ((f, o, t) in JUMPS) if (f == i && board[o] == 1 - me && board[t] < 0) out += Jump(f, o, t)
        }
        return out
    }

    /** Ends a chain of jumps early. */
    fun stop() { if (chaining >= 0) { chaining = -1; turn = 1 - turn } }

    fun apply(m: Move) {
        board[m.to] = board[m.from]; board[m.from] = -1
        if (m is Jump) {
            board[m.over] = -1
            val more = JUMPS.any { (f, o, t) -> f == m.to && board[o] == 1 - turn && board[t] < 0 }
            if (more) { chaining = m.to; return }
        }
        chaining = -1
        turn = 1 - turn
    }

    fun winner(): Int {
        if (count(0) == 0) return 1
        if (count(1) == 0) return 0
        if (chaining < 0 && moves().isEmpty()) return 1 - turn
        return -1
    }

    fun copy() = PathinaaruKaai().also { board.copyInto(it.board); it.turn = turn; it.chaining = chaining }

    fun bestMove(depth: Int): Move? {
        val me = turn
        val ms = moves().sortedByDescending { if (it is Jump) 1 else 0 }
        var best: Move? = null; var bestScore = Int.MIN_VALUE
        for (m in ms) {
            val c = copy(); c.apply(m); c.finishChainGreedy()
            val s = search(c, depth - 1, me, Int.MIN_VALUE, Int.MAX_VALUE)
            if (s > bestScore) { bestScore = s; best = m }
        }
        return best
    }

    /** In search, a chain keeps jumping with the first available capture. */
    private fun finishChainGreedy() {
        while (chaining >= 0) { val j = moves().firstOrNull() ?: run { stop(); null } ?: break; apply(j) }
    }

    private fun search(g: PathinaaruKaai, depth: Int, me: Int, a0: Int, b0: Int): Int {
        val w = g.winner()
        if (w == me) return 100_000 + depth
        if (w == 1 - me) return -100_000 - depth
        if (depth <= 0) return (g.count(me) - g.count(1 - me)) * 100 + g.safety(me) - g.safety(1 - me)
        var a = a0; var b = b0
        val max = g.turn == me
        var best = if (max) Int.MIN_VALUE else Int.MAX_VALUE
        for (m in g.moves().sortedByDescending { if (it is Jump) 1 else 0 }) {
            val c = g.copy(); c.apply(m); c.finishChainGreedy()
            val s = search(c, depth - 1, me, a, b)
            if (max) { best = maxOf(best, s); a = maxOf(a, s) } else { best = minOf(best, s); b = minOf(b, s) }
            if (b <= a) break
        }
        return best
    }

    /** Soldiers that cannot be jumped right now. */
    private fun safety(p: Int) = (0 until N).count { i ->
        board[i] == p && JUMPS.none { (f, o, t) -> o == i && board[f] == 1 - p && board[t] < 0 }
    }

    companion object {
        /** Coordinates: grid x 0..4, y 0..4 (top to bottom); triangles at y -2..-1 and 5..6. */
        val XY: List<Pair<Float, Float>>
        val ADJ: List<List<Int>>
        val JUMPS: List<Triple<Int, Int, Int>>
        val N: Int

        init {
            val pts = ArrayList<Pair<Float, Float>>()
            fun id(x: Float, y: Float): Int { val k = pts.indexOf(x to y); return if (k >= 0) k else { pts += x to y; pts.size - 1 } }
            val lines = ArrayList<List<Int>>()
            // Grid rows, columns and Alquerque diagonals.
            for (y in 0..4) lines += (0..4).map { id(it.toFloat(), y.toFloat()) }
            for (x in 0..4) lines += (0..4).map { id(x.toFloat(), it.toFloat()) }
            lines += (0..4).map { id(it.toFloat(), it.toFloat()) }
            lines += (0..4).map { id(it.toFloat(), (4 - it).toFloat()) }
            lines += listOf(id(0f, 2f), id(1f, 1f), id(2f, 0f)); lines += listOf(id(2f, 0f), id(3f, 1f), id(4f, 2f))
            lines += listOf(id(0f, 2f), id(1f, 3f), id(2f, 4f)); lines += listOf(id(2f, 4f), id(3f, 3f), id(4f, 2f))
            // Triangles.
            for ((apexY, dir) in listOf(0f to -1f, 4f to 1f)) {
                val apex = id(2f, apexY)
                lines += listOf(apex, id(1f, apexY + dir), id(0f, apexY + 2 * dir))
                lines += listOf(apex, id(2f, apexY + dir), id(2f, apexY + 2 * dir))
                lines += listOf(apex, id(3f, apexY + dir), id(4f, apexY + 2 * dir))
                lines += listOf(id(1f, apexY + dir), id(2f, apexY + dir), id(3f, apexY + dir))
                lines += listOf(id(0f, apexY + 2 * dir), id(2f, apexY + 2 * dir), id(4f, apexY + 2 * dir))
            }
            N = pts.size; XY = pts
            val adj = List(N) { LinkedHashSet<Int>() }
            val jumps = ArrayList<Triple<Int, Int, Int>>()
            for (l in lines) {
                for (k in 0 until l.size - 1) { adj[l[k]] += l[k + 1]; adj[l[k + 1]] += l[k] }
                for (k in 0 until l.size - 2) { jumps += Triple(l[k], l[k + 1], l[k + 2]); jumps += Triple(l[k + 2], l[k + 1], l[k]) }
            }
            ADJ = adj.map { it.toList() }; JUMPS = jumps.distinct()
        }
    }
}
