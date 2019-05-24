package com.dcostap.engine.map

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.JsonValue
import com.dcostap.engine.utils.GameDrawer
import com.dcostap.engine.utils.JsonSavedObject
import com.dcostap.engine.utils.addChildValue
import com.dcostap.engine.utils.input.InputController

/**
 * Created by Darius on 11/01/2018
 */
open class Tile(val map: EntityTiledMap, var depth: Int, var tileSprName: String) : DrawableSortable {
    var rotation: Int = 0
    lateinit var cell: MapCell

    val sprite: TextureRegion
        get() {
            return map.screen.assets.getTexture(tileSprName)
        }

    override fun draw(gameDrawer: GameDrawer, delta: Float) {
        gameDrawer.draw(sprite, cell.x.toFloat(), cell.y.toFloat())
    }

    override fun getDrawingRepresentativeY(): Float {
        return cell.y.toFloat()
    }

    override fun getDrawingRepresentativeDepth(): Int {
        return depth
    }

    override fun getDrawingRepresentativeYDepth(): Int {
        return 0
    }

    fun save(): JsonValue {
        val json = JsonSavedObject()
        json.addChildValue("rotation", rotation)
        json.addChildValue("depth", depth)
        json.addChildValue("tileSprName", tileSprName)
        return json
    }

    companion object {
        fun load(json: JsonValue, map: EntityTiledMap): Tile {
            val tileSprName = json.getString("tileSprName")
            val tile = Tile(map, json.getInt("depth"), tileSprName)
            tile.rotation = json.getInt("rotation")

            tile.tileSprName = tileSprName
            return tile
        }
    }

    override fun handleTouchInput(inputController: InputController, stageInputController: InputController?): Boolean {
        return false
    }
}
