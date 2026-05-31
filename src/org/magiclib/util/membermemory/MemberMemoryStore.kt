package org.magiclib.util.membermemory

class MemberMemoryStore {
    // Member ID -> MemberMemory
    private val members: MutableMap<String, MemberMemory> = mutableMapOf()

    fun set(memberID: String, key: String, value: Any?) {
        getMemberMemory(memberID).set(key, value)
    }

    fun get(memberID: String, key: String): Any? {
        return getMemberMemory(memberID).get(key)
    }

    fun containsKey(memberID: String, key: String): Boolean {
        return members[memberID]?.containsKey(key) == true
    }

    fun unsetKey(memberID: String, key: String) {
        members[memberID]?.unset(key)
    }

    fun getMemberIDs(): Set<String> {
        return members.keys
    }

    fun unsetMemberMemory(memberID: String) {
        members.remove(memberID)
    }

    @JvmOverloads
    fun getMemberMemory(memberID: String, persistUntilSeen: Boolean = false): MemberMemory {
        return members.getOrPut(memberID) {
            MemberMemory().apply {
                this.persistUntilSeen = persistUntilSeen
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
        val iterator = members.entries.iterator()

        while (iterator.hasNext()) {
            val (memberID, memory) = iterator.next()

            val isPresent = memberID in memberIDs

            if (isPresent) {
                if (memory.persistUntilSeen)
                    memory.persistUntilSeen = false
            } else if (!memory.persistUntilSeen) {
                iterator.remove()
            }
        }
    }
}