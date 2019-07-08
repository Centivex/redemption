package com.dcostap.engine.map.entities

import com.badlogic.gdx.math.Vector2

/** Lightest Entity possible. It's always drawn (no culling), can't be checked for collision (won't be put inside any collTree),
 * and won't modify mapCells when static. Note that if many of these entities may appear outside the camera view,
 * no culling might do more harm than good (unnecessary drawing) */
open class LightEntity(position: Vector2 = Vector2(), isStatic: Boolean = false) : Entity(position, isStatic = isStatic,
        providesCollidingInfo = false, providesCullingInfo = false, providesStaticInfoToCells = false) {

}