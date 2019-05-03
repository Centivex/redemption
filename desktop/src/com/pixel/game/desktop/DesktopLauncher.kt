package com.pixel.game.desktop

import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.dcostap.Engine

object DesktopLauncher {
    @JvmStatic
    fun main(arg: Array<String>) {
        val config = LwjglApplicationConfiguration()
        val displayMode = LwjglApplicationConfiguration.getDesktopDisplayMode()
        Engine.calculateAppSizing(displayMode.width, displayMode.height)

        config.width = (Engine.ORIG_APP_WIDTH * 4).toInt()
        config.height = (Engine.ORIG_APP_HEIGHT * 4).toInt()
        config.fullscreen = false
        config.vSyncEnabled = true
        config.foregroundFPS = 60
        config.backgroundFPS = 60
        config.title = "libgdx"
        config.resizable = false

        LwjglApplication(Engine(), config)
    }
}

