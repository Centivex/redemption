package com.dcostap.engine.utils.finite_state_machine

import com.badlogic.gdx.utils.ObjectMap
import com.dcostap.engine.utils.DebugLog
import com.dcostap.engine.utils.ifNotNull
import com.dcostap.printDebug

/**
 * Created by Darius on 31/08/2017.
 *
 * Custom implementation of a Finite State Machine
 * There may be only one globalState and one state executing at the same time.
 * However there can be any number of subStates executing at the same time. **All subStates will exit when changing state**
 */
class StateMachine @JvmOverloads constructor(currentState: State? = null, var globalState: GlobalState? = null)  {
    var currentState: State? = null
        private set
    var previousState: State? = null
        private set

    private var delay = 0f

    private val subStates = ObjectMap<SubState, Boolean>()

    init {
        currentState.ifNotNull { this.changeState(it) }
    }

    fun update(delta: Float) {
        if (delay > 0) {
            delay = Math.max(0f, delay - delta)
            return
        }

        globalState?.execute(delta)

        for (entry in subStates.entries()) {
            if (entry.value) {
                entry.key.execute(delta)
            }
        }

        if (currentState != null) {
            currentState!!.execute(delta)
        }
    }

    fun changeState(newState: State) {
        exitAllSubStates()
        previousState = currentState

        currentState?.exit()
        currentState = newState
        currentState?.enter()
    }

    fun exitState() {
        exitAllSubStates()
        previousState = currentState
        currentState?.exit()
        currentState = null
    }

    val debugInfo = false

    private fun printDebugStateInfo(string: String) {
        if (debugInfo) {
            printDebug(string)
        }
    }

    /**
     * Enters the indicated subState; if it is already executing, first exits it then enters (resets the subState).
     * If you want to set some variables before starting the new subState, make sure to call the exit method before, since
     * variables are normally reset on exit.
     */
    fun setSubState(subState: SubState) {
        if (subStates.get(subState, false)) {
            printDebugStateInfo("--> StateMachine: Exit substate ${subState.debugInfo()}")
            subState.exit()
        }

        subStates.put(subState, true)
        subState.enter()
        printDebugStateInfo("--> StateMachine: Enter substate ${subState.debugInfo()}")
    }

    fun exitSubState(subState: SubState) {
        if (subStates.get(subState, false)) {
            subStates.put(subState, false)
            subState.exit()
            printDebugStateInfo("--> StateMachine: Exit substate ${subState.debugInfo()}")
        }
    }

    fun exitAllSubStates() {
        for (subState in subStates.keys()) {
            exitSubState(subState)
        }
    }

    fun exitAllSubStatesBut(exception: SubState) {
        for (subState in subStates.keys()) {
            if (subState != exception) exitSubState(subState)
        }
    }

    fun isSubStateSet(subState: SubState): Boolean {
        return subStates.get(subState, false)
    }

    fun revertToPreviousState() {
        if (previousState != null) {
            changeState(previousState!!)
        }
    }

    val debugLog = DebugLog()

    fun stPrintDebug(string: String?) {
        debugLog.printDebug(string ?: "\n")
    }
}
