package com.dcostap.engine.utils.ui

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Stage
import com.dcostap.engine.Assets
import com.dcostap.engine.utils.GameDrawer

/**
 * Created by Darius on 06/04/2018.
 */
open class DrawingWidget(stage: Stage): OrderedTable() {
    val gameDrawer = GameDrawer(stage.batch).also { it.customPPM = 1; it.useCustomPPM = true }
    var drawingFunction: (self: DrawingWidget) -> Unit = {}

    override fun drawBackground(batch: Batch?, parentAlpha: Float, x: Float, y: Float) {
        super.drawBackground(batch, parentAlpha, x, y)
        drawingFunction(this)
    }
}
