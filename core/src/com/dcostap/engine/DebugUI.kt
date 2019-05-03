package com.dcostap.engine

import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.utils.Pool
import com.badlogic.gdx.utils.Pools
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.gdx.utils.viewport.Viewport
import com.dcostap.Engine
import com.dcostap.engine.utils.*
import com.dcostap.engine.utils.ui.ResizableActorTable
import com.dcostap.engine.utils.screens.BaseScreen
import com.dcostap.engine.utils.ui.ExtLabel
import com.dcostap.engine.utils.ui.ExtTable
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.VisTextButton
import ktx.actors.onChange
import ktx.collections.GdxArray
import java.util.*

/**
 * Created by Darius on 28/12/2017.
 *
 * Used for easier debugging. Allows to quickly add Actors to a stage which is rendered above everything else.
 * Can be accessed statically via [Engine].
 *
 * Stage uses ScreenViewport. Has utility methods to quickly add [DebugActor] which follow a
 * position in another viewport, but it is actually positioned in this Stage. Utility methods to add those [DebugActor]
 * with built-in animation or quickly add a Label. Has access to a default statically accessed debug Font for the text.
 *
 * For custom Actor adding, a [ResizableActorTable] is a good easy way of adding Actors or Widgets, while being positioned in arbitrary
 * positions - not following Table's layout (like a Window).
 */
class DebugUI(val engine: Engine) : ScreenAdapter() {
    val stage = Stage(ScreenViewport())

    val debugFont get() = engine.assets.getDebugFont()

    private var debugCommands: ExtTable? = null
    private var debugField: TextField? = null

    fun openCloseDebugCommandsWindow() {
        // debug command window, press enter to activate
        debugCommands.ifNotNull {
            it.clear()
            it.remove()
            engine.runDebugCommand(debugField!!.text)
            debugField = null
            debugCommands = null
        }.ifNull {
            stage.addActor(ExtTable().also {
                debugCommands = it
                it.setFillParent(true)
                it.center()
                debugField = TextField("", VisUI.getSkin())
                it.add(debugField)
                stage.keyboardFocus = debugField
                debugField?.text = ""
            })
        }
    }

    private var debugWindow: Window? = null
    fun openDebugWindow() {
        ExtLabel.defaultFont = VisUI.getSkin().getFont("default-font")
        ExtLabel.defaultColor = Color.WHITE
        val table = ExtTable()
        val window = object : Window("debug window", VisUI.getSkin()) {
            init {
                debugWindow = this
                isMovable = true

                background = NinePatchDrawable(NinePatch((VisUI.getSkin().getDrawable("window") as NinePatchDrawable).patch).also {
                    it.color.a = 0.85f
                })

                row()

                add(table.also {
                    it.defaults().left()
                    it.add(VisTextButton("hide window").also {
                        it.onChange {engine.debugWindow = !engine.debugWindow}
                    })

                    ExtLabel.defaultFont = VisUI.getSkin().getFont("small-font")
                    it.row().padTop(10f)

                    it.add(Utils.visUI_customCheckBox("DEBUG_MAP_CELLS", Engine.DEBUG_MAP_CELLS).also {
                        it.onChange {
                            Engine.DEBUG_MAP_CELLS = !Engine.DEBUG_MAP_CELLS
                        }
                    })

                    it.row()

                    it.add(Utils.visUI_customCheckBox("DEBUG_ENTITIES_BB", Engine.DEBUG_ENTITIES_BB).also {
                        it.onChange {
                            Engine.DEBUG_ENTITIES_BB = !Engine.DEBUG_ENTITIES_BB
                        }
                    })

                    it.row().padTop(10f)
                    it.add(Utils.visUI_customCheckBox("COLLISION_TREE_UPDATES", Engine.DEBUG_COLLISION_TREE_UPDATES).also {
                        it.onChange {
                            Engine.DEBUG_COLLISION_TREE_UPDATES = !Engine.DEBUG_COLLISION_TREE_UPDATES
                        }
                    })
                    it.row()
                    it.add(Utils.visUI_customCheckBox("COLLISION_TREE_CELLS", Engine.DEBUG_COLLISION_TREE_CELLS).also {
                        it.onChange {
                            Engine.DEBUG_COLLISION_TREE_CELLS = !Engine.DEBUG_COLLISION_TREE_CELLS
                        }
                    })

                    it.row().padTop(10f)
                    it.add(Utils.visUI_customCheckBox("DEBUG_UI_ENTITY_INFO_POPUP", Engine.DEBUG_UI_ENTITY_INFO_POPUP).also {
                        it.onChange {
                            Engine.DEBUG_UI_ENTITY_INFO_POPUP = !Engine.DEBUG_UI_ENTITY_INFO_POPUP
                        }
                    })
                    it.row()
                    it.add(Utils.visUI_customCheckBox("DEBUG_UI_ENTITY_INFO_ABOVE", Engine.DEBUG_UI_ENTITY_INFO_ABOVE).also {
                        it.onChange {
                            Engine.DEBUG_UI_ENTITY_INFO_ABOVE = !Engine.DEBUG_UI_ENTITY_INFO_ABOVE
                        }
                    })
                    it.row()
                    it.add(Utils.visUI_customCheckBox("DEBUG_STAGE_UI_HIDE_STATIC_ENTITY_INFO", Engine.DEBUG_UI_HIDE_STATIC_ENTITY_INFO).also {
                        it.onChange {
                            Engine.DEBUG_UI_HIDE_STATIC_ENTITY_INFO = !Engine.DEBUG_UI_HIDE_STATIC_ENTITY_INFO
                        }
                    })
                    it.row()
                })
            }
        }

        stage.addActor(window)

        (engine.screen as? BaseScreen)?.openDebugWindow(table)
        window.pack()
    }

    fun clearDebugWindow() {
        debugWindow?.remove()
        debugWindow = null
    }

    class DebugActor(val table: ResizableActorTable, val worldX: Float, val worldY: Float, val worldViewport: Viewport?, val projectToWorld: Boolean = true)
    private val debugActors = GdxArray<DebugActor>()

    fun addDebugActorInWorldPosition(actor: Actor, worldX: Float, worldY: Float, worldViewport: Viewport) {
        ResizableActorTable().also {
            stage.addActor(it)
            it.add(actor)

            debugActors.add(DebugActor(it, worldX, worldY, worldViewport))
        }
    }

    private fun Table.scaleInFadeOut(duration: Float, fadeOutDuration: Float) {
        this.isTransform = true
        this.setScale(0f)
        this.addAction(Actions.sequence(
                Actions.scaleTo(1f, 1f, 0.2f, Interpolation.pow2),
                Actions.delay(duration), Actions.fadeOut(fadeOutDuration, Interpolation.pow2),
                Actions.run { this.remove() }))
    }

    fun addDebugActorInWorldPositionScaleInFadeOut(actor: Actor, worldX: Float, worldY: Float, worldViewport: Viewport,
                                                   duration: Float = 2f, fadeOutDuration: Float = 1f) {
        ResizableActorTable().also {
            stage.addActor(it)
            it.add(actor)

            debugActors.add(DebugActor(it, worldX, worldY, worldViewport))

            it.scaleInFadeOut(duration, fadeOutDuration)
        }
    }

    fun addDebugLabelInWorldPositionScaleInFadeOut(text: String, color: Color, worldX: Float, worldY: Float, worldViewport: Viewport,
                                                   duration: Float = 2f, fadeOutDuration: Float = 1f) {
        this.addDebugActorInWorldPositionScaleInFadeOut(com.dcostap.engine.utils.ui.ExtLabel(text, debugFont, color),
                worldX, worldY, worldViewport, duration, fadeOutDuration)
    }

    fun addDebugActor(actor: Actor, stageX: Float, stageY: Float) {
        ResizableActorTable().also {
            stage.addActor(it)
            it.add(actor)
            it.setPosition(stageX, stageY)

            debugActors.add(DebugActor(it, stageX, stageY, null, false))
        }
    }

    fun addDebugActorScaleInFadeOut(actor: Actor, stageX: Float, stageY: Float, duration: Float = 2f, fadeOutDuration: Float = 1f) {
        ResizableActorTable().also {
            stage.addActor(it)
            it.add(actor)
            it.setPosition(stageX, stageY)

            debugActors.add(DebugActor(it, stageX, stageY, null, false))

            it.scaleInFadeOut(duration, fadeOutDuration)
        }
    }

    fun addDebugLabel(text: String, color: Color, stageX: Float, stageY: Float) {
        this.addDebugActor(com.dcostap.engine.utils.ui.ExtLabel(text, debugFont, color), stageX, stageY)
    }

    fun addDebugLabelScaleInFadeOut(text: String, color: Color, stageX: Float, stageY: Float, duration: Float = 2f, fadeOutDuration: Float = 1f) {
        this.addDebugActorScaleInFadeOut(com.dcostap.engine.utils.ui.ExtLabel(text, debugFont, color), stageX, stageY, duration, fadeOutDuration)
    }

    init {
        // make it poolable
        Pools.set(TextDrawOrder::class.java, object : Pool<TextDrawOrder>(5) {
            override fun newObject(): TextDrawOrder {
                return TextDrawOrder()
            }
        })

        Pools.set(LineDrawOrder::class.java, object : Pool<LineDrawOrder>(5) {
            override fun newObject(): LineDrawOrder {
                return LineDrawOrder()
            }
        })

        Pools.set(RectangleDrawOrder::class.java, object : Pool<RectangleDrawOrder>(5) {
            override fun newObject(): RectangleDrawOrder {
                return RectangleDrawOrder()
            }
        })
    }

    private class TextDrawOrder() {
        var text: String = ""; var stageX: Float = 0f; var stageY: Float = 0f; var color: Color = Color.WHITE
    }

    private class LineDrawOrder() {
        var originX: Float = 0f; var originY: Float = 0f; var objectiveX: Float = 0f; var objectiveY: Float = 0f; var isArrow: Boolean = false;
        var thickness: Float = 1f; var color: Color = Color.WHITE
    }

    private class RectangleDrawOrder() {
        var x: Float = 0f; var y: Float = 0f; var width: Float = 0f; var height: Float = 0f; var fill: Boolean = true;
        var borderThickness: Float = 1f; var color: Color = Color.WHITE
    }

    private val textDrawStack = GdxArray<TextDrawOrder>()
    fun drawDebugText(text: String, stageX: Float, stageY: Float) {
        val order = Pools.obtain(TextDrawOrder::class.java)
        order.text = text; order.stageX = stageX; order.stageY = stageY
        textDrawStack.add(order)
    }

    fun drawDebugTextInWorldPosition(text: String, worldX: Float, worldY: Float, worldViewport: Viewport, color: Color = Color.WHITE) {
        val order = Pools.obtain(TextDrawOrder::class.java)
        val pos = Utils.projectPosition(worldX, worldY, worldViewport, stage.viewport)
        order.text = text; order.stageX = pos.x; order.stageY = pos.y
        order.color = color
        textDrawStack.add(order)
    }

    private val lineDrawStack = GdxArray<LineDrawOrder>()

    fun drawDebugLineInWorldPosition(originX: Float, originY: Float, objectiveX: Float, objectiveY: Float, worldViewport: Viewport,
                                     thickness: Float = 1.1f, isArrow: Boolean = false, color: Color = Color.WHITE) {
        val order = Pools.obtain(LineDrawOrder::class.java)
        var pos = Utils.projectPosition(originX, originY, worldViewport, stage.viewport)
        order.originX = pos.x; order.originY= pos.y
        pos = Utils.projectPosition(objectiveX, objectiveY, worldViewport, stage.viewport)
        order.objectiveX = pos.x; order.objectiveY= pos.y

        order.thickness = thickness
        order.color = color
        order.isArrow = isArrow
        lineDrawStack.add(order)
    }

    private val rectangleDrawStack= GdxArray<RectangleDrawOrder>()

    fun drawDebugRectInWorldPosition(x: Float = 0f, y: Float = 0f, width: Float = 0f, height: Float = 0f, worldViewport: Viewport, fill: Boolean = true,
                                     borderThickness: Float = 1f, color: Color = Color.WHITE) {
        val order = Pools.obtain(RectangleDrawOrder::class.java)
        var pos = Utils.projectPosition(x, y, worldViewport, stage.viewport)
        order.x = pos.x; order.y= pos.y
        pos = Utils.projectPosition(x + width, y + height, worldViewport, stage.viewport)
        order.width = pos.x - order.x; order.height= pos.y - order.y

        order.borderThickness = borderThickness
        order.color = color
        order.fill = fill
        rectangleDrawStack.add(order)
    }

    private val gameDrawer = GameDrawer(stage.batch)

    private val drawOrders = Stack<(GameDrawer) -> Unit>()

    fun drawOnStage(draw: (GameDrawer) -> Unit) {
        drawOrders.push(draw)
    }

    init {
        gameDrawer.useCustomPPM = true
        gameDrawer.customPPM = 1
    }

    private val toBeRemoved = GdxArray<DebugActor>()
    override fun render(delta: Float) {
        super.render(delta)

        toBeRemoved.clear()
        for (actor in debugActors) {
            if (actor.table.stage == null || !actor.table.hasChildren()) {
                toBeRemoved.add(actor)
                continue
            }

            if (actor.projectToWorld) {
                val newPos = Utils.projectPosition(actor.worldX, actor.worldY, actor.worldViewport, stage.viewport)
                actor.table.setPosition(newPos.x, newPos.y)
            }
        }

        for (actor in toBeRemoved) {
            debugActors.removeValue(actor, true)
        }

        stage.act(delta)
        stage.draw()

        stage.batch.projectionMatrix = stage.camera.combined
        stage.batch.use {
            while (!drawOrders.isEmpty()) {
                drawOrders.pop() (gameDrawer)
            }

            for (text in textDrawStack) {
                gameDrawer.color = text.color
                gameDrawer.alpha = text.color.a
                gameDrawer.drawText(text.text, text.stageX, text.stageY, debugFont)
                Pools.free(text)
            }

            textDrawStack.clear()

            for (text in lineDrawStack) {
                gameDrawer.color = text.color
                gameDrawer.alpha = text.color.a
                gameDrawer.drawLine(text.originX, text.originY, text.objectiveX, text.objectiveY, text.thickness, text.isArrow, arrowSize = text.thickness * 4)
                Pools.free(text)
            }

            lineDrawStack.clear()

            for (text in rectangleDrawStack) {
                gameDrawer.color = text.color
                gameDrawer.alpha = text.color.a
                gameDrawer.drawRectangle(text.x, text.y, text.width, text.height, text.fill, text.borderThickness)
                Pools.free(text)
            }

            rectangleDrawStack.clear()
        }
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        stage.viewport.update(width, height, true)
    }

    override fun dispose() {
        stage.dispose()
    }
}
