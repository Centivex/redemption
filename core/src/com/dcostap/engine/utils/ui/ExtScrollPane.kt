package com.dcostap.engine.utils.ui

import com.badlogic.gdx.Input
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener

/** Custom ScrollPane with more options and some fixes
 * @param avoidShrinkOnHeight Also disables the scrolling bar on Y
 * @param avoidShrinkOnWidth Also disables the scrolling bar on X
 * @param optionsForComputerScrolling if true, prevents over-scrolling, fading of bars and scrolling clicking in
 * the contents
 * @param scrollWhenMouseIsOver will allow the Pane to catch mouse wheel input to scroll, even if not on focus
*/
open class ExtScrollPane(widget: Actor, skin: Skin, styleName: String = "default", private val scrollWhenMouseIsOver: Boolean = true,
                         private val avoidShrinkOnHeight: Boolean = false, private val avoidShrinkOnWidth: Boolean = false,
                         optionsForComputerScrolling: Boolean = true)
    : ScrollPane(widget, skin, styleName)
{
    private val clickListener: ClickListener

    var updateFunction: (delta: Float) -> Unit = {}

    init {
        clickListener = ClickListener(Input.Buttons.LEFT)
        addListener(clickListener)

        if (optionsForComputerScrolling) {
            this.setOverscroll(false, false)
            this.setFlickScroll(false)
            this.fadeScrollBars = false
        }

        this.setScrollingDisabled(avoidShrinkOnWidth, avoidShrinkOnHeight)

        // fix for when a Slider is inside the ScrollPane
        // pane won't move when you touch the slider
        addCaptureListener(object : InputListener() {
            override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                val actor = this@ExtScrollPane.hit(x, y, true)
                if (actor is Slider) {
                    this@ExtScrollPane.setFlickScroll(false)
                    return true
                }

                return super.touchDown(event, x, y, pointer, button)
            }

            override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {
                this@ExtScrollPane.setFlickScroll(true)
                super.touchUp(event, x, y, pointer, button)
            }
        })
    }

    override fun act(delta: Float) {
        super.act(delta)

		if (stage == null) return
        if (scrollWhenMouseIsOver) {
            if (clickListener.isOver) {
                stage.scrollFocus = this
            } else if (stage.scrollFocus === this) {
                stage.scrollFocus = null
            }
        }

        updateFunction(delta)
    }

    /** by default it returns 0 in getMinHeight(). This causes the widget to shrink completely when it can't expand
     * These booleans override the behavior  */
    override fun getMinHeight(): Float {
        return if (!avoidShrinkOnHeight)
            super.getMinHeight()
        else
            prefHeight
    }

    /** @see .getMinHeight
     */
    override fun getMinWidth(): Float {
        return if (!avoidShrinkOnWidth)
            super.getMinWidth()
        else
            prefWidth
    }
}