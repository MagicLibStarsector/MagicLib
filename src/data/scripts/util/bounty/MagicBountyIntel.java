package data.scripts.util.bounty;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.MagicDeserializable;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class MagicBountyIntel extends BaseIntelPlugin implements MagicDeserializable {
    @NotNull
    public final String bountyKey;
    @NotNull
    private final transient ActiveBounty bounty;

    private static final Float PADDING_DESC = 10f;
    private static final Float PADDING_INFO_SUBTITLE = 3f;

    public MagicBountyIntel(@NotNull String bountyKey) {
        this.bounty = MagicBountyCoordinator.getActiveBounty(bountyKey);
        this.bountyKey = bountyKey;

        if (bounty == null) {
            throw new NullPointerException("Expected an active bounty for key " + bountyKey);
        }
    }

    public Object readResolve() {

        return this;
    }

    @Override
    public String getIcon() {
        if (bounty.getFleet().getCommander() != null && bounty.getFleet().getCommander().getPortraitSprite() != null) {
            return bounty.getFleet().getCommander().getPortraitSprite();
        } else {
            return null; // TODO special bounty board icon?
        }
    }

    @Override
    protected String getName() {
        switch (bounty.getStage()) {
            case Succeeded:
                return "Bounty Completed - " + bounty.getSpec().job_name;
            case Failed:
                return "Bounty Failed - " + bounty.getSpec().job_name;
            case Accepted:
            case NotAccepted:
            default:
                return "Bounty Board - " + bounty.getSpec().job_name;
        }
    }

    @Override
    public Color getTitleColor(ListInfoMode mode) {
        switch (bounty.getStage()) {
            case Accepted:
            case NotAccepted:
                return super.getTitleColor(mode);
            case Succeeded:
            case Failed:
            default:
                return Misc.getGrayColor();
        }
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        super.createIntelInfo(info, mode);

        switch (bounty.getStage()) {
            case Succeeded:
                if (bounty.hasCreditReward()) {
                    bullet(info);
                    info.addPara("%s received",
                            PADDING_INFO_SUBTITLE,
                            Misc.getGrayColor(),
                            Misc.getHighlightColor(),
                            Misc.getDGSCredits(bounty.getRewardCredits()));
                    unindent(info);
                }
                break;
            case Failed:
                break;
            case Accepted:
            case NotAccepted:
            default:
                if (bounty.getGivingFaction() != null) {
                    bullet(info);
                    info.addPara("Faction: %s",
                            PADDING_INFO_SUBTITLE,
                            Misc.getGrayColor(),
                            bounty.getGivingFactionTextColor(),
                            bounty.getGivingFaction().getDisplayName());
                    unindent(info);
                }

                if (bounty.hasCreditReward() && bounty.hasExpiration()) {
                    bullet(info);
                    info.addPara("%s reward, %s days remaining",
                            PADDING_INFO_SUBTITLE,
                            Misc.getGrayColor(),
                            Misc.getHighlightColor(),
                            Misc.getDGSCredits(bounty.getRewardCredits()),
                            Integer.toString(Math.round(bounty.getDaysRemainingToComplete())));
                    unindent(info);
                } else if (bounty.hasCreditReward()) {
                    bullet(info);
                    info.addPara("%s reward",
                            PADDING_INFO_SUBTITLE,
                            Misc.getGrayColor(),
                            Misc.getHighlightColor(),
                            Misc.getDGSCredits(bounty.getRewardCredits()));
                    unindent(info);
                } else if (bounty.hasExpiration()) {
                    bullet(info);
                    info.addPara("%s days remaining",
                            PADDING_INFO_SUBTITLE,
                            Misc.getGrayColor(),
                            Misc.getHighlightColor(),
                            Integer.toString(Math.round(bounty.getDaysRemainingToComplete())));
                    unindent(info);
                }
                break;
        }
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        if (bounty.getFleet().getCommander() != null && bounty.getFleet().getCommander().getPortraitSprite() != null) {
            info.addImage(bounty.getFleet().getCommander().getPortraitSprite(), width, 128f, PADDING_DESC);
        }

        bounty.addDescriptionToTextPanel(info, PADDING_DESC);

        switch (bounty.getStage()) {
            case Succeeded:
                info.addPara(bounty.getSpec().job_conclusion_script, PADDING_DESC);

                if (bounty.hasCreditReward()) {
                    if (bounty.hasCreditReward()) {
                        bullet(info);
                        info.addPara("%s received",
                                PADDING_DESC,
                                Misc.getTextColor(),
                                Misc.getHighlightColor(),
                                Misc.getDGSCredits(bounty.getRewardCredits()));
                        unindent(info);
                    }
                }
                break;
            case Failed:
                break;
            case Accepted:
            case NotAccepted:
            default:
                if (bounty.hasCreditReward()) {
                    bullet(info);
                    info.addPara("%s reward",
                            PADDING_DESC,
                            Misc.getTextColor(),
                            Misc.getHighlightColor(),
                            Misc.getDGSCredits(bounty.getRewardCredits()));
                    unindent(info);
                }

                if (bounty.hasExpiration()) {
                    bullet(info);
                    addDays(info, "remaining", Math.round(bounty.getDaysRemainingToComplete()), Misc.getTextColor());
                    unindent(info);
                }

                if (bounty.getSpec().job_show_fleet) {
                    info.addPara("Fleet information is attached to the posting.", PADDING_DESC);
                    int columns = 7;
                    info.addShipList(columns, (int) Math.round(Math.ceil((double) bounty.getFleet().getNumMembersFast() / columns)), (width - 10) / columns,
                            bounty.getFleet().getFaction().getBaseUIColor(),
                            bounty.getFleet().getMembersWithFightersCopy(), 10f);
                }
                break;
        }
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        return bounty.getFleet().isInHyperspace() ? bounty.getFleet() : bounty.getFleet().getStarSystem().getCenter();
    }

    @Override
    public List<ArrowData> getArrowData(SectorMapAPI map) {
        if (!bounty.getSpec().job_show_arrow) {
            return null;
        }

        ArrowData arrowData = new ArrowData(bounty.getStartingEntity(), bounty.getFleet());
        arrowData.color = bounty.getGivingFactionTextColor();
        return Collections.singletonList(arrowData);
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        Collections.addAll(tags, Tags.INTEL_BOUNTY, Tags.INTEL_ACCEPTED);

        if (bounty.getGivingFaction() != null) {
            tags.add(bounty.getGivingFaction().getDisplayName());
        }

        return tags;
    }
}
