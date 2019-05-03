package com.dcostap.engine.utils.actions

import com.badlogic.gdx.math.Interpolation
import com.dcostap.engine.utils.Timer
import com.dcostap.engine.utils.Updatable
import com.dcostap.engine.utils.Utils

/**
 * Created by Darius on 10/04/2018.
 */
class Interpolator(var interpolation: Interpolation, var duration: Float) : Updatable {
    val timer = Timer(10000000f)

    /** @return the percentage (0f to 1f) of progress, with the interpolation applied, so may go above 1f */
    fun getPercentValue(): Float {
        return interpolation.apply(Utils.mapNumber(timer.elapsed, 0f, duration, 0f, 1f))
    }

    fun getValue(minValue: Number, maxValue: Number): Float {
        return Utils.lerp(minValue, maxValue, getPercentValue(), interpolation)
    }

    override fun update(delta: Float) {
        timer.tick(delta)
    }

    fun resetElapsed() {
        timer.reset()
    }

    val hasFinished
        get() = timer.elapsed > duration

    companion object {
        /** @return the percentage (0f to 1f) of progress, with the interpolation applied, so may go above 1f */
        fun getPercentValue(interpolation: Interpolation, percentProgress: Float): Float {
            return interpolation.apply(percentProgress)
        }

        fun getValue(interpolation: Interpolation, percentProgress: Float, minValue: Number, maxValue: Number): Float {
            return ((maxValue.toFloat() - minValue.toFloat()) * getPercentValue(interpolation, percentProgress)) + minValue.toFloat()
        }
    }
}
