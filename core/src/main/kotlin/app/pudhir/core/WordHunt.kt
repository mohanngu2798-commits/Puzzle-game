package app.pudhir.core

import kotlin.random.Random

/** A placed word: start cell, direction and length. */
data class PlacedWord(val word: String, val r: Int, val c: Int, val dr: Int, val dc: Int) {
    fun cells(size: Int): List<Int> = word.indices.map { (r + dr * it) * size + (c + dc * it) }
}

/**
 * Word Hunt: find the hidden words in the letter grid. Words run in straight lines;
 * harder levels add diagonals and backwards words.
 */
class WordHunt(val size: Int, val theme: String, val grid: CharArray, val words: List<PlacedWord>) {
    /** Returns the word whose cells match the dragged line from [a] to [b], if any. */
    fun match(a: Int, b: Int): PlacedWord? = words.firstOrNull { w ->
        val cells = w.cells(size)
        (cells.first() == a && cells.last() == b) || (cells.first() == b && cells.last() == a)
    }

    companion object {
        val THEMES: Map<String, List<String>> = mapOf(
            "Spices" to listOf("PEPPER", "CUMIN", "CLOVE", "GINGER", "TURMERIC", "CARDAMOM", "SAFFRON", "MUSTARD", "FENNEL", "NUTMEG", "CHILLI", "GARLIC", "CORIANDER", "CINNAMON", "TAMARIND"),
            "Fruits" to listOf("MANGO", "BANANA", "GUAVA", "PAPAYA", "LYCHEE", "ORANGE", "GRAPES", "LEMON", "COCONUT", "APPLE", "CHERRY", "MELON", "PEACH", "PLUM", "JACKFRUIT"),
            "Animals" to listOf("TIGER", "ELEPHANT", "PEACOCK", "MONKEY", "PARROT", "CAMEL", "LEOPARD", "RABBIT", "DEER", "BUFFALO", "COBRA", "EAGLE", "OTTER", "PANDA", "ZEBRA"),
            "Music" to listOf("VEENA", "SITAR", "TABLA", "FLUTE", "DRUM", "RHYTHM", "MELODY", "TEMPO", "CHORUS", "VIOLIN", "PIANO", "GUITAR", "SONG", "RAGA", "BEAT"),
            "Nature" to listOf("RIVER", "FOREST", "MOUNTAIN", "VALLEY", "OCEAN", "ISLAND", "DESERT", "MONSOON", "THUNDER", "RAINBOW", "BREEZE", "MEADOW", "LOTUS", "SUNRISE", "WATERFALL"),
            "Kitchen" to listOf("SPOON", "KETTLE", "GRATER", "LADLE", "OVEN", "PLATE", "BOWL", "KNIFE", "TONGS", "SIEVE", "MORTAR", "PESTLE", "TAWA", "CUP", "PAN"),
            "Space" to listOf("PLANET", "COMET", "GALAXY", "ORBIT", "ROCKET", "METEOR", "NEBULA", "SATURN", "VENUS", "MARS", "MOON", "STAR", "COSMOS", "ECLIPSE", "ASTEROID"),
            "Sports" to listOf("CRICKET", "KABADDI", "HOCKEY", "TENNIS", "CHESS", "CARROM", "FOOTBALL", "BOXING", "ARCHERY", "RUGBY", "GOLF", "SQUASH", "ROWING", "JUDO", "POLO"),
            "Colours" to listOf("CRIMSON", "SCARLET", "INDIGO", "VIOLET", "SAFFRON", "MAROON", "AMBER", "IVORY", "OLIVE", "CORAL", "TEAL", "GOLDEN", "SILVER", "BEIGE", "MAGENTA"),
            "Weather" to listOf("CLOUD", "STORM", "DRIZZLE", "HUMID", "FOGGY", "SUNNY", "WINDY", "FROST", "CYCLONE", "SHOWER", "BREEZE", "HAIL", "MIST", "HEAT", "CHILL"),
        )

        fun spec(d: Difficulty) = when (d) {
            Difficulty.Easy -> Triple(7, 6, false)
            Difficulty.Medium -> Triple(8, 8, false)
            Difficulty.Hard -> Triple(9, 9, true)
            Difficulty.Expert -> Triple(10, 11, true)
        }

        fun generate(seed: Long, difficulty: Difficulty): WordHunt {
            val rng = Random(seed)
            val (size, count, backwards) = spec(difficulty)
            val dirs = buildList {
                add(0 to 1); add(1 to 0)
                if (difficulty != Difficulty.Easy) { add(1 to 1); add(-1 to 1) }
                if (backwards) { add(0 to -1); add(-1 to 0); add(-1 to -1); add(1 to -1) }
            }
            while (true) {
                val (theme, pool) = THEMES.entries.toList()[rng.nextInt(THEMES.size)].toPair()
                val candidates = pool.filter { it.length <= size }.toMutableList().shuffled(rng)
                val grid = CharArray(size * size) { ' ' }
                val placed = ArrayList<PlacedWord>()
                for (w in candidates) {
                    if (placed.size >= count) break
                    repeat(150) {
                        if (placed.any { it.word == w }) return@repeat
                        val (dr, dc) = dirs[rng.nextInt(dirs.size)]
                        val r = rng.nextInt(size); val c = rng.nextInt(size)
                        val er = r + dr * (w.length - 1); val ec = c + dc * (w.length - 1)
                        if (er !in 0 until size || ec !in 0 until size) return@repeat
                        val pw = PlacedWord(w, r, c, dr, dc)
                        val cells = pw.cells(size)
                        if (cells.withIndex().all { (i, p) -> grid[p] == ' ' || grid[p] == w[i] }) {
                            cells.withIndex().forEach { (i, p) -> grid[p] = w[i] }
                            placed += pw
                        }
                    }
                }
                if (placed.size < count) continue
                val letters = "AEIOURSTLNMDCHPGBKVY"
                for (p in grid.indices) if (grid[p] == ' ') grid[p] = letters[rng.nextInt(letters.length)]
                return WordHunt(size, theme, grid, placed.sortedBy { it.word })
            }
        }
    }
}
