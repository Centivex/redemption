package com.dcostap.engine.map.map_loading

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import com.dcostap.engine.utils.Utils
import java.io.File

/**
 * Created by Darius on 14/11/2017
 *
 * Loads one tileSet from the provided path
 * Provides methods to retrieve an Image inside the tileSet from the provided local ID
 */
class JsonTileSet(tileSetFilePath: String, private val jsonReader: JsonReader) {
    var tileSet: JsonValue? = null

    init {
        loadTileSet(tileSetFilePath)
    }

    private fun loadTileSet(tileSetFilePath: String) {
        tileSet = jsonReader.parse(Gdx.files.internal(tileSetFilePath))
        if (tileSet == null) {
            throw RuntimeException("file not found or not valid tileset: $tileSetFilePath")
        }
    }

    /**
     * Retrieves the image name from the image with provided ID inside the tileSet
     *
     * @param localTileID local ID of the image inside the tileSet, not to confuse with GID
     * (global ID in the Tiled map, across all tileSets)
     * @return name of the image, without extension
     */
    fun getImageNameFromTileId(localTileID: Int): String {
        fun getCleanImageName(filename: String): String {
            return Utils.removeExtensionFromFilename(File(filename).name)
        }

        if (tileSet!!.hasChild("image")) {
            return getCleanImageName(tileSet!!.getString("image"))
        }

        var tile: JsonValue? = null
        for (t in tileSet!!.get("tiles")) {
            if (t.getInt("id") == localTileID) {
                tile = t
            }
        }

        tile!!

        val image_filename = tile.get("image").asString()
        val height = tile.get("imageheight").asInt()
        val width = tile.get("imagewidth").asInt()

        return getCleanImageName(image_filename)
    }
}
