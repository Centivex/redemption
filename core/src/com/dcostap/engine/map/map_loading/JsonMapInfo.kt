package com.dcostap.engine.map.map_loading

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.ObjectMap
import java.io.File

/**
 * Created by Darius on 19/05/2018.
 *
 * Loads and stores the info of a map JSON file.
 */
internal class JsonMapInfo(val mapName: String, val mapFolder: String, val objectTemplatesJsonFolder: String, val tileSetsJsonLocation: String) {
    //todo: tilesets / templates info should be shared globally. Right now with many JsonMapInfo they will have copies of the same info
    val jsonReader: JsonReader = JsonReader()
    private val mapFile = Gdx.files.internal(mapFolder + File.separator + mapName + ".json")

    init {
        if (!mapFile.exists()) {
            throw RuntimeException("Map file: ${mapFile.path()} doesn't exist")
        }
    }

    val jsonMapFile = jsonReader.parse(mapFile)

    /**
     * TilesetLoaders are stored here, one for each tileset.
     * They are only created when a tileset is first needed while reading the map
     */
    val loadedTileSets: ObjectMap<String, JsonTileSet> = ObjectMap()

    /** @see [loadedTileSets] */
    val loadedTemplates = ObjectMap<String, JsonValue>()

    fun buildTilesetFileName(tileset_name: String): String {
        return tileSetsJsonLocation + File.separator + tileset_name + ".json"
    }
}
