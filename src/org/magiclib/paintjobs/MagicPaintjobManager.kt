package org.magiclib.paintjobs

import com.fs.starfarer.api.Global
import org.json.JSONObject
import org.magiclib.kotlin.toStringList

object MagicPaintjobManager {
    private val logger = Global.getLogger(MagicPaintjobManager::class.java)
    private const val commonFilename = "magic_paintjobs.json"
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

        if (Global.getSettings().isDevMode) {
            unlockPaintjob(paintjob.id)
        }
    }

    @JvmStatic
    fun unlockPaintjob(id: String) {
        unlockedPaintjobsInner.add(id)
        saveUnlockedPaintJobs()
    }

    @JvmStatic
    fun onGameLoad() = initIntel()

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

        // Don't show achievements if there aren't any.
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

    private fun removeIntel() {
        if (Global.getSector() == null) return
        val intelManager = Global.getSector().intelManager
        while (intelManager.hasIntelOfClass(MagicPaintjobIntel::class.java)) {
            intelManager.removeIntel(intelManager.getFirstIntel(MagicPaintjobIntel::class.java))
        }
    }
}