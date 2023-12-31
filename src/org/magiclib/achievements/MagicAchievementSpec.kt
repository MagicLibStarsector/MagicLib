package org.magiclib.achievements

import com.fs.starfarer.api.util.Misc
import org.json.JSONObject

/**
 * The specification, or blueprint, for an achievement. Does not contain player progress. A [MagicAchievement], which tracks progress, is created from this.
 */
open class MagicAchievementSpec(
    val modId: String,
    val modName: String,
    val id: String,
    var name: String,
    var description: String,
    var tooltip: String?,
    var script: String,
    var image: String?,
    var spoilerLevel: MagicAchievementSpoilerLevel,
    var rarity: MagicAchievementRarity,
) {
    fun toJsonObject(): JSONObject {
        val json = JSONObject()
        json.put("modId", modId)
        json.put("modName", modName)
        json.put("id", id)
        json.put("name", name)
        json.put("description", description)
        json.put("tooltip", tooltip)
        json.put("script", script)
        json.put("image", image)
        json.put("spoilerLevel", spoilerLevel.toString())
        json.put("rarity", rarity.toString())
        return json
    }

    companion object {
        @JvmStatic
        fun fromJsonObject(json: JSONObject): MagicAchievementSpec {
            val modId = json.getString("modId")
            val modName = json.getString("modName")
            val id = json.getString("id")
            val name = json.getString("name")
            val description = json.getString("description")
            val tooltip = json.optString("tooltip", null)
            val script = json.getString("script")
            val image = json.optString("image", null)
            val spoilerLevel = MagicAchievementSpoilerLevel.valueOf(
                json.optString("spoilerLevel", "VISIBLE").lowercase().let { Misc.ucFirst(it) })
            val rarity =
                MagicAchievementRarity.valueOf(json.optString("rarity", "COMMON").lowercase().let { Misc.ucFirst(it) })

            return MagicAchievementSpec(
                modId,
                modName,
                id,
                name,
                description,
                tooltip,
                script,
                image,
                spoilerLevel,
                rarity
            )
        }
    }
}