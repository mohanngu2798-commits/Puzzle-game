package app.pudhir.core

import kotlin.random.Random

/**
 * Thaayam (Dayakattai), the Tamil dice race on a 5x5 board, you (0) against the phone (1), three pieces each.
 * Two long four-sided dice show 0 to 3; the throw is their sum, except 0 + 0 counts as 12.
 * A throw of 1 is "dhayam": it is the only throw that brings a piece onto the board.
 * Pieces run the outer ring, then the inner ring, and must reach the centre with an exact throw.
 * Landing on a lone enemy piece "cuts" it back home, except on the crossed safe squares.
 * Throws of 1, 5, 6 and 12 earn another throw. First to bring all pieces home to the centre wins.
 */
class Thaayam(seed: Long, val pieces: Int = 3) {
    private val rng = Random(seed)
    /** Progress of each piece along its path: -1 = waiting at home, 0..24 on the path, 24 = centre. */
    val pos = Array(2) { IntArray(pieces) { -1 } }
    var turn = 0
        private set
    var throwValue = 0
        private set
    var sticks = 0 to 0
        private set
    /** A throw is waiting to be used. */
    var pending = false
        private set

    fun cell(player: Int, step: Int): Pair<Int, Int> {
        val (r, c) = PATH[step]
        return if (player == 0) r to c else (4 - r) to (4 - c)
    }

    fun isSafe(rc: Pair<Int, Int>) = rc in SAFE

    fun throwSticks(): Int {
        check(!pending)
        val a = rng.nextInt(4); val b = rng.nextInt(4)
        sticks = a to b
        throwValue = if (a == 0 && b == 0) 12 else a + b
        pending = true
        if (moves().isEmpty()) endThrow()
        return throwValue
    }

    /** Pieces that can use the current throw. */
    fun moves(): List<Int> {
        if (!pending) return emptyList()
        return (0 until pieces).filter { canMove(turn, it, throwValue) }
    }

    private fun canMove(player: Int, piece: Int, n: Int): Boolean {
        val at = pos[player][piece]
        if (at == 24) return false
        if (at < 0) return n == 1
        return at + n <= 24
    }

    /** Where a move would land, as a path index. */
    fun target(piece: Int): Int = pos[turn][piece].let { if (it < 0) 0 else it + throwValue }

    /** Moves [piece]; returns the enemy piece index cut, or -1. */
    fun move(piece: Int): Int {
        check(piece in moves())
        val to = target(piece)
        pos[turn][piece] = to
        var cut = -1
        val rc = cell(turn, to)
        if (to < 24 && !isSafe(rc)) {
            val foe = 1 - turn
            val hit = (0 until pieces).filter { pos[foe][it] in 0..23 && cell(foe, pos[foe][it]) == rc }
            if (hit.size == 1) { pos[foe][hit[0]] = -1; cut = hit[0] }
        }
        endThrow()
        return cut
    }

    private fun endThrow() {
        pending = false
        if (throwValue !in setOf(1, 5, 6, 12)) turn = 1 - turn
    }

    fun winner(): Int = (0..1).firstOrNull { p -> pos[p].all { it == 24 } } ?: -1

    /** The phone's choice for the current throw: cut, finish, enter, then the piece furthest from danger. */
    fun choose(strength: Int, r: Random): Int? {
        val ms = moves(); if (ms.isEmpty()) return null
        if (strength <= 0 && r.nextInt(3) == 0) return ms.random(r)
        return ms.maxByOrNull { score(it) }
    }

    private fun score(piece: Int): Int {
        val to = target(piece); val rc = cell(turn, to)
        val foe = 1 - turn
        var s = to
        if (to == 24) s += 60
        if (pos[turn][piece] < 0) s += 25
        if (to < 24 && !isSafe(rc) && (0 until pieces).count { pos[foe][it] in 0..23 && cell(foe, pos[foe][it]) == rc } == 1) s += 80
        if (isSafe(rc)) s += 10
        // Danger: an enemy piece 1..12 steps behind on the shared ring.
        val danger = (0 until pieces).any { k -> val e = pos[foe][k]; e in 0..15 && (1..12).any { d -> e + d <= 23 && cell(foe, e + d) == rc } }
        if (danger && !isSafe(rc)) s -= 30
        return s
    }

    companion object {
        val PATH: List<Pair<Int, Int>> = listOf(
            4 to 2, 4 to 1, 4 to 0, 3 to 0, 2 to 0, 1 to 0, 0 to 0, 0 to 1, 0 to 2, 0 to 3, 0 to 4,
            1 to 4, 2 to 4, 3 to 4, 4 to 4, 4 to 3,
            3 to 3, 3 to 2, 3 to 1, 2 to 1, 1 to 1, 1 to 2, 1 to 3, 2 to 3,
            2 to 2,
        )
        val SAFE = setOf(4 to 2, 0 to 2, 2 to 0, 2 to 4, 2 to 2)
    }
}
