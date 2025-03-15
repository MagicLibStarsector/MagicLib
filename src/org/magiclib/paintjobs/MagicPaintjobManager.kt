package org.magiclib.paintjobs

import com.fs.graphics.Sprite
import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.ModSpecAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.graphics.SpriteAPI
import com.fs.starfarer.combat.entities.ship.trackers.MultiBarrelRecoilTracker
import org.dark.shaders.util.ShaderLib
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.lazywizard.lazylib.ext.json.optFloat
import org.lazywizard.lazylib.ext.logging.w
import org.magiclib.LunaWrapper
import org.magiclib.LunaWrapperSettingsListener
import org.magiclib.Magic_modPlugin
import org.magiclib.kotlin.forEach
import org.magiclib.kotlin.optColor
import org.magiclib.kotlin.toStringList
import org.magiclib.util.MagicMisc
import org.magiclib.util.MagicTxt
import org.magiclib.util.MagicVariables

object MagicPaintjobManager {
    private val logger = Global.getLogger(MagicPaintjobManager::class.java)
    const val specsFilename = "magic_paintjobs.csv"
    const val weaponSpecsFilename = "magic_weapon_paintjobs.csv"
    const val folderName = "paintjobs/"
    const val configPath = "data/config/"
    private const val commonFilename = "magic_paintjobs.json"
    private const val isIntelImportantMemKey = "\$magiclib_isPaintjobIntelImportant"
    private val jsonObjectKey = "unlockedPaintjobs"

    private val unlockedPaintjobsInner = mutableSetOf<String>()
    private val paintjobsInner = mutableListOf<MagicPaintjobSpec>()
    private val completedPaintjobIdsThatUserHasBeenNotifiedFor = mutableListOf<String>()

    private val weaponPaintjobsInner = mutableListOf<MagicWeaponPaintjobSpec>()

    const val PJTAG_PERMA_PJ = "MagicLib_PermanentPJ"
    const val PJTAG_SHINY = "MagicLib_ShinyPJ"



    @JvmStatic
    var isEnabled = true

    @JvmStatic
    val unlockedPaintjobIds: List<String>
        get() = unlockedPaintjobsInner.toList()

    @JvmStatic
    val unlockedPaintjobs: List<MagicPaintjobSpec>
        get() = unlockedPaintjobsInner.mapNotNull { id -> paintjobsInner.firstOrNull { it.id == id } }

    /**
     * Returns the paintjobs that are available to the player.
     * Shiny paintjobs should not be shown to the player and cannot be manually applied or unlocked.
     */
    @JvmStatic
    @JvmOverloads
    fun getPaintjobs(includeShiny: Boolean = false): Set<MagicPaintjobSpec> {
        return paintjobsInner.filter { it.isShiny == includeShiny }.toSet()
    }

    init {
        initIntel()
    }

    // make java users feel better
    @JvmStatic
    fun getInstance() = this

    @JvmStatic
    fun onApplicationLoad() {
        // Set up LunaLib settings.
        if (Global.getSettings().modManager.isModEnabled("lunalib")) {
            isEnabled = LunaWrapper.getBoolean(MagicVariables.MAGICLIB_ID, "magiclib_enablePaintjobs") ?: true

            // Add settings listener.
            LunaWrapper.addSettingsListener(object : LunaWrapperSettingsListener {
                override fun settingsChanged(modID: String) {
                    val lunaIsEnabled =
                        LunaWrapper.getBoolean(MagicVariables.MAGICLIB_ID, "magiclib_enablePaintjobs") ?: true

                    if (isEnabled != lunaIsEnabled) {
                        isEnabled = lunaIsEnabled
                        if (isEnabled) {
                            onGameLoad()
                            initIntel()
                        } else {
                            removeIntel()
                        }
                    }
                }
            })
        }

        val (paintjobSpecs, weaponPaintjobSpecs) = loadPaintjobs()

        paintjobsInner.clear()
        paintjobsInner.addAll(paintjobSpecs.values)
        weaponPaintjobsInner.clear()
        weaponPaintjobsInner.addAll(weaponPaintjobSpecs.values)
    }

    @JvmStatic
    fun onGameLoad() {
        loadUnlockedPaintjobs()
        initIntel()

        Global.getSector().addTransientListener(MagicPaintjobShinyAdder())
        if (!Global.getSector().hasTransientScript(MagicPaintjobRunner::class.java)) {
            Global.getSector().addTransientScript(MagicPaintjobRunner())
        }
    }


    @JvmStatic
    fun loadPaintjobs(): Pair<Map<String, MagicPaintjobSpec>, Map<String, MagicWeaponPaintjobSpec>> {
        val newSpecs: MutableList<MagicPaintjobSpec> = mutableListOf()
        val newWeaponSpecs: MutableList<MagicWeaponPaintjobSpec> = mutableListOf()
        for (mod in Global.getSettings().modManager.enabledModsCopy) {
            val fileLocations = listOf("$configPath$specsFilename", "$configPath$folderName$specsFilename")

            val modCsv = fileLocations.firstNotNullOfOrNull { path ->
                // ignore file not there errors
                runCatching { Global.getSettings().loadCSV(path, mod.id) }.onFailure { e ->
                    if (!(e is RuntimeException && e.message?.contains("not found in") == true)) {
                        logger.warn("Unable to load paintjobs in ${mod.id} by " +
                                "${MagicMisc.takeFirst(mod.author, 50)} from file $path", e)
                    }
                }.getOrNull()
            } ?: continue

            logger.info(modCsv)

            for (i in 0 until modCsv.length()) {
                var id: String? = null
                try {
                    val item = modCsv.getJSONObject(i)
                    id = item.getString("id").trim()
                    val hullId = item.optString("hullId")?.trim()
                    val hullIds =
                        (item.optString("hullIds")?.split(",").orEmpty().map { it.trim() } + hullId).filterNotNull()
                    val name = item.getString("name").trim()
                    val description = item.getString("description").trim()
                    val unlockConditions = item.getString("unlockConditions").trim()
                    val unlockedAutomatically = item.optBoolean("unlockedAutomatically", true)
                    val spriteId = item.getString("spriteId").trim()
                    val tags = item.optString("tags", "")?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
                    val paintjobFamily = item.optString("paintjobFamily").trim().takeIf { it.isNotEmpty() }

                    val jsonLocations = listOf("$configPath$id.paintjob", "$configPath$folderName$id.paintjob")
                    val paintjobJson = jsonLocations.firstNotNullOfOrNull { path ->
                        runCatching { Global.getSettings().loadJSON(path, mod.id) }.onFailure { e ->
                            // ignore file not there errors
                            if (!(e is RuntimeException && e.message?.contains("not found in") == true)){
                                logger.warn("Unable to load paintjob JSON of $id paintjob", e)
                            }
                        }.getOrNull()
                    }

                    val decos = paintjobJson?.runCatching {
                        getJSONObject("decos").let { decosJson ->
                            // map weaponSlotID to spritePath
                            decosJson.keys().asSequence().associate { weaponSlotID ->
                                weaponSlotID as String to decosJson.getString(weaponSlotID)
                            }
                        }
                    }?.onFailure { e ->
                        if (!(e is JSONException && e.message?.contains("not found") == true)){
                            logger.warn("Unable to load decos JSON of $id paintjob", e)
                        }
                    }?.getOrNull()

                    val engineSpec = paintjobJson?.runCatching {
                        getJSONObject("engines")?.let { engineJson ->
                            MagicPaintjobSpec.PaintjobEngineSpec(
                                engineJson.optColor("color", null),
                                engineJson.optColor("contrailColor", null),
                                engineJson.optFloat("spawnDistMult").takeIf { !it.isNaN() },
                                engineJson.optFloat("contrailWidthMultiplier").takeIf { !it.isNaN() },
                                engineJson.optColor("glowAlternateColor", null),
                                engineJson.optFloat("glowSizeMult").takeIf { !it.isNaN() }
                            )
                        }
                    }?.onFailure { e ->
                        if (!(e is JSONException && e.message?.contains("not found") == true)) {
                            logger.warn("Unable to load engines JSON of $id paintjob", e)
                        }
                    }?.getOrNull()

                    val shieldSpec = paintjobJson?.runCatching {
                        getJSONObject("shield")?.let{ shieldJson ->
                            MagicPaintjobSpec.PaintjobShieldSpec(
                                shieldJson.optColor("innerColor", null),
                                shieldJson.optColor("ringColor", null),
                                shieldJson.optFloat("innerRotationRate").takeIf { !it.isNaN() },
                                shieldJson.optFloat("ringRotationRate").takeIf { !it.isNaN() },
                            )
                        }
                    }?.onFailure { e ->
                        if(!(e is JSONException && e.message?.contains("not found") == true)) {
                            logger.warn("Unable to load shield JSON of $id paintjob", e)
                        }
                    }?.getOrNull()

                    var skip = false
                    for (paintjobSpec in newSpecs) {
                        if (paintjobSpec.id == id) {
                            skip = true
                            logger.warn(
                                String.format(
                                    "Paintjob with id %s in mod %s already exists in mod %s, skipping.",
                                    id, mod.id, paintjobSpec.modId
                                )
                            )
                            break
                        }
                    }

                    if (id.isBlank()) {
                        // Just a blank row, no need to warn.
//                        logger.warn("Paintjob #$i in ${mod.id} by '${mod.author}' has no id, skipping.")
                        skip = true
                    } else if (hullIds.isEmpty()) {
                        logger.warn("Paintjob $id in ${mod.id} by '${mod.author}' has no hullIds, skipping.")
                        skip = true
                    } else if (name.isBlank()) {
                        logger.warn("Paintjob $id in ${mod.id} by '${mod.author}' has no name, skipping.")
                        skip = true
                    } else if (spriteId.isBlank()) {
                        logger.warn("Paintjob $id in ${mod.id} by '${mod.author}' has no spriteId, skipping.")
                        skip = true
                    } else if (hullIds.none {
                            kotlin.runCatching { Global.getSettings().getHullSpec(it) }
                                .getOrNull() != null
                        }) {
                        logger.warn("Paintjob $id in ${mod.id} by '${mod.author}' has no valid hullIds, skipping.")
                        skip = true
                    }

                    if (!skip) {
                        newSpecs.add(
                            MagicPaintjobSpec(
                                modId = mod.id,
                                modName = mod.name,
                                id = id,
                                hullId = hullIds.first(),
                                hullIds = hullIds,
                                name = name,
                                unlockedAutomatically = unlockedAutomatically,
                                description = description,
                                unlockConditions = unlockConditions,
                                spriteId = spriteId,
                                tags = tags,
                                decos = decos,
                                engineSpec = engineSpec,
                                shieldSpec = shieldSpec,
                                paintjobFamily = paintjobFamily
                            )
                                .also {
                                    if (unlockedAutomatically && it.isUnlockable)
                                        unlockedPaintjobsInner.add(it.id)
                                }
                        )
                    }
                } catch (e: java.lang.Exception) {
                    logger.warn(
                        "Unable to load paintjob #$i ($id) in ${mod.id} by ${
                            MagicTxt.ellipsizeStringAfterLength(
                                mod.author,
                                30
                            )
                        } from file $specsFilename",
                        e
                    )
                }
            }

            newWeaponSpecs.addAll(loadWeaponPaintjobs(mod))
        }

        val newPaintjobSpecsById: MutableMap<String, MagicPaintjobSpec> = HashMap()
        for (newSpec in newSpecs) {
            newPaintjobSpecsById[newSpec.id] = newSpec
        }
        logger.info("Loaded " + newPaintjobSpecsById.size + " paintjobs.")

        val newWeaponPaintjobSpecsById: MutableMap<String, MagicWeaponPaintjobSpec> = HashMap()
        for (newSpec in newWeaponSpecs) {
            newWeaponPaintjobSpecsById[newSpec.id] = newSpec
        }
        logger.info("Loaded " + newWeaponPaintjobSpecsById.size + " weapon paintjobs.")

        return Pair(newPaintjobSpecsById, newWeaponPaintjobSpecsById)
    }

    private fun loadWeaponPaintjobs(mod: ModSpecAPI): List<MagicWeaponPaintjobSpec>{
        val newSpecs: MutableList<MagicWeaponPaintjobSpec> = mutableListOf()
        val fileLocations = listOf("$configPath$weaponSpecsFilename", "$configPath$folderName$weaponSpecsFilename")
        val weaponCsv = fileLocations.firstNotNullOfOrNull { path ->
            // ignore file not there errors
            runCatching { Global.getSettings().loadCSV(path, mod.id) }.onFailure { e ->
                if (!(e is RuntimeException && e.message?.contains("not found in") == true)) {
                    logger.warn("Unable to load weapon paintjobs in ${mod.id} by " +
                            "${MagicMisc.takeFirst(mod.author, 50)} from file $path", e)
                }
            }.getOrNull()
        } ?: return newSpecs

        logger.info(weaponCsv)

        weaponCsv.forEach<JSONObject>  { weaponPJEntry ->
            var id: String? = null
            runCatching {
                id = weaponPJEntry.getString("id")
                if(id?.isBlank() ?: true) return@forEach
                if(newSpecs.any {it.id == id}){
                    logger.warn("Weapon Paintjob with id: $id already exists, skipping.")
                    return@forEach
                }
                val paintjobFamilies = weaponPJEntry.getString("paintjobFamilies").split(",").map { it.trim() }.toSet()
                if (paintjobFamilies.isEmpty()) {
                    logger.warn("Weapon Paintjob with id: $id has no paintjobFamilies, skipping.")
                    return@forEach
                }
                val weaponIds = weaponPJEntry.getString("weaponIds").split(",").map { it.trim() }.toSet()
                if (weaponIds.isEmpty()) {
                    logger.warn("Weapon Paintjob with id: $id has no weaponId's, skipping.")
                    return@forEach
                }

                /* TODO: Uncomment this for 0.98
                if (!Global.getSettings().actuallyAllWeaponSpecs.any{ it.weaponId in weaponIds }) {
                    logger.warn("Weapon Paintjob with id: $id has no valid weaponId's, skipping.")
                    return@forEach
                }
                */
                val spriteMap = weaponPJEntry.getString("spriteMap").split(",").mapNotNull { mapEntry ->
                    val parts = mapEntry.split("->")
                    if(parts.size == 2){
                        parts[0].trim() to parts[1].trim()
                    } else{
                        logger.warn("Weapon Paintjob with id: $id has invalid spriteMap for $parts, skipping this map.")
                        null
                    }
                }.toMap()
                newSpecs.add(MagicWeaponPaintjobSpec(mod.id, id!!, paintjobFamilies, weaponIds, spriteMap))
            }.onFailure { e ->
                logger.warn(
                    "Unable to load weapon paintjob ($id) in ${mod.id} by ${
                        MagicTxt.ellipsizeStringAfterLength(mod.author, 30)
                    } from file $weaponSpecsFilename",
                    e
                )
            }
        }
        return newSpecs
    }

    @JvmStatic
    @JvmOverloads
    fun getPaintjobsForWeapon(weaponId: String, paintjobFamily: String? = null): List<MagicWeaponPaintjobSpec> =
        weaponPaintjobsInner.filter { spec ->
            weaponId in spec.weaponIds && (paintjobFamily?.let { it in spec.paintjobFamilies } ?: true)
        }

    @JvmStatic
    @JvmOverloads
    fun getPaintjobsForHull(hullId: String, includeShiny: Boolean = false): List<MagicPaintjobSpec> =
        paintjobsInner
            .filter { hullId in it.hullIds }
            .let { if (includeShiny) it else it.filter { !it.isShiny } }

    @JvmStatic
    fun saveUnlockedPaintJobs() {
        // Ensure that we have the latest unlocked pjs before saving
        loadUnlockedPaintjobs()
        if (unlockedPaintjobsInner.isEmpty()) return

        runCatching {
            val unlockedPJsObj = JSONObject()
            unlockedPJsObj.put(jsonObjectKey, unlockedPaintjobsInner.toList())
            val indentation = 3
            Global.getSettings().writeTextFileToCommon(commonFilename, unlockedPJsObj.toString(indentation))
        }
            .onFailure { logger.warn("Failed to save unlocked paintjobs.", it) }
    }

    @JvmStatic
    fun loadUnlockedPaintjobs() {
        if (!isEnabled) return

        runCatching {
            val unlockedPJsObj = runCatching {
                val result = JSONObject(Global.getSettings().readTextFileFromCommon(commonFilename))
                if (result.length() > 0) result
                else return // If there's no valid paintjob file, bail out.
            }.recover { JSONObject().apply { put(jsonObjectKey, JSONArray()) } }
                .getOrThrow()

            unlockedPaintjobsInner.addAll(unlockedPJsObj.getJSONArray(jsonObjectKey).toStringList())

            markAsAlreadyNotifiedPlayerOfNewUnlock(unlockedPaintjobsInner)
        }
            .onFailure { logger.warn("Failed to load unlocked paintjobs.", it) }
    }

    private fun markAsAlreadyNotifiedPlayerOfNewUnlock(paintjobs: Set<String>) {
        completedPaintjobIdsThatUserHasBeenNotifiedFor.clear()
        for (paintjobId in paintjobs) {
            completedPaintjobIdsThatUserHasBeenNotifiedFor.add(paintjobId)
        }
    }

    /**
     * Adds a paintjob to the list of paintjobs.
     * Replaces any existing paintjob with the same id.
     */
    @JvmStatic
    fun addPaintJob(paintjob: MagicPaintjobSpec) {
        if (paintjob.hullIds.none { runCatching { Global.getSettings().getHullSpec(it) }.getOrNull() != null }) {
            logger.warn("Did not add paintjob ${paintjob.id}. Hull with ids ${paintjob.hullIds} does not exist.")
            return
        }

        if (Global.getSettings().getSprite(paintjob.spriteId) == null) {
            // Don't preload the sprite, if feature is disabled it will never be used.
            // Global.getSettings().loadTexture(paintjob.spriteId)
            logger.warn("Did not add paintjob ${paintjob.id}. Sprite with id ${paintjob.spriteId} does not exist.")
            return
        }

        paintjobsInner.removeAll { it.id == paintjob.id }
        paintjobsInner.add(paintjob)

        if (Magic_modPlugin.isMagicLibTestMode()) {
            unlockPaintjob(paintjob.id)
        }
    }

    @JvmStatic
    fun unlockPaintjob(id: String) {
        if (getPaintjob(id)?.isUnlockable != true) return

        unlockedPaintjobsInner.add(id)
        saveUnlockedPaintJobs()
    }

    /**
     * Don't use this without good reason, such as debugging.
     */
    @JvmStatic
    fun lockPaintjob(id: String) {
        if (!unlockedPaintjobsInner.contains(id))
            return

        unlockedPaintjobsInner.remove(id)
        saveUnlockedPaintJobs()
    }

    @JvmStatic
    fun beforeGameSave() {
        // Save whether the intel is important.
        if (getIntel() != null)
            Global.getSector().memoryWithoutUpdate[isIntelImportantMemKey] = getIntel()!!.isImportant

        // No reason to add intel to the save.
        removeIntel()
    }

    @JvmStatic
    fun afterGameSave() = initIntel()

    @JvmStatic
    fun initIntel() {
        if (!isEnabled) return
        if (Global.getSector() == null) return
        removeIntel()

        // Don't show if there aren't any.
        if (getPaintjobs().isEmpty()) return

        val intel = MagicPaintjobIntel()
        Global.getSector().intelManager.addIntel(intel, true)
        intel.setImportant(Global.getSector().memoryWithoutUpdate.getBoolean(isIntelImportantMemKey))
        intel.isNew = false
    }

    @JvmStatic
    fun getIntel(): MagicPaintjobIntel? {
        if (!Global.getSector().intelManager.hasIntelOfClass(MagicPaintjobIntel::class.java))
            return null

        return try {
            Global.getSector().intelManager.getFirstIntel(MagicPaintjobIntel::class.java) as MagicPaintjobIntel
        } catch (ex: Exception) {
            logger.warn("Unable to get MagicPaintjobIntel.", ex)
            null
        }
    }

    @JvmStatic
    fun getPaintjob(paintjobId: String): MagicPaintjobSpec? = paintjobsInner.firstOrNull { it.id == paintjobId }

    @JvmStatic
    fun removePaintjobFromShip(fleetMember: FleetMemberAPI) {
        val variant = fleetMember.variant ?: return
        removePaintjobFromShip(variant)
        fleetMember.spriteOverride = null
    }

    @JvmStatic
    fun removePaintjobFromShip(variant: ShipVariantAPI){
        variant.tags.filter { it.startsWith(MagicPaintjobHullMod.PAINTJOB_TAG_PREFIX) }
            .forEach { variant.removeTag(it) }

        if (variant.hasHullMod(MagicPaintjobHullMod.ID)) {
            variant.removePermaMod(MagicPaintjobHullMod.ID)
        }
    }

    @JvmStatic
    fun applyPaintjob(fleetMember: FleetMemberAPI?, paintjob: MagicPaintjobSpec) {
        applyPaintjob(fleetMember?.variant, paintjob)
        // This causes the sprite to show at full size on game (re)load, ie massive hyperspace ships.
//      fleetMember.spriteOverride = paintjob.spriteId
    }

    @JvmStatic
    fun applyPaintjob(variant: ShipVariantAPI?, paintjob: MagicPaintjobSpec) {
        if(variant != null){
            if (!variant.hasHullMod(MagicPaintjobHullMod.ID)) {
                variant.addPermaMod(MagicPaintjobHullMod.ID)
            }

            variant.tags.filter {
                it.startsWith(MagicPaintjobHullMod.PAINTJOB_TAG_PREFIX)
            }.forEach { variant.removeTag(it) }
            variant.addTag(MagicPaintjobHullMod.PAINTJOB_TAG_PREFIX + paintjob.id)
        }
    }

    @JvmStatic
    fun applyPaintjob(combatShip: ShipAPI?, paintjob: MagicPaintjobSpec) {
        if (!isEnabled) return
        // In case it's a sprite path that wasn't loaded, load it.
        val spriteId = paintjob.spriteId
        Global.getSettings().loadTexture(spriteId)

        if (combatShip == null) return
        val replacementShipSprite = getReplacementSprite(combatShip.spriteAPI, spriteId) ?: return
        combatShip.setSprite(replacementShipSprite)

        if (Global.getSettings().modManager.isModEnabled("shaderLib")) {
            ShaderLib.overrideShipTexture(combatShip, spriteId)
        }

        combatShip.shield?.let { shield ->
            paintjob.shieldSpec?.let { spec ->
                spec.innerColor?.let { shield.innerColor = it }
                spec.ringColor?.let { shield.ringColor = it }
                spec.innerRotationRate?.let { shield.innerRotationRate = it }
                spec.ringRotationRate?.let { shield.ringRotationRate = it }
            }
        }

        combatShip.engineController?.shipEngines?.forEach { shipEngine ->
            val slot = shipEngine.engineSlot
            paintjob.engineSpec?.let { spec ->
                spec.color?.let { slot.color = it }
                spec.contrailColor?.let { slot.contrailColor = it }
                spec.contrailSpawnDistMult?.let { slot.contrailSpawnDistMult = it }
                spec.contrailWidthMultiplier?.let { slot.contrailWidthMultiplier = it }
                spec.glowAlternateColor?.let { slot.glowAlternateColor = it }
                spec.glowSizeMult?.let { slot.glowSizeMult = it }
            }
        }

        for(weapon in combatShip.allWeapons){
            getPaintjobsForWeapon(weapon.spec.weaponId, paintjob.paintjobFamily).forEach { weaponPaintjob ->
                applyWeaponPaintjob(weapon, weaponPaintjob)
            }
        }
    }

    @JvmStatic
    fun applyWeaponPaintjob(weapon: WeaponAPI?, paintjob: MagicWeaponPaintjobSpec){
        if (!isEnabled || weapon == null || paintjob.spriteMap == null) return

        // In case the sprites weren't loaded, load them, then get the SpriteAPIs
        val paintjobSprites = paintjob.spriteMap!!.entries.associate { (defaultSpritePath, replacementSpritePath) ->
            Global.getSettings().loadTexture(replacementSpritePath)
            Global.getSettings().getSprite(defaultSpritePath) to Global.getSettings().getSprite(replacementSpritePath)
        }

        // get all weapon sprites
        val weaponSprites = ReflectionUtils.getFieldsOfType(weapon, Sprite::class.java).mapNotNull { field ->
            ReflectionUtils.get(field, weapon) as Sprite?
        } + ReflectionUtils.getFieldsOfType(weapon, Array<Sprite>::class.java).mapNotNull { field ->
            ReflectionUtils.get(field, weapon) as Array<Sprite>?
        }.flatMap {
            it.toList()
        } + ReflectionUtils.getFieldsOfType(weapon, MultiBarrelRecoilTracker::class.java).mapNotNull { field ->
            ReflectionUtils.get(field, weapon) as MultiBarrelRecoilTracker?
        }.map { multiBarrelRecoilTracker ->
            ReflectionUtils.getFieldsOfType(multiBarrelRecoilTracker, Sprite::class.java).mapNotNull { field ->
                ReflectionUtils.get(field, multiBarrelRecoilTracker) as Sprite?
            }
        }.flatten()

        val tempWeaponSprite = Global.getSettings().getSprite("misc", "empty")

        for (weaponSprite in weaponSprites){
            // it's hard to get a textureId out of a true sprite, so box it into SpriteAPI
            ReflectionUtils.invoke("setSprite", tempWeaponSprite, weaponSprite)

            // make sure only one textureId matches, I don't think it's possible? for multiple, but CYA
            val replacementMap = paintjobSprites.filter { it.key.textureId == tempWeaponSprite.textureId }
            if(replacementMap.count() > 1){
                logger.warn("Weapon Paintjob with id: ${paintjob.id} has multiple source sprites of the same " +
                        "texture in the spriteMap (${replacementMap.keys.first().textureId}), skipping this map.")
                continue
            }
            else if (replacementMap.count() == 1) {
                val (_, replacementSpriteAPI) = replacementMap.entries.first()
                // unbox the replacement sprite and get its texture, then replace the weapon sprite's texture
                val replacementSprite = ReflectionUtils.invoke("getSprite", replacementSpriteAPI) as Sprite
                val replacementTexture = ReflectionUtils.invoke("getTexture", replacementSprite)
                val setTextureMethod = ReflectionUtils.getMethodsOfName("setTexture", weaponSprite)[0]
                ReflectionUtils.rawInvoke(setTextureMethod, weaponSprite, replacementTexture)

            }
        }
    }

    private fun getReplacementSprite(originalSprite: SpriteAPI, replacementSpriteID: String): SpriteAPI?{
        Global.getSettings().loadTexture(replacementSpriteID)
        val replacementSprite = Global.getSettings().getSprite(replacementSpriteID) ?: return null

        replacementSprite.setCenter(originalSprite.centerX, originalSprite.centerY)
        replacementSprite.alphaMult = originalSprite.alphaMult
        replacementSprite.angle = originalSprite.angle
        replacementSprite.color = originalSprite.color

        return replacementSprite
    }

    @JvmStatic
    fun getCurrentShipPaintjob(variant: ShipVariantAPI): MagicPaintjobSpec? {
        val pjTag = variant.tags.firstOrNull { it.startsWith(MagicPaintjobHullMod.PAINTJOB_TAG_PREFIX) }
        val paintjobId = pjTag?.removePrefix(MagicPaintjobHullMod.PAINTJOB_TAG_PREFIX) ?: return null

        return getPaintjob(paintjobId)
    }

    @JvmStatic
    fun getCurrentShipPaintjob(fleetMember: FleetMemberAPI): MagicPaintjobSpec? {
        val variant = fleetMember.variant ?: return null
        val pjTag = variant.tags.firstOrNull { it.startsWith(MagicPaintjobHullMod.PAINTJOB_TAG_PREFIX) }
        val paintjobId = pjTag?.removePrefix(MagicPaintjobHullMod.PAINTJOB_TAG_PREFIX) ?: return null

        // The player can remove the hullmod manually, so if it's not there, remove the paintjob.
        if (!fleetMember.variant.hasHullMod(MagicPaintjobHullMod.ID)) {
            removePaintjobFromShip(fleetMember)
            return null
        }

        return getPaintjob(paintjobId)
    }

    @JvmStatic
    fun advance(amount: Float) {
        if (!isEnabled) return
        val intel = getIntel() ?: return

        // For all paintjobs that were just unlocked, show intel update.
        // Only notify intel if in campaign and not showing a dialog.
        // If in combat, the intel will be shown when the player returns to the campaign.
        if (Global.getCurrentState() == GameState.CAMPAIGN && Global.getSector().campaignUI.currentInteractionDialog == null) {
            for (paintjob in getPaintjobs()) {
                if (paintjob.isUnlocked() && !completedPaintjobIdsThatUserHasBeenNotifiedFor.contains(paintjob.id)) {
                    // Player has unlocked a new paintjob! Let's notify them.

                    try {
                        intel.tempPaintjobForIntelNotification = paintjob
                        intel.sendUpdateIfPlayerHasIntel(null, false, false)
                        intel.tempPaintjobForIntelNotification = null
//                        MagicAchievementManager.playSoundEffect(paintjob)
                        completedPaintjobIdsThatUserHasBeenNotifiedFor.add(paintjob.id)
                    } catch (e: java.lang.Exception) {
                        logger.w(ex = e, message = { "Unable to notify intel of paintjob " + paintjob.id })
                    }
                }
            }
        }
    }

    private fun removeIntel() {
        if (Global.getSector() == null) return
        val intelManager = Global.getSector().intelManager
        while (intelManager.hasIntelOfClass(MagicPaintjobIntel::class.java)) {
            intelManager.removeIntel(intelManager.getFirstIntel(MagicPaintjobIntel::class.java))
        }
    }
}


internal class MagicPaintjobRunner : EveryFrameScript {
    override fun isDone(): Boolean = false

    override fun runWhilePaused(): Boolean = true

    override fun advance(amount: Float) {
        MagicPaintjobManager.advance(amount)
    }
}

fun MagicPaintjobSpec.isUnlocked() = MagicPaintjobManager.unlockedPaintjobIds.contains(id)