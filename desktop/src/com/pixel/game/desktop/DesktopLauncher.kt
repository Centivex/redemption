package com.pixel.game.desktop

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.dcostap.Engine
import com.dcostap.engine.utils.ExportedImagesProcessor

object DesktopLauncher {
    @JvmStatic
    fun main(arg: Array<String>) {
        val config = Lwjgl3ApplicationConfiguration()

        config.setWindowedMode(Engine.APP_WIDTH * 3, Engine.APP_HEIGHT * 3)
        config.useVsync(true)
        config.setTitle("Redemption")

        Lwjgl3Application(Engine(), config)
    }
}

object UpdateAsets {
    @JvmStatic
    fun main(arg: Array<String>) {
        val config = Lwjgl3ApplicationConfiguration()

        config.setWindowedMode(5, 5)
        config.useVsync(true)
        config.setTitle("")

        Lwjgl3Application(object : ApplicationListener {
            override fun render() {

            }

            override fun pause() {

            }

            override fun resume() {

            }

            override fun resize(width: Int, height: Int) {

            }

            override fun create() {
                ExportedImagesProcessor.processExportedImages()

                Gdx.app.exit()
            }

            override fun dispose() {

            }
        }, config)
    }
}

