package data.scripts.bounty;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.MagicCampaign;
import data.scripts.util.MagicTxt;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static data.scripts.util.MagicTxt.nullStringIfEmpty;
import data.scripts.util.MagicVariables;

/**
 * Represents a bounty that has been at least viewed by the player. Can be considered an inflated/instantiated version of {@link MagicBountyData.bountyData}.
 *
 * @author Wisp
 */
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
     * The original size of the bounty fleet in FP, before any battles.
     */
    private int initialBountyFleetPoints;

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
    private @Nullable Float rewardReputation;
    private @Nullable String rewardFaction;
    private boolean isDespawning = false;
    private static final Logger LOG = Global.getLogger(ActiveBounty.class);

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

        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            this.initialBountyFleetPoints += member.getFleetPointCost();
        }
    }

    /**
     * Call when the player accepts a bounty.
     * <br>- Spawns the bounty fleet.
     * <br>- Adds Intel to the Intel Manager.
     *
     * @param bountySource  From where the bounty was accepted from.
     * @param rewardCredits The number of credits to give as a reward. Null or zero if no reward.
     */
    public void acceptBounty(@NotNull SectorEntityToken bountySource, @Nullable Float rewardCredits, @Nullable Float rewardReputation, @Nullable String rewardFaction) {
        this.rewardCredits = rewardCredits;
        this.rewardReputation = rewardReputation;
        this.rewardFaction = rewardFaction;
        acceptedBountyTimestamp = Global.getSector().getClock().getTimestamp();
        stage = Stage.Accepted;
        this.bountySource = bountySource;
        
        //CHECK IF THE FLEET EXIST
        if(getFleet().getCurrentAssignment()==null){
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

            //if needed set the bounty faction to neutral with everyone but the player.
            if(spec.fleet_faction.equals(MagicVariables.BOUNTY_FACTION)){
                FactionAPI bountyFaction = Global.getSector().getFaction(MagicVariables.BOUNTY_FACTION);
                for(FactionAPI f : Global.getSector().getAllFactions()){
                    if(f!=bountyFaction && f!=Global.getSector().getPlayerFaction())f.setRelationship(MagicVariables.BOUNTY_FACTION, RepLevel.NEUTRAL);
                }
            }
        }

        // Flag fleet as important so it has a target icon
        Misc.makeImportant(getFleet(), "magicbounty");
        // Add comm reply if needed
        if(MagicTxt.nullStringIfEmpty(spec.job_comm_reply) != null){
            getFleet().getMemoryWithoutUpdate().set("$MagicLib_Bounty_target_hasReply", true);
            getFleet().getMemoryWithoutUpdate().set("$MagicLib_Bounty_comm_reply", MagicBountyUtils.replaceStringVariables(this, spec.job_comm_reply));
        }
        
        getFleet().getMemoryWithoutUpdate().set("$MagicLib_Bounty_target_fleet", true);
        getFleet().getMemoryWithoutUpdate().set(spec.job_memKey, true);
        
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
     * <br> - Updates intel.
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
            //reputation reward
            if (
                    ((BountyResult.Succeeded) result).shouldRewardCredits
                            && hasReputationReward()
            ) {
                Global.getSector().getPlayerFaction().adjustRelationship(getRewardFaction(), getRewardReputation());
            }

            //set the relevant outcome memkey
            if (MagicTxt.nullStringIfEmpty(spec.job_memKey) != null) {
                Global.getSector().getMemoryWithoutUpdate().set(spec.job_memKey, true);
                Global.getSector().getMemoryWithoutUpdate().set(spec.job_memKey + "_succeeded", true);
            }
        } else if (result instanceof BountyResult.EndedWithoutPlayerInvolvement) {
            stage = Stage.EndedWithoutPlayerInvolvement;
            //set the relevant outcome memkey
            if (MagicTxt.nullStringIfEmpty(spec.job_memKey) != null) {
                Global.getSector().getMemoryWithoutUpdate().set(spec.job_memKey, true);
                Global.getSector().getMemoryWithoutUpdate().set(spec.job_memKey + "_expired", true);
            }
        } else if (result instanceof BountyResult.FailedOutOfTime) {
            stage = Stage.ExpiredAfterAccepting;
            //reputation penalty
            if (hasReputationReward()) {
                Global.getSector().getPlayerFaction().adjustRelationship(getRewardFaction(), -Math.min(0.05f, getRewardReputation()));
            }
            //set the relevant outcome memkey
            if (MagicTxt.nullStringIfEmpty(spec.job_memKey) != null) {
                Global.getSector().getMemoryWithoutUpdate().set(spec.job_memKey, true);
                Global.getSector().getMemoryWithoutUpdate().set(spec.job_memKey + "_expired", true);
            }
        } else if (result instanceof BountyResult.ExpiredWithoutAccepting) {
            stage = Stage.ExpiredWithoutAccepting;
        } else if (result instanceof BountyResult.DismissedPermanently) {
            stage = Stage.Dismissed;
            //set the relevant outcome memkey
            if (MagicTxt.nullStringIfEmpty(spec.job_memKey) != null) {
                Global.getSector().getMemoryWithoutUpdate().set(spec.job_memKey, true);
//                Global.getSector().getMemoryWithoutUpdate().set(spec.job_memKey+"_expired", true);
            }
        } else if (result instanceof BountyResult.FailedSalvagedFlagship) {
            stage = Stage.FailedSalvagedFlagship;
            //reputation penalty
            if (hasReputationReward()) {
                Global.getSector().getPlayerFaction().adjustRelationship(getRewardFaction(), -Math.max(0.05f, getRewardReputation()));
            }
            //set the relevant outcome memkey
            if (MagicTxt.nullStringIfEmpty(spec.job_memKey) != null) {
                Global.getSector().getMemoryWithoutUpdate().set(spec.job_memKey, true);
                Global.getSector().getMemoryWithoutUpdate().set(spec.job_memKey + "_failed", true);
            }
        }

        if (MagicTxt.nullStringIfEmpty(spec.job_conclusion_script) != null) {
            runRuleScript(spec.job_conclusion_script);
        }

        MagicBountyIntel intel = getIntel();

        if (intel != null) {
            intel.sendUpdateIfPlayerHasIntel(new Object(), false);

            despawn();
        }
    }

    void despawn() {
        if (!isDespawning) {
            LOG.info(String.format("Despawning bounty %s with stage %s", getKey(), getStage().name()));

            if (fleet != null) {
                fleet.getMemoryWithoutUpdate().unset(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE);
                fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE, true);
                fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_ALLOW_DISENGAGE, true);
                fleet.clearAssignments();

                if (getFleetSpawnLocation() != null) {
                    fleet.getAI().addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, getFleetSpawnLocation(), 1000000f, null);
                } else {
                    fleet.despawn();
                }
            }

            MagicBountyIntel intel = getIntel();

            if (intel != null) {
                if (!intel.isEnding() && !intel.isEnded()) {
                    intel.endAfterDelay();
                }
            }

            isDespawning = true;
        }
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

        FireBest.fire(null, dialog, dialog.getPlugin().getMemoryMap(), scriptRuleId);

        // Turn it on for FireBest, then set it back to whatever it was.
        DebugFlags.PRINT_RULES_DEBUG_INFO = flagSetting;

        if (didCreateDialog && Global.getSector().getCampaignUI().getCurrentInteractionDialog() != null) {
            Global.getSector().getCampaignUI().getCurrentInteractionDialog().dismiss();
        }
    }

    /**
     * @return Float.POSITIVE_INFINITY if there is no time limit or quest hasn't been accepted.
     */
    public @NotNull Float getDaysRemainingToComplete() {
        if (getSpec().job_deadline > 0 && acceptedBountyTimestamp != null) {
            return Math.max(0, getSpec().job_deadline - Global.getSector().getClock().getElapsedDaysSince(acceptedBountyTimestamp));
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

    public @Nullable Float getRewardReputation() {
        return rewardReputation;
    }

    public @Nullable String getRewardFaction() {
        return rewardFaction;
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

    public int getInitialBountyFleetPoints() {
        return initialBountyFleetPoints;
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
     * The faction targeted by the bounty, if relevant.
     */
    @Nullable
    public FactionAPI getTargetFaction() {
        FactionAPI target = null;
        if (getSpec().job_show_captain != false || getSpec().job_show_fleet != MagicBountyData.ShowFleet.None) {
            target = Global.getSector().getFaction(getSpec().fleet_faction);
        }
        return target;
    }

    /**
     * The color for the target faction, or Misc.getTextColor() if none.
     */
    @NotNull
    public Color getTargetFactionTextColor() {
        if (getTargetFaction() != null) {
            return getTargetFaction().getBaseUIColor();
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
        
        if(getSpec().job_credit_scaling<=0 || getSpec().fleet_min_FP<=0){
            float rewardRoundedToNearest100 = Math.round(getSpec().job_credit_reward / 100.0) * 100;
            LOG.info(
                    String.format(
                            "Base reward of %sc for bounty '%s'. No reward scaling defined or no target min FP.",
                            rewardRoundedToNearest100,
                            getKey()
                    )
            );
            return rewardRoundedToNearest100;
        }

        //Reward scaling is a mult applied to the size ratio between the player fleet and the min target fleet
        float playerFleetScale = MagicCampaign.PlayerFleetSizeMultiplier(getSpec().fleet_min_FP) - 1;
        // Math.max in case the scaling ends up negative, we don't want to subtract from the base reward.
        
        if(playerFleetScale>0){
            float bonusCreditsFromScaling = getSpec().job_credit_reward * getSpec().job_credit_scaling * playerFleetScale;
            float reward = Math.round(getSpec().job_credit_reward + bonusCreditsFromScaling);
            float rewardRoundedToNearest100 = Math.round(reward / 100.0) * 100;
            LOG.info(
                    String.format(
                            "Rounded reward of %sc for bounty '%s'. Base reward of %sc, scaled by %s (%s credit scaling, %s player fleet size mult)",
                            rewardRoundedToNearest100,
                            getKey(),
                            getSpec().job_credit_reward,
                            playerFleetScale+1,
                            getSpec().job_credit_scaling,
                            playerFleetScale
                    )
            );
            return rewardRoundedToNearest100;
        } else {
            float rewardRoundedToNearest100 = Math.round(getSpec().job_credit_reward / 100.0) * 100;
            LOG.info(
                    String.format(
                            "Base reward of %sc for bounty '%s'. No scaling due to the player fleet being %s times as large as the minimum target fleet.",
                            rewardRoundedToNearest100,
                            getKey(),
                            1+playerFleetScale
                    )
            );
            return rewardRoundedToNearest100;
        }
    }

    /**
     * The [MagicBountyIntel] active for this bounty, if there is any.
     * <br>There will only be intel if the bounty has been accepted (and isn't long past ended).
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

    public boolean hasReputationReward() {
        return getRewardReputation() != null
                && getRewardReputation() > 0
                && getRewardFaction() != null
                && !getRewardFaction().isEmpty()
                && Global.getSector().getFaction(getRewardFaction()) != null;
    }

    /**
     * Whether the bounty expires or not.
     */
    public boolean hasExpiration() {
        return getDaysRemainingToComplete() != Float.POSITIVE_INFINITY;
    }

    public boolean isDespawning() {
        return isDespawning;
    }

    public @NotNull List<String> getPresetShipIds() {
        return presetShipIds;
    }

    public List<FleetMemberAPI> getPresetShipsInFleet() {
        List<FleetMemberAPI> ships = getFleet().getFleetData().getMembersInPriorityOrder();

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
            String replacedString = MagicBountyUtils.replaceStringVariables(this, spec.job_description);
            String[] replacedParas = replacedString.split("/n|\\n");

            for (String replacedPara : replacedParas) {
                if (text instanceof TextPanelAPI) {
                    MagicTxt.addPara(((TextPanelAPI) text), replacedPara, color, Misc.getHighlightColor());
                } else if (text instanceof TooltipMakerAPI) {
                    MagicTxt.addPara(((TooltipMakerAPI) text), replacedPara, 10f, color, Misc.getHighlightColor());
                }
            }
        }
    }

    /**
     * The current stage of the bounty.
     */
    public enum Stage {
        /**
         * Not yet accepted.
         */
        NotAccepted,
        /**
         * Player has accepted the bounty but not done anything else yet.
         */
        Accepted,
        /**
         * The player failed the bounty because they salvaged the flagship and weren't allowed to.
         * Note: A few bits of code use this ordinal as the threshold for whether the bounty is complete or not;
         * don't add a new Stage above this if it isn't some variation of "completed/failed".
         */
        FailedSalvagedFlagship,
        /**
         * The bounty expired because the player accepted it but didn't complete it in time.
         */
        ExpiredAfterAccepting,
        /**
         * The player dismissed the bounty permanently (and they never accepted it).
         */
        Dismissed,
        /**
         * The bounty expired and the player never accepted it. It will be regenerated.
         */
        ExpiredWithoutAccepting,
        /**
         * The bounty ended, probably due to another fleet destroying the bounty fleet.
         */
        EndedWithoutPlayerInvolvement,
        /**
         * The player successfully completed the bounty!
         */
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ActiveBounty{");
        sb.append("bountyKey='").append(bountyKey).append('\'');
        sb.append(", fleet=").append(fleet);
        sb.append(", fleetSpawnLocation=").append(fleetSpawnLocation);
        sb.append(", presetShipIds=").append(presetShipIds);
        sb.append(", spec=").append(spec);
        sb.append(", bountyCreatedTimestamp=").append(bountyCreatedTimestamp);
        sb.append(", captain=").append(captain);
        sb.append(", flagshipId='").append(flagshipId).append('\'');
        sb.append(", initialBountyFleetPoints=").append(initialBountyFleetPoints);
        sb.append(", acceptedBountyTimestamp=").append(acceptedBountyTimestamp);
        sb.append(", bountyResult=").append(bountyResult);
        sb.append(", bountySource=").append(bountySource);
        sb.append(", stage=").append(stage);
        sb.append(", rewardCredits=").append(rewardCredits);
        sb.append(", rewardReputation=").append(rewardReputation);
        sb.append(", rewardFaction='").append(rewardFaction).append('\'');
        sb.append(", isDespawning=").append(isDespawning);
        sb.append('}');
        return sb.toString();
    }
}
