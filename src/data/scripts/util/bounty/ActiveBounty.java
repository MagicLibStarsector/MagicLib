package data.scripts.util.bounty;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.plugins.MagicBountyData;
import data.scripts.util.MagicTxt;
import data.scripts.util.StringCreator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;

import static data.scripts.util.MagicTxt.nullStringIfEmpty;

public final class ActiveBounty {
    /**
     * A unique key for the bounty, as used by [MagicBountyCoordinator].
     */
    @NotNull
    private final String bountyKey;
    /**
     * The bounty fleet. The thing to kill.
     * Created without a location initially. A location (from `fleetLocation`) when the bounty is accepted.
     */
    @NotNull
    private final CampaignFleetAPI fleet;
    /**
     * The spawn location of the fleet.
     */
    @NotNull
    private final SectorEntityToken fleetSpawnLocation;
    /**
     * The original bounty spec, a mirror of the json definition.
     */
    @NotNull
    private final MagicBountyData.bountyData spec;

    /**
     * The timestamp of when the bounty was first created (not accepted).
     **/
    @NotNull
    private final Long bountyCreatedTimestamp;

    /**
     * The timestamp of when the player accepted the bounty, if they have done so.
     **/
    @Nullable
    private Long acceptedBountyTimestamp;
    /**
     * The result of the bounty, if there has been a terminus.
     */
    @Nullable BountyResult bountyResult;

    /**
     * The planet/station/etc from where the bounty was accepted.
     */
    @Nullable
    private SectorEntityToken bountySource;

    @NotNull
    private Stage stage = Stage.NotAccepted;

    /**
     * The number of credits that was promised as a reward upon completion. Includes scaling, if applicable.
     */
    @Nullable
    private Float rewardCredits;

    /**
     * @param bountyKey          A unique key for the bounty, as used by [MagicBountyCoordinator].
     * @param fleet              The fleet that, when destroyed, completes the bounty. Should have no location to start with.
     *                           The fleet's location will be set when the bounty is accepted (from fleetSpawnLocation).
     * @param fleetSpawnLocation The location to spawn the fleet when the bounty is accepted.
     * @param spec               The original bounty spec, a mirror of the json definition.
     */
    public ActiveBounty(@NotNull String bountyKey, @NotNull CampaignFleetAPI fleet, @NotNull SectorEntityToken fleetSpawnLocation, @NotNull MagicBountyData.bountyData spec) {
        this.bountyKey = bountyKey;
        this.fleet = fleet;
        this.fleetSpawnLocation = fleetSpawnLocation;
        this.spec = spec;
        this.bountyCreatedTimestamp = Global.getSector().getClock().getTimestamp();
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
        getFleet().getMemoryWithoutUpdate().set(MemFlags.ENTITY_MISSION_IMPORTANT, true);

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

        switch (result) {
            case Succeeded:
                stage = Stage.Succeeded;

                if (getRewardCredits() != null) {
                    Global.getSector().getPlayerFleet().getCargo().getCredits().add(getRewardCredits());
                }

                MagicBountyIntel intel = getIntel();
                if (intel != null) {
                    intel.sendUpdateIfPlayerHasIntel(new Object(), false);
                }
                break;
            case EndedWithoutPlayerInvolvement:
                stage = Stage.EndedWithoutPlayerInvolvement;
                break;
            case FailedOutOfTime:
                stage = Stage.Failed;
                break;
            case ExpiredWithoutAccepting:
                break;
        }

        destroy();
    }

    private void destroy() {
        if (fleet != null && !fleet.isDespawning()) {
            fleet.despawn();
        }
    }

    /**
     * @return Float.POSITIVE_INFINITY if there is no time limit or quest hasn't been accepted.
     */
    @NotNull
    public Float getDaysRemainingToComplete() {
        if (getSpec().job_deadline > 0 && acceptedBountyTimestamp != null) {
            return Math.max(1, getSpec().job_deadline - Global.getSector().getClock().getElapsedDaysSince(acceptedBountyTimestamp));
        } else {
            return Float.POSITIVE_INFINITY;
        }
    }

    @NotNull
    public String getKey() {
        return bountyKey;
    }

    @NotNull
    public CampaignFleetAPI getFleet() {
        return fleet;
    }

    @NotNull
    public MagicBountyData.bountyData getSpec() {
        return spec;
    }

    public @Nullable SectorEntityToken getBountySource() {
        return bountySource;
    }

    public @NotNull Stage getStage() {
        return stage;
    }

    @Nullable
    public Float getRewardCredits() {
        return rewardCredits;
    }

    @NotNull
    public SectorEntityToken getFleetSpawnLocation() {
        return fleetSpawnLocation;
    }

    public @NotNull Long getBountyCreatedTimestamp() {
        return bountyCreatedTimestamp;
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
        float rewardRoundedToNearest100 = Math.round(reward/100.0) * 100;
        Global.getLogger(ActiveBounty.class).info(String.format("Rounded reward of %sc for bounty '%s' has base %sc and scaled bonus of %sc (%s scaling * %s FP diff)",
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
        addDescriptionToTextPanelInternal(text, 0f);
    }

    /**
     * Adds the description for the bounty to a [TooltipMakerAPI].
     *
     * @param text    The [TooltipMakerAPI] to write to.
     * @param padding The amount of padding to use, in pixels.
     */
    public void addDescriptionToTextPanel(TooltipMakerAPI text, float padding) {
        addDescriptionToTextPanelInternal(text, padding);
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

    private void addDescriptionToTextPanelInternal(Object text, float padding) {
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
                    ((TextPanelAPI) text).addPara(replacedPara);
                } else if (text instanceof TooltipMakerAPI) {
                    ((TooltipMakerAPI) text).addPara(replacedPara, padding);
                }
            }
        }
    }

    enum Stage {
        NotAccepted,
        Accepted,
        Failed,
        EndedWithoutPlayerInvolvement,
        Succeeded
    }

    public enum BountyResult {
        Succeeded,
        EndedWithoutPlayerInvolvement,
        FailedOutOfTime,
        ExpiredWithoutAccepting
    }
}
