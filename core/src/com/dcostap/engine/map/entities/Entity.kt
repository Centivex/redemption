package com.dcostap.engine.map.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.GridPoint2
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.*
import com.badlogic.gdx.utils.Array
import com.dcostap.Engine
import com.dcostap.engine.map.DrawableSortable
import com.dcostap.engine.map.EntityTiledMap
import com.dcostap.engine.map.MapCell
import com.dcostap.engine.map.map_loading.CustomProperties
import com.dcostap.engine.utils.ui.ExtLabel
import com.dcostap.engine.utils.ui.ExtTable
import com.dcostap.engine.utils.ui.ResizableActorTable
import com.dcostap.engine.utils.*
import com.dcostap.engine.utils.actions.Action
import com.dcostap.engine.utils.actions.ActionsUpdater
import com.dcostap.engine.utils.input.InputController
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.spinner.IntSpinnerModel
import com.kotcrab.vis.ui.widget.spinner.Spinner
import ktx.actors.onChange
import ktx.collections.*

/**
 * Created by Darius on 14/09/2017.
 *
 * Entities are instances of the game world, stored and updated on a [EntityTiledMap].
 *
 * Default constructor creates a non-solid non-static Entity positioned at 0, 0 with boundingBox of size 0
 *
 * BoundingBox created in the constructor is the default BB; Entities may have more than one, each one identified by a
 * name (default BB's name is "default").
 *
 * Any BB may be modified later after creation (use [getActualBoundingBox]), however if the Entity is
 * static any modification will throw an Exception unless it is done before [isAddedToMap] (map will actually
 * add the Entity in the next frame)
 *
 * Position modification on static entities is forbidden as well in the same way
 *
 * **If [providesCollidingInfo] is false entity will never be included in the map's colliding tree for entities.
 * This means no other Entity will be able to find this one using that colliding tree, thus reducing overhead.
 * This applies to collision checking or just finding close Entities. You could, however use [EntityTiledMap.collisionTree]
 * whenever you need all Entities**. Note that not providing collInfo doesn't mean the Entity can't check for collisions!
 *
 *
 * Static entities:
 * - can't move from initial position.
 * - will be included in map's quadTree as a static entity, with information which won't change until it is removed;
 * they are more efficient
 * - MapCells occupied by its default BB will save information from the Entity
 *
 * Dynamic entities:
 * - can move
 * - will be included in map's quadTree as a dynamic entity, which means it will be updated each frame on the quadTree
 * whenever it moves. Dynamic entity not moving is practically the same as a static entity in terms of performance.
 *
 * @param boundingBox     x, y values are the offset of the bounding box. All in game units.
 * @param isSolid         Flag that may be used for collision response in [CollidingEntity].
 */
open class Entity @JvmOverloads constructor(val position: Vector2 = Vector2(), boundingBox: Rectangle = Rectangle(),
                                                var isSolid: Boolean = false, var isStatic: Boolean = false,
                                                providesCollidingInfo: Boolean = Engine.ENTITIES_PROVIDE_COLL_INFO_DEFAULT,
                                                providesCullingInfo: Boolean = true,
                                                providesStaticInfoToCells: Boolean = true)
    : Updatable, DrawableSortable
{
    var providesCollidingInfo = providesCollidingInfo
        set(value) {
            if (isAddedToMap) throw RuntimeException("providesCollidingInfo value can't be changed once added to a map!")
            field = value
        }

    /** if false entity will always be drawn even if outside the camera rectangle. Change it only before adding to a map */
    var providesCullingInfo: Boolean = providesCullingInfo
        set(value) {
            if (isAddedToMap) throw RuntimeException("providesCullingInfo value can't be changed once added to a map!")
            field = value
        }

    /** if true, static entities will modify colliding mapCells when added / removed to the map */
    var providesStaticInfoToCells: Boolean = providesStaticInfoToCells
        set(value) {
            if (isAddedToMap) throw RuntimeException("providesCullingInfo value can't be changed once added to a map!")
            field = value
        }

    val collisionTree get() = map?.collisionTree
    val worldViewport get() = map?.worldViewport

    /** Higher depth = further away from the camera. If two entities have same depth, y position is used to determine who is drawn first
     * @see DrawableSortable.getDrawingRepresentativeDepth */
    var depth = 0

    /** @see DrawableSortable.getDrawingRepresentativeYDepth */
    var yDepth = 0

    var map: EntityTiledMap? = null

    val isAddedToMap get() = map != null

    var actions = ActionsUpdater()
        private set

    var showDebugTable: Boolean = true

    fun addAction(action: Action) {
        actions.add(action)
    }

    /** will add all actions as sequence */
    fun addAction(vararg actions: Action) {
        addAction(Action.sequence(*actions))
    }

    var tiledEditorGroupNames: Array<String>? = null
    var customProperties = CustomProperties()

    var isKilled = false
        private set

    /** Should never be called directly, instead use [EntityTiledMap.removeEntity]. A killed Entity can never be added to a map again. */
    open fun kill() {
        debugTable.remove()
        isKilled = true
    }

    private var hasMoved = false

    /** Use [updateCollidingState] to update the Array */
    val possibleCollidingEntities = Array<Entity>()

    val debugFlashingRect = FlashingRect()

    var boundingBoxes: ObjectMap<String, BoundingBox>
        protected set

    /** @return The **default** absolute positioned Rectangle that represents the bounding box */
    val boundingBox: Rectangle
        get() = getBoundingBox("default")

    private val debugTable = ResizableActorTable()
    private var debugTableMouseOver = true
    private var debugTableTempInvisible = false

    private var initDebug = true
    private val debugTableChild = Table()

    init {
        boundingBoxes = ObjectMap()
        addBoundingBox("default", boundingBox)

        // debug UI table
        ExtLabel.defaultFont = VisUI.getSkin().getFont("small-font")
        ExtLabel.defaultColor = Color.WHITE

        Engine.debugUI.stage.addActor(debugTable)
        debugTable.setPosition(-999999f, -999999f)
        debugTable.add(Table().also {
            it.align(Align.top)
            it.top()
            it.background = Utils.solidColorDrawable(0f, 0f, 0f, 0.7f)
            it.pad(4f)

            it.add(ExtTable().also {
                it.top()
                val previousColor = ExtLabel.defaultColor
                ExtLabel.defaultColor = Color.WHITE
                it.add(ExtLabel(this.javaClass.simpleName))
                it.add(Utils.visUI_customCheckBox("Hide", false).also {
                    it.onChange {
                        debugTableTempInvisible = true
                    }
                }).padLeft(8f)
                ExtLabel.defaultColor = previousColor
            })

            it.row()

            it.add(debugTableChild)
        }).top()

        debugTable.isVisible = false
    }

    /** Called once when added to the map. Before this [mapNumber] might be un-initialized */
    open fun justAddedToMap() {
        previousPosition.set(position)
    }

    open fun justBeforeAddingToMap() {}

    open fun justRemovedFromMap() {
        map = null
    }

    open fun createDebugInfoTable(contents: Table) {
        contents.add(ExtLabel(textUpdateDelay = 0.15f) {
            "x: ${Utils.formatNumber(x, 3)}; y: " + Utils.formatNumber(y, 3)
        })
        contents.row()
        contents.add(ExtLabel().also {
            it.textUpdateFunction = {"solid: $isSolid"}
        })
        contents.row()
        contents.add(ExtLabel().also {
            it.textUpdateFunction = {"static: $isStatic"}
        })
        contents.row()

        contents.add(Table().also {
            it.add(ExtLabel().also {
                it.textUpdateFunction = {"depth: $depth"}
            })
//            it.add(ExtLabel().also {
//                it.textUpdateFunction = {"depth: $depth"}
//            })
            val spinner = IntSpinnerModel(depth, -100000, 100000, 1)
            it.add(Spinner("", spinner).also {
                    it.onChange {
                        depth = spinner.value
                    }
            }).padLeft(10f)
        })
        contents.row()
    }

    /**
     * If relative and using delta, will move "units specified" /s. To instantly move the quantity specified pass a
     * delta value of 1
     *
     * @param relative If not relative, delta is ignored
     * @param delta    Use value 1 to ignore delta and move the exact quantity specified
     */
    fun move(x: Float, y: Float, relative: Boolean, delta: Float = 1f) {
        var thisX = x
        var thisY = y

        if (relative) {
            thisX *= delta
            thisY *= delta

            position.x += thisX
            position.y += thisY
        } else {
            position.x = thisX
            position.y = thisY
        }
    }

    private val previousPosition = Vector2(position)
    private var previousDebugStageUIEntInfo = false

    private fun debugInfoIsVisible() = (Engine.DEBUG_UI && isAddedToMap && !debugTableTempInvisible && showDebugTable &&
            (Engine.DEBUG_UI_ENTITY_INFO_ABOVE || (Engine.DEBUG_UI_ENTITY_INFO_POPUP && debugTableMouseOver)) &&
            !(isStatic && Engine.DEBUG_UI_HIDE_STATIC_ENTITY_INFO))

    override fun update(delta: Float) {
        if (isAddedToMap && !previousPosition.equals(position)) {
            hasMoved = true

            if (isStatic)
                throw RuntimeException("Moved a static entity which is already added to a mapNumber: " + javaClass.simpleName)
        }

        if (initDebug) {
            initDebug = false

            debugTableChild.defaults().left()

            val previousColor = ExtLabel.defaultColor
            ExtLabel.defaultColor = Color.WHITE

            createDebugInfoTable(debugTableChild)

            ExtLabel.defaultColor = previousColor
        }

        previousPosition.set(position)

        actions.update(delta)

        if (debugTableTempInvisible && !Engine.DEBUG_UI_ENTITY_INFO_ABOVE) debugTableTempInvisible = false

        debugTable.isVisible = debugInfoIsVisible()
        debugTableMouseOver = false

        if (debugTable.ancestorsVisible()) {
            map.ifNotNull {
                val coords = Pools.obtain(Vector2::class.java)
                coords.set(Utils.projectPosition(x, y, it.worldViewport, Engine.debugUI.stage.viewport))
                debugTable.pack()
                debugTable.setPosition(coords.x, coords.y)

                Pools.free(coords)
            }
        }
    }

    override fun draw(gameDrawer: GameDrawer, delta: Float) {
        if (!Engine.DEBUG_ENTITIES_BB_X_RAY) drawDebug(gameDrawer, delta)
    }

    private val debugRect = Rectangle()

    fun drawDebug(gameDrawer: GameDrawer, delta: Float) {
        if (Engine.DEBUG_UI_HIDE_STATIC_ENTITY_INFO && isStatic) return

        gameDrawer.alpha = Engine.DEBUG_TRANSPARENCY
        gameDrawer.color = if (isStatic) Color.RED else Color.BLUE
        for (bb in boundingBoxes.values()) {
            gameDrawer.drawRectangle(bb.rect, Engine.DEBUG_LINE_THICKNESS, false)

            if (isSolid) {
                debugRect.set(bb.rect)
                Utils.growRectangle(debugRect, -Engine.DEBUG_LINE_THICKNESS * 1.6f)
                gameDrawer.drawRectangle(debugRect, Engine.DEBUG_LINE_THICKNESS, false)
            }
        }

        gameDrawer.color = Color.CHARTREUSE
        gameDrawer.alpha = 0.65f
        gameDrawer.drawCross(x, y, 0.2f, Engine.DEBUG_LINE_THICKNESS * 0.9f)

        gameDrawer.resetAlpha()
        gameDrawer.resetColor()

        debugFlashingRect.update()

        if (debugFlashingRect.isFlashing) {
            debugFlashingRect.setupGameDrawer(gameDrawer)

            gameDrawer.drawRectangle(boundingBox, 0f, true)

            debugFlashingRect.resetGameDrawer(gameDrawer)
        }
    }

    /** @param name Identifier of the bounding box, by default the **default** BB
     * @return The absolute positioned Rectangle that represents the bounding box */
    fun getBoundingBox(name: String = "default"): Rectangle {
        return boundingBoxes.get(name).rect
    }

    fun addBoundingBox(name: String, rectangle: Rectangle): BoundingBox {
        val newBB = BoundingBox(this, name)
        newBB.modify(rectangle)
        boundingBoxes.put(name, newBB)
        return newBB
    }

    fun removeBoundingBox(name: String) {
        boundingBoxes.remove(name)
    }

    @JvmOverloads fun getActualBoundingBox(name: String = "default"): BoundingBox {
        return boundingBoxes.get(name)
    }

    /**
     * Call each frame to update the list of possible colliding entities
     *
     *
     * Call this method when:
     *  * entities might have been added / removed
     *  * when dynamic entities, including this one, might have moved
     */
    @JvmOverloads fun updateCollidingState(includeDynamicEntities: Boolean, boundingBoxName: String = "default") {
        possibleCollidingEntities.clear()

        map.ifNotNull {
            for (entity in it.collisionTreeForEntityColliding.getPossibleCollidingEntities(getBoundingBox(boundingBoxName), includeDynamicEntities)) {
                if (entity === this) continue
                possibleCollidingEntities.add(entity)
            }
        }
    }

    @JvmOverloads fun isCollidingWith(otherEntity: Entity, thisBB: String = "default", otherBB: String = "default"): Boolean {
        return getBoundingBox(thisBB).overlaps(otherEntity.getBoundingBox(otherBB))
    }

    private val dummyCellArray = Array<MapCell>()

    /**
     * Use this to check for solid collision without having to do the expensive call to the CollisionTree,
     * if the Entity will only collide with the boundaries of the map cells
     *
     *
     * Limitations compared to checking collision against Entities (using [updateCollidingState]):
     *  * Solid collision is only done against mapCell's boundaries, no precise collision checking on arbitrary positions
     *  * MapCells only provide info about Static Entities above them, or whether they have been marked as solid
     *
     *  @see [MapCell.isSolid]
     */
    @JvmOverloads fun getCollidingMapCells(thisBB: String = "default", filter: (MapCell) -> Boolean = {true}): GdxArray<MapCell> {
        dummyCellArray.clear()
        for (mapCell in map!!.getCellsOccupiedByRectangle(getBoundingBox(thisBB))) {
            if (filter(mapCell)) {
                dummyCellArray.add(mapCell)
            }
        }

        return dummyCellArray
    }

    private val dummyEntityArray = Array<Entity>()

    /**
     * Update colliding state before this!
     *
     * @return All Entities found that are of the specified class and that collide with this Entity
     */
    @JvmOverloads fun getCollidingEntities(thisBB: String = "default", otherBB: String = "default", filter: (Entity) -> Boolean = {true}): GdxArray<Entity> {
        dummyEntityArray.clear()

        for (entity in possibleCollidingEntities) {
            if (filter(entity) && isCollidingWith(entity, thisBB, otherBB)) {
                dummyEntityArray.add(entity)
            }
        }

        return dummyEntityArray
    }

    fun hasMoved(): Boolean {
        return hasMoved
    }

    /**
     * Call on all Entities when a new update loop starts, since this variable is meant to show if the Entity
     * has moved since the loop started
     */
    fun resetHasMoved() {
        hasMoved = false
    }

    var x: Float
        get() = position.x
        set(value) {
            position.x = value
        }

    var y: Float
        get() = position.y
        set(value) {
            position.y = value
        }

    fun getTiledPosition(vectorToModify: Vector2): Vector2 {
        return vectorToModify.set(tiledX.toFloat(), tiledY.toFloat())
    }

    fun getTiledPosition(gridPointToModify: GridPoint2): GridPoint2 {
        return gridPointToModify.set(tiledX, tiledY)
    }

    val tiledX: Int
        get() = Math.floor(position.x.toDouble()).toInt()

    val tiledY: Int
        get() = Math.floor(position.y.toDouble()).toInt()

    override fun toString(): String {
        return (javaClass.simpleName + "(" + (if (isSolid) "solid" else "notSolid") + ", "
        + (if (isStatic) "static" else "notStatic") + ", " + (if (isKilled) "killed" else "notKilled") + ")"
        + "; hash: " + hashCode())
    }

    fun removeFromMap(forever: Boolean = true) {
        map?.removeEntity(this, forever)
    }

    override fun getDrawingRepresentativeY(): Float {
        return position.y
    }

    override fun getDrawingRepresentativeDepth(): Int {
        return depth
    }

    override fun getDrawingRepresentativeYDepth(): Int {
        return yDepth
    }

    /** Useful function for correctly handling touch input in a [EntityTiledMap]
     * @see [DrawableSortable.handleTouchInput] */
    override fun handleTouchInput(inputController: InputController, stageInputController: InputController?): Boolean {
        if (Engine.DEBUG_UI && showDebugTable && Engine.DEBUG_UI_ENTITY_INFO_POPUP) {
            pool(Rectangle::class.java) {
                it.set(boundingBox)
                if (it.width <= 0.2) it.width = 0.2f
                if (it.height <= 0.2) it.height = 0.2f
                if (it.contains(inputController.mousePos.world.x, inputController.mousePos.world.y)) {
                    debugTableMouseOver = true
                }
            }
        }

        return false
    }

    private val jsonlibgdx = Json()

    /** todo: Saving Actions in [ActionsUpdater] */
    open fun saveEntity(onlySaveProperties: Boolean = false): JsonValue {
        val json = JsonSavedObject()

        if (!onlySaveProperties) {
            json.addChildValue("class", this.javaClass.name)

            json.addChildValue("bbs", JsonSavedObject().also {
                for (bb in boundingBoxes.values()) {
                    it.addChild("bb", bb.save())
                }
            })

            json.addChildValue("isStatic", isStatic)
            json.addChildValue("isSolid", isSolid)
            json.addChildValue("depth", depth)

            if (tiledEditorGroupNames != null) {
                json.addChildValue("tiledEditorGroupNames", jsonlibgdx.toJson(tiledEditorGroupNames!!.toArray()))
            }

            json.addChildValue("position", jsonlibgdx.toJson(position))

            json.addChildValue("isKilled", isKilled)
        }

        json.addChildValue("customProperties", jsonlibgdx.toJson(customProperties))

        return json
    }

    open fun loadEntity(jsonSavedObject: JsonValue, saveVersion: String) {
        boundingBoxes.clear()

        for (bb in jsonSavedObject.get("bbs")) {
            val name = bb.getString("name")
            boundingBoxes.put(name, BoundingBox(this, name).also {
                it.load(bb, "0")
            })
        }

        isStatic = jsonSavedObject.getBoolean("isStatic")
        isSolid = jsonSavedObject.getBoolean("isSolid")
        isKilled = jsonSavedObject.getBoolean("isKilled")
        depth = jsonSavedObject.getInt("depth")

        if (jsonSavedObject.has("tiledEditorGroupNames")) {
            tiledEditorGroupNames = Array(jsonlibgdx.fromJson(kotlin.Array<String>::class.java,
                    jsonSavedObject.getString("tiledEditorGroupNames")) as kotlin.Array<String>)
        }
        customProperties = jsonlibgdx.fromJson(CustomProperties::class.java, jsonSavedObject.getString("customProperties"))

        position.set(jsonlibgdx.fromJson(Vector2::class.java, jsonSavedObject.getString("position")))
    }
}
