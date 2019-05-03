package com.dcostap.engine.utils.screens.transitions

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.dcostap.Engine

/**
 * Automatically switches from one Screen to another doing some effect in the middle
 */
class TransitionScreen(private val app: Engine, private val prevScreen: Screen, private val nextScreen: Screen,
                       private val outEffect: TransitionEffect, private val inEffect: TransitionEffect) : Screen
{
    private var currentState: TransitionState? = null

    init {
        this.currentState = TransitionState.OUT
    }

    override fun render(delta: Float) {
        when (currentState) {
            TransitionState.OUT -> renderOutTransition(delta)
            TransitionState.IN -> renderInTransition(delta)
        }
    }

    private fun renderOutTransition(delta: Float) {
        // Draw the previous screen and the effect on top of it
        prevScreen.render(delta)
        outEffect.render(delta)

        // Hide the previous screen, then prepare and show the next screen
        if (outEffect.isFinished) {
            prevScreen.hide()
            currentState = TransitionState.IN
            nextScreen.show()
            nextScreen.resize(Gdx.graphics.width, Gdx.graphics.height)
        }
    }

    private fun renderInTransition(delta: Float) {
        // Draw the next screen and the effect on top of it
        nextScreen.render(delta)
        inEffect.render(delta)

        if (inEffect.isFinished) {
            app.screen = nextScreen
        }
    }

    override fun resize(width: Int, height: Int) {
        inEffect.resize(width, height)
        outEffect.resize(width, height)
    }

    override fun pause() {}

    override fun resume() {}

    override fun show() {}

    override fun hide() {}

    override fun dispose() {}

    enum class TransitionState {
        IN, OUT
    }
}
