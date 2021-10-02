package data.scripts.bounty;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.MagicTxt;
import data.scripts.util.StringCreator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Iterator;
import java.util.List;

import static data.scripts.util.MagicTxt.nullStringIfEmpty;
import java.util.ArrayList;

public final class ActiveBounty {
    /**
     * A unique key for the bounty, as used by [MagicBountyCoordinator].
     */
    private final @NotNull String bountyKey;

    /**
     * The bounty fleet. The thing to kill.
     * Created without a location initially. A location (from `fleetLocation`) when the bounty is accepted.
     */
    private final @NotNull CampaignFleetAPI fleet;

    /**
     * The spawn location of the fleet.
     */
    private final @NotNull SectorEntityToken fleetSpawnLocation;

    private final @NotNull List<String> presetShipIds;
    /**
     * The original bounty spec, a mirror of the json definition.
     */
    private final @NotNull MagicBountyData.bountyData spec;

    /**
     * The timestamp of when the bounty was first created (not accepted).
     **/
    private final @NotNull Long bountyCreatedTimestamp;

    /**
     * The target captain of the bounty fleet.
     **/
    private final @NotNull PersonAPI captain;

    /**
     * The id of the fleet's flagship. This variable will be set even after the fleet is destroyed.
     */
    private final @Nullable String flagshipId;

    /**
     * The timestamp of when the player accepted the bounty, if they have done so.
     **/
    private @Nullable Long acceptedBountyTimestamp;

    /**
     * The result of the bounty, if there has been a terminus.
     */
    private @Nullable BountyResult bountyResult;

    /**
     * The planet/station/etc from where the bounty was accepted.
     */
    private @Nullable SectorEntityToken bountySource;

    private @NotNull Stage stage = Stage.NotAccepted;

    /**
     * The number of credits that was promised as a reward upon completion. Includes scaling, if applicable.
     */
    private @Nullable Float rewardCredits;

    /**
     * @param bountyKey          A unique key for the bounty, as used by [MagicBountyCoordinator].
     * @param fleet              The fleet that, when destroyed, completes the bounty. Should have no location to start with.
     *                           The fleet's location will be set when the bounty is accepted (from fleetSpawnLocation).
     * @param fleetSpawnLocation The location to spawn the fleet when the bounty is accepted.
     * @param presetShipIds      Ships that should always be added, if fleet DP allows.
     * @param spec               The original bounty spec, a mirror of the json definition.
     */
    public ActiveBounty(@NotNull String bountyKey,
                        @NotNull CampaignFleetAPI fleet,
                        @NotNull SectorEntityToken fleetSpawnLocation,
                        @NotNull List<String> presetShipIds,
                        @NotNull MagicBountyData.bountyData spec) {
        this.bountyKey = bountyKey;
        this.fleet = fleet;
        this.fleetSpawnLocation = fleetSpawnLocation;
        this.presetShipIds = presetShipIds;
        this.spec = spec;
        this.bountyCreatedTimestamp = Global.getSector().getClock().getTimestamp();
        this.flagshipId = fleet.getFlagship() != null ? fleet.getFlagship().getId() : null;
        this.captain = fleet.getCommander();
    }

    /**
     * Call when the player accepts a bounty.
     * <br /> - Spawns the bounty fleet.
     * <br /> - Adds Intel to the Intel Manager.
     *
     * @param bountySource  From where the bounty was accepted from.
     * @param rewardCredits The number of credits to give as a reward. Null or zero if no reward.
     */
    public void acceptBounty(@NotNull SectorEntityToken bountySource, @Nullable Float rewardCredits) {
        this.rewardCredits = rewardCredits;
        acceptedBountyTimestamp = Global.getSector().getClock().getTimestamp();
        stage = Stage.Accepted;
        this.bountySource = bountySource;

        LocationAPI systemLocation = fleetSpawnLocation.getContainingLocation();
        systemLocation.addEntity(getFleet());
        getFleet().setLocation(fleetSpawnLocation.getLocation().x, fleetSpawnLocation.getLocation().y);
        getFleet().getAI().addAssignment(
                getSpec().fleet_behavior == null
                        ? FleetAssignment.ORBIT_AGGRESSIVE
                        : getSpec().fleet_behavior,
                fleetSpawnLocation,
                1000000f,
                null);

        // Flag fleet as important so it has a target icon
        Misc.makeImportant(getFleet(), "magicbounty");

        IntelManagerAPI intelManager = Global.getSector().getIntelManager();
        List<IntelInfoPlugin> existingMagicIntel = intelManager.getIntel(MagicBountyIntel.class);
        MagicBountyIntel intelForBounty = null;

        // Intel shouldn't already exist since we're just accepting it now, but just in case.
        for (IntelInfoPlugin bounty : existingMagicIntel) {
            if (((MagicBountyIntel) bounty).bountyKey.equals(this.bountyKey)) {
                intelForBounty = (MagicBountyIntel) bounty;
            }
        }

        if (intelForBounty == null) {
            intelForBounty = new MagicBountyIntel(bountyKey);
            intelManager.addIntel(intelForBounty);
        }

        if (MagicTxt.nullStringIfEmpty(spec.job_memKey) != null) {
            Global.getSector().getMemoryWithoutUpdate().set(spec.job_memKey, false);
        }

        if (MagicTxt.nullStringIfEmpty(spec.job_pick_script) != null) {
            runRuleScript(spec.job_pick_script);
        }
    }

    /**
     * Finishes the bounty with the provided result.
     * Idempotent (if called more than once with the same result, will not trigger again).
     * <br /> - Updates intel.
     *
     * @param result The final result of the bounty.
     */
    public void endBounty(@NotNull BountyResult result) {
        if (bountyResult == result) {
            return;
        }

        this.bountyResult = result;
        Misc.makeUnimportant(getFleet(), "magicbounty");

        if (result instanceof BountyResult.Succeeded) {
            stage = Stage.Succeeded;

            if (((BountyResult.Succeeded) result).shouldRewardCredits && getRewardCredits() != null) {
                Global.getSector().getPlayerFleet().getCargo().getCredits().add(getRewardCredits());
            }

            if (MagicTxt.nullStringIfEmpty(spec.job_memKey) != null) {
                Global.getSector().getMemoryWithoutUpdate().set(spec.job_memKey, true);
            }

            if (MagicTxt.nullStringIfEmpty(spec.job_conclusion_script) != null) {
                runRuleScript(spec.job_conclusion_script);
            }
        } else if (result instanceof BountyResult.EndedWithoutPlayerInvolvement) {
            stage = Stage.EndedWithoutPlayerInvolvement;
        } else if (result instanceof BountyResult.FailedOutOfTime) {
            stage = Stage.Failed;
        } else if (result instanceof BountyResult.ExpiredWithoutAccepting) {
            stage = Stage.ExpiredWithoutAccepting;
        } else if (result instanceof BountyResult.DismissedPermanently) {
            stage = Stage.Dismissed;
        }

        MagicBountyIntel intel = getIntel();

        if (intel != null) {
            intel.sendUpdateIfPlayerHasIntel(new Object(), false);
        }

//        destroy(); // Caused ConcurrentModification crash
    }

    private void runRuleScript(String scriptRuleId) {
        InteractionDialogAPI dialog = Global.getSector().getCampaignUI().getCurrentInteractionDialog();
        boolean didCreateDialog = false;

        if (dialog == null) {
            Global.getSector().getCampaignUI().showInteractionDialog(Global.getSector().getPlayerFleet());
            dialog = Global.getSector().getCampaignUI().getCurrentInteractionDialog();
            didCreateDialog = true;
        }

        boolean flagSetting = DebugFlags.PRINT_RULES_DEBUG_INFO;

        if (Global.getSettings().isDevMode()) {
            DebugFlags.PRINT_RULES_DEBUG_INFO = true;
        }

        // This does not work.
//        ScriptEvaluator eval = new ScriptEvaluator();
//        try {
//            eval.cook("BountyScriptTest.java", Paths.get("", "data/scripts/bounty/rulecmd").toString());
//            eval.evaluate(null);
//        } catch (Exception e) {
//            Global.getLogger(ActiveBounty.class).error(e);
//        }
        FireBest.fire(null, dialog, dialog.getPlugin().getMemoryMap(), scriptRuleId);

        // Turn it on for FireBest, then set it back to whatever it was.
        DebugFlags.PRINT_RULES_DEBUG_INFO = flagSetting;

        if (didCreateDialog && Global.getSector().getCampaignUI().getCurrentInteractionDialog() != null) {
            Global.getSector().getCampaignUI().getCurrentInteractionDialog().dismiss();
        }
    }

    private void destroy() {
        if (fleet != null && !fleet.isDespawning()) {
            fleet.despawn();
        }
    }

    /**
     * @return Float.POSITIVE_INFINITY if there is no time limit or quest hasn't been accepted.
     */
    public @NotNull Float getDaysRemainingToComplete() {
        if (getSpec().job_deadline > 0 && acceptedBountyTimestamp != null) {
            return Math.max(1, getSpec().job_deadline - Global.getSector().getClock().getElapsedDaysSince(acceptedBountyTimestamp));
        } else {
            return Float.POSITIVE_INFINITY;
        }
    }

    public @NotNull String getKey() {
        return bountyKey;
    }

    public @NotNull CampaignFleetAPI getFleet() {
        return fleet;
    }

    public @NotNull MagicBountyData.bountyData getSpec() {
        return spec;
    }

    public @Nullable SectorEntityToken getBountySource() {
        return bountySource;
    }

    public @NotNull Stage getStage() {
        return stage;
    }

    public @Nullable Float getRewardCredits() {
        return rewardCredits;
    }

    public @NotNull SectorEntityToken getFleetSpawnLocation() {
        return fleetSpawnLocation;
    }

    public @NotNull Long getBountyCreatedTimestamp() {
        return bountyCreatedTimestamp;
    }

    public @Nullable String getFlagshipId() {
        return flagshipId;
    }

    public @NotNull PersonAPI getCaptain() {
        return captain;
    }

    /**
     * The faction that offered the bounty, if any.
     */
    @Nullable
    public FactionAPI getGivingFaction() {
        return MagicTxt.nullStringIfEmpty(getSpec().job_forFaction) != null
                ? Global.getSector().getFaction(getSpec().job_forFaction)
                : null;
    }

    /**
     * The color for the giving faction, or Misc.getTextColor() if none.
     */
    @NotNull
    public Color getGivingFactionTextColor() {
        if (getGivingFaction() != null) {
            return getGivingFaction().getBaseUIColor();
        } else {
            return Misc.getTextColor();
        }
    }

    /**
     * Calculates and returns the number of credits that will be awarded upon completion, if any.
     * Includes any scaling factor.
     */
    @Nullable
    Float calculateCreditReward() {
        if (spec.job_credit_reward <= 0) {
            return null;
        }

        int bountyFPIncreaseOverBaseDueToScaling = getFleet().getFleetPoints() - getSpec().fleet_min_DP;

        // Math.max in case the scaling ends up negative, we don't want to subtract from the base reward.
        float bonusCreditsFromScaling = Math.max(0, getSpec().job_reward_scaling * bountyFPIncreaseOverBaseDueToScaling);
        float reward = Math.round(getSpec().job_credit_reward + bonusCreditsFromScaling);
        float rewardRoundedToNearest100 = Math.round(reward / 100.0) * 100;
        Global.getLogger(ActiveBounty.class)
                .info(String.format("Rounded reward of %sc for bounty '%s' has base %sc and scaled bonus of %sc (%s scaling * %s FP diff)",
                        rewardRoundedToNearest100,
                        getKey(),
                        getSpec().job_credit_reward,
                        bonusCreditsFromScaling,
                        getSpec().job_reward_scaling,
                        bountyFPIncreaseOverBaseDueToScaling));

        return rewardRoundedToNearest100;
    }

    /**
     * The [MagicBountyIntel] active for this bounty, if there is any. <br />
     * There will only be intel if the bounty has been accepted (and isn't long past ended).
     */
    @Nullable
    public MagicBountyIntel getIntel() {
        List<IntelInfoPlugin> intels = Global.getSector().getIntelManager().getIntel(MagicBountyIntel.class);

        for (IntelInfoPlugin intel : intels) {
            MagicBountyIntel bountyIntel = (MagicBountyIntel) intel;

            if (bountyIntel.bountyKey.equals(this.bountyKey)) {
                return bountyIntel;
            }
        }

        return null;
    }

    /**
     * Adds the description for the bounty to a [TextPanelAPI].
     *
     * @param text The [TextPanelAPI] to write to.
     */
    public void addDescriptionToTextPanel(TextPanelAPI text) {
        addDescriptionToTextPanelInternal(text, Misc.getTextColor(), 0f);
    }

    /**
     * Adds the description for the bounty to a [TooltipMakerAPI].
     *
     * @param text    The [TooltipMakerAPI] to write to.
     * @param padding The amount of padding to use, in pixels.
     */
    public void addDescriptionToTextPanel(TooltipMakerAPI text, Color color, float padding) {
        addDescriptionToTextPanelInternal(text, color, padding);
    }

    /**
     * Whether the bounty has a credit reward or not.
     */
    public boolean hasCreditReward() {
        return getRewardCredits() != null && getRewardCredits() > 0;
    }

    /**
     * Whether the bounty expires or not.
     */
    public boolean hasExpiration() {
        return getDaysRemainingToComplete() != Float.POSITIVE_INFINITY;
    }

    public @NotNull List<String> getPresetShipIds() {
        return presetShipIds;
    }

    public List<FleetMemberAPI> getPresetShipsInFleet() {
        List<FleetMemberAPI> ships = getFleet().getMembersWithFightersCopy();

        for (Iterator<FleetMemberAPI> iterator = ships.iterator(); iterator.hasNext(); ) {
            FleetMemberAPI ship = iterator.next();

            if (!getPresetShipIds().contains(ship.getId())) {
                iterator.remove();
            }
        }

        return ships;
    }
    
    public List<FleetMemberAPI> getFlagshipInFleet() {
        List<FleetMemberAPI> ships = new ArrayList<>();
        ships.add(getFleet().getFlagship());

        return ships;
    }

    private void addDescriptionToTextPanelInternal(Object text, Color color, float padding) {
        if (nullStringIfEmpty(spec.job_description) != null) {
            String[] paras = spec.job_description.split("/n|\\n");
            for (String para : paras) {
                String replacedPara = para;

                final ActiveBounty finalActiveBounty = this;
                replacedPara = MagicTxt.replaceAllIfPresent(replacedPara, "$system_name", new StringCreator() {
                    @Override
                    public String create() {
                        return finalActiveBounty.getFleetSpawnLocation().getContainingLocation().getNameWithNoType();
                    }
                });
                replacedPara = MagicTxt.replaceAllIfPresent(replacedPara, "$target", new StringCreator() {
                    @Override
                    public String create() {
                        return finalActiveBounty.getFleet().getFaction().getDisplayNameWithArticle();
                    }
                });
                replacedPara = MagicTxt.replaceAllIfPresent(replacedPara, "$reward", new StringCreator() {
                    @Override
                    public String create() {
                        return Misc.getDGSCredits(spec.job_credit_reward);
                    }
                });
                replacedPara = MagicTxt.replaceAllIfPresent(replacedPara, "$name", new StringCreator() {
                    @Override
                    public String create() {
                        return finalActiveBounty.getFleet().getCommander().getNameString();
                    }
                });
                replacedPara = MagicTxt.replaceAllIfPresent(replacedPara, "$constellation", new StringCreator() {
                    @Override
                    public String create() {
                        return finalActiveBounty.getFleetSpawnLocation().getContainingLocation().getConstellation().getName();
                    }
                });

                if (text instanceof TextPanelAPI) {
                    ((TextPanelAPI) text).addPara(replacedPara, color);
                } else if (text instanceof TooltipMakerAPI) {
                    ((TooltipMakerAPI) text).addPara(replacedPara, color, padding);
                }
            }
        }
    }

    enum Stage {
        NotAccepted,
        Accepted,
        Failed,
        Dismissed,
        ExpiredWithoutAccepting,
        EndedWithoutPlayerInvolvement,
        Succeeded
    }

    public interface BountyResult {
        class DismissedPermanently implements BountyResult {
        }

        class Succeeded implements BountyResult {
            public boolean shouldRewardCredits;

            public Succeeded(boolean shouldRewardCredits) {
                this.shouldRewardCredits = shouldRewardCredits;
            }
        }

        class EndedWithoutPlayerInvolvement implements BountyResult {
        }

        class FailedOutOfTime implements BountyResult {
        }

        class FailedSalvagedFlagship implements BountyResult {
        }

        class ExpiredWithoutAccepting implements BountyResult {
        }
    }
}
