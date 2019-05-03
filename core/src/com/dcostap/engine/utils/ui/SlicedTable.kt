package com.dcostap.engine.utils.ui

import com.badlogic.gdx.scenes.scene2d.ui.Table

/** Created by Darius on 04-Mar-19. */
class SlicedTable : Table() {
    val upTable: Table
    val centerLeftTable: Table
    val centerRightTable: Table
    val downTable: Table

    init {
        setFillParent(true)

        upTable = Table()
        centerLeftTable = Table()
        centerRightTable = Table()
        downTable = Table()

        add(upTable).expandX().top().colspan(2)
        row()
        add(centerLeftTable).expand().left()
        add(centerRightTable).expand().right()
        row()
        add(downTable).expandX().bottom().colspan(2)
    }
}