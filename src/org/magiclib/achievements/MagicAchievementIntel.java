package org.magiclib.achievements;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.magiclib.MagicLunaElementInternal;
import org.magiclib.util.MagicTxt;

import java.awt.*;
import java.text.DateFormat;
import java.util.List;
import java.util.*;

public class MagicAchievementIntel extends BaseIntelPlugin {
    private static final Logger logger = Global.getLogger(MagicAchievementIntel.class);
    public transient MagicAchievement tempAchievement;

    public MagicAchievementIntel() {
        super();

        // Previous dev version added this as non-transient, so remove it if it's there.
        if (Global.getSector().hasScript(MagicAchievementIntel.class)) {
            Global.getSector().removeScriptsOfClass(MagicAchievementIntel.class);
        }

        // Add this as a transient script if it's not already there.
        if (!Global.getSector().hasTransientScript(MagicAchievementIntel.class)) {
            Global.getSector().addTransientScript(this);
        }
    }

    @Override
    protected String getName() {
        return tempAchievement != null
                ? MagicTxt.getString("ml_ma_achievementUnlockedNotification", tempAchievement.getName())
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
        int headerHeight = 40;
        TooltipMakerAPI headerComponent = panel.createUIElement(width, headerHeight, false);

        FactionAPI faction = Global.getSector().getPlayerFaction();

        headerComponent.setParaFontVictor14();
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

        int listedAchievements = achievements.size();

        // Remove achievements that aren't displayed at all from the total and percent calculation.
        for (MagicAchievement achievement : achievements) {
            if (!achievement.shouldShowInIntel()) {
                listedAchievements--;
            }
        }


        addAchievementsHeader(panel, width, unlockedAchievements, listedAchievements, opad, headerComponent);

        TooltipMakerAPI achievementsComponent = panel.createUIElement(width, height - headerHeight - 24, true);
        if (!unlockedAchievements.isEmpty()) {
            displayAchievements(panel, achievementsComponent, width, unlockedAchievements);
        }

        if (!lockedAchievements.isEmpty()) {
//            achievementsComponent.addSectionHeading("", faction.getBaseUIColor(), faction.getDarkUIColor(), Alignment.MID, 0f);
            achievementsComponent.setParaOrbitronLarge();
            achievementsComponent.addPara(MagicTxt.getString("ml_ma_lockedAchievementsTitle"), opad);
            achievementsComponent.setParaFontDefault();
//            achievementsComponent.addSectionHeading("", faction.getBaseUIColor(), faction.getDarkUIColor(), Alignment.MID, 0f);
            displayAchievements(panel, achievementsComponent, width, lockedAchievements);
        }

        panel.addUIElement(headerComponent).inTL(0, 0);
        panel.addUIElement(achievementsComponent).belowLeft(headerComponent, 0);
    }

    private static void addAchievementsHeader(CustomPanelAPI panel, float width, List<MagicAchievement> unlockedAchievements, int listedAchievements, float opad, TooltipMakerAPI headerComponent) {
        int headerTextHeight = 30;
        float headerStartX = width * 0.24f;
        int progressBarHeight = 10;
        int imageHeight = 40;
        String defaultImage = Global.getSettings().getSpriteName("intel", "achievement");
        String headerText = MagicTxt.getString("ml_ma_achievementCompletionProgress",
                Integer.toString(unlockedAchievements.size()),
                Integer.toString(listedAchievements),
                Integer.toString((int) ((float) unlockedAchievements.size() / listedAchievements * 100))
        );

        CustomPanelAPI headerSubPanel = panel.createCustomPanel(width, headerTextHeight, null);
        TooltipMakerAPI imagePanel = headerSubPanel.createUIElement(imageHeight, imageHeight, false);
        imagePanel.addImage(defaultImage, imageHeight, imageHeight, 3);
        headerSubPanel.addUIElement(imagePanel).inTL(headerStartX, 0);

        TooltipMakerAPI headerTextAndProgressBar = headerSubPanel.createUIElement(width, headerTextHeight, false);
        headerTextAndProgressBar.setTitleOrbitronVeryLarge();
        headerTextAndProgressBar.addTitle(headerText, Misc.getBasePlayerColor());

        new ProgressBarInternal(
                unlockedAchievements.size(),
                0,
                listedAchievements,
                Misc.getTextColor(),
                headerTextAndProgressBar,
                448,
                progressBarHeight,
                false);
        headerTextAndProgressBar.getPrev().getPosition().setXAlignOffset(7).setYAlignOffset(-5);
        headerSubPanel.addUIElement(headerTextAndProgressBar)
                .rightOfTop(imagePanel, 10)
                .setYAlignOffset(-4);
        headerComponent.addCustom(headerSubPanel, 0);

        headerComponent.addSpacer(23 + progressBarHeight);
        new DividerCustomPanelPlugin(width - 7, Global.getSettings().getBasePlayerColor()).addTo(headerComponent);
    }

    /**
     * Lots of credit to Histidine for this - it's heavily based on `MilestoneTracker`.
     */
    @SuppressWarnings("SuspiciousNameCombination")
    public void displayAchievements(CustomPanelAPI panel, TooltipMakerAPI info, float rowWidth, List<MagicAchievement> achievements) {
        final int entryHeight = 72;
        final int imageHeight = 48;
        final float pad = 3;
        float opad = 10;
        boolean isFirstItem = true;
        String defaultImage = Global.getSettings().getSpriteName("intel", "achievement");

        Collections.sort(achievements, new Comparator<MagicAchievement>() {
            @Override
            public int compare(MagicAchievement leftAch, MagicAchievement rightAch) {
                // sort by achievement completion time, then rarity, then name

                if (leftAch.getDateCompleted() != null && rightAch.getDateCompleted() != null) {
                    int dateCompare = rightAch.getDateCompleted().compareTo(leftAch.getDateCompleted());
                    if (dateCompare != 0) return dateCompare;
                }

                // spoilered, incomplete achievements go to the bottom
                if (leftAch.getSpoilerLevel() == MagicAchievementSpoilerLevel.Spoiler && !leftAch.isComplete())
                    return 1;
                if (rightAch.getSpoilerLevel() == MagicAchievementSpoilerLevel.Spoiler && !rightAch.isComplete())
                    return -1;

                if (leftAch.getRarity() != rightAch.getRarity()) {
                    return leftAch.getRarity().compareTo(rightAch.getRarity());
                }

                return leftAch.getSpecId().compareTo(rightAch.getSpecId());
            }
        });

        for (final MagicAchievement achievement : achievements) {
            if (!achievement.shouldShowInIntel())
                continue;

            // Mod name header
//            if (!achievement.getModId().equals(prevModId)) {
//                info.addSectionHeading("   " + achievement.getModName(), faction.getBaseUIColor(), faction.getSecondaryUIColor(), Alignment.LMID, 10f);
//                info.getPrev().getPosition().setXAlignOffset(10f);
//                prevModId = achievement.getModId();
//            }

            // Icon
            CustomPanelAPI row = panel.createCustomPanel(rowWidth, entryHeight, null);
            TooltipMakerAPI image = row.createUIElement(imageHeight, entryHeight, false);

            if (achievement.getImage() != null && !achievement.getImage().isEmpty()) {
                try {
                    image.addImage(achievement.getImage(), imageHeight, imageHeight, 3);
                } catch (Exception ex) {
                    image.addImage(defaultImage, imageHeight, imageHeight, 3);
                }
            } else {
                image.addImage(defaultImage, imageHeight, imageHeight, 3);
            }

            row.addUIElement(image).inTL(0, 0);

            if (!achievement.isComplete()) {
                // Add a black overlay to "dim" the icon.
                // Would be better if we could just set the alpha of the image, but that doesn't seem to be possible.
                MagicLunaElementInternal imageOverlay = new MagicLunaElementInternal();
                imageOverlay.setRenderBackground(true);
                imageOverlay.setRenderBorder(false);
                imageOverlay.setEnableTransparency(true);
                imageOverlay.setBackgroundAlpha(0.6f);
                imageOverlay.setBackgroundColor(Color.BLACK);
                imageOverlay.addTo(row, imageHeight, entryHeight);
            }


            // Particle effect, if complete and not common.
            if (achievement.isComplete() && !achievement.getRarity().equals(MagicAchievementRarity.Common)) {
                row.addComponent(row.createCustomPanel(
                        imageHeight,
                        imageHeight,
                        new MagicAchievementIntelParticleEffect(image.getPosition(), imageHeight, achievement)));
            }

            float leftColWidth = rowWidth * 0.50f - imageHeight;
            float rightColWidth = rowWidth * 0.50f;
            TooltipMakerAPI leftElement = row.createUIElement(leftColWidth, entryHeight, false);
            TooltipMakerAPI rightElement = row.createUIElement(rightColWidth, entryHeight, false);
//            TooltipMakerAPI farRightElement = row.createUIElement(rowWidth * 0.20f, entryHeight, false);

            // If no description, add a blank line to put the name in the middle.
            boolean showDescription = achievement.isComplete() || achievement.getSpoilerLevel() == MagicAchievementSpoilerLevel.Visible;
            if (!showDescription) {
                // Blank line if the desc isn't shown to put name in the middle.
                leftElement.addPara("", 3);
            }

            // Name
            String name = achievement.getName();

            leftElement.setTitleOrbitronLarge();
            leftElement.addSpacer(2);
            leftElement.addTitle(name, achievement.isComplete()
                    ? Misc.getBasePlayerColor()
                    : Misc.getTextColor().darker());

            // Name of mod that added the achievement.
            leftElement.addPara(achievement.getModName(), Misc.getGrayColor(), 0);
            leftElement.addSpacer(2);

            int maxLengthOfDescription = 150;
            boolean doesDescriptionGetCutOff = false;

            // Description or error message if there is one.
            if (achievement.errorMessage == null) {
                boolean isSpoilered = !achievement.isComplete() && achievement.getSpoilerLevel() != MagicAchievementSpoilerLevel.Visible;

                if (isSpoilered) {
                    leftElement.addPara(MagicTxt.getString("ml_ma_spoilered"), Misc.getTextColor().darker(), 0);
                } else if (showDescription) {
                    String description = MagicTxt.ellipsizeStringAfterLength(achievement.getDescription(), maxLengthOfDescription);
                    doesDescriptionGetCutOff = description.length() < achievement.getDescription().length();

                    if (achievement.isComplete()) {
                        leftElement.addPara(description, 0);
                    } else {
                        leftElement.addPara(description, Misc.getTextColor().darker(), 0);
                    }
                }
            } else {
                leftElement.addPara(achievement.errorMessage, Misc.getNegativeHighlightColor(), 0);
            }

            // Debug buttons
            if (Global.getSettings().isDevMode()) {
                if (!achievement.isComplete())
                    leftElement.addButton(MagicTxt.getString("ml_ma_grantAchievement"), achievement.getSpecId(), 128, 16, pad);
                if (achievement.isComplete())
                    leftElement.addButton(MagicTxt.getString("ml_ma_resetAchievement"), achievement.getSpecId(), 128, 16, pad);
            }

            // Tooltip
            if (achievement.hasTooltip() || doesDescriptionGetCutOff) {
                leftElement.addTooltipTo(getTooltipCreator(achievement, pad), row, TooltipMakerAPI.TooltipLocation.ABOVE);
            }

            // Progress bar
            float progressBarWidth = 225;
            float progressBarHeight = 15f;
            float progressBarYOffset = -pad;

            if (achievement.getHasProgressBar()) {
                try {
                    if (!achievement.isComplete()) {
//                        rightElement.addPara("", progressBarHeight + progressBarYOffset);
                    }
                    new ProgressBarInternal(
                            achievement.isComplete() ? achievement.getMaxProgress() : achievement.getProgress(),
                            0,
                            achievement.getMaxProgress(),
                            Misc.getTextColor(),
                            rightElement,
                            progressBarWidth,
                            progressBarHeight,
                            !achievement.isComplete());
                    rightElement.getPrev().getPosition().setYAlignOffset(progressBarYOffset);
                } catch (Exception ex) {
                    logger.info(String.format("Failed to create progress bar for achievement %s from mod %s: %s",
                            achievement.getSpecId(), achievement.getModId(), ex.getMessage()));
                }
            } else if (achievement.isComplete()) {
                rightElement.addSpacer(progressBarHeight + progressBarYOffset + pad + 1);
            }

            // Completed info, shown on the right.
            if (achievement.isComplete()) {
                Date date = achievement.getDateCompleted();
                String str = MagicTxt.getString("ml_ma_achievementCompletedDate", DateFormat.getDateInstance().format(date), DateFormat.getTimeInstance(DateFormat.SHORT).format(date));
                rightElement.addPara(str, pad + 1);

                if (achievement.getCompletedByUserName() != null) {
                    rightElement.addPara(MagicTxt.getString("ml_ma_achievementCompletedBy", achievement.getCompletedByUserName()), pad);
                }
            }

            // Put it all together.
            row.addUIElement(leftElement).rightOfTop(image, 16);
            row.addUIElement(rightElement).rightOfTop(image, 16).setXAlignOffset(leftElement.getWidthSoFar() + imageHeight);//.setXAlignOffset((rowWidth * 0.75f) - barWidth - IMAGE_HEIGHT - pad);
//            row.addUIElement(farRightElement).rightOfTop(image, 16).setXAlignOffset(leftElement.getWidthSoFar() + rightElement.getWidthSoFar());//.setXAlignOffset((rowWidth * 0.75f) - barWidth - IMAGE_HEIGHT - pad);
            PositionAPI pos = row.getPosition();

            TooltipMakerAPI pointsText = row.createUIElement(rowWidth * 0.25f, entryHeight, false);
            pointsText.setParaOrbitronVeryLarge();
//            pointsText.addPara(def.points + "", pad);

            row.addUIElement(pointsText).rightOfTop(leftElement, 0);

            info.addCustom(row, isFirstItem ? opad : pad);
            isFirstItem = false;
        }
    }

    @NotNull
    private static BaseTooltipCreator getTooltipCreator(final MagicAchievement achievement, final float pad) {
        return new BaseTooltipCreator() {
            @Override
            public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                super.createTooltip(tooltip, expanded, tooltipParam);
                achievement.createTooltip(tooltip, expanded, getTooltipWidth(tooltipParam));
            }
        };
    }

    @Override
    public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
        MagicAchievement achievement = MagicAchievementManager.getInstance().getAchievement((String) buttonId);

        if (achievement == null) {
            logger.warn(String.format("Unable to find achievement with ID %s", buttonId));
            return;
        }

        if (!achievement.isComplete()) {
            achievement.completeAchievement(Global.getSector().getPlayerPerson());
        } else {
            achievement.uncompleteAchievement(true);
        }

        achievement.saveChanges();
        ui.updateUIForItem(this);
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add("Personal");
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
