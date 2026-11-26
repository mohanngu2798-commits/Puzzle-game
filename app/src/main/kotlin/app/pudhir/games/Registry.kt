package app.pudhir.games

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import app.pudhir.core.Difficulty

/** What a game screen gets from the shell. */
class PlaySession(
    val gameId: String,
    val seed: Long,
    val difficulty: Difficulty,
    val accent: Color,
    /** The puzzle is solved. [hints] lowers the star rating. */
    val onSolved: (hints: Int) -> Unit,
    /** An endless or failable round ended. [score] is shown when not null. */
    val onGameOver: (score: Int?, message: String) -> Unit,
    val haptic: (strong: Boolean) -> Unit,
    /** Pays for a hint (1 star, or a rewarded ad) and runs [grant] once paid for. */
    val useHint: (grant: () -> Unit) -> Unit,
)

enum class Shelf(val title: String) { Hits("Quick plays"), Brain("Brain trainers") }

class GameInfo(
    val id: String,
    val name: String,
    val tagline: String,
    val rules: List<String>,
    val accent: Color,
    val glyph: String,
    val shelf: Shelf,
    /** Score-chasing game with no levels (no daily mode). */
    val endless: Boolean = false,
    val content: @Composable (PlaySession) -> Unit,
)

object Games {
    val all: List<GameInfo> = listOf(
        GameInfo(
            "kolam", "Kolam", "Draw one loop around the dots",
            listOf(
                "Draw a single closed loop along the dotted lines.",
                "A number tells how many sides of that square the loop uses.",
                "The loop never crosses or touches itself.",
                "Tap a side to draw, tap again to mark it empty.",
            ),
            Color(0xFFE5487A), "🌸", Shelf.Brain,
        ) { KolamGame(it) },
        GameInfo(
            "spicebox", "Spice Box", "Sudoku, one spice per row",
            listOf(
                "Fill the grid so every row, column and 3×3 box holds 1 to 9 once.",
                "Tap a square, then a number. Use notes for your guesses.",
                "There is always exactly one answer.",
            ),
            Color(0xFFF26B3A), "🌶️", Shelf.Brain,
        ) { SpiceBoxGame(it) },
        GameInfo(
            "threads", "Rangoli Threads", "Join colours, fill the floor",
            listOf(
                "Drag to join each pair of matching dots with a thread.",
                "Threads cannot cross or overlap.",
                "Fill every square of the floor to finish.",
            ),
            Color(0xFF9B5DE5), "🧵", Shelf.Brain,
        ) { ThreadsGame(it) },
        GameInfo(
            "brickkiln", "Brick Kiln", "Place blocks, fire full lines",
            listOf(
                "Drag each block onto the kiln floor.",
                "Fill a whole row or column to fire it and clear it.",
                "Clear lines in a row for combo points. The game ends when nothing fits.",
            ),
            Color(0xFFD9583B), "🧱", Shelf.Hits, endless = true,
        ) { BrickKilnGame(it) },
        GameInfo(
            "templetiles", "Temple Tiles", "Match free tile pairs",
            listOf(
                "Tap two matching tiles to clear them.",
                "A tile is free when nothing is on top and its left or right side is open.",
                "Clear every tile to win. Stuck? Shuffle.",
            ),
            Color(0xFF2BA36B), "🛕", Shelf.Hits,
        ) { TempleTilesGame(it) },
        GameInfo(
            "trio", "Bazaar Trio", "Collect three of a kind",
            listOf(
                "Tap an uncovered item to put it in your basket.",
                "Three alike in the basket are sold and vanish.",
                "Seven items with no triple and the basket bursts.",
            ),
            Color(0xFF3A86F2), "🧺", Shelf.Hits,
        ) { BazaarTrioGame(it) },
        GameInfo(
            "chaisort", "Chai Sort", "Pour until every glass is one colour",
            listOf(
                "Tap a glass, then another, to pour the top tea across.",
                "You can pour onto the same colour or into an empty glass.",
                "Finish when each glass holds a single colour.",
            ),
            Color(0xFFB5763B), "🍵", Shelf.Hits,
        ) { ChaiSortGame(it) },
        GameInfo(
            "bolts", "Bolt Bazaar", "Unscrew bolts into colour boxes",
            listOf(
                "Tap an uncovered bolt to unscrew it.",
                "It flies into the open box of its colour, or waits in the tray.",
                "A box holds three bolts. Fill the tray and you lose.",
            ),
            Color(0xFF6C7A89), "🔩", Shelf.Hits,
        ) { BoltBazaarGame(it) },
        GameInfo(
            "handloom", "Handloom", "Weave the hidden pattern",
            listOf(
                "The numbers beside each row and column are the runs of woven squares, in order.",
                "Tap or drag to weave squares. Switch to ✕ to mark squares that stay empty.",
                "Every pattern can be solved by logic alone.",
            ),
            Color(0xFFC2185B), "🧶", Shelf.Brain,
        ) { HandloomGame(it) },
        GameInfo(
            "bloom", "Bloom", "Balance jasmine and marigold",
            listOf(
                "Fill every square with jasmine or marigold. Tap to switch.",
                "Each row and column holds the same number of both.",
                "Never three of the same flower in a row, and no two rows or columns alike.",
            ),
            Color(0xFF6BBF59), "🌼", Shelf.Brain,
        ) { BloomGame(it) },
        GameInfo(
            "tea", "Tea Estate", "One bush per row, column and plot",
            listOf(
                "Plant exactly one tea bush in every row, every column and every coloured plot.",
                "Bushes may not touch each other, not even at the corners.",
                "Tap once to mark a square empty, twice to plant.",
            ),
            Color(0xFF2E8B57), "🍃", Shelf.Brain,
        ) { TeaEstateGame(it) },
        GameInfo(
            "autoride", "Auto Ride", "One route through every square",
            listOf(
                "Drag from stop 1 to draw one route through every square.",
                "Pass the numbered stops in order, finishing on the last one.",
                "Drag back along your route to undo.",
            ),
            Color(0xFF7B61FF), "🗺️", Shelf.Brain,
        ) { AutoRideGame(it) },
        GameInfo(
            "wells", "Hidden Wells", "Logic sweeper, no guessing",
            listOf(
                "Open every square that has no hidden well.",
                "A number tells how many wells touch that square, diagonals included.",
                "Long-press to flag a well. Every board can be cleared by logic alone.",
            ),
            Color(0xFF1C7ED6), "💧", Shelf.Brain,
        ) { HiddenWellsGame(it) },
        GameInfo(
            "godown", "Godown", "Push every crate home",
            listOf(
                "Push all the crates onto the marked spots.",
                "You can push one crate at a time, never pull.",
                "Swipe to step, or tap a square to walk there.",
            ),
            Color(0xFFC98B4E), "📦", Shelf.Brain,
        ) { GodownGame(it) },
        GameInfo(
            "autojam", "Auto Jam", "Free the golden auto",
            listOf(
                "Slide the cars and lorries along their lanes.",
                "Clear a path so the golden auto can leave through the gate on the right.",
                "Fewer moves earn more pride.",
            ),
            Color(0xFFF5A623), "🛺", Shelf.Brain,
        ) { AutoJamGame(it) },
        GameInfo(
            "mithai", "Mithai Match", "Swap sweets, line up three",
            listOf(
                "Swipe a sweet into its neighbour to swap them.",
                "Line up three or more alike to clear them. Four in a row clears the whole line.",
                "Reach the target score before your moves run out.",
            ),
            Color(0xFFF2709C), "🍬", Shelf.Hits,
        ) { MithaiMatchGame(it) },
        GameInfo(
            "goldmerge", "Gold Merge", "Slide and merge coins",
            listOf(
                "Swipe to slide every coin.",
                "Two equal coins merge into one worth double.",
                "Reach the target coin to win the level.",
            ),
            Color(0xFFD4A017), "💰", Shelf.Hits,
        ) { GoldMergeGame(it) },
        GameInfo(
            "postcards", "Postcards", "Swap tiles, restore the picture",
            listOf(
                "A painted postcard has been cut up and shuffled.",
                "Tap two tiles to swap them. Tiles in the right place lock in.",
                "Tap Peek to see the whole picture.",
            ),
            Color(0xFF1FB5C9), "🖼️", Shelf.Hits,
        ) { PostcardsGame(it) },
        GameInfo(
            "wordhunt", "Word Hunt", "Find the hidden words",
            listOf(
                "Drag across letters in a straight line to circle a word.",
                "Words can run across, down and, on harder levels, diagonally or backwards.",
                "Find every word on the list.",
            ),
            Color(0xFF4C6EF5), "🔤", Shelf.Hits,
        ) { WordHuntGame(it) },
        GameInfo(
            "patience", "Royal Patience", "Classic card solitaire",
            listOf(
                "Build the four foundations from ace to king, one suit each.",
                "On the table, stack cards down in alternating colours. Only kings fill empty columns.",
                "Tap a card to move it to the best spot. Tap the deck to draw.",
            ),
            Color(0xFF8E2C48), "🃏", Shelf.Hits,
        ) { RoyalPatienceGame(it) },
    )

    fun byId(id: String) = all.first { it.id == id }
}
