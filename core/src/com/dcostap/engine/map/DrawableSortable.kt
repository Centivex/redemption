package com.dcostap.engine.map

import com.dcostap.engine.utils.Drawable
import com.dcostap.engine.utils.GameDrawer
import com.dcostap.engine.utils.input.InputController

/** Created by Darius on 21-Aug-18.
 *
 * Something that can be sorted to specify drawing order. Depth is the most important. If 2 things have same depth then y is compared */
open interface DrawableSortable : Drawable {
    /** Higher depth = further away from the camera */
    fun getDrawingRepresentativeDepth(): Int

    /** Used to compare drawing order when depth of two drawables is equal. Lower y = drawn after */
    fun getDrawingRepresentativeY(): Float

    /** Used to compare when both depth and y position is equal */
    fun getDrawingRepresentativeYDepth(): Int

    /** Called on all visible Drawable after drawing in a [EntityTiledMap]
     * Use this for correct input handling on a EntityTiledMap: since it happens after drawing the call order will
     * correspond to the sorting order. So something that is drawn above something else will receive the input first.
     *
     * Also it is only called when the InputProcessor assigned to the [EntityTiledMap] is relayed input info, so when using
     * InputMultiplexer this won't propagate past UI elements that catch it
     *
     * @return true to handle the input and avoid any other Drawable to receive the function */
    fun handleTouchInput(inputController: InputController, stageInputController: InputController?): Boolean
}

abstract class DrawableBase : DrawableSortable {
    override fun getDrawingRepresentativeDepth(): Int {
        return 0
    }

    override fun getDrawingRepresentativeY(): Float {
        return 0f
    }

    override fun getDrawingRepresentativeYDepth(): Int {
        return 0
    }

    override fun handleTouchInput(inputController: InputController, stageInputController: InputController?): Boolean {
        return false
    }

    override fun draw(gameDrawer: GameDrawer, delta: Float) {

    }
}