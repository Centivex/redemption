package com.dcostap.udf;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.dcostap.Engine;
import com.dcostap.engine.map.entities.CollidingEntity;
import com.dcostap.engine.utils.Animation;
import com.dcostap.engine.utils.GameDrawer;
import org.jetbrains.annotations.NotNull;

public class Jugador extends CollidingEntity {

    public float velocidad = 5;
    public GameScreen gs;
    public float timeAnim=0.1f;
    //varible para pasar la animacion correcta, en funcion del movimiento. la i es basicamente la dirección
    public float i=0;
    public boolean atacando=false;

    private Animation<TextureAtlas.AtlasRegion> animCamAb, animCamAr, animCamDe, animCamIz, animAttAb, animAttAr, animAttDe, animAttIz;

    public Jugador(GameScreen gs) {
        super();
        this.gs=gs;
        animCamAb =new Animation<>(gs.getAssets().getTextures("caminar_abajo"),timeAnim );
        animCamAr =new Animation<>(gs.getAssets().getTextures("caminar_arriba"),timeAnim );
        animCamDe =new Animation<>(gs.getAssets().getTextures("caminar_derecha"),timeAnim );
        animCamIz =new Animation<>(gs.getAssets().getTextures("caminar_izquierda"),timeAnim );

        animAttAb =new Animation<>(gs.getAssets().getTextures("atacar_abajo"),timeAnim);
        animAttAr =new Animation<>(gs.getAssets().getTextures("atacar_arriba"),timeAnim);
        animAttDe =new Animation<>(gs.getAssets().getTextures("atacar_derecha"),timeAnim);
        animAttIz =new Animation<>(gs.getAssets().getTextures("atacar_izquierda"),timeAnim);

        animCamAb.pause();
        animCamAr.pause();
        animCamDe.pause();
        animCamIz.pause();

        animAttAb.pause();
        animAttAr.pause();
        animAttDe.pause();
        animAttIz.pause();


        this.getActualBoundingBox().modify(new Rectangle());
        getActualBoundingBox().modify(new Rectangle(-0.25f, -0.20f, 7f / Engine.Info.getPPM(), 7f / Engine.Info.getPPM()));
    }

    @Override
    public void update(float delta) {
        super.update(delta);
        getSpeed().set(0,0);

        //las animaciones de movimiento
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            getSpeed().x = -velocidad;
            animCamIz.resume();
            i=1;

        }else {
            animCamIz.pause();
            animCamIz.reset();
        }

        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            getSpeed().x = +velocidad;
            animCamDe.resume();
            i=2;
        }else {
             animCamDe.pause();
             animCamDe.reset();
         }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            getSpeed().y = -velocidad;
            animCamAb.resume();
            i=3;
        }
        else {
            animCamAb.pause();
            animCamAb.reset();
        }
        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            getSpeed().y = +velocidad;
            animCamAr.resume();
            i=4;
        }
        else {
            animCamAr.pause();
            animCamAr.reset();
        }

        //las animaciones de atacar
        if(Gdx.input.isKeyJustPressed(Input.Keys.SPACE)){
            getSpeed().x = 0f;
            atacando=true;

            if (i==1){
                animCamIz.pause();
                animCamIz.reset();
                animAttIz.resume();
            }
            else {
                animAttIz.pause();
                animAttIz.reset();
            }

            if (i==2){
                animCamDe.pause();
                animCamDe.reset();
                animAttDe.resume();
            }
            else {
                animAttDe.pause();
                animAttDe.reset();
            }

            if (i==3 || i ==0){
                animCamAb.pause();
                animCamAb.reset();
                animAttAb.resume();
            }
            else {
                animAttAb.pause();
                animAttAb.reset();
            }

            if (i==4){
                animCamAr.pause();
                animCamAr.reset();
                animAttAr.resume();
            }
            else {
                animAttAr.pause();
                animAttAr.reset();
            }
        }
    }

    @Override
    public void draw(@NotNull GameDrawer gameDrawer, float delta) {

        //por si algun dia quieres obtener la altura del sprite
        //int er= gs.getAssets().getTexture("caminar_abajo").getRegionHeight();

        TextureRegion frame =null;

        if (i==4){
            animCamAr.update(delta);
            frame=animCamAr.getFrame();

            if (atacando==true){
                animAttAr.update(delta);
                frame=animAttAr.getFrame();

                if (animAttAr.getFinishedNormalAnimation()) atacando = false;
            }

        }
        else  if (i==3){
            animCamAb.update(delta);
            frame=animCamAb.getFrame();

            if (atacando==true){
                animAttAb.update(delta);
                frame=animAttAb.getFrame();

                if (animAttAb.getFinishedNormalAnimation()) atacando = false;
            }
        }
        else  if (i==2){
            animCamDe.update(delta);
            frame=animCamDe.getFrame();

            if (atacando==true){
                animAttDe.update(delta);
                frame=animAttDe.getFrame();

                if (animAttDe.getFinishedNormalAnimation()) atacando = false;
            }
        }
        else  if (i==1){
            animCamIz.update(delta);
            frame=animCamIz.getFrame();

            if (atacando==true){
                animAttIz.update(delta);
                frame=animAttIz.getFrame();

                if (animAttIz.getFinishedNormalAnimation()) {
                    atacando = false;
                }
            }
        }
        else if(i==0){
            frame=animCamAb.getFrame();

            if (atacando==true){
                animAttAb.update(delta);
                frame=animAttAb.getFrame();

                if (animAttAb.getFinishedNormalAnimation()) atacando = false;
            }
        }

        gameDrawer.draw(frame, getX(), getY()-1f, 1f, 1f,0f, 0f, 0f, false, false, 0, 0, true, false);


        super.draw(gameDrawer, delta);
    }

}
