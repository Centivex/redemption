package com.dcostap.udf;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.dcostap.Engine;
import com.dcostap.engine.utils.GameDrawer;
import com.dcostap.engine.utils.Utils;
import com.dcostap.engine.utils.screens.BaseScreenWithUI;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Darius on 03-May-19.
 */
public class GameScreen extends BaseScreenWithUI {
    public GameScreen(Engine engine) {
        super(engine);
    }

    @Override
    public Viewport createViewport() {
        return new ExtendViewport(Engine.Info.getViewportWidth(), Engine.Info.getViewportHeight());
    }

    @Override
    public Stage createStage() {
        return new Stage(new ExtendViewport(Engine.Info.getViewportWidth(), Engine.Info.getViewportHeight()));
    }

    @Override
    public void draw(@NotNull GameDrawer gameDrawer, float delta) {
        Utils.clearScreen(52, 87, 133);

        // update camera & viewport
        getWorldViewport().apply();

        // start the batch
        getEngine().getBatch().setProjectionMatrix(getCamera().combined);

        getEngine().getBatch().begin();

        // draw here

        getEngine().getBatch().end();

        getStage().act(delta);
        getStage().draw();
    }

    @Override
    public void runDebugCommand(@NotNull String string) {

    }
}
