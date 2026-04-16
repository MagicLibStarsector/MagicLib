package org.magiclib.util.memberMemory

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import org.magiclib.util.memberMemory.MemberMemoryAccess.SECTOR_MEMBER_MEMORY_KEY

internal object MemberMemoryManager {
    fun getMemberMemoryStore(): MemberMemoryStore = getOrInitStore()

    private fun getOrInitStore(): MemberMemoryStore {
        val memory = Global.getSector().memoryWithoutUpdate

        return memory?.get(SECTOR_MEMBER_MEMORY_KEY) as? MemberMemoryStore
            ?: MemberMemoryStore().also { memory?.set(SECTOR_MEMBER_MEMORY_KEY, it) }
    }

    @JvmStatic
    fun beforeGameSave() {
        val store = getOrInitStore()

        if (!store.getMemberIDs().isEmpty()) {
            // This shouldn't ever crash, but if it did, beforeGameSave would be a terrible place for it to happen. So prevent it anyway.
            val mostMemberIDs = try {
                getMostMemberIDs()
            } catch (e: Exception) {
                Global.getLogger(this.javaClass).error("Failed to get member IDs", e)
                return
            }

            store.unsetMembersNotInSet(mostMemberIDs)
        }
    }

    private fun getMostMemberIDs(): Set<String> {
        val locations = Global.getSector().allLocations

        val markets = locations
            .flatMap { it.allEntities }
            .mapNotNull { it.market }
            .distinctBy { it.id } // A planet and the planet's station may link to the same market. This prevents that

        val storages = markets
            .flatMap { it.submarketsCopy }
            .mapNotNull { it.cargo }
            .distinctBy { it } // Mods such as Trails of Tooth and Claw share cargo. This avoids that causing mis-reporting duplicated members.

        val fleetMembers = listOf(
            locations.flatMap { it.fleets }.map { it.fleetData }, // Ships in active fleets.
            storages.mapNotNull { it.mothballedShips },  // Ships in storage.
        ).flatten().flatMap { it.membersListCopy }

        val ids = fleetMembers.map { it.id }

        // While there shouldn't be duplicates in normal circumstances, let's check just in case. If any duplicates exist, it's usually fault of a mod.
        if (ids.groupingBy { it }.eachCount().any { it.value > 1 }) {
            duplicateLocationReporter() // Report where duplicates member IDs are into the log.
        }

        return ids.toSet()
    }

    private fun duplicateLocationReporter() {
        val locations = Global.getSector().allLocations

        val markets = locations
            .flatMap { it.allEntities }
            .mapNotNull { it.market }
            .distinctBy { it.id }

        data class StorageRef(
            val market: MarketAPI,
            val submarket: SubmarketAPI,
            val cargo: CargoAPI
        )

        val storages = markets.flatMap { market ->
            market.submarketsCopy.mapNotNull { sub ->
                val cargo = sub.cargo ?: return@mapNotNull null
                StorageRef(market, sub, cargo)
            }
        }.distinctBy { it.cargo }

        // id -> list of human-readable locations
        val idMap = mutableMapOf<String, MutableList<String>>()

        // === ACTIVE FLEETS ===
        for (loc in locations) {
            for (fleet in loc.fleets) {
                val fleetName = fleet.name

                for (member in fleet.fleetData.membersListCopy) {
                    val id = member.id

                    idMap.getOrPut(id) { mutableListOf() }
                        .add("Fleet: $fleetName | Ship: ${member.shipName}")
                }
            }
        }

        // === STORAGE (MOTHBALLED SHIPS) ===
        for (storage in storages) {
            val sub = storage.submarket
            val market = storage.market
            val cargo = storage.cargo

            val mothballed = cargo.mothballedShips ?: continue

            val where = "Storage: ${sub.specId} @ ${market.name}"

            for (member in mothballed.membersListCopy) {
                val id = member.id

                idMap.getOrPut(id) { mutableListOf() }
                    .add("$where | Ship: ${member.shipName}")
            }
        }

        // === REPORT DUPLICATES ===
        val sb = StringBuilder()
        var duplicates = 0

        for ((id, list) in idMap) {
            if (list.size > 1) {
                duplicates++

                sb.appendLine("=== DUPLICATE ID: $id ===")

                for (entry in list) {
                    sb.appendLine("  $entry")
                }
                sb.appendLine()
            }
        }

        sb.appendLine("Duplicate Member IDs found: $duplicates")

        Global.getLogger(this.javaClass).warn(sb.toString())
    }
}