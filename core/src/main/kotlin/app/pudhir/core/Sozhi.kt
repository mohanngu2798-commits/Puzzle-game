package app.pudhir.core

import kotlin.random.Random

/**
 * Sozhi: a push-your-luck race with six cowrie shells, you (0) against the phone (1).
 * Toss the shells: each shell landing mouth-up scores a point. Keep tossing to build your turn's score,
 * or bank it. A toss with no shell mouth-up loses the turn's points. Six mouth-up is a bonus of 12.
 * First to [target] wins.
 */
class Sozhi(seed: Long, val target: Int = 50) {
    private val rng = Random(seed)
    val banked = IntArray(2)
    var turnScore = 0
        private set
    var turn = 0
        private set
    var lastToss = BooleanArray(6)
        private set

    /** Tosses; returns the points from this toss, or 0 when the turn is lost. */
    fun toss(): Int {
        lastToss = BooleanArray(6) { rng.nextFloat() < 0.45f }
        val up = lastToss.count { it }
        if (up == 0) { turnScore = 0; turn = 1 - turn; return 0 }
        val pts = if (up == 6) 12 else up
        turnScore += pts
        return pts
    }

    fun bank() { banked[turn] += turnScore; turnScore = 0; if (winner() < 0) turn = 1 - turn }

    fun winner() = (0..1).firstOrNull { banked[it] >= target } ?: -1

    /** Whether the phone should toss again. */
    fun phoneKeepsGoing(strength: Int): Boolean {
        val need = target - banked[1]
        if (turnScore >= need) return false
        val behind = banked[0] - banked[1]
        val stopAt = when (strength) { 1 -> 8; 2 -> 12; 3 -> if (behind > 15) 18 else 13; else -> if (behind > 15) 20 else if (banked[0] >= target - 8) 99 else 14 }
        return turnScore < stopAt
    }
}
