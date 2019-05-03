package com.dcostap.engine.map.map_loading

import com.dcostap.engine.map.entities.Entity

/** Created by Darius on 20-Jul-18. */
interface EntityLoaderFromClass {
    fun loadEntity(clazz: Class<Entity>): Entity?
}