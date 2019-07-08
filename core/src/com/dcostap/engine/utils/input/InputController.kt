package com.dcostap.engine.utils.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.viewport.Viewport
import com.dcostap.engine.utils.Updatable

/**
 * Created by Darius on 19/11/2017.
 *
 * Notifies adapted touch events, with input coordinates projected to one Viewport. Has functions for polling too.
 *
 * (as of now, it is recommended to use polling due to unsupported stuff with desktop mouse buttons on listener's methods)
 *
 * Input notified (touch down and released) is projected into the Viewport's coords, and also screen raw coords (Y coordinate
 * is inverted so screen coords origin starts on bottom-left corner)
 *
 * You can also get the current mouse position (for desktop) with [mousePos]
 *
 * **Must be set as InputProcessor.**
 */
class InputController(private val worldViewport: Viewport) : InputAdapter(), Updatable {
    private var dummyVector = Vector2()

//    private val listeners = Array<InputListener>()

    /** All registered touches. A literal finger touch in mobile or a click in desktop.
     * Key represents the pointer. Each pressed finger in mobile has +1 pointer. In desktop 0 for left click and -1 for right click
     * A touch is registered when it is pressed down, and erased when released. */
    val touches = ObjectMap<Int, Touch>()

    /** Returns the Touch in [touches] representing the specified desktop button.
     * Keep in mind Touches exist from justPressed to a frame after justReleased.
     *
     * For left click button, in mobile it will return the first finger touch.
     * For the rest of the buttons, in mobile the Touch will never exist */
    fun touchForButton(button: Int): Touch? {
        return touches.get(button, null)
    }

    /** only for desktop */
    val mousePos = Mouse()

    /** Button is a desktop button ([Input.Buttons]) or a pointer in mobile if [forMobileMultiTouch] is true. Left button / first touch is the default value */
    fun isPressed(button: Int = Input.Buttons.LEFT, forMobileMultiTouch: Boolean = false): Boolean {
        val pointer = if (forMobileMultiTouch) button else pointerAssignedForButton(button)
        return touches.run { containsKey(pointer) && !touches[pointer].isJustReleased}
    }

    fun pointerAssignedForButton(button: Int): Int {
        return when (button) {
            Input.Buttons.LEFT -> 0
            Input.Buttons.RIGHT -> -1
            Input.Buttons.MIDDLE -> -2
            Input.Buttons.BACK -> -3
            Input.Buttons.FORWARD -> -4
            else -> -5
        }
    }

    /** Button is a desktop button ([Input.Buttons]) or a pointer in mobile if [forMobileMultiTouch] is true. Left button / first touch is the default value */
    fun isJustPressed(button: Int = Input.Buttons.LEFT, forMobileMultiTouch: Boolean = false): Boolean {
        val pointer = if (forMobileMultiTouch) button else pointerAssignedForButton(button)
        return touches.containsKey(pointer) && touches[pointer].isJustPressed
    }

    /** Button is a desktop button ([Input.Buttons]) or a pointer in mobile if [forMobileMultiTouch] is true. Left button / first touch is the default value */
    fun isJustReleased(button: Int = Input.Buttons.LEFT, forMobileMultiTouch: Boolean = false): Boolean {
        val pointer = if (forMobileMultiTouch) button else pointerAssignedForButton(button)
        return touches.containsKey(pointer) && touches[pointer].isJustReleased
    }

    private var scrolled = 0
    private var wasScrolled = false

    fun mouseWheelScrolled(): Int {
        return scrolled
    }

    private val tmpVector = Vector2()
    fun draggedDistanceScreen(button: Int = Input.Buttons.LEFT, forMobileMultiTouch: Boolean = false): Vector2 {
        if (forMobileMultiTouch) {
            if (!touches.containsKey(button)) return tmpVector.also { it.set(0f, 0f) }
            val touch = touches[button]
            tmpVector.set(touch.screenX - touch.startScreenX, touch.screenY - touch.startScreenY)
            return tmpVector
        } else {
            if (!touches.containsKey(button)) return tmpVector.also { it.set(0f, 0f) }
            val touch = touches[button]
            tmpVector.set(mousePos.screen.x - touch.startScreenX, mousePos.screen.y - touch.startScreenY)
            return tmpVector
        }
    }

    fun draggedDistanceWorld(button: Int = Input.Buttons.LEFT, forMobileMultiTouch: Boolean = false): Vector2 {
        if (forMobileMultiTouch) {
            if (!touches.containsKey(button)) return tmpVector.also { it.set(0f, 0f) }
            val touch = touches[button]
            tmpVector.set(touch.worldX - touch.startWorldX, touch.worldY - touch.startWorldY)
            return tmpVector
        } else {
            if (!touches.containsKey(button)) return tmpVector.also { it.set(0f, 0f) }
            val touch = touches[button]
            tmpVector.set(mousePos.world.x - touch.startWorldX, mousePos.world.y - touch.startWorldY)
            return tmpVector
        }
    }

    /** Listeners are notified on this method. */
    override fun update(delta: Float) {
        var pointer = 0

        if (scrolled != 0) {
            if (wasScrolled) {
                scrolled = 0
                wasScrolled = false
            } else
                wasScrolled = true
        }

        for (entry in touches) {
            // reset justPressed flag. Used wasJustPressed flag to delay the resetting so that value isn't erased on
            // same frame, for other classes using polling
            if (entry.value.isJustPressed) {
                if (entry.value.wasJustPressed) {
                    entry.value.wasJustPressed = false
                    entry.value.isJustPressed = false
                } else
                    entry.value.wasJustPressed = true
            }

            if (entry.value.isJustReleased) {
                if (entry.value.wasJustReleased) {
                    touches.remove(entry.key)
                } else
                    entry.value.wasJustReleased = true
            }

//            if (!entry.value.isJustReleased) {
//                for (listener in listeners) {
//                    listener.touchDownEvent(entry.value.screenX, entry.value.screenY, entry.value.worldX, entry.value.worldY,
//                            entry.key, entry.value.isJustPressed)
//                }
//            }

            pointer++
        }

        // update mouse position
        mousePos.screen.set(Gdx.input.x.toFloat(), Gdx.graphics.height - Gdx.input.y.toFloat())

        dummyVector.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
        dummyVector = screenToViewportCoordinates(dummyVector)

        mousePos.world.set(dummyVector.x, dummyVector.y)
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        dummyVector.set(screenX.toFloat(), screenY.toFloat())
        dummyVector = screenToViewportCoordinates(dummyVector)

        //System.out.printDebug("INPUT CONTROLLER: TOUCHDOWN RECEIVED, listeners number: " + listeners.size);

        val p = if (button == Input.Buttons.LEFT) pointer else pointerAssignedForButton(button)

        touches.put(p, Touch(screenX.toFloat(), (Gdx.graphics.height - screenY).toFloat(),
                dummyVector.x, dummyVector.y, true))

        return false
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        dummyVector.set(screenX.toFloat(), screenY.toFloat())
        dummyVector = screenToViewportCoordinates(dummyVector)

        val p = if (button == Input.Buttons.LEFT) pointer else pointerAssignedForButton(button)
        if (touches.containsKey(p))
            touches[p].isJustReleased = true

//        for (listener in listeners) {
//            listener.touchReleasedEvent(screenX.toFloat(), (Gdx.graphics.height - screenY).toFloat(),
//                    dummyVector.x, dummyVector.y, button, p)
//        }

        return false
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        dummyVector.set(screenX.toFloat(), screenY.toFloat())
        dummyVector = screenToViewportCoordinates(dummyVector)

        val touch = touches.get(0) ?: return false

        touch.screenX = screenX.toFloat()
        touch.screenY = (Gdx.graphics.height - screenY).toFloat()
        touch.worldX = dummyVector.x
        touch.worldY = dummyVector.y

        return false
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        return false
    }

    /** Warning: Modifies input vector */
    private fun screenToViewportCoordinates(screenCoords: Vector2): Vector2 {
        worldViewport.unproject(screenCoords)
        return screenCoords
    }

//    fun registerListener(listener: InputListener) {
//        listeners.add(listener)
//    }
//
//    fun removeListener(listener: InputListener) {
//        if (listeners.contains(listener, false))
//            listeners.removeValue(listener, false)
//    }

    override fun scrolled(amount: Int): Boolean {
        scrolled = amount
        return false
    }

    /** For desktop button pressed, position isn't updated. Use [InputController.mousePos] */
    data class Touch(var screenX: Float, var screenY: Float, var worldX: Float, var worldY: Float,
                     var isJustPressed: Boolean, var wasJustPressed: Boolean = false, var isJustReleased: Boolean = false,
                     var wasJustReleased: Boolean = false) {
        val startScreenX: Float = screenX; val startScreenY: Float = screenY
        val startWorldX: Float = worldX; val startWorldY: Float = worldY
    }

    class Mouse {
        val world = Vector2()
        val screen = Vector2()
    }
}

//interface InputListener {
//    /**
//     * **WARNING: you can't distinguish mouse buttons in this method, because it is unsupported**
//     *
//     * *(mouse buttons share the same pointer, so [Touch.button] behaves erratically when more than one of those buttons are pressed)*
//     *
//     * *If you wanna know which mouse button is pressed / just pressed, use [InputController.isPressed] instead*
//     *
//     * Called when the touch is first pressed until it is released
//     *
//     * @param screenX with origin on bottom-left
//     * @param screenY with origin on bottom-left
//     * @param pointer ID of the finger pressed; always 0 if it's a mouse in desktop
//     * Various events can be issued at the same time, one for each pointer;
//     * ignore pointers other than 0 if you don't want to potentially have repeated calls of the same method
//     */
//    fun touchDownEvent(screenX: Float, screenY: Float, worldX: Float, worldY: Float, pointer: Int, isJustPressed: Boolean)
//
//    /**
//     * @see touchDownEvent
//     */
//    fun touchReleasedEvent(screenX: Float, screenY: Float, worldX: Float, worldY: Float, button: Int, pointer: Int)
//}
