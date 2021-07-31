package data.scripts.util.bounty;

import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import data.scripts.util.MagicDeserializable;

public class MagicBountyIntel extends BaseIntelPlugin implements MagicDeserializable {
    private transient ActiveBounty bounty;

    public Object readResolve() {

        return this;
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        super.createIntelInfo(info, mode);

        info.addPara(bounty.getSpec().job_name, 0f);

    }
}
