package com.dcostap.engine.utils.ui

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Table

/** Created by Darius on 08-Sep-18.
 *
 * A Table which needs to be added as Actor to the stage itself. (addActor()).
 * Then add _only_ one child to this Table (.add())
 *
 * Now you can move this table around with .setPosition() (like you normally would with any Actor added with addActor()).
 * But this Table will also automatically resize the child, unlike what would normally happen with a Table added with addActor().
 * Add all the content to the child.
 *
 * Basically you can have a floating Table / Window / whatever (with arbitrary position) which also has its size taken care
 * of by this parent Table. So the contents of that child define the size and any change will make the parent Table automatically resize.
 *
 * Take into account that in UI design draggable windows usually never change size,
 * so the standard behavior of a Table / Window added with addActor works fine for those cases.
 * In a normal Window, just add the contents and call pack() once when finished
 *
 * Solves this problem:
 *
 * I wanna have the resizing that comes when you add children to a Table, but without the position constraints
 * (because when you add children to a Table, that Table will take care of the sizing)
 *
 * If I understood how exactly Table takes care of that, I could replicate that in the Window that has no table parent.
 * It has something to do with invalidate, validate, pack etc
 * The other option is calling pack each frame... it works
 * */
open class ResizableActorTable(private var invisibleForFrames: Int = 0) : ExtTable() {
    override fun <T : Actor?> add(actor: T): Cell<T> {
        if (hasChildren()) throw RuntimeException("Adding more than one child to a ResizableActorTable. Add maximum one to this class")

        return super.add(actor)
    }

    override fun act(delta: Float) {
        super.act(delta)

        if (invisibleForFrames > 0) {
            invisibleForFrames--
        }

        if (children.size > 0) {
            val child = children[0]
            setSize(child.width, child.height)
        }
    }

    override fun setPosition(x: Float, y: Float) {
        super.setPosition(x - width / 2f, y - height / 2f)
    }

    override fun setPosition(x: Float, y: Float, alignment: Int) {
        super.setPosition(x - width / 2f, y - height / 2f, alignment)
    }

    override fun drawBackground(batch: Batch?, parentAlpha: Float, x: Float, y: Float) {
        if (invisibleForFrames > 0) return
        super.drawBackground(batch, parentAlpha, x, y)
    }
}