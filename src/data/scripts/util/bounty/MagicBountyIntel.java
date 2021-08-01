package data.scripts.util.bounty;

import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.MagicDeserializable;
import org.jetbrains.annotations.NotNull;

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
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        super.createIntelInfo(info, mode);

        info.addPara(bounty.getSpec().job_name, 0f);

        boolean hasCreditReward = bounty.getRewardCredits() != null && bounty.getRewardCredits() > 0;
        boolean hasExpiration = bounty.getDaysRemainingToComplete() != Float.POSITIVE_INFINITY;

        if (hasCreditReward && hasExpiration) {
            bullet(info);
            info.addPara("%s reward, %s days remaining",
                    2f,
                    Misc.getGrayColor(),
                    Misc.getHighlightColor(),
                    Misc.getDGSCredits(bounty.getRewardCredits()),
                    Integer.toString(Math.round(bounty.getDaysRemainingToComplete())));
        } else if (hasCreditReward) {
            bullet(info);
            info.addPara("%s reward",
                    2f,
                    Misc.getGrayColor(),
                    Misc.getHighlightColor(),
                    Misc.getDGSCredits(bounty.getRewardCredits()));
        } else if (hasExpiration) {
            bullet(info);
            info.addPara("%s days remaining",
                    2f,
                    Misc.getGrayColor(),
                    Misc.getHighlightColor(),
                    Integer.toString(Math.round(bounty.getDaysRemainingToComplete())));
        }
    }

    @Override
    public String getSmallDescriptionTitle() {
        return bounty.getSpec().job_name;
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        if (bounty.getFleet().getCommander() != null && bounty.getFleet().getCommander().getPortraitSprite() != null) {
            info.addImage(bounty.getFleet().getCommander().getPortraitSprite(), width, 128f, PADDING_DESC);
        }

        bounty.addDescriptionToTextPanel(info, PADDING_DESC);
    }

    @Override
    public List<ArrowData> getArrowData(SectorMapAPI map) {
        if (!bounty.getSpec().job_show_arrow) {
            return null;
        }

        ArrowData arrowData = new ArrowData(bounty.getStartingEntity(), bounty.getFleet());
        arrowData.color = bounty.getTextColor();
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
