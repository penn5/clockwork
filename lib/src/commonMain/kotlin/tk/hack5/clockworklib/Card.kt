package tk.hack5.clockworklib

import kotlinx.serialization.Serializable

@Serializable
data class Card(val suit: Suit, val number: Int, var position: Int? = null)
