@file:JvmName("JSONUtils")
package org.magiclib.kotlin

import org.json.JSONArray
import org.json.JSONObject
import org.lazywizard.lazylib.ext.json.getFloat


/**
 * Converts a `JSONArray` to a `List<String>`.
 *
 * @since 0.46.0
 */
fun JSONArray.toStringList(): List<String> {
    return MutableList(this.length()) {
        this.getString(it)
    }
        .filterNotNull()
}

/**
 * Converts a `JSONArray` to a `List<Long>`.
 *
 * @since 0.46.0
 */
fun JSONArray.toLongList(): List<Long> {
    return MutableList(this.length()) {
        this.getLong(it)
    }
}

/**
 * Tries to get a value from a `JSONObject` by key. The value must exist.
 * Usage:
 * ```kt
 * obj.tryGet<String>("key")
 * ```
 * @since 0.46.0
 */
inline fun <reified T> JSONObject.getObj(key: String): T =
    getJsonObj(this, key)

/**
 * Tries to get a value from a `JSONObject` by key, returning `default` if the key is not found.
 * Usage:
 * ```kt
 * obj.tryGet<String>("key") { "default" }
 * ```
 * @since 0.46.0
 */
inline fun <reified T> JSONObject.tryGet(key: String, default: () -> T): T =
    runCatching { getJsonObj<T>(this, key) }
        .getOrDefault(default())

/**
 * Gets an object from a `JSONObject` by key, returning `default` (null if not specified) if the key is not found.
 *
 * Usage:
 * ```kt
 * jsonObj.optional<String>("key")
 * jsonObj.options<String>("key") { "default" }
 * ```
 *
 * @since 0.46.0
 */
inline fun <reified T> JSONObject.optional(key: String, default: () -> T? = { null }): T? =
    runCatching { getJsonObj<T>(this, key) }
        .getOrDefault(default())

/**
 *
 * Usage:
 * ```kt
 * jsonArray.forEach<String> { obj -> doSomething(obj) }
 * ```
 *
 * Optionally, you can specify a transform function to manually convert the object to type `T`.
 *
 * @since 0.46.0
 */
inline fun <reified T> JSONArray.forEach(
    transform: (JSONArray, Int) -> T = { json, i -> getJsonObjFromArray(json, i) },
    action: (T) -> Unit
) {
    for (i in (0 until this.length()))
        action.invoke(transform.invoke(this, i))
}

/**
 * Usage:
 * ```kt
 * jsonArray.map<String> { obj -> doSomething(obj) }
 * ```
 *
 * Optionally, you can specify a transform function to manually convert the object to type `T`.
 *
 * @since 0.46.0
 */
inline fun <reified T, K> JSONArray.map(
    transform: (JSONArray, Int) -> T = { json, i -> getJsonObjFromArray(json, i) },
    action: (T) -> K
): List<K> {
    val results = mutableListOf<K>()

    for (i in (0 until this.length()))
        results += action.invoke(transform.invoke(this, i))

    return results
}

/**
 *
 * Usage:
 * ```kt
 *  jsonArray.filter<String> { obj -> obj == "something" }
 *  ```
 *
 * Optionally, you can specify a transform function to manually convert the object to type `T`.
 *
 * @since 0.46.0
 */
inline fun <reified T> JSONArray.filter(
    transform: (JSONArray, Int) -> T = { json, i -> getJsonObjFromArray(json, i) },
    predicate: (T) -> Boolean
): List<T> {
    val results = mutableListOf<T>()

    for (i in (0 until this.length())) {
        val obj = transform.invoke(this, i)

        if (predicate.invoke(obj)) {
            results += obj
        }
    }

    return results
}

/**
 * Gets an object from a JSONArray, automatically converting it to the specified type.
 *
 * @since 0.46.0
 */
inline fun <reified T> getJsonObjFromArray(json: JSONArray, i: Int) =
    when (T::class) {
        String::class -> json.getString(i) as T
        Float::class -> json.getFloat(i) as T
        Int::class -> json.getInt(i) as T
        Boolean::class -> json.getBoolean(i) as T
        Double::class -> json.getDouble(i) as T
        JSONArray::class -> json.getJSONArray(i) as T
        Long::class -> json.getLong(i) as T
        else -> json.getJSONObject(i) as T
    }

/**
 * Gets an object from a JSONObject, automatically converting it to the specified type.
 *
 * @since 0.46.0
 */
inline fun <reified T> getJsonObj(json: JSONObject, key: String) =
    when (T::class) {
        String::class -> json.getString(key) as T
        Float::class -> json.getFloat(key) as T
        Int::class -> json.getInt(key) as T
        Boolean::class -> json.getBoolean(key) as T
        Double::class -> json.getDouble(key) as T
        JSONArray::class -> json.getJSONArray(key) as T
        Long::class -> json.getLong(key) as T
        else -> json.getJSONObject(key) as T
    }




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