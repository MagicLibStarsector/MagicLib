package org.magiclib.achievements;

import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import kotlin.jvm.Transient;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MagicAchievementIntel extends BaseIntelPlugin {
    public static final int ENTRY_HEIGHT = 72;
    public static final int IMAGE_HEIGHT = 48;
    private transient List<MagicAchievement> achievements = new ArrayList<>();

    public MagicAchievementIntel() {
        super();
    }

    

    @Override
    protected String getName() {
        return "Achievements";
    }

    @Override
    public void createLargeDescription(CustomPanelAPI panel, float width, float height) {
        // todo make lukas do it or something ahhhh
        for (MagicAchievement achievement : MagicAchievementManager.getInstance().getAchievements()) {
            TooltipMakerAPI element = panel.createUIElement(300f, 80f, false);
            LabelAPI labelAPI = element.addPara(achievement.getName(), 10f);
            labelAPI.setColor(Misc.getHighlightColor());
            panel.addUIElement(element).inTL(0f, 0f);
        }
    }

    public void displayAchievements(CustomPanelAPI panel, TooltipMakerAPI info, float width) {
        CustomPanelAPI row = panel.createCustomPanel(width, ENTRY_HEIGHT, null);


        TooltipMakerAPI image = row.createUIElement(IMAGE_HEIGHT, ENTRY_HEIGHT, false);
        if (complete || !def.spoiler)
            image.addImage(def.image, IMAGE_HEIGHT, IMAGE_HEIGHT, 3);

        row.addUIElement(image).inTL(0, 0);

        TooltipMakerAPI text = row.createUIElement(width * 0.75f - IMAGE_HEIGHT, ENTRY_HEIGHT, false);
        //text.setParaInsigniaLarge();
        String name = def.name;
        if (!complete && def.spoiler) name = "?";
        text.addPara(name, titleColor, 0);
        //text.setParaFontDefault();
        text.addPara(def.desc, 3);
        if (!complete && ExerelinModPlugin.isNexDev) {
            text.addButton(getString("grantMilestone"), def.id, 128, 16, pad);
        }
        if (complete) {
            Integer[] date = completedMilestones.get(def.id);
            String str = getString("awardDate");
            str = StringHelper.substituteToken(str, "$day", date[0] + "");
            str = StringHelper.substituteToken(str, "$month", date[1] + "");
            str = StringHelper.substituteToken(str, "$year", date[2] + "");
            text.addPara(str, pad);
        }

        row.addUIElement(text).rightOfTop(image, 16);

        TooltipMakerAPI pointsText = row.createUIElement(width * 0.25f, ENTRY_HEIGHT, false);
        pointsText.setParaOrbitronVeryLarge();
        pointsText.addPara(def.points + "", pad);

        row.addUIElement(pointsText).rightOfTop(text, 0);

        info.addCustom(row, first ? opad : pad);
        first = false;
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add("Achievements");
        return tags;
    }

    @Override
    public boolean hasLargeDescription() {
        return true;
    }

    @Override
    public boolean hasSmallDescription() {
        return false;
    }

    @Override
    public boolean isEnding() {
        return false;
    }

    @Override
    public boolean isEnded() {
        return false;
    }
}
