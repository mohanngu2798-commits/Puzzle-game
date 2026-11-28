package app.pudhir.core

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** A round body for the little physics games. Units are board widths (board is 1 wide). */
class Ball(var x: Float, var y: Float, var r: Float, var kind: Int) {
    var vx = 0f
    var vy = 0f
    /** Seconds since this body appeared (used for pop-in and happy faces). */
    var age = 0f
    var id = nextId++
    companion object { var nextId = 0 }
}

/**
 * Fruit Bazaar: drop fruits into the basket. Two of the same fruit touching merge into the next bigger one.
 * The basket is 1 wide and [height] tall. Grow the goal fruit to win; if fruits pile above the line, it's over.
 */
class FruitMerge(seed: Long, difficulty: Difficulty) {
    private val rng = Random(seed)
    val height = 1.4f
    val line = 0.2f
    val balls = ArrayList<Ball>()
    var score = 0
        private set
    /** The fruit to grow to win. */
    val goal = when (difficulty) { Difficulty.Easy -> 6; Difficulty.Medium -> 7; Difficulty.Hard -> 9; Difficulty.Expert -> 10 }
    var best = 0
        private set
    var current = rng.nextInt(3)
        private set
    var next = rng.nextInt(4)
        private set
    /** Merges since the last call, as (x, y, newKind), for pop effects. */
    val merges = ArrayList<Triple<Float, Float, Int>>()
    private var overTime = 0f

    fun radius(kind: Int) = 0.042f + kind * 0.025f

    fun drop(x: Float) {
        val r = radius(current)
        balls += Ball(x.coerceIn(r, 1 - r), r + 0.01f, r, current)
        current = next; next = rng.nextInt(5)
    }

    /** Advances the simulation by [dt] seconds. */
    fun step(dt: Float) {
        val sub = 6; val h = dt / sub
        for (b in balls) b.age += dt
        repeat(sub) {
            for (b in balls) { b.vy += 2.4f * h; b.x += b.vx * h; b.y += b.vy * h; b.vx *= 0.996f }
            for (iter in 0 until 3) {
                for (i in balls.indices) for (j in i + 1 until balls.size) {
                    val a = balls[i]; val b = balls[j]
                    val dx = b.x - a.x; val dy = b.y - a.y; val d = hypot(dx, dy); val min = a.r + b.r
                    if (d < min && d > 1e-5f) {
                        val nx = dx / d; val ny = dy / d
                        val ma = a.r * a.r; val mb = b.r * b.r; val push = min - d
                        a.x -= nx * push * mb / (ma + mb); a.y -= ny * push * mb / (ma + mb)
                        b.x += nx * push * ma / (ma + mb); b.y += ny * push * ma / (ma + mb)
                        val rel = (b.vx - a.vx) * nx + (b.vy - a.vy) * ny
                        if (rel < 0) {
                            val imp = -rel * 1.25f / (1 / ma + 1 / mb)
                            a.vx -= imp / ma * nx; a.vy -= imp / ma * ny; b.vx += imp / mb * nx; b.vy += imp / mb * ny
                        }
                    }
                }
                for (b in balls) {
                    if (b.x < b.r) { b.x = b.r; b.vx = abs(b.vx) * 0.3f }
                    if (b.x > 1 - b.r) { b.x = 1 - b.r; b.vx = -abs(b.vx) * 0.3f }
                    if (b.y > height - b.r) { b.y = height - b.r; b.vy = -abs(b.vy) * 0.15f; b.vx *= 0.92f }
                }
            }
        }
        mergeTouching()
        val settledHigh = balls.any { it.y - it.r < line && abs(it.vy) < 0.06f && it.age > 1.2f }
        overTime = if (settledHigh) overTime + dt else 0f
    }

    private fun mergeTouching() {
        var i = 0
        loop@ while (i < balls.size) {
            for (j in i + 1 until balls.size) {
                val a = balls[i]; val b = balls[j]
                if (a.kind == b.kind && a.kind < 10 && hypot(b.x - a.x, b.y - a.y) < a.r + b.r + 0.004f) {
                    val k = a.kind + 1
                    val m = Ball((a.x + b.x) / 2, (a.y + b.y) / 2, radius(k), k).also { it.vy = -0.25f }
                    m.x = m.x.coerceIn(m.r, 1 - m.r)
                    balls.removeAt(j); balls.removeAt(i); balls += m
                    score += (k + 1) * (k + 1) * 2
                    best = maxOf(best, k)
                    merges += Triple(m.x, m.y, k)
                    continue@loop
                }
            }
            i++
        }
    }

    fun isWon() = best >= goal
    fun isOver() = overTime > 1.5f
}

/**
 * Holi Pop: a hexagonal field of colour balloons. Aim and shoot a balloon; three or more of a colour
 * touching pop, and any balloons no longer hanging from the top fall.
 */
class HoliPop(seed: Long, difficulty: Difficulty) {
    private val rng = Random(seed)
    val cols = 9
    val rows = 14
    val colours = when (difficulty) { Difficulty.Easy -> 4; Difficulty.Medium -> 5; Difficulty.Hard -> 5; Difficulty.Expert -> 6 }
    /** -1 empty; rows are offset half a cell on odd rows. */
    val grid = Array(rows) { IntArray(cols) { -1 } }
    var loaded = 0
        private set
    var next = 0
        private set
    var shots = 0
        private set
    var score = 0
        private set
    private val dropEvery = when (difficulty) { Difficulty.Easy -> 9; Difficulty.Medium -> 8; Difficulty.Hard -> 7; Difficulty.Expert -> 6 }
    val r get() = 1f / (cols * 2 + 1)

    init {
        val startRows = when (difficulty) { Difficulty.Easy -> 4; Difficulty.Medium -> 5; Difficulty.Hard -> 6; Difficulty.Expert -> 7 }
        for (rr in 0 until startRows) for (c in 0 until cols) if (!(rr % 2 == 1 && c == cols - 1)) grid[rr][c] = rng.nextInt(colours)
        loaded = pick(); next = pick()
    }

    private fun pick(): Int {
        val present = grid.flatMap { it.filter { v -> v >= 0 } }.distinct()
        return if (present.isEmpty()) rng.nextInt(colours) else present[rng.nextInt(present.size)]
    }

    fun centre(row: Int, col: Int): Pair<Float, Float> = (r + col * 2 * r + (if (row % 2 == 1) r else 0f)) to (r + row * r * sqrt(3f))

    fun neighbours(row: Int, col: Int): List<Pair<Int, Int>> {
        val odd = row % 2 == 1
        val d = if (odd) listOf(-1 to 0, -1 to 1, 0 to -1, 0 to 1, 1 to 0, 1 to 1) else listOf(-1 to -1, -1 to 0, 0 to -1, 0 to 1, 1 to -1, 1 to 0)
        return d.map { (a, b) -> row + a to col + b }.filter { (a, b) -> a in 0 until rows && b in 0 until cols && !(a % 2 == 1 && b == cols - 1) }
    }

    /**
     * Traces a shot from the launcher at ([sx], [sy]) at [angle] (radians, 0 = right, upward negative y).
     * Returns the path points and the grid cell where the balloon sticks.
     */
    fun trace(sx: Float, sy: Float, angle: Float): Pair<List<Pair<Float, Float>>, Pair<Int, Int>?> {
        var x = sx; var y = sy
        var dx = cos(angle); val dy = sin(angle)
        val pts = arrayListOf(x to y)
        val step = r * 0.25f
        repeat(4000) {
            x += dx * step; y += dy * step
            if (x < r) { x = r; dx = -dx; pts += x to y }
            if (x > 1 - r) { x = 1 - r; dx = -dx; pts += x to y }
            if (y <= r) { pts += x to y; return pts to snap(x, y) }
            for (rr in 0 until rows) for (c in 0 until cols) if (grid[rr][c] >= 0) {
                val (cx, cy) = centre(rr, c)
                if (hypot(cx - x, cy - y) < r * 1.75f) { pts += x to y; return pts to snap(x, y) }
            }
        }
        return pts to null
    }

    private fun snap(x: Float, y: Float): Pair<Int, Int>? {
        var best: Pair<Int, Int>? = null; var bd = Float.MAX_VALUE
        for (rr in 0 until rows) for (c in 0 until cols) if (grid[rr][c] < 0) {
            if (rr % 2 == 1 && c == cols - 1) continue
            val (cx, cy) = centre(rr, c); val d = hypot(cx - x, cy - y)
            if (d < bd) { bd = d; best = rr to c }
        }
        return best
    }

    /** Fires along [angle]; returns popped and dropped cells for effects. */
    fun fire(sx: Float, sy: Float, angle: Float): Pair<List<Pair<Int, Int>>, List<Pair<Int, Int>>> {
        val (_, cell) = trace(sx, sy, angle)
        shots++
        val popped = ArrayList<Pair<Int, Int>>(); val dropped = ArrayList<Pair<Int, Int>>()
        if (cell != null) {
            val (cr, cc) = cell
            grid[cr][cc] = loaded
            val group = flood(cr, cc) { grid[it.first][it.second] == loaded }
            if (group.size >= 3) {
                for ((a, b) in group) { grid[a][b] = -1 }
                popped += group
                val hanging = HashSet<Pair<Int, Int>>()
                for (c in 0 until cols) if (grid[0][c] >= 0) hanging += flood(0, c) { grid[it.first][it.second] >= 0 }
                for (a in 0 until rows) for (b in 0 until cols) if (grid[a][b] >= 0 && (a to b) !in hanging) { grid[a][b] = -1; dropped += a to b }
                score += popped.size * 10 + dropped.size * 20
            }
        }
        if (shots % dropEvery == 0 && !isWon()) addRow()
        loaded = next; next = pick()
        return popped to dropped
    }

    fun swap() { val t = loaded; loaded = next; next = t }

    /** Pushes the field down two rows (keeping the zig-zag) and adds fresh balloons on top. */
    private fun addRow() {
        for (a in rows - 1 downTo 2) grid[a] = grid[a - 2].copyOf()
        grid[0] = IntArray(cols) { rng.nextInt(colours) }
        grid[1] = IntArray(cols) { if (it == cols - 1) -1 else rng.nextInt(colours) }
    }

    private fun flood(r0: Int, c0: Int, ok: (Pair<Int, Int>) -> Boolean): Set<Pair<Int, Int>> {
        val seen = hashSetOf(r0 to c0); val q = ArrayDeque(listOf(r0 to c0))
        while (q.isNotEmpty()) { val (a, b) = q.removeFirst(); for (n in neighbours(a, b)) if (n !in seen && ok(n)) { seen += n; q += n } }
        return seen
    }

    fun isWon() = grid.all { row -> row.all { it < 0 } }
    fun isLost() = (rows - 2 until rows).any { a -> grid[a].any { it >= 0 } }
}

/** Express Snake: a train on a grid picking up passengers. Each passenger adds a carriage. */
class ExpressSnake(seed: Long, val w: Int = 14, val h: Int = 20) {
    private val rng = Random(seed)
    val body = ArrayDeque<Int>()
    var dir = 0 // 0 up, 1 right, 2 down, 3 left
        private set
    private var pendingDir = 0
    var food = 0
        private set
    var score = 0
        private set
    var dead = false
        private set

    init {
        val start = (h / 2) * w + w / 2
        body.addFirst(start + 2 * w); body.addFirst(start + w); body.addFirst(start)
        placeFood()
    }

    fun turn(d: Int) { if ((d + 2) % 4 != dir) pendingDir = d }

    private fun placeFood() {
        val free = (0 until w * h).filter { it !in body }
        food = free[rng.nextInt(free.size)]
    }

    /** Moves one cell; returns true if a passenger was picked up. */
    fun tick(): Boolean {
        if (dead) return false
        dir = pendingDir
        val head = body.first()
        val r = head / w; val c = head % w
        val (nr, nc) = when (dir) { 0 -> r - 1 to c; 1 -> r to c + 1; 2 -> r + 1 to c; else -> r to c - 1 }
        if (nr !in 0 until h || nc !in 0 until w) { dead = true; return false }
        val n = nr * w + nc
        val eat = n == food
        if (!eat) body.removeLast()
        if (n in body) { dead = true; return false }
        body.addFirst(n)
        if (eat) { score += 10; placeFood() }
        return eat
    }
}

/**
 * Rangoli Paint: colour a rangoli by numbers. The design is rings of petals around a centre;
 * every region shows the number of the colour it takes.
 */
class RangoliPaint(seed: Long, difficulty: Difficulty) {
    data class Region(val ring: Int, val sector: Int, val sectors: Int, val colour: Int)
    val rings: Int
    val regions: List<Region>
    val painted: IntArray
    val palette = 6

    init {
        val rng = Random(seed)
        rings = when (difficulty) { Difficulty.Easy -> 3; Difficulty.Medium -> 4; Difficulty.Hard -> 5; Difficulty.Expert -> 6 }
        val list = ArrayList<Region>()
        for (ring in 0 until rings) {
            val sectors = if (ring == 0) 1 else listOf(6, 8, 12, 16)[minOf(ring - 1, 3)]
            // Symmetric colouring: the same colour repeats round the ring with a pattern.
            val period = if (sectors == 1) 1 else listOf(1, 2, 2, 3, 4).random(rng).coerceAtMost(sectors)
            val cols = List(period) { rng.nextInt(palette) }
            for (s in 0 until sectors) list += Region(ring, s, sectors, cols[s % period])
        }
        regions = list
        painted = IntArray(list.size) { -1 }
    }

    /** Region under a point given in board units centred on 0 (radius 1). */
    fun regionAt(x: Float, y: Float): Int? {
        val d = hypot(x, y); if (d > 1f) return null
        val ring = (d * rings).toInt().coerceAtMost(rings - 1)
        val ang = ((atan2(y, x) + Math.PI * 2.5) % (Math.PI * 2)).toFloat()
        return regions.indexOfFirst { it.ring == ring && it.sector == ((ang / (Math.PI * 2).toFloat()) * it.sectors).toInt().coerceAtMost(it.sectors - 1) }.takeIf { it >= 0 }
    }

    fun paint(i: Int, colour: Int): Boolean { if (regions[i].colour != colour) return false; painted[i] = colour; return true }
    fun isDone() = painted.indices.all { painted[it] == regions[it].colour }
}

/**
 * Carrom Flick: pocket every coin with the striker in a limited number of shots.
 * Board is 1x1; pockets in the four corners.
 */
class CarromFlick(seed: Long, difficulty: Difficulty) {
    val coins = ArrayList<Ball>()
    val striker = Ball(0.5f, 0.82f, 0.045f, -1)
    val pocketR = 0.065f
    val coinR = 0.032f
    var shotsLeft: Int
        private set
    var pocketed = 0
        private set
    val total: Int

    init {
        val rng = Random(seed)
        val n = when (difficulty) { Difficulty.Easy -> 5; Difficulty.Medium -> 7; Difficulty.Hard -> 9; Difficulty.Expert -> 9 }
        total = n
        shotsLeft = when (difficulty) { Difficulty.Easy -> 12; Difficulty.Medium -> 14; Difficulty.Hard -> 15; Difficulty.Expert -> 12 }
        coins += Ball(0.5f, 0.5f, coinR, 0) // the queen
        var k = 0
        while (coins.size < n) {
            val a = k * (Math.PI * 2 / 6) + (if (k >= 6) 0.5 else 0.0); val d = if (k < 6) coinR * 2.05f else coinR * 3.9f
            coins += Ball(0.5f + d * cos(a).toFloat(), 0.5f + d * sin(a).toFloat(), coinR, 1 + (k % 2))
            k++
        }
    }

    val moving get() = (coins + striker).any { hypot(it.vx, it.vy) > 0.002f }

    fun shoot(fromX: Float, angle: Float, power: Float) {
        striker.x = fromX.coerceIn(0.2f, 0.8f); striker.y = 0.82f
        striker.vx = cos(angle) * power; striker.vy = sin(angle) * power
        shotsLeft--
    }

    /** Coins pocketed during this step. */
    fun step(dt: Float): List<Ball> {
        val out = ArrayList<Ball>()
        val sub = 6; val h = dt / sub
        val all = ArrayList<Ball>(coins); all += striker
        repeat(sub) {
            for (b in all) { b.x += b.vx * h; b.y += b.vy * h; val f = 1f - 1.1f * h; b.vx *= f; b.vy *= f }
            for (i in all.indices) for (j in i + 1 until all.size) {
                val a = all[i]; val b = all[j]
                val dx = b.x - a.x; val dy = b.y - a.y; val d = hypot(dx, dy); val min = a.r + b.r
                if (d < min && d > 1e-6f) {
                    val nx = dx / d; val ny = dy / d
                    val ma = a.r * a.r; val mb = b.r * b.r
                    val push = (min - d)
                    a.x -= nx * push * mb / (ma + mb); a.y -= ny * push * mb / (ma + mb); b.x += nx * push * ma / (ma + mb); b.y += ny * push * ma / (ma + mb)
                    val rel = (b.vx - a.vx) * nx + (b.vy - a.vy) * ny
                    if (rel < 0) {
                        val j2 = -1.9f * rel / (1 / ma + 1 / mb)
                        a.vx -= j2 / ma * nx; a.vy -= j2 / ma * ny; b.vx += j2 / mb * nx; b.vy += j2 / mb * ny
                    }
                }
            }
            for (b in all) {
                if (b.x < b.r) { b.x = b.r; b.vx = abs(b.vx) * 0.75f }
                if (b.x > 1 - b.r) { b.x = 1 - b.r; b.vx = -abs(b.vx) * 0.75f }
                if (b.y < b.r) { b.y = b.r; b.vy = abs(b.vy) * 0.75f }
                if (b.y > 1 - b.r) { b.y = 1 - b.r; b.vy = -abs(b.vy) * 0.75f }
            }
            val it2 = coins.iterator()
            while (it2.hasNext()) {
                val c = it2.next()
                if (POCKETS.any { (px, py) -> hypot(c.x - px, c.y - py) < pocketR }) { it2.remove(); all.remove(c); out += c; pocketed++ }
            }
            if (POCKETS.any { (px, py) -> hypot(striker.x - px, striker.y - py) < pocketR }) {
                striker.vx = 0f; striker.vy = 0f; striker.x = 0.5f; striker.y = 0.82f
            }
        }
        for (b in all) if (hypot(b.vx, b.vy) < 0.004f) { b.vx = 0f; b.vy = 0f }
        return out
    }

    fun isWon() = coins.isEmpty()
    fun isLost() = !isWon() && shotsLeft <= 0 && !moving

    companion object { val POCKETS = listOf(0.035f to 0.035f, 0.965f to 0.035f, 0.035f to 0.965f, 0.965f to 0.965f) }
}
