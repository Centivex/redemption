package com.dcostap.engine.utils

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Align
import com.dcostap.Engine

/**
 * Created by Darius on 14/09/2017.
 *
 * Helper class to draw stuff in a specific viewport. By default it adapts textures drawn to the global [Engine.PPM].
 *
 * Use it with a **scaling viewport** with arbitrary units (so 1 unit = PPM pixels):
 *
 * * Automatically scales all textures to the PPM, so when drawing 1 pixel isn't 1 unit
 *
 * Note: you shouldn't draw fonts in scaling viewports, do it in another viewport
 *
 * Use it with a **ScreenViewport** that adjusts to different resolutions using DensityFactor
 * (so higher resolution != smaller images, and so physical size of drawn things stays the same):
 *
 * * Use drawScaled to draw images that scale to comply with DensityFactor, based on an initial size
 *
 * * Draw normally with a font, if that font is generated in Assets with size based on DensityFactor
 *
 * If you use **Scene2d**, adapt to density factor this way:
 *
 * * use stage's actor (ExtRegionImage) to draw images, will work the same way as using drawScaled
 *
 * * with 9patches (button / pane / widget graphics), create them with a base size multiplied by Engine.getDensityFactor
 *
 * * use fonts the same way as before :D
 *
 * * If you don't want it to scale textures based on the PPM** (scaling viewport with no arbitrary units),
 * change the global [Engine.PPM] to 1
 */
class GameDrawer(val batch: Batch) {
    var alpha = 1f
        set(value) {
            field = value
            updateDrawingColorAndAlpha()
        }

    /** won't modify alpha */
    var color = Color.WHITE
        set(value) {
            field = value
            updateDrawingColorAndAlpha()
        }

    /** won't modify alpha */
    fun setColor(r: Float, g: Float, b: Float) {
        color.r = r
        color.g = g
        color.b = b

        updateDrawingColorAndAlpha()
    }

    /** modifies alpha too */
    fun setColorAndAlpha(r: Float, g: Float, b: Float, a: Float) {
        color.r = r
        color.g = g
        color.b = b
        alpha = a

        updateDrawingColorAndAlpha()
    }

    fun resetColor() {
        this.color = Color.WHITE
        updateDrawingColorAndAlpha()
    }

    fun resetAlpha() {
        this.alpha = 1f
        updateDrawingColorAndAlpha()
    }

    fun resetColorAndAlpha() {
        this.resetAlpha()
        this.resetColor()
    }

    private fun updateDrawingColorAndAlpha() = batch.setColor(color.r, color.g, color.b, alpha)

    private var dummyVector2 = Vector2()

    var drawingOffset = Vector2(0f, 0f)

    fun setDrawingOffsetPixels(x: Float, y: Float) {
        drawingOffset.set(pixelsToUnits(x), pixelsToUnits(y))
    }
    fun setDrawingOffsetPixelsXY(xy: Float) {
        drawingOffset.set(pixelsToUnits(xy), pixelsToUnits(xy))
    }

    fun resetDrawingOffset() {
        drawingOffset.set(0f, 0f)
    }

    /** Local scaleX, used when no scaleX is specified on the draw methods */
    var scaleX = 1f

    /** Local scaleY, used when no scaleY is specified on the draw methods */
    var scaleY = 1f

    fun setScaleXY(value: Float) {
        scaleX = value
        scaleY = value
    }

    var originX = 0f
    var originY = 0f

    /** Local rotation, used when no rotation is specified on the draw methods */
    var rotation = 0f

    /** Resets local scale and rotation to the default values */
    fun resetModifiers() {
        scaleX = 1f; scaleY = 1f
        rotation = 0f
        originX = 0f; originY = 0f
    }

    fun reset() {
        resetDrawingOffset()
        resetColorAndAlpha()
        resetModifiers()
    }

    fun getFinalDrawingX(x: Float): Float {
        return x + drawingOffset.x
    }

    fun getFinalDrawingY(y: Float): Float {
        return y + drawingOffset.y
    }

    var useCustomPPM = false
    var customPPM = 16

    /** the actual PPM value used by this GameDrawer. Will be [Engine.PPM] if [useCustomPPM] is false */
    val usedPPM get() = if (useCustomPPM) customPPM else Engine.PPM

    /** returns the amount the texture needs to be scaled to be drawn according to the Pixels Per Meter (PPM) constant **/
    fun getUnitWidth(textureRegion: TextureRegion) = textureRegion.regionWidth / usedPPM.toFloat()

    fun getUnitHeight(textureRegion: TextureRegion) = textureRegion.regionHeight / usedPPM.toFloat()

    /**
     * @param rotation in degrees.
     *
     * @param flipX flips the textureRegion, alternative to negative [scaleX].
     * @param flipY flips the textureRegion, alternative to negative [scaleY].
     *
     * @param displaceX displaces the texture by its entire width. If 1 to the right, if -1 to the left, 0 disables it.
     * Won't reset any previous displacement.
     *
     * @param displaceY see [displaceX]
     *
     * @param centerOriginOnXAxis sets the origin to the center of the texture.
     * Note this only affects transformations (Scaling & rotation happen around the origin).
     *
     * @param centerOriginOnYAxis see [centerOriginOnXAxis]
     *
     * @param centerOnXAxis offsets the drawing by half of its width so the origin of the position X is in the middle of the texture, thus
     * the texture is being drawn centered. Note that if true the previous displacement is temporarily reset
     * @param centerOnYAxis see [centerOnXAxis]
     */
    @JvmOverloads fun draw(textureRegion: TextureRegion, x: Float, y: Float, scaleX: Float = this.scaleX, scaleY: Float = this.scaleY,
             rotation: Float = this.rotation,
             originX: Float = this.originX, originY: Float = this.originY,
             flipX: Boolean = false, flipY: Boolean = false, displaceX: Int = 0, displaceY: Int = 0,
             customWidth: Float = -1f, customHeight: Float = -1f,
             centerOnXAxis: Boolean = false, centerOnYAxis: Boolean = false,
             centerOriginOnXAxis: Boolean = false, centerOriginOnYAxis: Boolean = false)
    {
        textureRegion.flip(flipX, flipY)
        val width = if (customWidth != -1f) customWidth else getUnitWidth(textureRegion)
        val height = if (customHeight != -1f) customHeight else getUnitHeight(textureRegion)

        val previousOffset = dummyVector2
        previousOffset.set(drawingOffset)

        if (centerOnXAxis || centerOnYAxis) resetDrawingOffset()

        if (displaceX != 0 || displaceY != 0) {
            if (displaceX != 0) drawingOffset.x += (width * displaceX)
            if (displaceY != 0) drawingOffset.y += (height * displaceY)
        }

        if (centerOnXAxis) drawingOffset.x += -width / 2f
        if (centerOnYAxis) drawingOffset.y += -height / 2f

        val thisX = getFinalDrawingX(x)
        val thisY = getFinalDrawingY(y)

        batch.draw(textureRegion, thisX, thisY, if (!centerOriginOnXAxis) originX else (width / 2f),
                if (!centerOriginOnYAxis) originY else (height / 2f), width, height, scaleX, scaleY, rotation)

        drawingOffset.set(previousOffset)
        textureRegion.flip(flipX, flipY)
    }

    @JvmOverloads fun draw(textureRegion: TextureRegion, position: Vector2, scaleX: Float = this.scaleX, scaleY: Float = this.scaleY,
                           rotation: Float = this.rotation,
                           originX: Float = this.originX, originY: Float = this.originY,
                           flipX: Boolean = false, flipY: Boolean = false, displaceX: Int = 0, displaceY: Int = 0,
                           customWidth: Float = -1f, customHeight: Float = -1f,
                           centerOnXAxis: Boolean = false, centerOnYAxis: Boolean = false,
                           centerOriginOnXAxis: Boolean = false, centerOriginOnYAxis: Boolean = false)
    {
        this.draw(textureRegion, position.x, position.y, scaleX, scaleY, rotation, originX, originY, flipX, flipY,
                displaceX, displaceY, customWidth, customHeight, centerOnXAxis, centerOnYAxis, centerOriginOnXAxis, centerOriginOnYAxis)
    }

    @JvmOverloads fun drawCentered(textureRegion: TextureRegion, x: Float, y: Float, scaleX: Float = this.scaleX, scaleY: Float = this.scaleY,
                     rotation: Float = this.rotation,
                     originX: Float = this.originX, originY: Float = this.originY,
                     flipX: Boolean = false, flipY: Boolean = false, displaceX: Int = 0, displaceY: Int = 0,
                     customWidth: Float = -1f, customHeight: Float = -1f,
                     centerOnXAxis: Boolean = true, centerOnYAxis: Boolean = true) {
        this.draw(textureRegion, x, y, scaleX, scaleY, rotation, originX, originY, flipX, flipY, displaceX, displaceY, customWidth, customHeight,
                centerOnXAxis, centerOnYAxis)
    }

    @JvmOverloads fun drawWithOriginOnCenter(textureRegion: TextureRegion, x: Float, y: Float, scaleX: Float = this.scaleX, scaleY: Float = this.scaleY,
                               rotation: Float = this.rotation, originX: Float = this.originX, originY: Float = this.originY,
                               flipX: Boolean = false, flipY: Boolean = false, displaceX: Int = 0, displaceY: Int = 0,
                               customWidth: Float = -1f, customHeight: Float = -1f,
                               centerOriginOnXAxis: Boolean = true, centerOriginOnYAxis: Boolean = true) {
        this.draw(textureRegion, x, y, scaleX, scaleY, rotation, originX, originY, flipX, flipY, displaceX, displaceY, customWidth, customHeight,
                centerOriginOnXAxis = centerOriginOnXAxis, centerOriginOnYAxis = centerOriginOnYAxis)
    }

    @JvmOverloads fun drawCenteredWithOriginOnCenter(textureRegion: TextureRegion, x: Float, y: Float, scaleX: Float = this.scaleX, scaleY: Float = this.scaleY,
                                       rotation: Float = this.rotation, originX: Float = this.originX, originY: Float = this.originY,
                                       flipX: Boolean = false, flipY: Boolean = false, displaceX: Int = 0, displaceY: Int = 0,
                                       customWidth: Float = -1f, customHeight: Float = -1f,
                                       centerOnXAxis: Boolean = true, centerOnYAxis: Boolean = true,
                                       centerOriginOnXAxis: Boolean = true, centerOriginOnYAxis: Boolean = true) {
        this.draw(textureRegion, x, y, scaleX, scaleY, rotation, originX, originY, flipX, flipY, displaceX, displaceY, customWidth, customHeight,
                centerOnXAxis, centerOnYAxis, centerOriginOnXAxis, centerOriginOnYAxis)
    }

    /**
     * Draws an image scaled to the nearest non-decimal scale factor, based on the densityFactor / resolutionFactor of the app
     * This means that the images will -almost- always have the same physical size on all devices
     *
     * Use it with ScreenViewport - since with a scaling viewport the above can't be achieved
     * @param useDensityFactor if false will use resolution factor
     */
    fun drawScaled(textureRegion: TextureRegion, x: Float, y: Float, baseSize: Float, useDensityFactor: Boolean,
                   resolutionFactorBaseWidth: Int) {
        val thisX = getFinalDrawingX(x)
        val thisY = getFinalDrawingY(y)

        val scaleFactor = getImageScaleFactor(baseSize, textureRegion.regionWidth.toFloat(),
                textureRegion.regionHeight.toFloat(), useDensityFactor, resolutionFactorBaseWidth)

        batch.draw(textureRegion, thisX, thisY, (textureRegion.regionWidth * scaleFactor).toFloat(),
                (textureRegion.regionHeight * scaleFactor).toFloat())
    }

    @JvmOverloads fun drawText(text: String, x: Float, y: Float, font: BitmapFont, color: Color = this.color, scaleX: Float = 1f, scaleY: Float = 1f,
                 hAlign: Int = Align.left, targetWidth: Float = 0f, wrap: Boolean = false) {
        val oldColor = font.color
        font.color = color

        val oldScaleX = font.getScaleX()
        val oldScaleY = font.getScaleY()
        val usedIntegers = font.usesIntegerPositions()
        font.setUseIntegerPositions(false)
        font.getData().setScale((1f / usedPPM) * scaleX, (1f / usedPPM) * scaleY)

        font.draw(batch, text, x, y, targetWidth, hAlign, wrap)

        font.getData().setScale(oldScaleX, oldScaleY)
        font.setUseIntegerPositions(usedIntegers)
        font.color = oldColor
    }

    fun drawText(text: String, x: Float, y: Float, font: BitmapFont, color: Color = this.color, scaleXY: Float = 1f, hAlign: Int = Align.left,
                 targetWidth: Float = 0f, wrap: Boolean = false) {
        this.drawText(text, x, y, font, color, scaleXY, scaleXY, hAlign, targetWidth, wrap)
    }

    /**
     * Draws a Rectangle using "pixel" image on atlas
     *
     * @param x         bottom-left corner x
     * @param y         bottom-left corner y
     * @param thickness size of the borders if the rectangle is not filled
     * @param fill      whether the rectangle drawn will be filled with the color
     */
    @JvmOverloads fun drawRectangle(x: Float, y: Float, width: Float, height: Float, fill: Boolean, thickness: Float = 0.13f,
                                    originX: Float = this.originX, originY: Float = this.originY,
                                    scaleX: Float = this.scaleX, scaleY: Float = this.scaleY, rotation: Float = this.rotation) {
        val thisX = getFinalDrawingX(x)
        val thisY = getFinalDrawingY(y)

        if (!fill) {
            batch.draw(Engine.pixelTexture, thisX, thisY, originX, originY, width, thickness, scaleX, scaleY, rotation)
            batch.draw(Engine.pixelTexture, thisX, thisY, originX, originY, thickness, height, scaleX, scaleY, rotation)
            batch.draw(Engine.pixelTexture, thisX, thisY + height - thickness, originX, originY, width, thickness, scaleX, scaleY, rotation)
            batch.draw(Engine.pixelTexture, thisX + width - thickness, thisY, originX, originY, thickness, height, scaleX, scaleY, rotation)
        } else {
            batch.draw(Engine.pixelTexture, thisX, thisY, originX, originY, width, height, scaleX, scaleY, rotation)
        }
    }

    fun drawRectangle(rectangle: Rectangle, thickness: Float, fill: Boolean) {
        this.drawRectangle(rectangle.x, rectangle.y, rectangle.width, rectangle.height, fill, thickness)
    }

    fun drawCross(x: Float, y: Float, axisSize: Float, thickness: Float) {
        this.drawLine(x - axisSize, y, x + axisSize, y, thickness)
        this.drawLine(x, y - axisSize, x, y + axisSize, thickness)
    }

    /**
     * Draws a line using "pixel" image on atlas. Lines drawn together will not be correctly joined.
     *
     * @param x1        start x
     * @param y1        start y
     * @param x2        end x
     * @param y2        end y
     * @param thickness size of the line
     */
    @JvmOverloads fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, thickness: Float, isArrow: Boolean = false, arrowSize: Float = 0.5f) {
        val thisX1 = getFinalDrawingX(x1)
        val thisX2 = getFinalDrawingX(x2)
        val thisY1 = getFinalDrawingY(y1)
        val thisY2 = getFinalDrawingY(y2)

        val dx = thisX2 - thisX1
        val dy = thisY2 - thisY1
        val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        val deg = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
        batch.draw(Engine.pixelTexture, thisX1, thisY1, 0f, thickness / 2f, dist, thickness, 1f, 1f, deg)

        if (isArrow) {
            val angle = Math . toRadians (Utils.getAngleBetweenPoints(x1, y1, x2, y2).toDouble())
            val angleDiff = -40
            val angle1 = angle +angleDiff
            val angle2 = angle -angleDiff
            drawLine(x2, y2, (x2 + (Math.cos(angle1) * arrowSize)).toFloat(), (y2 + (Math.sin(angle1) * arrowSize)).toFloat(), thickness, isArrow = false)
            drawLine(x2, y2, (x2 + (Math.cos(angle2) * arrowSize)).toFloat(), (y2 + (Math.sin(angle2) * arrowSize)).toFloat(), thickness, isArrow = false)
        }
    }

    fun drawLine(start: Vector2, end: Vector2, thickness: Float, isArrow: Boolean = false, arrowSize: Float = 0.5f) {
        this.drawLine(start.x, start.y, end.x, end.y, thickness, isArrow, arrowSize)
    }

    fun drawLineFromAngle(x1: Float, y1: Float, distance: Float, angleDegrees: Float, thickness: Float) {
        this.drawLine(x1, y1, ((x1 + distance * Math.cos(Math.toRadians(angleDegrees.toDouble()))).toFloat()),
                ((y1 + distance * Math.sin(Math.toRadians(angleDegrees.toDouble()))).toFloat()), thickness)
    }

    companion object {
        /**
         * Gets the nearest non-decimal scale factor for the image, based on the densityFactor / resolutionFactor of the app
         * @param useDensityFactor if false will use resolution factor
         */
        fun getImageScaleFactor(size: Float, imageWidthPixels: Float, imageHeightPixels: Float,
                                useDensityFactor: Boolean = true, resolutionFactorBaseWidth: Int): Int
        {
            val pixels: Float =
                    if (useDensityFactor)
                        Engine.densityFactor * size
                    else
                        Engine.getResolutionFactor(resolutionFactorBaseWidth.toFloat())

            return Math.round(pixels / Math.max(imageHeightPixels, imageWidthPixels))
        }
    }
}