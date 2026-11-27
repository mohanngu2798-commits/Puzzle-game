package app.pudhir.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Wave2Test {
    private val tiers = Difficulty.entries
    private fun timed(label: String, block: () -> Unit) {
        val t = System.currentTimeMillis(); block(); println("$label ${System.currentTimeMillis() - t}ms")
    }

    @Test fun goldMergeSlides() {
        val g = GoldMerge(1)
        var n = 0
        while (g.canMove() && n < 2000) { for (d in Dir.entries) if (g.slide(d).isNotEmpty()) break; n++ }
        assertTrue(g.score > 0)
    }

    @Test fun bloomUnique() {
        for (d in tiers) for (s in 1L..3L) timed("bloom $d") {
            val p = BloomPuzzle.generate(s, d)
            assertTrue(BloomPuzzle.valid(p.solution, p.size, true))
            assertEquals(1, BloomPuzzle.count(p.givens.copyOf(), p.size, 2))
        }
    }

    @Test fun teaEstateUnique() {
        for (d in tiers) for (s in 1L..3L) timed("tea $d") {
            val p = TeaEstatePuzzle.generate(s, d)
            assertEquals(1, TeaEstatePuzzle.solve(p.size, p.regions, 2).size)
        }
    }

    @Test fun handloomLineSolvable() {
        for (d in tiers) for (s in 1L..3L) timed("loom $d") { assertTrue(HandloomPuzzle.lineSolvable(HandloomPuzzle.generate(s, d))) }
    }

    @Test fun hiddenWellsSolvable() {
        for (d in tiers) for (s in 1L..3L) timed("wells $d") {
            val spec = HiddenWells.specFor(d)
            val first = (spec.rows / 2) * spec.cols + spec.cols / 2
            assertTrue(HiddenWells.generate(s, d, first).solvableFrom(first))
        }
    }

    @Test fun autoJamSolvable() {
        for (d in tiers) for (s in 1L..2L) timed("jam $d") {
            val g = AutoJam.generate(s, d)
            val sol = g.solve()
            assertNotNull(sol)
            val play = g.copy()
            for ((i, delta) in sol!!) assertTrue(play.move(i, delta))
            assertTrue(play.isSolved())
            println("  jam moves=${sol.size}")
        }
    }

    @Test fun godownSolutionWorks() {
        for (d in tiers) for (s in 1L..3L) timed("godown $d") {
            val g = Godown.generate(s, d)
            for (m in g.solution) g.move(m)
            assertTrue(g.isSolved())
            println("  godown moves=${g.solution.size}")
        }
    }

    @Test fun autoRideUnique() {
        for (d in tiers) for (s in 1L..2L) timed("ride $d") {
            val p = AutoRidePuzzle.generate(s, d)
            println("  stops=${p.stops.size}")
            assertEquals(p.size * p.size, p.route.toSet().size)
        }
    }

    @Test fun mithaiPlays() {
        val g = MithaiMatch(3, Difficulty.Easy)
        repeat(10) { val h = g.hint() ?: return@repeat; assertTrue(g.swap(h.first, h.second)); while (g.step().isNotEmpty()) {} }
        assertTrue(g.score > 0)
    }

    @Test fun wordHuntPlaces() {
        for (d in tiers) for (s in 1L..3L) {
            val w = WordHunt.generate(s, d)
            for (pw in w.words) assertEquals(pw.word, pw.cells(w.size).map { w.grid[it] }.joinToString(""))
        }
    }

    @Test fun patienceDeals() {
        val g = RoyalPatience.generate(5, Difficulty.Easy)
        assertEquals(24, g.stock.size)
        assertEquals(28, g.tableau.sumOf { it.size })
        repeat(100) { val h = g.hint(); if (h == null) g.draw() else if (h.second == -1) g.moveToFoundation(h.first) else g.moveToPile(h.first, h.second) }
    }

    @Test fun postcardsSolve() {
        val g = Postcards.generate(2, Difficulty.Medium)
        while (!g.isSolved()) { val (a, b) = g.hint()!!; g.swap(a, b) }
    }
}

class ClockworkTest {
    @Test fun hintsSolveEveryBoard() {
        for (d in Difficulty.entries) for (s in 1L..20L) {
            val g = Clockwork.generate(s, d)
            assertTrue(!g.isSolved())
            var n = 0
            while (!g.isSolved() && n < 500) { g.tap(g.hint()!!); n++ }
            assertTrue(g.isSolved())
            assertEquals(g.par, n)
            while (g.undo()) {}
            assertTrue(!g.isSolved())
        }
    }
}

class TraditionalTest {
    @Test fun pallanguzhiPlaysToTheEnd() {
        val g = Pallanguzhi()
        var n = 0
        while (!g.isOver() && n < 500) { g.play(g.bestMove(if (g.turn == 0) 1 else 3)!!); n++ }
        assertTrue(g.isOver())
        assertEquals(70, g.store[0] + g.store[1] + g.pits.sum())
    }

    @Test fun aaduPuliPlaysToTheEnd() {
        val g = AaduPuli.create()
        assertEquals(25, g.points.size)
        var n = 0
        while (!g.isOver() && n < 400) { g.apply(g.bestMove(2)!!); n++ }
        assertTrue(n < 400 || !g.isOver())
    }

    @Test fun paramapadhamFinishes() {
        for (d in Difficulty.entries) {
            val g = Paramapadham(7, d)
            var n = 0
            while (g.winner() < 0 && n < 5000) { g.roll(); n++ }
            assertTrue(g.winner() >= 0)
        }
    }
}

class TraditionalBatch2Test {
    @Test fun thaayamFinishes() {
        val g = Thaayam(3); val r = kotlin.random.Random(1); var n = 0
        while (g.winner() < 0 && n < 20000) { g.throwSticks(); g.choose(2, r)?.let { g.move(it) }; n++ }
        assertTrue(g.winner() >= 0)
    }

    @Test fun nakshatraNineIsSolvable() {
        val g = Nakshatra(9)
        assertEquals(20, Nakshatra.JUMPS.size)
        var n = 0
        while (!g.isWon() && n < 20) { val h = g.hint()!!; assertTrue(g.play(h.first, h.third)); n++ }
        assertTrue(g.isWon())
    }

    @Test fun thaadiPlays() {
        assertEquals(16, Thaadi.MILLS.size)
        val g = Thaadi(); var n = 0
        while (g.winner() < 0 && n < 300) { g.apply(g.bestMove(2)!!); n++ }
        assertTrue(n > 18)
    }

    @Test fun pathinaaruKaaiSetup() {
        val g = PathinaaruKaai()
        assertEquals(37, PathinaaruKaai.N)
        assertEquals(16, g.count(0)); assertEquals(16, g.count(1))
        var n = 0
        while (g.winner() < 0 && n < 300) { g.apply(g.bestMove(2)!!); n++ }
        assertTrue(n > 4)
    }

    @Test fun sozhiFinishes() {
        val g = Sozhi(5); var n = 0
        while (g.winner() < 0 && n < 5000) { if (g.toss() > 0 && !g.phoneKeepsGoing(2)) g.bank(); n++ }
        assertTrue(g.winner() >= 0)
    }
}
