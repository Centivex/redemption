package com.dcostap.engine.utils.ui

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import ktx.collections.*

/** Created by Darius on 01/05/2018.
 *
 * Table with custom draw methods to alter drawing order of all children. This helps to avoid extra render calls by drawing
 * all fonts / images together
 *
 * By default scene2d groups draw all their children in one go: sorting inside the group is possible
 * but sorting of children inside of child Groups is not possible. This causes render calls if many Labels of different fonts
 * and Images appear in the UI together
 *
 * OrderedTables sort all its children widgets and widgets inside nested OrderedTables and then draws them all
 * ordered by either the zIndex passed when adding that child or automatically by type (default).
 *
 * When sorting by Types it groups Images and Labels with same font together automatically.
 * When sorting manually it groups the actors with the same zIndex, provided in the new add() method
 *
 * Doesn't support transformations (rotation / scale) but it should work well with changes of position. (not sure about alpha). Also
 * children with transformations should work (Nested Tables are drawn normally)
 *
 * Drawing is more intensive than normal Tables, so use this only when it has many different children and you have high render calls number
 * */
open class OrderedTable(val automaticSorting: Boolean = true) : ExtTable() {
    private val toBeOrderedActors = GdxMap<Int, GdxArray<Actor>>()
    private val orderedActors = GdxArray<Actor>()

    private var isOrdered = true
    private var orderedSize = 0

    /** If true it will behave like a normal Table, so transformations will work again but render calls will be higher */
    var normalTableBehavior = false
    fun <T : Actor> add(actor: T, zIndex: Int = 0): Cell<T> {
        if (actor is OrderedTable) return add(actor)

        isOrdered = false
        if (!toBeOrderedActors.containsKey(zIndex)) {
            toBeOrderedActors.put(zIndex, GdxArray())
        }
        toBeOrderedActors.get(zIndex).add(actor)

        return add(actor)
    }

    private fun updateOrderedArrays() {
        if (toBeOrderedActors.isEmpty) return
        val deletedOnes = GdxArray<Actor>()
        for (actors in toBeOrderedActors.values()) {
            for (actor in actors) {
                if (!actor.hasParent()) deletedOnes.add(actor)
            }
        }

        for (actor in deletedOnes) {
            for (array in toBeOrderedActors.values()) {
                if (array.removeValue(actor, true)) break
            }
        }

        for (child in children) {
            if (child is OrderedTable) {
                child.updateOrderedArrays()
            }
        }
    }

    private fun getAllOrderedActors(orderedOutputMap: GdxMap<Int, GdxArray<Actor>>) {
        for ((key, value) in toBeOrderedActors.entries()) {
            if (!orderedOutputMap.containsKey(key)) {
                orderedOutputMap.put(key, GdxArray(value)) // create key and put a copy of the array as the new array
            } else {
                orderedOutputMap[key].addAll(value)
            }
        }

        for (actor in children) {
            if (actor is OrderedTable) {
                actor.getAllOrderedActors(orderedOutputMap)
            }
        }
    }

    private fun applyOrder() {
        orderedActors.clear()

        updateOrderedArrays()

        var toBeOrderedActorsFull = GdxMap<Int, GdxArray<Actor>>()
        getAllOrderedActors(toBeOrderedActorsFull)

        orderedActors.addAll(children)

        if (automaticSorting) {
            val newSortedMap = GdxMap<Int, GdxArray<Actor>>()
            val fonts = GdxMap<BitmapFont, Int>()
            var availableFontIndex = 2

            for (array in toBeOrderedActorsFull.values()) {
                for (actor in array) {
                    val index = when (actor) {
                        is Image -> 1
                        is ExtLabel ->
                            if (actor.style is Label.LabelStyle) {
                                if (fonts.keys().contains(actor.style.font)) {
                                    fonts[actor.style.font]
                                } else {
                                    fonts.put(actor.style.font, availableFontIndex)
                                    availableFontIndex++
                                    availableFontIndex - 1
                                }
                            } else availableFontIndex
                        else -> 0
                    }

                    if (!newSortedMap.containsKey(index)) newSortedMap[index] = GdxArray()
                    newSortedMap[index].add(actor)
                }
            }

            toBeOrderedActorsFull = newSortedMap
        }

        val indices = GdxSet<Int>()
        for (index in toBeOrderedActorsFull.keys()) {
            indices.add(index)
        }
        val i = indices.sorted()
        for (a in i) {
            val actors = toBeOrderedActorsFull.get(a)
            for (actor in actors) {
                if (orderedActors.contains(actor)) orderedActors.removeValue(actor, true)
                orderedActors.add(actor)
            }
        }

        isOrdered = true
        orderedSize = getRecursiveChildrenSize(children)
    }

    override fun draw(batch: Batch?, parentAlpha: Float) {
        if (normalTableBehavior) {
            super.draw(batch, parentAlpha)
            return
        }
        if (!isOrdered || orderedSize != getRecursiveChildrenSize(children))
            applyOrder()

        drawBackground(batch, parentAlpha, x, y)
        drawChildren(batch, parentAlpha)
    }

    private fun drawIgnoringOrderedChildren(batch: Batch?, parentAlpha: Float) {
        drawBackground(batch, parentAlpha, x, y)
        for (actor in children) {
            if (actor is OrderedTable) {
                drawChild(batch, parentAlpha, actor)
                continue
            }
            var actorIsOrdered = false
            for (array in toBeOrderedActors.values()) {
                if (array.contains(actor)) {
                    actorIsOrdered = true
                    break
                }
            }
            if (!actorIsOrdered) drawChild(batch, parentAlpha, actor)
        }
    }

    override fun drawChildren(batch: Batch?, parentAlpha: Float) {
        if (normalTableBehavior) {
            super.drawChildren(batch, parentAlpha)
            return
        }

        val offsetX = x
        val offsetY = y
        x = 0f
        y = 0f

        for (child in orderedActors) {
            if (!child.isVisible) {
                continue
            }
            val cx = child.x
            val cy = child.y
            child.x = cx + offsetX
            child.y = cy + offsetY
            if (child is OrderedTable) {
                child.drawIgnoringOrderedChildren(batch, parentAlpha)
            } else {
                if (children.contains(child))
                    child.draw(batch, parentAlpha)
                else {
                    for (actor in children) {
                        if (actor is OrderedTable) {
                            if (actor.drawChild(batch, parentAlpha, child)) break
                        }
                    }
                }
            }
            child.x = cx
            child.y = cy
        }

        x = offsetX
        y = offsetY
    }

    /** @return whether it could draw that child (invisible Actors are considered drawn) */
    private fun drawChild(batch: Batch?, parentAlpha: Float, child: Actor): Boolean {
        var succesful = false
        val offsetX = x
        val offsetY = y
        x = 0f
        y = 0f

        if (!child.isVisible) return true
        val cx = child.x
        val cy = child.y
        child.x = cx + offsetX
        child.y = cy + offsetY
        if (child is OrderedTable) {
            child.drawIgnoringOrderedChildren(batch, parentAlpha)
            succesful = true
        } else if (!children.contains(child)) {
            for (actor in children) {
                if (actor is OrderedTable) {
                    succesful = actor.drawChild(batch, parentAlpha, child)
                }
                if (succesful) break
            }
        } else {
            child.draw(batch, parentAlpha)
            succesful = true
        }
        child.x = cx
        child.y = cy
        x = offsetX
        y = offsetY

        return succesful
    }

    private fun getRecursiveChildrenSize(children: GdxArray<Actor>): Int {
        var sum = 0
        for (actor in children) {
            sum += 1
            if (actor is Group) {
                sum += getRecursiveChildrenSize(actor.children)
            }
        }
        return sum
    }
}