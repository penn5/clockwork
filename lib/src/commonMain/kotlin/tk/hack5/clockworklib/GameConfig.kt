package tk.hack5.clockworklib

import kotlinx.serialization.Serializable

/**
 * Discard on reveal:
 *
 */

@Serializable
data class GameConfig(val discardOnReveal: Boolean)
