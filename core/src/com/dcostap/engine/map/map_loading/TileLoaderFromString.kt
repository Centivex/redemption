package com.dcostap.engine.map.map_loading

import com.dcostap.engine.map.EntityTiledMap
import com.dcostap.engine.map.MapCell

/**
 * Links strings with Tile creation, inside cells in a TileLayer in Tiled
 */
interface TileLoaderFromString {
    /**
     * When loading a cell may have a tile (image in the cell).
     *
     * @return false to let the MapLoader do the default cell loading (insert the image in the cell searching by its name);
     * @param imageName    The name of the tile inserted in the cell
     */
    fun loadTileFromImageName(imageName: String, mapCell: MapCell, map: EntityTiledMap): Boolean
}
