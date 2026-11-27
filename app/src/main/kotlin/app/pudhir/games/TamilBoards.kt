package app.pudhir.games

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pudhir.core.Difficulty
import app.pudhir.core.Nakshatra
import app.pudhir.core.PathinaaruKaai
import app.pudhir.core.Sozhi
import app.pudhir.core.Thaadi
import app.pudhir.core.Thaayam
import app.pudhir.ui.ActionChip
import app.pudhir.ui.ActionRow
import app.pudhir.ui.LocalPalette
import app.pudhir.ui.PrimaryButton
import app.pudhir.ui.SoftButton
import app.pudhir.ui.jellyClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val YouColour = Color(0xFFF26B3A)
private val PhoneColour = Color(0xFF3A86F2)

/** A carved-stone board: lines between points and tappable round points with pieces. */
@Composable
private fun PointBoard(
    points: List<Pair<Float, Float>>,
    lines: List<Pair<Int, Int>>,
    aspect: Float,
    dotFraction: Float,
    pieceAt: (Int) -> Int, // -1 none, 0 you, 1 phone, 2 neutral coin
    ring: (Int) -> Color?,
    enabled: Boolean,
    onTap: (Int) -> Unit,
) {
    val p = LocalPalette.current
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val w = minOf(maxWidth, maxHeight / aspect)
        val h = w * aspect
        val dot = w * dotFraction
        Box(
            Modifier.size(w, h).shadow(8.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(if (p.dark) listOf(Color(0xFF3A3530), Color(0xFF26221E)) else listOf(Color(0xFFEFE6D8), Color(0xFFD9CBB5)))),
        ) {
            val pad = 0.08f
            fun px(x: Float) = pad + x * (1 - 2 * pad)
            Canvas(Modifier.fillMaxSize()) {
                val line = if (p.dark) Color(0xFF8E8273) else Color(0xFF8A7560)
                for ((a, b) in lines) {
                    val pa = points[a]; val pb = points[b]
                    drawLine(line, Offset(px(pa.first) * size.width, px(pa.second) * size.height), Offset(px(pb.first) * size.width, px(pb.second) * size.height), 3.dp.toPx(), StrokeCap.Round)
                }
            }
            for (i in points.indices) {
                val (x, y) = points[i]
                val who = pieceAt(i)
                val r = ring(i)
                val pop by animateFloatAsState(if (who >= 0) 1f else 0.55f, spring(dampingRatio = 0.45f, stiffness = 500f), label = "pt")
                Box(
                    Modifier.offset(w * px(x) - dot / 2, h * px(y) - dot / 2).size(dot)
                        .jellyClick(enabled) { onTap(i) }
                        .graphicsLayer { scaleX = pop; scaleY = pop }
                        .shadow(if (who >= 0) 4.dp else 0.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            when (who) {
                                0 -> Brush.radialGradient(listOf(Color(0xFFFFB08A), YouColour))
                                1 -> Brush.radialGradient(listOf(Color(0xFF9CC4FF), PhoneColour))
                                2 -> Brush.radialGradient(listOf(Color(0xFFFFF1C4), Color(0xFFC98A1B)))
                                else -> Brush.radialGradient(listOf(if (p.dark) Color(0xFF5A5248) else Color(0xFFC9B79C), if (p.dark) Color(0xFF4A4238) else Color(0xFFB8A386)))
                            },
                        )
                        .border(if (r != null) 3.dp else 0.dp, r ?: Color.Transparent, CircleShape),
                )
            }
        }
    }
}

@Composable
private fun VersusHeader(you: String, phone: String, message: String) {
    val p = LocalPalette.current
    Text(message, color = p.ink, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        for ((c, t) in listOf(YouColour to you, PhoneColour to phone)) Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(c)); Spacer(Modifier.width(6.dp)); Text(t, color = p.muted, fontSize = 13.sp)
        }
    }
}

// ---------------------------------------------------------------- Thaadi

@Composable
fun ThaadiGame(s: PlaySession) {
    val game = remember { Thaadi() }
    var version by remember { mutableIntStateOf(0) }
    var selected by remember { mutableIntStateOf(-1) }
    var busy by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf<Thaadi.Move?>(null) }
    var hints by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val lines = remember { (0 until 24).flatMap { i -> Thaadi.ADJ[i].filter { it > i }.map { i to it } } }

    fun msg(): String = when {
        game.removing && game.turn == 0 -> "Thaadi! Tap a blue piece to take it"
        game.inHand[0] > 0 -> "Place a piece · ${game.inHand[0]} in hand"
        game.onBoard(0) == 3 -> "Only three left: you can jump anywhere"
        else -> "Slide a piece to make three in a row"
    }

    fun end(): Boolean {
        when (game.winner()) { 0 -> { s.onSolved(hints); return true }; 1 -> { s.onGameOver(null, "The phone won Thaadi"); return true } }
        return false
    }

    fun phone() {
        busy = true
        scope.launch {
            while (game.turn == 1 && game.winner() < 0) {
                delay(450)
                val m = withContext(Dispatchers.Default) { game.copy().bestMove(aiDepth(s.difficulty) + 1) } ?: break
                game.apply(m); s.haptic(m is Thaadi.Remove); version++
            }
            end(); busy = false
        }
    }

    fun play(m: Thaadi.Move) { game.apply(m); hint = null; selected = -1; s.haptic(m is Thaadi.Remove); version++; if (!end() && game.turn == 1) phone() }

    fun tap(i: Int) {
        if (busy || game.turn != 0) return
        val ms = game.moves()
        ms.firstOrNull { (it is Thaadi.Remove && it.at == i) || (it is Thaadi.Place && it.at == i) }?.let { play(it); return }
        if (game.board[i] == 0) { selected = i; s.haptic(false); return }
        ms.firstOrNull { it is Thaadi.Slide && it.from == selected && it.to == i }?.let { play(it); return }
        s.haptic(true)
    }

    Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        version.let { VersusHeader("You · ${game.count(0)}", "Phone · ${game.count(1)}", msg()) }
        Box(Modifier.weight(1f).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
            version.let {
                val targets = game.moves().filterIsInstance<Thaadi.Slide>().filter { it.from == selected }.map { it.to }.toSet()
                PointBoard(
                    List(24) { Thaadi.xy(it) }, lines, 1f, 0.1f,
                    pieceAt = { game.board[it] },
                    ring = { i ->
                        val h = hint
                        when {
                            h is Thaadi.Place && h.at == i || h is Thaadi.Remove && h.at == i || h is Thaadi.Slide && (h.from == i || h.to == i) -> Color(0xFFF5B83D)
                            i == selected -> Color.White
                            i in targets -> Color.White.copy(alpha = 0.6f)
                            game.removing && game.turn == 0 && game.board[i] == 1 -> Color(0xFFD62839)
                            else -> null
                        }
                    },
                    enabled = !busy,
                ) { tap(it) }
            }
        }
        ActionRow { ActionChip(Icons.Rounded.Lightbulb, "Hint", !busy) { s.useHint { hints++; hint = game.copy().bestMove(3) } } }
    }
}

// ---------------------------------------------------------------- Pathinaaru Kaai

@Composable
fun PathinaaruKaaiGame(s: PlaySession) {
    val game = remember { PathinaaruKaai() }
    var version by remember { mutableIntStateOf(0) }
    var selected by remember { mutableIntStateOf(-1) }
    var busy by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf<PathinaaruKaai.Move?>(null) }
    var hints by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val n = PathinaaruKaai.N
    val pts = remember { PathinaaruKaai.XY.map { (x, y) -> x / 4f to (y + 2f) / 8f } }
    val lines = remember { (0 until n).flatMap { i -> PathinaaruKaai.ADJ[i].filter { it > i }.map { i to it } } }

    fun end(): Boolean {
        when (game.winner()) { 0 -> { s.onSolved(hints); return true }; 1 -> { s.onGameOver(null, "The phone's soldiers won"); return true } }
        return false
    }

    fun phone() {
        busy = true
        scope.launch {
            while (game.turn == 1 && game.winner() < 0) {
                delay(420)
                val m = withContext(Dispatchers.Default) { game.copy().bestMove(aiDepth(s.difficulty)) } ?: break
                game.apply(m); s.haptic(m is PathinaaruKaai.Jump); version++
            }
            end(); busy = false
        }
    }

    fun tap(i: Int) {
        if (busy || game.turn != 0) return
        if (game.board[i] == 0 && game.chaining < 0) { selected = i; s.haptic(false); return }
        val m = game.moves().firstOrNull { it.from == (if (game.chaining >= 0) game.chaining else selected) && it.to == i } ?: run { s.haptic(true); return }
        game.apply(m); hint = null; s.haptic(m is PathinaaruKaai.Jump)
        selected = if (game.chaining >= 0) game.chaining else -1
        version++
        if (!end() && game.turn == 1) phone()
    }

    Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        version.let {
            VersusHeader("You · ${game.count(0)}", "Phone · ${game.count(1)}",
                if (game.chaining >= 0) "Keep jumping, or tap Done" else "Step, or jump over a blue soldier to take it")
        }
        Box(Modifier.weight(1f).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            version.let {
                val from = if (game.chaining >= 0) game.chaining else selected
                val targets = game.moves().filter { it.from == from }.map { it.to }.toSet()
                PointBoard(
                    pts, lines, 2f, 0.12f,
                    pieceAt = { game.board[it] },
                    ring = { i ->
                        val h = hint
                        when {
                            h != null && (h.from == i || h.to == i) -> Color(0xFFF5B83D)
                            i == from -> Color.White
                            i in targets -> Color.White.copy(alpha = 0.6f)
                            else -> null
                        }
                    },
                    enabled = !busy,
                ) { tap(it) }
            }
        }
        ActionRow {
            if (version >= 0 && game.chaining >= 0) ActionChip(Icons.Rounded.RestartAlt, "Done", !busy) {
                game.stop(); selected = -1; version++; if (!end()) phone()
            }
            ActionChip(Icons.Rounded.Lightbulb, "Hint", !busy) { s.useHint { hints++; hint = game.copy().bestMove(3) } }
        }
    }
}

// ---------------------------------------------------------------- Nakshatra

@Composable
fun NakshatraGame(s: PlaySession) {
    val p = LocalPalette.current
    val goal = when (s.difficulty) { Difficulty.Easy -> 7; Difficulty.Medium -> 8; else -> 9 }
    val game = remember { Nakshatra(goal) }
    var version by remember { mutableIntStateOf(0) }
    var start by remember { mutableIntStateOf(-1) }
    var hint by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) }
    var hints by remember { mutableIntStateOf(0) }
    val pts = remember {
        List(10) { i ->
            val k = i % 5
            val (rad, ang) = if (i < 5) 0.5f to (-90f + 72f * k) else 0.19f to (-54f + 72f * k)
            val a = Math.toRadians(ang.toDouble())
            (0.5f + rad * cos(a).toFloat()) to (0.53f + rad * sin(a).toFloat())
        }
    }
    val lines = remember { Nakshatra.LINES.flatMap { l -> listOf(l[0] to l[1], l[1] to l[2], l[2] to l[3]) } }

    fun tap(i: Int) {
        if (game.isWon()) return
        if (start < 0 || game.coins[i]) {
            if (game.coins[i] || game.moves().none { it.first == i }) { s.haptic(true); return }
            start = i; s.haptic(false); return
        }
        if (game.play(start, i)) {
            start = -1; hint = null; version++; s.haptic(false)
            if (game.isWon()) s.onSolved(hints) else if (game.isStuck()) s.haptic(true)
        } else { start = if (game.moves().any { it.first == i }) i else -1; s.haptic(true) }
    }

    Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        version.let {
            Text("${game.placed} of $goal coins placed", color = p.ink, fontWeight = FontWeight.SemiBold)
            Text(
                if (game.isStuck()) "No moves left. Undo or restart." else if (start < 0) "Tap an empty point to start" else "Now tap the empty point two steps along a line",
                color = if (game.isStuck()) p.bad else p.muted, fontSize = 13.sp,
            )
        }
        Box(Modifier.weight(1f).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
            version.let {
                val ends = if (start >= 0) game.moves().filter { it.first == start }.map { it.third }.toSet() else emptySet()
                PointBoard(
                    pts, lines, 1f, 0.13f,
                    pieceAt = { if (game.coins[it]) 2 else -1 },
                    ring = { i ->
                        val h = hint
                        when {
                            h != null && (h.first == i || h.third == i) -> Color(0xFFF5B83D)
                            i == start -> s.accent
                            i in ends -> s.accent.copy(alpha = 0.5f)
                            else -> null
                        }
                    },
                    enabled = true,
                ) { tap(it) }
            }
        }
        ActionRow {
            ActionChip(Icons.AutoMirrored.Rounded.Undo, "Undo") { if (game.undo()) { start = -1; version++ } }
            ActionChip(Icons.Rounded.RestartAlt, "Restart") { game.restart(); start = -1; hint = null; version++ }
            ActionChip(Icons.Rounded.Lightbulb, "Hint") {
                s.useHint {
                    hints++
                    hint = game.hint()
                    if (hint == null) { game.restart(); hint = game.hint() } // dead end: start over with a good first move
                    start = -1; version++
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Thaayam

@Composable
fun ThaayamGame(s: PlaySession) {
    val p = LocalPalette.current
    val game = remember { Thaayam(s.seed) }
    var version by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Throw the dice sticks. You need a 1 (dhayam) to bring a piece in.") }
    val scope = rememberCoroutineScope()
    val rnd = remember { Random(s.seed) }

    fun end(): Boolean {
        when (game.winner()) { 0 -> { s.onSolved(0); return true }; 1 -> { s.onGameOver(null, "The phone brought all its pieces home"); return true } }
        return false
    }

    suspend fun phoneTurn() {
        while (game.turn == 1 && game.winner() < 0) {
            delay(600)
            val n = game.throwSticks(); version++
            message = "The phone threw $n"
            delay(500)
            if (game.pending) {
                val piece = game.choose(if (s.difficulty == Difficulty.Easy) 0 else 1, rnd) ?: break
                val cut = game.move(piece); version++
                if (cut >= 0) { message = "The phone cut your piece!"; s.haptic(true) }
            }
        }
    }

    fun afterMine() {
        version++
        if (end()) return
        if (game.turn == 1) {
            busy = true
            scope.launch { phoneTurn(); if (!end()) message = "Your throw"; busy = false }
        }
    }

    fun throwNow() {
        if (busy || game.turn != 0 || game.pending) return
        val n = game.throwSticks(); s.haptic(false)
        message = when {
            game.pending -> "You threw $n" + if (n == 1) " · Dhayam!" else "" + " · tap a piece to move"
            else -> "You threw $n · no piece can move"
        }
        afterMine()
    }

    fun movePiece(k: Int) {
        if (busy || game.turn != 0 || !game.pending || k !in game.moves()) { s.haptic(true); return }
        val cut = game.move(k)
        if (cut >= 0) { message = "You cut the phone's piece!"; s.haptic(true) } else { message = if (game.turn == 0) "Throw again!" else "Phone's turn"; s.haptic(false) }
        afterMine()
    }

    Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        version.let {
            Text(message, color = p.ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            val side = minOf(maxWidth, maxHeight)
            val cell = side / 5
            Box(Modifier.size(side).shadow(8.dp, RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp)).background(Color(0xFFF4E6C8))) {
                Canvas(Modifier.fillMaxSize()) {
                    val cs = size.width / 5
                    for (r in 0..4) for (c in 0..4) {
                        val o = Offset(c * cs, r * cs)
                        drawRect(if ((r + c) % 2 == 0) Color(0xFFF4E6C8) else Color(0xFFEBD8B0), o, androidx.compose.ui.geometry.Size(cs, cs))
                        drawRect(Color(0xFF9C7B45), o, androidx.compose.ui.geometry.Size(cs, cs), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))
                        if ((r to c) in Thaayam.SAFE) {
                            val m = cs * 0.22f
                            drawLine(Color(0xFFB0472E), o + Offset(m, m), o + Offset(cs - m, cs - m), 3f)
                            drawLine(Color(0xFFB0472E), o + Offset(cs - m, m), o + Offset(m, cs - m), 3f)
                        }
                    }
                }
                version.let {
                    val movable = if (game.turn == 0) game.moves().toSet() else emptySet()
                    for (pl in 0..1) for (k in 0 until game.pieces) {
                        val at = game.pos[pl][k]
                        val (r, c) = if (at < 0) (if (pl == 0) 4 to 2 else 0 to 2) else game.cell(pl, at)
                        // Spread pieces sharing a square.
                        val jitter = (k - 1) * 0.18f + (if (pl == 0) 0.06f else -0.06f)
                        val x by animateFloatAsState(c + 0.5f + jitter, spring(dampingRatio = 0.6f, stiffness = 300f), label = "x")
                        val y by animateFloatAsState(r + 0.5f + (if (at < 0) (if (pl == 0) 0.32f else -0.32f) else 0f), spring(dampingRatio = 0.6f, stiffness = 300f), label = "y")
                        val tok = cell * 0.34f
                        Box(
                            Modifier.offset(cell * x - tok / 2, cell * y - tok / 2).size(tok)
                                .jellyClick(pl == 0 && k in movable) { movePiece(k) }
                                .shadow(3.dp, CircleShape).clip(CircleShape)
                                .background(if (pl == 0) YouColour else PhoneColour)
                                .border(if (pl == 0 && k in movable) 3.dp else 1.dp, if (pl == 0 && k in movable) Color(0xFFF5B83D) else Color.White, CircleShape)
                                .graphicsLayer { alpha = if (at == 24) 0.6f else 1f },
                        )
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            version.let { for (v in listOf(game.sticks.first, game.sticks.second)) DiceStick(v) }
            PrimaryButton("Throw", s.accent, enabled = !busy && game.turn == 0 && !game.pending) { throwNow() }
        }
    }
}

@Composable
private fun DiceStick(v: Int) {
    Box(
        Modifier.size(24.dp, 64.dp).shadow(3.dp, RoundedCornerShape(6.dp)).clip(RoundedCornerShape(6.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFFFFE8A3), Color(0xFFC98A1B)))),
        contentAlignment = Alignment.Center,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            repeat(v) { Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF5E3A17))) }
        }
    }
}

// ---------------------------------------------------------------- Sozhi

@Composable
fun SozhiGame(s: PlaySession) {
    val p = LocalPalette.current
    val target = when (s.difficulty) { Difficulty.Easy -> 30; Difficulty.Medium -> 40; Difficulty.Hard -> 50; Difficulty.Expert -> 60 }
    val game = remember { Sozhi(s.seed, target) }
    var version by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var toss by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("Toss the six cowries. Mouth-up shells score.") }
    val scope = rememberCoroutineScope()

    fun end(): Boolean {
        when (game.winner()) { 0 -> { s.onSolved(0); return true }; 1 -> { s.onGameOver(null, "The phone reached $target first"); return true } }
        return false
    }

    fun phoneTurn() {
        busy = true
        scope.launch {
            delay(600)
            while (game.turn == 1 && game.winner() < 0) {
                val pts = game.toss(); toss++; version++
                if (pts == 0) { message = "The phone tossed nothing up and lost its turn"; s.haptic(true); break }
                message = "The phone scored $pts (turn ${game.turnScore})"
                delay(750)
                if (!game.phoneKeepsGoing(aiDepth(s.difficulty))) { game.bank(); message = "The phone banked"; version++; break }
            }
            if (!end()) { delay(300); message = "Your toss" }
            busy = false
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        version.let {
            Text("First to $target", color = p.muted)
            Row(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                for ((label, v, c) in listOf(Triple("You", game.banked[0], YouColour), Triple("Phone", game.banked[1], PhoneColour))) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$v", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = c)
                        Text(label, color = p.muted)
                    }
                }
            }
            Text(message, color = p.ink, fontWeight = FontWeight.SemiBold)
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            androidx.compose.runtime.key(toss) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    game.lastToss.forEachIndexed { i, up -> Cowrie(up, i) }
                }
            }
        }
        version.let { Text("This turn: ${game.turnScore}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = p.ink) }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton("Toss", s.accent, enabled = !busy && game.turn == 0) {
                val pts = game.toss(); toss++; version++
                if (pts == 0) { message = "No shell mouth-up. Turn lost!"; s.haptic(true); phoneTurn() }
                else { message = if (pts == 12) "All six up! +12" else "+$pts"; s.haptic(false) }
            }
            SoftButton("Bank") {
                if (busy || game.turn != 0 || game.turnScore == 0) return@SoftButton
                game.bank(); version++; s.haptic(false)
                if (!end()) phoneTurn()
            }
        }
    }
}

@Composable
private fun Cowrie(up: Boolean, i: Int) {
    val spin = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(Unit) { spin.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 200f + i * 40f)) }
    Canvas(Modifier.size(40.dp, 56.dp).graphicsLayer { rotationZ = (1 - spin.value) * 540f; translationY = (1 - spin.value) * -120f }) {
        drawOval(Brush.radialGradient(listOf(Color(0xFFFFFBF0), Color(0xFFE8D6B0))), size = size)
        if (up) {
            drawLine(Color(0xFF8A6A3A), Offset(size.width / 2, size.height * 0.15f), Offset(size.width / 2, size.height * 0.85f), 5f, StrokeCap.Round)
            for (k in 1..5) { val y = size.height * (0.15f + k * 0.12f); drawLine(Color(0xFF8A6A3A), Offset(size.width * 0.38f, y), Offset(size.width * 0.62f, y), 2f) }
        } else drawCircle(Color(0xFFD9B98A), size.width * 0.12f, Offset(size.width / 2, size.height * 0.4f))
    }
}
