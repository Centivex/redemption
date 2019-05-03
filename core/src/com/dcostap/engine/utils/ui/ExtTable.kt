package com.dcostap.engine.utils.ui

import com.badlogic.gdx.scenes.scene2d.ui.Table

/**
 * Created by Darius on 19/04/2018.
 *
 * Provides an updateFunction
 */
open class ExtTable : Table() {
    var deltaMult = 1f
    var updateFunction: (delta: Float) -> Unit = {}
    var updateFunction2: (delta: Float) -> Unit = {}

    override fun act(delta: Float) {
        val newDelta = delta * deltaMult
        super.act(newDelta)

        updateFunction(newDelta)
        updateFunction2(newDelta)
    }
}