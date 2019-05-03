package com.dcostap.engine.utils

import com.badlogic.gdx.graphics.Color

/**
 * Created by Darius on 09/01/2018
 *
 * Setup the stuff in the constructor if you want make it flash right away
 */
class FlashingThing(private var flashColor: Color = Color.BLACK, private var flashDuration: Float = 0.2f,
                    private var startingAlpha: Float = 0.5f)
    : Updatable
{
    init {
        // if no fields are initialized on constructor, flashing won't actually happen
        this.flashColor(flashColor, flashDuration, startingAlpha)
    }

    var isFlashing = false
        private set

    private var currentFlashDuration = 0f
    private var currentAlpha = 0f

    fun flashColor(color: Color, duration: Float, startingAlpha: Float) {
        this.isFlashing = true
        this.flashColor = color
        this.flashDuration = duration
        this.currentFlashDuration = flashDuration
        this.startingAlpha = startingAlpha
        this.currentAlpha = startingAlpha
    }

    fun flashColor(color: Color, duration: Float) {
        this.flashColor(color, duration, 0.5f)
    }

    override fun update(delta: Float) {
        if (isFlashing) {
            currentFlashDuration -= delta
            currentAlpha = Utils.mapNumber(currentFlashDuration, 0f, flashDuration, 0f, startingAlpha)
            if (currentFlashDuration <= 0) {
                isFlashing = false
            }
        }
    }

    fun setupGameDrawer(gameDrawer: GameDrawer) {
        gameDrawer.alpha = currentAlpha
        gameDrawer.color = flashColor
    }

    fun resetGameDrawer(gameDrawer: GameDrawer) {
        gameDrawer.resetColor()
        gameDrawer.resetAlpha()
    }
}
