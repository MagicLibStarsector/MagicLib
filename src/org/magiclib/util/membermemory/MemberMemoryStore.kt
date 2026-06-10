package org.magiclib.util.membermemory

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.rules.MemoryAPI

class MemberMemoryStore {
    // Member ID -> MemberMemory
    @Deprecated("TODO: remove on 0.98.5a")
    private val members: MutableMap<String, MemberMemory> = mutableMapOf()

    private val membersMap: MutableMap<String, MemoryAPI> = mutableMapOf()

    fun set(memberID: String, key: String, value: Any?) {
        getMemberMemory(memberID).set(key, value)
    }

    fun get(memberID: String, key: String): Any? {
        return getMemberMemory(memberID).get(key)
    }

    fun containsKey(memberID: String, key: String): Boolean {
        return membersMap[memberID]?.contains(key) == true
    }

    fun unsetKey(memberID: String, key: String) {
        membersMap[memberID]?.unset(key)
    }

    fun getMemberIDs(): Set<String> {
        return membersMap.keys
    }

    fun unsetMemberMemory(memberID: String) {
        membersMap.remove(memberID)
    }

    @JvmOverloads
    fun getMemberMemory(memberID: String, persistUntilSeen: Boolean = false): MemoryAPI {
        return membersMap.getOrPut(memberID) {
            Global.getFactory().createMemory().apply {
                if(persistUntilSeen)
                    this.set("\$ML_persistUntilSeen", true)
            }
        }
    }

    /**
     * Syncs stored members with [memberIDs].
     *
     * - Marks present members as seen (clears persistUntilSeen)
     * - Removes absent members unless persistUntilSeen is true
     */
    fun syncMembers(memberIDs: Set<String>) {
        val iterator = membersMap.entries.iterator()

        while (iterator.hasNext()) {
            val (memberID, memory) = iterator.next()

            val isPresent = memberID in memberIDs
            val persistUntilSeen = memory.contains("\$ML_persistUntilSeen")

            if (isPresent) {
                if (persistUntilSeen)
                    memory.unset("\$ML_persistUntilSeen")
            } else if (!persistUntilSeen) {
                iterator.remove()
            }
        }
    }
}