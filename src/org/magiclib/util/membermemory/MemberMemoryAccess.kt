package org.magiclib.util.membermemory

import com.fs.starfarer.api.campaign.rules.MemoryAPI
import org.magiclib.util.membermemory.MemberMemoryManager.getMemberMemoryStore

object MemberMemoryAccess {
    const val SECTOR_MEMBER_MEMORY_KEY = "\$ML_MemberMemoryStore"

    /**
     * Member must be present in either an active fleet or in storage before game save, otherwise the memory related to it will be removed on game save as it is considered no longer existing.
     *
     * @param persistUntilSeen If true, the memory will be kept regardless until the member appears in an active fleet or storage for the first time.
     */
    @JvmOverloads
    @JvmStatic
    fun getMemberMemory(memberID: String, persistUntilSeen: Boolean = false): MemoryAPI {
        return getMemberMemoryStore().getMemberMemory(memberID, persistUntilSeen)
    }

    @JvmStatic
    fun unsetMemberMemory(memberID: String) {
        getMemberMemoryStore().unsetMemberMemory(memberID)
    }
}