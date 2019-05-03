package com.dcostap.engine.utils

import com.badlogic.gdx.utils.JsonValue
import com.dcostap.printDebug

/** Created by Darius on 19-Jul-18.
 *
 * A [JsonValue.ValueType.object] which allows to automatically save and load variables without
 * specifying a name: they are stored in order by the [counter], so you save and load
 * the variables on the same order for it to work. */
class JsonSavedObject : JsonValue(JsonValue.ValueType.`object`) {
    var counter: Int = 0

    private var justSaved = false

    fun saveValues(vararg values: Any) {
        for (v in values) {
            saveAnotherValue(v)
        }
    }

    private var id = "jsonSavedValueID"

    fun saveAnotherValue(value: Any) {
        if (!justSaved) {justSaved = true; counter = 0}
        try {
            addChildValue( id + counter.toString(), value)
        } catch (exc: Exception) {
            printDebug("(JsonSavedObject) Error when saving value: $value. \nException: ${exc.message}" +
                    "\nIgnored and continuing...")
        }
        counter++
    }

    fun <T : Any> loadAnotherValue(defaultValue: T): T {
        if (justSaved) {justSaved = false; counter = 0}
        return (getChildValue(id + counter.toString(), defaultValue) as T).also { counter++ }
    }
}