package com.dcostap.engine.utils.font_loaders

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.BitmapFontLoader.BitmapFontParameter
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont

/**
 * Created by Darius on 04/01/2018
 *
 *
 * Loads one BitmapFont
 */
class BitmapFontLoader(fontFolder: String, fontName: String, private val fontSetSingle: FontSetSingle)
    : FontLoaderBase(fontFolder, fontName)
{
    lateinit var font: BitmapFont
        private set

    override fun loadFonts(assetManager: AssetManager) {
        val parameter = BitmapFontParameter()
        parameter.magFilter = Texture.TextureFilter.Linear
        parameter.minFilter = Texture.TextureFilter.Linear

        assetManager.load(fontLocation, BitmapFont::class.java, parameter)
    }

    override fun finishLoading(assetManager: AssetManager) {
        font = assetManager.get(fontLocation, BitmapFont::class.java)
        fontSetSingle.font = font
    }
}
