package com.dcostap.engine.utils

import com.badlogic.gdx.graphics.g2d.TextureRegion
import ktx.collections.GdxMap

/** Created by Darius on 27-Feb-19. */
class Tileset(val mainImage: TextureRegion, val width: Int, val height: Int, val offsetX: Int = 0, val offsetY: Int = 0) {
    val tiles = GdxMap<Int, TextureRegion>()
    var tileNumber: Int = 0
        private set

    init {
        var x = offsetX; var y = offsetY
        var i = 0
        while (true) {
            val region = TextureRegion(mainImage.texture, mainImage.regionX + x, mainImage.regionY + y, width, height)
            tiles.put(i, region)
            i++
            x += width
            if (x + width > mainImage.regionWidth) {
                x = 0
                y += height
                if (y + height > mainImage.regionHeight) {
                    break
                }
            }
        }

        tileNumber = i
    }
}