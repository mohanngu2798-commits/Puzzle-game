package app.pudhir.core

import kotlin.random.Random

/** Riddles in the style of Tamil folk riddles (vidukathai), retold in English. */
object Riddles {
    data class Riddle(val text: String, val answer: String)

    val all = listOf(
        Riddle("I have three eyes but cannot see, and sweet water hides inside me.", "Coconut"),
        Riddle("Born in water, I live in a jar, and in water I die.", "Salt"),
        Riddle("A lamp that burns with no oil and no wick, yet lights the whole world.", "Sun"),
        Riddle("Thousands of gems scattered on a dark cloth at night, and nobody gathers them by dawn.", "Stars"),
        Riddle("Green outside with white pearls inside, I bite your tongue though I have no teeth.", "Green chilli"),
        Riddle("A box with no lid and no lock, yet gold is hidden inside.", "Egg"),
        Riddle("I follow you all day long but vanish when night comes.", "Shadow"),
        Riddle("The more you take away from me, the bigger I grow.", "Hole"),
        Riddle("A white field sown with black seeds by hand, and harvested by the eyes.", "Writing"),
        Riddle("Small as a seed, I carry my own lamp through the night garden.", "Firefly"),
        Riddle("I have a mouth but never eat, a bed but never sleep, and I run without legs.", "River"),
        Riddle("I wear a hundred clothes, and the more you take off, the more you cry.", "Onion"),
        Riddle("My house is on my back and I am never late coming home.", "Tortoise"),
        Riddle("I have many teeth but never bite; I tidy heads every morning.", "Comb"),
        Riddle("One small room with many children, all wearing red caps.", "Matchbox"),
        Riddle("I fly without wings and cry without eyes.", "Cloud"),
        Riddle("I sit in the kitchen, eat fire all day and give you cooked rice.", "Cooking pot"),
        Riddle("I hang upside down, sleep all day and fly at night.", "Bat"),
        Riddle("A yellow coat with soft white sweetness inside.", "Banana"),
        Riddle("I move without legs, sing without a mouth and come without being called.", "Wind"),
        Riddle("It belongs to you, yet others use it far more than you do.", "Your name"),
        Riddle("Red pearls packed in a hundred little rooms behind a leather door.", "Pomegranate"),
        Riddle("A tall man on one leg, wearing a green turban.", "Coconut tree"),
        Riddle("The more I dry you, the wetter I get.", "Towel"),
        Riddle("It goes up and up and never comes down.", "Age"),
        Riddle("I have many ears but cannot hear a sound; I grow in flooded fields.", "Paddy"),
        Riddle("I shine to give you light, and weep as I melt away.", "Candle"),
        Riddle("A silver plate in the sky that grows and shrinks each month.", "Moon"),
        Riddle("I have hands but hold nothing, and walk all day without legs.", "Clock"),
        Riddle("Break me and I am more useful: I am cracked before I am cooked.", "Egg shell"),
        Riddle("I have a neck but no head, and wear a cap to keep my water in.", "Bottle"),
        Riddle("I am full of holes, yet I help you clean the flour.", "Sieve"),
        Riddle("A thousand soldiers in a row, each with a white coat, guarding a red cave.", "Teeth"),
        Riddle("I have no feet, yet I leave tracks wherever I go across the paper.", "Pen"),
        Riddle("You see me once in a year, in a minute, never; but twice in a moment.", "Letter M"),
        Riddle("I am always in front of you but can never be seen.", "Future"),
        Riddle("Feed me and I live, give me water and I die.", "Fire"),
        Riddle("I speak every language but have never been to school.", "Echo"),
        Riddle("A bridge of seven colours that no one can walk on.", "Rainbow"),
        Riddle("I run around the house all night but never leave the walls.", "Fence"),
    )

    /** A round of [n] riddles, each with four choices (the answer among them). */
    fun round(seed: Long, n: Int): List<Pair<Riddle, List<String>>> {
        val rng = Random(seed)
        val answers = all.map { it.answer }.distinct()
        return all.shuffled(rng).take(n).map { r ->
            val wrong = answers.filter { it != r.answer }.shuffled(rng).take(3)
            r to (wrong + r.answer).shuffled(rng)
        }
    }
}
