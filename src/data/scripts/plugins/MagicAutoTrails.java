package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.ProjectileSpawnType;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.lazylib.MathUtils;

import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;

public class MagicAutoTrails extends BaseEveryFrameCombatPlugin {
    
    private static final Logger LOG = Global.getLogger(MagicAutoTrails.class); 
    private static final String PATH="data/trails/trail_data.csv";
    //Each proj can have multiple trails
    private static final Map<String,List<trailData>> PROJ_TRAILS = new HashMap<>();
    
    //A map for known projectiles and their IDs: should be cleared in init
    private Map<DamagingProjectileAPI, List<Float>> slowProjTrailIDs = new WeakHashMap<>();
    private Map<DamagingProjectileAPI, List<Float>> fastProjTrailIDs = new WeakHashMap<>();
    //A map to check the minimal length of everyframe projectiles and avoid issues with time warps
    private Map<DamagingProjectileAPI, Vector2f> fastProjLoc = new WeakHashMap<>();
    
    //timer for slow projectiles
    private final IntervalUtil timer = new IntervalUtil(0.06f,0.07f);

    @Override
    public void init(CombatEngineAPI engine) {
        //Reinitialize the lists
        fastProjTrailIDs.clear();
        fastProjLoc.clear();
        slowProjTrailIDs.clear();
        //reload the trails all the time in dev mode for easy testing :D
        if(Global.getSettings().isDevMode()){
            getTrailData();
        }
    }

    @Override
    public void advance (float amount, List<InputEventAPI> events) {
        if (Global.getCombatEngine() == null || Global.getCombatEngine().isPaused()) {
            return;
        }
        CombatEngineAPI engine = Global.getCombatEngine();
        
        //Runs once on each projectile that matches one of the IDs specified in our maps
        for (DamagingProjectileAPI proj : engine.getProjectiles()) {
            
            //Ignore already-collided projectiles, and projectiles that don't match our IDs
            if (proj.getProjectileSpecId() == null || proj.didDamage()) {
                continue;
            }
            
            //check if that type of proj has any trail
            if (!PROJ_TRAILS.keySet().contains(proj.getProjectileSpecId())) {
                continue;
            }            
            
            Vector2f projVel = new Vector2f(proj.getVelocity());
            String specID = proj.getProjectileSpecId();
            
            //add segments to the projs that already have a trail:
            
            //everyframe projectiles:
            if(fastProjLoc.containsKey(proj)){
                //check for min segment length
                if(!MathUtils.isWithinRange(proj.getLocation(), fastProjLoc.get(proj), PROJ_TRAILS.get(specID).get(0).minLength)){
                    //put new location and draw a new segment
                    fastProjLoc.put(proj, new Vector2f(proj.getLocation()));
                    createTrailSegment(proj, specID, fastProjTrailIDs.get(proj), projVel);
                }
                continue;
            }
            
            //slow projectiles:
            timer.advance(amount);
            if(timer.intervalElapsed()){
                if(slowProjTrailIDs.containsKey(proj)){
                    createTrailSegment(proj, specID, slowProjTrailIDs.get(proj), projVel);
                    continue;
                }
            }
            
            //apparently this projectile doesn't have a trail, let's fix that!
                        
            //check if it is a fast or slow proj
            if(PROJ_TRAILS.get(specID).get(0).minLength<0){
                
                //SLOW MODE
                
                List<Float> specs = new ArrayList<>();
                slowProjTrailIDs.put(proj,specs);
                
                //add all the necessary trails to that proj
                for (trailData get : PROJ_TRAILS.get(specID)) {
                    slowProjTrailIDs.get(proj).add(MagicTrailPlugin.getUniqueID());
                }

                //Fix for some first-frame error shenanigans
                if (projVel.length() < 0.1f && proj.getSource() != null) {
                    projVel = new Vector2f(proj.getSource().getVelocity());
                }
                
                //add initial segment
                createTrailSegment(proj, specID, slowProjTrailIDs.get(proj), projVel);
                
            } else {
                
                //EVERYFRAME MODE
                
                List<Float> specs = new ArrayList<>(); 
                fastProjTrailIDs.put(proj,specs);    
                
                //store position for minimal length check
                fastProjLoc.put(proj, new Vector2f(proj.getLocation()));
                
                //add all the necessary trails to that proj
                for (trailData get : PROJ_TRAILS.get(specID)) {
                    fastProjTrailIDs.get(proj).add(MagicTrailPlugin.getUniqueID());
                }

                //Fix for some first-frame error shenanigans
                if (projVel.length() < 0.1f && proj.getSource() != null) {
                    projVel = new Vector2f(proj.getSource().getVelocity());
                }
                
                //add initial segment                
                createTrailSegment(proj, specID, fastProjTrailIDs.get(proj), projVel);
            }
        }
    } 
        
    private static void createTrailSegment(DamagingProjectileAPI proj, String specID, List<Float> trailIDs, Vector2f projVel){
        
        for(int i=0; i<trailIDs.size(); i++){

            SpriteAPI spriteToUse = Global.getSettings().getSprite("fx", PROJ_TRAILS.get(specID).get(i).sprite);

            //If we use angle adjustment, do that here
            if (PROJ_TRAILS.get(specID).get(i).angleAdjustment && projVel.length() > 0.1f && !proj.getSpawnType().equals(ProjectileSpawnType.BALLISTIC_AS_BEAM)) {
                proj.setFacing(VectorUtils.getAngle(new Vector2f(0f, 0f), projVel));
            }

            //Gets a custom "offset" position, so we can slightly alter the spawn location to account for "natural fade-in", and add that to our spawn position
            Vector2f offsetPoint = new Vector2f((float)Math.cos(Math.toRadians(proj.getFacing())) * PROJ_TRAILS.get(specID).get(i).distance, (float)Math.sin(Math.toRadians(proj.getFacing())) * PROJ_TRAILS.get(specID).get(i).distance);
            Vector2f spawnPosition = new Vector2f(offsetPoint.x + proj.getLocation().x, offsetPoint.y + proj.getLocation().y);

            //Sideway offset velocity, for projectiles that use it
            Vector2f projBodyVel = new Vector2f(projVel);
            projBodyVel = VectorUtils.rotate(projBodyVel, -proj.getFacing());
            Vector2f projLateralBodyVel = new Vector2f(0f, projBodyVel.getY());
            Vector2f sidewayVel = new Vector2f(projLateralBodyVel);
            sidewayVel = (Vector2f)VectorUtils.rotate(sidewayVel, proj.getFacing()).scale(PROJ_TRAILS.get(specID).get(i).drift);

            //random dispersion of the segments if necessary
            float rotationIn = PROJ_TRAILS.get(specID).get(i).rotationIn;
            float rotationOut = PROJ_TRAILS.get(specID).get(i).rotationOut;
            
            float velIn = PROJ_TRAILS.get(specID).get(i).velocityIn;
            float velOut = PROJ_TRAILS.get(specID).get(i).velocityOut;
            
            if(PROJ_TRAILS.get(specID).get(i).randomRotation){
                float rand = MathUtils.getRandomNumberInRange(-1, 1);
                rotationIn = rotationIn*rand;
                rotationOut = rotationOut*rand;
            }
            
            if(PROJ_TRAILS.get(specID).get(i).dispersion>0){
                Vector2f.add(
                        sidewayVel,
                        MathUtils.getRandomPointInCircle(new Vector2f(),PROJ_TRAILS.get(specID).get(i).dispersion),
                        sidewayVel);
            }
            
            if(PROJ_TRAILS.get(specID).get(i).randomVelocity>0){
                float rand = MathUtils.getRandomNumberInRange(-1, 1);
                velIn*= 1 + PROJ_TRAILS.get(specID).get(i).randomVelocity*rand;
                velOut*= 1 + PROJ_TRAILS.get(specID).get(i).randomVelocity*rand;
            }

            //Opacity adjustment for fade-out, if the projectile uses it
            float opacityMult = 1f;
            if (PROJ_TRAILS.get(specID).get(i).fadeOnFadeOut && proj.isFading()) {
                opacityMult = Math.max(0,Math.min(1,proj.getDamageAmount() / proj.getBaseDamageAmount()));
            }

            //Then, actually spawn a trail
            MagicTrailPlugin.AddTrailMemberAdvanced(
                    proj,
                    trailIDs.get(i),
                    spriteToUse,
                    spawnPosition,
                    velIn,
                    velOut,
                    proj.getFacing() - 180f + PROJ_TRAILS.get(specID).get(i).angle,                    
                    rotationIn,
                    rotationOut,
                    PROJ_TRAILS.get(specID).get(i).sizeIn,
                    PROJ_TRAILS.get(specID).get(i).sizeOut,
                    PROJ_TRAILS.get(specID).get(i).colorIn,
                    PROJ_TRAILS.get(specID).get(i).colorOut,
                    PROJ_TRAILS.get(specID).get(i).opacity * opacityMult,
                    PROJ_TRAILS.get(specID).get(i).fadeIn,
                    PROJ_TRAILS.get(specID).get(i).duration,
                    PROJ_TRAILS.get(specID).get(i).fadeOut,
                    GL_SRC_ALPHA,
                    PROJ_TRAILS.get(specID).get(i).blendOut,
                    PROJ_TRAILS.get(specID).get(i).textLength,
                    PROJ_TRAILS.get(specID).get(i).textScroll,
                    sidewayVel,
                    null,
                    PROJ_TRAILS.get(specID).get(i).layer
            );
        }        
    }
    
    public static void getTrailData(){
        //clear up the trash
        PROJ_TRAILS.clear();
        
        //merge all the trail_data
        JSONArray trailData = new JSONArray();
        try {
            trailData = Global.getSettings().getMergedSpreadsheetDataForMod("trail", PATH, "MagicLib");
        } catch (IOException | JSONException | RuntimeException ex) {
            LOG.error("unable to read trail_data.csv");
            LOG.error(ex);
        }
            
        for(int i=0; i<trailData.length(); i++){
            try {
                
//                if(trailData.getJSONObject(i).get("projectile").equals("")){
//                    continue;
//                }
                
                JSONObject row = trailData.getJSONObject(i);
                
                //check the blending first
                int blend = GL_ONE_MINUS_SRC_ALPHA;
                if(row.getBoolean("additive")){
                    blend=GL_ONE;
                }
                
                //get the concerned projectile
                String thisProj = row.getString("projectile");
                
                //setup layer override
                CombatEngineLayers layer = CombatEngineLayers.BELOW_INDICATORS_LAYER;
                try{
                    if(row.getBoolean("renderBellowExplosions")==true){
                        layer = CombatEngineLayers.ABOVE_SHIPS_LAYER;
                    }
                } catch (JSONException ex) {
                    LOG.error("missing layer override for "+thisProj);
                }
                
                //check if there are any trail already assigned to that projectile
                if(PROJ_TRAILS.containsKey(thisProj)){
                    //add the new trail to the existing proj
                    PROJ_TRAILS.get(thisProj).add(
                            new trailData(
                                    row.getString("sprite"),
                                    (float)row.getDouble("minLength"),
                                    (float)row.getDouble("fadeIn"),
                                    (float)row.getDouble("duration"),
                                    (float)row.getDouble("fadeOut"),
                                    (float)row.getDouble("sizeIn"),
                                    (float)row.getDouble("sizeOut"),
                                    toColor3(row.getString("colorIn")),
                                    toColor3(row.getString("colorOut")),
                                    (float)row.getDouble("opacity"),
                                    blend,
                                    (float)row.getDouble("textLength"),
                                    (float)row.getDouble("textScroll"),
                                    (float)row.getDouble("distance"),
                                    (float)row.getDouble("drift"),
                                    row.getBoolean("fadeOnFadeOut"),
                                    row.getBoolean("angleAdjustment"),
                                    (float)row.getDouble("dispersion"),
                                    (float)row.getDouble("velocityIn"),
                                    (float)row.getDouble("velocityOut"),
                                    (float)row.getDouble("randomVelocity"),
                                    (float)row.getDouble("angle"),
                                    (float)row.getDouble("rotationIn"),
                                    (float)row.getDouble("rotationOut"),
                                    row.getBoolean("randomRotation"),
                                    layer
                            ));
                
                } else {
                    //add a new entry with that first trail
                    List<trailData> list = new ArrayList<>();
                    list.add(
                            new trailData(
                                    row.getString("sprite"),
                                    (float)row.getDouble("minLength"),
                                    (float)row.getDouble("fadeIn"),
                                    (float)row.getDouble("duration"),
                                    (float)row.getDouble("fadeOut"),
                                    (float)row.getDouble("sizeIn"),
                                    (float)row.getDouble("sizeOut"),
                                    toColor3(row.getString("colorIn")),
                                    toColor3(row.getString("colorOut")),
                                    (float)row.getDouble("opacity"),
                                    blend,
                                    (float)row.getDouble("textLength"),
                                    (float)row.getDouble("textScroll"),
                                    (float)row.getDouble("distance"),
                                    (float)row.getDouble("drift"),
                                    row.getBoolean("fadeOnFadeOut"),
                                    row.getBoolean("angleAdjustment"),
                                    (float)row.getDouble("dispersion"),
                                    (float)row.getDouble("velocityIn"),
                                    (float)row.getDouble("velocityOut"),
                                    (float)row.getDouble("randomVelocity"),
                                    (float)row.getDouble("angle"),
                                    (float)row.getDouble("rotationIn"),
                                    (float)row.getDouble("rotationOut"),
                                    row.getBoolean("randomRotation"),
                                    layer
                            ));                    
                    PROJ_TRAILS.put(
                            thisProj,
                            list
                            );
                }
            } catch (JSONException ex) {
               LOG.error("Invalid line, skipping");
           }  
        }
    }
    
    //public methods for those that do not want to use the CSV merging
    
    public static void createProjTrail(String theProj, trailData theTrail){
        List<trailData> list = new ArrayList<>();
        list.add(theTrail);
        PROJ_TRAILS.put(theProj,list);
    }
    
    public static void addProjTrail(String theProj, trailData theTrail){
        PROJ_TRAILS.get(theProj).add(theTrail);
    } 
    
    /////// UTILS ///////
    
    private static Color toColor3(String in) {
        final String inPredicate = in.substring(1, in.length() - 1);
        final String[] array = inPredicate.split(",");
        return new Color(clamp255(Integer.parseInt(array[0])), clamp255(Integer.parseInt(array[1])), clamp255(Integer.parseInt(array[2])), 255);
    }
    
    private static int clamp255(int x) {
        return Math.max(0, Math.min(255, x));
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
                CombatEngineLayers layer
        ){
            this.sprite=sprite;
            this.minLength=minLength;
            this.fadeIn=fadeIn;
            this.duration=duration;
            this.fadeOut=fadeOut;
            this.sizeIn=sizeIn;
            this.sizeOut=sizeOut;
            this.colorIn=colorIn;
            this.colorOut=colorOut;
            this.opacity=opacity;
            this.blendOut=blendOut;
            this.textLength=textLength;
            this.textScroll=textScroll;
            this.distance=distance;
            this.drift=drift;
            this.fadeOnFadeOut=fadeOnFadeOut;
            this.angleAdjustment=angleAdjustment;  
            this.dispersion=dispersion; 
            this.velocityIn=velocityIn; 
            this.velocityOut=velocityOut; 
            this.randomVelocity=randomVelocity;
            this.angle=angle; 
            this.rotationIn=rotationIn; 
            this.rotationOut=rotationOut;
            this.randomRotation=randomRotation;
            this.layer=layer;
        }
    }
}