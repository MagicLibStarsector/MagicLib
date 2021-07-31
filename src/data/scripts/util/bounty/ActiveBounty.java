package data.scripts.util.bounty;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import data.scripts.plugins.MagicBountyData;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

class ActiveBounty {
    @NotNull
    private final String key;
    @NotNull
    private final CampaignFleetAPI fleet;
    @NotNull
    private final MagicBountyData.bountyData spec;

    public ActiveBounty(@NotNull String key, @NotNull CampaignFleetAPI fleet, @NotNull MagicBountyData.bountyData spec) {
        this.key = key;
        this.fleet = fleet;
        this.spec = spec;
    }

    @NotNull
    public String getKey() {
        return key;
    }

//    public void setKey(@NotNull String key) {
//        this.key = key;
//    }

    @NotNull
    public CampaignFleetAPI getFleet() {
        return fleet;
    }

//    public void setFleet(@NotNull CampaignFleetAPI fleet) {
//        this.fleet = Objects.requireNonNull(fleet);
//    }

    @NotNull
    public MagicBountyData.bountyData getSpec() {
        return spec;
    }

//    @NotNull
//    public void setSpec(MagicBountyData.bountyData spec) {
//        this.spec = spec;
//    }
}
