package com.dcostap.udf;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.dcostap.Engine;
import com.dcostap.engine.map.EntityTiledMap;
import com.dcostap.engine.map.MapCell;
import com.dcostap.engine.map.entities.CollidingEntity;
import com.dcostap.engine.map.entities.Entity;
import com.dcostap.engine.map.map_loading.CustomProperties;
import com.dcostap.engine.map.map_loading.EntityLoaderFromClass;
import com.dcostap.engine.map.map_loading.EntityLoaderFromString;
import com.dcostap.engine.map.map_loading.TileLoaderFromString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by Darius on 03-May-19.
 */
public class GameMapLoader implements EntityLoaderFromString, TileLoaderFromString {

    public GameScreen gs;

    public GameMapLoader(GameScreen gs){
        this.gs=gs;
    }

    @Override
    public Entity loadEntityFromTiledTileObject(@NotNull String imageName, @NotNull String objectName,
                                                @NotNull Vector2 position, int widthPixels, int heightPixels,
                                                @NotNull EntityTiledMap map, @NotNull CustomProperties objectProps) {
        return null;
    }

    @Override
    public Entity loadEntityFromObjectName(@NotNull String objectName, @NotNull Vector2 position, int widthPixels,
                                           int heightPixels, @NotNull EntityTiledMap map, @NotNull CustomProperties objectProps) {
        if (objectName.equals("solid")){
        return new Entity(new Vector2(position), new Rectangle(0f, 0f, widthPixels / (float)Engine.Info.getPPM(),
                heightPixels / (float)Engine.Info.getPPM()), true);
        }
        else if (objectName.equals("player")){
            Jugador jugador= new Jugador(gs);
            jugador.getPosition().set(position);
            return jugador;
        }
        return null;
    }

    // TILES
    @Override
    public boolean loadTileFromImageName(@NotNull String imageName, @NotNull MapCell mapCell, @NotNull EntityTiledMap map) {
        return false;
    }
}
