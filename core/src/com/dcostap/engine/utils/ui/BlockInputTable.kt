package com.dcostap.engine.utils.ui

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener

/**
 * Created by Darius on 24/11/2017
 *
 * Blocks all input. Input blocked will not be propagated to Actors below or other InputProcessors
 */
open class BlockInputTable(blockInput: Boolean = true, clickedFunction: () -> Unit = {}) : ExtTable() {
    init {
        if (blockInput) this.touchable = Touchable.enabled

        addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                clickedFunction()
            }
        })
    }
}