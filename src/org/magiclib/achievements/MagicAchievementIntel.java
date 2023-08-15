package org.magiclib.achievements;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import org.magiclib.util.MagicTxt;

import java.awt.*;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class MagicAchievementIntel extends BaseIntelPlugin {
    public static final int ENTRY_HEIGHT = 72;
    public static final int IMAGE_HEIGHT = 48;

    public MagicAchievementIntel() {
        super();
    }


    @Override
    protected String getName() {
        return "Achievements";
    }

    @Override
    public String getIcon() {
        return Global.getSettings().getSpriteName("intel", "achievement");
    }

    @Override
    public void createLargeDescription(CustomPanelAPI panel, float width, float height) {
        float opad = 10;
        float pad = 3;
        TooltipMakerAPI info = panel.createUIElement(width, height, true);

        FactionAPI faction = Global.getSector().getPlayerFaction();

        info.setParaFontVictor14();
//        info.addPara("test?", opad);
        List<MagicAchievement> achievements = MagicAchievementManager.getInstance().getAchievements();

        List<MagicAchievement> unlockedAchievements = new ArrayList<>();
        for (MagicAchievement achievement : achievements) {
            if (achievement.isComplete()) {
                unlockedAchievements.add(achievement);
            }
        }

        List<MagicAchievement> lockedAchievements = new ArrayList<>();
        for (MagicAchievement achievement : achievements) {
            if (!achievement.isComplete()) {
                lockedAchievements.add(achievement);
            }
        }

        info.addSectionHeading("", faction.getBaseUIColor(), faction.getDarkUIColor(), Alignment.MID, 0f);
        info.addSectionHeading(MagicTxt.getString("achievementCompletionProgress",
                        Integer.toString(unlockedAchievements.size()),
                        Integer.toString(achievements.size())), faction.getBaseUIColor(),
                faction.getDarkUIColor(), Alignment.MID, 0f);
        info.addSectionHeading("", faction.getBaseUIColor(), faction.getDarkUIColor(), Alignment.MID, 0f);

        if (!unlockedAchievements.isEmpty()) {
//            info.addSectionHeading(MagicTxt.getString("unlockedAchievementsTitle"), faction.getBaseUIColor(),
//                    faction.getDarkUIColor(), Alignment.MID, opad);
            displayAchievements(panel, info, width, unlockedAchievements);
        }

        if (!lockedAchievements.isEmpty()) {
            info.addSectionHeading(MagicTxt.getString("lockedAchievementsTitle"), faction.getBaseUIColor(),
                    faction.getDarkUIColor(), Alignment.MID, opad);
            displayAchievements(panel, info, width, lockedAchievements);
        }

        panel.addUIElement(info).inTL(0, 0);
    }

    /**
     * Lots of credit to Histidine for this - it's heavily based on `MilestoneTracker`.
     */
    @SuppressWarnings("SuspiciousNameCombination")
    public void displayAchievements(CustomPanelAPI panel, TooltipMakerAPI info, float width, List<MagicAchievement> achievements) {
        Color titleColor = Misc.getHighlightColor();
        float pad = 3;
        float opad = 10;
        boolean isFirstItem = true;
        String defaultImage = Global.getSettings().getSpriteName("intel", "achievement");

        for (MagicAchievement achievement : achievements) {
            if (achievement.getSpoilerLevel() == SpoilerLevel.Hidden)
                continue;

            CustomPanelAPI row = panel.createCustomPanel(width, ENTRY_HEIGHT, null);
            TooltipMakerAPI image = row.createUIElement(IMAGE_HEIGHT, ENTRY_HEIGHT, false);
            if (achievement.isComplete()) {
                if (achievement.getImage() != null && !achievement.getImage().isEmpty()) {
                    try {
                        image.addImage(achievement.getImage(), IMAGE_HEIGHT, IMAGE_HEIGHT, 3);
                    } catch (Exception ex) {
                        image.addImage(defaultImage, IMAGE_HEIGHT, IMAGE_HEIGHT, 3);
                    }
                } else {
                    image.addImage(defaultImage, IMAGE_HEIGHT, IMAGE_HEIGHT, 3);
                }
            } else {
                image.addImage(defaultImage, IMAGE_HEIGHT, IMAGE_HEIGHT, 3);
            }

            row.addUIElement(image).inTL(0, 0);

            TooltipMakerAPI text = row.createUIElement(width * 0.75f - IMAGE_HEIGHT, ENTRY_HEIGHT, false);
            boolean showDescription = achievement.isComplete() || achievement.getSpoilerLevel() == SpoilerLevel.Visible;

            if (!showDescription) {
                // Blank line if the desc isn't show to put title in the middle.
                text.addPara("", 3);
            }

            String name = achievement.getName();
            if (!achievement.isComplete() && achievement.getSpoilerLevel() != SpoilerLevel.Visible) {
                name = "(hidden)";
            }

            text.addPara(name, titleColor, 0);

            if (showDescription) {
                text.addPara(achievement.getDescription(), 3);
            }

            // Debugging TODO
            if (!achievement.isComplete() && Global.getSettings().isDevMode()) {
                text.addButton(MagicTxt.getString("grantAchievement"), achievement.getSpecId(), 128, 16, pad);
            }

            if (achievement.isComplete()) {
                Date date = achievement.getDateCompleted();
                String str = MagicTxt.getString("achievementCompletedDate", DateFormat.getDateInstance().format(date));
                text.addPara(str, pad);

                if (achievement.getCompletedByUserName() != null) {
                    text.addPara(MagicTxt.getString("achievementCompletedBy", achievement.getCompletedByUserName()), pad);
                }
            }

            row.addUIElement(text).rightOfTop(image, 16);

            TooltipMakerAPI pointsText = row.createUIElement(width * 0.25f, ENTRY_HEIGHT, false);
            pointsText.setParaOrbitronVeryLarge();
//            pointsText.addPara(def.points + "", pad);

            row.addUIElement(pointsText).rightOfTop(text, 0);

            info.addCustom(row, isFirstItem ? opad : pad);
            isFirstItem = false;
        }
    }

    @Override
    public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
        MagicAchievement achievement = MagicAchievementManager.getInstance().getAchievement((String) buttonId);
        achievement.completeAchievement(Global.getSector().getPlayerPerson());
        achievement.saveChanges();
        ui.updateUIForItem(this);
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
