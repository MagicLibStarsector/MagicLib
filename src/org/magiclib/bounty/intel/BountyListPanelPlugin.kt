package org.magiclib.bounty.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.api.util.Misc
import org.lwjgl.opengl.GL11
import org.magiclib.bounty.intel.filters.LocationFilter
import org.magiclib.bounty.ui.BaseUIPanelPlugin
import org.magiclib.bounty.ui.lists.ListItemUIPanelPlugin
import org.magiclib.bounty.ui.lists.filtered.FilteredListPanelPlugin
import org.magiclib.bounty.ui.lists.filtered.ListFilter
import org.magiclib.kotlin.setAlpha
import org.magiclib.util.MagicTxt
import java.awt.Color

class BountyListPanelPlugin(parentPanel: CustomPanelAPI) : FilteredListPanelPlugin<BountyInfo>(parentPanel) {
    override val rowWidth
        get() = panelWidth - 4f
    override val rowHeight = 68f

    override val listHeader: String
        get() = MagicTxt.getString("mb_listHeader")

    private var selectedItem: BountyInfo? = null
    private var finalItem: BountyInfo? = null

    override fun getApplicableFilters(): List<ListFilter<BountyInfo, *>> {
        return listOf(LocationFilter())
    }

    override fun getFiltersFromItem(item: BountyInfo): List<String> {
        return listOf(item.getBountyType())
    }

    override fun sortMembers(items: List<BountyInfo>): List<BountyInfo> {
        return super.sortMembers(items)
            .sortedBy { it.getSortIndex() }
    }

    override fun shouldMakePanelForItem(item: BountyInfo): Boolean {
        // Check `shouldShow` first because it doesn't ONLY check that,
        // it also creates the ActiveBounty if it doesn't exist and should be shown.
        return item.shouldShow() || item.shouldAlwaysShow()
    }

    override fun createPanelForItem(tooltip: TooltipMakerAPI, item: BountyInfo): ListItemUIPanelPlugin<BountyInfo> {
        return BountyItemPanelPlugin(item, rowWidth, rowHeight)
    }

    override fun getListHeight(rows: Int): Float {
        return super.getListHeight(rows) + (4f * rows)
    }

    override fun layoutPanels(members: List<BountyInfo>): CustomPanelAPI {
        finalItem = members.lastOrNull()

        val outerPanelLocal = super.layoutPanels(members)

        this.addListener {
            selectedItem = it
        }

        return outerPanelLocal
    }

    override fun placeItem(
        listTooltip: TooltipMakerAPI,
        rowPlugin: ListItemUIPanelPlugin<BountyInfo>,
        lastRowPanel: UIPanelAPI?
    ): UIPanelAPI {
        var lastItem = super.placeItem(listTooltip, rowPlugin, lastRowPanel)
        if (finalItem != rowPlugin.item) {
            val panel = Global.getSettings().createCustom(rowWidth, 2f, BountySpacerPanelPlugin())
            listTooltip.addCustom(panel, 3f)
            lastItem = panel
        }
        return lastItem
    }

    override fun renderBelow(alphaMult: Float) {
        val x = pos.x
        val y = pos.y
        val width = pos.width
        val height = pos.height
        val c = Color.black

        GL11.glPushMatrix()
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_BLEND)

        GL11.glColor4f(
            c.red / 255f,
            c.green / 255f,
            c.blue / 255f,
            c.alpha / 255f * (alphaMult * 1f)
        )

        GL11.glRectf(x, y, x + width, y + height)

        GL11.glPopMatrix()
    }

    override fun render(alphaMult: Float) {
        val x = pos.x
        val y = pos.y
        val width = pos.width
        val height = pos.height

        val c = Misc.getDarkPlayerColor()
        GL11.glPushMatrix()

        GL11.glTranslatef(0f, 0f, 0f)
        GL11.glRotatef(0f, 0f, 0f, 1f)

        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_BLEND)

        GL11.glColor4f(
            c.red / 255f,
            c.green / 255f,
            c.blue / 255f,
            c.alpha / 255f * (alphaMult * 1f)
        )

        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glBegin(GL11.GL_LINE_STRIP)

        GL11.glVertex2f(x, y)
        GL11.glVertex2f(x, y + height)
        GL11.glVertex2f(x + width, y + height)
        GL11.glVertex2f(x + width, y)
        GL11.glVertex2f(x, y)

        GL11.glEnd()
        GL11.glPopMatrix()
    }

    inner class BountyItemPanelPlugin(item: BountyInfo, private val width: Float, private val height: Float) :
        ListItemUIPanelPlugin<BountyInfo>(item) {
        private var wasHovered: Boolean = false
        private var wasSelected: Boolean = false
        var baseBgColor: Color = Color(255, 255, 255, 0)
        var hoveredColor: Color = Misc.getBasePlayerColor().setAlpha(75)
        var selectedColor: Color = Misc.getBasePlayerColor().setAlpha(125)
        override var bgColor: Color = baseBgColor

        override fun layoutPanel(tooltip: TooltipMakerAPI): CustomPanelAPI {
            val panel = Global.getSettings().createCustom(width, height, this)
            val innerTooltip = panel.createUIElement(width, height, false)

            item.decorateListItem(this, innerTooltip, width, height)

            panel.addUIElement(innerTooltip)
            tooltip.addCustom(panel, 0f)
            return panel
        }

        override fun advance(amount: Float) {
            super.advance(amount)

            if (selectedItem === item) {
                if (!wasSelected) {
                    wasSelected = true
                }
            } else {
                if (wasSelected) {
                    wasSelected = false
                }
            }

            bgColor = when {
                wasSelected -> selectedColor
                wasHovered -> hoveredColor
                else -> baseBgColor
            }
        }

        override fun processInput(events: List<InputEventAPI>) {
            if (wasSelected) return // selected already

            if (isHovered(events)) {
                if (!wasHovered) {
                    wasHovered = true
                }
            } else if (wasHovered) {
                wasHovered = false
            }
        }
    }

    inner class BountySpacerPanelPlugin : BaseUIPanelPlugin() {
        override fun renderBelow(alphaMult: Float) {
            val x = pos.x
            val y = pos.y
            val width = pos.width
            val height = pos.height
            val c = Misc.getDarkPlayerColor().darker().darker()

            GL11.glPushMatrix()
            GL11.glDisable(GL11.GL_TEXTURE_2D)

            GL11.glDisable(GL11.GL_BLEND)

            GL11.glColor4f(
                c.red / 255f,
                c.green / 255f,
                c.blue / 255f,
                c.alpha / 255f * (alphaMult * 1f)
            )

            GL11.glRectf(x, y, x + width, y + height)

            //GL11.glEnd()
            GL11.glPopMatrix()
        }
    }
}