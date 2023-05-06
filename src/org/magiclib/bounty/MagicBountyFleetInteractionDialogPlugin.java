package org.magiclib.bounty;

import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;

public class MagicBountyFleetInteractionDialogPlugin extends FleetInteractionDialogPluginImpl {
    public MagicBountyFleetInteractionDialogPlugin() {
        this(null);
    }

    public MagicBountyFleetInteractionDialogPlugin(FIDConfig params) {
        super(params);
        context = new MagicBountyFleetEncounterContext();
    }
}
