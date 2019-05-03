package com.dcostap.engine.utils

/**
 * Created by Darius on 29/11/2017
 */
class AlphaIncreasingTexture(var maxAlpha: Double, var startingAlpha: Double) {
    private var drawingAlpha: Double = 0.toDouble()
    private var decreased = false
    private var increased = false

    init {
        this.drawingAlpha = startingAlpha
    }

    fun increaseAlpha(amountPerSecond: Float, delta: Float) {
        decreased = false
        if (increased) return
        drawingAlpha += (delta * amountPerSecond).toDouble()

        if (drawingAlpha > maxAlpha) {
            drawingAlpha = maxAlpha
            increased = true
        }
    }

    fun decreaseAlpha(amountPerSecond: Float, delta: Float) {
        increased = false
        if (decreased) return
        drawingAlpha -= (delta * amountPerSecond).toDouble()

        if (drawingAlpha < startingAlpha) {
            drawingAlpha = startingAlpha
            decreased = true
        }
    }

    fun getDrawingAlpha(): Float {
        return drawingAlpha.toFloat()
    }

    fun resetAlpha() {
        decreased = true
        increased = false

        drawingAlpha = startingAlpha
    }

}
