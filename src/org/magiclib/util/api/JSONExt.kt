@file:JvmName("JSONUtils")

package org.magiclib.util.api

import org.json.JSONArray
import org.json.JSONObject
import kotlin.collections.iterator


fun JSONObject.toMap(): Map<String, Any?> {
    val map = mutableMapOf<String, Any?>()

    val keys = this.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        if (key !is String) continue
        val value = this.get(key)

        map[key] = when (value) {
            is JSONObject -> value.toMap()
            is JSONArray -> value.toList()
            JSONObject.NULL -> null
            else -> value
        }
    }

    return map
}


fun JSONArray.toList(): List<Any?> {
    val list = mutableListOf<Any?>()

    for (i in 0 until this.length()) {
        val value = this.get(i)

        list.add(
            when (value) {
                is JSONObject -> value.toMap()
                is JSONArray -> value.toList()
                JSONObject.NULL -> null
                else -> value
            }
        )
    }

    return list
}

fun Map<*, *>.toJson(): JSONObject {
    val json = JSONObject()

    for ((key, value) in this) {
        val stringKey = key?.toString() ?: continue

        json.put(
            stringKey, when (value) {
                null -> JSONObject.NULL
                is Map<*, *> -> value.toJson()
                is List<*> -> value.toJson()
                else -> value
            }
        )
    }

    return json
}

fun List<*>.toJson(): JSONArray {
    val array = JSONArray()

    for (value in this) {
        array.put(
            when (value) {
                null -> JSONObject.NULL
                is Map<*, *> ->  value.toJson()
                is List<*> -> value.toJson()
                else -> value
            }
        )
    }

    return array
}