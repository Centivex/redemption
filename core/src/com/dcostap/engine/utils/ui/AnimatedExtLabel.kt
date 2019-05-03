package com.dcostap.engine.utils.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.dcostap.engine.utils.ifNotNull
import com.rafaskoberg.gdx.typinglabel.TypingLabel

/**
 * Created by Darius on 27/11/2017
 *
 * Like [ExtLabel], but supports fancy letter animation and many other tokens that modify the behavior.
 * Shouldn't be used if you don't need those effects since it has unsolved strange bugs.
 *
 * Supports letter by letter animation powered by rafaskb's [TypingLabel] (modified).
 * Contrary to [ExtLabel], TypingLabel provides more letter animation tricks and cool features.
 * How to use: https://github.com/rafaskb/typing-label/wiki/Tokens
 *
 * @param animateText If false, text can be animated later as many times as needed using [TypingLabel] methods
 */
open class AnimatedExtLabel @JvmOverloads constructor(text: String = "", font: BitmapFont = defaultFont!!,
                                                      color: Color? = defaultColor, alignment: Int? = null,
                                                      animateText: Boolean = false)
    : TypingLabel(text, Label.LabelStyle(font, color))
{
    var updateFunction: (delta: Float) -> Unit = {}

    /** Executes the function right away. Returning null means text will not be updated */
    var textUpdateFunction: (delta: Float) -> String? = {null}
        set(value) {field = value; updateText(0f)}

    init {
        if (alignment != null) {
            setAlignment(alignment)
        }

        if (!animateText) {
            skipToTheEnd()
        }
    }

    var fontAlpha: Float = 1f
        set(alpha) {
            style.fontColor.a = alpha
            field = alpha
        }

    fun setFontColor(newColor: Color?) {
        if (newColor != style.fontColor) {
            style.fontColor = newColor
            newColor.ifNotNull {
                style.fontColor.a = fontAlpha
            }
        }
    }

    override fun act(delta: Float) {
        super.act(delta)

        updateFunction(delta)
        updateText(delta)
    }

    private fun updateText(delta: Float) {
        textUpdateFunction(delta).ifNotNull {
            if (!text.equals(it)) {
                setText(it, true)
            }
        }
    }

    companion object {
        var defaultColor = Color.BLACK
        var defaultFont: BitmapFont? = null
    }
}
