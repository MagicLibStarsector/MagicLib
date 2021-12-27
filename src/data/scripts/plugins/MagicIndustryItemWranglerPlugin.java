package data.scripts.plugins;

import data.scripts.util.MagicIndustryItemWrangler;
import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;

public class MagicIndustryItemWranglerPlugin extends BaseModPlugin {
	
	@Override
	public void onGameLoad( boolean newGame ) {
		SectorAPI sector = Global.getSectorAPI();
		if( sector != null ) {
			sector.addTransientListener( new MagicIndustryItemWrangler() );
		}
	}
}




