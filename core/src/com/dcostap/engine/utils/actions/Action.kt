package com.dcostap.engine.utils.actions

import com.badlogic.gdx.math.Interpolation
import com.dcostap.engine.utils.Updatable

/**
 * Created by Darius on 15/04/2018.
 *
 * @param tag Can be used to identify an Action by name. See [ActionsUpdater.hasTag]
 */
abstract class Action @JvmOverloads constructor(duration: Float, interpolation: Interpolation = Interpolation.linear, val tag: String = ""): Updatable {
    val interpolator = Interpolator(interpolation, duration)
    var pauseTimer = false
    private var finalUpdateDone = false

    override fun update(delta: Float) {
        if (interpolator.hasFinished) {
            if (finalUpdateDone) return

            finalUpdate()
            finalUpdateDone = true
            return
        }

        if (!pauseTimer) interpolator.update(delta)
        updateProgress(interpolator.getPercentValue())
    }

    /** @param percent Progress from 0 to 1 (completed) affected by interpolation.
     * Note that interpolation is already applied, and progress may go above 1 */
    abstract fun updateProgress(percent: Float)

    /** Always runs before finishing, even if the duration is 0 */
    abstract fun finalUpdate()

    fun forceFinish() {
        interpolator.timer.elapsed = interpolator.timer.timeLimit
    }

    /** Prepares the Action to be reused, reset all variables */
    open fun reset() {
        interpolator.timer.elapsed = 0f
        finalUpdateDone = false
    }

    open val hasFinished: Boolean
        get() = interpolator.hasFinished && finalUpdateDone

    companion object {
        @JvmStatic fun run(delay: Float, function: () -> Unit) = RunAction(delay, function)
        @JvmStatic fun update(duration: Float, function: () -> Unit) = UpdateAction(duration, function)
        @JvmStatic fun run(function: () -> Unit) = RunAction(0f, function)
        @JvmStatic fun wait(delay: Float) = WaitAction(delay)
        @JvmStatic fun sequence(vararg actions: Action) = SequenceAction(*actions)
        @JvmStatic fun parallel(vararg actions: Action) = ParallelAction(*actions)
        @JvmStatic fun value(initialValue: Float, endValue: Float, duration: Float, interpolation: Interpolation,
                  tag: String = "", valueUpdated: (Float) -> Unit = {})
                = ValueAction(initialValue, endValue, duration, interpolation, tag, valueUpdated)
    }
}
