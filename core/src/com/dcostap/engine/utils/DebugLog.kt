package com.dcostap.engine.utils

import java.util.*

/** Created by Darius on 07-Nov-18. */
class DebugLog(storeLimit: Int = 80) {
    var log = Stack<String>()
    val appTimeStart = System.currentTimeMillis()
    private var lastDebugTime: Long = -1
    var printIt = false

    init {
        log.setSize(storeLimit)
    }

    fun printDebug(string: String = "\n") {
        val newString: String
        val elapsed = (System.currentTimeMillis() - appTimeStart) / 1000

        if (elapsed != lastDebugTime) {
            lastDebugTime = elapsed
            newString = "\n\n# ${elapsed}s elapsed #\n $string"
        } else {
            newString = "\n$string"
        }

        log.push(newString)

        if (printIt) {
            print(newString)
        }
    }

    fun printDebug(number: Number) {
        printDebug(number.toString())
    }

    override fun toString(): String {
        var string = ""
        for (str in log) {
            string += str ?: ""
        }
        return string
    }
}