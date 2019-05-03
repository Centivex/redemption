package com.dcostap.engine.utils.font_loaders

import com.badlogic.gdx.graphics.g2d.BitmapFont

/**
 * Created by Darius on 04/01/2018
 */
class FontSetSingle(uniqueSize: Int = 20) : FontSet {
    lateinit var font: BitmapFont

    // default sizes
    var uniqueSize = uniqueSize
        private set
}
