package com.dcostap.engine.utils

import com.badlogic.gdx.math.MathUtils
import ktx.collections.GdxArray

/**
 * Created by Darius on 17/01/2018
 */
open class Animation<T> @JvmOverloads constructor(val frames: GdxArray<T>, frameDuration: Float = 1f, var animType: AnimType = AnimType.LOOP) {
    var elapsedTime = 0f
    var isPaused = false

    var totalAnimationDuration: Float = 0f
        private set



    /** -1 = deactivate */
    var stopWhenFinishedThisNumberOfLoops = -1

    enum class AnimType {
        ONE_LOOP, LOOP, REVERSED, REVERSED_LOOP, STOP_IN_EACH_NEW_FRAME
    }

    private var normalReversedCycle = AnimType.LOOP

    fun changeFrameDurationKeepingFrameIndex(frameDuration: Float) {
        val frame = frameIndex
        val progress = Utils.getDecimalPart(elapsedTime / frameDuration)
        this.frameDuration = frameDuration
        this.elapsedTime = frame * frameDuration
        this.elapsedTime += frameDuration * progress
    }

    /** Be aware that changing frameDuration while animation is running will affect the current animation frame.
     * Use [changeFrameDurationKeepingFrameIndex] instead*/
    var frameDuration: Float = frameDuration
        set(value) {
            field = value
            updateTotalAnimationDuration()
        }

    val numberOfFrames: Int
        get() = frames.size

    /** Starts counting loops since the last reset. What a loop is depends on type of animation. *(So, for example, in a
     * normalReversed animation, a loop is counted when the animation goes forward and backwards 1 time* **/
    var loopsFinished = 0

    private fun finishedOneLoop() {
        loopsFinished++

        if (loopsFinished == stopWhenFinishedThisNumberOfLoops) {
            pause()
            setToLastFrame()
        }
    }

    init {
        updateTotalAnimationDuration()
    }

    private fun updateTotalAnimationDuration() {
        this.totalAnimationDuration = frameDuration * frames.size
    }

    fun update(delta: Float) {
        if (!isPaused) {
            finishedNormalAnimation = false
            when (animType) {
                AnimType.ONE_LOOP -> {
                    elapsedTime += delta
                    if (finishedNormalAnimation()) {
                        setToLastFrame()
                        finishedOneLoop()
                        isPaused = true
                    }
                }
                AnimType.LOOP -> {
                    elapsedTime += delta
                    if (finishedNormalAnimation()) {
                        elapsedTime = 0f
                        finishedOneLoop()
                    }
                }
                AnimType.REVERSED -> {
                    elapsedTime -= delta
                    if (finishedReversedAnimation()) {
                        elapsedTime = totalAnimationDuration - 0.0001f
                        finishedOneLoop()
                    }
                }
                AnimType.REVERSED_LOOP -> {
                    if (normalReversedCycle == AnimType.LOOP) {
                        elapsedTime += delta
                        if (finishedNormalAnimation()) {
                            normalReversedCycle = AnimType.REVERSED
                            elapsedTime = totalAnimationDuration - frameDuration - 0.001f
                        }
                    } else if (normalReversedCycle == AnimType.REVERSED) {
                        elapsedTime -= delta
                        if (finishedReversedAnimation()) {
                            normalReversedCycle = AnimType.LOOP
                            elapsedTime = frameDuration
                            finishedOneLoop()
                        }
                    }
                }
                AnimType.STOP_IN_EACH_NEW_FRAME -> {
                    val previousFrame = frameIndex
                    elapsedTime += delta
                    val newFrame = frameIndex
                    if (newFrame != previousFrame) {
                        pause()
                        setFrame(newFrame)
                    }

                    if (finishedNormalAnimation()) {
                        finishedOneLoop()
                    }
                }
            }
        }
    }

    fun getFrame(): T {
        return frameFromElapsedTime
    }

    /** Whether the animation finished playing forward
     *
     *
     * More detailed: Returns true when the elapsed time is bigger than maximum elapsed time (depending on
     * number of frames and frame duration)  */
    var finishedNormalAnimation: Boolean = false
        private set

    private fun finishedNormalAnimation(): Boolean {
        return (elapsedTime >= totalAnimationDuration).also { finishedNormalAnimation = it }
    }

    /** Whether the animation finished playing backwards
     *
     *
     * More detailed: Returns true when the elapsed time is smaller than 0 */
    fun finishedReversedAnimation(): Boolean {
        return elapsedTime <= 0
    }

    private val frameFromElapsedTime: T
        get() {
            val index = frameIndex
            return frames[index]
        }

    val frameIndex: Int
        get() {
            var index = MathUtils.floor((elapsedTime / frameDuration))
            index = Math.max(index, 0)
            index = Math.min(index, frames.size - 1)
            return index
        }

    fun pause() {
        isPaused = true
    }

    fun resume() {
        isPaused = false
    }

    fun reset() {
        finishedNormalAnimation = false
        elapsedTime = 0f
        loopsFinished = 0
        isPaused = false
    }

    fun setFrame(frameIndex: Int) {
        elapsedTime = frameDuration * frameIndex
    }

    fun setToLastFrame() {
        setFrame(frames.size - 1)
    }
}
