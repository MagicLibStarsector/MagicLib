package org.magiclib.paintjobs

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import org.dark.shaders.util.ShaderLib

class MagicSkinSwapHullMod : BaseHullMod() {
    override fun applyEffectsAfterShipCreation(ship: ShipAPI?, id: String?) {
        super.applyEffectsAfterShipCreation(ship, id)
        ship ?: return
        id ?: return

        val skins = MagicPaintjobManager.getPaintjobsForHull(ship.hullSpec.hullId)

        if (skins.isEmpty()) return

        val spriteId = skins.random().spriteId
        // In case it's a sprite path that wasn't loaded, load it.
        Global.getSettings().loadTexture(spriteId)

        val sprite = Global.getSettings().getSprite(spriteId) ?: return

        val x = ship.spriteAPI.centerX
        val y = ship.spriteAPI.centerY
        val alpha = ship.spriteAPI.alphaMult
        val angle = ship.spriteAPI.angle
        val color = ship.spriteAPI.color
        ship.setSprite(sprite)
        if (Global.getSettings().modManager.isModEnabled("shaderLib")) {
            ShaderLib.overrideShipTexture(ship, spriteId)
        }
        ship.spriteAPI.setCenter(x, y)
        ship.spriteAPI.alphaMult = alpha
        ship.spriteAPI.angle = angle
        ship.spriteAPI.color = color
    }

    override fun addPostDescriptionSection(
        tooltip: TooltipMakerAPI?,
        hullSize: ShipAPI.HullSize?,
        ship: ShipAPI?,
        width: Float,
        isForModSpec: Boolean
    ) {
        super.addPostDescriptionSection(tooltip, hullSize, ship, width, isForModSpec)
        ship ?: return

        val skins = MagicPaintjobManager.getPaintjobsForHull(ship.hullSpec.hullId)
        skins.forEach { skin ->
            tooltip?.addPara(skin.name, 10f)
        }
    }
}