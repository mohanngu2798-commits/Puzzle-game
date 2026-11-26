package app.pudhir.core

import kotlin.random.Random

/** A block shape: cells relative to its top-left corner, plus a colour index. */
data class Shape(val cells: List<Cell>, val colour: Int) {
    val height = cells.maxOf { it.r } + 1
    val width = cells.maxOf { it.c } + 1
}

/**
 * Brick Kiln: drag block shapes onto an 8x8 kiln floor. Filling a full row or
 * column fires those bricks and clears them. The game ends when no shape fits.
 */
class BrickKiln(seed: Long, val size: Int = 8) {
    private val rng = Random(seed)
    val board = IntArray(size * size) { -1 }
    val tray = arrayOfNulls<Shape>(3)
    var score = 0; private set
    var combo = 0; private set
    var lastCleared: List<Int> = emptyList(); private set

    init { refill() }

    fun fits(shape: Shape, r: Int, c: Int): Boolean = shape.cells.all { (dr, dc) ->
        val rr = r + dr; val cc = c + dc
        rr in 0 until size && cc in 0 until size && board[rr * size + cc] == -1
    }

    fun canPlaceAnywhere(shape: Shape): Boolean {
        for (r in 0..size - shape.height) for (c in 0..size - shape.width) if (fits(shape, r, c)) return true
        return false
    }

    /** Places tray piece [slot] with its top-left at (r,c). Returns false if it does not fit. */
    fun place(slot: Int, r: Int, c: Int): Boolean {
        val shape = tray[slot] ?: return false
        if (!fits(shape, r, c)) return false
        for ((dr, dc) in shape.cells) board[(r + dr) * size + c + dc] = shape.colour
        tray[slot] = null
        score += shape.cells.size
        val rows = (0 until size).filter { rr -> (0 until size).all { board[rr * size + it] != -1 } }
        val cols = (0 until size).filter { cc -> (0 until size).all { board[it * size + cc] != -1 } }
        val cleared = HashSet<Int>()
        for (rr in rows) for (cc in 0 until size) cleared += rr * size + cc
        for (cc in cols) for (rr in 0 until size) cleared += rr * size + cc
        lastCleared = cleared.toList()
        for (p in cleared) board[p] = -1
        val lines = rows.size + cols.size
        if (lines > 0) {
            combo++
            score += lines * size * lines * combo
        } else combo = 0
        if (tray.all { it == null }) refill()
        return true
    }

    fun isGameOver() = tray.filterNotNull().none { canPlaceAnywhere(it) }

    private fun refill() {
        // Make sure at least one new piece fits, so a fresh tray is never dead on arrival.
        repeat(20) {
            for (i in 0 until 3) tray[i] = randomShape()
            if (tray.filterNotNull().any { canPlaceAnywhere(it) }) return
        }
        tray[0] = Shape(listOf(Cell(0, 0)), 0)
    }

    private fun randomShape(): Shape {
        val base = SHAPES[rng.nextInt(SHAPES.size)]
        var cells = base
        repeat(rng.nextInt(4)) { cells = rotate(cells) }
        return Shape(cells, rng.nextInt(COLOURS))
    }

    companion object {
        const val COLOURS = 6
        private fun parse(vararg rows: String): List<Cell> =
            rows.flatMapIndexed { r, s -> s.mapIndexedNotNull { c, ch -> if (ch == '#') Cell(r, c) else null } }

        val SHAPES = listOf(
            parse("#"), parse("##"), parse("###"), parse("####"), parse("#####"),
            parse("##", "##"), parse("###", "###", "###"), parse("##", "#."), parse("###", "#..", "#.."),
            parse("###", ".#."), parse("##.", ".##"), parse(".##", "##."), parse("#..", "###"), parse("..#", "###"),
            parse("###", "###"),
        )

        fun rotate(cells: List<Cell>): List<Cell> {
            val h = cells.maxOf { it.r }
            return cells.map { Cell(it.c, h - it.r) }
        }
    }
}
