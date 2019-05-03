package com.dcostap.engine.utils.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.badlogic.gdx.utils.viewport.Viewport
import com.dcostap.engine.utils.Drawable
import com.dcostap.engine.utils.GameDrawer
import com.dcostap.engine.utils.Updatable
import com.dcostap.engine.utils.actions.ActionsUpdater
import com.dcostap.engine.utils.input.InputController
import com.dcostap.engine.utils.input.InputListener
import com.dcostap.engine.utils.use
import com.dcostap.Engine
import com.dcostap.engine.utils.ui.ExtTable
import ktx.collections.GdxArray

/**
 * Convenience behavior already coded: base variables with Engine, a Camera, a Viewport, InputController and GameDrawer
 *
 * Extend this class instead of ScreenAdapter if you need all that functionality
 */
abstract class BaseScreen(val engine : Engine) : Screen, Drawable, Updatable, InputListener {
    var camera: OrthographicCamera = OrthographicCamera()
        private set

    var worldViewport: Viewport
        protected set

    val assets get() = engine.assets

    val gameDrawer: GameDrawer

    val inputController: InputController

    /** For anything that wants to be drawn after all other Drawables in maps and the UI. Add them here */
    val lateDrawingList = GdxArray<LateDrawing>()

    var inputMulti: InputMultiplexer

    init {
        worldViewport = createViewport()
        gameDrawer = GameDrawer(engine.batch)

        inputController = InputController(worldViewport)
        inputMulti = InputMultiplexer(Engine.debugUI.stage, inputController)

        Gdx.input.inputProcessor = inputMulti

        inputController.registerListener(this)
    }

    override fun hide() {

    }

    override fun show() {

    }

    open var worldInputEnabled: Boolean = true
        set (value) {
            if (value != field) {
                if (value) {
                    inputMulti.clear()
                    inputMulti.addProcessor(Engine.debugUI.stage)
                    inputMulti.addProcessor(inputController)
                }
                else
                    inputMulti.removeProcessor(1)
            }
            field = value
        }

    val actions = ActionsUpdater()

    /** Override to change created viewport */
    open fun createViewport(): Viewport {
        return ExtendViewport(480 / Engine.PPM.toFloat(),
                270 / Engine.PPM.toFloat(), camera)
    }

    override fun draw(gameDrawer: GameDrawer, delta: Float) {}

    private var firstUpdate = true

    /** Called on the first frame update() method is called. Surprisingly useful */
    open fun firstUpdate(delta: Float) {

    }

    override fun update(delta: Float) {
        if (firstUpdate) {
            firstUpdate = false
            firstUpdate(delta)
        }

        // round camera position to 3 decimals, to avoid random blank lines between map tiles
        // todo: does this actually do anything?
        camera.position.x = (Math.round(camera.position.x * 1000.0) / 1000.0).toFloat()
        camera.position.y = (Math.round(camera.position.y * 1000.0) / 1000.0).toFloat()

        inputController.update(delta)

        actions.update(delta)
    }

    override fun render(delta: Float) {
        update(delta)

        draw(gameDrawer, delta)

        lateDrawing(gameDrawer, delta)
    }

    private fun lateDrawing(gameDrawer: GameDrawer, delta: Float) {
        if (lateDrawingList.size > 0) {
            engine.batch.use {
                for (ent in lateDrawingList) ent.lateDraw(gameDrawer, delta)
            }
        }
    }

    override fun resize(width: Int, height: Int) {
        worldViewport.update(width, height)
        Engine.debugUI.resize(width, height)
    }

    open fun runDebugCommand(string: String) {}

    open fun openDebugWindow(table: ExtTable) {}

    /** when android app regains focus and in desktop when app closes, called before [dispose] */
    override fun pause() {}

    /** only for android, when app loses focus */
    override fun resume() {}

    override fun dispose() {
        inputController.removeListener(this)
        Engine.debugUI.stage.clear()
    }

    override fun touchDownEvent(screenX: Float, screenY: Float, worldX: Float, worldY: Float, pointer: Int, isJustPressed: Boolean) {

    }

    override fun touchReleasedEvent(screenX: Float, screenY: Float, worldX: Float, worldY: Float, button: Int, pointer: Int) {

    }
}

interface LateDrawing {
    fun lateDraw(gameDrawer: GameDrawer, delta: Float)
}
