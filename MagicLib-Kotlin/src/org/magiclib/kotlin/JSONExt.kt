package org.magiclib.kotlin

import org.json.JSONArray
import org.json.JSONObject
import org.lazywizard.lazylib.ext.json.getFloat


/**
 * @since 0.46.0
 */
fun JSONArray.toStringList(): List<String> {
    return MutableList(this.length()) {
        this.getString(it)
    }
        .filterNotNull()
}

/**
 * @since 0.46.0
 */
fun JSONArray.toLongList(): List<Long> {
    return MutableList(this.length()) {
        this.getLong(it)
    }
}

/**
 * @since 0.46.0
 */
inline fun <reified T> JSONObject.getObj(key: String): T =
    getJsonObj(this, key)

/**
 * @since 0.46.0
 */
inline fun <reified T> JSONObject.tryGet(key: String, default: () -> T): T =
    kotlin.runCatching { getJsonObj<T>(this, key) }
        .getOrDefault(default())

/**
 * @since 0.46.0
 */
inline fun <reified T> JSONObject.optional(key: String, default: () -> T? = { null }): T? =
    kotlin.runCatching { getJsonObj<T>(this, key) }
        .getOrDefault(default())

/**
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