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
    )

    fun byId(id: String) = all.first { it.id == id }
}
