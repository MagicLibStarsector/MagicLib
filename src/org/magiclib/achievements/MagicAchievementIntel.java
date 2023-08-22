package org.magiclib.achievements;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import org.apache.log4j.Logger;
import org.magiclib.util.MagicTxt;

import java.text.DateFormat;
import java.util.*;

public class MagicAchievementIntel extends BaseIntelPlugin {
    private static final Logger logger = Global.getLogger(MagicAchievementIntel.class);
    public static final int ENTRY_HEIGHT = 72;
    public static final int IMAGE_HEIGHT = 48;
    public transient MagicAchievement tempAchievement;

    public MagicAchievementIntel() {
        super();
        if (!Global.getSector().hasScript(MagicAchievementIntel.class)) {
            Global.getSector().addScript(this);
        }
    }


    @Override
    protected String getName() {
        return tempAchievement != null
                ? MagicTxt.getString("achievementUnlockedNotification", tempAchievement.getName())
                : "Achievements";
    }

    @Override
    protected void addBulletPoints(TooltipMakerAPI info, ListInfoMode mode) {
        if (tempAchievement != null) {
            info.addPara(tempAchievement.getName(), 3f);
        }
    }

    @Override
    public String getIcon() {
        return (tempAchievement != null && tempAchievement.getImage() != null && !tempAchievement.getImage().isEmpty())
                ? tempAchievement.getImage()
                : Global.getSettings().getSpriteName("intel", "achievement");
    }

    @Override
    public void createLargeDescription(CustomPanelAPI panel, float width, float height) {
        float opad = 10;
        float pad = 3;
        TooltipMakerAPI info = panel.createUIElement(width, height, true);

        FactionAPI faction = Global.getSector().getPlayerFaction();

        info.setParaFontVictor14();
//        info.addPara("test?", opad);
        Collection<MagicAchievement> achievements = MagicAchievementManager.getInstance().getAchievements().values();

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
        int listedAchievements = achievements.size();

        // Remove achievements that aren't displayed at all from the total and percent calculation.
        for (MagicAchievement achievement : achievements) {
            if (!achievement.shouldShowInIntel()) {
                listedAchievements--;
            }
        }

        info.addSectionHeading(MagicTxt.getString("achievementCompletionProgress",
                        Integer.toString(unlockedAchievements.size()),
                        Integer.toString(listedAchievements),
                        Integer.toString((int) ((float) unlockedAchievements.size() / listedAchievements * 100))
                ), faction.getBaseUIColor(),
                faction.getDarkUIColor(), Alignment.MID, 0f);
        info.addSectionHeading("", faction.getBaseUIColor(), faction.getDarkUIColor(), Alignment.MID, 0f);

        if (!unlockedAchievements.isEmpty()) {
//            info.addSectionHeading(MagicTxt.getString("unlockedAchievementsTitle"), faction.getBaseUIColor(),
//                    faction.getDarkUIColor(), Alignment.MID, opad);
            displayAchievements(panel, info, width, unlockedAchievements);
        }

        if (!lockedAchievements.isEmpty()) {
            info.addSectionHeading("", faction.getBaseUIColor(), faction.getDarkUIColor(), Alignment.MID, 0f);
            info.addSectionHeading(MagicTxt.getString("lockedAchievementsTitle"), faction.getBaseUIColor(),
                    faction.getDarkUIColor(), Alignment.MID, 0f);
            info.addSectionHeading("", faction.getBaseUIColor(), faction.getDarkUIColor(), Alignment.MID, 0f);
            displayAchievements(panel, info, width, lockedAchievements);
        }

        panel.addUIElement(info).inTL(0, 0);
    }

    /**
     * Lots of credit to Histidine for this - it's heavily based on `MilestoneTracker`.
     */
    @SuppressWarnings("SuspiciousNameCombination")
    public void displayAchievements(CustomPanelAPI panel, TooltipMakerAPI info, float rowWidth, List<MagicAchievement> achievements) {
        float pad = 3;
        float opad = 10;
        FactionAPI faction = Global.getSector().getPlayerFaction();
        boolean isFirstItem = true;
        String defaultImage = Global.getSettings().getSpriteName("intel", "achievement");

        Collections.sort(achievements, new Comparator<MagicAchievement>() {
            @Override
            public int compare(MagicAchievement leftAch, MagicAchievement rightAch) {
                // spoilered, incomplete achievements go to the bottom
                if (leftAch.getSpoilerLevel() == MagicAchievementSpoilerLevel.Spoiler && !leftAch.isComplete())
                    return 1;
                if (rightAch.getSpoilerLevel() == MagicAchievementSpoilerLevel.Spoiler && !rightAch.isComplete())
                    return -1;

                // sort by mod name, then by achievement spec id
                ModSpecAPI leftMod = Global.getSettings().getModManager().getModSpec(leftAch.getModId());
                ModSpecAPI rightMod = Global.getSettings().getModManager().getModSpec(rightAch.getModId());
                String leftModName = leftMod != null ? leftMod.getName() : leftAch.getModId();
                String rightModName = rightMod != null ? rightMod.getName() : rightAch.getModId();
                int modNameCompare = leftModName.compareTo(rightModName);
                if (modNameCompare != 0) return modNameCompare;
                return leftAch.getSpecId().compareTo(rightAch.getSpecId());
            }
        });

        String prevModId = null;

        for (MagicAchievement achievement : achievements) {
            if (!achievement.shouldShowInIntel())
                continue;

            // Mod name header
            if (!achievement.getModId().equals(prevModId)) {
                info.addSectionHeading(achievement.getModName(), faction.getBaseUIColor(), faction.getSecondaryUIColor(), Alignment.LMID, 10f);
                info.getPrev().getPosition().setXAlignOffset(10f);
                prevModId = achievement.getModId();
            }

            // Icon
            CustomPanelAPI row = panel.createCustomPanel(rowWidth, ENTRY_HEIGHT, null);
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

            // Particle effect, if complete and not common.
            if (achievement.isComplete() && !achievement.getRarity().equals(MagicAchievementRarity.Common)) {
                row.addComponent(row.createCustomPanel(
                        IMAGE_HEIGHT,
                        IMAGE_HEIGHT,
                        new MagicAchievementIntelParticleEffect(image.getPosition(), IMAGE_HEIGHT, achievement)));
            }

            // Description
            TooltipMakerAPI leftElement = row.createUIElement(rowWidth * 0.75f - IMAGE_HEIGHT, ENTRY_HEIGHT, false);
            TooltipMakerAPI rightElement = row.createUIElement(rowWidth * 0.75f - IMAGE_HEIGHT, ENTRY_HEIGHT, false);
            boolean showDescription = achievement.isComplete() || achievement.getSpoilerLevel() == MagicAchievementSpoilerLevel.Visible;

            if (!showDescription) {
                // Blank line if the desc isn't show to put title in the middle.
                leftElement.addPara("", 3);
            }

            // Name
            String name = achievement.getName();
            if (!achievement.isComplete() && achievement.getSpoilerLevel() != MagicAchievementSpoilerLevel.Visible) {
                name = "(hidden)";
            }

            leftElement.addPara(name, achievement.isComplete()
                    ? Misc.getHighlightColor()
                    : Misc.getTextColor(), 0);

            // Error message if there is one.
            if (achievement.errorMessage == null) {
                if (showDescription) {
                    leftElement.addPara(achievement.getDescription(), 3);
                }
            } else {
                leftElement.addPara(achievement.errorMessage, Misc.getNegativeHighlightColor(), 3);
            }

            // Debug buttons
            if (Global.getSettings().isDevMode()) {
                if (!achievement.isComplete())
                    leftElement.addButton(MagicTxt.getString("grantAchievement"), achievement.getSpecId(), 128, 16, pad);
                if (achievement.isComplete())
                    leftElement.addButton(MagicTxt.getString("resetAchievement"), achievement.getSpecId(), 128, 16, pad);
            }

            // Completed info, shown on the right.
            if (achievement.isComplete()) {
                Date date = achievement.getDateCompleted();
                String str = MagicTxt.getString("achievementCompletedDate", DateFormat.getDateTimeInstance().format(date), DateFormat.getTimeInstance().format(date));
                rightElement.addPara(str, pad);

                if (achievement.getCompletedByUserName() != null) {
                    rightElement.addPara(MagicTxt.getString("achievementCompletedBy", achievement.getCompletedByUserName()), pad);
                }
            }

            // Progress bar
            float barWidth = rowWidth / 6;

            if (achievement.getHasProgressBar()) {
                try {
                    if (!achievement.isComplete()) {
                        rightElement.addPara("", pad);
                    }
                    new ProgressBarInternal(
                            achievement.getProgress(),
                            0,
                            achievement.getMaxProgress(),
                            Misc.getTextColor(),
                            rightElement,
                            barWidth,
                            15f,
                            !achievement.isComplete());
                    rightElement.getPrev().getPosition().setYAlignOffset(-5f);
                } catch (Exception ex) {
                    logger.info(String.format("Failed to create progress bar for achievement %s from mod %s: %s",
                            achievement.getSpecId(), achievement.getModId(), ex.getMessage()));
                }
            }

            // Put it all together.
            row.addUIElement(leftElement).rightOfTop(image, 16);
            row.addUIElement(rightElement).rightOfTop(image, 16).setXAlignOffset((rowWidth * 0.75f) - barWidth - IMAGE_HEIGHT - pad);
            PositionAPI pos = row.getPosition();

            TooltipMakerAPI pointsText = row.createUIElement(rowWidth * 0.25f, ENTRY_HEIGHT, false);
            pointsText.setParaOrbitronVeryLarge();
//            pointsText.addPara(def.points + "", pad);

            row.addUIElement(pointsText).rightOfTop(leftElement, 0);

            info.addCustom(row, isFirstItem ? opad : pad);
            isFirstItem = false;
        }
    }

    @Override
    public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
        MagicAchievement achievement = MagicAchievementManager.getInstance().getAchievement((String) buttonId);

        if (achievement == null) {
            logger.warn(String.format("Unable to find achievement with ID %s", buttonId));
            return;
        }

        if (achievement.isComplete()) {
            achievement.completeAchievement(Global.getSector().getPlayerPerson());
        } else {
            achievement.uncompleteAchievement();
        }

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
