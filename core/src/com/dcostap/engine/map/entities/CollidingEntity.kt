package com.dcostap.engine.map.entities

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonValue
import com.dcostap.Engine
import com.dcostap.engine.map.MapCell
import com.dcostap.engine.utils.addChildValue
import com.dcostap.printDebug

/**
 * Created by Darius on 15/09/2017.
 *
 *
 *  Provides collision response against certain Entities, define which in [filterEntityForPossibleCollision]
 * (defaults to solid entities)
 *
 *  Provides speed vector variable for movement. Collision response will be done when calling [update].
 *
 * During collision response the Entity will move the quantity specified in the speed vector, and check for collisions
 * against the specified Entities; if collisions exist, this Entity will move out of the collision.
 *
 *
 *  You can override or add to this default movement by manually calling [moveColliding].
 * Movement using that method will always perform collision response
 *
 *
 *  Note that the Entity's colliding state will be updated while doing collision response, so any subclass might want to avoid
 * updating it again after. See [updateCollidingState]
 *
 *
 *  If the collision response will only be done against solid static Entities, consider setting [onlyCollideAgainstMapCells]
 *
 *
 *  Collision response is only provided for the default Entity's bounding box
 */
abstract class CollidingEntity @JvmOverloads constructor(position: Vector2 = Vector2(), boundingBox: Rectangle = Rectangle(),
                                                         isSolid: Boolean = false, var collisionResponseIncludeDynamicEnts: Boolean = true,
                                                         providesCollidingInfo: Boolean = Engine.ENTITIES_PROVIDE_COLL_INFO_DEFAULT)
    : Entity(position, boundingBox, isSolid, false, providesCollidingInfo)
{
    /** precision, in game units, of collision detection  */
    private val precision = 0.01f

    var speed = Vector2()
        private set

    var stopMovingColliding = false

    private var hasCollided = false
    private var collidedX = 0
    private var collidedY = 0

    /** List of valid (only solid ones by default) entities that the object collided with in this frame, while resolving collision
     * Use it with hasCollidedX to find out in which direction it collided with those entities
     *
     * Example: if hasCollidedX() returns 0, (or hasCollided() returns false) this will never return anything since
     * there was no collision response in this coordinate. If it returns 1, check this method.
     * All those entities collided with this entity and were to its right.
     *
     *  Note that this applies to collision-response valid entities only, just like the other methods
     * related to collision response flags */
    val collidingEntitiesX = Array<Entity>()
    /** @see [collidingEntitiesX] */
    val collidingEntitiesY = Array<Entity>()

    /** default: false; set it to true so that collisionResponse is done without using CollisionTree (less expensive).
     *
     * If true collisionResponse will only check for MapCells filtered in
     * @see MapCell
     */
    var onlyCollideAgainstMapCells = false

    var collidingBB: String = "default"
    /** will check collision with other Entities which have this BB */
    var othersCollidingBB: String = "default"

    override fun update(delta: Float) {
        super.update(delta)

        if (!stopMovingColliding)
            moveColliding(speed.x, speed.y, delta)
    }

    open fun moveColliding(xAdd: Float, yAdd: Float, delta: Float) {
        // reset collision information flags
        collidedX = 0
        collidedY = 0
        hasCollided = false
        collidingEntitiesX.clear()
        collidingEntitiesY.clear()

        if (xAdd == 0f && yAdd == 0f) return

        super.move(xAdd, yAdd, true, delta)

        // collision response with collision tree
        if (!onlyCollideAgainstMapCells) {
            updateCollidingState(collisionResponseIncludeDynamicEnts, collidingBB)

            if (isCollidingWithOneEntityValidForCollisionResponse(delta)) {
                hasCollided = true
                fixCollisionAgainstCollisionTree(xAdd, yAdd, precision, delta)
            }
        } else { // collision response with mapCells
            if (isCollidingWithMapCell()) {
                hasCollided = true
                fixCollisionAgainstMapCells(xAdd, yAdd, precision, delta)
            }
        }
    }

    open fun filterMapCellForCollision(cell: MapCell): Boolean {
        return cell.isSolid
    }

    /** Which entities are ignored / allowed *before* doing collision check
     * @return whether to allow the Entity for collision checking */
    open fun filterEntityForPossibleCollision(entity: Entity): Boolean {
        return entity.isSolid
    }

    /** Which entities are ignored / allowed *after* doing collision check. It is colliding with the entity during this call
     * @return whether to allow the Entity for collision response */
    open fun filterEntityThatIsColliding(entity: Entity): Boolean {
        return true
    }

    private fun isCollidingWithOneEntityValidForCollisionResponse(delta: Float): Boolean {
        for (entity in possibleCollidingEntities) {
            if (filterEntityForPossibleCollision(entity) && this.isCollidingWith(entity, collidingBB, othersCollidingBB)
                    && filterEntityThatIsColliding(entity)) {
                return true
            }
        }

        return false
    }

    private fun findAllCollidingEntitiesValidForCollisionResponse(arrayToPopulate: Array<Entity>, delta: Float) {
        for (entity in possibleCollidingEntities) {
            if (filterEntityForPossibleCollision(entity) && this.isCollidingWith(entity, collidingBB, othersCollidingBB)
                    && filterEntityThatIsColliding(entity)) {
                arrayToPopulate.add(entity)
            }
        }
    }

    private fun fixCollisionAgainstCollisionTree(xAdd: Float, yAdd: Float, STEP: Float, delta: Float) {
        val signX = Math.signum(xAdd).toInt()
        val signY = Math.signum(yAdd).toInt()

        // go back to original position
        super.move(-xAdd, -yAdd, true, delta)

        updateCollidingState(true)

        // was already colliding with solid? go back like crazy
        if (isCollidingWithOneEntityValidForCollisionResponse(delta)) {
            var count = 0

            findAllCollidingEntitiesValidForCollisionResponse(collidingEntitiesX, delta)
            collidingEntitiesY.addAll(collidingEntitiesX)

            var amount = 2f
            var switch = false
            var signX = signX
            var signY = signY
            while (count < 100) {
                count++

//                if (count >= 2)
                    printDebug("Warning - resolving deep collision response to entity $this")

                super.move(-STEP * signX * amount, -STEP * signY * amount, true, 1f)

                if (!switch) {
                    switch = true
                    signX *= -1
                    signY *= -1
                } else {
                    switch = false
                    amount += 1f
                }

                updateCollidingState(true)
                if (!isCollidingWithOneEntityValidForCollisionResponse(delta)) {
                    break
                }
            }
        } else {
            var xAdded = 0f
            var yAdded = 0f
            var xEnded = signX == 0
            var yEnded = signY == 0

            // first move STEP on x, then on y. Stop one coordinate when it moved enough, or when collided
            while (true) {
                val xTotalMoved = xAdd * signX.toFloat() * delta
                val yTotalMoved = yAdd * signY.toFloat() * delta
                if (!xEnded) {
                    super.move(STEP * signX, 0f, true, 1f)
                    xAdded += STEP

                    updateCollidingState(true)
                    val hasSolidCollision = isCollidingWithOneEntityValidForCollisionResponse(delta)

                    // stop when collided with solid or when you moved all the original quantity moved
                    if (hasSolidCollision || xAdded >= xTotalMoved) {
                        if (hasSolidCollision) { // stopped because of solid collision
                            collidedX = signX
                            findAllCollidingEntitiesValidForCollisionResponse(collidingEntitiesX, delta)

                            // go back before the collision
                            super.move(-STEP * signX, 0f, true, 1f)
                            updateCollidingState(true)
                        }

                        xEnded = true
                    }
                }

                if (!yEnded) {
                    super.move(0f, STEP * signY, true, 1f)
                    yAdded += STEP

                    updateCollidingState(true)

                    val hasSolidCollision = isCollidingWithOneEntityValidForCollisionResponse(delta)
                    if (hasSolidCollision || yAdded >= yTotalMoved) {
                        if (hasSolidCollision) {
                            collidedY = signY
                            findAllCollidingEntitiesValidForCollisionResponse(collidingEntitiesY, delta)

                            super.move(0f, -STEP * signY, true, 1f)
                            updateCollidingState(true)
                        }

                        yEnded = true
                    }
                }

                if (xEnded && yEnded) {
                    break
                }
            }
        }
    }

    private fun isCollidingWithMapCell(): Boolean {
        return !getCollidingMapCells(collidingBB) {filterMapCellForCollision(it)}.isEmpty
    }

    private fun fixCollisionAgainstMapCells(xAdd: Float, yAdd: Float, STEP: Float, delta: Float) {
        val signX = Math.signum(xAdd).toInt()
        val signY = Math.signum(yAdd).toInt()

        // go back to original position
        super.move(-xAdd, -yAdd, true, delta)

        // was already colliding with solid? go back like crazy
        if (isCollidingWithMapCell()) {
            var count = 0
            while (count < 1000) {
                count++
                super.move(-STEP * signX, -STEP * signY, true, 1f)

                if (isCollidingWithMapCell()) {
                    break
                }
            }
        } else {
            var xAdded = 0f
            var yAdded = 0f
            var xEnded = signX == 0
            var yEnded = signY == 0

            // first move STEP on x, then on y. Stop one coordinate when it moved enough, or when collided
            while (true) {
                val xTotalMoved = xAdd * signX.toFloat() * delta
                val yTotalMoved = yAdd * signY.toFloat() * delta
                if (!xEnded) {
                    super.move(STEP * signX, 0f, true, 1f)
                    xAdded += STEP

                    val hasSolidCollision = isCollidingWithMapCell()

                    // stop when collided with solid or when you moved all the original quantity moved
                    if (hasSolidCollision || xAdded >= xTotalMoved) {
                        if (hasSolidCollision) { // stopped because of solid collision
                            collidedX = signX

                            // go back before the collision
                            super.move(-STEP * signX, 0f, true, 1f)
                        }

                        xEnded = true
                    }
                }

                if (!yEnded) {
                    super.move(0f, STEP * signY, true, 1f)
                    yAdded += STEP

                    val hasSolidCollision = isCollidingWithMapCell()
                    if (hasSolidCollision || yAdded >= yTotalMoved) {
                        if (hasSolidCollision) {
                            collidedY = signY

                            super.move(0f, -STEP * signY, true, 1f)
                        }

                        yEnded = true
                    }
                }

                if (xEnded && yEnded) {
                    break
                }
            }
        }

    }

    /** Whether in this frame, after processing movement, the Entity had to fix a collision with solid entities */
    fun hasCollided(): Boolean {
        return hasCollided
    }

    /** Whether in this frame it has done collision response in X coordinate with a entity
     * @return 0 if false, 1 if true going to the right, -1 if true going to the left
     */
    fun hasCollidedX(): Int {
        return collidedX
    }

    /** Whether in this frame it has done collision response in Y coordinate with a solid entity
     * @return 0 if false, 1 if true going upwards, -1 if true going downwards
     */
    fun hasCollidedY(): Int {
        return collidedY
    }

    override fun saveEntity(onlySaveProperties: Boolean): JsonValue {
        return super.saveEntity(onlySaveProperties).also {
            it.addChildValue("speed", Json().toJson(speed))
            it.addChildValue("onlyCollideAgainstMapCells", onlyCollideAgainstMapCells)
        }
    }

    override fun loadEntity(jsonSavedObject: JsonValue, saveVersion: String) {
        super.loadEntity(jsonSavedObject, saveVersion)
        speed.set(Json().fromJson(Vector2::class.java, jsonSavedObject.getString("speed")))
        onlyCollideAgainstMapCells = jsonSavedObject.getBoolean("onlyCollideAgainstMapCells")
    }
}
