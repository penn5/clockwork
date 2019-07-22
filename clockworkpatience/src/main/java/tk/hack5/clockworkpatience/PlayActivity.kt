package tk.hack5.clockworkpatience

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.res.ResourcesCompat
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import kotlinx.android.synthetic.main.activity_play.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import tk.hack5.clockworklib.*
import java.io.FileNotFoundException
import kotlin.concurrent.thread


class PlayActivity : AppCompatActivity() {

    private lateinit var cardViews: Array<CardView?>
    private lateinit var game: Game
    private var clickedCard: Pair<Int, CardView>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play)

        // Initialize layout with cards generated at runtime
        val layoutInflater = layoutInflater
        var newCard: CardView
        cardViews = arrayOfNulls(13)
        cardViews[12] = center_card as CardView
        Log.d(tag, resources.getDimension(R.dimen.card_rad).toInt().toString())
        for ((i, cardNum) in ((11..11)+(0..10)).withIndex()) { // clock goes 12,1..11,13
            newCard = layoutInflater.inflate(R.layout.card, rootView, false) as CardView
            newCard.id = CardView.generateViewId()
            newCard.rotation = i * 30f
            rootView.addView(newCard)
            cardViews[cardNum] = newCard
        }
        val constraints = ConstraintSet()
        constraints.clone(rootView)
        for ((i, cardNum) in ((11..11)+(0..10)).withIndex()) {
            constraints.constrainCircle(cardViews[cardNum]!!.id, R.id.center,
                    resources.getDimension(R.dimen.card_rad).toInt(), i * 30f)
        }
        constraints.applyTo(rootView)
        for ((i, card) in cardViews.withIndex()) {
            ((card!!.getChildAt(0) as ConstraintLayout).getChildAt(0) as TextView).text =
                    (i+1).toString()
            card.setOnClickListener { onTouchForCard(i+1, it) }
        }

        deck.setOnClickListener { onTouchForCard(0, it) }

        val json = Json(JsonConfiguration.Stable)

        // Check mode
        var resume = intent.getBooleanExtra(EXTRA_RESUME, false)
        var gameState = ""
        if (resume) {
            try {
                openFileInput(saveFile).use {
                    gameState = String(it.readBytes())
                }
            } catch (e: FileNotFoundException) {
                Log.w(tag, "saved state FnEE", e)
                resume = false
            } catch (e: Exception) {
                Log.e(tag, "Failed to restore saved state, file access fail!", e)
            }
        }
        if (resume) {
            Log.d(tag, "Got serialized game state $gameState")
            game = json.parse(Game.serializer(), gameState)
        } else {
            game = Game(GameConfig(intent.getBooleanExtra(EXTRA_DISCARD_ON_REVEAL, false)))
            game.getCard()
        }
        updateGame()
    }

    private fun updateGame() {
        updateDeck()
        updateVisibles()
        updateClickables()
    }

    private fun updateDeck() {
        (deck_remaining as TextView).text = game.getSizeOfDeck().toString()
        putCardPhotoAndNum(game.visibleState.currentAction!!, deck as CardView)
        putCardPhotoAndNum(game.peekCard(), deck_behind_second as CardView)
    }

    private fun updateClickables(deletion: Boolean = false) {
        deck.isClickable = false
        when (game.getAction(game.visibleState.currentAction!!)) {
            Action.TURN_OVER -> {
                makeAllClickable(false)
                makeOneClickable(true, cardViews[game.visibleState.currentAction!!.number-1]!!)
            }
            Action.SWAP_ONE -> {
                makeAllClickable(false)
                makeOneClickable(true, cardViews[game.getMovableCards(game.visibleState.currentAction!!).first().position!!-1]!!)
            }
            Action.SWAP_ANY -> {
                makeAllClickable(false)
                for (card in game.visibleState.visibleCards) {
                    card?.let {
                        if (game.isCardMoveable(it.position!!))
                            makeOneClickable(true, cardViews[it.position!! - 1]!!)
                    }
                }
            }
            Action.SWAP_SUIT_NUMBER -> {
                val swaps = when(deletion) {
                    true -> game.getMovableCards(game.visibleState.currentAction!!)
                    false -> game.getCardsOfSuit(game.visibleState.currentAction!!.suit)
                }
                makeAllClickable(false)
                for (card in swaps) {
                    // Make all swappable cards clickable
                    makeOneClickable(true, cardViews[card.position!!-1]!!)
                }
            }
            Action.DISCARD_CARD -> {
                makeAllClickable(false)
                deck.isClickable = true
                (deck as CardView).setCardBackgroundColor(ResourcesCompat.getColor(resources, R.color.card_background_movable, theme))
            }
        }
        for ((i, card) in cardViews.withIndex()) {
            if (game.isCardCompleted(i+1))
                card!!.setCardBackgroundColor(ResourcesCompat.getColor(resources, R.color.card_background_correct, theme))
        }
    }

    private fun updateVisibles() {
        for ((i, card) in cardViews.withIndex()) {
            val cardData = game.visibleState[i+1]
            putCardPhotoAndNum(cardData, card!!)
            (deck as CardView).setCardBackgroundColor(ResourcesCompat.getColor(resources, R.color.deck_background_default, theme))
        }
    }

    private fun makeAllClickable(clickable: Boolean) {
        for (card in cardViews)
            makeOneClickable(clickable, card!!)
    }
    private fun makeOneClickable(clickable: Boolean, cardView: CardView) {
        cardView.isClickable = clickable
        cardView.setCardBackgroundColor(if (clickable) ResourcesCompat.getColor(resources, R.color.card_background_movable, theme) else ResourcesCompat.getColor(resources, R.color.card_background_default, theme))
    }
    
    private fun putCardPhotoAndNum(card: Card?, view: CardView) {
        putCardPhoto(card?.suit, view)
        putCardNum(card?.number, view)
    }
    private fun putCardPhoto(suit: Suit?, view: CardView) {
        val photo = when (suit) {
            Suit.CLUB -> R.drawable.cards_club
            Suit.DIAMOND -> R.drawable.cards_diamond
            Suit.HEART -> R.drawable.cards_heart
            Suit.SPADE -> R.drawable.cards_spade
            null -> R.drawable.cards_playing_outline
        }
        putCardPhoto(photo, view)
    }
    private fun putCardPhoto(@DrawableRes photo: Int, view: CardView) {
        val constraintLayout = view.getChildAt(0) as ConstraintLayout
        (constraintLayout.getChildAt(1) as ImageView).setImageResource(photo)
        (constraintLayout.getChildAt(2) as ImageView).setImageResource(photo)
    }
    private fun putCardNum(num: Int?, view: CardView) {
        val text = when (num) {
            1 -> resources.getString(R.string.ace)
            in 2..10 -> num.toString()
            11 -> resources.getString(R.string.jack)
            12 -> resources.getString(R.string.queen)
            13 -> resources.getString(R.string.king)
            else -> "?"
        }
        ((view.getChildAt(0) as ConstraintLayout).getChildAt(0) as TextView).text = text
    }

    private fun checkGameState(): Boolean {
        AlertDialog.Builder(this)
                .setMessage(if (game.isWon() ?: return true) R.string.you_win else R.string.you_lose)
                .setIcon(R.drawable.cards_playing_outline)
                .setOnDismissListener { finish() }
                .show()
        updateGame()
        (deck as CardView).setCardBackgroundColor(ResourcesCompat.getColor(resources, if (game.isWon() ?: return true) R.color.card_background_correct else R.color.deck_background_default, theme))
        return false
    }

    private fun checkAndDraw() {
        if (checkGameState())
            game.getCard()
    }

    @Synchronized
    private fun onTouchForCard(numberShownOnCard: Int, view: View?) {
        when (game.getAction(game.visibleState.currentAction!!)) {
            Action.TURN_OVER -> {
                game.doTurnOver(game.visibleState.currentAction!!)
                checkAndDraw()
                updateGame()
            }
            Action.SWAP_ANY, Action.SWAP_SUIT_NUMBER -> {
                if (clickedCard == null) {
                    updateClickables(true)
                    (view as CardView).setCardBackgroundColor(ResourcesCompat.getColor(resources, R.color.card_background_moving, theme))
                    clickedCard = Pair(numberShownOnCard, view)
                } else {
                    thread(start = true) @Synchronized {
                        clickedCard?.second?.setCardBackgroundColor(ResourcesCompat.getColor(resources, R.color.card_background_default, theme))
                        if (clickedCard?.first != numberShownOnCard) {
                            if (clickedCard != null) {
                                animateSwap3(deck as CardView, clickedCard!!.second, view as CardView)
                                game.doSwap(game.visibleState.currentAction!!, game.visibleState[clickedCard!!.first]!!, game.visibleState[numberShownOnCard]!!)
                                checkAndDraw()
                            }
                        }
                        clickedCard = null
                        runOnUiThread { updateGame() }
                    }
                }
            }
            Action.SWAP_ONE -> {
                game.doSwap(game.visibleState.currentAction!!, game.visibleState[numberShownOnCard]!!)
                checkAndDraw()
                updateGame()
            }
            Action.DISCARD_CARD -> {
                checkAndDraw()
                updateGame()
            }
        }
    }

    // Don't run from UI thread! It deadlocks!
    private fun animateSwap3(actionCard: CardView, moveCard: CardView, discardCard: CardView) {
        runOnUiThread {
            moveCard.bringToFront()
            actionCard.bringToFront()
            rootView.invalidate()
        }

        val animActionX = SpringAnimation(actionCard, DynamicAnimation.X, moveCard.x)
        val animActionY = SpringAnimation(actionCard, DynamicAnimation.Y, moveCard.y)
        val animActionR = SpringAnimation(actionCard, DynamicAnimation.ROTATION, moveCard.rotation)
        val animMoveX = SpringAnimation(moveCard, DynamicAnimation.X, discardCard.x)
        val animMoveY = SpringAnimation(moveCard, DynamicAnimation.Y, discardCard.y)
        val animMoveR = SpringAnimation(moveCard, DynamicAnimation.ROTATION, discardCard.rotation)

        var finishedCount = 0
        val finishedLock = Object()

        val mainCb = {
            synchronized(finishedLock) {
                finishedCount++
                finishedLock.notify()
            }
        }

        val actionX = actionCard.x
        animActionX.addEndListener { _, _, _, _ -> mainCb() }
        val actionY = actionCard.y
        animActionY.addEndListener { _, _, _, _ -> mainCb() }
        val actionR = actionCard.rotation
        animActionR.addEndListener { _, _, _, _ -> mainCb() }
        val moveX = moveCard.x
        animMoveX.addEndListener { _, _, _, _ -> mainCb() }
        val moveY = moveCard.y
        animMoveY.addEndListener { _, _, _, _ -> mainCb() }
        val moveR = moveCard.rotation
        animMoveR.addEndListener { _, _, _, _ -> mainCb() }

        runOnUiThread {
            animActionX.start()
            animActionY.start()
            animActionR.start()
            animMoveX.start()
            animMoveY.start()
            animMoveR.start()
        }

        synchronized(finishedLock) {
            while (finishedCount < 6) {
                finishedLock.wait()
            }
        }
        runOnUiThread {
            actionCard.x = actionX
            actionCard.y = actionY
            actionCard.rotation = actionR
            moveCard.x = moveX
            moveCard.y = moveY
            moveCard.rotation = moveR
        }
    }

    private fun saveGame() {
        val json = Json(JsonConfiguration.Stable)
        val text = json.stringify(Game.serializer(), game)
        openFileOutput(saveFile, Context.MODE_PRIVATE).use {
            it.write (text.toByteArray())
        }
    }

    override fun onStop() {
        saveGame()
        super.onStop()
    }

    override fun onDestroy() {
        saveGame()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_RESUME = "resume"
        const val EXTRA_DISCARD_ON_REVEAL = "discardOnReveal"
        const val tag = "ClockworkMainActivity"
        const val saveFile = "game_save.json"
    }
}
