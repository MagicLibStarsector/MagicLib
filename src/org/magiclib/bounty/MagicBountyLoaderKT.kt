package org.magiclib.bounty

import com.fs.starfarer.api.Global
import org.json.JSONArray
import org.json.JSONObject
import org.magiclib.bounty.MagicBountyLoader.magicVariantExists
import org.magiclib.kotlin.toList

internal object MagicBountyLoaderKT {
    @JvmStatic
    fun loadShipArray(shipArray: JSONArray): Pair<String, Int>? {
        val shipList = shipArray.toList()

        var first_valid = false
        var optional = false
        var amount = 1

        val ships = mutableListOf<String>()

        for (entry in shipList) {
            if(entry is String) {
                if(entry.startsWith("$")) {
                    if(entry.contains("first_valid"))
                        first_valid = true
                    if(entry.contains("optional"))
                        optional = true
                } else {
                    ships.add(entry)
                }
            } else if(entry is Int) {
                amount = entry
            } else {
                Global.getLogger(this.javaClass).warn("Unknown variable type: ${entry?.javaClass}. Bounty is INVALID")
                return null
            }
        }

        if(ships.isEmpty()) {
            Global.getLogger(this.javaClass).warn("No ships specified in array. Bounty is INVALID")
            return null
        }

        val validShips = ships.filter { magicVariantExists(it) }

        if(validShips.isEmpty()) {
            return if(optional) {
                "" to 0
            } else {
                Global.getLogger(this.javaClass).warn("No valid ships specified in array. Bounty is INVALID")
                null
            }
        }

        return if(first_valid) {
            validShips.first() to amount
        } else {
            validShips.random() to amount
        }
    }

    @JvmStatic
    fun loadShipMap(shipObject: JSONObject): Map<String, Int>? {
        val output = mutableMapOf<String, Int>()

        val keys = shipObject.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (key !is String) continue
            val value = shipObject.get(key)

            val currentAmount = output.getOrDefault(key, 0)

            if(value is Int) {
                output[key] = currentAmount + value
            } else {
                Global.getLogger(this.javaClass).warn("Unknown variable type: ${value?.javaClass}. Bounty is INVALID")
                return null
            }
        }

        return output
    }

    @JvmStatic
    fun loadShipMap(shipArray: JSONArray): Map<String, Int>? {
        val output = mutableMapOf<String, Int>()

        for (i in 0 until shipArray.length()) {
            val value = shipArray.get(i) as JSONArray

            val test = loadShipArray(value) ?: return null
            val ship = test.first
            val amount = test.second
            if(amount == 0)
                continue

            val currentAmount = output.getOrDefault(ship, 0)

            output[ship] = currentAmount + amount
        }

        return output
    }

}