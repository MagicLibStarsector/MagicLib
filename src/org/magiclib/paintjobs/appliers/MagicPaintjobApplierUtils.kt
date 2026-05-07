package org.magiclib.paintjobs.appliers

import com.fs.graphics.Sprite
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignUIAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.ui.ButtonAPI
import com.fs.starfarer.api.ui.UIComponentAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.loading.specs.HullVariantSpec
import com.fs.state.AppDriver
import org.magiclib.ReflectionUtils
import org.magiclib.ReflectionUtils.get
import org.magiclib.ReflectionUtils.getFieldsMatching
import org.magiclib.ReflectionUtils.getMethodsMatching
import org.magiclib.ReflectionUtils.invoke
import org.magiclib.internalextensions.*
import org.magiclib.paintjobs.MagicPaintjobManager
import java.awt.Color

internal object MagicPaintjobApplierUtils {
    //val shinyIconSprite = "graphics/ui/icons/32x_star_circle.png"
    val shinyIconSprite = "graphics/magic/icons/shiny_icon.png"
    val shinyIconColor = Color(255, 255, 255)
    val shinyIconOpacity = 1f

    fun getScreenPanel(): UIPanelAPI? {
        val state = AppDriver.getInstance().currentState
        return state.invoke("getScreenPanel") as? UIPanelAPI
    }

    fun CampaignUIAPI.isIdle(): Boolean {
        return currentInteractionDialog == null &&
                !isShowingDialog &&
                !isShowingMenu
    }
    private fun UIPanelAPI.addImage(imageSpritePath: String, width: Float, height: Float): UIComponentAPI {
        val tempPanel = Global.getSettings().createCustom(width, height, null)
        val tempTMAPI = tempPanel.createUIElement(width, height, false)
        tempTMAPI.addImage(imageSpritePath, width, height, 0f)
        val tempTMAPIsUIPanel = tempTMAPI.getChildrenCopy()[0] as UIPanelAPI
        val image = tempTMAPIsUIPanel.getChildrenCopy()[0]

        this.addComponent(image)
        return image
    }


    fun applyPaintjobsToShipList(
        shipList: UIComponentAPI,
        showShinyIcon: Boolean = false,
        forceUpdateShinyIcon: Boolean = false,
        diffPositioning: Boolean = false,
    ) {
        @Suppress("UNCHECKED_CAST")
        val members = shipList.invoke("getMembers") as? List<FleetMemberAPI> ?: return

        val paintJobMembers = members.filter { member -> MagicPaintjobManager.hasPaintjob(member) }

        if(forceUpdateShinyIcon && members.isNotEmpty()) {
            // Remove all shiny icons
            val memberButton = shipList.invoke("getButtonForMember", members[0]) as? ButtonAPI ?: return
            memberButton.parent?.getChildrenCopy()?.toList()?.forEach { child ->
                if(child.getMethodsMatching(name = "isTexClamp").isNotEmpty()) {
                    memberButton.parent?.removeComponent(child)
                }
            }
        }

        paintJobMembers.forEach { member ->
            val memberIcon = shipList.invoke("getIconForMember", member) ?: return@forEach

            if (showShinyIcon && MagicPaintjobManager.getCurrentShipPaintjob(member)?.isShiny == true) {
                val memberButton = shipList.invoke("getButtonForMember", member) as? ButtonAPI ?: return
                if (memberButton.customData == null || forceUpdateShinyIcon) { // Prevent multiple shiny icons from being applied to the same ship.
                    val extraSize = 8f
                    val width = memberButton.width + extraSize
                    val height = memberButton.height + extraSize
                    val background = memberButton.parent?.addImage(shinyIconSprite, width, height)
                    background?.let {
                        if(!diffPositioning) {
                            background.position?.rightOfMid(memberButton, -width + extraSize / 2f)
                        } else {
                            val pos = memberButton.position
                            val x: Float
                            val y: Float
                            val parent = memberButton.parent ?: return@let

                            //x = pos.x - (pos.width - extraSize) / 2f
                            //y = parent.height - pos.y - height/2f

                            x = pos.x - parent.x - extraSize/2f
                            y = pos.y - parent.y - extraSize/2f

                            it.position.inBL(x, y)
                        }
                        it.opacity = shinyIconOpacity
                        (it.invoke("getSprite") as Sprite).color = shinyIconColor
                    }
                    memberButton.parent?.invoke("sendToBottomWithinItself", background)
                    memberButton.customData = "SIP" // Shiny Icon Applied
                }
            }

            changeIconSprite(member, memberIcon)
        }
    }

    fun changeIconSprite(member: FleetMemberAPI, memberIcon: Any) {

        // Variant

        val variantPaintJobSpec = MagicPaintjobManager.getCurrentShipPaintjob(member.variant)

        if (variantPaintJobSpec != null) {
            val spriteFields = memberIcon.getFieldsMatching(type = Sprite::class.java)
            val variantSpriteField = spriteFields.firstOrNull { field ->
                val sprite = field.get(memberIcon) as? Sprite ?: return@firstOrNull false
                sprite.getFieldsMatching(name = "textureId").getOrNull(0)?.get(sprite) == member.hullSpec.spriteName
            }
            //val spriteDirect = memberIcon.getMethodsMatching(returnType = Sprite::class.java).firstOrNull()?.invoke(memberIcon) as? Sprite

            if (variantSpriteField != null)
                makeNewSpriteToReplace(variantPaintJobSpec.spriteId, variantSpriteField, memberIcon)
        }

        // Variant Modules

        val modules = member.variant.stationModules
            ?.mapNotNull { (slot, _) ->
                val variant: ShipVariantAPI? = member.variant.getModuleVariant(slot)
                variant?.let { slot to it }
            }
            ?.toMap() // converts the list of pairs back into a Map
            ?: emptyMap()

        val modulePaintJobSpecs = modules.mapNotNull { (_, module) ->
            MagicPaintjobManager.getCurrentShipPaintjob(module)
        }
        if (modulePaintJobSpecs.isNotEmpty()) {

            // Array should be a list of elements which each contain a Sprite, HullVariantSpec, and something obfuscated which seems to contain details on where the module is on its host variant.
            fun isModuleList(list: List<*>): Boolean {
                val first = list.firstOrNull() ?: return false
                return first.getFieldsMatching(type = HullVariantSpec::class.java).isNotEmpty()
            }

            val moduleList = memberIcon
                .getFieldsMatching(type = List::class.java)
                .asSequence()
                .mapNotNull { field -> field.get(memberIcon) as? List<*> }
                .firstOrNull(::isModuleList)

            moduleList?.forEach {
                val variant = it?.get(type = HullVariantSpec::class.java) as? ShipVariantAPI
                if (variant != null) {
                    val paintjob = MagicPaintjobManager.getCurrentShipPaintjob(variant)
                    if (paintjob != null) {
                        val spriteField = it.getFieldsMatching(type = Sprite::class.java).getOrNull(0)
                        if (spriteField != null)
                            makeNewSpriteToReplace(paintjob.spriteId, spriteField, it)
                    }
                }
            }
        }

        // Decoration Weapons
        // In this context, weapon rendering is done in CampaignFleetMemberView.renderWeapons. There, it creates a new sprite and assigns a new texture from the WeaponSpec every frame. This is done from a FleetMember input.
        // No editable sprite is stored anywhere. This makes it very very hard to apply a paintjob.
        /*
        member.hullSpec.builtInWeapons.forEach { weapon ->
            val paintjobFamily = MagicPaintjobManager.getCurrentShipPaintjob(member)?.paintjobFamily ?: return@forEach

            val weaponSpec = member.variant.getWeaponSpec(weapon.key)
            val weaponPaintjobs = MagicPaintjobManager.getPaintjobsForWeapon(weaponSpec.weaponId, paintjobFamily)
            val weaponPaintjob = weaponPaintjobs.getOrNull(0) ?: return@forEach
        }
        */
    }

    private fun makeNewSpriteToReplace(
        newSpriteId: String,
        onField: ReflectionUtils.ReflectedField,
        onInstance: Any
    ) {
        val spriteToReplace = onField.get(onInstance) as? Sprite ?: return

        val newSpriteAPI = Global.getSettings().getSprite(newSpriteId)?.takeIf { it.textureId != 0 } ?: run {
            Global.getSettings().loadTexture(newSpriteId)
            Global.getSettings().getSprite(newSpriteId)
        }
        if (newSpriteAPI == null || newSpriteAPI.textureId == 0) return
        val newSprite = newSpriteAPI.getFieldsMatching(type = Sprite::class.java).getOrNull(0)?.get(newSpriteAPI) as? Sprite ?: return

        newSprite.width = spriteToReplace.width
        newSprite.height = spriteToReplace.height
        newSprite.texWidth = spriteToReplace.texWidth
        newSprite.texHeight = spriteToReplace.texHeight
        newSprite.alphaMult = spriteToReplace.alphaMult

        onField.set(onInstance, newSprite)
    }
}