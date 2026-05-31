package org.magiclib.util.membermemory

import org.magiclib.util.membermemory.MemberMemoryManager.getMemberMemoryStore

object MemberMemoryAccess {
    const val SECTOR_MEMBER_MEMORY_KEY = "\$ML_MemberMemoryStore"

    /**
     * Member must be present in either an active fleet or in storage before game save, otherwise the memory related to it will be removed on game save as it is considered no longer existing.
     */
    @JvmStatic
    fun getMemberMemory(memberID: String): MutableMap<String, Any?> {
        return getMemberMemoryStore().getMemberMemory(memberID)
    }

    @JvmStatic
    fun unsetMemberMemory(memberID: String) {
        getMemberMemoryStore().unsetMemberMemory(memberID)
    }
}