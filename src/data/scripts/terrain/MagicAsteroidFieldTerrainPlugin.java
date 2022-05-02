package data.scripts.terrain;

import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AsteroidAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.terrain.AsteroidFieldTerrainPlugin.AsteroidFieldParams;
import com.fs.starfarer.api.util.Misc;
import static data.scripts.util.MagicTxt.getString;

public class MagicAsteroidFieldTerrainPlugin extends MagicAsteroidBeltTerrainPlugin {

    public AsteroidFieldParams fieldParams;

    @Override
    public void init(String terrainId, SectorEntityToken entity, Object param) {
        super.init(terrainId, entity, param);
        fieldParams = (AsteroidFieldParams) param;
        name = fieldParams.name;
        if (name == null) {
            //"af_fieldName" : "Asteroid Field",
            name = getString("af_fieldName");
        }
        fieldParams.numAsteroids = fieldParams.minAsteroids;
        if (fieldParams.maxAsteroids > fieldParams.minAsteroids) {
            fieldParams.numAsteroids += new Random().nextInt(fieldParams.maxAsteroids - fieldParams.minAsteroids);
        }
    }

    private final transient SpriteAPI icon = null;

    @Override
    public void renderOnMap(float factor, float alphaMult) {
    }

    @Override
    public void regenerateAsteroids() {
        createAsteroidField();
    }

    protected void createAsteroidField() {
        if (!(fieldParams instanceof AsteroidFieldParams)) {
            return;
        }

        Random rand = new Random(Global.getSector().getClock().getTimestamp() + entity.getId().hashCode());

        float fieldRadius = fieldParams.minRadius + (fieldParams.maxRadius - fieldParams.minRadius) * rand.nextFloat();
        fieldParams.bandWidthInEngine = fieldRadius;
        fieldParams.middleRadius = fieldRadius / 2f;

        LocationAPI location = entity.getContainingLocation();
        if (location == null) {
            return;
        }
        for (int i = 0; i < fieldParams.numAsteroids; i++) {
            float size = fieldParams.minSize + (fieldParams.maxSize - fieldParams.minSize) * rand.nextFloat();
            AsteroidAPI asteroid = location.addAsteroid(size);
            asteroid.setFacing(rand.nextFloat() * 360f);

            float r = rand.nextFloat();
            r = 1f - r * r;

            float currRadius = fieldRadius * r;

            float minOrbitDays = Math.max(1f, currRadius * 0.05f);
            float maxOrbitDays = Math.max(2f, currRadius * 2f * 0.05f);
            float orbitDays = minOrbitDays + rand.nextFloat() * (maxOrbitDays - minOrbitDays);

            float angle = rand.nextFloat() * 360f;
            asteroid.setCircularOrbit(this.entity, angle, currRadius, orbitDays);
            Misc.setAsteroidSource(asteroid, this);
        }
        needToCreateAsteroids = false;
    }

    @Override
    public void advance(float amount) {
        if (needToCreateAsteroids) {
            createAsteroidField();
        }
        super.advance(amount);
    }

    @Override
    public String getNameForTooltip() {
        //"af_fieldName" : "Asteroid Field",
        return getString("af_fieldName");
    }

    @Override
    public void reportAsteroidPersisted(SectorEntityToken asteroid) {
        if (Misc.getAsteroidSource(asteroid) == this) {
            fieldParams.numAsteroids--;
        }
    }
}
