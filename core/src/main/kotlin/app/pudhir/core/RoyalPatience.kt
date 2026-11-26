package app.pudhir.core

import kotlin.random.Random

/** A playing card: suit 0 spades, 1 hearts, 2 diamonds, 3 clubs; rank 1 (ace) to 13 (king). */
data class Card(val suit: Int, val rank: Int, var up: Boolean = false) {
    val red get() = suit == 1 || suit == 2
}

/**
 * Royal Patience: classic Klondike solitaire. Build the four foundations up from ace to king,
 * by suit. In the tableau, stack down in alternating colours; only kings fill empty columns.
 */
class RoyalPatience(val drawCount: Int, val tableau: List<MutableList<Card>>, val stock: MutableList<Card>) {
    val waste = ArrayList<Card>()
    val foundations = List(4) { ArrayList<Card>() }
    private val undoStack = ArrayList<Snapshot>()
    var moves = 0; private set

    private data class Snapshot(val t: List<List<Card>>, val s: List<Card>, val w: List<Card>, val f: List<List<Card>>, val moves: Int)

    private fun save() {
        undoStack += Snapshot(tableau.map { p -> p.map { it.copy() } }, stock.map { it.copy() }, waste.map { it.copy() }, foundations.map { f -> f.map { it.copy() } }, moves)
        if (undoStack.size > 200) undoStack.removeAt(0)
    }

    fun undo(): Boolean {
        val s = undoStack.removeLastOrNull() ?: return false
        for (i in tableau.indices) { tableau[i].clear(); tableau[i].addAll(s.t[i]) }
        stock.clear(); stock.addAll(s.s); waste.clear(); waste.addAll(s.w)
        for (i in foundations.indices) { foundations[i].clear(); foundations[i].addAll(s.f[i]) }
        moves = s.moves
        return true
    }

    fun canUndo() = undoStack.isNotEmpty()

    fun draw() {
        save()
        if (stock.isEmpty()) {
            stock.addAll(waste.reversed().map { it.copy(up = false) }); waste.clear()
        } else repeat(drawCount) { stock.removeLastOrNull()?.let { waste += it.copy(up = true) } }
        moves++
    }

    fun canStackOnTableau(card: Card, pile: Int): Boolean {
        val top = tableau[pile].lastOrNull() ?: return card.rank == 13
        return top.up && top.red != card.red && top.rank == card.rank + 1
    }

    fun canFound(card: Card): Int? {
        for (i in foundations.indices) {
            val f = foundations[i]
            if (f.isEmpty() && card.rank == 1 && foundations.indices.none { j -> j < i && foundations[j].isEmpty() }) return i
            if (f.isNotEmpty() && f.last().suit == card.suit && f.last().rank == card.rank - 1) return i
        }
        return null
    }

    private fun flipTops() { for (p in tableau) p.lastOrNull()?.let { if (!it.up) it.up = true } }

    sealed interface Source
    data object Waste : Source
    data class Pile(val pile: Int, val index: Int) : Source
    data class Foundation(val index: Int) : Source

    /** Moves [src] to tableau pile [to]. */
    fun moveToPile(src: Source, to: Int): Boolean {
        val cards: List<Card> = when (src) {
            Waste -> listOfNotNull(waste.lastOrNull())
            is Pile -> if (src.pile == to || src.index < 0 || src.index >= tableau[src.pile].size || !tableau[src.pile][src.index].up) emptyList() else tableau[src.pile].subList(src.index, tableau[src.pile].size).toList()
            is Foundation -> listOfNotNull(foundations[src.index].lastOrNull())
        }
        if (cards.isEmpty() || !canStackOnTableau(cards[0], to)) return false
        save()
        when (src) {
            Waste -> waste.removeAt(waste.lastIndex)
            is Pile -> repeat(cards.size) { tableau[src.pile].removeAt(tableau[src.pile].lastIndex) }
            is Foundation -> foundations[src.index].removeAt(foundations[src.index].lastIndex)
        }
        tableau[to].addAll(cards)
        flipTops(); moves++
        return true
    }

    /** Sends the top card of [src] up to a foundation. */
    fun moveToFoundation(src: Source): Boolean {
        val card = when (src) {
            Waste -> waste.lastOrNull()
            is Pile -> tableau[src.pile].lastOrNull()?.takeIf { src.index == tableau[src.pile].lastIndex && it.up }
            is Foundation -> null
        } ?: return false
        val f = canFound(card) ?: return false
        save()
        when (src) {
            Waste -> waste.removeAt(waste.lastIndex)
            is Pile -> tableau[src.pile].removeAt(tableau[src.pile].lastIndex)
            else -> {}
        }
        foundations[f] += card
        flipTops(); moves++
        return true
    }

    /** Tap-to-move: foundation first, then the best tableau pile. */
    fun autoMove(src: Source): Boolean {
        if (moveToFoundation(src)) return true
        val order = (0 until 7).sortedByDescending { tableau[it].isNotEmpty() }
        for (to in order) if (moveToPile(src, to)) return true
        return false
    }

    fun isWon() = foundations.all { it.size == 13 }

    fun canAutoFinish() = stock.isEmpty() && waste.isEmpty() && tableau.all { p -> p.all { it.up } }

    /** Plays one foundation move for auto-finish; false when none is left. */
    fun autoFinishStep(): Boolean {
        for (i in 0 until 7) if (tableau[i].isNotEmpty() && moveToFoundation(Pile(i, tableau[i].lastIndex))) return true
        return false
    }

    /** A helpful move description: (source, target pile or -1 for foundation), or null meaning "draw". */
    fun hint(): Pair<Source, Int>? {
        waste.lastOrNull()?.let { if (canFound(it) != null) return Waste to -1 }
        for (i in 0 until 7) tableau[i].lastOrNull()?.let { if (it.up && canFound(it) != null) return Pile(i, tableau[i].lastIndex) to -1 }
        // Moves that uncover a face-down card.
        for (i in 0 until 7) {
            val first = tableau[i].indexOfFirst { it.up }
            if (first <= 0) continue
            for (to in 0 until 7) if (to != i && canStackOnTableau(tableau[i][first], to)) return Pile(i, first) to to
        }
        waste.lastOrNull()?.let { w -> for (to in 0 until 7) if (canStackOnTableau(w, to)) return Waste to to }
        // A king into an empty column from a pile that has cards beneath it.
        for (i in 0 until 7) {
            val first = tableau[i].indexOfFirst { it.up }
            if (first > 0 && tableau[i][first].rank == 13) for (to in 0 until 7) if (tableau[to].isEmpty()) return Pile(i, first) to to
        }
        return null
    }

    companion object {
        fun generate(seed: Long, difficulty: Difficulty): RoyalPatience {
            val rng = Random(seed)
            val deck = ArrayList<Card>()
            for (s in 0 until 4) for (r in 1..13) deck += Card(s, r)
            deck.shuffle(rng)
            val tableau = List(7) { ArrayList<Card>() }
            var k = 0
            for (i in 0 until 7) for (j in 0..i) { tableau[i] += deck[k++].copy(up = j == i) }
            val stock = deck.subList(k, deck.size).map { it.copy() }.toMutableList()
            return RoyalPatience(if (difficulty.ordinal >= Difficulty.Hard.ordinal) 3 else 1, tableau, stock)
        }
    }
}
