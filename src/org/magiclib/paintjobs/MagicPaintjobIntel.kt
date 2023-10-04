package org.magiclib.paintjobs

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.SectorMapAPI
import org.magiclib.util.MagicTxt

class MagicPaintjobIntel : BaseIntelPlugin() {
    override fun getName(): String = MagicTxt.getString("ml_mp_intelName")

    override fun createLargeDescription(panel: CustomPanelAPI?, width: Float, height: Float) {
        panel ?: return
        val opad = 10f
        val pad = 3f
        val pjs = MagicPaintjobManager.paintjobs
        val pjMainContainer = panel.createUIElement(width, height, true)
        val pjMain = panel.createCustomPanel(width, height, null)
        val baseUnit = 8f
        val cellWidth = baseUnit * 18
        val cellHeight = baseUnit * 10
        val imageSize = baseUnit * 14
        val gridWidth = (width / cellWidth).toInt()

        // create a grid using a nested loop
        var x = 0
        var y = 0
        for (pj in pjs) {
            val pjCell = pjMain.createUIElement(cellWidth, cellHeight, false)
            Global.getSettings().loadTexture(pj.spriteId)
            pjCell.addImage(pj.spriteId, imageSize, imageSize, opad)
            pjCell.addPara(pj.name, opad)
            pjCell.addPara(pj.description, opad)
            pjMain.addUIElement(pjCell).inTL(x * cellWidth, y * cellHeight)
            x++
            if (x >= gridWidth) {
                x = 0
                y++
            }
        }

        pjMainContainer.addCustom(pjMain, 0f)


//        pjPanel.addPara(pj.name, 10f)

        panel.addUIElement(pjMainContainer).inTL(0f, 0f)
    }

    override fun getIntelTags(map: SectorMapAPI?): Set<String> = super.getIntelTags(map) + "Personal"
    override fun hasLargeDescription(): Boolean = true
    override fun hasSmallDescription(): Boolean = false
    override fun isEnded(): Boolean = false
    override fun isEnding(): Boolean = false
}