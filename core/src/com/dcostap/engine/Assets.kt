package com.dcostap.engine

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.ObjectSet
import com.dcostap.engine.utils.Utils
import com.dcostap.engine.utils.font_loaders.FontLoader
import com.dcostap.printDebug
import ktx.collections.GdxArray
import ktx.collections.GdxMap

/**
 * Created by Darius on 11/11/2017.
 */
abstract class Assets(val textureAtlasFilename: String = "atlas/atlas.atlas") : Disposable {
    lateinit var textureAtlas: TextureAtlas
        private set
    lateinit var assetManager: AssetManager
        private set

    private var firstTimeLoadingFonts = true
    private var initialTimeLoadingFonts = 0f

    private val fontLoaders = Array<FontLoader>()

    abstract fun getDebugFont(): BitmapFont

    /** Scales the graphics of a Drawable inside skin; use with a scaling factor
     *
     * (for example resolution factor to adapt to app's resolution: this avoids that the graphics of buttons / windows, etc
     *  get really small on phones with high resolution) */
    fun scaleNinePatchDrawable(skin: Skin, drawableName: String, scale: Float) {
        val drawable = skin.getDrawable(drawableName)
        if (drawable is NinePatchDrawable) {
            drawable.patch.scale(scale, scale)
            drawable.patch = drawable.patch
        }
    }

    /** Call first to setup the Asset Manager loading.
     * After that you can use the Asset Manager to load the assets in another thread
     *
     * Override to add the fonts Assets will load, then call this method (super.()) */
    fun initAssetLoading() {
        assetManager = AssetManager()
        assetManager.load(textureAtlasFilename, TextureAtlas::class.java)

        setupFontsToBeLoaded()
        loadFontsToAssetManager()
    }

    internal abstract fun setupFontsToBeLoaded()

    internal fun addFontLoader(fontLoader: FontLoader) {
        fontLoaders.add(fontLoader)
    }

    /** call before finishing loading assetManager */
    private fun loadFontsToAssetManager() {
        // load fonts from asset manager
        for (fontLoader in fontLoaders) {
            fontLoader.loadFonts(assetManager)
        }
    }

    /** Finishes both Asset Manager and own Assets loading. Override to add more things to load, like adding styles to the
     * Skin
     * @return whether it finished loading
     */
    open fun processAssetLoading(): Boolean {
        if (assetManager.update()) {
            if (!finishLoadingFonts()) return false

            this.textureAtlas = assetManager.get(textureAtlasFilename, TextureAtlas::class.java)
            setupTextureAtlas()

            justFinishedAssetLoading()
            return true
        }

        return false
    }

    fun reloadTextureAtlas() {
        textureAtlas = TextureAtlas(textureAtlasFilename)
        cachedIndividualTexturesInsideGroups.clear()
        cachedTextureGroups.clear()
        cachedTextures.clear()
        setupTextureAtlas()
    }

    open fun setupTextureAtlas() {
        val regions = GdxMap<String, Int>()
        for (region in textureAtlas.regions) {
            regions.put(region.name, regions.get(region.name, -1) + 1)
        }

        for (entry in regions.entries()) {
            for (i in 0..entry.value) {
                val finalName = entry.key + "_$i"
                cachedIndividualTexturesInsideGroups.put(finalName, IndividualTextureInsideGroup(entry.key, i))
            }
        }
    }

    open fun loadSkin(skinFolder: String = "skins", skinName: String = "skin"): Skin {
        val skin = Skin()

//        skin.addRegions(TextureAtlas(Gdx.files.internal("$skinFolder/$skinName.atlas")))
        skin.addRegions(textureAtlas)
        skin.load(Gdx.files.internal("$skinFolder/$skinName.json"))
        return skin
    }

    abstract fun justFinishedAssetLoading()

    /** Call after finishing loading assetManager
     * Will load one font at a time, so the program doesn't hang for too long. (Causes problems on Android)
     * @return true when it finished */
    private fun finishLoadingFonts(): Boolean {
        if (firstTimeLoadingFonts) {
            firstTimeLoadingFonts = false
            initialTimeLoadingFonts = System.currentTimeMillis().toFloat()
            printDebug("____\nStarted loading fonts")
        }

        if (fontLoaders.size > 0) {
            fontLoaders.get(0).finishLoading(assetManager)
            fontLoaders.removeIndex(0)
            return false
        } else {
            printDebug("Finished loading fonts; elapsed: "
                    + (System.currentTimeMillis() - initialTimeLoadingFonts) / 1000f + "s\n____")
            return true
        }
    }

    /**
     * Finds image in the atlas ignoring extensions. If the name ends with "_#" it loads it from the array that texturePacker creates
     */
    fun findRegionFromRawImageName(rawImageName: String): TextureRegion {
        Utils.removeExtensionFromFilename(rawImageName)

        val i = rawImageName.lastIndexOf('_')
        if (i != -1) {
            try {
                val index = Integer.valueOf(rawImageName.substring(i + 1))!!
                val realName = rawImageName.substring(0, i)
                return textureAtlas.findRegion(realName, index)
            } catch (exception: Exception) {

            }
        }

        return getTexture(rawImageName)
    }

    private class IndividualTextureInsideGroup(val groupName: String, val index: Int)

    private val cachedTextures = HashMap<String, TextureAtlas.AtlasRegion>()
    private val cachedTextureGroups = HashMap<String, GdxArray<TextureAtlas.AtlasRegion>>()

    /** When textures end with _# they are treated as a group by texturePacker. Original names are cached here
     * so if you access a texture by its original name you still get the result. This helps with Tiled exported maps, which
     * will refer to images in tilesets with their original name.
     *
     * This might give unexpected behavior if there are  multiple textures with same base name ("spr.png" & "spr_0.png") */
    private val cachedIndividualTexturesInsideGroups = HashMap<String, IndividualTextureInsideGroup>()

    fun getTexture(name: String): TextureAtlas.AtlasRegion {
        cachedIndividualTexturesInsideGroups.get(name)?.also {
            return getTextures(it.groupName)[it.index]
        }

        return cachedTextures.getOrElse(name) {
            val texture = textureAtlas.findRegion(name) ?: throw RuntimeException("Region not found on atlas, name: $name\nAll regions: ${getRegionNames()}")
            cachedTextures.put(name, texture)
            texture
        }
    }

    fun getTextures(name: String): GdxArray<TextureAtlas.AtlasRegion> {
        return cachedTextureGroups.getOrElse(name) {
            val atlasRegions = textureAtlas.findRegions(name)
            if (atlasRegions.size == 0) throw RuntimeException("Group of regions ( .findRegions() ) not found on atlas, name: $name")
            cachedTextureGroups.put(name, atlasRegions)
            return atlasRegions
        }
    }

    private fun getRegionNames(): String {
        val names = ObjectSet<String>()
        for (region in textureAtlas.regions) {
            names.add(region.name)
        }
        return names.toString("; ")
    }

    override fun dispose() {
        assetManager.dispose()
    }
}
