package data.scripts.util;

import com.fs.starfarer.api.impl.campaign.intel.bar.PortsideBarEvent;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventCreator;

public class MagicBountyBarEventCreator extends BaseBarEventCreator {
    @Override
    public PortsideBarEvent createBarEvent() {
        return new MagicBountyBarEvent();
    }

    @Override
    public boolean isPriority() {
        return true;
    }
}
