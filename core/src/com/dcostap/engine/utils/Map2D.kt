package com.dcostap.engine.utils

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Pools
import com.badlogic.gdx.utils.viewport.Viewport
import com.dcostap.Engine
import ktx.collections.GdxArray
import ktx.collections.GdxMap

/** Created by Darius on 12-Mar-19. */
class Map2D<T> {
    val coords = GdxMap<Vector2, T>()
    private val xPos = GdxArray<Float>()
    private val yPos = GdxArray<Float>()

    private var computed = false

    fun reset() {
        computed = false
        xPos.clear()
        yPos.clear()
        coords.clear()
    }

    fun put(x: Float, y: Float, value: T?) {
        val point = Vector2(x, y)
        coords.put(point, value)

        if (!xPos.contains(x)) xPos.add(x)
        if (!yPos.contains(y)) yPos.add(y)

        computed = false
    }

    fun get(x: Float, y: Float): T? {
        val point = Pools.obtain(Vector2::class.java)
        point.set(x, y)
        val value = coords.get(point, null)
        Pools.free(point)
        return value
    }

    private fun computeNodeRelations() {
        xPos.sort { f1, f2 ->
            f1.compareTo(f2)
        }
        yPos.sort { f1, f2 ->
            f1.compareTo(f2)
        }

        computed = true
    }

    fun getTop(x: Float, y: Float): T? {
        if (!computed) computeNodeRelations()

        var i = 0
        for (thisY in yPos) {
            if (thisY == y) {
                return get(x, yPos.getOrDefault(i + 1, -99f))
            }

            i++
        }
        return null
    }

    fun getBottom(x: Float, y: Float): T? {
        if (!computed) computeNodeRelations()

        var i = 0
        for (thisY in yPos) {
            if (thisY == y) {
                return get(x, yPos.getOrDefault(i - 1, -99f))
            }

            i++
        }
        return null
    }

    fun getLeft(x: Float, y: Float): T? {
        if (!computed) computeNodeRelations()

        var i = 0
        for (thisX in xPos) {
            if (thisX == x) {
                return get(xPos.getOrDefault(i - 1, -99f), y)
            }

            i++
        }
        return null
    }

    fun getRight(x: Float, y: Float): T? {
        if (!computed) computeNodeRelations()

        var i = 0
        for (thisX in xPos) {
            if (thisX == x) {
                return get(xPos.getOrDefault(i + 1, -99f), y)
            }

            i++
        }
        return null
    }

    private val debugColor = Color(0f, 1f, 0f, 0.5f)

    /** draws the node relations. Color is 50% transparent so full relations would draw a opaque line (2 lines above each other)*/
    fun debug(worldViewport: Viewport) {
        for (node in coords.keys())
            Engine.debugUI.drawDebugRectInWorldPosition(node.x, node.y, 0.2f, 0.2f, worldViewport, color = Color.RED)

        for (node in coords.keys()) {
            if (getLeft(node.x, node.y) != null) {
                var i = 0
                var otherX = -99f
                for (thisX in xPos) {
                    if (thisX == node.x) {
                        otherX = xPos.getOrDefault(i - 1, -99f)
                    }

                    i++
                }
                if (otherX != -99f)
                    Engine.debugUI.drawDebugLineInWorldPosition(node.x, node.y, otherX, node.y, worldViewport, isArrow = true, color = debugColor)
            }

            if (getRight(node.x, node.y) != null) {
                var i = 0
                var otherX = -99f
                for (thisX in xPos) {
                    if (thisX == node.x) {
                        otherX = xPos.getOrDefault(i + 1, -99f)
                    }

                    i++
                }
                if (otherX != -99f)
                    Engine.debugUI.drawDebugLineInWorldPosition(node.x, node.y, otherX, node.y, worldViewport, isArrow = true, color = debugColor)
            }

            if (getTop(node.x, node.y) != null) {
                var i = 0
                var otherY = -99f
                for (thisY in yPos) {
                    if (thisY == node.y) {
                        otherY = yPos.getOrDefault(i + 1, -99f)
                    }

                    i++
                }
                if (otherY != -99f)
                    Engine.debugUI.drawDebugLineInWorldPosition(node.x, node.y, node.x, otherY, worldViewport, isArrow = true, color = debugColor)
            }

            if (getBottom(node.x, node.y) != null) {
                var i = 0
                var otherY = -99f
                for (thisY in yPos) {
                    if (thisY == node.y) {
                        otherY = yPos.getOrDefault(i - 1, -99f)
                    }

                    i++
                }
                if (otherY != -99f)
                    Engine.debugUI.drawDebugLineInWorldPosition(node.x, node.y, node.x, otherY, worldViewport, isArrow = true, color = debugColor)
            }
        }
    }
}