package org.magiclib.util.api

import org.json.JSONArray
import org.json.JSONObject

object JSONUtils {

    @JvmStatic
    fun jsonToMap(json: JSONObject): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()

        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (key !is String) continue
            val value = json.get(key)

            map[key] = when (value) {
                is JSONObject -> jsonToMap(value)
                is JSONArray -> jsonToList(value)
                JSONObject.NULL -> null
                else -> value
            }
        }

        return map
    }

    @JvmStatic
    fun jsonToList(array: JSONArray): List<Any?> {
        val list = mutableListOf<Any?>()

        for (i in 0 until array.length()) {
            val value = array.get(i)

            list.add(
                when (value) {
                    is JSONObject -> jsonToMap(value)
                    is JSONArray -> jsonToList(value)
                    JSONObject.NULL -> null
                    else -> value
                }
            )
        }

        return list
    }

    @JvmStatic
    fun mapToJson(map: Map<*, *>): JSONObject {
        val json = JSONObject()

        for ((key, value) in map) {
            val stringKey = key?.toString() ?: continue

            json.put(
                stringKey, when (value) {
                    null -> JSONObject.NULL
                    is Map<*, *> -> mapToJson(value)
                    is List<*> -> listToJson(value)
                    else -> value
                }
            )
        }

        return json
    }

    @JvmStatic
    fun listToJson(list: List<*>): JSONArray {
        val array = JSONArray()

        for (value in list) {
            array.put(
                when (value) {
                    null -> JSONObject.NULL
                    is Map<*, *> -> mapToJson(value)
                    is List<*> -> listToJson(value)
                    else -> value
                }
            )
        }

        return array
    }
}