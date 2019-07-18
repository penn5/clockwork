package tk.hack5.clockworklib

import kotlinx.serialization.*
import kotlinx.serialization.internal.SerialClassDescImpl

@Serializable
class Game(val config: GameConfig) {
    private var deck: Deck
    var visibleState: VisibleState
    init {
        deck = Deck()
        visibleState = VisibleState(deck)
    }


    @Serializer(forClass = Game::class)
    companion object : KSerializer<Game>, DeserializationStrategy<Game> {
        override fun serialize(encoder: Encoder, obj: Game) {
            val output = encoder.beginStructure(descriptor)
            output.encodeSerializableElement(descriptor, 0, GameConfig.serializer(), obj.config)
            output.encodeSerializableElement(descriptor, 1, Deck.serializer(), obj.deck)
            output.encodeSerializableElement(descriptor, 2, VisibleState.serializer(), obj.visibleState)
            output.endStructure(descriptor)
        }

        override fun deserialize(decoder: Decoder): Game {
            var config: GameConfig? = null
            var deck: Deck? = null
            var visibleState: VisibleState? = null
            val input = decoder.beginStructure(descriptor)
            loop@while (true) {
                when (val i = input.decodeElementIndex(descriptor)) {
                    CompositeDecoder.READ_DONE -> break@loop
                    0 -> config = input.decodeSerializableElement(descriptor, i, GameConfig.serializer())
                    1 -> deck = input.decodeSerializableElement(descriptor, i, Deck.serializer())
                    2 -> visibleState = input.decodeSerializableElement(descriptor, i, VisibleState.serializer())
                    else -> throw SerializationException("Unknown index $i")
                }
            }
            input.endStructure(descriptor)
            return Game(config ?: throw MissingFieldException("config"),
                    deck?.cards ?: throw MissingFieldException("deck"),
                    visibleState ?: throw MissingFieldException("visibleState"))
        }

        override val descriptor: SerialDescriptor = object : SerialClassDescImpl("Game") {
            init {
                addElement("config")
                addElement("deck")
                addElement("visibleState")
            }
        }
    }


    /**
     * Start the game with a custom deck (for fixed scenarios)
     */
    constructor(config: GameConfig, customDeck: List<Card>) : this(config) {
        deck = Deck(customDeck.toMutableList())
    }


    /**
     * Start the game with a fixed state (to restore old state or for cheats/testing)
     */
    constructor(config: GameConfig, customDeck: List<Card>, customVisibleState: VisibleState) :
            this(config, customDeck) {
        visibleState = customVisibleState
    }

    fun getSizeOfDeck() = deck.cards.size

    /**
     * Turn over a card from the deck and store it
     * @return the card
     */
    fun getCard(): Card {
        visibleState.currentAction = deck.pop()
        return visibleState.currentAction!!
    }

    fun peekCard(): Card {
        return deck.peek()
    }

    /**
     * Check if a card is turned over
     * @return true if it is turned over (visible), otherwise false
     */
    fun isCardTurnedOver(index: Int): Boolean {
        return visibleState[index] != null
    }

    /**
     * Check if a card is in the right place
     * @return true if the card is in the right place, otherwise false. If the card is not turned
     *         over, return false
     */
    fun isCardCompleted(index: Int): Boolean {
        return (visibleState[index]?.number) == index
    }

    /**
     * Check if a card can be moved
     * @return true if it can be moved, otherwise false
     */
    fun isCardMoveable(index: Int): Boolean {
        return isCardTurnedOver(index) && !isCardCompleted(index)
    }

    /**
     * Return the last drawn card to the deck
     */
    fun returnToDeck() {
        deck.add(visibleState.currentAction!!)
    }

    /**
     * List all cards in a suit that are movable
     */
    fun getCardsOfSuit(suit: Suit): List<Card> {
        return visibleState.visibleCards.filterNotNull().filter { it.suit == suit }.filter {
            isCardMoveable(it.position!!) }
    }

    /**
     * Only valid if {@link #getAction(Card)} returns SWAP_SUIT_NUMBER or SWAP_ONE
     * Get all cards that can be moved
     * Note: this is only valid for the deletion stage. For addition stage, must use
     * {@link #getCardsOfSuit(Suit)}
     */
    fun getMovableCards(actionCard: Card): List<Card> {
        var ret = getCardsOfSuit(actionCard.suit)
        if (isCardMoveable(actionCard.number))
            ret = ret + visibleState[actionCard.number]!!
        if (ret.isEmpty())
            if (visibleState.visibleCards.filterNotNull().filter
                    { this.isCardMoveable(it.position!!) }.size == 1)
                ret = visibleState.visibleCards.filterNotNull().filter {
                    this.isCardMoveable(it.position!!) }
            else
                throw IndexOutOfBoundsException("No cards can be moved but getMovableCards called!")
        return ret
    }

    /**
     * Get the applicable action type
     */
    fun getAction(actionCard: Card): Action {
        val cardsInSuit = getCardsOfSuit(actionCard.suit)
        if (!isCardTurnedOver(actionCard.number))
            return Action.TURN_OVER
        if (visibleState.visibleCards.filterNotNull().filter { isCardMoveable(it.position!!) }
                        .isEmpty())
            return Action.DISCARD_CARD
        if (cardsInSuit.isEmpty())
            // if there is only one movable card
            return if (visibleState.visibleCards.filterNotNull().filter
                    { this.isCardMoveable(it.position!!) }.size == 1)
                Action.SWAP_ONE
            else
                // if there is multiple cards that can move, but aren't in the suit (incl. same num)
                Action.SWAP_ANY
        if (isCardCompleted(actionCard.number) && cardsInSuit.size == 1)
            return Action.SWAP_ONE
        if (!isCardCompleted(actionCard.number) && cardsInSuit.size == 1)
            return if (cardsInSuit.last().position == actionCard.number)
            // The only card in the suit is also the card at the position referred to by the action.
                Action.SWAP_ONE
            else
            // There's only 2 cards that can be swapped, just act as though we can swap any valid.
                Action.SWAP_SUIT_NUMBER
        // There's multiple cards in the suit. Swap whatever you want within the rules
        return Action.SWAP_SUIT_NUMBER
    }

    /**
     * Turn over card referred to by action card
     */
    fun doTurnOver(actionCard: Card) {
        visibleState.turnOver(actionCard.number)
        if (!config.discardOnReveal)
            returnToDeck()
    }

    /**
     * Swap 3 cards on the board
     * @param actionCard the card to put onto the board. It will replace the card in moveCard
     * @param moveCard the card to move on the board. It will replace the card in removeCard
     * @param removeCard the card to remove from the game. It will be discarded.
     */
    fun doSwap(actionCard: Card, moveCard: Card, removeCard: Card) {
        visibleState[moveCard.position!!] = actionCard
        visibleState[removeCard.position!!] = moveCard
    }

    /**
     * Swap the action card with the in-play card
     * @param actionCard the action card, to be added to play
     * @param removeCard the card currently in play to be removed
     */
    fun doSwap(actionCard: Card, removeCard: Card) {
        visibleState[removeCard.position!!] = actionCard
    }

    /**
     * Check if game is over, and if it is, return the reason
     * @return null if game is ongoing, true if won, false if lost
     */
    fun isWon(): Boolean? {
        // all cards complete = win
        if ((1..13).all { isCardCompleted(it) })
            return true
        // empty deck = lose
        if (deck.cards.isEmpty())
            return false
        return null
    }
}
