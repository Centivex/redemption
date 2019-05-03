package com.dcostap.engine.utils.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.dcostap.engine.utils.Timer
import com.dcostap.engine.utils.ifNotNull
import ktx.collections.GdxMap
import java.util.regex.Pattern

/**
 * Created by Darius on 27/11/2017
 *
 * Label that doesn't use LabelStyle. Directly specify BitmapFont and its color.
 * No need to manually include into the skin all the fonts if you use this Label :D
 *
 * Supports letter by letter animation and libgdx's markup colors together. Letter by letter animation supports textwrapping
 * though with small width strange things and bugs might happen.
 *
 * Doesn't support fancy letter animation (Use [AnimatedExtLabel] for that)
 *
 * @param textUpdateDelay Will update text each x seconds. 0 means update every frame.
 */
open class ExtLabel @JvmOverloads constructor(text: String = "", font: BitmapFont = defaultFont!!,
                                              color: Color? = defaultColor, alignment: Int? = null,
                                              var textUpdateDelay: Float = 0f,
                                              textUpdateFunction: (delta: Float) -> String? = {null})
    : Label(text, Label.LabelStyle(font, color))
{
    /** Executes the function right away. Returning null means text will not be updated */
    var textUpdateFunction: (delta: Float) -> String? = textUpdateFunction
        set(value) {field = value; updateText(0f)}


    private var pauses = GdxMap<Int, Float>()

    /** Pauses markup are found, processed and erased */
    fun replacePausesMarkup() {
        var str = text.toString()
        pauses.clear()
        str.ifNotNull {
            val pattern = Pattern.compile( "(\\[(\\d+)ms])")
            var m = pattern.matcher(it)
            while (m.find()) {
                pauses.put(m.start(), m.group(2).toInt() / 1000f)
//                printDebug("original: $str")
                str = str.replaceFirst(Regex.fromLiteral(m.group(1)), "")
//                printDebug("replaced ${Regex.fromLiteral(m.group(1))} to $str")
                m = pattern.matcher(str)
            }
        }

        setText(str)
    }

    var updateFunction: (delta: Float) -> Unit = {}

    private var animatedTextIndex = 0
    private var animatedTextProgress = ""
    private var objectiveText = ""

    private var animateText = false

    private var lastPause = 0

    /** Current text will be the objective of the animation.
     * If using pauses markup you must manually call [replacePausesMarkup] to replace the text before this function */
    fun startAnimatingText(startIndex: Int = 0) {
        if (isAnimatingText) finishAnimatingText()

        animatedTextIndex = startIndex
        animateText = true
        objectiveText = text.toString()
        animatedTextProgress = ""
        animatedTextTimer.elapsed = 0f
        animatedTextTimer.timeLimit = animatedTextDelay
        lastPause = 0
        skipColorMarkupAndSpaces()
    }

    fun finishAnimatingText() {
        animateText = false
        animatedTextProgress = objectiveText
    }

    val isAnimatingText get() = animateText

    var animatedTextDelay = 0.04f
    var ignoreSpacesDelay = true

    init {
        if (alignment != null) {
            setAlignment(alignment)
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

    private fun skipColorMarkupAndSpaces() {
        // todo: if after removing the last markup still are spaces... this actually needs to be an infinite check
        // [RED]  []   <- there, a space after [] won't be skipped
        fun checkForMarkup() {
            if (objectiveText[animatedTextIndex] == '[') {
                while (animatedTextIndex < objectiveText.length && objectiveText[animatedTextIndex] != ']') {
                    animatedTextIndex++
                }
                animatedTextIndex++
            }
        }

        checkForMarkup()

        if (ignoreSpacesDelay) {
            while (animatedTextIndex < objectiveText.length && objectiveText[animatedTextIndex] == ' ') {
                animatedTextIndex++
            }
        }

        checkForMarkup()
    }

    private val animatedTextTimer = Timer()
    override fun act(delta: Float) {
        super.act(delta)

        updateFunction(delta)

        if (animateText) {
            if (animatedTextTimer.tick(delta)) {
                animatedTextIndex++

                if (animatedTextIndex >= objectiveText.length) {
                    finishAnimatingText()
                } else {
                    skipColorMarkupAndSpaces()
                    // run through all the letters between the last one and the current one and add all the pauses together
                    var pause = -1f
                    for (i in (lastPause + 1)..animatedTextIndex) {
                        val value = pauses.get(i, -1f)
                        if (value != -1f) {
                            if (pause == -1f) pause = 0f
                            pause += value
                        }
                    }
                    lastPause = animatedTextIndex

                    // fix for wrapping issue where a word will wrap but when it finishes its animation, cause size isn't complete yet
                    // add a newline before the word to simulate the newline that will happen later when the word is big enough
                    val extraString = StringBuilder() // to ignore the extra that always happens first

                    var i = animatedTextIndex

                    while (i < objectiveText.length && objectiveText[i] != ' ' && objectiveText[i] != '\n') {
                        extraString.append(objectiveText[i])
                        i++
                    }

                    animatedTextProgress = objectiveText.substring(0, animatedTextIndex)

                    // first calculate the height with original string
                    setText(animatedTextProgress)
                    invalidate()
                    val previousHeight = prefHeight
                    // then calculate the height with the word completed
                    setText(animatedTextProgress + extraString)
                    invalidate()
                    // if height was different the word causes a newline, so add a newline before to simulate it beforehand
                    if (prefHeight != previousHeight) {
                        var a = animatedTextIndex - 1
                        while (a > 0 && animatedTextProgress[a] != ' ' && animatedTextProgress[a] != '\n') {
                            a--
                        }
                        a = Math.min(a + 1, animatedTextIndex - 1)
                        val full = animatedTextProgress
                        animatedTextProgress = full.substring(0, a)
                        animatedTextProgress += "\n" + full.substring(a, full.length)
                    }

                    if (pause != -1f) {
                        animatedTextTimer.timeLimit = pause
                    } else {
                        animatedTextTimer.timeLimit = animatedTextDelay
                    }
                }
//                printDebug(animatedTextProgress)
            }
            setText(animatedTextProgress)
        } else {
            if (!ancestorsVisible()) return
            if (textUpdateDelay > 0) {
                updateDelta += delta
                if (updateDelta >= textUpdateDelay) {
                    updateDelta = 0f
                    updateText(delta)
                }
            } else
                updateText(delta)
        }
    }

    private var updateDelta = 0f

    private fun updateText(delta: Float) {
        textUpdateFunction(delta).ifNotNull {
            if (!text.equals(it)) {
                setText(it)
            }
        }
    }

    companion object {
        var defaultColor = Color.BLACK
        var defaultFont: BitmapFont? = null
    }
}
