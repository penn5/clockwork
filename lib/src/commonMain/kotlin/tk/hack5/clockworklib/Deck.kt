package tk.hack5.clockworklib

import kotlinx.serialization.Serializable

@Serializable
class Deck(val cards: MutableList<Card>) {
    /**
     * This is the main constructor. It is used to generate a random deck.
     */
    constructor() : this(allCards.shuffled().toMutableList())

    fun pop(): Card {
        if (cards.isEmpty())
            throw IndexOutOfBoundsException("Game over!")
        val ret = cards.last()
        cards.removeAt(cards.size-1)
        return ret
    }

    fun add(card: Card) {
        cards.add(0, card)
    }

    companion object {
        private val allCards = mutableListOf<Card>()
        init {
            for (suit in Suit.values()) {
                for (number in 1..13) {
                    allCards.add(Card(suit, number))
                }
            }
        }
    }
}
