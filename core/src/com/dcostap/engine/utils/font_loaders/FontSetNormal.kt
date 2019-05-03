package com.dcostap.engine.utils.font_loaders

import com.badlogic.gdx.graphics.g2d.BitmapFont

/**
 * Created by Darius on 04/01/2018
 */
class FontSetNormal(smallSize: Int = 16, mediumSize: Int = 20, bigSize: Int = 26) : FontSet {
    lateinit var font_small: BitmapFont
    lateinit var font_medium: BitmapFont
    lateinit var font_big: BitmapFont

    // default sizes
    var smallSize = smallSize
        private set
    var mediumSize = mediumSize
        private set
    var bigSize = bigSize
        private set
}
