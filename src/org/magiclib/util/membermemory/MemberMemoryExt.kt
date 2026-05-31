package org.magiclib.util.membermemory

import com.fs.starfarer.api.fleet.FleetMemberAPI

object MemberMemoryExt {
    /**
     * Delegate for [MemberMemoryAccess.getMemberMemory]
     *
     * It is suggested to read the documentation on what this delegates to before using.
     */
    fun FleetMemberAPI.getMemberMemory(persistUntilSeen: Boolean = false) =
        MemberMemoryAccess.getMemberMemory(id, persistUntilSeen)
}