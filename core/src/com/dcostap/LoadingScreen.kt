package com.dcostap

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.dcostap.engine.utils.addAction
import com.dcostap.engine.utils.ui.*
import com.dcostap.game.GameScreen

/**
 * Created by Darius on 26/02/2018.
 */
class LoadingScreen(val engine: Engine) : Screen {
    init {

    }

    override fun render(delta: Float) {
        if (engine.assets.processAssetLoading()) {
            engine.screen = GameScreen(engine)
        }
    }

    override fun resize(width: Int, height: Int) {

    }

    override fun dispose() {

    }

    override fun show() {

    }

    override fun pause() {

    }

    override fun resume() {

    }

    override fun hide() {

    }
}
