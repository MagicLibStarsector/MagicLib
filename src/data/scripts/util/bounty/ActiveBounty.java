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
    @NotNull
    private final String bountyKey;
    /**
     * The bounty fleet. The thing to kill.
     * <p>
     * Lifecycle:
     * Created when an ActiveBounty is created, which happens when the player *views a bounty on the bounty board*,
     * regardless of whether they accept it.
     * Destroyed by player/another fleet, or
     * Auto-destroyed when...never? If the bounty was shown, it means requirements were met,
     * and it can always be shown again on the same board. No need to despawn the fleet, future bar events will just
     * reuse the same ActiveBounty with the same fleet.
     */
    @NotNull
    private final CampaignFleetAPI fleet;
    @NotNull
    private final SectorEntityToken fleetLocation;
    @NotNull
    private final MagicBountyData.bountyData spec;

    @Nullable
    private Long acceptedBountyTimestamp;
    @Nullable BountyResult bountyResult;
    /**
     * The planet/station/etc from where the bounty was accepted.
     */
    @Nullable
    private SectorEntityToken startingEntity;

    @NotNull
    private Stage stage = Stage.NotAccepted;
    /**
     * The number of credits that was promised as a reward upon completion. Includes scaling, if applicable.
     */
    @Nullable
    private Integer rewardCredits;

    /**
     * @param bountyKey     A unique key for the bounty.
     * @param fleet         The fleet that, when destroyed, completes the bounty. Should have no location to start with.
     *                      The location will be set when the bounty is accepted.
     * @param fleetLocation The location to spawn the fleet when the bounty is accepted.
     * @param spec          The original bounty spec, a mirror of the json definition.
     */
    public ActiveBounty(@NotNull String bountyKey, @NotNull CampaignFleetAPI fleet, @NotNull SectorEntityToken fleetLocation, @NotNull MagicBountyData.bountyData spec) {
        this.bountyKey = bountyKey;
        this.fleet = fleet;
        this.fleetLocation = fleetLocation;
        this.spec = spec;
    }

    public void acceptBounty(SectorEntityToken startingEntity, Integer rewardCredits) {
        this.rewardCredits = rewardCredits;
        acceptedBountyTimestamp = Global.getSector().getClock().getTimestamp();
        stage = Stage.Accepted;
        this.startingEntity = startingEntity;


        LocationAPI systemLocation = fleetLocation.getContainingLocation();
        systemLocation.addEntity(getFleet());
        getFleet().setLocation(fleetLocation.getLocation().x, fleetLocation.getLocation().y);
        getFleet().getAI().addAssignment(
                getSpec().fleet_behavior == null
                        ? FleetAssignment.ORBIT_AGGRESSIVE
                        : getSpec().fleet_behavior,
                fleetLocation,
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

    public @Nullable SectorEntityToken getStartingEntity() {
        return startingEntity;
    }

    public @NotNull Stage getStage() {
        return stage;
    }

    /**
     * Calculates and returns the number of credits that will be awarded upon completion, if any.
     * Includes any scaling factor.
     */
    @Nullable
    public Integer calculateCreditReward() {
        return spec.job_credit_reward > 0
                ? spec.job_credit_reward // TODO scaling
                : null;
    }

    @Nullable
    public Integer getRewardCredits() {
        return rewardCredits;
    }

    @NotNull
    public SectorEntityToken getFleetLocation() {
        return fleetLocation;
    }

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

    public void addDescriptionToTextPanel(TextPanelAPI text) {
        addDescriptionToTextPanelInternal(text, 0f);
    }

    public void addDescriptionToTextPanel(TooltipMakerAPI text, float padding) {
        addDescriptionToTextPanelInternal(text, padding);
    }

    public boolean hasCreditReward() {
        return getRewardCredits() != null && getRewardCredits() > 0;
    }

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
                        return finalActiveBounty.getFleetLocation().getContainingLocation().getNameWithNoType();
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
                        return finalActiveBounty.getFleetLocation().getContainingLocation().getConstellation().getName();
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
    }
}
