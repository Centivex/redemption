package com.dcostap.engine.map.entities

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonValue
import com.dcostap.Engine
import com.dcostap.engine.utils.JsonSavedObject
import com.dcostap.engine.utils.Saveable
import com.dcostap.engine.utils.addChildValue
import java.lang.RuntimeException

/**
 * Created by Darius on 21/03/2018.
 *
 * Holds information about the BB's width, height and offset in [internalRect].
 *
 * [rect] represents the BB in world, adapted to offset and entity's position.
 *
 * Modifying the relative BB (the only way to modify the actual BB) raises
 * exception if it is marked as [isStatic].
 *
 * When first creating the BoundingBox, set the size first, then activate the [isStatic] flag later (this
 * avoids raising exceptions while building the BB).
 */
open class BoundingBox(val entity: Entity, @Transient val name: String) : Saveable {
    private val internalRect = Rectangle(0f, 0f, 0f, 0f)
    private val internalRectInWorld = Rectangle(0f, 0f, 0f, 0f)

    val isStatic get() = entity.isStatic

    /** The BB rectangle positioned in the world according to [internalRect] */
    val rect: Rectangle get() {
        updateAbsoluteBB()
        return internalRectInWorld
    }

    private var stopAdjustments = false

    var width get() = internalRect.width
        set(value) {
            checkIfAllowedToModify()
            internalRect.width = value
            if (!stopAdjustments) {
                adjustBBSize()
                updateAbsoluteBB()
            }
            if (value < 0) throw RuntimeException("For some reason I didn't bother to check for now, negative bb's sizes give problems")
        }
    var height get() = internalRect.height
        set(value) {
            checkIfAllowedToModify()
            internalRect.height = value
            if (!stopAdjustments) {
                adjustBBSize()
                updateAbsoluteBB()
            }
            if (value < 0) throw RuntimeException("For some reason I didn't bother to check for now, negative bb's sizes give problems")
        }
    var offsetX get() = internalRect.x
        set(value) {
            checkIfAllowedToModify()
            internalRect.x = value
            if (!stopAdjustments) {
                adjustBBSize()
                updateAbsoluteBB()
            }
        }
    var offsetY get() = internalRect.y
        set(value) {
            checkIfAllowedToModify()
            internalRect.y = value
            if (!stopAdjustments) {
                adjustBBSize()
                updateAbsoluteBB()
            }
        }

    fun modify(rectangle: Rectangle) {
        stopAdjustments = true
        width = rectangle.width
        height = rectangle.height
        offsetX = rectangle.x

        stopAdjustments = false
        offsetY = rectangle.y
    }

    /** world coordinates, when applying [offsetX] to [Entity.x] */
    val x get() = internalRectInWorld.x
    /** world coordinates, when applying [offsetY] to [Entity.y] */
    val y get() = internalRectInWorld.x

    private fun updateAbsoluteBB() {
        internalRectInWorld.x = entity.x + internalRect.x
        internalRectInWorld.y = entity.y + internalRect.y
        internalRectInWorld.width = internalRect.width
        internalRectInWorld.height = internalRect.height
    }

    /** BoundingBox size is reduced a bit to avoid BBs with size of 1 unit to occupy 2 cells, when the BB is snapped to the grid
     *
     *  (because when checking for occupied cells, if BB is at position 2, 2 with size of 1, 1; the algorithm will
     * return cells 2, 2 and (2 + 1), (2 + 1) as occupied. But BB of size 1 is normally meant to occupy 1 cell only; this
     * fixes it)  */
    private fun adjustBBSize() {
        if (internalRect.width == 0f && internalRect.height == 0f) return  // don't do it if size is 0
        val bbSizeMargin = 0.01f
        internalRect.width -= bbSizeMargin
        internalRect.height -= bbSizeMargin
    }

    private fun checkIfAllowedToModify() {
        if (isStatic && entity.isAddedToMap)
            throw UnsupportedOperationException("Tried to modify bounding box of static Entity: " + javaClass.simpleName
                    + "\nThis is unsupported as it leads to bugs")
    }

    override fun save(): JsonValue {
        val json = Json()
        return JsonSavedObject().also {
            it.addChildValue("name", name)
            it.addChildValue("internalRect", json.toJson(internalRect))
            it.addChildValue("internalRectInWorld", json.toJson(internalRectInWorld))
        }
    }

    override fun load(json: JsonValue, saveVersion: String) {
        val libgdxJson = Engine.libgdxJson
        internalRect.set(libgdxJson.fromJson(Rectangle::class.java, json.getString("internalRect")))
        internalRectInWorld.set(libgdxJson.fromJson(Rectangle::class.java, json.getString("internalRectInWorld")))
    }
}
