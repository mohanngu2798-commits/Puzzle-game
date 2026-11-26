package app.pudhir.core

import kotlin.random.Random

/** Four difficulty tiers shared by every game. */
enum class Difficulty(val label: String) { Easy("Easy"), Medium("Medium"), Hard("Hard"), Expert("Expert") }

/** A grid position. */
data class Cell(val r: Int, val c: Int) {
    fun neighbours4() = listOf(Cell(r - 1, c), Cell(r + 1, c), Cell(r, c - 1), Cell(r, c + 1))
}

/** Deterministic seed for "level N of game G" or "the daily puzzle of game G on day D". */
object Seeds {
    fun level(gameId: String, level: Int): Long = mix(gameId.hashCode().toLong() * 1_000_003L + level)
    fun daily(gameId: String, epochDay: Long): Long = mix(gameId.hashCode().toLong() * 7_919L + epochDay * 104_729L + 17)
    private fun mix(x0: Long): Long {
        var x = x0 + -0x61c8864680b583ebL
        x = (x xor (x ushr 30)) * -0x40a7b892e31b1a47L
        x = (x xor (x ushr 27)) * -0x6b2fb644ecceee15L
        return x xor (x ushr 31)
    }
}

/** Difficulty for a level number: levels ramp up through the tiers. */
fun difficultyForLevel(level: Int): Difficulty = when {
    level <= 10 -> Difficulty.Easy
    level <= 30 -> Difficulty.Medium
    level <= 70 -> Difficulty.Hard
    else -> Difficulty.Expert
}

fun <T> MutableList<T>.shuffled(rng: Random): MutableList<T> { shuffle(rng); return this }
