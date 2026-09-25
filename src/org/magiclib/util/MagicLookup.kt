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
import org.magiclib.MagicUserSettings
import org.magiclib.util.api.getActualHull
import org.magiclib.util.api.getActualHullId
import org.magiclib.util.api.getEffectiveHull
import org.magiclib.util.api.getEffectiveHullId
import org.magiclib.util.api.removeModFull
import org.magiclib.util.internal.AssignHullSkinSourceMod.assignHullSkinSourceMods
import org.magiclib.util.internal.MiscellaneousUtil.findMissingElements
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * Cached, indexed lookups for game data (hulls, variants, weapons, hull mods, skills, etc)
 *
 * Populated by [setup] on first use; must not be accessed before `onApplicationLoad`.
 */
object MagicLookup {

    private var isSetupComplete = false
    fun isSetup(): Boolean = isSetupComplete

    private fun <K, V> Map<K, V>.unmodifiable(): Map<K, V> = Collections.unmodifiableMap(this)
    private fun <T> List<T>.unmodifiable(): List<T> = Collections.unmodifiableList(this)
    private fun <T> Set<T>.unmodifiable(): Set<T> = Collections.unmodifiableSet(this)

    private var dModIds: Set<String> = emptySet()
    private var hiddenEverywhereModIds: Set<String> = emptySet()
    private var factionIds: Set<String> = emptySet()

    private var allVariants: List<ShipVariantAPI> = emptyList()

    private var variantsByHullId: Map<String, List<ShipVariantAPI>> = emptyMap()
    private var variantsByEffectiveHullId: Map<String, List<ShipVariantAPI>> = emptyMap()
    private var variantsByBaseHullId: Map<String, List<ShipVariantAPI>> = emptyMap()

    private var hullSpecsById: Map<String, ShipHullSpecAPI> = emptyMap()
    private var wingsById: Map<String, FighterWingSpecAPI> = emptyMap()
    private var weaponsById: Map<String, WeaponSpecAPI> = emptyMap()
    private var hullModsById: Map<String, HullModSpecAPI> = emptyMap()
    private var skillsById: Map<String, SkillSpecAPI> = emptyMap()
    private var shipSystemsById: Map<String, ShipSystemSpecAPI> = emptyMap()

    private var enabledModIds: Set<String> = emptySet()

    //
    //Mod Content
    //
    /**
     * Groups items by the mod ID returned from [modId], skipping any item where it is null.
     *
     * @param modId Extracts the mod ID from an item, or null if it has no source mod.
     * @return Map of mod ID to the items that came from that mod.
     */
    private inline fun <T> Iterable<T>.groupByModId(modId: (T) -> String?): Map<String, List<T>> =
        mapNotNull { item -> modId(item)?.let { it to item } }
            .groupBy({ it.first }, { it.second })

    private var elementsByModId: ModContentCache = ModContentCache.EMPTY

    private class SourceModContent(modId: String, index: ModContentCache) {
        val hullspecs: Map<String, ShipHullSpecAPI> by lazy {
            index.hullsByMod[modId].orEmpty().associateBy { it.hullId }
        }
        val hullmods: Map<String, HullModSpecAPI> by lazy {
            index.hullModsByMod[modId].orEmpty().associateBy { it.id }
        }
        val weapons: Map<String, WeaponSpecAPI> by lazy {
            index.weaponsByMod[modId].orEmpty().associateBy { it.weaponId }
        }
        val wings: Map<String, FighterWingSpecAPI> by lazy {
            index.wingsByMod[modId].orEmpty().associateBy { it.id }
        }
        val skills: Map<String, SkillSpecAPI> by lazy {
            index.skillsByMod[modId].orEmpty().associateBy { it.id }
        }
        val shipSystems: Map<String, ShipSystemSpecAPI> by lazy {
            index.systemsByMod[modId].orEmpty().associateBy { it.id }
        }
    }

    /**
     * Lazily builds per-mod content. Nothing is grouped until first requested.
     */
    private class ModContentCache(
        private val hulls: Collection<ShipHullSpecAPI>,
        private val hullMods: Collection<HullModSpecAPI>,
        private val weapons: Collection<WeaponSpecAPI>,
        private val wings: Collection<FighterWingSpecAPI>,
        private val skills: Collection<SkillSpecAPI>,
        private val systems: Collection<ShipSystemSpecAPI>
    ) {
        // One pass over each element type, only when that type is first needed.
        val hullsByMod by lazy { hulls.groupByModId { it.sourceMod?.id } }
        val hullModsByMod by lazy { hullMods.groupByModId { it.sourceMod?.id } }
        val weaponsByMod by lazy { weapons.groupByModId { it.sourceMod?.id } }
        val wingsByMod by lazy { wings.groupByModId { it.sourceMod?.id } }
        val skillsByMod by lazy { skills.groupByModId { it.sourceMod?.id } }
        val systemsByMod by lazy { systems.groupByModId { it.sourceMod?.id } }

        private val contents = ConcurrentHashMap<String, SourceModContent>()

        operator fun get(modId: String): SourceModContent? {
            if (modId !in enabledModIds) return null
            return contents.getOrPut(modId) { SourceModContent(modId, this) }
        }

        companion object {
            val EMPTY = ModContentCache(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        }
    }
    //
    //Mod Content
    //

    internal fun setup() {
        val settings = Global.getSettings()

        val allHullSpecs = settings.allShipHullSpecs
        if (allHullSpecs.isEmpty()) {
            Global.getLogger(this.javaClass).error("No hulls found. It is very likely that the '${this.javaClass.name}' object was accessed before onApplicationLoad. Avoid calling ${this.javaClass.name} before onApplicationLoad")
            return
        }

        hullSpecsById = allHullSpecs.associateBy { it.hullId } // Do note this contains all the '_default_D' ships as well.
        wingsById = settings.allFighterWingSpecs.associateBy { it.id }
        hullModsById = settings.allHullModSpecs.associateBy { it.id }
        weaponsById = settings.actuallyAllWeaponSpecs.associateBy { it.weaponId }
        skillsById = settings.skillIds.map { settings.getSkillSpec(it) }.associateBy { it.id }
        shipSystemsById = settings.allShipSystemSpecs.associateBy { it.id }

        factionIds = settings.allFactionSpecs.mapTo(HashSet()) { it.id }
        dModIds = hullModsById.filterValues { it.hasTag(Tags.HULLMOD_DMOD) }.keys
        hiddenEverywhereModIds = hullModsById.filterValues { it.isHiddenEverywhere }.keys

        allVariants = settings.allVariantIds.mapNotNull { variantId ->
            try { settings.getVariant(variantId) }
            catch(e: Exception) {
                Global.getLogger(this.javaClass).error("Failed to get variant '$variantId': ${e.message}", e)
                null
            }
        }

        if(MagicUserSettings.getBoolean(MagicVariables.MAGICLIB_ID, "magiclib_FixMissionVariantError", default = true))
            stripMissingElementsFromMissionVariants(allVariants)

        // Log a warning if any variant had missing elements (hull-mod, weapon, wing)
        allVariants.forEach {
            if(it.source == VariantSource.MISSION_SAVE) // Saved mission variants are out of a modders control, do not report them.
                return@forEach

            val missingElements = it.findMissingElements(includeModules = false)
            if(missingElements.hadMissing)
                missingElements.logIfHadMissing(Level.WARN)
        }

        variantsByHullId = allVariants.groupBy { it.hullSpec.hullId }
        variantsByEffectiveHullId = allVariants.groupBy { it.hullSpec.getEffectiveHullId() }
        variantsByBaseHullId = allVariants.groupBy { it.hullSpec.baseHullId }

        enabledModIds = settings.modManager.enabledModsCopy.mapTo(HashSet()) { it.id }


        if(MagicUserSettings.getBoolean(MagicVariables.MAGICLIB_ID, "magiclib_AssignMissingSourceMods", default = true))
            assignHullSkinSourceMods()


        elementsByModId = ModContentCache(
            hulls = hullSpecsById.values,
            hullMods = hullModsById.values,
            weapons = weaponsById.values,
            wings = wingsById.values,
            skills = skillsById.values,
            systems = shipSystemsById.values
        )

        isSetupComplete = true
    }

    private fun stripMissingElementsFromMissionVariants(allVariants: List<ShipVariantAPI>) {
        var cleanedCount = 0
        for(variant in allVariants) {
            if(variant.source != VariantSource.MISSION_SAVE)
                continue

            val missingElements = variant.findMissingElements(includeModules = false)
            if(!missingElements.hadMissing)
                continue
            cleanedCount++

            missingElements.missingHullmods.forEach { hullMod ->
                variant.removeModFull(hullMod)
                Global.getLogger(this.javaClass).info("Cleaned missing hull-mod '$hullMod' from variant-id '${variant.hullVariantId}' of hull-id '${variant.hullSpec.hullId}'")
            }
            missingElements.missingWeapons.forEach { (slot, weaponId) ->
                variant.clearSlot(slot)
                Global.getLogger(this.javaClass).info("Cleaned missing weapon '$weaponId' from variant-id '${variant.hullVariantId}' of hull-id '${variant.hullSpec.hullId}'")
            }
            missingElements.missingWings.forEach { wing ->
                variant.wings.removeAll { it == wing }
                Global.getLogger(this.javaClass).info("Cleaned missing wing '$wing' from variant-id '${variant.hullVariantId}' of hull-id '${variant.hullSpec.hullId}'")
            }
        }

        if(cleanedCount > 0)
            Global.getLogger(this.javaClass).info("Cleaned $cleanedCount mission variants for removed elements")
    }

    /**
     * This function does not clone the variants, use with caution.
     *
     * Editing the variants in this list will edit them for everything that uses them.
     */
    @JvmStatic
    fun getAllVariantsRaw(): List<ShipVariantAPI> = allVariants

    /**Does not clone the variant, use with caution.*/
    internal fun getVariantsForEffectiveHullSpecRaw(hullSpec: ShipHullSpecAPI): List<ShipVariantAPI> {
        return variantsByEffectiveHullId[hullSpec.getEffectiveHullId()].orEmpty()
    }

    /**
     * Returns a list of effective hull variants for the given hull spec, cloned.
     *
     * See [ShipHullSpecAPI.getEffectiveHull] for more information.
     */
    @JvmStatic
    fun getVariantsForEffectiveHullSpec(hullSpec: ShipHullSpecAPI): List<ShipVariantAPI> =
        variantsByEffectiveHullId[hullSpec.getEffectiveHullId()].orEmpty().map { it.clone() }

    /**
     * Returns a list of base hull variants for the given hull spec, cloned.
     *
     * See [ShipHullSpecAPI.getBaseHull] for more information.
     */
    @JvmStatic
    fun getVariantsForBaseHullSpec(hullSpec: ShipHullSpecAPI): List<ShipVariantAPI> =
        variantsByBaseHullId[hullSpec.baseHullId].orEmpty().map { it.clone() }

    /**
     * Returns a list of actual hull variants for the given hull spec, cloned.
     *
     * See [ShipHullSpecAPI.getActualHull] for more information.
     */
    @JvmStatic
    fun getVariantsForActualHullSpec(hullSpec: ShipHullSpecAPI): List<ShipVariantAPI> =
        variantsByHullId[hullSpec.getActualHullId()].orEmpty().map { it.clone() }


    @JvmStatic
    fun getHullSpec(hullId: String) = hullSpecsById[hullId]
    @JvmStatic
    fun getHullSpec(hullId: String, sourceModId: String) = elementsByModId[sourceModId]?.hullspecs?.get(hullId)

    @JvmStatic
    fun getHullSpecMap(): Map<String, ShipHullSpecAPI> = hullSpecsById
    @JvmStatic
    fun getHullSpecMap(sourceModId: String): Map<String, ShipHullSpecAPI>? = elementsByModId[sourceModId]?.hullspecs


    @JvmStatic
    fun getFighterWingSpec(wingId: String) = wingsById[wingId]
    @JvmStatic
    fun getFighterWingSpec(wingId: String, sourceModId: String) = elementsByModId[sourceModId]?.wings?.get(wingId)

    @JvmStatic
    fun getFighterWingMap(): Map<String, FighterWingSpecAPI> = wingsById
    @JvmStatic
    fun getFighterWingMap(sourceModId: String): Map<String, FighterWingSpecAPI>? = elementsByModId[sourceModId]?.wings


    @JvmStatic
    fun getWeaponSpec(weaponId: String) = weaponsById[weaponId]
    @JvmStatic
    fun getWeaponSpec(weaponId: String, sourceModId: String) = elementsByModId[sourceModId]?.weapons?.get(weaponId)

    @JvmStatic
    fun getWeaponSpecMap(): Map<String, WeaponSpecAPI> = weaponsById
    @JvmStatic
    fun getWeaponSpecMap(sourceModId: String): Map<String, WeaponSpecAPI>? = elementsByModId[sourceModId]?.weapons


    @JvmStatic
    fun getHullModSpec(hullModId: String) = hullModsById[hullModId]
    @JvmStatic
    fun getHullModSpec(hullModId: String, sourceModId: String) = elementsByModId[sourceModId]?.hullmods?.get(hullModId)

    @JvmStatic
    fun getHullModMap(): Map<String, HullModSpecAPI> = hullModsById
    @JvmStatic
    fun getHullModMap(sourceModId: String): Map<String, HullModSpecAPI>? = elementsByModId[sourceModId]?.hullmods


    @JvmStatic
    fun getSkillSpec(skillId: String) = skillsById[skillId]
    @JvmStatic
    fun getSkillSpec(skillId: String, sourceModId: String) = elementsByModId[sourceModId]?.skills?.get(skillId)

    @JvmStatic
    fun getSkillSpecMap(): Map<String, SkillSpecAPI> = skillsById
    @JvmStatic
    fun getSkillSpecMap(sourceModId: String): Map<String, SkillSpecAPI>? = elementsByModId[sourceModId]?.skills


    @JvmStatic
    fun getShipSystemSpec(systemId: String) = shipSystemsById[systemId]
    @JvmStatic
    fun getShipSystemSpec(systemId: String, sourceModId: String) = elementsByModId[sourceModId]?.shipSystems?.get(systemId)

    @JvmStatic
    fun getShipSystemSpecMap(): Map<String, ShipSystemSpecAPI> = shipSystemsById
    @JvmStatic
    fun getShipSystemSpecMap(sourceModId: String): Map<String, ShipSystemSpecAPI>? = elementsByModId[sourceModId]?.shipSystems


    @JvmStatic
    fun getAllDModIds(): Set<String> = dModIds

    @JvmStatic
    fun isDMod(hullModId: String): Boolean = dModIds.contains(hullModId)


    @JvmStatic
    fun getAllHiddenEverywhereModIds(): Set<String> = hiddenEverywhereModIds


    @JvmStatic
    fun getAllFactionIds(): Set<String> = factionIds


    @JvmStatic
    fun getEnabledModIds(): Set<String> = enabledModIds



    init {
        setup()
    }
}