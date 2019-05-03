package com.dcostap.engine.utils.screens.transitions

import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.gdx.utils.viewport.Viewport
import com.dcostap.Engine
import com.dcostap.engine.utils.GameDrawer

abstract class TransitionEffect(protected var game: Engine) {
    var isFinished: Boolean = false
        protected set

    protected lateinit var viewport: Viewport
    protected lateinit var gameDrawer: GameDrawer

    init {
        this.isFinished = false
        create()
    }

    /**
     * Default viewport and camera creation. Override to make changes on children.
     */
    protected fun create() {
        viewport = ScreenViewport()
        gameDrawer = GameDrawer(game.batch)
    }

    fun resize(width: Int, height: Int) {
        viewport.update(width, height)
    }

    abstract fun render(delta: Float)

}
