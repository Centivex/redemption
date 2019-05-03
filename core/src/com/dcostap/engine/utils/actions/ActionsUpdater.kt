package com.dcostap.engine.utils.actions

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.utils.Array
import com.dcostap.engine.utils.Updatable
import ktx.collections.*

/**
 * Created by Darius on 15/04/2018.
 */
class ActionsUpdater : Updatable {
    private val actions = Array<Action>()
    private val dummy = Array<Action>()

    private val tags = GdxSet<String>()

    val size
        get() = actions.size

    val isEmpty
        get() = actions.size == 0

    /**
     * Allows to add actions with += operator.
     */
    operator fun plusAssign(action: Action) = add(action)

    /**
     * Allows to remove actions with -= operator.
     */
    operator fun minusAssign(action: Action) = removeAction(action)

    override fun update(delta: Float) {
        dummy.clear()

        for (action in actions) {
            action.update(delta)

            if (action.hasFinished) {
                dummy.add(action)
            }
        }

        for (action in dummy) {
            removeTags(action)
        }

        actions.removeAll(dummy, true)
    }

    fun add(action: Action) {
        action.reset()
        actions.add(action)
        addTags(action)
    }

    private fun addTags(action: Action) {
        if (action.tag != "")
            tags.add(action.tag)
        if (action is SequenceAction) {
            for (a in action.sequenceOfActions) addTags(a)
        } else if (action is ParallelAction) {
            for (a in action.sequenceOfActions) addTags(a)
        }
    }

    private fun removeTags(action: Action) {
        if (action.tag != "")
            tags.remove(action.tag)
        if (action is SequenceAction) {
            for (a in action.sequenceOfActions) removeTags(a)
        } else if (action is ParallelAction) {
            for (a in action.sequenceOfActions) removeTags(a)
        }
    }

    fun add(vararg actions: Action) {
        add(Action.sequence(*actions))
    }

    /** looks for tags in all actions inside, including actions inside sequences or parallel actions */
    fun hasTag(tag: String): Boolean = tags.contains(tag)

    fun removeAction(action: Action) {
        actions.removeValue(action, true)
        removeTags(action)
    }

    fun clear() {
        tags.clear()
        actions.clear()
    }
}
