package app.pudhir.core

import kotlin.random.Random

/** Deepam: light every lamp. Tapping a lamp flips it and its four neighbours. */
class Deepam(seed: Long, difficulty: Difficulty) {
    val size = when (difficulty) { Difficulty.Easy -> 4; Difficulty.Medium -> 5; Difficulty.Hard -> 5; Difficulty.Expert -> 6 }
    val lit = BooleanArray(size * size) { true }
    private val need = BooleanArray(size * size)
    var moves = 0
        private set

    init {
        val rng = Random(seed)
        val taps = when (difficulty) { Difficulty.Easy -> 4; Difficulty.Medium -> 6; Difficulty.Hard -> 9; Difficulty.Expert -> 13 }
        do {
            lit.fill(true); need.fill(false)
            for (i in (0 until size * size).shuffled(rng).take(taps)) { flip(i); need[i] = !need[i] }
        } while (isSolved())
        moves = 0
    }

    private fun flip(i: Int) {
        val r = i / size; val c = i % size
        lit[i] = !lit[i]
        if (r > 0) lit[i - size] = !lit[i - size]
        if (r < size - 1) lit[i + size] = !lit[i + size]
        if (c > 0) lit[i - 1] = !lit[i - 1]
        if (c < size - 1) lit[i + 1] = !lit[i + 1]
    }

    fun tap(i: Int) { flip(i); need[i] = !need[i]; moves++ }
    fun isSolved() = lit.all { it }
    fun hint(): Int? = need.indices.firstOrNull { need[it] }
}

/** Jodi: memory pairs. */
class Jodi(seed: Long, difficulty: Difficulty) {
    val cols: Int
    val rows: Int
    val cards: List<Int>
    val matched: BooleanArray
    var flips = 0
        private set

    init {
        val (c, r) = when (difficulty) { Difficulty.Easy -> 3 to 4; Difficulty.Medium -> 4 to 4; Difficulty.Hard -> 4 to 5; Difficulty.Expert -> 5 to 6 }
        cols = c; rows = r
        val pairs = c * r / 2
        cards = (0 until pairs).flatMap { listOf(it, it) }.shuffled(Random(seed))
        matched = BooleanArray(cards.size)
    }

    /** Checks two face-up cards; returns true on a match. */
    fun check(a: Int, b: Int): Boolean {
        flips++
        if (a != b && cards[a] == cards[b]) { matched[a] = true; matched[b] = true; return true }
        return false
    }

    fun isSolved() = matched.all { it }
}

/** Nagaram: the sliding tile puzzle. Tiles 1..n²-1 and one gap (0). */
class Nagaram(seed: Long, difficulty: Difficulty) {
    val size = when (difficulty) { Difficulty.Easy -> 3; Difficulty.Medium -> 4; Difficulty.Hard -> 4; Difficulty.Expert -> 5 }
    val tiles = IntArray(size * size) { (it + 1) % (size * size) }
    var moves = 0
        private set

    init {
        val rng = Random(seed)
        val steps = when (difficulty) { Difficulty.Easy -> 30; Difficulty.Medium -> 80; Difficulty.Hard -> 200; Difficulty.Expert -> 300 }
        var prev = -1
        repeat(steps) {
            val gap = tiles.indexOf(0)
            val opts = neighbours(gap).filter { it != prev }
            val pick = opts[rng.nextInt(opts.size)]
            tiles[gap] = tiles[pick]; tiles[pick] = 0; prev = gap
        }
        if (isSolved()) { val gap = tiles.indexOf(0); val n = neighbours(gap).first(); tiles[gap] = tiles[n]; tiles[n] = 0 }
        moves = 0
    }

    fun neighbours(i: Int): List<Int> {
        val n = size; val r = i / n; val c = i % n
        return buildList { if (r > 0) add(i - n); if (r < n - 1) add(i + n); if (c > 0) add(i - 1); if (c < n - 1) add(i + 1) }
    }

    /** Slides the tile at [i] (and any tiles between it and the gap in the same row or column). */
    fun slide(i: Int): Boolean {
        val gap = tiles.indexOf(0)
        val gr = gap / size; val gc = gap % size; val r = i / size; val c = i % size
        if (i == gap || (r != gr && c != gc)) return false
        val step = if (r == gr) (if (c > gc) 1 else -1) else (if (r > gr) size else -size)
        var g = gap
        while (g != i) { tiles[g] = tiles[g + step]; tiles[g + step] = 0; g += step }
        moves++
        return true
    }

    fun isSolved() = tiles.indices.all { tiles[it] == (it + 1) % (size * size) }
}

/**
 * Water Channels: rotate the channel pieces so water from the well reaches every field with no channel left open.
 * Each tile's openings are a bitmask: 1 up, 2 right, 4 down, 8 left.
 */
class WaterChannels(seed: Long, difficulty: Difficulty) {
    val size = when (difficulty) { Difficulty.Easy -> 4; Difficulty.Medium -> 5; Difficulty.Hard -> 6; Difficulty.Expert -> 7 }
    val solution = IntArray(size * size)
    val tiles = IntArray(size * size)
    val source: Int
    var moves = 0
        private set

    init {
        val rng = Random(seed)
        source = (size / 2) * size + size / 2
        val seen = BooleanArray(size * size)
        val stack = ArrayDeque(listOf(source)); seen[source] = true
        while (stack.isNotEmpty()) {
            val cur = stack.last()
            val opts = dirs(cur).filter { (n, _) -> !seen[n] }
            // Prefer fewer branches so the board has long winding channels.
            if (opts.isEmpty() || (stack.size > 1 && rng.nextInt(10) == 0 && opts.size < 2)) { stack.removeLast(); if (opts.isEmpty()) continue }
            if (opts.isEmpty()) continue
            val (n, bit) = opts[rng.nextInt(opts.size)]
            solution[cur] = solution[cur] or bit; solution[n] = solution[n] or opposite(bit)
            seen[n] = true; stack.addLast(n)
        }
        solution.copyInto(tiles)
        for (i in tiles.indices) repeat(rng.nextInt(4)) { tiles[i] = rot(tiles[i]) }
        if (isSolved()) tiles[0] = rot(tiles[0]).let { if (it == solution[0]) rot(it) else it }
        moves = 0
    }

    private fun dirs(i: Int): List<Pair<Int, Int>> {
        val n = size; val r = i / n; val c = i % n
        return buildList { if (r > 0) add(i - n to 1); if (c < n - 1) add(i + 1 to 2); if (r < n - 1) add(i + n to 4); if (c > 0) add(i - 1 to 8) }
    }

    private fun opposite(b: Int) = when (b) { 1 -> 4; 2 -> 8; 4 -> 1; else -> 2 }
    fun rot(m: Int) = ((m shl 1) or (m shr 3)) and 15

    fun rotate(i: Int) { tiles[i] = rot(tiles[i]); moves++ }

    /** Cells water reaches from the well through matching openings. */
    fun watered(): BooleanArray {
        val w = BooleanArray(size * size); w[source] = true
        val q = ArrayDeque(listOf(source))
        while (q.isNotEmpty()) {
            val cur = q.removeFirst()
            for ((n, bit) in dirs(cur)) if (tiles[cur] and bit != 0 && tiles[n] and opposite(bit) != 0 && !w[n]) { w[n] = true; q.addLast(n) }
        }
        return w
    }

    fun isSolved(): Boolean {
        if (!watered().all { it }) return false
        for (i in tiles.indices) for (bit in listOf(1, 2, 4, 8)) if (tiles[i] and bit != 0) {
            val n = dirs(i).firstOrNull { it.second == bit }?.first ?: return false
            if (tiles[n] and opposite(bit) == 0) return false
        }
        return true
    }

    /** A tile that is not yet turned the right way, if any. */
    fun hint(): Int? = tiles.indices.firstOrNull { i ->
        // Straight pieces look the same turned half way.
        val t = tiles[i]; val s = solution[i]
        t != s && !(s == 5 && t == 5) && !(s == 10 && t == 10)
    }
}
