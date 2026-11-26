package app.pudhir.core

import kotlin.math.abs
import kotlin.random.Random

/**
 * Rangoli Threads: connect each pair of same-coloured dots with a thread.
 * Threads may not cross, and together they must fill every cell.
 * [solution] holds one valid filling (colour index per cell); any valid filling wins.
 */
class ThreadsPuzzle(val size: Int, val endpoints: List<Pair<Cell, Cell>>, val solution: IntArray, val solutionPaths: List<List<Cell>>) {
    val colours get() = endpoints.size

    companion object {
        fun sizeFor(d: Difficulty) = when (d) {
            Difficulty.Easy -> 5; Difficulty.Medium -> 6; Difficulty.Hard -> 7; Difficulty.Expert -> 8
        }

        fun generate(seed: Long, difficulty: Difficulty): ThreadsPuzzle {
            val n = sizeFor(difficulty)
            val rng = Random(seed)
            val colours = when (difficulty) {
                Difficulty.Easy -> 4 + rng.nextInt(2)
                Difficulty.Medium -> 5 + rng.nextInt(2)
                Difficulty.Hard -> 6 + rng.nextInt(2)
                Difficulty.Expert -> 7 + rng.nextInt(2)
            }
            while (true) {
                val path = hamiltonian(n, rng)
                val cuts = cutPath(path.size, colours, rng) ?: continue
                val endpoints = mutableListOf<Pair<Cell, Cell>>()
                val sol = IntArray(n * n)
                val paths = ArrayList<List<Cell>>()
                var ok = true
                for ((k, range) in cuts.withIndex()) {
                    val a = path[range.first]; val b = path[range.last]
                    if (abs(a.r - b.r) + abs(a.c - b.c) <= 1) { ok = false; break }
                    endpoints += a to b
                    paths += path.subList(range.first, range.last + 1).toList()
                    for (i in range) sol[path[i].r * n + path[i].c] = k
                }
                if (ok) return ThreadsPuzzle(n, endpoints, sol, paths)
            }
        }

        /** A random Hamiltonian path on an n x n grid via backbite moves on a serpentine. */
        fun hamiltonian(n: Int, rng: Random): List<Cell> {
            var path = ArrayList<Cell>()
            for (r in 0 until n) {
                if (r % 2 == 0) for (c in 0 until n) path += Cell(r, c) else for (c in n - 1 downTo 0) path += Cell(r, c)
            }
            val index = HashMap<Cell, Int>()
            repeat(n * n * 12) {
                if (rng.nextBoolean()) path.reverse()
                index.clear(); path.forEachIndexed { i, c -> index[c] = i }
                val end = path[0]
                val options = end.neighbours4().filter { it.r in 0 until n && it.c in 0 until n && index[it] != 1 }
                if (options.isEmpty()) return@repeat
                val x = options[rng.nextInt(options.size)]
                val i = index[x]!!
                val head = path.subList(0, i).reversed()
                val next = ArrayList<Cell>(path.size)
                next.addAll(head); next.addAll(path.subList(i, path.size))
                path = next
            }
            return path
        }

        private fun cutPath(len: Int, parts: Int, rng: Random): List<IntRange>? {
            val minLen = 3
            if (parts * minLen > len) return null
            val extra = len - parts * minLen
            val weights = IntArray(parts)
            repeat(extra) { weights[rng.nextInt(parts)]++ }
            val ranges = ArrayList<IntRange>()
            var s = 0
            for (w in weights) { val l = minLen + w; ranges += s until s + l; s += l }
            return ranges
        }

        /** True when [fill] (colour per cell, -1 empty) is a complete, valid solution. */
        fun isSolved(p: ThreadsPuzzle, paths: List<List<Cell>>): Boolean {
            val n = p.size
            val seen = IntArray(n * n) { -1 }
            for ((k, path) in paths.withIndex()) {
                if (path.size < 2) return false
                val (a, b) = p.endpoints[k]
                val ends = setOf(path.first(), path.last())
                if (ends != setOf(a, b)) return false
                for (i in path.indices) {
                    val cl = path[i]
                    if (seen[cl.r * n + cl.c] != -1) return false
                    seen[cl.r * n + cl.c] = k
                    if (i > 0 && abs(path[i - 1].r - cl.r) + abs(path[i - 1].c - cl.c) != 1) return false
                }
            }
            return seen.none { it == -1 }
        }
    }
}
