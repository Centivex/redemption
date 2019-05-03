package com.dcostap.engine.map

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.JsonValue
import com.dcostap.engine.map.entities.CollidingEntity
import com.dcostap.engine.map.entities.Entity
import com.dcostap.engine.utils.FlashingThing
import com.dcostap.engine.utils.JsonSavedObject
import com.dcostap.engine.utils.addChildValue

/**
 * Created by Darius on 14/09/2017.
 */
open class MapCell(val x: Int, val y: Int, private val cellSize: Int, val map: EntityTiledMap) {
    /** Use to directly mark the cell itself as solid*/
    var markedAsSolid = false
    private var hasSolid = false

    /** Whether any static solid Entity occupies this cell *or* the cell is directly set as solid.
     * May be used to speed up collision detection.
     * @see CollidingEntity */
    val isSolid
        get() = hasSolid || markedAsSolid

    var staticEntitiesAbove = Array<Entity>()
    var node = PathfindingNode()
    val debugFlashingThing = FlashingThing()

    /** Each cell may have several tiles (graphical information) */
    private val tiles = Array<Tile>()

    fun getTiles(): Array<Tile> = tiles
    fun addTile(tile: Tile) {
        tiles.add(tile)
        tile.cell = this
    }

    internal fun updateHasSolid() {
        hasSolid = false
        for (ent in staticEntitiesAbove) {
            if (ent.isSolid) {
                hasSolid = true
                return
            }
        }
    }

    val middleX: Float get() = x + cellSize.toFloat() / 2f
    val middleY: Float get() = y + cellSize.toFloat() / 2f

    inner class PathfindingNode {
        var g: Float = 0f
        var f: Float = 0f
        var cameFrom: MapCell? = null

        init {
            reset()
        }

        // after performing a pathfinding algorithm, reset these values
        fun reset() {
            g = -1f
            f = -1f
            cameFrom = null
        }
    }

    fun save(): JsonValue {
        val json = JsonSavedObject()
        json.addChildValue("x", x)
        json.addChildValue("y", y)

        json.addChildValue("markedAsSolid", markedAsSolid)
        json.addChildValue("hasSolid", hasSolid)
//        json.addChildValue("customProperties", jsonlibgdx.toJson(properties))

        json.addChildValue("tiles", JsonSavedObject().also {
            for (tile in tiles) {
                it.addChild("tile", tile.save())
            }
        })

        json.addChildValue("cellSize", cellSize)

        return json
    }

    companion object {
        @JvmStatic fun load(json: JsonValue, map: EntityTiledMap): MapCell {
            val mapCell = MapCell(json.getInt("x"), json.getInt("y"), json.getInt("cellSize"), map)
            mapCell.markedAsSolid = json.getBoolean("markedAsSolid")
            mapCell.hasSolid = json.getBoolean("hasSolid")

//            val jsonlibgdx = Json()
//            mapCell.properties = jsonlibgdx.fromJson(CustomProperties::class.java, json.getString("customProperties"))

            val tilesInfo = json.get("tiles")
            if (tilesInfo != null) {
                for (tile in tilesInfo) {
                    mapCell.tiles.add(Tile.load(tile, map))
                }
            }

            return mapCell
        }
    }
}
