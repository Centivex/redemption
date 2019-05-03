package com.dcostap.engine.utils.font_loaders

import com.badlogic.gdx.assets.AssetManager

/**
 * Created by Darius on 04/01/2018
 */
interface FontLoader {
    /**
     * uses the libgdx AssetManager to load the font files. Call before finishing loading assets with AssetManager!
     */
    fun loadFonts(assetManager: AssetManager)

    /**
     * actually retrieves the final font / fonts, applying any configuration.
     *
     * call after loading the font files in the assetManager!
     */
    fun finishLoading(assetManager: AssetManager)
}
