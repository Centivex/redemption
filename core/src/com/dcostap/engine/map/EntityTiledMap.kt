package com.dcostap.engine.map

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.GridPoint2
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.*
import com.badlogic.gdx.utils.Array
import com.dcostap.Engine
import com.dcostap.engine.map.entities.Entity
import com.dcostap.engine.map.map_loading.*
import com.dcostap.engine.utils.*
import com.dcostap.engine.utils.input.InputController
import com.dcostap.engine.utils.screens.BaseScreen
import com.dcostap.printDebug
import ktx.collections.*
import java.util.*
import kotlin.collections.HashMap

/**
 * Created by Darius on 14/09/2017.
 *
 * After creating the map object, call [initMap] to load basic variables
 *
 * When drawing, Entities and Tiles will be sorted according to their depth or y variables. (see [DrawableSortable])
 * Each mapCell may have more than one Tile. See [MapCell.tiles] for info on how tiles (graphical info on each cell) work
 *
 * For saving map info there are two approaches (you can choose what to save in [save], also see [load]):
 * - Raw save: save everything: all Entities, cells, properties, etc the way they are when doing the saving. Useful for complex maps with
 * many arbitrary Entities
 * - Custom Properties save: (from a json file) ignore Entities and cells, and only save properties in [save].
 * When you load the map, using JsonMapLoader, pass the previously saved info to its JsonMapLoader.
 * Useful for pre-designed maps (with Tiled for example), and maps that
 * don't change a lot (or changes are scripted, see JsonMapLoader and the interaction with groups of layers in Tiled).
 *
 * @param inputController If not null, will be used to call [DrawableSortable.handleTouchInput] on Entities and Cells
 * @param entityDeactivationBorder    In world map units, border applied to each edge of the camera view to
 * form the deactivation Rectangle. Entities outside that rectangle will not be updated
 */
open class EntityTiledMap @JvmOverloads constructor(val screen: BaseScreen,
                                                    val stageInputController: InputController? = null,
                                                    private val deactivateEntities: Boolean = false,
                                                    private val entityDeactivationBorder: Int = 0,
                                                    var ignoreTileDrawing: Boolean = false)
    : Drawable, Updatable, Disposable
{
    companion object {
        const val defaultCollTreeCellSize = 15
    }

    val worldViewport get() = screen.worldViewport
    private val worldCamera: OrthographicCamera = worldViewport.camera as OrthographicCamera
    val inputController: InputController get() = screen.inputController

    val entityList = Array<Entity>()
    private val dynamicEntityList = Array<Entity>()
    private val dynamicEntityListDraw = Array<Entity>()
    private val toBeRemoved = Array<Entity>()
    private val toBeAdded = Array<Entity>()

    var customProperties = CustomProperties()

    private val drawingCameraBoundsBorder = 2
    private lateinit var mapCells: HashMap<GridPoint2, MapCell>
    var width: Int = 0
        private set
    var height: Int = 0
        private set

    /** This collTree may not include some entities if [Entity.providesCollidingInfo] is false, so in that case you can selectively
     * ignore Entities that will never be useful as colliders for others. */
    lateinit var collisionTreeForEntityColliding: CollisionTree
        private set

    /** This collTree is used internally by the map for culling and drawing, and includes all Entities */
    lateinit var collisionTree: CollisionTree

    private val dummyGridPoint = GridPoint2()
    private val dummyCellArray = Array<MapCell>()
    private val dummyEntityArray = Array<Entity>()

    private val cameraRectangle = Rectangle()
    private val deactivationRectangle = Rectangle()

    var isInitiated = false
        private set

    val endOfFrameNotifier = Notifier<EndOfFrameListener>()
    val startOfFrameNotifier = Notifier<StartOfFrameListener>()

    val extraMapDrawables = GdxArray<DrawableSortable>()
    private val frameDrawOrders = GdxArray<PoolableDrawable>()

    private class PoolableDrawable : DrawableBase(), Pool.Poolable {
        var draw: (GameDrawer) -> Unit = {}
        var depth = 0
        var drawOrderY = 0f
        var yDepth: Int = 0

        override fun reset() {
            draw = {}
            depth = 0
            drawOrderY = 0f
            yDepth = 0
        }

        override fun getDrawingRepresentativeY(): Float {
            return drawOrderY
        }

        override fun getDrawingRepresentativeDepth(): Int {
            return depth
        }

        override fun getDrawingRepresentativeYDepth(): Int {
            return yDepth
        }

        override fun draw(gameDrawer: GameDrawer, delta: Float) {
            draw(gameDrawer)

            Pools.free(this)
        }
    }

    init {
        Pools.set(PoolableDrawable::class.java, object : Pool<PoolableDrawable>(5) {
            override fun newObject(): PoolableDrawable {
                return PoolableDrawable()
            }
        })
    }

    /** Easy way to draw something with custom depth / drawingY ([DrawableSortable.getDrawingRepresentativeY]) this frame. Alternative to using [extraMapDrawables]*/
    fun drawThisFrame(depth: Int = 0, drawOrderY: Float = 0f, yDepth: Int = 0, draw: (GameDrawer) -> Unit) {
        frameDrawOrders.add(Pools.obtain(PoolableDrawable::class.java).also { it.depth = depth; it.draw = draw; it.drawOrderY = drawOrderY; it.yDepth = yDepth })
    }

    /** avoids this entity from being not-drawn when its bounding box isn't inside the camera.
     * It might be a bit expensive for maps with many entities */
    private val noCulling = GdxArray<Entity>()

    /** Call after creating map! */
    open fun initMap(mapWidth: Int, mapHeight: Int, collisionTreeCellSize: Int = defaultCollTreeCellSize) {
        if (isInitiated)
            throw RuntimeException("Tried to initiate a map that was already initiated.")

        mapCells = HashMap(mapWidth * mapHeight)

        for (x in 0 until mapWidth) {
            for (y in 0 until mapHeight) {
                mapCells.put(GridPoint2(x, y), MapCell(x, y, 1, this))
            }
        }

        this.width = mapWidth
        this.height = mapHeight

        this.collisionTreeForEntityColliding = CollisionTree(collisionTreeCellSize, mapWidth, mapHeight, this)
        this.collisionTree = CollisionTree(collisionTreeCellSize, mapWidth, mapHeight, this)

        isInitiated = true
    }

    open fun appendLoadedMap(mapInfo: JsonMapLoader, originX: Int, originY: Int, entityLoaderFromString: EntityLoaderFromString? = null,
                             tileLoaderFromString: TileLoaderFromString? = null, savedMapInfoJson: JsonValue? = null) {
        mapInfo.appendToLoadedMap(this, originX, originY, entityLoaderFromString, tileLoaderFromString, savedMapInfoJson)
    }

    open fun getMapCell(position: GridPoint2): MapCell {
        if (!isInsideMap(position.x.toFloat(), position.y.toFloat()))
            throw IllegalArgumentException("cell position: $position is outside of map!")
        return mapCells.get(position)!!
    }

    open fun getMapCell(x: Int, y: Int): MapCell {
        return getMapCell(dummyGridPoint.set(x, y))
    }

    private var maxUsedIndex = 0L
        set(value) {
            if (value < -1L) { // avoid overflow
                field = 0L // dummy value in case it's in the middle of a floodFill
                didFullFloodFill = false // reset it in update
            } else {
                field = value
            }
        }

    private fun floodFillFull() {
        val unexploredCells = GdxMap<Int, MapCell>()
        var indexFill = 0L

        fun indexToX(index: Int): Int {
            return index % width
        }

        fun indexToY(index: Int): Int {
            return MathUtils.floor(index / width.toFloat())
        }

        fun xyToIndex(x: Int, y: Int): Int {
            return x + (y * width)
        }

        val floodList = GdxArray<MapCell>()
        var originCell: MapCell? = null
        fun flood(x: Int, y: Int) {
            val cell = getMapCell(x, y)
            if (cell.floodRegion != originCell!!.floodRegion) return
            if (cell.pathfindingFloodIndex != -1L) return
            if (cell.isSolid != originCell!!.isSolid) return

            unexploredCells.remove(xyToIndex(x, y))

            cell.pathfindingFloodIndex = indexFill

            if (isInsideMap(x, y + 1))
                floodList.add(getMapCell(x + 0, y + 1))

            if (isInsideMap(x + 1, y))
                floodList.add(getMapCell(x + 1, y + 0))

            if (isInsideMap(x - 1, y))
                floodList.add(getMapCell(x - 1, y + 0))

            if (isInsideMap(x, y - 1))
                floodList.add(getMapCell(x + 0, y - 1))
        }

        for (i in 0 until width * height) {
            val cell = getMapCell(indexToX(i), indexToY(i))
            cell.floodRegion = MathUtils.floor(indexToX(i) / floodRegionSize.toFloat()) +
                    MathUtils.floor(indexToY(i) / floodRegionSize.toFloat()) * (MathUtils.floor(width / floodRegionSize.toFloat()) + 1)
            cell.pathfindingFloodIndex = -1L
            unexploredCells.put(i, cell)
        }

        while (!unexploredCells.isEmpty) {
            val chosen = unexploredCells.first()
            originCell = chosen.value

            floodList.add(chosen.value)
            while (!floodList.isEmpty) {
                val cell = floodList.pop()
                flood(cell.x, cell.y)
            }

            maxUsedIndex = indexFill
            indexFill++
        }
    }

    fun floodFill(motherCell: MapCell) {
        if (!didFullFloodFill) return
        if (!doFloodFillForSolidCellsPathfinding) return

        var index = 0L
        maxUsedIndex++
        val startingMaxIndex = maxUsedIndex

        var originIsSolid = false
        var originRegion = -1
        val floodList = GdxArray<MapCell>()

        fun flood(x: Int, y: Int) {
            val thisCell = getMapCell(x, y)
            if (originRegion != thisCell.floodRegion) return
            if (thisCell.pathfindingFloodIndex == index) return
            if (thisCell.pathfindingFloodIndex > startingMaxIndex) return
            if (originIsSolid != thisCell.isSolid) return

            if (Engine.DEBUG_CELL_FLOOD_FILL)
                thisCell.debugFlashingRect.flashColor(Color.RED, 0.4f)

            thisCell.pathfindingFloodIndex = index

            if (isInsideMap(x, y + 1))
                floodList.add(getMapCell(x + 0, y + 1))

            if (isInsideMap(x + 1, y))
                floodList.add(getMapCell(x + 1, y + 0))

            if (isInsideMap(x - 1, y))
                floodList.add(getMapCell(x - 1, y + 0))

            if (isInsideMap(x, y - 1))
                floodList.add(getMapCell(x + 0, y - 1))
        }

        val x = motherCell.x; val y = motherCell.y

        fun floodStart(x: Int, y: Int) {
            maxUsedIndex++
            index = maxUsedIndex

            if (isInsideMap(x, y)) {
                val cell = getMapCell(x, y)
                if (motherCell.floodRegion == cell.floodRegion) {
                    floodList.add(cell)
                    originIsSolid = cell.isSolid
                    originRegion = cell.floodRegion
                }
            }

            while (!floodList.isEmpty) {
                val cell = floodList.pop()
                flood(cell.x, cell.y)
            }
        }

        motherCell.pathfindingFloodIndex = maxUsedIndex
        floodStart(x + 0, y + 1)
        floodStart(x + 1, y + 0)
        floodStart(x + 0, y - 1)
        floodStart(x - 1, y - 0)
    }

    private var wasUpdated = false

    override fun update(delta: Float) {
        wasUpdated = true

        if (!didFullFloodFill && doFloodFillForSolidCellsPathfinding) {
            floodFillFull()
            didFullFloodFill = true
        }

        updateCameraRectangle()
        removeAndAddEntities()

        startOfFrameNotifier.notifyListeners { it.startOfFrameUpdate(delta) }

        dummyEntityArray.clear()
        if (deactivateEntities) {
            dummyEntityArray.addAll(collisionTree.getPossibleCollidingEntities(getDeactivationRectangle(), true))
            dummyEntityArray.addAll(noCulling)
        } else
            dummyEntityArray.addAll(entityList)

//        if (updateFloodCells && doFloodFillForSolidCellsPathfinding) {
//            updateFloodCells = false
//            floodFillFull()
//        }

        // update entities
        for (ent in dummyEntityArray) {
            ent.update(delta)

            if (!ent.isStatic && ent.hasMoved()) {
                if (ent.providesCollidingInfo)
                    collisionTreeForEntityColliding.addDynamicEntityThatMoved(ent)

                if (ent.providesCullingInfo)
                    collisionTree.addDynamicEntityThatMoved(ent)
            }
        }
    }

    /** If true, whenever there is a change in solid state in any cell, [floodFill] once in current or next frame.
     * Also, [floodFillFull] (performed on all cells of the map) is called at the start of
     * @see MapCell.pathfindingFloodIndex
     * @see floodRegionSize */
    var doFloodFillForSolidCellsPathfinding = true
        set(value) {
            if (field != value && value) {
                didFullFloodFill = false
            }

            field = value
        }

    /** Limits flood fill algorithm to regions, so that changes in a cell's solid will have to update at most this number ^ 2.
     * This helps with performance in huge maps (nothing cycles through all tiles of the map).
     * This all at the expense of pathfinding between regions not taking advantage of the flood filling optimization to detect unreachable zones */
    var floodRegionSize = 50

    private var didFullFloodFill = false

    private val dummyEntArray2 = GdxArray<Entity>()

    /** You may call this to force update of EntityList  */
    open fun removeAndAddEntities() {
        dummyEntArray2.clear()
        dummyEntArray2.addAll(toBeRemoved)
        toBeRemoved.clear()
        for (ent in dummyEntArray2) {
            entityList.removeValue(ent, true)

            if (!ent.providesCullingInfo) {
                noCulling.removeValue(ent, true)
            }

            if (ent.isStatic) {
                if (ent.providesCollidingInfo)
                    collisionTreeForEntityColliding.removeStaticEntity(ent)

                if (ent.providesCullingInfo)
                    collisionTree.removeStaticEntity(ent)
            } else {
                if (ent.providesCollidingInfo)
                    dynamicEntityList.removeValue(ent, true)

                if (ent.providesCullingInfo)
                    dynamicEntityListDraw.removeValue(ent, true)
            }

            updateCellsDueToEntity(ent, true)
            ent.justRemovedFromMap()
        }

        dummyEntArray2.clear()
        dummyEntArray2.addAll(toBeAdded)
        toBeAdded.clear()
        for (ent in dummyEntArray2) {
            if (ent.isAddedToMap) throw RuntimeException("Entity $ent was added to a map twice")
            ent.justBeforeAddingToMap()
            entityList.add(ent)

            if (!ent.providesCullingInfo) {
                noCulling.add(ent)
            }

            if (ent.isStatic) {
                if (ent.providesCollidingInfo)
                    collisionTreeForEntityColliding.addStaticEntity(ent)

                if (ent.providesCullingInfo)
                    collisionTree.addStaticEntity(ent)
            } else {
                if (ent.providesCollidingInfo)
                    dynamicEntityList.add(ent)

                if (ent.providesCullingInfo)
                    dynamicEntityListDraw.add(ent)
            }

            updateCellsDueToEntity(ent, false)
            ent.map = this
            ent.justAddedToMap()
        }

        collisionTreeForEntityColliding.resetDynamicEntities(dynamicEntityList)
        collisionTree.resetDynamicEntities(dynamicEntityListDraw)
    }

    private val drawables = GdxArray<DrawableSortable>()
    private val orderComparator = ComparatorByDepthAndYPosition()

    /** Draws map cells and Entities; uses culling on both  */
    override fun draw(gameDrawer: GameDrawer, delta: Float) {
        if (!wasUpdated) {
            printDebug("WARNING --> draw function is called but the EntityTiledMap wasn't updated!")
            wasUpdated = true // does the warning only one time
        }

        if (!ignoreTileDrawing)
            getTilesDrawn()

        getEntitiesDrawn()

        if (extraMapDrawables.size > 0)
            drawables.addAll(extraMapDrawables)

        if (frameDrawOrders.size > 0) {
            drawables.addAll(frameDrawOrders)
            frameDrawOrders.clear()
        }

        drawables.sort(orderComparator)

        for (drawable in drawables) {
            gameDrawer.reset()
            drawable.draw(gameDrawer, delta)
        }

        if (Engine.DEBUG && Engine.DEBUG_ENTITIES_BB && Engine.DEBUG_ENTITIES_BB_X_RAY) {
            for (drawable in drawables) {
                if (drawable is Entity) {
                    gameDrawer.reset()
                    drawable.drawDebug(gameDrawer, delta)
                }
            }
        }

        gameDrawer.reset()

        // propagate input handling
        inputController.ifNotNull {
            // loop reversed
            for (i in 0..drawables.size - 1) {
                val drawable = drawables.get((drawables.size - 1) - i)
                if (drawable.handleTouchInput(it, stageInputController)) break
            }
        }

        drawCellsDebug(gameDrawer, delta)

        if (Engine.DEBUG_COLLISION_TREE_CELLS) {
            collisionTreeForEntityColliding.debugDrawCellBounds(gameDrawer)

            gameDrawer.alpha = 0.5f
            gameDrawer.color = Color.RED
            gameDrawer.drawRectangle(cameraRectangle, 0.1f, false)
            gameDrawer.color = Color.BLUE
            gameDrawer.drawRectangle(getDeactivationRectangle(), 0.1f, false)
            gameDrawer.resetColor()
            gameDrawer.resetAlpha()
        }

        drawables.clear()

        endOfFrameNotifier.notifyListeners { it.endOfFrameUpdate(delta) }
    }

    private val dummyRectangle = Rectangle()
    /** Uses culling */
    private fun getTilesDrawn() {
        for (cell in getCellsOccupiedByRectangle(getCameraRectangle(dummyRectangle, drawingCameraBoundsBorder))) {
            drawables.addAll(cell.getTiles())
        }
    }

    /** Uses culling */
    private fun getEntitiesDrawn() {
        drawables.addAll(noCulling)
        for (ent in collisionTree.getPossibleCollidingEntities(getCameraRectangle(dummyRectangle, drawingCameraBoundsBorder), true)) {
            drawables.add(ent)
        }
    }

    fun drawCellsDebug(gameDrawer: GameDrawer, delta: Float) {
        if (!Engine.DEBUG) return
        if (!Engine.DEBUG_MAP_CELLS && !Engine.DEBUG_CELL_FLOOD_FILL) return

        val rectangle = Pools.obtain(Rectangle::class.java)

        val colorSize = if (Engine.DEBUG_CELL_FLOOD_FILL) Utils.colors.values().toArray().size else 0

        for (cell in getCellsOccupiedByRectangle(getCameraRectangle(rectangle, drawingCameraBoundsBorder))) {
            if (Engine.DEBUG_MAP_CELLS) {
                if (cell.isSolid) {
                    gameDrawer.alpha = Engine.DEBUG_TRANSPARENCY / 2f
                    gameDrawer.color = Color.BLACK
                    gameDrawer.drawRectangle(cell.x.toFloat(), cell.y.toFloat(), 1f, 1f, true, 0f)
                    gameDrawer.resetColorAndAlpha()
                }

                if (cell.debugFlashingRect.isFlashing) {
                    cell.debugFlashingRect.update()
                    cell.debugFlashingRect.setupGameDrawer(gameDrawer)
                    gameDrawer.drawRectangle(cell.x.toFloat(), cell.y.toFloat(), 1f, 1f, true, 0f)
                    cell.debugFlashingRect.resetGameDrawer(gameDrawer)
                }

                gameDrawer.color = Color.BLACK
                gameDrawer.alpha = Engine.DEBUG_TRANSPARENCY / 2f
                gameDrawer.drawRectangle(cell.x.toFloat(), cell.y.toFloat(), 1f, 1f, false, 0.03f)

                gameDrawer.resetColorAndAlpha()
            }

            if (Engine.DEBUG_CELL_FLOOD_FILL) {
                if (cell.pathfindingFloodIndex != -1L) {
                    gameDrawer.alpha = Engine.DEBUG_TRANSPARENCY / (if (cell.isSolid) 1f else 3f)
                    gameDrawer.color = Utils.colors.values().elementAt((cell.pathfindingFloodIndex % colorSize).toInt())
                    gameDrawer.drawRectangle(cell.x.toFloat(), cell.y.toFloat(), 1f, 1f, true, 0f)
                    gameDrawer.drawText(cell.floodRegion.toString(), cell.middleX, cell.middleY, Engine.debugUI.debugFont, Color.BLACK)
                    gameDrawer.resetColorAndAlpha()
                }
            }
        }

        Pools.free(rectangle)
    }

    private fun getDeactivationRectangle(): Rectangle {
        getCameraRectangle(deactivationRectangle, entityDeactivationBorder)
        return deactivationRectangle
    }

    /**
     * Returned camera rectangle is updated at the start of the map's update cycle, so be careful about posterior camera
     * modifications (use some border)
     *
     * @param border border around the camera, in game units; can be negative
     */
    fun getCameraRectangle(rectangleToReturn: Rectangle, border: Int): Rectangle {
        rectangleToReturn.set(cameraRectangle)

        if (border == 0) return rectangleToReturn

        rectangleToReturn.x -= border.toFloat()
        rectangleToReturn.y -= border.toFloat()
        rectangleToReturn.width += (border * 2).toFloat()
        rectangleToReturn.height += (border * 2).toFloat()

        return rectangleToReturn
    }

    private fun updateCameraRectangle() {
        val width = worldCamera.viewportWidth * worldCamera.zoom
        val height = worldCamera.viewportHeight * worldCamera.zoom

        cameraRectangle.set(worldCamera.position.x - width / 2, worldCamera.position.y - height / 2, width, height)
    }

    open fun getClosestValidCell(startingCell: MapCell, allowSolidCells: Boolean, allowCellsWithTiledEntity: Boolean): MapCell? {
        if (isCellValid(startingCell, allowSolidCells, allowCellsWithTiledEntity))
            return startingCell

        var x = 0
        var y = 0
        var amount = 1
        var sign = 1
        var yTurn = false

        // spiral loop around the start cell
        while (true) {
            // if cell is inside map
            val posX: Int = startingCell.x + x
            val posY: Int = startingCell.y + y

            if (isInsideMap(posX.toFloat(), posY.toFloat())) {
                val cell = getMapCell(posX, posY)

                if (isCellValid(startingCell, allowSolidCells, allowCellsWithTiledEntity)) {
                    return cell
                }
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

    open fun isCellValid(mapCell: MapCell, allowSolidCells: Boolean, allowCellsWithTiledEntity: Boolean): Boolean {
        return (allowSolidCells || mapCell.isSolid) && (allowCellsWithTiledEntity || mapCell.staticEntitiesAbove.size == 0)
    }

    fun fixPositionX(x: Int): Int {
        var x2 = x
        x2 = Utils.clamp(x2.toFloat(), 0f, (width - 1).toFloat()).toInt()
        x2 = Utils.clamp(x2.toFloat(), 0f, (width - 1).toFloat()).toInt()
        return x2
    }

    fun fixPositionY(y: Int): Int {
        var y2 = y
        y2 = Utils.clamp(y2.toFloat(), 0f, (height - 1).toFloat()).toInt()

        return y2
    }

    fun isInsideMap(x: Number, y: Number): Boolean {
        return x.toFloat() >= 0 && y.toFloat() >= 0 && x.toFloat() < width && y.toFloat() < height
    }

    fun addEntity(ent: Entity) {
        if (ent.isKilled) {
            throw RuntimeException("Tried to add an Entity that was killed: $ent")
        }

        toBeAdded.add(ent)
    }

    /** @param forever if false the entity may be added again to another map, otherwise it will be marked as "killed" */
    fun removeEntity(ent: Entity, forever: Boolean = true) {
        toBeRemoved.add(ent)
        if (forever) {
            ent.kill()
        }
    }

    /** Updates cell variables on cells affected by Entity's bounding box  */
    open fun updateCellsDueToEntity(ent: Entity, entityIsRemoved: Boolean) {
        if (!ent.isStatic || !ent.providesStaticInfoToCells) return

        // will ignore Static entities that overlap outside the map
        for (cell in getCellsOccupiedByRectangle(ent.boundingBox)) {
            if (entityIsRemoved) {
                cell.staticEntitiesAbove.removeValue(ent, true)
            } else {
                cell.staticEntitiesAbove.add(ent)
            }

            if (Engine.DEBUG && Engine.DEBUG_MAP_CELLS)
                cell.debugFlashingRect.flashColor(Color.YELLOW, 0.6f, 0.1f)

            if (ent.isSolid) {
                cell.updateHasSolid()

                if (Engine.DEBUG && Engine.DEBUG_MAP_CELLS)
                    cell.debugFlashingRect.flashColor(Color.RED, 0.6f, 0.1f)
            }
        }
    }

    fun getCellsOccupiedByRectangle(rectangle: Rectangle): GdxArray<MapCell> {
        val cells = dummyCellArray
        cells.clear()

        val rectangleOrigin = Pools.obtain(GridPoint2::class.java)
        val rectangleEnd = Pools.obtain(GridPoint2::class.java)

        rectangleOrigin.set(Math.floor(rectangle.x.toDouble()).toInt(), Math.floor(rectangle.y.toDouble()).toInt())
        rectangleEnd.set(Math.floor((rectangle.x + rectangle.width).toDouble()).toInt(), Math.floor((rectangle.y + rectangle.height).toDouble()).toInt())

        for (xx in rectangleOrigin.x..rectangleEnd.x) {
            for (yy in rectangleOrigin.y..rectangleEnd.y) {
                if (isInsideMap(xx.toFloat(), yy.toFloat()))
                    cells.add(getMapCell(xx, yy))
            }
        }

        Pools.free(rectangleEnd)
        Pools.free(rectangleOrigin)

        return cells
    }

    open fun resetCellTiles() {
        for (cell in mapCells.values) {
            cell.getTiles().clear()
        }
    }

    override fun dispose() {

    }

    enum class Event {
        MAP_SOLID_INFORMATION_CHANGED
    }

    /** Saves to a JsonValue: all entities (see [Entity.saveEntity]), custom properties, misc. variables (size, deactivation flags), and cells info */
    open fun save(saveEntities: Boolean = true, onlySaveEntitiesProperties: Boolean = false, saveMapProperties: Boolean = true,
             saveMapCells: Boolean = true): JsonValue {
        val info = JsonSavedObject()
        val json = Json()
        info.addChildValue("deactivateEntities", deactivateEntities)
        info.addChildValue("entityDeactivationBorder", entityDeactivationBorder)

        if (saveEntities) {
            removeAndAddEntities()

            info.addChild("entities", JsonSavedObject().also {
                for (ent in entityList) {
                    it.addChild("ent", ent.saveEntity(onlySaveEntitiesProperties))
                }
            })
        }

        if (saveMapProperties) info.addChildValue("customProperties", json.toJson(customProperties))

        info.addChildValue("width", width)
        info.addChildValue("height", height)
        info.addChildValue("collisionTree.cellSize", collisionTreeForEntityColliding.cellSize)

        if (saveMapCells) {
            info.addChildValue("cells", JsonSavedObject().also {
                for (cell in mapCells.values) {
                    it.addChild("cell", cell.save())
                }
            })
        }

        return info
    }

    /** Will ignore entities, custom properties, and cells whenever any of those is not saved (so basically *only* loads what is saved) */
    open fun load(savedMapInfoJson: JsonValue, entityLoaderFromClass: EntityLoaderFromClass? = null, saveVersion: String) {
        val json = Json()

        initMap(savedMapInfoJson.getInt("width", 0),
                savedMapInfoJson.getInt("height", 0),
                savedMapInfoJson.getInt("collisionTree.cellSize", 0))

        val ents = savedMapInfoJson.get("entities")
        if (ents != null) {
            for (entInfo in ents) {
                if (entityLoaderFromClass != null) {
                    val clazz = Class.forName(entInfo.getString("class")) as Class<Entity>
                    var ent: Entity? = entityLoaderFromClass.loadEntity(clazz)
                    if (ent == null) ent = clazz.newInstance()
                    addEntity(ent!!)
                    ent.loadEntity(entInfo, saveVersion)
                }
            }
        }

        val cells = savedMapInfoJson.get("cells")
        if (cells != null) {
            for (cellInfo in cells) {
//                mapCells.add(GridPoint2(cellInfo.getInt("x"), cellInfo.getInt("y")),
//                        MapCell.load(cellInfo, this))
            }
        }

        customProperties = json.fromJson(CustomProperties::class.java, savedMapInfoJson.getString("customProperties")) ?: customProperties
    }
}

/**
 * Created by Darius on 28/10/2017.
 *
 * Orders entities based on depth, and then y-position if depth is equal. Higher depth means further away from camera.
 * Entity with -10 depth is drawn above than another with 30 depth
 */
class ComparatorByDepthAndYPosition : Comparator<DrawableSortable> {
    override fun compare(o1: DrawableSortable, o2: DrawableSortable): Int {
        var comparison = o2.getDrawingRepresentativeDepth().compareTo(o1.getDrawingRepresentativeDepth())

        if (comparison == 0) comparison = o2.getDrawingRepresentativeY().compareTo(o1.getDrawingRepresentativeY())

        if (comparison == 0) // same y position
                comparison = o2.getDrawingRepresentativeYDepth().compareTo(o1.getDrawingRepresentativeYDepth())

        return comparison
    }
}

interface EndOfFrameListener {
    fun endOfFrameUpdate(delta: Float)
}

interface StartOfFrameListener {
    fun startOfFrameUpdate(delta: Float)
}