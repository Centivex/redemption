package com.dcostap.udf;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.dcostap.engine.map.entities.CollidingEntity;
import com.dcostap.engine.utils.Animation;
import com.dcostap.engine.utils.GameDrawer;
import org.jetbrains.annotations.NotNull;

public class Jugador extends CollidingEntity {

    public float velocidad = 5;
    public GameScreen gs;
    public float timeAnim=0.1f;
    //varible para pasar la animacion correcta, en funcion del movimiento
    public float i=0;

    private Animation<TextureAtlas.AtlasRegion> animCamAb, animCamAr, animCamDe, animCamIz, animAttAb, animAttAr, animAttDe, animAttIz;

    public Jugador(GameScreen gs) {
        super();
        this.gs=gs;
        animCamAb =new Animation<>(gs.getAssets().getTextures("caminar_abajo"),timeAnim );
        animCamAr =new Animation<>(gs.getAssets().getTextures("caminar_arriba"),timeAnim );
        animCamDe =new Animation<>(gs.getAssets().getTextures("caminar_derecha"),timeAnim );
        animCamIz =new Animation<>(gs.getAssets().getTextures("caminar_izquierda"),timeAnim );

        animAttAb =new Animation<>(gs.getAssets().getTextures("atacar_abajo"),timeAnim );
        animAttAr =new Animation<>(gs.getAssets().getTextures("atacar_arriba"),timeAnim );
        animAttDe =new Animation<>(gs.getAssets().getTextures("atacar_derecha"),timeAnim );
        animAttIz =new Animation<>(gs.getAssets().getTextures("atacar_izquierda"),timeAnim );

        animCamAb.pause();
        animCamAr.pause();
        animCamDe.pause();
        animCamIz.pause();

    }

    @Override
    public void update(float delta) {
        super.update(delta);
        getSpeed().set(0,0);
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
    }

    @Override
    public void draw(@NotNull GameDrawer gameDrawer, float delta) {
        super.draw(gameDrawer, delta);
        if (i==4){
            animCamAr.update(delta);
            gameDrawer.draw(animCamAr.getFrame(),getX(),getY(),0.5f,0.5f);
        }
        else  if (i==3){
            animCamAb.update(delta);
            gameDrawer.draw(animCamAb.getFrame(),getX(),getY(),0.5f,0.5f);
        }
        else  if (i==2){
            animCamDe.update(delta);
            gameDrawer.draw(animCamDe.getFrame(),getX(),getY(),0.5f,0.5f);
        }
        else  if (i==1){
            animCamIz.update(delta);
            gameDrawer.draw(animCamIz.getFrame(),getX(),getY(),0.5f,0.5f);
        }
        else if(i==0){
            gameDrawer.draw(animCamAb.getFrame(),getX(),getY(),0.5f,0.5f);
        }
    }
}
