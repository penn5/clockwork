package tk.hack5.clockworklib

import kotlinx.serialization.*
import kotlinx.serialization.internal.NullableSerializer
import kotlinx.serialization.internal.SerialClassDescImpl

@Serializable
class VisibleState {
    var initialCards: Array<Card> // This starts at 0 but the cards start at 1 to match cards in real life.
    var visibleCards = arrayOfNulls<Card>(13)
    var currentAction: Card? = null

    constructor(deck: Deck) {
        val cards = arrayOfNulls<Card>(13)
        for (i in 0..12) {
            // Move the first 13 cards into the hidden state
            cards[i] = deck.pop()
            cards[i]!!.position = i+1
        }
        // Turn over the one in position "King"
        visibleCards[12] = cards[12]
        if (cards.any { it == null })
            throw RuntimeException("Failed to initialize deck!")
        initialCards = cards.filterNotNull().toTypedArray()
    }

    // It works. Don't touch.
    @Serializer(forClass = VisibleState::class)
    companion object : KSerializer<VisibleState> {
        override fun serialize(encoder: Encoder, obj: VisibleState) {
            val output = encoder.beginStructure(descriptor)
            output.encodeSerializableElement(descriptor, 0, Card.serializer().list, obj.initialCards.toList())
            output.encodeSerializableElement(descriptor, 1, NullableSerializer(Card.serializer()).list, obj.visibleCards.toList())
            output.encodeSerializableElement(descriptor, 2, NullableSerializer(Card.serializer()), obj.currentAction)
            output.endStructure(descriptor)
        }

        override fun deserialize(decoder: Decoder): VisibleState {
            var initialCards: Array<Card>? = null
            var visibleCards: Array<Card?>? = null
            var currentAction: Card? = null
            val input = decoder.beginStructure(descriptor)
            loop@while (true) {
                when (val i = input.decodeElementIndex(descriptor)) {
                    CompositeDecoder.READ_DONE -> break@loop
                    0 -> initialCards = input.decodeSerializableElement(descriptor, i, Card.serializer().list).toTypedArray()
                    1 -> visibleCards = input.decodeSerializableElement(descriptor, i, NullableSerializer(Card.serializer()).list).toTypedArray()
                    2 -> currentAction = input.decodeSerializableElement(descriptor, i, NullableSerializer(Card.serializer()))
                    else -> throw SerializationException("Unknown index $i")
                }
            }
            input.endStructure(descriptor)
            return VisibleState(initialCards ?: throw  MissingFieldException("initialCards"),
                    visibleCards ?: throw MissingFieldException("visibleCards"),
                    currentAction)
        }

        override val descriptor: SerialDescriptor = object : SerialClassDescImpl("Game") {
            init {
                addElement("initialCards")
                addElement("visibleCards")
                addElement("currentAction")
            }
        }
    }

    constructor(initialCards: Array<Card>, visibleCards: Array<Card?>, currentAction: Card?) {
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
