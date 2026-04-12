package org.magiclib.util.memberMemory

import com.fs.starfarer.api.fleet.FleetMemberAPI

object MemberMemoryExt {
    /**
     * Delegate for [MemberMemoryAccess.getMemberMemory]
     *
     * It is suggested to read the documentation on what this delegates to before using.
     */
    fun FleetMemberAPI.getMemberMemory() =
        MemberMemoryAccess.getMemberMemory(id)
}