package org.magiclib.util.api

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.combat.WeaponAPI
import kotlin.collections.component1
import kotlin.collections.component2

object VariantUtils {
    /**
     * Returns a map of all modules attached to this variant.
     *
     * Any slots with null variants are filtered out. Use [getModulesAllowNull] if you want to include null variants.
     *
     * The map key is the module slot ID, and the value is the corresponding [ShipVariantAPI] for that module.
     */
    @JvmStatic
    fun getModules(variant: ShipVariantAPI): Map<String, ShipVariantAPI> {
        // stationModules: weapon slot id -> original variant id
        val modules = variant.stationModules
            ?.mapNotNull { (slot, _) ->
                if (variant.hullSpec.getWeaponSlot(slot)?.weaponType != WeaponAPI.WeaponType.STATION_MODULE) {
                    Global.getLogger(this.javaClass).warn("Slot '$slot' of variantID '${variant.hullVariantId}' of hullID '${variant.hullSpec.hullId}' is not a station module despite a module being assigned to that slot?")
                    return@mapNotNull null
                }
                val variant: ShipVariantAPI? = variant.getModuleVariant(slot)
                variant?.let { slot to it }
            }
            ?.toMap() // converts the list of pairs back into a Map
            ?: emptyMap()

        return modules
    }

    /**
     * Returns a map of all modules attached to this variant.
     *
     * The map key is the module slot ID, and the value is the corresponding [ShipVariantAPI] for that module.
     */
    @JvmStatic
    fun getModulesAllowNull(
        variant: ShipVariantAPI
    ): Map<String, ShipVariantAPI?> {
        val modules = variant.stationModules
            ?.mapNotNull { (slot, _) ->
                if (variant.hullSpec.getWeaponSlot(slot)?.weaponType != WeaponAPI.WeaponType.STATION_MODULE) {
                    Global.getLogger(this.javaClass).warn("Slot '$slot' of variantID '${variant.hullVariantId}' of hullID '${variant.hullSpec.hullId}' is not a station module despite a module being assigned to that slot?")
                    return@mapNotNull null
                }
                val variant: ShipVariantAPI? = variant.getModuleVariant(slot)
                slot to variant
            }
            ?.toMap()
            ?: emptyMap()

        return modules
    }
}