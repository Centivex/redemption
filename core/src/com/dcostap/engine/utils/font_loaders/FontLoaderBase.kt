package com.dcostap.engine.utils.font_loaders

import com.badlogic.gdx.assets.AssetManager

/**
 * Created by Darius on 04/01/2018
 */
abstract class FontLoaderBase(fontFolder: String, val fontName: String) : FontLoader {
    protected val fontLocation: String = fontFolder + "/" + fontName

    abstract override fun loadFonts(assetManager: AssetManager)

    abstract override fun finishLoading(assetManager: AssetManager)
}