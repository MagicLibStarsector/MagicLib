package org.magiclib.util

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.SkillSpecAPI
import com.fs.starfarer.api.combat.ShipHullSpecAPI
import com.fs.starfarer.api.combat.ShipSystemSpecAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.loading.FighterWingSpecAPI
import com.fs.starfarer.api.loading.HullModSpecAPI
import com.fs.starfarer.api.loading.VariantSource
import com.fs.starfarer.api.loading.WeaponSpecAPI
import org.apache.log4j.Level
import org.magiclib.LunaWrapper
import org.magiclib.util.api.getActualHullId
import org.magiclib.util.api.getEffectiveHullId
import org.magiclib.util.api.removeModFull
import org.magiclib.util.internal.AssignHullSkinSourceMod.assignHullSkinSourceMods
import org.magiclib.util.internal.MiscellaneousUtil.findMissingElements

object MagicLookup {
    init {
        setup()
    }

    private lateinit var allDMods: Set<String>
    private lateinit var allHiddenEverywhereMods: Set<String>

    private lateinit var allVariants: List<ShipVariantAPI>
    private lateinit var hullIDToVariant: Map<String, List<ShipVariantAPI>>
    private lateinit var effectiveHullIDToVariant: Map<String, List<ShipVariantAPI>>
    private lateinit var baseHullIDToVariant: Map<String, List<ShipVariantAPI>>
    private lateinit var hullIDSet: Set<String>
    private lateinit var IDToHullSpec: Map<String, ShipHullSpecAPI>
    private lateinit var IDToWing: Map<String, FighterWingSpecAPI>
    private lateinit var IDToWeapon: Map<String, WeaponSpecAPI>
    private lateinit var IDToHullMod: Map<String, HullModSpecAPI>
    private lateinit var IDToSkill: Map<String, SkillSpecAPI>
    private lateinit var allFactionIDs: Set<String>
    private lateinit var IDToShipSystem: Map<String, ShipSystemSpecAPI>
    private var init = false
    fun isSetup() = init

    internal fun setup() {
        val settings = Global.getSettings()

        hullIDSet = settings.allShipHullSpecs.map { it.hullId }.toSet()

        if (hullIDSet.isEmpty())
            Global.getLogger(this.javaClass).error("No hulls found. It is very likely that the '${this.javaClass.name}' object was accessed before onApplicationLoad. Avoid calling ${this.javaClass.name} before onApplicationLoad")

        allDMods = settings.allHullModSpecs
            .asSequence()
            .filter { it.hasTag(Tags.HULLMOD_DMOD) }
            .map { it.id }
            .toSet()

        allHiddenEverywhereMods = settings.allHullModSpecs
            .asSequence()
            .filter { it.isHiddenEverywhere }
            .map { it.id }
            .toSet()

        IDToHullSpec = settings.allShipHullSpecs.associateBy { it.hullId }
        IDToWing = settings.allFighterWingSpecs.associateBy { it.id }
        IDToHullMod = settings.allHullModSpecs.associateBy { it.id }
        IDToWeapon = settings.actuallyAllWeaponSpecs.associateBy { it.weaponId }
        IDToSkill = settings.skillIds.map { settings.getSkillSpec(it) }.associateBy { it.id }
        allFactionIDs = settings.allFactionSpecs.map { it.id }.toSet()
        IDToShipSystem = settings.allShipSystemSpecs.associateBy { it.id }


        allVariants = settings.allVariantIds.mapNotNull { runCatching { settings.getVariant(it) }.getOrNull() }

        if(LunaWrapper.getBoolean(MagicVariables.MAGICLIB_ID, "magiclib_FixMissionVariantError", default = true))
            cleanMissionVariantsForRemovedElements(allVariants)
        if(LunaWrapper.getBoolean(MagicVariables.MAGICLIB_ID, "magiclib_AssignMissingSourceMods", default = true))
            assignHullSkinSourceMods()

        // Log a warning if any variant had missing elements (hull-mod, weapon, wing)
        allVariants.forEach {
            val missingElements = it.findMissingElements(includeModules = false)
            if(missingElements.hadMissing)
                missingElements.logIfHadMissing(Level.WARN)
        }

        hullIDToVariant = allVariants.groupBy { it.hullSpec.hullId }
        effectiveHullIDToVariant = allVariants.groupBy { it.hullSpec.getEffectiveHullId() }
        baseHullIDToVariant = allVariants.groupBy { it.hullSpec.baseHullId }

        init = true
    }

    private fun cleanMissionVariantsForRemovedElements(allVariants: List<ShipVariantAPI>) {
        var count = 0
        for(variant in allVariants) {
            if(variant.source != VariantSource.MISSION_SAVE)
                return

            val missingElements = variant.findMissingElements(includeModules = false)
            if(missingElements.hadMissing)
                count++

            missingElements.missingHullmods.forEach { hullMod ->
                variant.removeModFull(hullMod)
                Global.getLogger(this.javaClass).info("Cleaned missing hull-mod '$hullMod' from variant-id '${variant.hullVariantId}' of hull-id '${variant.hullSpec.hullId}'")
            }
            missingElements.missingWeapons.forEach { (slot, weaponId) ->
                variant.clearSlot(slot)
                Global.getLogger(this.javaClass).info("Cleaned missing weapon '$weaponId' from variant-id '${variant.hullVariantId}' of hull-id '${variant.hullSpec.hullId}'")
            }
            missingElements.missingWings.forEach { wing ->
                variant.wings.remove(wing)
                Global.getLogger(this.javaClass).info("Cleaned missing wing '$wing' from variant-id '${variant.hullVariantId}' of hull-id '${variant.hullSpec.hullId}'")
            }
        }

        if(count > 0)
            Global.getLogger(this.javaClass).info("Cleaned $count mission variants for removed elements")
    }

    /**Does not clone the variant, use with caution.*/
    internal fun getVariantsForEffectiveHullSpecRaw(hullSpec: ShipHullSpecAPI): List<ShipVariantAPI> {
        return effectiveHullIDToVariant[hullSpec.getEffectiveHullId()].orEmpty()
    }

    @JvmStatic
    fun getVariantsForEffectiveHullSpec(hullSpec: ShipHullSpecAPI): List<ShipVariantAPI> {
        return effectiveHullIDToVariant[hullSpec.getEffectiveHullId()].orEmpty().map { it.clone() }
    }

    @JvmStatic
    fun getVariantsForBaseHullSpec(hullSpec: ShipHullSpecAPI): List<ShipVariantAPI> {
        return hullIDToVariant[hullSpec.baseHullId].orEmpty().map { it.clone() }
    }

    @JvmStatic
    fun getVariantsForActualHullSpec(
        hullSpec: ShipHullSpecAPI
    ): List<ShipVariantAPI> {
        return hullIDToVariant[hullSpec.getActualHullId()].orEmpty().map {
            it.clone()
        }
    }

    @JvmStatic
    fun getHullSpec(hullId: String) = IDToHullSpec[hullId]

    @JvmStatic
    fun getHullIDSet(): Set<String> = IDToHullSpec.keys

    @JvmStatic
    fun getFighterWingSpec(wingId: String) = IDToWing[wingId]

    @JvmStatic
    fun getFighterWingIDSet(): Set<String> = IDToWing.keys

    @JvmStatic
    fun getWeaponSpec(weaponId: String) = IDToWeapon[weaponId]

    @JvmStatic
    fun getActuallyAllWeaponSpecIDSet(): Set<String> = IDToWeapon.keys

    @JvmStatic
    fun getHullModSpec(hullModId: String) = IDToHullMod[hullModId]

    @JvmStatic
    fun getHullModIDSet(): Set<String> = IDToHullMod.keys

    @JvmStatic
    fun getSkillSpec(skillId: String) = IDToSkill[skillId]

    @JvmStatic
    fun getAllSkillSpecs(): Collection<SkillSpecAPI> = IDToSkill.values

    @JvmStatic
    fun getAllDMods(): Set<String> = allDMods

    @JvmStatic
    fun isDMod(modID: String): Boolean = allDMods.contains(modID)

    @JvmStatic
    fun getAllHiddenEverywhereMods(): Set<String> = allHiddenEverywhereMods

    /**
     * This function does not clone the variants, use with caution.
     *
     * Editing the variants in this list will edit them for everything that uses them.
     */
    @JvmStatic
    fun getAllVariantsRaw(): List<ShipVariantAPI> = allVariants

    @JvmStatic
    fun getAllFactionIDs(): Set<String> = allFactionIDs

    @JvmStatic
    fun getShipSystemSpec(systemId: String): ShipSystemSpecAPI? = IDToShipSystem[systemId]
}