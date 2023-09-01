package org.magiclib.achievements.builtin

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.campaign.ids.Items
import org.magiclib.achievements.MagicAchievement
import org.magiclib.kotlin.getFactionMarkets

class ColonyItemsAchievement : MagicAchievement() {
    companion object {
        private const val key = "itemsAcquired"
    }

    private val targets: MutableSet<String> = HashSet()

    val itemsAcquiredSoFar: Set<String>
        get() = (memory[key] as? MutableSet<String>?) ?: mutableSetOf()

    override fun onApplicationLoaded() {
        super.onApplicationLoaded()
        targets.addAll(
            listOf(
                Items.CORRUPTED_NANOFORGE,
                Items.PRISTINE_NANOFORGE,
                Items.SYNCHROTRON,
                Items.ORBITAL_FUSION_LAMP,
                Items.MANTLE_BORE,
                Items.CATALYTIC_CORE,
                Items.SOIL_NANITES,
                Items.BIOFACTORY_EMBRYO,
                Items.FULLERENE_SPOOL,
                Items.PLASMA_DYNAMO,
                Items.CRYOARITHMETIC_ENGINE,
                Items.DRONE_REPLICATOR,
                Items.DEALMAKER_HOLOSUITE,
                Items.CORONAL_PORTAL
            )
        )
    }

    override fun onSaveGameLoaded() {
        super.onSaveGameLoaded()

        Global.getSector().listenerManager.addListener(this, true)
    }

    override fun onDestroyed() {
        super.onDestroyed()
        Global.getSector().listenerManager.removeListener(this)
    }

    override fun advanceAfterInterval(amount: Float) {
        super.advanceAfterInterval(amount)

        val existing: MutableSet<String> = itemsAcquiredSoFar.toMutableSet()
        val initialCount = existing.size

        val installedItems = Global.getSector().playerFaction
            ?.getFactionMarkets().orEmpty()
            .flatMap { marketAPI ->
                marketAPI.industries.orEmpty()
                    .flatMap { it.installableItems.orEmpty() }
            }
            .map { it.currentlyInstalledItemData.id }
            .toSet()
        existing += installedItems

        if (existing.size != initialCount) {
            memory[key] = existing
            saveChanges()
        }

        if (existing.containsAll(targets)) {
            completeAchievement()
            saveChanges()
            onDestroyed()
        }
    }

    override fun getProgress(): Float = itemsAcquiredSoFar.size.toFloat()
    override fun getMaxProgress(): Float = targets.size.toFloat()
}
