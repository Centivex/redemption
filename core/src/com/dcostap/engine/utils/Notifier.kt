package com.dcostap.engine.utils

import com.badlogic.gdx.utils.Array

/**
 * Created by Darius on 06/09/2017.
 *
 * Common class for all notifiers
 */
open class Notifier<T> {
    var listeners = Array<T>()

    fun registerListener(listener: T) {
        listeners.add(listener)
    }

    fun removeListener(listener: T) {
        listeners.removeValue(listener, true)
    }

    fun notifyListeners(f: (T) -> Unit) {
        for (listener in listeners) {
            f(listener)
        }
    }
}