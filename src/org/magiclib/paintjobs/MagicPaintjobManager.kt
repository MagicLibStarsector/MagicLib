package org.magiclib.paintjobs

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import lunalib.lunaSettings.LunaSettings.addSettingsListener
import lunalib.lunaSettings.LunaSettings.getBoolean
import lunalib.lunaSettings.LunaSettingsListener
import org.dark.shaders.util.ShaderLib
import org.json.JSONArray
import org.json.JSONObject
import org.magiclib.Magic_modPlugin
import org.magiclib.kotlin.toStringList
import org.magiclib.util.MagicMisc
import org.magiclib.util.MagicVariables

object MagicPaintjobManager {
    private val logger = Global.getLogger(MagicPaintjobManager::class.java)
    private const val commonFilename = "magic_paintjobs.json"
    const val specsFilename = "magic_paintjobs.csv"
    private val jsonObjectKey = "unlockedPaintjobs"
    private val unlockedPaintjobsInner = mutableSetOf<String>()
    private val paintjobsInner = mutableListOf<MagicPaintjobSpec>()
    private const val isIntelImportantMemKey = "\$magiclib_isPaintjobIntelImportant"

    // TODO remove before release
    internal fun diable() {
        listOf(
            "graphics/pj_test/da/rosenritter/ships/diableavionics_vapor.png",
            "graphics/pj_test/da/rosenritter/ships/diableavionics_calm_deck.png",
            "graphics/pj_test/da/rosenritter/ships/diableavionics_chinook.png",
            "graphics/pj_test/da/rosenritter/ships/diableavionics_cirrus.png",
            "graphics/pj_test/da/rosenritter/ships/diableavionics_daze.png",
            "graphics/pj_test/da/rosenritter/ships/diableavionics_derecho.png",
            "graphics/pj_test/da/rosenritter/ships/diableavionics_draft.png",
            "graphics/pj_test/da/rosenritter/ships/diableavionics_draftarmor.png",
            "graphics/pj_test/da/rosenritter/ships/diableavionics_fractus.png",
            "graphics/pj_test/da/rosenritter/ships/diableavionics_gust.png",
            "graphics/pj_test/da/rosenritter/ships/diableavionics_hayle.png",
            "graphics/pj_test/da/rosenritter/ships/diableavionics_haze.png",
            "graphics/pj_test/da/rosenritter/ships/diableavionics_laminar.png",
            "graphics/pj_test/da/rosenritter/ships/diableavionics_maelstrom.png",
            "graphics/pj_test/da/rosenritter/ships/diableavionics_minigust.png",
            "graphics/pj_test/da/rosenritter/ships/diableavionics_pandemonium.png",
            "graphics/pj_test/da/rosenritter/ships/diableavionics_rime.png",
            "graphics/pj_test/da/rosenritter/ships/diableavionics_rime_military.png",
            "graphics/pj_test/da/rosenritter/ships/diableavionics_rime_p.png",
            "graphics/pj_test/da/rosenritter/ships/diableavionics_shear.png",
            "graphics/pj_test/da/rosenritter/ships/diableavionics_sleet.png",
            "graphics/pj_test/da/rosenritter/ships/diableavionics_storm.png",
            "graphics/pj_test/da/rosenritter/ships/diableavionics_stratus.png",
            "graphics/pj_test/da/rosenritter/ships/diableavionics_stratus_p.png",
            "graphics/pj_test/da/lilium/ships/diableavionics_calm_deck.png",
            "graphics/pj_test/da/lilium/ships/diableavionics_chinook.png",
            "graphics/pj_test/da/lilium/ships/diableavionics_cirrus.png",
            "graphics/pj_test/da/lilium/ships/diableavionics_daze.png",
            "graphics/pj_test/da/lilium/ships/diableavionics_derecho.png",
            "graphics/pj_test/da/lilium/ships/diableavionics_draft.png",
            "graphics/pj_test/da/lilium/ships/diableavionics_draftarmor.png",
            "graphics/pj_test/da/lilium/ships/diableavionics_fractus.png",
            "graphics/pj_test/da/lilium/ships/diableavionics_gust.png",
            "graphics/pj_test/da/lilium/ships/diableavionics_hayle.png",
            "graphics/pj_test/da/lilium/ships/diableavionics_haze.png",
            "graphics/pj_test/da/lilium/ships/diableavionics_laminar.png",
            "graphics/pj_test/da/lilium/ships/diableavionics_maelstrom.png",
            "graphics/pj_test/da/lilium/ships/diableavionics_minigust.png",
            "graphics/pj_test/da/lilium/ships/diableavionics_pandemonium.png",
            "graphics/pj_test/da/lilium/ships/diableavionics_rime.png",
            "graphics/pj_test/da/lilium/ships/diableavionics_rime_military.png",
            "graphics/pj_test/da/lilium/ships/diableavionics_rime_p.png",
            "graphics/pj_test/da/lilium/ships/diableavionics_shear.png",
            "graphics/pj_test/da/lilium/ships/diableavionics_sleet.png",
            "graphics/pj_test/da/lilium/ships/diableavionics_storm.png",
            "graphics/pj_test/da/lilium/ships/diableavionics_stratus.png",
            "graphics/pj_test/da/lilium/ships/diableavionics_stratus_p.png",
            "graphics/pj_test/da/lilium/ships/diableavionics_vapor.png",
        )
            .forEach { spriteId ->
                val name = spriteId.removeSuffix(".png").takeLastWhile { it != '/' }
                addPaintJob(
                    MagicPaintjobSpec(
                        modId = "magiclib",
                        modName = "MagicLib",
                        id = "ml_$spriteId",
                        hullId = name,
                        name = name.takeLastWhile { it != '_' }.replaceFirstChar { it.uppercase() },
                        description = null,
                        spriteId = spriteId
                    )
                )
            }
    }

    @JvmStatic
    var isEnabled = true

    @JvmStatic
    val unlockedPaintjobIds: List<String>
        get() = unlockedPaintjobsInner.toList()

    @JvmStatic
    val unlockedPaintjobs: List<MagicPaintjobSpec>
        get() = unlockedPaintjobsInner.mapNotNull { id -> paintjobsInner.firstOrNull { it.id == id } }

    @JvmStatic
    val paintjobs: Set<MagicPaintjobSpec>
        get() = paintjobsInner.toSet()

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
            // Add settings listener.
            addSettingsListener(object : LunaSettingsListener {
                override fun settingsChanged(settings: String) {
                    getBoolean(MagicVariables.MAGICLIB_ID, "magiclib_enablePaintjobs")?.also { lunaIsEnabled ->
                        isEnabled = lunaIsEnabled
                    }
                }
            })
        }

        paintjobsInner.addAll(loadPaintjobs().values)
    }

    @JvmStatic
    fun loadPaintjobs(): Map<String, MagicPaintjobSpec> {
        val newSpecs: MutableList<MagicPaintjobSpec> = mutableListOf()

        for (mod in Global.getSettings().modManager.enabledModsCopy) {
            var modCsv: JSONArray? = null
            try {
                modCsv = Global.getSettings().loadCSV(specsFilename, mod.id)
            } catch (e: java.lang.Exception) {
                if (e is RuntimeException && e.message!!.contains("not found in")) {
                    // Swallow exceptions caused by the mod not having pjs.
                } else {
                    logger.warn(
                        "Unable to load paintjobs in " + mod.id + " by " + MagicMisc.takeFirst(
                            mod.author,
                            50
                        ) + " from file " + specsFilename, e
                    )
                }
            }
            if (modCsv == null) continue

            logger.info(modCsv)

            for (i in 0 until modCsv.length()) {
                var id: String? = null
                try {
                    val item = modCsv.getJSONObject(i)
                    id = item.getString("id").trim()
                    val hullId = item.getString("hullId").trim()
                    val name = item.getString("name").trim()
                    val description = item.getString("description").trim()
                    val unlockedAutomatically = item.optBoolean("unlockedAutomatically", true)
                    var spriteId = item.getString("spriteId").trim()

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
                    if (!skip) {
                        newSpecs.add(
                            MagicPaintjobSpec(
                                modId = mod.id,
                                modName = mod.name,
                                id = id,
                                hullId = hullId,
                                name = name,
                                unlockedAutomatically = unlockedAutomatically,
                                description = description,
                                spriteId = spriteId
                            )
                        )
                    }
                } catch (e: java.lang.Exception) {
                    logger.warn(
                        "Unable to load paintjob #" + i + " (" + id + ") in " + mod.id + " by " + mod.author.substring(
                            0,
                            30
                        ) + " from file " + specsFilename, e
                    )
                }
            }
        }

        val newPaintjobSpecsById: MutableMap<String, MagicPaintjobSpec> = HashMap()

        for (newSpec in newSpecs) {
            newPaintjobSpecsById[newSpec.id] = newSpec
        }

        return newPaintjobSpecsById
    }

    @JvmStatic
    fun getPaintjobsForHull(hullId: String): List<MagicPaintjobSpec> {
        return paintjobsInner.filter { it.hullId.equals(hullId, ignoreCase = true) }
    }

    @JvmStatic
    fun saveUnlockedPaintJobs() {
        // Ensure that we have the latest unlocked pjs before saving
        loadUnlockedPaintjobs()
        if (unlockedPaintjobsInner.isEmpty()) return
        val magicNumber = 3

        runCatching {
            val unlockedPJsObj = JSONObject()
            unlockedPJsObj.put(jsonObjectKey, unlockedPaintjobsInner.toList())
            Global.getSettings().writeTextFileToCommon(commonFilename, unlockedPJsObj.toString(magicNumber))
        }
            .onFailure { logger.error("Failed to save unlocked paintjobs.", it) }
    }

    @JvmStatic
    fun loadUnlockedPaintjobs() {
        runCatching {
            val unlockedPJsObj = runCatching {
                val result = JSONObject(Global.getSettings().readTextFileFromCommon(commonFilename))
                if (result.length() > 0) result
                else JSONObject()
            }.recover { JSONObject() }
                .getOrThrow()

            unlockedPaintjobsInner.addAll(unlockedPJsObj.getJSONArray(jsonObjectKey).toStringList())
        }
            .onFailure { logger.error("Failed to load unlocked paintjobs.", it) }
    }

//    @JvmStatic
//    fun loadPaintjobs() {
//        runCatching {
//            val unlockedPJsObj = runCatching {
//                val result = JSONObject(Global.getSettings().readTextFileFromCommon(commonFilename))
//                if (result.length() > 0) result
//                else JSONObject()
//            }.recover { JSONObject() }
//                .getOrThrow()
//
//            paintjobsInner.addAll(unlockedPJsObj.getJSONArray(jsonObjectKey)
//                .map<JSONObject, MagicPaintjobSpec> { MagicPaintjobSpec.fromJsonObject(it) })
//        }
//            .onFailure { logger.error("Failed to load unlocked paintjobs.", it) }
//    }

    /**
     * Adds a paintjob to the list of paintjobs.
     * Replaces any existing paintjob with the same id.
     */
    @JvmStatic
    fun addPaintJob(paintjob: MagicPaintjobSpec) {
        if (runCatching { Global.getSettings().getHullSpec(paintjob.hullId) }.getOrNull() == null) {
            logger.error("Did not add paintjob ${paintjob.id}. Hull with id ${paintjob.hullId} does not exist.")
            return
        }

        if (Global.getSettings().getSprite(paintjob.spriteId) == null) {
            Global.getSettings().loadTexture(paintjob.spriteId)
            logger.error("Did not add paintjob ${paintjob.id}. Sprite with id ${paintjob.spriteId} does not exist.")
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
        unlockedPaintjobsInner.add(id)
        saveUnlockedPaintJobs()
    }

    @JvmStatic
    fun onGameLoad() {
        loadUnlockedPaintjobs()
        initIntel()
    }

    @JvmStatic
    fun beforeGameSave() {
        if (getIntel() != null) Global.getSector().memoryWithoutUpdate[isIntelImportantMemKey] =
            getIntel()!!.isImportant

        // No reason to add intel to the save.
        removeIntel()
    }

    @JvmStatic
    fun afterGameSave() = initIntel()

    @JvmStatic
    fun initIntel() {
        if (Global.getSector() == null) return
        removeIntel()

        // Don't show if there aren't any.
        if (paintjobs.isEmpty()) return

        val intel = MagicPaintjobIntel()
        Global.getSector().intelManager.addIntel(intel, true)
        intel.setImportant(Global.getSector().memoryWithoutUpdate.getBoolean(isIntelImportantMemKey))
        intel.isNew = false
    }

    @JvmStatic
    fun getIntel(): MagicPaintjobIntel? {
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

        variant.tags.filter { it.startsWith(MagicSkinSwapHullMod.PAINTJOB_TAG_PREFIX) }
            .forEach { variant.removeTag(it) }

        if (variant.hasHullMod(MagicSkinSwapHullMod.ID)) {
            variant.removeMod(MagicSkinSwapHullMod.ID)
        }

        fleetMember.spriteOverride = null
    }

    @JvmStatic
    fun applyPaintjob(fleetMember: FleetMemberAPI?, combatShip: ShipAPI?, paintjob: MagicPaintjobSpec) {
        // In case it's a sprite path that wasn't loaded, load it.
        val spriteId = paintjob.spriteId
        Global.getSettings().loadTexture(spriteId)

        if (fleetMember != null) {
            val variant = fleetMember.variant
            if (variant?.hasHullMod(MagicSkinSwapHullMod.ID) != true) {
                variant.addMod(MagicSkinSwapHullMod.ID)
            }

            if (variant != null) {
                variant.tags.filter { it.startsWith(MagicSkinSwapHullMod.PAINTJOB_TAG_PREFIX) }
                    .forEach { variant.removeTag(it) }
                variant.addTag(MagicSkinSwapHullMod.PAINTJOB_TAG_PREFIX + paintjob.id)
            }

            fleetMember.spriteOverride = paintjob.spriteId
        }

        if (combatShip != null) {
            val sprite = Global.getSettings().getSprite(spriteId) ?: return
            val x = combatShip.spriteAPI.centerX
            val y = combatShip.spriteAPI.centerY
            val alpha = combatShip.spriteAPI.alphaMult
            val angle = combatShip.spriteAPI.angle
            val color = combatShip.spriteAPI.color
            combatShip.setSprite(sprite)
            if (Global.getSettings().modManager.isModEnabled("shaderLib")) {
                ShaderLib.overrideShipTexture(combatShip, spriteId)
            }
            combatShip.spriteAPI.setCenter(x, y)
            combatShip.spriteAPI.alphaMult = alpha
            combatShip.spriteAPI.angle = angle
            combatShip.spriteAPI.color = color
        }
    }

    @JvmStatic
    fun getCurrentShipPaintjob(fleetMember: FleetMemberAPI): MagicPaintjobSpec? {
        val variant = fleetMember.variant ?: return null
        val pjTag = variant.tags.firstOrNull { it.startsWith(MagicSkinSwapHullMod.PAINTJOB_TAG_PREFIX) }
        val paintjobId = pjTag?.removePrefix(MagicSkinSwapHullMod.PAINTJOB_TAG_PREFIX) ?: return null
        return getPaintjob(paintjobId)
    }

    private fun removeIntel() {
        if (Global.getSector() == null) return
        val intelManager = Global.getSector().intelManager
        while (intelManager.hasIntelOfClass(MagicPaintjobIntel::class.java)) {
            intelManager.removeIntel(intelManager.getFirstIntel(MagicPaintjobIntel::class.java))
        }
    }
}