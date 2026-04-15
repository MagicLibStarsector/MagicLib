package org.magiclib.util.memberMemory

import com.fs.starfarer.api.Global
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

        if(!store.getMemberIDs().isEmpty()) {
            // This shouldn't ever crash, but if it did, beforeGameSave would be a terrible place for it to happen. So prevent it anyway.
            val mostMemberIDs = try {
                 getMostMemberIDs()
            } catch(e: Exception) {
                Global.getLogger(this.javaClass).error("Failed to get member IDs", e)
                return
            }

            store.unsetMembersNotInSet(mostMemberIDs)
        }
    }

    private fun getMostMemberIDs(): Set<String> {
        val locations = Global.getSector().allLocations

        val submarkets = locations.flatMap { it.allEntities }.mapNotNull { it.market }.flatMap { it.submarketsCopy }

        val fleetMembers = listOf(
            locations.flatMap { it.fleets }.map { it.fleetData }, // Ships in active fleets.
            submarkets.mapNotNull { it.cargo?.mothballedShips },  // Ships in storage.
        ).flatten().flatMap { it.membersListCopy }

        val ids = fleetMembers.map { it.id }

        // While there shouldn't be duplicates, let's check just in case.
        val duplicates = ids.groupingBy { it }.eachCount()
            .filter { it.value > 1 }
            .keys

        if (duplicates.isNotEmpty()) {
            Global.getLogger(this.javaClass).error("Duplicate fleet member IDs detected: $duplicates")
        }

        return ids.toSet()
    }
}