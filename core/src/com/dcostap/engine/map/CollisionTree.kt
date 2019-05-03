package com.dcostap.engine.map

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.GridPoint2
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.Pool
import com.dcostap.Engine
import com.dcostap.engine.map.entities.BoundingBox
import com.dcostap.engine.map.entities.Entity
import com.dcostap.engine.utils.GameDrawer
import ktx.collections.GdxArray

/**
 * Created by Darius on 20/10/2017.
 *
 * Holds entities in cells, each one with configurable size with [cellSize]
 * Cells occupy the entire provided map's size, creating as many cells as necessary to fill it
 *
 * Holds dynamic and static entities as separate groups
 * - Dynamic entities _that moved_ are removed & added on each frame. Before that, method resetDynamicEntities() must be called
 * - Static entities are added only when created, until they are removed
 * - Note: Dynamic entities can move, Static entities can't!
 *
 * All coordinates not related to arrays of cells are in game units
 */
class CollisionTree(val cellSize: Int, mapSizeX: Int, mapSizeY: Int, private val entityTiledMap: EntityTiledMap) {
    private val dynamicEntitiesThatMoved = GdxArray<Entity>()

    private val dummyGridPoint1 = GridPoint2()
    private val dummyGridPoint2 = GridPoint2()
    private val dummyTreeCellSet = GdxArray<CollisionTreeCell>()
    private val dummyEntityArray = GdxArray<Entity>()
    private val dummyEntityArray2 = GdxArray<Entity>()
    private val dummyTreeCellArray = GdxArray<CollisionTreeCell>()

    private val dynamicBBsPosition = HashMap<BoundingBox, GdxArray<CollisionTreeCell>>()
    private val addedDynamicEntities = GdxArray<Entity>()

    private val rectangleOrigin = GridPoint2()
    private val rectangleEnd = GridPoint2()

    private val collisionTreePool = CollisionTreePool()

    private var cellNumber = 0
    private val sizeX: Int = MathUtils.ceil(mapSizeX / cellSize.toFloat())
    private val sizeY: Int = MathUtils.ceil(mapSizeY / cellSize.toFloat())

    private val treeCells: GdxArray<GdxArray<CollisionTreeCell>>
    private val outsideCell = CollisionTreeCell(GridPoint2(-1, -1), true)

    private val engine get() = entityTiledMap.screen.engine

    init {
        treeCells = GdxArray(sizeX)
        for (x in 0 until sizeX) {
            val yArray = GdxArray<CollisionTreeCell>(sizeY)
            for (y in 0 until sizeY) {
                yArray.add(CollisionTreeCell(GridPoint2(x, y), false))
                cellNumber++
            }
            treeCells.add(yArray)
        }
    }

    private fun isTreeCellPositionInsideTree(x: Int, y: Int): Boolean {
        return (x >= 0 && y >= 0 && x < sizeX && y < sizeY)
    }

    /**
     * Returns list of Entities surrounding input Rectangle, based on input maxDistance
     *
     * Checks adjacent treeCells following a spiral, minimum check is a complete spiral around the first treeCell.
     * Stops searching when current spiral of cell has a maximum estimated distance >= maxDistance
     * (but includes that spiral of cells).
     *
     * Method useful when searching for closest entities. Use another method for collision-checking.
     *
     * Ignores Entities outside of the map, and returns nothing if input Entity is outside.
     *
     *
     * @return A list of entities ordered from closest to farthest
     * It's an estimation, so that first entity in the list could be farther from the third
     * But assures that in large search areas last entities are farthest, so if you loop through the array you start checking
     * the (possibly) closest ones. Also being allowed to specify the maximum distance, the array is limited in size
     *
     * Warning: Don't keep references to the returned Array, as it is reused in this object
     */
    @JvmOverloads fun getClosestEntities(rectangle: Rectangle, maxDistance: Int, boundingBoxName: String = "default"): GdxArray<Entity> {
        // get tiled position
        dummyGridPoint2.set(MathUtils.floor(rectangle.x), MathUtils.floor(rectangle.x))
        getTreeCellCoordsFromMapCellCoords(dummyGridPoint2)
        val startingCell = getTreeCellFromTreeCellCoords(dummyGridPoint2.x, dummyGridPoint2.y)

        var exit = false
        var count = 0
        val entities = dummyEntityArray
        entities.clear()

        // input rectangle is outside
        if (startingCell == outsideCell) return entities

        val position = dummyGridPoint1
        var x = 0
        var y = 0
        var amount = 1
        var sign = 1
        var yTurn = false

        // spiral loop around the start cell
        while (true) {
            position.set(startingCell.position.x + x, startingCell.position.y + y)

            // stop when you checked all cells
            if (count == cellNumber) {
                exit = true
            } else if (isTreeCellPositionInsideTree(position.x, position.y)) {
                count++

                // stop when the outer ring of cells is farther that the maxDistance
                // but, include that ring. Also always include the first ring around the starting cell
                val cycle = Math.max(Math.abs(x), Math.abs(y))
                if ((cycle - 1) * cellSize > maxDistance && cycle != 0)
                    exit = true

                for (bb in treeCells[position.x][position.y].boundingBoxes) {
                    if (bb.name.equals(boundingBoxName)) {
                        entities.add(bb.entity)
                    }
                }
            }

            if (exit) {
                return entities
            }

            // make a spiral loop
            if (yTurn)
                y += sign
            else
                x += sign

            if (!yTurn && x == sign * amount) {
                yTurn = true
            } else if (yTurn && y == sign * amount) {
                yTurn = false
                sign *= -1

                if (sign == 1)
                    amount++
            }
        }
    }

    /** Searches possible colliding Entities with bounding boxes with the name. Ignores entities without a bounding box with that name.
     * Don't keep references to the returned Array, as it is reused in this object
     *
     * Includes Entities outside of the map (if the input rectangle is outside of the map) */
    @JvmOverloads fun getPossibleCollidingEntities(rectangle: Rectangle, includeDynamicEntities: Boolean, boundingBoxesName: String = "default",
                                     filter: (Entity) -> Boolean = {true}): GdxArray<Entity>
    {
        if (includeDynamicEntities)
            updateDynamicEntitiesThatMoved()

        val entities = dummyEntityArray
        val entities2 = dummyEntityArray2

        entities.clear(); entities2.clear()

        getEntitiesFromTreeCellsOccupiedByRectangle(rectangle, boundingBoxesName, entities2, !includeDynamicEntities, false)

        for (ent in entities2) if (filter(ent)) entities.add(ent)

        return entities
    }

    private val dummyRectangle = Rectangle()

    /** Not really exact, point is replicated with a rectangle of size 1x1 pixels; point being the origin */
    @JvmOverloads fun getPossibleCollidingEntities(x: Float, y: Float, includeDynamicEntities: Boolean, boundingBoxesName: String = "default",
                                     filter: (Entity) -> Boolean = {true}): GdxArray<Entity> {
        dummyRectangle.set(x, y, 1f / Engine.PPM, 1f / Engine.PPM)
        return getPossibleCollidingEntities(dummyRectangle, includeDynamicEntities, boundingBoxesName, filter)
    }

    @JvmOverloads fun getPossibleCollidingEntities(point: Vector2, includeDynamicEntities: Boolean, boundingBoxesName: String = "default",
                                     filter: (Entity) -> Boolean = {true})
            : GdxArray<Entity> {
        return getPossibleCollidingEntities(point.x, point.y, includeDynamicEntities, boundingBoxesName, filter)
    }

    private val collEntitiesArray = GdxArray<Entity>()

    @JvmOverloads fun getCollidingEntities(rectangle: Rectangle, includeDynamicEntities: Boolean, boundingBoxesName: String = "default",
                             filter: (Entity) -> Boolean = {true}): GdxArray<Entity> {
        collEntitiesArray.clear()
        for (ent in getPossibleCollidingEntities(rectangle, includeDynamicEntities, boundingBoxesName, filter)) {
            if (rectangle.overlaps(ent.getBoundingBox(boundingBoxesName))) {
                collEntitiesArray.add(ent)
            }
        }
        return collEntitiesArray
    }

    @JvmOverloads fun getCollidingEntities(x: Float, y: Float, includeDynamicEntities: Boolean, boundingBoxesName: String = "default",
                             filter: (Entity) -> Boolean = {true}): GdxArray<Entity> {
        collEntitiesArray.clear()
        for (ent in getPossibleCollidingEntities(x, y, includeDynamicEntities, boundingBoxesName, filter)) {
            if (ent.getBoundingBox(boundingBoxesName).contains(x, y)) {
                collEntitiesArray.add(ent)
            }
        }

        return collEntitiesArray
    }

    @JvmOverloads fun getCollidingEntities(point: Vector2, includeDynamicEntities: Boolean, boundingBoxesName: String = "default",
                             filter: (Entity) -> Boolean = {true}): GdxArray<Entity> {
        return getCollidingEntities(point.x, point.y, includeDynamicEntities, boundingBoxesName, filter)
    }

    /** Searches Entities by specific bounding box with a name
     * @param boundingBoxName The name of the bounding boxes to search for. Entities without a BB with this name are ignored */
    private fun getEntitiesFromTreeCellsOccupiedByRectangle(rectangle: Rectangle, boundingBoxName: String = "default",
                                                    entityArrayToPopulate: GdxArray<Entity>,
                                                    excludeDynamicEntities: Boolean, excludeStaticEntities: Boolean)
    {
        for (treeCell in getTreeCellsOverlappedByRectangle(rectangle)) {
            for (bb in treeCell.boundingBoxes) {
                if (bb.name != boundingBoxName) continue
                if ((excludeDynamicEntities && !bb.isStatic) || (excludeStaticEntities && bb.isStatic))
                    continue

                if (entityArrayToPopulate.contains(bb.entity, true)) continue
                entityArrayToPopulate.add(bb.entity)

                // try to ignore camera checks for debug purposes
                if (Engine.DEBUG_COLLISION_TREE_UPDATES && rectangle.area() < 12) {
                    bb.entity.debugEntityFlashingThing.flashColor(Color.RED, 0.3f)
                }
            }
        }
    }

    fun addDynamicEntityThatMoved(entity: Entity) {
        dynamicEntitiesThatMoved.add(entity)
    }

    /**
     * Use to update dynamic entities that moved, to get correct collision information as Dynamic Entities can move anytime
     */
    private fun updateDynamicEntitiesThatMoved() {
        for (entity in dynamicEntitiesThatMoved) {
            addEntity(entity, true, true)
            addEntity(entity, true, false)
        }
    }

    fun resetDynamicEntities(updatedDynamicEntityList: GdxArray<Entity>) {
        val itr = addedDynamicEntities.iterator()
        while (itr.hasNext()) {
            val entity = itr.next()
            if (!entity.hasMoved() && (entity.isAddedToMap && !entity.isKilled)) continue

            entity.resetHasMoved()
            addEntity(entity, true, true)
            itr.remove()
        }

        for (ent in updatedDynamicEntityList) {
            if (ent.hasMoved() || !addedDynamicEntities.contains(ent, true)) {
                addEntity(ent, true, false)
                addedDynamicEntities.add(ent)
            }
        }

        dynamicEntitiesThatMoved.clear()
    }

    private fun addEntity(ent: Entity, isDynamic: Boolean, remove: Boolean) {
        dummyTreeCellArray.clear()

        if (isDynamic && remove) {
            removeDynamicEnt(ent)
            return
        }

        // find out which cells the Entity occupies
        for (boundingBox in ent.boundingBoxes.values()) {
            val cells = getTreeCellsOverlappedByRectangle(boundingBox.rect)
            for (treeCell in cells) {
                if (remove) {
                    treeCell.boundingBoxes.removeValue(boundingBox, true)
                } else {
                    treeCell.boundingBoxes.add(boundingBox)
                }
            }

            // only happens on add
            if (isDynamic) {
                addDynamicEntBB(cells, boundingBox)
            }
        }
    }

    private fun removeDynamicEnt(ent: Entity) {
        for (boundingBox in ent.boundingBoxes.values()) {
            // todo: make sure that commenting this apparently not needed line didn't break the collision tree :D
            //if (!dynamicBBsPosition.containsKey(boundingBox)) continue
            val bbCells = dynamicBBsPosition.get(boundingBox)
            for (cell in bbCells!!) {
                cell.boundingBoxes.removeValue(boundingBox, true)
            }

            collisionTreePool.free(bbCells)
            dynamicBBsPosition.remove(boundingBox)
        }
    }

    private fun addDynamicEntBB(cells: GdxArray<CollisionTreeCell>, boundingBox: BoundingBox) {
        val newArray = collisionTreePool.obtain()
        newArray.clear()
        newArray.addAll(cells)
        dynamicBBsPosition.put(boundingBox, newArray)
    }

    /**
     * Don't keep references to the returned Array nor its contents, as they are reused in this object
     *
     * @param rectangle Positioned and sized in game map units
     */
    private fun getTreeCellsOverlappedByRectangle(rectangle: Rectangle): GdxArray<CollisionTreeCell> {
        val returnedCells = dummyTreeCellSet
        returnedCells.clear()

        rectangleOrigin.set(rectangle.x.toInt(), rectangle.y.toInt())
        rectangleEnd.set(((rectangle.x + rectangle.width).toInt()), ((rectangle.y + rectangle.height).toInt()))

        val cell1 = getTreeCellCoordsFromMapCellCoords(rectangleOrigin)
        val cell2 = getTreeCellCoordsFromMapCellCoords(rectangleEnd)

        for (x in cell1.x..cell2.x) {
            for (y in cell1.y..cell2.y) {
                returnedCells.add(getTreeCellFromTreeCellCoords(x, y))
            }
        }

        return returnedCells
    }

    /**
     * Modifies input vector
     */
    private fun getTreeCellCoordsFromMapCellCoords(mapCellCoords: GridPoint2): GridPoint2 {
        // outside coords are all transformed to -1, -1 or sizeX, sizeY (first outside coordinate on all sides)
        // this avoids unnecessary loops if looping over all range of coords
        return mapCellCoords.set(Math.min(Math.max(-1f, (mapCellCoords.x / cellSize.toFloat())), sizeX.toFloat()).toInt(),
                Math.min(Math.max(-1f, (mapCellCoords.y / cellSize.toFloat())), sizeY.toFloat()).toInt())
    }

    private fun getTreeCellFromTreeCellCoords(treeCellX: Int, treeCellY: Int): CollisionTreeCell {
        if (isTreeCellPositionInsideTree(treeCellX, treeCellY)) {
            return treeCells[treeCellX][treeCellY]
        } else {
            return outsideCell
        }
    }

    fun addStaticEntity(ent: Entity) {
        addEntity(ent, false, false)
    }

    fun removeStaticEntity(ent: Entity) {
        addEntity(ent, false, true)
    }

    fun debugDrawCellBounds(gameDrawer: GameDrawer) {
        gameDrawer.color = Color.RED
        gameDrawer.alpha = 0.3f

        for (x in 0 until sizeX) {
            for (y in 0 until sizeY) {
                gameDrawer.drawRectangle((x * cellSize).toFloat(), (y * cellSize).toFloat(), cellSize.toFloat(), cellSize.toFloat(),
                        false, 0.06f)
            }
        }

        gameDrawer.resetColor()
        gameDrawer.resetAlpha()
    }

    class CollisionTreeCell(val position: GridPoint2, val isOutside: Boolean) {
        val boundingBoxes = GdxArray<BoundingBox>()
    }

    private class CollisionTreePool : Pool<GdxArray<CollisionTreeCell>>(10) {
        override fun newObject(): GdxArray<CollisionTreeCell> {
            return GdxArray<CollisionTreeCell>()
        }
    }
}
