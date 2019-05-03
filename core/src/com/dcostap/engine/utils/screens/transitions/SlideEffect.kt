package com.dcostap.engine.utils.screens.transitions

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.dcostap.Engine

/**
 * Created by Darius on 03/01/2018
 */
class SlideEffect(engine: Engine, private val secondsToFinishEffect: Float, internal var extraSeconds: Float,
                  internal var transitionState: TransitionScreen.TransitionState) : TransitionEffect(engine)
{
    private val slidePosition = Vector2()
    private var effectFinished = false
    private var elapsedTime: Float = 0.toFloat()

    init {

        if (transitionState == TransitionScreen.TransitionState.OUT) {
            slidePosition.set(Gdx.graphics.width.toFloat(), 0f)
        } else {
            slidePosition.set(0f, 0f)
        }
    }

    override fun render(delta: Float) {
        viewport.apply(true)

        elapsedTime += delta

        effectFinished = elapsedTime >= secondsToFinishEffect

        isFinished = elapsedTime >= secondsToFinishEffect + extraSeconds

        if (!effectFinished) {
            val amount = Gdx.graphics.width / secondsToFinishEffect * delta
            slidePosition.add(-amount, 0f)
        } else {
            if (transitionState == TransitionScreen.TransitionState.OUT) {
                slidePosition.set(0f, 0f)
            } else {
                slidePosition.set((Gdx.graphics.width * 2).toFloat(), 0f)
            }
        }

        game.batch.projectionMatrix = viewport.camera.combined
        game.batch.begin()

        gameDrawer.color = Color.BLACK

        gameDrawer.drawRectangle(slidePosition.x, slidePosition.y, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat(), true, 0f)

        gameDrawer.resetColor()
        game.batch.end()
    }
}
