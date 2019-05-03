package com.dcostap.engine.utils.ui

import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.dcostap.engine.utils.Timer
import com.dcostap.engine.utils.Utils
import ktx.actors.onChange

/**
 * Created by Darius on 10/04/2018.
 */
open class ExtButton @JvmOverloads constructor(skin: Skin, label: Label? = null, styleName: String = "default", changeListener: (() -> Unit)? = null) : Button(skin, styleName) {
    var updateFunction: (delta: Float) -> Unit = {}
    var updateFunction2: (delta: Float) -> Unit = {}

    /** Automatically enables the button if the function returns true; disables it otherwise.
     * Could be coded on the updateFunction, this is for convenience
     *
     * The updateFunction runs after this function so this might be overwritten
     *
     * Runs the provided function right away */
    var enableCondition: () -> Boolean = {!isDisabled}
        set(value) {
            field = value
            isDisabled = !value()
        }

    init {
        if (label != null) {
            add(label)
        }

        if (changeListener != null) onChange(changeListener)
    }

    override fun act(delta: Float) {
        super.act(delta)

        isDisabled = !enableCondition()

        updateFunction(delta)
        updateFunction2(delta)
    }
}

/** Button that will auto-toggle himself at a rate after being pressed some time */
open class ExtButtonAutoToggle(skin: Skin, label: Label? = null, styleName: String = "default") : ExtButton(skin, label, styleName) {
    var timePressedNeeded = 0.12f
    var autoFireRate = 0.085f

    var rateIncreaseTimeLimit = 3.1f
    var rateIncreaseTimeInit = 0.25f
    var finalRateIncrease = -0.060f

    private var finalRate = 0f

    private val pressTimer = Timer(timePressedNeeded, true, true)
    private val autoPressTimer = Timer(autoFireRate , true, false)
    private var timeAutoFiring = 0f
    private var autoPressMode = false

    override fun act(delta: Float) {
        super.act(delta)

        if (autoPressMode) {
            timeAutoFiring += delta
            finalRate = autoFireRate + Utils.mapNumber(timeAutoFiring,
                    rateIncreaseTimeInit, rateIncreaseTimeLimit, 0f, finalRateIncrease)
            autoPressTimer.timeLimit = finalRate

            if (autoPressTimer.tick(delta)) {
                setProgrammaticChangeEvents(true)
                toggle()
            }
        }

        if (isPressed) {
            if (pressTimer.tick(delta)) {
                finalRate = autoFireRate
                timeAutoFiring = 0f
                autoPressMode = true
            }
        } else {
            pressTimer.turnOn()
            pressTimer.reset()
            autoPressTimer.reset()
            autoPressMode = false
        }
    }
}
