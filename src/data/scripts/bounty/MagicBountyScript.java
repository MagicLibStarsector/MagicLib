package data.scripts.bounty;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;

/**
 * This EveryFrameScript will
 */
public final class MagicBountyScript implements EveryFrameScript {
    private boolean isDone = false;

    /**
     * How long to wait before checking again. No need to do on every frame.
     */
    private static final Float PERIOD_IN_MILLIS = 100f;
    private transient long lastCheckTimestamp = 0;

    /**
     * Use SectorAPI.getClock() to convert to campaign days.
     *
     * @param amount seconds elapsed during the last frame.
     */
    @Override
    public void advance(float amount) {
        if (Global.getSector().getClock().getTimestamp() - lastCheckTimestamp < PERIOD_IN_MILLIS) {
            lastCheckTimestamp = Global.getSector().getClock().getTimestamp();
        } else {
//            Collection<ActiveBounty> activeBounties = MagicBountyCoordinator.getInstance().getActiveBounties().values();
//
//            for (ActiveBounty bounty : activeBounties) {
//                switch (bounty.getStage()) {
//                    case NotAccepted:
//                        break;
//                    case Accepted:
//                        break;
//                    case Failed:
//                        break;
//                    case Succeeded:
//                        break;
//                }
//            }
        }
    }

    /**
     * @return true when the script is finished and can be cleaned up by the engine.
     */
    @Override
    public boolean isDone() {
        return isDone;
    }

    /**
     * @return whether advance() should be called while the campaign engine is paused.
     */
    @Override
    public boolean runWhilePaused() {
        return false;
    }
}
