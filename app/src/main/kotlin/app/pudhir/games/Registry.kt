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

enum class Shelf(val title: String) { Hits("Quick plays"), Brain("Brain trainers"), Traditional("Traditional games") }

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
    /** Played against the phone: no daily puzzle and no skipping; difficulty sets how strong the phone plays. */
    val versus: Boolean = false,
    val content: @Composable (PlaySession) -> Unit,
)

object Games {
    val all: List<GameInfo> = listOf(
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
            "clockwork", "Clockwork", "Set every clock to twelve",
            listOf(
                "Tap a gear to turn its clock hand a quarter turn forward.",
                "Every gear joined to it by a rod turns a quarter turn back.",
                "Point every hand at twelve to win.",
            ),
            Color(0xFFB8860B), "⚙️", Shelf.Brain,
        ) { ClockworkGame(it) },
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
        GameInfo(
            "pallanguzhi", "Pallanguzhi", "Seed Sowing · an ancient Tamil board game",
            listOf(
                "Tap a pit on your side to pick up its seeds and sow them one by one, anticlockwise.",
                "If the next pit has seeds, pick them up and keep sowing. If it is empty, capture the pit after it.",
                "Four-seed capture: whenever a pit reaches exactly four seeds, the owner of that pit takes them. Whoever holds the most seeds at the end wins.",
            ),
            Color(0xFF8B5A2B), "🥥", Shelf.Traditional, versus = true,
        ) { PallanguzhiGame(it) },
        GameInfo(
            "aadupuli", "Aadu Puli", "Goats & Tigers · hunt or be trapped",
            listOf(
                "You lead 15 goats against 3 tigers. First place your goats one by one, then move them along the lines.",
                "A tiger catches a goat by jumping over it in a straight line to an empty point.",
                "Trap all the tigers so none can move. If they catch 6 goats, the tigers win.",
            ),
            Color(0xFFE08A1E), "🐯", Shelf.Traditional, versus = true,
        ) { AaduPuliGame(it) },
        GameInfo(
            "paramapadham", "Paramapadham", "Snakes & Ladders · the original",
            listOf(
                "Roll the dice and race the phone to square 100, the gateway to heaven.",
                "Ladders lift you up. Snakes bring you down.",
                "Roll a six to roll again. You must land exactly on 100.",
            ),
            Color(0xFF1F8A4C), "🐍", Shelf.Traditional, versus = true,
        ) { ParamapadhamGame(it) },
        GameInfo(
            "thaayam", "Thaayam", "Dice Race · cut and capture",
            listOf(
                "Throw two dice sticks (0 to 3 each; two zeros count as 12). Race your three pieces round the board to the centre.",
                "A throw of 1 is the entry throw: it is the only throw that brings a new piece onto the board.",
                "Land on the phone's piece to cut it back home, except on the crossed safe squares. 1, 5, 6 and 12 give another throw.",
            ),
            Color(0xFFB0472E), "🎲", Shelf.Traditional, versus = true,
        ) { ThaayamGame(it) },
        GameInfo(
            "thaadi", "Thaadi", "Temple Mill · three in a row",
            listOf(
                "Take turns placing your nine pieces on the points, then slide them along the lines.",
                "Three of yours in a straight line lets you take away one of the phone's pieces.",
                "Leave the phone with two pieces, or no move, to win. With three pieces left you can jump anywhere.",
            ),
            Color(0xFF7A5C3E), "⚪", Shelf.Traditional, versus = true,
        ) { ThaadiGame(it) },
        GameInfo(
            "pathinaaru", "Pathinaaru Kaai", "Sixteen Soldiers · jump and capture",
            listOf(
                "You and the phone each have 16 soldiers. Move one soldier a step along a line to an empty point.",
                "Jump over an enemy soldier to the empty point just beyond it to capture it. You can keep jumping.",
                "Capture every enemy soldier, or leave them no move, to win.",
            ),
            Color(0xFF5D6B3A), "⚔️", Shelf.Traditional, versus = true,
        ) { PathinaaruKaaiGame(it) },
        GameInfo(
            "nakshatra", "Nakshatra", "Star Puzzle · place every coin",
            listOf(
                "Start on any empty point of the star, pass over the next point along a straight line, and place a coin on the point after it.",
                "The point you land on must be empty. The point you pass over can be anything.",
                "Place all the coins to win. Plan ahead or you will run out of moves.",
            ),
            Color(0xFFC98A1B), "⭐", Shelf.Traditional,
        ) { NakshatraGame(it) },
        GameInfo(
            "sozhi", "Sozhi", "Cowrie Toss · push your luck",
            listOf(
                "Toss six cowrie shells. Every shell that lands mouth-up scores a point; all six up scores 12.",
                "Keep tossing to build up your turn, or Bank to keep the points.",
                "If no shell lands mouth-up you lose that turn's points. First to the target wins.",
            ),
            Color(0xFFB48A5A), "🐚", Shelf.Traditional, versus = true,
        ) { SozhiGame(it) },
        GameInfo(
            "chathurangam", "Chathurangam", "Royal War · the ancestor of chess",
            listOf(
                "Checkmate the phone's Raja (king), or capture every other piece it has.",
                "Raja: one step any way. Mantri (minister): one step diagonally. Gaja (elephant): jumps exactly two squares diagonally.",
                "Ashva (horse): moves like a knight. Ratha (chariot): any distance straight. Soldier: one step forward, captures diagonally, and becomes a Mantri on the last row.",
            ),
            Color(0xFF8E2C48), "🐘", Shelf.Traditional, versus = true,
        ) { ChathurangamGame(it) },
        GameInfo(
            "vidukathai", "Vidukathai", "Folk Riddles · what am I?",
            listOf(
                "Riddles in the style of the old Tamil village riddles, retold in English.",
                "Read the riddle and pick the answer. You have two spare hearts for wrong guesses.",
                "Answer every riddle in the round to win.",
            ),
            Color(0xFF7B3F8C), "❓", Shelf.Traditional,
        ) { VidukathaiGame(it) },
    )

    fun byId(id: String) = all.first { it.id == id }
}
