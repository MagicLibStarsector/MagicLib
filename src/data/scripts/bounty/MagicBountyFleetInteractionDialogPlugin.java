package data.scripts.bounty;

import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;

/**
 * @deprecated Please replace `data.scripts` with `org.magiclib`.
 */
public class MagicBountyFleetInteractionDialogPlugin extends FleetInteractionDialogPluginImpl {
    public MagicBountyFleetInteractionDialogPlugin() {
        this(null);
    }

    public MagicBountyFleetInteractionDialogPlugin(FIDConfig params) {
        super(params);
        context = new MagicBountyFleetEncounterContext();
    }
}
