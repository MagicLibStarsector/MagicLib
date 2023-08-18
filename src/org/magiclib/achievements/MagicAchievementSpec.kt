package org.magiclib.achievements

import com.fs.starfarer.api.util.Misc
import org.json.JSONObject

data class MagicAchievementSpec(
    val modId: String,
    val id: String,
    var name: String,
    var description: String,
    var script: String,
    var image: String?,
    var hasProgressBar: Boolean,
    var spoilerLevel: MagicAchievementSpoilerLevel,
    var rarity: MagicAchievementRarity,
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
        json.put("rarity", rarity.toString())
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
            val spoilerLevel = MagicAchievementSpoilerLevel.valueOf(json.optString("spoilerLevel", "VISIBLE").lowercase().let { Misc.ucFirst(it) })
            val rarity = MagicAchievementRarity.valueOf(json.optString("rarity", "COMMON").lowercase().let { Misc.ucFirst(it) })
            return MagicAchievementSpec(modId, id, name, description, script, image, hasProgressBar, spoilerLevel, rarity)
        }
    }
}