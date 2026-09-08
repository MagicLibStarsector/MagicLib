package org.magiclib.util.ui

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.combat.CombatState
import com.fs.state.AppDriver
import org.lazywizard.lazylib.ext.minus
import org.lazywizard.lazylib.ext.rotate
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL13.*
import org.lwjgl.opengl.GL15.*
import org.lwjgl.util.vector.Vector2f
import org.magiclib.ReflectionUtils.getFieldsMatching
import org.magiclib.ReflectionUtils.invoke
import org.magiclib.kotlin.internal.ExtendableCustomUIPanelPlugin
import org.magiclib.kotlin.internal.ExtendableCustomUIPanelPlugin.Companion.CustomPanel
import org.magiclib.kotlin.internal.ExtendableCustomUIPanelPlugin.Companion.anchorInBottomLeftOfParent
import org.magiclib.kotlin.internal.centerX
import org.magiclib.kotlin.internal.centerY
import org.magiclib.kotlin.internal.getChildrenCopy
import org.magiclib.util.internal.MiscellaneousUtil.armorAtCell
import org.magiclib.util.internal.MiscellaneousUtil.interpolateColorNicely
import org.magiclib.util.internal.MiscellaneousUtil.weakestArmorRegion
import java.awt.Color
import kotlin.math.max
import kotlin.math.min

/**
 * Adds a small paperdoll-style panel to the combat ship info tooltip, showing each
 * module's position and armor/hull health as a colored miniature sprite.
 *
 * Only activates for the player ship if it has modules and its hull spec carries the
 * paperdoll opt-in tag [TAG_ID]. Skips re-adding the panel if one is already present.
 */
class PaperdollUIPanelAdder: BaseEveryFrameCombatPlugin() {
    companion object {
        const val TAG_ID = "ML_showModulePaperdoll"
    }

    private val noHPColor = Color(200, 30, 30, 255)
    private val fullHPColor = Color(120, 230, 0, 255)

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        val engine = Global.getCombatEngine() ?: return
        if(engine.playerShip == null || !engine.playerShip.isShipWithModules || !engine.playerShip.hullSpec.hasTag(TAG_ID))
            return
        val state = AppDriver.getInstance().currentState
        if (state !is CombatState)
            return
        val shipInfo = state.invoke("getShipInfo") as UIPanelAPI

        val uiElements = shipInfo.getChildrenCopy()
        if (uiElements.any { it is CustomPanelAPI && it.plugin is ExtendableCustomUIPanelPlugin }) return // return if added
        val shipField = shipInfo.getFieldsMatching(fieldAssignableTo = ShipAPI::class.java)[0]

        shipInfo.CustomPanel(200f, 200f) { plugin ->
            anchorInBottomLeftOfParent()
            val center = Vector2f(centerX, centerY)

            plugin.render { alpha ->
                initRendering()

                val ship = shipField.get(shipInfo) as? ShipAPI ?: return@render
                val targetWidth = ( ship.hullSize.ordinal / 5f ) * 170f
                val moduleScaleFactor = min(targetWidth / max(ship.spriteAPI.width, ship.spriteAPI.height), 2f)

                val shipSprite = ship.spriteAPI
                val shipOffset = Vector2f(shipSprite.centerX - shipSprite.width/2, shipSprite.centerY - shipSprite.height/2).rotate(ship.facing - 90f)
                val shipSpriteLocation = ship.location - shipOffset

                for(module in ship.childModulesCopy){
                    if (module.hitpoints <= 0f) continue

                    val moduleSprite = module.spriteAPI
                    val moduleOffset = Vector2f(moduleSprite.centerX - moduleSprite.width/2, moduleSprite.centerY - moduleSprite.height/2).rotate(module.facing - 90f)
                    val moduleSpriteLocation = module.location - moduleOffset

                    val offset = (shipSpriteLocation - moduleSpriteLocation).scale(moduleScaleFactor) as Vector2f
                    val paperDollLocation = center - offset

                    val armorHealthLevel = with(module.armorGrid) {
                        (armorAtCell(weakestArmorRegion()!!)!! + module.hitpoints) / (armorRating + module.maxHitpoints)
                    }

                    val sprite = Global.getSettings().getSprite(module.hullSpec.spriteName).apply {
                        angle = module.facing - 90f
                        color = interpolateColorNicely(noHPColor, fullHPColor, armorHealthLevel)
                        alphaMult = 0.75f * alpha
                        setSize(width * moduleScaleFactor, height * moduleScaleFactor)
                    }

                    sprite.renderAtCenter(paperDollLocation.x, paperDollLocation.y)
                }

                glPopAttrib()
            }
        }
    }

    private fun initRendering(){
        // Save GL state (includes texture, blend, matrix modes, texenv, etc.)
        glPushAttrib(GL_ALL_ATTRIB_BITS)
        // Use GL_COMBINE to allow arbitrary uniform colors
        glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_COMBINE)

        // RGB Combine -> Use Primary Color (from glColor, set internally by spriteAPI.render based on spriteAPI.color)
        glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_RGB, GL_REPLACE)
        glTexEnvi(GL_TEXTURE_ENV, GL_SRC0_RGB, GL_PRIMARY_COLOR)
        glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND0_RGB, GL_SRC_COLOR)

        // Alpha Combine -> Modulate Texture Alpha * Primary Color Alpha (from glColor)
        glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_ALPHA, GL_MODULATE)
        glTexEnvi(GL_TEXTURE_ENV, GL_SRC0_ALPHA, GL_TEXTURE)
        glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND0_ALPHA, GL_SRC_ALPHA)
        glTexEnvi(GL_TEXTURE_ENV, GL_SRC1_ALPHA, GL_PRIMARY_COLOR)
        glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND1_ALPHA, GL_SRC_ALPHA)
    }
}