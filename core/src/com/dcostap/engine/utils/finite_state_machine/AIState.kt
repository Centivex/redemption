package com.dcostap.engine.utils.finite_state_machine

/** Created by Darius on 24-Oct-18. */
abstract class State {
    abstract fun enter()
    abstract fun execute(delta: Float)
    abstract fun exit()
    open fun debugInfo(): String {
        return this.javaClass.simpleName
    }
}

abstract class GlobalState : State()

abstract class SubState : State()

