package com.dcostap.engine.utils.font_loaders

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGeneratorLoader
import com.dcostap.engine.utils.font_loaders.smart_font_generator.SmartFontGenerator
import com.dcostap.Engine
import com.dcostap.printDebug

/**
 * Created by Darius on 04/01/2018
 *
 * Generates BitmapFonts dynamically from one TrueTypeFont. Sizes generated are determined from the FontSet object passed
 *
 * Can be configured to use density factor or resolution factor, so that input sizes are adapted to the factor
 */
class FreeTypeFontLoader(fontFolder: String, fontName: String, private val fontSet: FontSet,
                         /** Must be different for each generated font, so that each one is cached separated, even if loading from the same .ttf file
                          * That is, this variable gives a name to each generated font  */
                         private val fontIdentifier: String,
                         private val parameter: FreeTypeFontGenerator.FreeTypeFontParameter = defaultGeneratorParameter,
                         private val useDensityFactor: Boolean = false,
                         /** will scale up based on base resolution  */
                         private val useResolutionFactor: Boolean = true,
                         /** if using resolution factor, this width value will correspond to scale factor 1x  */
                         private val baseResolutionWidth: Int = 0,
                         /** (scaling based on fonts of size 16px) If the scaling factor is 1, means that in fonts of
                          * size 32 (double of 16) the border is double the size too. lower values lower the
                          * scaling factor (so bigger fonts get bigger increment of border width) but also
                          * lowers the overall border width sizes
                          *
                          * Setting it to 0 disables scaling based on font's size; so that border width is the same on all
                          * font sizes */
                         private val borderSizeScalingFactorAgainstFontSize: Float = 0f,
                         /** called **spaceY** in the freeType class. Distance between font's lines.
                          * It is scaled to the factor chosen in the loader, then scaled to
                          * each font's size, the base size being 20. If -1, parameter's default lineHeight will be unaffected. */
                         private val lineHeight: Int = 0)
    : FontLoaderBase(fontFolder, fontName)
{
    private lateinit var generator: FreeTypeFontGenerator

    private val useSmartFontGenerator = true

    private val originalBorderWidth = parameter.borderWidth

    /**
     * Loads the TrueType (.ttf) fonts
     */
    override fun loadFonts(assetManager: AssetManager) {
        // font loading
        assetManager.setLoader<FreeTypeFontGenerator, FreeTypeFontGeneratorLoader.FreeTypeFontGeneratorParameters>(FreeTypeFontGenerator::class.java, FreeTypeFontGeneratorLoader(InternalFileHandleResolver()))
        assetManager.load(fontLocation, FreeTypeFontGenerator::class.java)
    }

    override fun finishLoading(assetManager: AssetManager) {
        generator = assetManager.get(fontLocation, FreeTypeFontGenerator::class.java)
        FreeTypeFontGenerator.setMaxTextureSize(2048)
        generateFonts()
    }

    /**
     * Override on children to generate the fonts on specific sizes with specific config
     */
    private fun generateFonts() {
        printDebug("Loading font: $fontName; id: $fontIdentifier...")
        if (fontSet is FontSetFull) {
            val fontSetFull = fontSet
            fontSetFull.font_verySmall = generateFontFromSize(fontSetFull.verySmallSize)
            fontSetFull.font_small = generateFontFromSize(fontSetFull.smallSize)
            fontSetFull.font_medium = generateFontFromSize(fontSetFull.mediumSize)
            fontSetFull.font_big = generateFontFromSize(fontSetFull.bigSize)
            fontSetFull.font_veryBig = generateFontFromSize(fontSetFull.veryBigSize)
        }
        if (fontSet is FontSetNormal) {
            val fontSetNormal = fontSet
            fontSetNormal.font_small = generateFontFromSize(fontSetNormal.smallSize)
            fontSetNormal.font_medium = generateFontFromSize(fontSetNormal.mediumSize)
            fontSetNormal.font_big = generateFontFromSize(fontSetNormal.bigSize)
        }
        if (fontSet is FontSetSingle) {
            val fontSetSingle = fontSet
            fontSetSingle.font = generateFontFromSize(fontSetSingle.uniqueSize)
        }

        printDebug("_____")
    }

    /**
     * Overwrites the size of the parameter config, for ease of use when creating multiple fonts with different sizes
     */
    private fun generateFontFromSize(baseSize: Int): BitmapFont {
        printDebug("     size: " + baseSize)
        parameter.size = getSizeAdaptedToFactors(baseSize.toFloat()).toInt()
        parameter.borderWidth = getSizeAdaptedToFactors(originalBorderWidth) *
                (if (borderSizeScalingFactorAgainstFontSize == 0f) 1f else Math.pow((baseSize / 16f).toDouble(),
                        borderSizeScalingFactorAgainstFontSize.toDouble()).toFloat())
        //print("borderWidth:" + parameter.borderWidth + "factor: " + Engine.getResolutionFactor(baseResolutionWidth.toFloat()))
        if (lineHeight != 0) {
            parameter.spaceY = (getSizeAdaptedToFactors(lineHeight.toFloat()) * (baseSize / 20f)).toInt()
        }

        return if (useSmartFontGenerator) {
            SmartFontGenerator.loadFontOrGenerateIt(fontIdentifier + "_" + baseSize, generator, parameter)
        } else {
            generator.generateFont(parameter)
        }
    }

    /** adapts the base size to the factors chosen for this generator: no factors / adapted from base
     * resolution / adapted from density factor  */
    private fun getSizeAdaptedToFactors(baseSize: Float): Float {
        return (baseSize *
                if (useDensityFactor)
                    Engine.densityFactor
                else
                    if (useResolutionFactor) Engine.getResolutionFactor(baseResolutionWidth.toFloat())
                    else 1f
                )
    }

    companion object {
        val defaultGeneratorParameter: FreeTypeFontGenerator.FreeTypeFontParameter
            get() = getGeneratorParameterFromConfig(Color.WHITE, 0f, Color.WHITE)

        fun getGeneratorParameterFromConfig(fontColor: Color, baseBorderSize: Float, borderColor: Color)
                : FreeTypeFontGenerator.FreeTypeFontParameter
        {
            val parameter = FreeTypeFontGenerator.FreeTypeFontParameter()
            parameter.borderWidth = baseBorderSize
            parameter.borderColor = borderColor
            parameter.color = fontColor

            return parameter
        }
    }
}
