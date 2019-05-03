package com.dcostap.engine.utils.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.dcostap.engine.utils.input.InputController
import com.dcostap.Engine

/**
 * Created by Darius on 28/12/2017.
 *
 * Adds base UI functionality: UIController and a Stage
 */
abstract class BaseScreenWithUI(engine : Engine) : BaseScreen(engine) {
    val stage: Stage
    val stageInputController: InputController

    init {
        stage = createStage()
        stageInputController = InputController(stage.viewport)

        inputMulti = InputMultiplexer(Engine.debugUI.stage, stage, stageInputController, inputController)
        Gdx.input.inputProcessor = inputMulti
    }

    override var worldInputEnabled: Boolean = true
        set (value) {
            if (value != field) {
                if (value) {
                    inputMulti.clear()
                    inputMulti.addProcessor(Engine.debugUI.stage)

                    if (stageInputEnabled) {
                        inputMulti.addProcessor(stage)
                        inputMulti.addProcessor(stageInputController)
                    }
                    inputMulti.addProcessor(inputController)
                }
                else
                    inputMulti.removeProcessor(inputController)
            }
            field = value
        }

    open var stageInputEnabled: Boolean = true
        set (value) {
            if (value != field) {
                if (value) {
                    inputMulti.clear()
                    inputMulti.addProcessor(Engine.debugUI.stage)
                    inputMulti.addProcessor(stage)
                    inputMulti.addProcessor(stageInputController)

                    if (worldInputEnabled)
                        inputMulti.addProcessor(inputController)
                }
                else {
                    inputMulti.removeProcessor(stage)
                    inputMulti.removeProcessor(stageInputController)
                }
            }
            field = value
        }

    override fun update(delta: Float) {
        super.update(delta)

        stageInputController.update(delta)
    }

    /** Override to change created Stage */
    open fun createStage(): Stage {
        return Stage(ScreenViewport())
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        stage.viewport.update(width, height, true)
    }

    override fun dispose() {
        super.dispose()
        stage.dispose()
    }
}
