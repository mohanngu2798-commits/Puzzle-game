package app.pudhir.core

/**
 * Aadu Puli Attam (goats and tigers). Three tigers against fifteen goats on a fan-shaped board:
 * an apex point, four lines fanning down from it crossed by five rows, and the second and third rows
 * reaching out one extra point on each side.
 *
 * Goats first place all fifteen goats one per turn, then move. Each turn a tiger steps to a free
 * neighbouring point, or jumps over a goat in a straight line to the free point beyond, capturing it.
 * Tigers win by capturing [captureToWin] goats. Goats win when no tiger can move.
 */
class AaduPuli private constructor(
    val points: List<Pair<Float, Float>>,
    val edges: List<Pair<Int, Int>>,
    /** (from, over, to) straight-line jumps. */
    private val jumps: List<Triple<Int, Int, Int>>,
) {
    enum class Piece { Empty, Goat, Tiger }

    val board = Array(points.size) { Piece.Empty }
    var goatsToPlace = 15
        private set
    var captured = 0
        private set
    /** true while it is the goats' turn. */
    var goatsTurn = true
        private set
    val captureToWin = 6
    private val adj: List<List<Int>> = List(points.size) { i -> edges.mapNotNull { (a, b) -> if (a == i) b else if (b == i) a else null } }

    sealed interface Move
    data class Place(val at: Int) : Move
    data class Step(val from: Int, val to: Int) : Move
    data class Jump(val from: Int, val over: Int, val to: Int) : Move

    init {
        for (t in START_TIGERS) board[t] = Piece.Tiger
    }

    fun goatMoves(): List<Move> =
        if (goatsToPlace > 0) board.indices.filter { board[it] == Piece.Empty }.map { Place(it) }
        else board.indices.filter { board[it] == Piece.Goat }.flatMap { g -> adj[g].filter { board[it] == Piece.Empty }.map { Step(g, it) } }

    fun tigerMoves(): List<Move> {
        val out = ArrayList<Move>()
        for (t in board.indices) if (board[t] == Piece.Tiger) {
            for (n in adj[t]) if (board[n] == Piece.Empty) out += Step(t, n)
            for ((f, o, to) in jumps) if (f == t && board[o] == Piece.Goat && board[to] == Piece.Empty) out += Jump(t, o, to)
        }
        return out
    }

    fun movesFor(goats: Boolean) = if (goats) goatMoves() else tigerMoves()

    fun tigersWin() = captured >= captureToWin
    fun goatsWin() = !goatsTurn && tigerMoves().isEmpty()
    fun isOver() = tigersWin() || goatsWin() || (goatsTurn && goatMoves().isEmpty())

    fun apply(m: Move) {
        when (m) {
            is Place -> { board[m.at] = Piece.Goat; goatsToPlace-- }
            is Step -> { board[m.to] = board[m.from]; board[m.from] = Piece.Empty }
            is Jump -> { board[m.to] = Piece.Tiger; board[m.from] = Piece.Empty; board[m.over] = Piece.Empty; captured++ }
        }
        goatsTurn = !goatsTurn
    }

    fun copy(): AaduPuli = AaduPuli(points, edges, jumps).also {
        for (i in board.indices) it.board[i] = board[i]
        it.goatsToPlace = goatsToPlace; it.captured = captured; it.goatsTurn = goatsTurn
    }

    /** Best move for whoever is to move, searching [depth] plies. */
    fun bestMove(depth: Int): Move? {
        val goats = goatsTurn
        val moves = movesFor(goats).shuffled(kotlin.random.Random(captured * 31 + goatsToPlace))
        var best: Move? = null; var bestScore = Int.MIN_VALUE
        for (m in moves) {
            val c = copy(); c.apply(m)
            val s = -negamax(c, depth - 1)
            if (s > bestScore) { bestScore = s; best = m }
        }
        return best
    }

    /** Score from the point of view of the side to move. */
    private fun negamax(g: AaduPuli, depth: Int): Int {
        if (g.tigersWin()) return if (g.goatsTurn) -10_000 else 10_000
        val moves = g.movesFor(g.goatsTurn)
        if (moves.isEmpty()) return -10_000 // side to move is stuck and loses
        if (depth <= 0) {
            val tigerScore = g.captured * 100 + g.tigerMoves().size * 3 + g.tigerMoves().count { it is Jump } * 25
            return if (g.goatsTurn) -tigerScore else tigerScore
        }
        var best = Int.MIN_VALUE
        for (m in moves) {
            val c = g.copy(); c.apply(m)
            best = maxOf(best, -negamax(c, depth - 1))
        }
        return best
    }

    companion object {
        /** Row y positions and the x of each of the four fan lines on that row (board is 0..1 wide). */
        private val ROW_Y = listOf(0.24f, 0.40f, 0.56f, 0.72f, 0.88f)
        private val START_TIGERS = listOf(0, 2, 3)

        fun create(): AaduPuli {
            val pts = ArrayList<Pair<Float, Float>>()
            pts += 0.5f to 0.06f // apex = 0
            val grid = Array(5) { IntArray(6) { -1 } } // row -> column (0 and 5 are side extensions)
            for ((r, y) in ROW_Y.withIndex()) {
                val spread = 0.12f + r * 0.085f
                for (k in 0 until 4) {
                    val x = 0.5f + spread * (k - 1.5f) / 1.5f
                    grid[r][k + 1] = pts.size; pts += x to y
                }
                if (r == 1 || r == 2) {
                    grid[r][0] = pts.size; pts += 0.04f to y
                    grid[r][5] = pts.size; pts += 0.96f to y
                }
            }
            val edges = LinkedHashSet<Pair<Int, Int>>()
            val jumps = ArrayList<Triple<Int, Int, Int>>()
            fun line(seq: List<Int>) {
                for (i in 0 until seq.size - 1) edges += minOf(seq[i], seq[i + 1]) to maxOf(seq[i], seq[i + 1])
                for (i in 0 until seq.size - 2) {
                    jumps += Triple(seq[i], seq[i + 1], seq[i + 2]); jumps += Triple(seq[i + 2], seq[i + 1], seq[i])
                }
            }
            // Fan lines from the apex.
            for (k in 1..4) line(listOf(0) + (0 until 5).map { grid[it][k] })
            // Rows.
            for (r in 0 until 5) line(grid[r].filter { it >= 0 })
            return AaduPuli(pts, edges.toList(), jumps)
        }
    }
}
