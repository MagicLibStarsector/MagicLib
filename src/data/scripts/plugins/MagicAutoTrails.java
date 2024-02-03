package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.ProjectileSpawnType;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.util.MagicSettings;
import data.scripts.util.MagicVariables;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.*;

import static org.lwjgl.opengl.GL11.*;

@Deprecated
public class MagicAutoTrails extends BaseEveryFrameCombatPlugin {

    private static final Logger LOG = Global.getLogger(MagicAutoTrails.class);
    //Each proj can have multiple trails
    private static final Map<String, List<trailData>> PROJ_TRAILS = new HashMap<>();

    //A map for known projectiles and their IDs: should be cleared in init
    private final Map<DamagingProjectileAPI, List<Float>> slowProjTrailIDs = new WeakHashMap<>();
    private final Map<DamagingProjectileAPI, List<Float>> fastProjTrailIDs = new WeakHashMap<>();
    //A map to check the minimal length of everyframe projectiles and avoid issues with time warps
    private final Map<DamagingProjectileAPI, Vector2f> fastProjLoc = new WeakHashMap<>();

    //timer for slow projectiles
    private final IntervalUtil timer = new IntervalUtil(0.06f, 0.07f);

//    private CombatEngineAPI engine;

    @Override
    public void init(CombatEngineAPI engine) {
        //Reinitialize the lists
        fastProjTrailIDs.clear();
        fastProjLoc.clear();
        slowProjTrailIDs.clear();
        //reload the trails all the time in dev mode for easy testing :D
        if (Global.getSettings().isDevMode()) {
            getTrailData();
        }
//        this.engine = engine;
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {

        CombatEngineAPI engine = Global.getCombatEngine();
        if (
//                engine == null || 
                engine.isPaused()) {
            return;
        }

        //Runs once on each projectile that matches one of the IDs specified in our maps
        for (DamagingProjectileAPI proj : engine.getProjectiles()) {

            //Ignore already-collided projectiles, and projectiles that don't match our IDs
            if (proj.getProjectileSpecId() == null || proj.didDamage()) {
                continue;
            }

            //check if that type of proj has any trail
            if (!PROJ_TRAILS.containsKey(proj.getProjectileSpecId())) {
                continue;
            }

            Vector2f projVel = new Vector2f(proj.getVelocity());
            String specID = proj.getProjectileSpecId();

            //add segments to the projs that already have a trail:

            //everyframe projectiles:
            List<trailData> trailDataList = PROJ_TRAILS.get(specID);

            if (fastProjLoc.containsKey(proj)) {


                //check for min segment length
                if (!MathUtils.isWithinRange(proj.getLocation(), fastProjLoc.get(proj), trailDataList.get(0).minLength)) {
                    //put new location and draw a new segment
                    fastProjLoc.put(proj, new Vector2f(proj.getLocation()));
                    createTrailSegment(proj, specID, fastProjTrailIDs.get(proj), projVel);
                }
                continue;
            }

            //slow projectiles:
            timer.advance(amount);
            if (slowProjTrailIDs.containsKey(proj)) {
                if (timer.intervalElapsed()) {
                    createTrailSegment(proj, specID, slowProjTrailIDs.get(proj), projVel);
                }
                continue;
            }

            //apparently this projectile doesn't have a trail, let's fix that!

            //check if it is a fast or slow proj
            List<Float> specs = new ArrayList<>();
            if (trailDataList.get(0).minLength < 0) {
                //SLOW MODE
                slowProjTrailIDs.put(proj, specs);
            } else {
                //EVERYFRAME MODE
                fastProjTrailIDs.put(proj, specs);
                //store position for minimal length check
                fastProjLoc.put(proj, new Vector2f(proj.getLocation()));
            }

            //add all the necessary trails to that proj
            for (int i = 0; i < trailDataList.size(); i++) {
                specs.add(MagicTrailPlugin.getUniqueID());
            }

            //Fix for some first-frame error shenanigans
            if (projVel.length() < 0.1f && proj.getSource() != null) {
                projVel = new Vector2f(proj.getSource().getVelocity());
            }

            //add initial segment
            createTrailSegment(proj, specID, specs, projVel);
        }
    }

    private static void createTrailSegment(DamagingProjectileAPI proj, String specID, List<Float> trailIDs, Vector2f projVel) {

        List<trailData> trailDataList = PROJ_TRAILS.get(specID);

        for (int i = 0; i < trailIDs.size(); i++) {
            trailData trailData = trailDataList.get(i);

            SpriteAPI spriteToUse = Global.getSettings().getSprite("fx", trailData.sprite);

            //If we use angle adjustment, do that here
            if (trailData.angleAdjustment && projVel.length() > 0.1f && !proj.getSpawnType().equals(ProjectileSpawnType.BALLISTIC_AS_BEAM)) {
                proj.setFacing(VectorUtils.getFacing(projVel));
            }

            //Gets a custom "offset" position, so we can slightly alter the spawn location to account for "natural fade-in", and add that to our spawn position
//            Vector2f offsetPoint = new Vector2f((float) Math.cos(Math.toRadians(proj.getFacing())) * trailData.distance, (float) Math.sin(Math.toRadians(proj.getFacing())) * trailData.distance);
//            Vector2f spawnPosition = new Vector2f(offsetPoint.x + proj.getLocation().x, offsetPoint.y + proj.getLocation().y);
            Vector2f spawnPosition = MathUtils.getPointOnCircumference(
                    proj.getLocation(), trailData.distance, proj.getFacing());

            //Sideway offset velocity, for projectiles that use it
            Vector2f projBodyVel = new Vector2f(projVel);
            projBodyVel = VectorUtils.rotate(projBodyVel, -proj.getFacing());
            Vector2f projLateralBodyVel = new Vector2f(0f, projBodyVel.getY());
            Vector2f sidewayVel = new Vector2f(projLateralBodyVel);
            sidewayVel = (Vector2f) VectorUtils.rotate(sidewayVel, proj.getFacing()).scale(trailData.drift);

            //random dispersion of the segments if necessary
            float rotationIn = trailData.rotationIn;
            float rotationOut = trailData.rotationOut;

            float velIn = trailData.velocityIn;
            float velOut = trailData.velocityOut;

            if (trailData.randomRotation) {
                float rand = MathUtils.getRandomNumberInRange(-1f, 1f);
                rotationIn = rotationIn * rand;
                rotationOut = rotationOut * rand;
            }

            if (trailData.dispersion > 0) {
                Vector2f.add(
                        sidewayVel,
                        MathUtils.getRandomPointInCircle(null, trailData.dispersion),
                        sidewayVel);
            }

            if (trailData.randomVelocity > 0) {
                float rand = MathUtils.getRandomNumberInRange(-1f, 1f);
                velIn *= 1 + trailData.randomVelocity * rand;
                velOut *= 1 + trailData.randomVelocity * rand;
            }

            //Opacity adjustment for fade-out, if the projectile uses it
            float opacityMult = 1f;
            if (trailData.fadeOnFadeOut && proj.isFading()) {
                opacityMult = Math.max(0, Math.min(1, proj.getDamageAmount() / proj.getBaseDamageAmount()));
            }

            //Then, actually spawn a trail
            MagicTrailPlugin.AddTrailMemberAdvanced(
                    proj,
                    trailIDs.get(i),
                    spriteToUse,
                    spawnPosition,
                    velIn,
                    velOut,
                    proj.getFacing() - 180f + trailData.angle,
                    rotationIn,
                    rotationOut,
                    trailData.sizeIn,
                    trailData.sizeOut,
                    trailData.colorIn,
                    trailData.colorOut,
                    trailData.opacity * opacityMult,
                    trailData.fadeIn,
                    trailData.duration,
                    trailData.fadeOut,
                    GL_SRC_ALPHA,
                    trailData.blendOut,
                    trailData.textLength,
                    trailData.textScroll,
                    trailData.textOffset,
                    sidewayVel,
                    null,
                    trailData.layer,
                    trailData.frameOffsetMult
            );
        }
    }

    public static void getTrailData() {
        //clear up the trash
        PROJ_TRAILS.clear();

        // Wisp: don't load from csv, that's done by the new classes and we don't want to load the same fx twice.

//        List<String> trailFiles = MagicSettings.getList(MagicVariables.MAGICLIB_ID, "magicTrail_files");
//        trailFiles.add("data/config/modFiles/magicTrail_data.csv");
//
//        for (String path : trailFiles) {
//
//            if (MagicVariables.verbose) LOG.warn("Merging trails from " + path);
//
//            //merge all the trail
//            JSONArray trailData = new JSONArray();
//            try {
//                trailData = Global.getSettings().getMergedSpreadsheetDataForMod("trail", path, MagicVariables.MAGICLIB_ID);
//            } catch (IOException | JSONException | RuntimeException ex) {
//                LOG.warn("unable to read " + path, ex);
//            }
//
//            for (int i = 0; i < trailData.length(); i++) {
//                try {
//                    JSONObject row = trailData.getJSONObject(i);
//
//                    //check the blending first
//                    int blend = GL_ONE_MINUS_SRC_ALPHA;
//                    if (row.getBoolean("additive")) {
//                        blend = GL_ONE;
//                    }
//
//                    //get the concerned projectile
//                    String thisProj = row.getString("projectile");
//
//                    //setup layer override
//                    CombatEngineLayers layer = CombatEngineLayers.BELOW_INDICATORS_LAYER;
//                    try {
//                        if (row.getBoolean("renderBelowExplosions")) {
//                            layer = CombatEngineLayers.ABOVE_SHIPS_LAYER;
//                        }
//                    } catch (JSONException ex) {
////                            LOG.warn("missing layer override for " + thisProj);
//                    }
//
//                    float frameOffsetMult = 1f;
//                    try {
//                        frameOffsetMult = (float) row.getDouble("frameOffsetMult");
//                    } catch (JSONException ex) {
////                            LOG.warn("missing frame offset mult override for " + thisProj);
//                    }
//
//                    float textureOffset = 0;
//                    try {
//                        if (row.getBoolean("randomTextureOffset")) {
//                            textureOffset = -1;
//                        }
//                    } catch (JSONException ignored) {
////                            LOG.warn("missing random texture offset boolean for " + thisProj);
//                    }
//
//                    //check if there are any trail already assigned to that projectile
//                    if (PROJ_TRAILS.containsKey(thisProj)) {
//                        //add the new trail to the existing proj
//                        PROJ_TRAILS.get(thisProj).add(
//                                new trailData(
//                                        row.getString("sprite"),
//                                        (float) row.getDouble("minLength"),
//                                        (float) row.getDouble("fadeIn"),
//                                        (float) row.getDouble("duration"),
//                                        (float) row.getDouble("fadeOut"),
//                                        (float) row.getDouble("sizeIn"),
//                                        (float) row.getDouble("sizeOut"),
//                                        MagicSettings.toColor3(row.getString("colorIn")),
//                                        MagicSettings.toColor3(row.getString("colorOut")),
//                                        (float) row.getDouble("opacity"),
//                                        blend,
//                                        (float) row.getDouble("textLength"),
//                                        (float) row.getDouble("textScroll"),
//                                        textureOffset,
//                                        (float) row.getDouble("distance"),
//                                        (float) row.getDouble("drift"),
//                                        row.getBoolean("fadeOnFadeOut"),
//                                        row.getBoolean("angleAdjustment"),
//                                        (float) row.getDouble("dispersion"),
//                                        (float) row.getDouble("velocityIn"),
//                                        (float) row.getDouble("velocityOut"),
//                                        (float) row.getDouble("randomVelocity"),
//                                        (float) row.getDouble("angle"),
//                                        (float) row.getDouble("rotationIn"),
//                                        (float) row.getDouble("rotationOut"),
//                                        row.getBoolean("randomRotation"),
//                                        layer,
//                                        frameOffsetMult
//                                ));
//
//                    } else {
//                        //add a new entry with that first trail
//                        List<trailData> list = new ArrayList<>();
//                        list.add(
//                                new trailData(
//                                        row.getString("sprite"),
//                                        (float) row.getDouble("minLength"),
//                                        (float) row.getDouble("fadeIn"),
//                                        (float) row.getDouble("duration"),
//                                        (float) row.getDouble("fadeOut"),
//                                        (float) row.getDouble("sizeIn"),
//                                        (float) row.getDouble("sizeOut"),
//                                        MagicSettings.toColor3(row.getString("colorIn")),
//                                        MagicSettings.toColor3(row.getString("colorOut")),
//                                        (float) row.getDouble("opacity"),
//                                        blend,
//                                        (float) row.getDouble("textLength"),
//                                        (float) row.getDouble("textScroll"),
//                                        textureOffset,
//                                        (float) row.getDouble("distance"),
//                                        (float) row.getDouble("drift"),
//                                        row.getBoolean("fadeOnFadeOut"),
//                                        row.getBoolean("angleAdjustment"),
//                                        (float) row.getDouble("dispersion"),
//                                        (float) row.getDouble("velocityIn"),
//                                        (float) row.getDouble("velocityOut"),
//                                        (float) row.getDouble("randomVelocity"),
//                                        (float) row.getDouble("angle"),
//                                        (float) row.getDouble("rotationIn"),
//                                        (float) row.getDouble("rotationOut"),
//                                        row.getBoolean("randomRotation"),
//                                        layer,
//                                        frameOffsetMult
//                                ));
//                        PROJ_TRAILS.put(
//                                thisProj,
//                                list
//                        );
//                    }
//                } catch (JSONException ex) {
//                    if (MagicVariables.verbose) LOG.warn("Invalid line, skipping");
//                }
//            }
//
//        }
    }

    //public methods for those that do not want to use the CSV merging

    public static void createProjTrail(String theProj, trailData theTrail) {
        List<trailData> list = new ArrayList<>();
        list.add(theTrail);
        PROJ_TRAILS.put(theProj, list);
    }

    public static void addProjTrail(String theProj, trailData theTrail) {
        PROJ_TRAILS.get(theProj).add(theTrail);
    }

    public static class trailData {
        private final String sprite;
        private final float minLength;
        private final float fadeIn;
        private final float duration;
        private final float fadeOut;
        private final float sizeIn;
        private final float sizeOut;
        private final Color colorIn;
        private final Color colorOut;
        private final float opacity;
        private final int blendOut;
        private final float textLength;
        private final float textScroll;
        private final float textOffset;
        private final float distance;
        private final float drift;
        private final boolean fadeOnFadeOut;
        private final boolean angleAdjustment;
        private final float dispersion;
        private final float velocityIn;
        private final float velocityOut;
        private final float randomVelocity;
        private final float angle;
        private final float rotationIn;
        private final float rotationOut;
        private final boolean randomRotation;
        private final CombatEngineLayers layer;
        private final float frameOffsetMult;

        public trailData(
                String sprite,
                float minLength,
                float fadeIn,
                float duration,
                float fadeOut,
                float sizeIn,
                float sizeOut,
                Color colorIn,
                Color colorOut,
                float opacity,
                int blendOut,
                float textLength,
                float textScroll,
                float textOffset,
                float distance,
                float drift,
                boolean fadeOnFadeOut,
                boolean angleAdjustment,
                float dispersion,
                float velocityIn,
                float velocityOut,
                float randomVelocity,
                float angle,
                float rotationIn,
                float rotationOut,
                boolean randomRotation,
                CombatEngineLayers layer,
                float frameOffsetMult
        ) {
            this.sprite = sprite;
            this.minLength = minLength;
            this.fadeIn = fadeIn;
            this.duration = duration;
            this.fadeOut = fadeOut;
            this.sizeIn = sizeIn;
            this.sizeOut = sizeOut;
            this.colorIn = colorIn;
            this.colorOut = colorOut;
            this.opacity = opacity;
            this.blendOut = blendOut;
            this.textLength = textLength;
            this.textScroll = textScroll;
            this.textOffset = textOffset;
            this.distance = distance;
            this.drift = drift;
            this.fadeOnFadeOut = fadeOnFadeOut;
            this.angleAdjustment = angleAdjustment;
            this.dispersion = dispersion;
            this.velocityIn = velocityIn;
            this.velocityOut = velocityOut;
            this.randomVelocity = randomVelocity;
            this.angle = angle;
            this.rotationIn = rotationIn;
            this.rotationOut = rotationOut;
            this.randomRotation = randomRotation;
            this.layer = layer;
            this.frameOffsetMult = frameOffsetMult;
        }
    }
}