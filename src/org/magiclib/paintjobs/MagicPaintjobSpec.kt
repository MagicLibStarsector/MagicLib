package org.magiclib.paintjobs

data class MagicPaintjobSpec @JvmOverloads constructor(
    val modId: String,
    val modName: String,
    val id: String,
    val hullId: String,
    var name: String,
    var description: String? = null,
    var unlockedAutomatically: Boolean = true,
    var spriteId: String,
    var tags: List<String>?,
) {
    val isShiny: Boolean
        get() = tags?.contains(MagicPaintjobManager.PJTAG_SHINY) == true

    val isPermament: Boolean
        get() = tags?.contains(MagicPaintjobManager.PJTAG_PERMA_PJ) == true
//    fun toJsonObject(): JSONObject {
//        val json = JSONObject()
//        json.put("id", id)
//        json.put("hullId", hullId)
//        json.put("name", name)
//        json.put("description", description)
//        json.put("spriteId", spriteId)
//        return json
//    }
//
//    companion object {
//        @JvmStatic
//        fun fromJsonObject(json: JSONObject): MagicPaintjobSpec {
//            val id = json.getString("id")
//            val hullId = json.getString("hullId")
//            val name = json.getString("name")
//            val description = json.optString("description")
//            val spriteId = json.getString("spriteId")
//
//            return MagicPaintjobSpec(
//                id,
//                hullId,
//                name,
//                description,
//                spriteId,
//            )
//        }
//    }
}