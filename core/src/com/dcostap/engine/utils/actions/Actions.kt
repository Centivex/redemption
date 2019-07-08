package com.dcostap.engine.utils.actions

import com.badlogic.gdx.math.Interpolation

/**
 * Created by Darius on 04/05/2018.
 *
 * Interpolates a number from one initial value to an end value during the duration of the Action.
 * Very versatile, attach a function and do whatever you want to the value
 */
class ValueAction(val startValue: Float, val endValue: Float, duration: Float,
                  interpolation: Interpolation, tag: String = "", val valueUpdated: (Float) -> Unit = {})
    : Action(duration, interpolation, tag) {

    override fun updateProgress(percent: Float, delta: Float) {
        val value = ((endValue - startValue) * percent) + startValue

        valueUpdated(value)
    }

    override fun finalUpdate() {
        valueUpdated(endValue)
    }
}

class SequenceAction(vararg actions: Action) : Action(0f) {
    val sequenceOfActions = actions

    override fun updateProgress(percent: Float, delta: Float) {

    }

    override fun finalUpdate() {

    }

    override fun reset() {
        index = 0
    }

    var index = 0

    override fun update(delta: Float) {
        if (hasFinished) return
        sequenceOfActions[index].update(delta)
        if (sequenceOfActions[index].hasFinished) index++
    }

    override val hasFinished: Boolean
        get() = index == sequenceOfActions.size
}

class ParallelAction(vararg actions: Action) : Action(0f) {
    val sequenceOfActions = actions

    override fun updateProgress(percent: Float, delta: Float) {

    }

    override fun finalUpdate() {

    }

    override fun reset() {

    }

    var finished = 0
    override fun update(delta: Float) {
        if (hasFinished) return

        finished = 0
        for (action in sequenceOfActions) {
            action.update(delta)
            if (action.hasFinished) finished++
        }
    }

    override val hasFinished: Boolean
        get() = finished == sequenceOfActions.size
}

class RunAction(delay: Float = 0f, val function: () -> Unit) : Action(delay) {
    override fun updateProgress(percent: Float, delta: Float) {

    }

    override fun finalUpdate() {
        function()
    }
}

class RunUntil(val delay: Float = 0f, val function: (Float) -> Boolean) : Action(0f) {
    override fun updateProgress(percent: Float, delta: Float) {

    }

    private var elapsed = 0f
    private var finish = false
    override fun update(delta: Float) {
        if (hasFinished) return

        if (elapsed >= delay) {
            finish = function(delta)
        } else
            elapsed += delta
    }

    override val hasFinished: Boolean
        get() = finish

    override fun finalUpdate() {

    }
}

class UpdateAction(time: Float = 0f, val function: () -> Unit) : Action(time) {
    override fun updateProgress(percent: Float, delta: Float) {
        function()
    }

    override fun finalUpdate() {
        function()
    }
}

class WaitAction(delay: Float = 0f) : Action(delay) {
    override fun updateProgress(percent: Float, delta: Float) {

    }

    override fun finalUpdate() {

    }
}