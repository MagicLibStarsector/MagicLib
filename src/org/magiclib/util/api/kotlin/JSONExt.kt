package org.magiclib.util.api.kotlin

import org.json.JSONArray
import org.json.JSONObject
import org.magiclib.util.api.JSONUtils

/** Delegate to [JSONUtils.jsonToMap] */
fun JSONObject.toMap(): Map<String, Any?> = JSONUtils.jsonToMap(this)

/** Delegate to [JSONUtils.jsonToList] */
fun JSONArray.toList(): List<Any?> = JSONUtils.jsonToList(this)

/** Delegate to [JSONUtils.mapToJson] */
fun Map<*, *>.toJson(): JSONObject = JSONUtils.mapToJson(this)

/** Delegate to [JSONUtils.listToJson] */
fun List<*>.toJson(): JSONArray = JSONUtils.listToJson(this)
