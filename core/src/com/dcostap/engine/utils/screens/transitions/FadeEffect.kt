package com.dcostap.engine.utils.screens.transitions

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.dcostap.Engine
import com.dcostap.engine.utils.Utils

/**
 * Created by Darius on 03/01/2018
 */
class FadeEffect(engine: Engine, private val secondsToFinishEffect: Float, private val extraSecondsAfterFadeFinished: Float,
                 private val state: TransitionScreen.TransitionState) : TransitionEffect(engine)
{
    private var alpha: Float = 0f

    init {
        alpha = when (state) {
            TransitionScreen.TransitionState.OUT -> 0f
            TransitionScreen.TransitionState.IN -> 1.2f
        }
    }

    override fun render(delta: Float) {
        viewport.apply(true)

        game.batch.projectionMatrix = viewport.camera.combined
        game.batch.begin()

        gameDrawer.color = Color.BLACK

        val finalAlpha = Utils.clamp(alpha, 0f, 1f)
        gameDrawer.alpha = finalAlpha

        gameDrawer.drawRectangle(0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat(), true, 0f)

        alpha += delta / secondsToFinishEffect * if (state == TransitionScreen.TransitionState.OUT) 1 else -1

        isFinished = state == TransitionScreen.TransitionState.IN && alpha <= 0 || state == TransitionScreen.TransitionState.OUT && alpha >= 1 + extraSecondsAfterFadeFinished

        gameDrawer.resetColor()

        game.batch.end()
    }
}
