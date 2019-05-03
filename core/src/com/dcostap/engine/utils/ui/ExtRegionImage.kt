package com.dcostap.engine.utils.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Scaling
import com.dcostap.engine.utils.ifNotNull
import com.dcostap.engine.utils.tint

/**
 * Created by Darius on 01/04/2018.
 *
 * Allows scaling to a size on one axis, automatically scales the other axis to keep the aspect ratio
 */
open class ExtRegionImage(var region: TextureRegion) : Image(region) {
    var sizeWidth = 0
        set(value) {
            field = value
            width = value.toFloat()
        }

    var sizeHeight = 0
        set(value) {
            field = value
            height = value.toFloat()
        }

    init {
        region.ifNotNull {
            sizeWidth = it.regionWidth
            sizeHeight = it.regionHeight
        }
    }

    /** If image was scaled you will need to re-scale it again to adapt to new image */
    fun changeImage(texture: TextureRegion) {
        drawable = TextureRegionDrawable(texture)
        region = texture

        region.ifNotNull {
            sizeWidth = it.regionWidth
            sizeHeight = it.regionHeight
        }
    }

    var updateFunction: (delta: Float) -> Unit = {}

    fun scaleToWidth(width: Float) {
        sizeWidth = width.toInt()
        sizeHeight = (width * (region.regionHeight / region.regionWidth.toFloat())).toInt()
    }

    fun scaleToHeight(height: Float) {
        sizeHeight = height.toInt()
        sizeWidth = (height * (region.regionWidth / region.regionHeight.toFloat())).toInt()
    }

    /** Scale original texture's width and height by factor; Factor of 2 means double the size */
    fun scale(factor: Float) {
        sizeWidth = (region.regionWidth * factor).toInt()
        sizeHeight = (region.regionHeight * factor).toInt()
    }

    override fun getMinWidth(): Float {
        return sizeWidth.toFloat()
    }

    override fun getMinHeight(): Float {
        return sizeHeight.toFloat()
    }

    override fun getMaxWidth(): Float {
        return sizeWidth.toFloat()
    }

    override fun getMaxHeight(): Float {
        return sizeHeight.toFloat()
    }

    override fun act(delta: Float) {
        super.act(delta)

        updateFunction(delta)
    }

    var tint: Boolean = false
    var tintColor: Color = Color.WHITE

    override fun draw(batch: Batch?, parentAlpha: Float) {
        if (tint) {
            batch!!.tint(tintColor) {
                super.draw(batch, parentAlpha)
            }
        } else super.draw(batch, parentAlpha)
    }
}

class ExtImage(drawable: Drawable?) : Image(drawable) {
    var updateFunction: (delta: Float) -> Unit = {}

    constructor(region: TextureRegion) : this(TextureRegionDrawable(region))

    fun setTextureRegion(region: TextureRegion) {
        if (drawable is TextureRegionDrawable && (drawable as TextureRegionDrawable).region === region) return

        this.drawable = TextureRegionDrawable(region)
    }

    fun clearTextureRegion() {
        this.drawable = null
    }

    override fun act(delta: Float) {
        super.act(delta)

        updateFunction(delta)
    }
}