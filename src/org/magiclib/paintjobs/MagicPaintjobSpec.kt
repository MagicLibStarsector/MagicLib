package org.magiclib.paintjobs

data class MagicPaintjobSpec @JvmOverloads constructor(
    val id: String,
    val hullId: String,
    var name: String,
    var description: String? = null,
    var spriteId: String,
) {
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