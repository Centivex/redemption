package com.dcostap.engine.map.map_loading

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.GridPoint2
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonValue
import com.dcostap.Engine
import com.dcostap.engine.map.EntityTiledMap
import com.dcostap.engine.map.Tile
import com.dcostap.engine.map.entities.Entity
import com.dcostap.engine.utils.Utils
import com.dcostap.engine.utils.ifNotNull
import com.dcostap.printDebug
import ktx.collections.GdxArray
import java.io.File


/**
 * Handles the loading of objects and tiles inside a map created with Tiled map editor, exported to .json files. Stores all
 * the info of a map's .json file. Can be reused with any number of [EntityTiledMap], each time you want to load the map onto a
 * [EntityTiledMap] use [loadMap]
 *
 * Uses:
 *  * .json file of the map itself
 *  * .json file of each tileSet the map uses
 *  * .json file of each object template the map uses
 *
 *
 * **Loads:**
 *  * TileLayers: by default loads the tile images into the game map cells.
 *  Loading may be customized by using a [TileLoaderFromString]. For example, you may use a special solid texture
 *  as tile in the editor then in the TileLoader used set the cells as solid and ignore the texture. This allows for easier
 *  marking of solid cells than using snapped solid objects in the map editor.
 *
 *  * ObjectLayers: creates Entities for each Object. Can be Tiled's tile objects or shaped objects.
 * Tile objects load Entities based on its tile image name; shaped objects load Entities based on each object name.
 * See [EntityLoaderFromString].
 *
 * The map itself, the entities and the tiles all can have custom properties which are set in Tiled. Map properties
 * may be passed in the constructor, or otherwise they will be loaded from Tiled. This allows for properties in maps to
 * persist.
 *
 * **Takes into account:**
 *  * Layer offset & visible properties: they are inherited from parent groups. In short, it will look just like in Tiled.
 *  Keep into account that when offsetting tile layers, tiles outside the map are silently ignored. Invisible layers are simply ignored.
 *
 * **Group System**
 *
 * Layers may be selectively ignored via the group system. Folders in Tiled may be considered "groups" in this loader,
 * if the name of the folder follows the format "group:name", where name is the name of the group. Layers may belong to
 * 0 or more groups. The map may have a custom boolean property with name "group:name", which may disable that group
 * if the boolean is false. Layers belonging to at least 1 disabled group will be ignored.
 *
 * Special custom properties:
 * - depth: can be in an ObjectLayer, TileLayer, and an Object. Will modify the render order of tiles and entities
 * Object's depth property will overwrite the ObjectLayer's depth property if both exist.
 * By default (if no custom depth property specified) the depth is changed by a fixed amount per each layer loaded.
 * (see [layerDepthDecrease] and [layerDepthStarting])
 *
 * todo: Changes of base tile size from map to map is not supported... PPM is global and tile size of TileMaps is ignored
 *
 * Notes:
 *  * While working on Tiled, base files may be different than .json (.tmx for example). You must then use "export"
 * option and export them as .json each time you edit. You may use .json base file for convenience. (When creating tileSets or
 * templates, save them as .json when first prompted)
 *  * Size of objects in Tiled may or may not be ignored when loading them into Entities
 *  * Note that when using object templates, the size change in one instance of that template won't be saved, so in the game
 * the loaded size is the template's object size. To actually load that object's size, detach it from the template
 */
class JsonMapLoader(mapName: String, mapFolder: String, objectTemplatesJsonFolder: String, tileSetsJsonLocation: String) {
    internal var mapInfo = JsonMapInfo(mapName, mapFolder, objectTemplatesJsonFolder, tileSetsJsonLocation)
    private var entityLoaderFromString: EntityLoaderFromString? = null
    private var tileLoaderFromString: TileLoaderFromString? = null
    private var savedMapInfoJson: JsonValue? = null

    /** @param savedMapInfoJson Pass a JsonValue from saving the map so that properties of entities and the map itself can be loaded */
    fun loadMap(map: EntityTiledMap, entityLoaderFromString: EntityLoaderFromString? = null,
                tileLoaderFromString: TileLoaderFromString? = null, savedMapInfoJson: JsonValue? = null, collisionTreeCellSize: Int = 10) {
        this.savedMapInfoJson = savedMapInfoJson
        this.entityLoaderFromString = entityLoaderFromString
        this.tileLoaderFromString = tileLoaderFromString

        val height = mapInfo.jsonMapFile.get("height").asInt()
        val width = mapInfo.jsonMapFile.get("width").asInt()

        map.initMap(width, height, collisionTreeCellSize)

        if (savedMapInfoJson == null || !loadMapProps(map)) {
            map.customProperties = getCustomPropertiesFromMapJson()
        }

        loadLayers(mapInfo.jsonMapFile, map)

        this.savedMapInfoJson = null

        map.removeAndAddEntities()
    }

    private fun loadMapProps(map: EntityTiledMap): Boolean {
        val json = Json()
        val jsonData = savedMapInfoJson!!
        map.customProperties = json.fromJson(CustomProperties::class.java, jsonData.getString("customProperties")) ?: return false

        return true
    }

    private fun loadEntityProps(entity: Entity): Boolean {
        val json = Json()
        var success = false
        val jsonData = savedMapInfoJson!!

        val id = entity.customProperties.strings.get("id", null) ?: return false

        for (ent in jsonData.get("entities")) {
            ent.getString("customProperties", null).ifNotNull {
                val props = json.fromJson(CustomProperties::class.java, it)
                props.strings.get("id", null).ifNotNull {
                    if (it == id) {
                        entity.customProperties = props
                        success = true
                    }
                }
            }

            if (success) break
        }

        return success
    }

    fun preLoadTemplatesAndTilesets() {
        loadLayers(mapInfo.jsonMapFile, null, true)
    }

    fun getCustomPropertiesFromMapJson(): CustomProperties {
        return getCustomPropertiesFromJson(mapInfo.jsonMapFile)
    }

    fun getCustomPropertiesFromJson(parentJsonValue: JsonValue): CustomProperties {
        val props = parentJsonValue.get("properties")
        val properties = CustomProperties()
        if (props == null) return properties

        for (prop in props) {
            val value = prop["value"]
            val name = value?.parent?.get("name")?.asString() ?: ""
            when (value?.type()) {
                JsonValue.ValueType.booleanValue -> properties.addValue(prop.name, props.get(prop.name).asBoolean())
                JsonValue.ValueType.doubleValue -> properties.addValue(prop.name, props.get(prop.name).asFloat())
                JsonValue.ValueType.longValue -> properties.addValue(name, value.asInt())
                JsonValue.ValueType.stringValue -> properties.addValue(name, props.get(prop.name).asString())
                else -> printDebug("! - JSON LOADER WARNING: (map name: ${mapInfo.mapName}) - When loading " +
                        "Tiled custom properties, property with name: ${prop.name}" +
                        " was found; it has no supported type (type: ${prop.type()}): IT WAS IGNORED")
            }
        }
        return properties
    }

    /** default depth ordering of layers starts with this value*/
    var layerDepthStarting = 10

    private var layerDepth = layerDepthStarting

    /** default decrease of depth for each layer processed */
    var layerDepthDecrease = 10

    /**
     * Loads the info inside all tile layers found in the .json file.
     *
     * Layers might be disabled, and therefore not loaded. This happens if the layer is inside a Tiled folder
     * (called group in Tiled) with a name of format "group:name", then the layer belongs to that group.
     * Layers can belong to more than one group.
     *
     * If any of those groups is disabled on map's properties (boolean with name "group:name" is false) the layer is ignored
     */
    private fun loadLayers(jsonMapFile: JsonValue, map: EntityTiledMap?, onlyPreload: Boolean = false) {
        // iterate through all tile layers of the map
        val layers = jsonMapFile.get("layers")

        val layer: JsonValue? = layers.child() // get first one

        fun loadLayerGroup(firstLayer: JsonValue?, folders: GdxArray<String>? = null, offsetX: Float = 0f, offsetY: Float = 0f) {
            var thisLayer = firstLayer

            // whether the layer is inside a group disabled in map's properties
            fun isLayerAllowed(): Boolean {
                // get name of group from the string. Group names have syntax "group:name"
                fun getGroupFromName(name: String): String? {
                    if (name.trim().startsWith("group:")) {
                        return name.substring(6, name.length)
                    } else return null
                }

                if (map == null || onlyPreload) return true
                if (folders != null) {
                    fun isGroupEnabled(groupName: String): Boolean {
                        for (entry in map.customProperties.booleans.entries()) {
                            if (!entry.value) {
                                val group = getGroupFromName(entry.key)
                                if (group != null && groupName == group) {
                                    // map's custom properties have that group as disabled: layer will be ignored
                                    return false
                                }
                            }
                        }
                        return true
                    }

                    // layer is inside some folder groups: find if one of them has name with format group:name
                    // then find if any of those group names are disabled
                    for (folder in folders) {
                        val group = getGroupFromName(folder)
                        if (group != null)
                            if (!isGroupEnabled(group)) return false
                    }
                    return true
                } else return true
            }

            while (thisLayer != null) {
                layerDepth -= layerDepthDecrease
                if (thisLayer.getBoolean("visible", true)) {
                    if (thisLayer.getString("type") == "tilelayer") {
                        if (isLayerAllowed())
                            loadTileLayer(jsonMapFile, thisLayer, map, onlyPreload, folders, offsetX, offsetY)
                    } else if (thisLayer.getString("type") == "objectgroup") {
                        if (isLayerAllowed())
                            loadObjectLayer(jsonMapFile, thisLayer, map, onlyPreload, folders, offsetX, offsetY)
                    } else if (thisLayer.getString("type") == "group") {
                        val newLayerFolders = GdxArray<String>()
                        folders.ifNotNull {
                            newLayerFolders.addAll(folders)
                        }
                        newLayerFolders.add(thisLayer.getString("name"))
                        loadLayerGroup(thisLayer.get("layers").child, newLayerFolders,
                                offsetX + thisLayer.getInt("offsetx", 0),
                                offsetY + thisLayer.getInt("offsety", 0))
                    }
                }
                thisLayer = thisLayer.next()
            }
        }

        loadLayerGroup(layer)
    }

    /**
     * Loops through all cells inside the tile layer.
     */
    private fun loadTileLayer(jsonMapFile: JsonValue, jsonLayerInfo: JsonValue, map: EntityTiledMap?,
                              onlyPreload: Boolean = false, groupNames: GdxArray<String>? = null, parentOffsetX: Float = 0f, parentOffsetY: Float = 0f) {
        val width = if (map == null) 0 else map.width
        val height = if (map == null) 0 else map.height

        val cellPosition = GridPoint2()

        val layerProperties = getCustomPropertiesFromJson(jsonLayerInfo)

        // if layer contains "depth" property
        val currentLayerDepth = layerProperties.ints.get("depth", layerDepth)

        // loop through all the cells of the layer
        val cells = jsonLayerInfo.get("data").asIntArray()
        for (i in cells.indices) {
            // GID of the cell = global identifier that links the cell with a texture inside a tileset
            val GID = cells[i]

            // GID = 0 means cell has no texture
            if (GID == 0) continue

            val imageName = getTileImageNameFromGIDInsideTileSet(GID, jsonMapFile, null)

            if (!onlyPreload && map != null) {
                cellPosition.set(i % width, height - i / width - 1)

                val mapTileSize = Math.max(jsonMapFile.getInt("tileheight"), jsonMapFile.getInt("tilewidth"))
                // apply the offset
                cellPosition.add(Math.ceil((parentOffsetX + jsonLayerInfo.getInt("offsetx", 0)) / mapTileSize.toDouble()).toInt(),
                        Math.ceil((parentOffsetY + jsonLayerInfo.getInt("offsety", 0)) / mapTileSize.toDouble()).toInt())

                // tiles outside map are silently ignored
                if (!map.isInsideMap(cellPosition.x, cellPosition.y)) continue
                val mapCell = map.getMapCell(cellPosition)

//                mapCell.properties = Json().fromJson(CustomProperties::class.java, jsonData.getString("customProperties"))
                var doNormalLoading = true

                tileLoaderFromString.ifNotNull {
                    // custom loading for that tile?
                    if (it.loadTileFromImageName(imageName, mapCell, map)) doNormalLoading = false
                }

                if (doNormalLoading) {
                    mapCell.getTiles().add(Tile(map, currentLayerDepth, imageName).also {
                        it.cell = mapCell
                        it.rotation = 0 //todo: don't ignore rotation
                    })
                }
            }
        }
    }

    private fun loadObjectLayer(jsonMapFile: JsonValue, jsonLayerInfo: JsonValue, map: EntityTiledMap?,
                                onlyPreload: Boolean = false, groupNames: GdxArray<String>? = null, parentOffsetX: Float = 0f, parentOffsetY: Float = 0f) {
        val layerProperties = getCustomPropertiesFromJson(jsonLayerInfo)

        // if layer contains "depth" property
        val currentLayerDepth = layerProperties.ints.get("depth", layerDepth)

        // loop through all objects
        val objects = jsonLayerInfo.get("objects")
        var objectInfo: JsonValue? = objects.child
        while (objectInfo != null) {
            val position = Vector2(objectInfo.getInt("x").toFloat(), objectInfo.getInt("y").toFloat())

            // apply the offset
            position.x += jsonLayerInfo.getInt("offsetx", 0) + parentOffsetX
            position.y += jsonLayerInfo.getInt("offsety", 0) + parentOffsetY

            // Tiled saves object's coords as pixel units with origin on top-left, so translate it to game coords

            // todo: support odd-sized tile size? - better yet... support change of tile sizes (forget the evil global PPM)
            val mapTileSize = Math.max(jsonMapFile.getInt("tileheight"), jsonMapFile.getInt("tilewidth"))

            position.x /= mapTileSize
            position.y /= mapTileSize
            position.y = if (map == null) 0f else map.height - position.y

            val isTileObject: Boolean

            // if it's a template, the actual info will be located in that template file
            var objectActualInfo: JsonValue = objectInfo

            // used to store the template info root when the object is a template object, to be used in methods
            // to retrieve tileset info - it's in the root of the template file (and objectActualInfo is in a child of the root)
            var templateInfoRootJson: JsonValue? = null

            // find if it's a tile object -> needs to find if it has a "gid" attribute in the .json
            // if it's a template object, the attribute is in the template file, so parse that file
            if (objectInfo.get("template") == null) {
                isTileObject = objectActualInfo.get("gid") != null
            } else {
                val path = getTemplatePathFromObject(objectInfo)
                objectActualInfo = mapInfo.loadedTemplates.get(path)
                templateInfoRootJson = objectActualInfo
                objectActualInfo = objectActualInfo.get("object")

                isTileObject = objectActualInfo.get("gid") != null
            }

            val widthPixels = objectActualInfo.getInt("width")
            val heightPixels = objectActualInfo.getInt("height")

            if (onlyPreload) {
                if (isTileObject) {
                    getTileImageNameFromGIDInsideTileSet(objectInfo.getInt("gid"), jsonMapFile, templateInfoRootJson)
                }
            }
            else if (map != null && entityLoaderFromString != null) {
                val entity: Entity?
                val props = getCustomPropertiesFromJson(objectInfo)
                // is tile object?
                if (isTileObject) {
                    entity = loadTileObject(jsonMapFile, map, position, widthPixels, heightPixels, objectActualInfo, templateInfoRootJson, props)
                } else {
                    // Tiled objects have their origin on their top-left corner, fix that. (the origin is "correct" in tileObjects)
                    position.y -= heightPixels.toFloat() / Engine.PPM

                    entity = loadObject(map, position, widthPixels, heightPixels, objectActualInfo, props)
                }

                entity.ifNotNull { entity ->
                    entity.tiledEditorGroupNames = groupNames

                    entity.customProperties = props
                    if (savedMapInfoJson != null) {
                        loadEntityProps(entity)
                    }

                    // if specified, get depth from the custom property of the Entity; otherwise get it from the layer
                    if (entity.customProperties.ints.containsKey("depth")) {
                        entity.depth = entity.customProperties.ints.get("depth")
                    } else entity.depth = currentLayerDepth
                }
            }

            objectInfo = objectInfo.next
        }
    }

    private fun getTemplatePathFromObject(objectInfo: JsonValue): String {
        var templateFile = objectInfo.getString("template")
        val tileset = File(templateFile)
        templateFile = tileset.name // using File's getName() path modifiers are removed

        // parse .json template file
        val path = mapInfo.objectTemplatesJsonFolder + File.separator + templateFile
        // if it's the first time this template is accessed...
        if (!mapInfo.loadedTemplates.containsKey(path)) {
            val value = mapInfo.jsonReader.parse(Gdx.files.internal(path))
            mapInfo.loadedTemplates.put(path, value)
        }
        return path
    }

    private fun loadTileObject(jsonMapFile: JsonValue, map: EntityTiledMap, position: Vector2, widthPixels: Int, heightPixels: Int,
                               objectInfo: JsonValue, templateInfoRootJson: JsonValue?, objectProps: CustomProperties): Entity? {
        val GID = objectInfo.getInt("gid")
        if (GID == 0) throw RuntimeException("Entity with name: ${objectInfo.getString("name")} " +
                "in map named ${mapInfo.mapName} has GID with value 0 which means no Tile is associated with it, " +
                "but Entity is a Tiled Object")

        val tileImageName = getTileImageNameFromGIDInsideTileSet(GID, jsonMapFile, templateInfoRootJson)
        val objectName = objectInfo.getString("name")
        val entity = entityLoaderFromString!!.loadEntityFromTiledTileObject(tileImageName, objectName, position, widthPixels, heightPixels, map, objectProps)
        entity.ifNotNull { map.addEntity(it) }
        return entity
    }

    /**
     * @param objectInfo Object's name as configured in Tiled and saved in .json. If saved as a template object, the
     * name will be in the template file
     */
    private fun loadObject(map: EntityTiledMap, position: Vector2, widthPixels: Int, heightPixels: Int,
                           objectInfo: JsonValue, objectProps: CustomProperties): Entity? {
        val objectName = objectInfo.getString("name")
        val entity = entityLoaderFromString!!.loadEntityFromObjectName(objectName, position, widthPixels, heightPixels, map, objectProps)
        entity.ifNotNull { map.addEntity(it) }
        return entity
    }

    /**
     * Returns the image name of the tile with the GID provided.
     * Looks in the folder where tileSets are for the tileSet where that GID belongs
     *
     * @param objectTemplateInfoRootJson The .json info of the template file, if it's a template object.
     * If not an object (tile), pass null
     * @return Name of the image, without extension and path modifiers
     */
    private fun getTileImageNameFromGIDInsideTileSet(GID: Int, jsonMapFile: JsonValue, objectTemplateInfoRootJson: JsonValue?): String {
        val jsonTileSet: JsonValue? = findTileset(GID, jsonMapFile, objectTemplateInfoRootJson)
        val tilesetFile = tilesetFileName(GID, jsonTileSet)

        // get the local tileset image ID from the Global ID (GID) of the cell
        val localTileID = GID - jsonTileSet!!.getInt("firstgid")

        return mapInfo.loadedTileSets.get(tilesetFile).getImageNameFromTileId(localTileID)
    }

    private fun findTileset(GID: Int, jsonMapFile: JsonValue, objectTemplateInfoRootJson: JsonValue?): JsonValue? {
        var jsonTileSet: JsonValue? = null
        // find the tileset: if it's a object from a template, the tileset is in the template file
        // if not, the tileset is in one of the tileset array in the jsonMapFile
        if (objectTemplateInfoRootJson?.get("tileset") == null) {
            val jsonTileSetsInfo = jsonMapFile.get("tilesets")

            // iterate through all the tileSets of the map
            // using the "firstGID" property of each tileset, find the tileset that belongs to the GID provided
            var entry: JsonValue? = jsonTileSetsInfo.child
            while (entry != null) {
                if (GID >= entry.getInt("firstgid")) {
                    jsonTileSet = entry
                }
                entry = entry.next
            }
        } else {
            // template
            jsonTileSet = objectTemplateInfoRootJson.get("tileset")
        }

        return jsonTileSet
    }

    private fun tilesetFileName(GID: Int, jsonTileSet: JsonValue?): String {
        // get the name of the tileset file, without extension
        var tilesetFile = jsonTileSet!!.getString("source")
        val tileset = File(tilesetFile)
        val fileName = tileset.name // using File's getName() path modifiers are removed
        tilesetFile = Utils.removeExtensionFromFilename(fileName) // remove extension

        // if it's the first time this tileset is accessed, create a JsonTileSet
        if (!mapInfo.loadedTileSets.containsKey(tilesetFile)) {
            val tilesetLoader = JsonTileSet(mapInfo.buildTilesetFileName(tilesetFile), mapInfo.jsonReader)
            mapInfo.loadedTileSets.put(tilesetFile, tilesetLoader)
        }

        return tilesetFile
    }
}
