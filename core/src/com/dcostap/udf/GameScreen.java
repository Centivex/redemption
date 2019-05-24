package com.dcostap.udf;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.dcostap.Engine;
import com.dcostap.engine.map.EntityTiledMap;
import com.dcostap.engine.utils.GameDrawer;
import com.dcostap.engine.utils.Utils;
import com.dcostap.engine.utils.screens.BaseScreenWithUI;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Darius on 03-May-19.
 */
public class GameScreen extends BaseScreenWithUI {

    private EntityTiledMap map = new EntityTiledMap(this, getWorldViewport());
    Jugador jugador =new Jugador(this);

    public GameScreen(Engine engine) {
        super(engine);
        getAssets().getZona1mapa().loadMap(map, new GameMapLoader(this), null, null, 10);
        map.addEntity(jugador);
        getCamera().position.set(5.6875f,4f,0);
    }


    @Override
    public void update(float delta) {
        super.update(delta);
        map.update(delta);
        getCamera().position.set(jugador.getPosition().x+5.6875f,jugador.getPosition().y+4f,0);
       // System.out.println(jugador.getPosition().x+" holaaaaa "+jugador.getPosition().y);


    }


    @Override
    public Viewport createViewport() {
        return new ExtendViewport(Engine.Info.getViewportWidth() / (float) Engine.Info.getPPM(),
                Engine.Info.getViewportHeight() / (float) Engine.Info.getPPM(), getCamera());
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
        map.draw(gameDrawer, delta);
        getEngine().getBatch().end();

        getStage().act(delta);
        getStage().draw();
    }

    @Override
    public void runDebugCommand(@NotNull String string) {

    }
}
