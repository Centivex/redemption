package com.dcostap.engine.utils.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen

/**
 * Created by Darius on 29/12/2017
 *
 * Use to provide Screen functionality to any class.
 * For example, this way you can easily have Screens with multiple other Screens inside.
 *
 * You can therefore have a hierarchy of Screens:
 *
 *  * GameScreen
 *  * MapScreen
 *  * MainScreen
 *  * OptionsScreen
 *
 */
class ScreenManager {
    var screen: Screen? = null
        set(screen) {
            if (this.screen != null) this.screen!!.hide()
            field = screen
            if (this.screen != null) {
                this.screen!!.show()
                this.screen!!.resize(Gdx.graphics.width, Gdx.graphics.height)
            }
        }
}
