package org.magiclib.util.membermemory

@Deprecated("TODO: remove on 0.98.5a")
class MemberMemory {
    private val data: MutableMap<String, Any?> = mutableMapOf()
    var persistUntilSeen: Boolean = false

    fun shouldPersist(memberID: String, validIDs: Set<String>): Boolean {
        return memberID in validIDs || persistUntilSeen
    }

    fun set(key: String, value: Any?) {
        data[key] = value
    }

    fun get(key: String): Any? {
        return data[key]
    }

    fun containsKey(key: String): Boolean {
        return data.containsKey(key)
    }

    fun unset(key: String) {
        data.remove(key)
    }

    fun asMap(): Map<String, Any?> = data
}