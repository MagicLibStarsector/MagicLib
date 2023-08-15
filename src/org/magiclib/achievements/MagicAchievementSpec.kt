package org.magiclib.achievements

import org.json.JSONObject

data class MagicAchievementSpec(
    val modId: String,
    val id: String,
    var name: String,
    var description: String,
    var script: String,
    var image: String?,
    var hasProgressBar: Boolean,
    var spoilerLevel: SpoilerLevel,
) {
    fun clone() = this.copy()

    fun toJsonObject(): JSONObject {
        val json = JSONObject()
        json.put("modId", modId)
        json.put("id", id)
        json.put("name", name)
        json.put("description", description)
        json.put("script", script)
        json.put("image", image)
        json.put("hasProgressBar", hasProgressBar)
        json.put("spoilerLevel", spoilerLevel.toString())
        return json
    }

    companion object {
        @JvmStatic
        fun fromJsonObject(json: JSONObject): MagicAchievementSpec {
            val modId = json.getString("modId")
            val id = json.getString("id")
            val name = json.getString("name")
            val description = json.getString("description")
            val script = json.getString("script")
            val image = json.optString("image", null)
            val hasProgressBar = json.optBoolean("hasProgressBar", false)
            val spoilerLevel = SpoilerLevel.valueOf(json.optString("spoilerLevel", "VISIBLE"))
            return MagicAchievementSpec(modId, id, name, description, script, image, hasProgressBar, spoilerLevel)
        }
    }
}