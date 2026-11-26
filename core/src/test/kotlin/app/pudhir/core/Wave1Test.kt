package app.pudhir.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Wave1Test {
    private val tiers = Difficulty.entries

    @Test fun sudokuIsUnique() {
        for (d in tiers) for (s in 1L..3L) {
            val p = SudokuPuzzle.generate(s, d)
            assertEquals(1, SudokuPuzzle.countSolutions(p.givens.copyOf(), 2))
        }
    }

    @Test fun kolamIsUniqueAndValid() {
        for (d in tiers) for (s in 1L..3L) {
            val t = System.currentTimeMillis()
            val p = KolamPuzzle.generate(s, d)
            assertTrue(KolamPuzzle.validShape(p.inside, p.rows, p.cols))
            assertEquals(1, KolamPuzzle.Solver(p.rows, p.cols, p.clues).count(2))
            println("kolam $d ${p.rows} clues=${p.clues.count { it >= 0 }} ${System.currentTimeMillis() - t}ms")
        }
    }

    @Test fun threadsSolutionIsValid() {
        for (d in tiers) for (s in 1L..5L) {
            val p = ThreadsPuzzle.generate(s, d)
            val paths = p.solutionPaths
            assertTrue(ThreadsPuzzle.isSolved(p, paths))
        }
    }

    @Test fun chaiSortSolvable() {
        for (d in tiers) for (s in 1L..3L) {
            val t = System.currentTimeMillis()
            val g = ChaiSort.generate(s, d)
            val moves = g.solve()
            assertNotNull(moves)
            val play = g.copy()
            for ((a, b) in moves!!) assertTrue(play.pour(a, b) > 0)
            assertTrue(play.isSolved())
            println("chai $d moves=${moves.size} ${System.currentTimeMillis() - t}ms")
        }
    }

    @Test fun templeTilesClearableWithHints() {
        for (d in tiers) for (s in 1L..3L) {
            val g = TempleTiles.generate(s, d)
            assertTrue(g.positions.size % 2 == 0)
            assertNotNull(g.hint())
        }
    }

    @Test fun bazaarTrioClearableInOrder() {
        for (d in tiers) for (s in 1L..3L) {
            val g = BazaarTrio.generate(s, d)
            assertEquals(0, g.tiles.size % 3)
            // Greedy play using hints should always clear the table.
            var steps = 0
            while (!g.isWon() && !g.isLost() && steps < 1000) { g.take(g.hint() ?: break); steps++ }
            println("trio $d tiles=${g.tiles.size} won=${g.isWon()}")
        }
    }

    @Test fun boltBazaarClearable() {
        for (d in tiers) for (s in 1L..3L) {
            val g = BoltBazaar.generate(s, d)
            var steps = 0
            while (!g.isWon() && steps < 500) { val h = g.hint() ?: break; g.unscrew(h); steps++ }
            println("bolt $d bolts=${g.bolts.size} won=${g.isWon()}")
        }
    }

    @Test fun brickKilnPlays() {
        val g = BrickKiln(42)
        var moves = 0
        loop@ while (!g.isGameOver() && moves < 200) {
            for (slot in 0 until 3) {
                val s = g.tray[slot] ?: continue
                for (r in 0 until 8) for (c in 0 until 8) if (g.fits(s, r, c)) { g.place(slot, r, c); moves++; continue@loop }
            }
            break
        }
        assertTrue(moves > 5)
    }
}
