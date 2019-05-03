package com.dcostap.engine.map.map_loading

import ktx.collections.GdxMap

/** Created by Darius on 19/05/2018. */
class CustomProperties {
    val booleans = GdxMap<String, Boolean>()
    val strings = GdxMap<String, String>()
    val floats = GdxMap<String, Float>()
    val ints = GdxMap<String, Int>()

    fun addValue(name: String, value: Any) {
        when (value) {
            is Boolean -> booleans.put(name, value)
            is String -> strings.put(name, value)
            is Int -> ints.put(name, value)
            is Float -> floats.put(name, value)
            else -> throw RuntimeException("Tried to add value: $value to CustomProperties; but value has no supported type")
        }
    }

    override fun toString(): String {
        return booleans.toString() + ", " + ints.toString() + ", " + floats.toString() + ", " + strings.toString()
    }
}