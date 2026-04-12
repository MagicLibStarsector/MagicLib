package org.magiclib.util.memberMemory

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import org.magiclib.util.memberMemory.MemberMemoryAccess.SECTOR_MEMBER_MEMORY_KEY

internal object MemberMemoryManager {
    private var memberMemoryStore: MemberMemoryStore? = null
    private var currentGameState: GameState? = null
    fun getMemberMemoryStore(): MemberMemoryStore = getOrInitStore()

    private fun getOrInitStore(forceReload: Boolean = false): MemberMemoryStore {
        var existing = memberMemoryStore

        val currentGameState = Global.getCurrentState()
        if (currentGameState != this.currentGameState || forceReload) {
            this.currentGameState = currentGameState
            existing = null
        }

        if (existing != null) return existing

        val memory = Global.getSector().memoryWithoutUpdate

        val store = memory.get(SECTOR_MEMBER_MEMORY_KEY) as? MemberMemoryStore
            ?: MemberMemoryStore().also { memory.set(SECTOR_MEMBER_MEMORY_KEY, it) }

        memberMemoryStore = store
        return store
    }

    @JvmStatic
    fun onGameLoad() {
        getOrInitStore(true)
    }

    @JvmStatic
    fun beforeGameSave() {
        val store = memberMemoryStore ?: run {
            Global.getLogger(this.javaClass).error("MemberMemoryStore is null. This should never happen.")
            return
        }

        if(!store.getMemberIDs().isEmpty()) {
            val mostMemberIDs = getMostMemberIDs()
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