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

    private val listeners = Array<InputListener>()

    /** All registered touches. A literal finger touch in mobile or a click in desktop.
     * Key represents the pointer. Each pressed finger in mobile has +1 pointer. Always 0 in desktop
     * A touch is registered when it is pressed down, and erased when released.
     * Don't use this for distinguishing mouse buttons, it will lead to errors. (mouse buttons share the same
     * pointer, so [Touch.button] behaves erratically when more than one of those buttons are pressed)*/
    val touches = ObjectMap<Int, Touch>()

    /** only for desktop */
    val mousePos = Mouse()

    /** for desktop and mobile */
    fun isTouchPressed(button: Int = Input.Buttons.LEFT, pointer: Int = 0): Boolean {
        return if (pointer == 0)
            Gdx.input.isButtonPressed(button)
        else
            touches.containsKey(pointer)
    }

    /** for desktop and mobile */
    fun isTouchJustPressed(button: Int = Input.Buttons.LEFT, pointer: Int = 0): Boolean {
        return touches.containsKey(pointer) && touches[pointer].button == button && touches[pointer].isJustPressed
    }

    /** for desktop and mobile */
    fun isTouchJustReleased(button: Int = Input.Buttons.LEFT, pointer: Int = 0): Boolean {
        return touches.containsKey(pointer) && touches[pointer].button == button && touches[pointer].isJustReleased
    }

    private var scrolled = 0
    private var wasScrolled = false

    fun mouseWheelScrolled(): Int {
        return scrolled
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

            if (!entry.value.isJustReleased) {
                for (listener in listeners) {
                    listener.touchDownEvent(entry.value.screenX, entry.value.screenY, entry.value.worldX, entry.value.worldY,
                            entry.key, entry.value.isJustPressed)
                }
            }

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

        touches.put(pointer, Touch(screenX.toFloat(), (Gdx.graphics.height - screenY).toFloat(),
                dummyVector.x, dummyVector.y, button, true))

        return false
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        dummyVector.set(screenX.toFloat(), screenY.toFloat())
        dummyVector = screenToViewportCoordinates(dummyVector)

        if (touches.containsKey(pointer))
            touches[pointer].isJustReleased = true

        for (listener in listeners) {
            listener.touchReleasedEvent(screenX.toFloat(), (Gdx.graphics.height - screenY).toFloat(),
                    dummyVector.x, dummyVector.y, button, pointer)
        }

        return false
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        return false
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        dummyVector.set(screenX.toFloat(), screenY.toFloat())
        dummyVector = screenToViewportCoordinates(dummyVector)

        // the check for null avoids it, but still... dunno why it happened
        val touch = touches.get(0) ?: return false

        touch.screenX = screenX.toFloat()
        touch.screenY = (Gdx.graphics.height - screenY).toFloat()
        touch.worldX = dummyVector.x
        touch.worldY = dummyVector.y

        return false
    }

    /** Warning: Modifies input vector  */
    fun screenToViewportCoordinates(screenCoords: Vector2): Vector2 {
        worldViewport.unproject(screenCoords)
        return screenCoords
    }

    fun registerListener(listener: InputListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: InputListener) {
        if (listeners.contains(listener, false))
            listeners.removeValue(listener, false)
    }

    override fun scrolled(amount: Int): Boolean {
        scrolled = amount
        return false
    }
}

interface InputListener {
    /**
     * **WARNING: you can't distinguish mouse buttons in this method, because it is unsupported**
     *
     * *(mouse buttons share the same pointer, so [Touch.button] behaves erratically when more than one of those buttons are pressed)*
     *
     * *If you wanna know which mouse button is pressed / just pressed, use [InputController.isTouchPressed] instead*
     *
     * Called when the touch is first pressed until it is released
     *
     * @param screenX with origin on bottom-left
     * @param screenY with origin on bottom-left
     * @param pointer ID of the finger pressed; always 0 if it's a mouse in desktop
     * Various events can be issued at the same time, one for each pointer;
     * ignore pointers other than 0 if you don't want to potentially have repeated calls of the same method
     */
    fun touchDownEvent(screenX: Float, screenY: Float, worldX: Float, worldY: Float, pointer: Int, isJustPressed: Boolean)

    /**
     * @see touchDownEvent
     */
    fun touchReleasedEvent(screenX: Float, screenY: Float, worldX: Float, worldY: Float, button: Int, pointer: Int)
}

data class Touch(var screenX: Float, var screenY: Float, var worldX: Float, var worldY: Float,
                 var button: Int, var isJustPressed: Boolean, var wasJustPressed: Boolean = false, var isJustReleased: Boolean = false,
                 var wasJustReleased: Boolean = false)

class Mouse {
    val world = Vector2()
    val screen = Vector2()
}
