package com.dcostap.engine.map.map_loading

import com.badlogic.gdx.math.Vector2
import com.dcostap.engine.map.EntityTiledMap
import com.dcostap.engine.map.entities.Entity

/**
 * Links strings with Entity creation
 */
interface EntityLoaderFromString {
    /**
     * Loads Entity associated with a Tiled's TileObject (Object with image). Identified by its tile image name
     *
     * @param imageName    The name of the tile used by the Tile Object
     * @param heightPixels Depending on object created, may be used or ignored; same with width
     */
    fun loadEntityFromTiledTileObject(imageName: String, objectName: String, position: Vector2, widthPixels: Int, heightPixels: Int,
                                      map: EntityTiledMap, objectProps: CustomProperties): Entity?

    /**
     * Load Entity associated with a Tiled's Object (not a TileObject). Identified by its name
     */
    fun loadEntityFromObjectName(objectName: String, position: Vector2, widthPixels: Int, heightPixels: Int,
                                 map: EntityTiledMap, objectProps: CustomProperties): Entity?
}
