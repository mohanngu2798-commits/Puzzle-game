package app.pudhir.core

/**
 * Chathurangam, the ancient Indian ancestor of chess, on an 8x8 board. You are side 0 (bottom), the phone side 1.
 * Pieces: Raja (king, one step any way), Mantri (minister, one step diagonally), Gaja (elephant, jumps exactly
 * two squares diagonally), Ashva (horse, moves like a knight), Ratha (chariot, any distance straight),
 * Padati (foot soldier, one step forward, captures one step diagonally forward, becomes a Mantri on the last row).
 * Checkmate the enemy Raja, or capture every other enemy piece (a bare Raja), to win.
 */
class Chathurangam {
    enum class Kind(val value: Int, val label: String) {
        Raja(0, "Raja"), Mantri(250, "Mantri"), Gaja(220, "Gaja"), Ashva(320, "Ashva"), Ratha(500, "Ratha"), Padati(100, "Padati")
    }
    data class Piece(val kind: Kind, val side: Int)
    data class Move(val from: Int, val to: Int)

    val board = arrayOfNulls<Piece>(64)
    var turn = 0
        private set
    var lastMove: Move? = null
        private set
    private var plies = 0

    init {
        val back = listOf(Kind.Ratha, Kind.Ashva, Kind.Gaja, Kind.Mantri, Kind.Raja, Kind.Gaja, Kind.Ashva, Kind.Ratha)
        for (c in 0 until 8) {
            board[7 * 8 + c] = Piece(back[c], 0); board[6 * 8 + c] = Piece(Kind.Padati, 0)
            board[c] = Piece(back[c], 1); board[8 + c] = Piece(Kind.Padati, 1)
        }
    }

    private fun on(r: Int, c: Int) = r in 0..7 && c in 0..7

    /** Moves ignoring whether they leave the own Raja attacked. */
    private fun pseudo(side: Int): List<Move> {
        val out = ArrayList<Move>()
        for (i in 0 until 64) {
            val p = board[i] ?: continue
            if (p.side != side) continue
            val r = i / 8; val c = i % 8
            fun add(rr: Int, cc: Int, mustCapture: Boolean = false, noCapture: Boolean = false): Boolean {
                if (!on(rr, cc)) return false
                val t = board[rr * 8 + cc]
                if (t != null && t.side == side) return false
                if (mustCapture && t == null) return false
                if (noCapture && t != null) return false
                out += Move(i, rr * 8 + cc)
                return t == null
            }
            when (p.kind) {
                Kind.Raja -> for (dr in -1..1) for (dc in -1..1) if (dr != 0 || dc != 0) add(r + dr, c + dc)
                Kind.Mantri -> for (dr in listOf(-1, 1)) for (dc in listOf(-1, 1)) add(r + dr, c + dc)
                Kind.Gaja -> for (dr in listOf(-2, 2)) for (dc in listOf(-2, 2)) add(r + dr, c + dc)
                Kind.Ashva -> for ((dr, dc) in listOf(1 to 2, 2 to 1, -1 to 2, -2 to 1, 1 to -2, 2 to -1, -1 to -2, -2 to -1)) add(r + dr, c + dc)
                Kind.Ratha -> for ((dr, dc) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
                    var k = 1
                    while (add(r + dr * k, c + dc * k)) k++
                }
                Kind.Padati -> {
                    val f = if (side == 0) -1 else 1
                    add(r + f, c, noCapture = true)
                    add(r + f, c - 1, mustCapture = true); add(r + f, c + 1, mustCapture = true)
                }
            }
        }
        return out
    }

    fun attacked(square: Int, by: Int) = pseudo(by).any { it.to == square }
    private fun rajaOf(side: Int) = (0 until 64).firstOrNull { board[it]?.let { p -> p.kind == Kind.Raja && p.side == side } == true }

    fun inCheck(side: Int): Boolean = rajaOf(side)?.let { attacked(it, 1 - side) } ?: true

    fun moves(side: Int = turn): List<Move> = pseudo(side).filter { m ->
        val undo = make(m)
        val ok = !inCheck(side)
        unmake(m, undo)
        ok
    }

    private data class Undo(val captured: Piece?, val moved: Piece, val turn: Int, val last: Move?)

    private fun make(m: Move): Undo {
        val moved = board[m.from]!!
        val u = Undo(board[m.to], moved, turn, lastMove)
        val lastRow = if (moved.side == 0) 0 else 7
        board[m.to] = if (moved.kind == Kind.Padati && m.to / 8 == lastRow) Piece(Kind.Mantri, moved.side) else moved
        board[m.from] = null
        turn = 1 - moved.side
        lastMove = m
        return u
    }

    private fun unmake(m: Move, u: Undo) {
        board[m.from] = u.moved; board[m.to] = u.captured; turn = u.turn; lastMove = u.last
    }

    fun play(m: Move): Piece? {
        val captured = board[m.to]
        make(m); plies++
        return captured
    }

    private fun bare(side: Int) = (0 until 64).none { board[it]?.let { p -> p.side == side && p.kind != Kind.Raja } == true }

    /** 0 or 1 when a side has won, 2 for a draw, -1 while the game goes on. */
    fun result(): Int {
        if (bare(1) && !bare(0)) return 0
        if (bare(0) && !bare(1)) return 1
        if (moves().isEmpty()) return if (inCheck(turn)) 1 - turn else 2
        if (plies > 300 || (bare(0) && bare(1))) return 2
        return -1
    }

    fun bestMove(depth: Int): Move? {
        val side = turn
        var best: Move? = null; var bestScore = Int.MIN_VALUE + 1
        val ms = ordered(moves())
        for (m in ms) {
            val u = make(m)
            val s = -negamax(depth - 1, Int.MIN_VALUE + 1, -bestScore)
            unmake(m, u)
            if (s > bestScore || best == null) { bestScore = s; best = m }
        }
        return best
    }

    private fun ordered(ms: List<Move>) = ms.sortedByDescending { (board[it.to]?.kind?.value ?: 0) * 10 - (board[it.from]?.kind?.value ?: 0) }

    private fun negamax(depth: Int, alpha0: Int, beta: Int): Int {
        val ms = moves()
        if (ms.isEmpty()) return if (inCheck(turn)) -100_000 - depth else 0
        if (bare(turn)) return -90_000
        if (depth <= 0) return eval()
        var alpha = alpha0
        for (m in ordered(ms)) {
            val u = make(m)
            val s = -negamax(depth - 1, -beta, -alpha)
            unmake(m, u)
            if (s >= beta) return s
            if (s > alpha) alpha = s
        }
        return alpha
    }

    /** Material and a little central presence, from the side to move. */
    private fun eval(): Int {
        var s = 0
        for (i in 0 until 64) {
            val p = board[i] ?: continue
            val r = i / 8; val c = i % 8
            val centre = 6 - (kotlin.math.abs(3.5 - r) + kotlin.math.abs(3.5 - c)).toInt()
            var v = p.kind.value + centre * 3
            if (p.kind == Kind.Padati) v += (if (p.side == 0) 6 - r else r - 1) * 8
            s += if (p.side == turn) v else -v
        }
        return s
    }
}
