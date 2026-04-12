package org.magiclib.util.memberMemory

import com.fs.starfarer.api.Global

object MemberMemoryAccess {
    const val SECTOR_MEMBER_MEMORY_KEY = "\$ML_MemberMemoryStore"

    private fun storeOrNull(): MemberMemoryStore? {
        val store = MemberMemoryManager.getMemberMemoryStore()
        if (store == null) {
            Global.getLogger(this::class.java).error(
                "MemberMemoryStore is null." +
                        "\nThe MemberMemoryStore loads in the onGameLoad of MagicLib. If your mod tries to get the memory of a member in onGameLoad and your mod is sooner in load order than MagicLib, an emptyMap will be returned." +
                        "\nYou may need to wait for MagicLib's onGameLoad to occur before running your code."
            )
        }
        return store
    }

    /**
     * Only functions in the campaign. Does not function anywhere else.
     *
     * The [MemberMemoryStore] loads in the onGameLoad of MagicLib. If your mod tries to get the memory of a member in onGameLoad and your mod is sooner in load order than MagicLib, an emptyMap will be returned.
     * You may need to wait for MagicLib's onGameLoad to occur before running your code. An error will show up in the log if this happens.
     *
     * Member must be present in either an active fleet or in storage before game save, otherwise the memory related to it will be removed as it is considered no longer existing.
     */
    @JvmStatic
    fun getMemberMemory(memberID: String): MutableMap<String, Any?> {
        return storeOrNull()?.getMemberMemory(memberID) ?: mutableMapOf()
    }

    @JvmStatic
    fun unsetMemberMemory(memberID: String) {
        storeOrNull()?.unsetMemberMemory(memberID)
    }
}