package org.magiclib.util.memberMemory

@Deprecated("Use MemberMemoryStore in membermemory instead.") // TODO, remove this on 0.98.5a
class MemberMemoryStore {
    // Member ID first, then the key
    val memKeys: MutableMap<String, MutableMap<String, Any?>> = mutableMapOf()

    fun set(memberID: String, key: String, value: Any?) {
        val memberMemory = getMemberMemory(memberID)
        memberMemory[key] = value
    }

    fun get(memberID: String, key: String): Any? {
        return getMemberMemory(memberID)[key]
    }

    fun containsKey(memberID: String, key: String): Boolean {
        return memKeys[memberID]?.containsKey(key) == true
    }

    fun unsetKey(memberID: String, key: String) {
        memKeys[memberID]?.remove(key)
    }

    fun getMemberIDs(): Set<String> {
        return memKeys.keys
    }

    fun unsetMemberMemory(memberID: String) {
        memKeys.remove(memberID)
    }

    fun getMemberMemory(memberID: String): MutableMap<String, Any?> {
        return memKeys[memberID] ?: run {
            val newMemberMemory = mutableMapOf<String, Any?>()
            memKeys[memberID] = newMemberMemory
            newMemberMemory
        }
    }

    fun unsetMembersNotInSet(memberIDs: Set<String>) {
        memKeys.keys.retainAll(memberIDs)
    }
}