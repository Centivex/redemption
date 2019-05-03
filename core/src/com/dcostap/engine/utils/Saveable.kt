package com.dcostap.engine.utils

import com.badlogic.gdx.utils.JsonValue

/** Created by Darius on 7/16/2018. */
interface Saveable {
    fun save(): JsonValue
    fun load(json: JsonValue, saveVersion: String)
}