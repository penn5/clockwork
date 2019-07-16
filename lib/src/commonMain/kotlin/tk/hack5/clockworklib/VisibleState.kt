package tk.hack5.clockworklib

import kotlinx.serialization.*

@Serializable
class VisibleState {
    var initialCards = arrayOfNulls<Card>(13) // This starts at 0 but the cards start at 1 to match cards in real life.
    var visibleCards = arrayOfNulls<Card>(13)
    var currentAction: Card? = null

    constructor(deck: Deck) {
        for (i in 0..12) {
            // Move the first 13 cards into the hidden state
            initialCards[i] = deck.pop()
            initialCards[i]!!.position = i+1
        }
        // Turn over the one in position "King"
        visibleCards[12] = initialCards[12]
    }

    @Serializer(forClass = VisibleState::class)
    companion object : KSerializer<VisibleState> {
        @ImplicitReflectionSerializer
        override fun serialize(encoder: Encoder, obj: VisibleState) {
            encoder.encode(obj.initialCards)
            encoder.encode(obj.visibleCards)
            val currentAction = obj.currentAction
            if (currentAction == null)
                encoder.encodeNull()
            else
                encoder.encode(currentAction)
        }

        @ImplicitReflectionSerializer
        override fun deserialize(decoder: Decoder): VisibleState {
            return VisibleState(decoder.decode(), decoder.decode(), decoder.decode())
        }
    }

    constructor(initialCards: Array<Card?>, visibleCards: Array<Card?>, currentAction: Card?) {
        this.initialCards = initialCards
        this.visibleCards = visibleCards
        this.currentAction = currentAction
    }


    operator fun get(index: Int) = visibleCards[index-1]
    operator fun set(index: Int, value: Card) {
        value.position = index
        visibleCards[index-1] = value
    }
    fun turnOver(index: Int) {
        if (visibleCards[index-1] != null)
            throw IllegalArgumentException("Trying to turn over a card that's already revealed!")
        visibleCards[index-1] = initialCards[index-1]
    }
}
