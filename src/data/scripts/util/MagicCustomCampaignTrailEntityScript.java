//By Nicke535
//Special entity script which runs "behind the scenes" to make the campaign trail plugin work
package data.scripts.util;

import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;
import data.scripts.plugins.MagicCampaignTrailPlugin;

@Deprecated
public class MagicCustomCampaignTrailEntityScript extends BaseCustomEntityPlugin {

    //Which plugin does this script belong to?
    private MagicCampaignTrailPlugin associatedPlugin = null;

    //Render at any range, since we don't move the object around
    @Override
    public float getRenderRange() {
        return 9999999999999999999999999f;
    }

    //Initializer; the params field is expected to be a MagicCampaignTrailPlugin
    @Override
    public void init(SectorEntityToken entity, Object params) {
        if (params instanceof MagicCampaignTrailPlugin) {
            associatedPlugin = (MagicCampaignTrailPlugin) params;
        }
    }

    //Call the render function of our associated plugin; this is the only reason we spawned this darn entity
    @Override
    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        if (associatedPlugin != null) {
            associatedPlugin.render(layer, viewport);
        }
    }
}
