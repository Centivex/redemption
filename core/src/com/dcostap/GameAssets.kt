package com.dcostap

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.dcostap.engine.Assets
import com.dcostap.engine.map.map_loading.JsonMapLoader
import com.dcostap.engine.utils.font_loaders.FontSetSingle
import com.dcostap.engine.utils.font_loaders.FreeTypeFontLoader
import com.dcostap.engine.utils.font_loaders.smart_font_generator.SmartFontGenerator

class GameAssets : Assets() {
    val fontDarenAtlas = FontSetSingle(10)
    val fontDarenSAtlas = FontSetSingle(7)
    val fontEquipmentAtlas = FontSetSingle(16)

    lateinit var skin: Skin
        private set

    // fonts are stored in "skin's atlas"
    // skin's atlas is the game's main atlas, so all textures are from there
    // however fonts need also a .fnt file, so the skin has that information too
    // the fonts are the generated ones (which were previously the ones used), moved to workingAssets folder be packed within the game atlas

    // skin composer is still used, but the output atlas is ignored, and the .fnt and .atlas files used
    val fontDare get() = skin.getFont("darePixel_10")
    val fontDareS get() = skin.getFont("darePixelSmall_7")
    val fontEquipment get() = skin.getFont("equipment_16")
    val fontDareOutline get() = skin.getFont("darePixelOutline")

    override fun setupFontsToBeLoaded() {
        SmartFontGenerator.pageSize = 256
        val parameter = FreeTypeFontGenerator.FreeTypeFontParameter()
        parameter.color = Color.WHITE
        parameter.shadowColor = Color(0f, 0f, 0f, 0.28f)
        parameter.shadowOffsetX = 1
        parameter.shadowOffsetY = 1
        addFontLoader(FreeTypeFontLoader("fonts/true_type", "darepixel.ttf", fontDarenAtlas, "darePixel",
                parameter, false, false))
        addFontLoader(FreeTypeFontLoader("fonts/true_type", "darepixelsmall.ttf", fontDarenSAtlas, "darePixelSmall",
                parameter, false, false, lineHeight = -8))

        addFontLoader(FreeTypeFontLoader("fonts/true_type", "EquipmentExtended.ttf", fontEquipmentAtlas, "equipment",
                parameter, false, false))
    }

    override fun getDebugFont(): BitmapFont {
        return fontDare
    }

    override fun justFinishedAssetLoading() {
        Engine.pixelTexture = getTexture("pixel")
        skin = loadSkin("skins", "skin")

        fontDare.setFixedWidthGlyphs("1234567890")
        fontDareS.setFixedWidthGlyphs("1234567890")
//        fontEquipment.setFixedWidthGlyphs("1234567890")

//        fontDare.data.markupEnabled = true

        // pixelated scaling on the font
        fontDare.region.texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        fontDareS.region.texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        fontEquipment.region.texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)

        fontDarenAtlas.font.region.texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        fontDarenSAtlas.font.region.texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        fontEquipmentAtlas.font.region.texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)

        // add styles to the skin
        skin.add("default", Window.WindowStyle(fontDare, Color.BLACK, skin.getDrawable("back1")))
        skin.add("default", TextField.TextFieldStyle(fontDare, Color.BLACK, skin.getDrawable("verBarNude"),
                skin.getDrawable("verBarNude"), skin.getDrawable("button1")))
    }
}