package com.dcostap;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.viewport.Viewport;

/** Scales the content to nearest integer factor */
public class PixelViewport extends Viewport {
    private float worldScreenWidth;
    private float worldScreenHeight;

    public PixelViewport(float worldWidth, float worldHeight, float worldWidthPixels, float worldHeightPixels, Camera camera) {
        setWorldSize(worldWidth, worldHeight);
        setCamera(camera);

        this.worldScreenWidth = worldWidthPixels;
        this.worldScreenHeight = worldHeightPixels;
    }

    public PixelViewport(float worldWidth, float worldHeight, float screenWidth, float screenHeight) {
        this(worldWidth, worldHeight, screenWidth, screenHeight, new OrthographicCamera());
    }

    @Override
    public void update (int screenWidth, int screenHeight, boolean centerCamera) {
        int scaling = Math.min(MathUtils.floor(screenWidth / worldScreenWidth), MathUtils.floor(screenHeight / worldScreenHeight));

        int viewportWidth = MathUtils.floor(worldScreenWidth * scaling);
        int viewportHeight = MathUtils.floor(worldScreenHeight * scaling);

        // Center.
        setScreenBounds((screenWidth - viewportWidth) / 2, (screenHeight - viewportHeight) / 2, viewportWidth, viewportHeight);

        apply(centerCamera);
    }
}
