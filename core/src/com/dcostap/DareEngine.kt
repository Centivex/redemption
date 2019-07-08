package com.dcostap

import com.badlogic.gdx.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Json
import com.dcostap.engine.DebugUI
import com.dcostap.engine.utils.DebugLog
import com.dcostap.engine.utils.ExportedImagesProcessor
import com.dcostap.engine.utils.Utils
import com.dcostap.engine.utils.actions.ActionsUpdater
import com.dcostap.engine.utils.font_loaders.smart_font_generator.SmartFontGenerator
import com.dcostap.engine.utils.screens.BaseScreen
import com.dcostap.engine.utils.screens.BaseScreenWithUI
import com.dcostap.game.GameScreen
import com.kotcrab.vis.ui.VisUI
import java.text.SimpleDateFormat
import java.util.*

class Settings {
    var scaling = 0
    var fullscreen = false
}

class Engine : ApplicationListener {
    companion object Info {
        //region Configuration stuff
        val APP_WIDTH = 288
        val APP_HEIGHT = 162

        var settings = Settings()

        var isRelease = false

        /** Pixels per game unit */
        var PPM: Int = 16

        /** password that, when typed, switches debug mode */
        val DEBUG_PASSWORD = "DEBUG"

        var ALLOW_DEBUG_PASSWORD = true

        /** set to false to make it easier to selectively choose which entities are included in map's colliding trees */
        var ENTITIES_PROVIDE_COLL_INFO_DEFAULT = true

        var DEBUG_UI = true
        var DEBUG_UI_ENTITY_INFO_ABOVE = false
        var DEBUG_UI_ENTITY_INFO_POPUP = false
        var DEBUG_UI_HIDE_STATIC_ENTITY_INFO = false

        var DEBUG_COMMAND_WINDOW_KEY = Input.Keys.ENTER

        var DEBUG = true

        var DEBUG_PRINT = true

        var DEBUG_MAP_CELLS = true
        var DEBUG_ENTITIES_BB = true
        var DEBUG_ENTITIES_BB_X_RAY = true // debug drawing of Entities will be above normal drawing

        var DEBUG_COLLISION_TREE_CELLS = false
        var DEBUG_COLLISION_TREE_UPDATES = false

        var DEBUG_PATHFINDING = false
        var DEBUG_CELL_FLOOD_FILL = false

        var DEBUG_LINE_THICKNESS = 0.1f
        var DEBUG_TRANSPARENCY = 0.5f

        init {
            SmartFontGenerator.fontVersion = "1.0"
            SmartFontGenerator.alwaysRegenerateFonts = false
            SmartFontGenerator.desktopDebugGenerateFontsOnHomeFolder = true
        }
        //endregion

        /** This allows desktop density factor to be bigger, since eyes will be further away from screen than in a mobile device, it
         * is needed to increase the density factor by a bit; so it is multiplied by distance factor (default: 1.5) */
        private val distanceFactor: Float
            get() {
                return when (Gdx.app.type) {
                    Application.ApplicationType.Desktop -> 1.5f
                    else -> 1f
                }
            }

        /** Resolution Factor: difference of current resolution's width compared to a base width.
         * Normally base width is the baseAppWidth.
         * If current resolution is double than the base, this would return 2.
         *
         * Use it to scale things up to all resolutions similar to a FitViewport.  */
        fun getResolutionFactor(baseResolutionWidth: Float): Float {
            return Gdx.graphics.width / baseResolutionWidth
        }

        /** Density Factor: difference of a standard density compared to current device's density.
         * Density means number of pixels per inch of screen. SmartPhones normally have higher density since
         * the screens are small but have big resolutions.
         *
         * Density factor is multiplied by distanceFactor. In computers distance factor
         * is bigger than in smartPhones, since you are further from screen; therefore density factor should be
         * higher (scale of things would be too small otherwise, you are not close to the screen like in a smartPhone!).
         * See [distanceFactor]
         *
         * Use this factor to scale things and keep them the same physical size. Bigger screen sizes will mean
         * more free space for things!  */
        val densityFactor: Float
            get() = Gdx.graphics.density * distanceFactor

        //endregion

        val libgdxJson = Json()
        lateinit var pixelTexture: TextureRegion
        lateinit var debugUI: DebugUI

        /** Measures render calls performed on each frame when [render] is called */
        var renderCalls = 0
            private set
    }

    var debugWindow = false
        set(value) {
            debugUI.clearDebugWindow()
            if (value) debugUI.openDebugWindow()
            field = value
        }

    var screen: Screen? = null
        set(value) {
            screen?.hide()
            field = value
            screen?.show()
            screen?.resize(Gdx.graphics.width, Gdx.graphics.height)
        }

    override fun pause() {
        screen?.pause()
    }

    override fun resume() {
        screen?.resume()
    }

    lateinit var batch: SpriteBatch
        private set

    lateinit var gitTagVersion: String
        private set

    lateinit var gitCommit: String
        private set

    private fun loadVersionProperties() {
        try {
            val versionProperties = Properties()
            versionProperties.load(Gdx.files.internal("version.properties").read())
            gitTagVersion = versionProperties.getProperty("version") ?: ""
            gitCommit = versionProperties.getProperty("commit") ?: ""
        } catch (exc: Exception) {
            printDebug(exc.message)
        }
    }

    fun runDebugCommand(string: String) {
        (screen as? BaseScreen)?.runDebugCommand(string)

        when (string) {
            "debug" -> Engine.DEBUG = !Engine.DEBUG
            "ent" -> Engine.DEBUG_ENTITIES_BB = !Engine.DEBUG_ENTITIES_BB
            "cells" -> Engine.DEBUG_MAP_CELLS = !Engine.DEBUG_MAP_CELLS
            "atlas" -> reloadAtlas()
            "window" -> debugWindow = !debugWindow
        }
    }

    var fixedDelta = 1 / 60f

    /** If true, it will ignore the app's delta */
    var useFixedDelta = false

    lateinit var assets: GameAssets
        private set

    override fun create() {
        if (!isRelease) {
            ExportedImagesProcessor.processExportedImages()
        }

        loadVersionProperties()

        batch = SpriteBatch()

        VisUI.load()

        debugUI = DebugUI(this)

        screen = LoadingScreen(this)

        assets = GameAssets()
        assets.initAssetLoading()

        printDebug("version $gitTagVersion")
    }

    override fun resize(width: Int, height: Int) {
        screen?.resize(width, height)
        debugUI.resize(width, height)
    }

    private var debugCurrentWritten = 0
    private var debugCurrentWrittenTimer = 0f

    override fun render() {
        batch.totalRenderCalls = 0
        (debugUI.stage.batch as SpriteBatch).totalRenderCalls = 0
        if (screen != null && screen is BaseScreenWithUI) {((screen as BaseScreenWithUI).stage.batch as SpriteBatch).totalRenderCalls = 0}

//        println(renderCalls)

        updateDelta()
        val delta = if (useFixedDelta) fixedDelta else smoothedDelta

        actions.update(delta)

        try {
            screen?.render(delta)
            if (DEBUG && DEBUG_UI) {
                debugUI.render(delta)
            }
        } catch(e: Exception) {
            if (isRelease)
                saveDebugLog("crashLog-${SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().time)}.txt")
            dispose()
            throw e
        }

        renderCalls = batch.totalRenderCalls
        renderCalls += (debugUI.stage.batch as SpriteBatch).totalRenderCalls
        if (screen != null && screen is BaseScreenWithUI) {
            renderCalls += ((screen as BaseScreenWithUI).stage.batch as SpriteBatch).totalRenderCalls}

        // debug purposes
        if (DEBUG && Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            screen?.dispose()
            screen = GameScreen(this)
        }

        if (DEBUG && Gdx.input.isKeyJustPressed(DEBUG_COMMAND_WINDOW_KEY)) {
            debugUI.openCloseDebugCommandsWindow()
        }

        if (ALLOW_DEBUG_PASSWORD && debugUI.consoleClosed) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.valueOf(DEBUG_PASSWORD[debugCurrentWritten].toString()))) {
                debugCurrentWritten++
                if (debugCurrentWritten >= DEBUG_PASSWORD.length) {
                    debugCurrentWritten = 0
                    Engine.DEBUG = !Engine.DEBUG
                }
                debugCurrentWrittenTimer = 1f
            }

            if (debugCurrentWrittenTimer > 0f) {
                debugCurrentWrittenTimer -= delta
                if (debugCurrentWrittenTimer <= 0f) debugCurrentWritten = 0
            }
        }
    }

    fun reloadAtlas() {
        ExportedImagesProcessor.processExportedImages()
        assets.reloadTextureAtlas()
    }

    private var smoothedDelta = 1 / 60f
    private fun updateDelta() {
        // limit delta value
        var delta = Gdx.graphics.deltaTime
        delta = Utils.clamp(delta, delta, 1 / 25f)

        // smooth delta to avoid wonky movement at high speeds
        val smoothIncrement = 1 / 1000f
        if (smoothedDelta < delta) {
            smoothedDelta = Math.min(delta, smoothedDelta + smoothIncrement)
        } else if (smoothedDelta > delta) {
            smoothedDelta = Math.max(delta, smoothedDelta - smoothIncrement)
        }
    }

    override fun dispose() {
        assets.dispose()
        batch.dispose()
        screen?.dispose()

        VisUI.dispose()

        debugLog.log.clear()
    }

    /** Note that actions added here can't pause its progress. Note that Entities have a local
     * ActionsUpdater which might be a better fit*/
    val actions = ActionsUpdater()
}

private val debugLog = DebugLog(500)
fun printDebug(string: String? = null) {
    debugLog.printIt = Engine.DEBUG_PRINT
    debugLog.printDebug(string ?: "\n")
}

fun printDebug(number: Number) {
    printDebug(number.toString())
}

fun saveDebugLog(name: String = "debugLog.txt") {
    Gdx.files.local(name).writeString(debugLog.toString(), false)
}