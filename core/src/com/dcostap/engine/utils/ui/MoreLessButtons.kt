package com.dcostap.engine.utils.ui

import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener

/**
 * Created by Darius on 10/03/2018.
 *
 *
 * Adds a Table with two buttons in the same column; one to increment and another to decrease something. Create
 * anonymous class and override the abstract methods
 */
abstract class MoreLessButtons constructor(skin: Skin, font: BitmapFont, buttonSize: Float = 20f) : Table() {
    private val moar: Button
    private val less: Button

    init {
        moar = ExtButton(skin, ExtLabel("+"))
        less = ExtButton(skin, ExtLabel("-"))

        add(moar).size(buttonSize)
        row()
        add(less).size(buttonSize)

        moar.addListener(object : ChangeListener() {
            override fun changed(event: ChangeListener.ChangeEvent, actor: Actor) {
                moreAction()
            }
        })

        less.addListener(object : ChangeListener() {
            override fun changed(event: ChangeListener.ChangeEvent, actor: Actor) {
                lessAction()
            }
        })
    }

    abstract fun moreAction()
    abstract fun lessAction()
}
