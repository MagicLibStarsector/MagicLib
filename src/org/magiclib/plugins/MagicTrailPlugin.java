
package org.magiclib.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.jetbrains.annotations.Nullable;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicTrailObject;
import org.magiclib.util.MagicTrailTracker;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.Map.Entry;

import static org.lwjgl.opengl.GL11.*;

/**
 * Allows custom QUAD_STRIP-style trails to be drawn freely, with a bunch of customization available.
 * Note that any sprites that use this plugin must have a multiple of 2 as size (so 16, 32, 64, 128 etc.), both in width and height.
 * The trails are made by spawning "trail pieces", which if they have the same ID links together to form a smooth trail (the trail will not render without having at least 2 pieces link together).
 *
 * @author Nicke535, Originem (optimization)
 */
public class MagicTrailPlugin extends BaseEveryFrameCombatPlugin {
    public static final String PLUGIN_KEY = "MagicTrailPlugin";
    //Tracker for unique ID getting. Only use for this script, though: it's dangerous to use for other ID purposes, since it is so simple
    //NOTE: IDs should be bigger than 0; lower than 0 IDs are used by the script for "cut" trails
    private static float usedIDs = 1f;
    private static float usedCutterIDs = -1f;

    private static final int ANIM_KEY = -1;

    //Map which handles all the trails: takes in a render layer, an integer (the texture) and a map of MagicTrailTrackers, identified by a unique ID which must be tracked for each source independently
    //Also, if tex id =-1, it means it's an animated trail that texture could be changed mid-trail
    protected Map<CombatEngineLayers, Map<Integer, Map<Float, MagicTrailTracker>>> mainMap = new EnumMap<>(CombatEngineLayers.class);

    //Map for "cutting" trails: if a trail belongs to an entity (it hasn't already been cut) its ID is here
    //if tex id =-1, it's animated cutting map
    private Map<CombatEngineLayers, Map<Integer, Map<CombatEntityAPI, List<Float>>>> cuttingMap = new EnumMap<>(CombatEngineLayers.class);


    //clean the main map and cutting map per second
    private final IntervalUtil cleanTimer = new IntervalUtil(1f, 1f);
    private CombatEngineAPI engine;


    /**
     * Add trail tracker to target plugin
     *
     * @param plugin       MagicTrailPlugin
     * @param ID           trail id, use Misc.getUniqueID()
     * @param linkedEntity
     * @param layer        rendered layer
     * @param sprite
     * @param isAnim       if the trail is anim
     * @return
     */
    private static MagicTrailTracker addOrGetTrailTracker(MagicTrailPlugin plugin, Float ID, CombatEntityAPI linkedEntity, CombatEngineLayers layer, SpriteAPI sprite, boolean isAnim) {

        int texID;
        texID = isAnim ? ANIM_KEY : sprite.getTextureId();

        Map<Float, MagicTrailTracker> trailTrackerMap;
        Map<Integer, Map<Float, MagicTrailTracker>> layerMap = plugin.mainMap.get(layer);
        if (layerMap == null) {
            layerMap = new HashMap<>();
            plugin.mainMap.put(layer, layerMap);
        }

        trailTrackerMap = layerMap.get(texID);
        if (trailTrackerMap == null) {
            trailTrackerMap = new HashMap<>();
            layerMap.put(texID, trailTrackerMap);
        }

        MagicTrailTracker trailTracker = trailTrackerMap.get(ID);
        if (trailTracker == null) {
            trailTracker = new MagicTrailTracker();
            trailTrackerMap.put(ID, trailTracker);
        }

        if (linkedEntity != null) {
            Map<Integer, Map<CombatEntityAPI, List<Float>>> layerCutMap = plugin.cuttingMap.get(layer);
            if (layerCutMap == null) {
                layerCutMap = new HashMap<>();
                plugin.cuttingMap.put(layer, layerCutMap);
            }
            Map<CombatEntityAPI, List<Float>> entityCuttingIDMap = layerCutMap.get(texID);
            if (entityCuttingIDMap == null) {
                entityCuttingIDMap = new HashMap<>();
                layerCutMap.put(texID, entityCuttingIDMap);
            }
            List<Float> cuttingIDs = entityCuttingIDMap.get(linkedEntity);
            if (cuttingIDs == null) {
                cuttingIDs = new ArrayList<>();
                entityCuttingIDMap.put(linkedEntity, cuttingIDs);
            }
            if (!cuttingIDs.contains(ID)) {
                cuttingIDs.add(ID);
            }
        }
        return trailTracker;
    }

    //SIMPLE DECLARATION

    /**
     * Spawns a trail piece, which links up with other pieces with the same ID
     * to form a smooth trail. This function has most of the functionality you
     * need; should you want more configurability, use AddTrailMemberAdvanced
     * instead
     *
     * @param linkedEntity The entity this trail is attached to, used for cutting trails.
     *                     Can be Null, but that should really only be done in weird, edge-case scenarios
     * @param ID           The ID for this specific trail. Preferably get this from getUniqueID,
     *                     but it's not required: just expect very weird results if you don't
     * @param sprite       Which sprite to draw for this trail: do *not* change this halfway through a trail,
     *                     as that will split it into two trails
     * @param position     Starting position for this piece of trail
     * @param speed        The speed, in SU, this trail piece is moving at
     * @param angle        Which angle this piece of trail has in degrees; determines which direction it moves,
     *                     and which direction its size is measured over
     * @param startSize    The starting size (or rather width) this piece of trail has. Measured in SU. A trail
     *                     smoothly transitions from its startSize to its endSize over its duration
     * @param endSize      The ending size (or rather width) this trail piece has. Measured in SU. A trail smoothly
     *                     transitions from its startSize to its endSize over its duration
     * @param color        The color of this piece of trail. Can be changed in the middle of a trail, and will blend
     *                     smoothly between pieces. Ignores alpha component entirely
     * @param opacity      The starting opacity of this piece of trail. Is a value between 0f and 1f. The opacity
     *                     gradually approaches 0f over the trail's duration
     * @param inDuration   How long this trail spends "fading in"; for this many seconds, the opacity of the trail piece
     *                     steadily increases until reaching "opacity". A trail's total duration is
     *                     inDuration + mainDuration + outDuration
     * @param mainDuration How long a trail uses its maximum opacity. A trail's total duration is
     *                     inDuration + mainDuration + outDuration
     * @param outDuration  How long a trail spends "fading out"; over this many seconds at the end of the trail's
     *                     duration, its opacity goes from "opacity" to 0f. A trail's total duration is
     *                     inDuration + mainDuration + outDuration
     * @param additive     Whether this trail will use additive blending or not. Does not support being changed in
     *                     the middle of a trail
     */
    public static void addTrailMemberSimple(
            CombatEntityAPI linkedEntity, float ID, SpriteAPI sprite,
            Vector2f position, float speed, float angle,
            float startSize, float endSize,
            Color color, float opacity,
            float inDuration, float mainDuration, float outDuration,
            boolean additive) {
        //First, find the plugin
        MagicTrailPlugin plugin = getPlugin();
        if (plugin == null) return;

        CombatEngineLayers layer = CombatEngineLayers.CONTRAILS_LAYER;

        //Finds the correct maps, and ensures they are actually instantiated [and adds our ID to the cutting map]
        MagicTrailTracker tracker = addOrGetTrailTracker(plugin, ID, linkedEntity, layer, sprite, false);

        //Converts our additive/non-additive option to true openGL stuff
        int srcBlend = GL_SRC_ALPHA;
        int destBlend = additive ? GL_ONE : GL_ONE_MINUS_SRC_ALPHA;

        //Creates the custom object we want
        MagicTrailObject objectToAdd = new MagicTrailObject(inDuration, mainDuration, outDuration, startSize, endSize, 0f, 0f,
                opacity, srcBlend, destBlend,
                speed, speed, color, color,
                angle, position,
                -1f, 0, new Vector2f(),
                0f, 0);

        //And finally add it to the correct location in our maps
        tracker.addNewTrailObject(objectToAdd);
    }


    // ADVANCED DECLARATION

    /**
     * Spawns a trail piece, which links up with other pieces with the same ID
     * to form a smooth trail. This function has all available functions; if you
     * just want to spawn a normal trail without all the extra configuration involved,
     * use AddTrailMemberSimple instead.
     *
     * @param linkedEntity         The entity this trail is attached to, used for cutting trails.
     *                             Can be Null, but that should really only be done in weird, edge-case scenarios
     * @param ID                   The ID for this specific trail. Preferably get this from getUniqueID,
     *                             but it's not required: just expect very weird results if you don't
     * @param sprite               Which sprite to draw for this trail: do *not* change this halfway through a trail,
     *                             as that will split it into two trails
     * @param position             Starting position for this piece of trail
     * @param startSpeed           The starting speed, in SU, this trail piece is moving at. The trail piece smoothly
     *                             transitions from its startSpeed to its endSpeed over its duration
     * @param endSpeed             The ending speed, in SU, this trail piece is moving at. The trail piece smoothly
     *                             transitions from its startSpeed to its endSpeed over its duration
     * @param angle                Which angle this piece of trail has in degrees; determines which direction it moves,
     *                             and which direction its size is measured over
     * @param startAngularVelocity The angular velocity this trail piece has when spawned. The angular velocity
     *                             of a trail piece smoothly transitions from startAngularVelocity to
     *                             endAngularVelocity over its duration
     * @param endAngularVelocity   The angular velocity this trail piece has just before disappearing.
     *                             The angular velocity of a trail piece smoothly transitions from
     *                             startAngularVelocity to endAngularVelocity over its duration
     * @param startSize            The starting size (or rather width) this piece of trail has. Measured in SU. A trail
     *                             smoothly transitions from its startSize to its endSize over its duration
     * @param endSize              The ending size (or rather width) this trail piece has. Measured in SU. A trail smoothly
     *                             transitions from its startSize to its endSize over its duration
     * @param startColor           The color this piece of trail has when spawned. Can be changed in the middle of a trail,
     *                             and will blend smoothly between pieces. Ignores alpha component entirely. Each trail piece
     *                             smoothly transitions from startColor to endColor over its duration
     * @param endColor             The color this piece of trail has just before disappearing. Can be changed in the middle of a
     *                             trail, and will blend smoothly between pieces. Ignores alpha component entirely. Each trail piece
     *                             smoothly transitions from startColor to endColor over its duration
     * @param opacity              The starting opacity of this piece of trail. Is a value between 0f and 1f. The opacity
     *                             gradually approaches 0f over the trail's duration
     * @param inDuration           How long this trail spends "fading in"; for this many seconds, the opacity of the trail piece
     *                             steadily increases until reaching "opacity". A trail's total duration is
     *                             inDuration + mainDuration + outDuration
     * @param mainDuration         How long a trail uses its maximum opacity. A trail's total duration is
     *                             inDuration + mainDuration + outDuration
     * @param outDuration          How long a trail spends "fading out"; over this many seconds at the end of the trail's
     *                             duration, its opacity goes from "opacity" to 0f. A trail's total duration is
     *                             inDuration + mainDuration + outDuration
     * @param additive             Whether this trail will use additive blending or not. Does not support being changed in
     *                             the middle of a trail
     * @param textureLoopLength    How many SU it takes for the texture to loop. Should preferably be non-zero. If the
     *                             trail is not supposed to loop, put this as -1f
     * @param textureScrollSpeed   How fast, and in which direction, the texture scrolls over the trail. Defined so that
     *                             1000 means scrolling the entire texture length once per second, and 2000 means
     *                             scrolling the entire texture length twice per second
     * @param textureOffset        Optional texture offset to prevent repetitions between trails, default:0, fixed random offset: -1;
     * @param offsetVelocity       The offset velocity of the trail; this is an additional velocity that is
     *                             unaffected by rotation and facing, and will never change over the trail's lifetime
     * @param advancedOptions      The most unique and special options go in a special Map<> here. Be careful to input the
     *                             correct type values and use the right data keys. Any new features will be added here to
     *                             keep compatibility with old mod versions. Can be null. Currently supported keys:
     *                             "SIZE_PULSE_WIDTH" :  Float - How much additional width the trail gains each "pulse", at
     *                             most. Used in conjunction with SIZE_PULSE_COUNT
     *                             "SIZE_PULSE_COUNT" :  Integer - How many times the trail "pulses" its width over its
     *                             lifetime. Used in conjunction with SIZE_PULSE_WIDTH
     *                             "FORWARD_PROPAGATION" :  Boolean - If the trail uses the legacy render method of
     *                             "forward propagation". Used to be the default. CANNOT be
     *                             changed mid-trail
     * @param layerToRenderOn      Which combat layer to render the trail on. All available layers are specified in
     *                             CombatEngineLayers. Old behaviour was CombatEngineLayers.BELOW_INDICATORS_LAYER.
     *                             CANNOT change mid-trail, under any circumstance
     * @param frameOffsetMult      The per-frame multiplier for the per-frame velocity offset magnitude. Used to finely
     *                             adjust trail offset at different speeds. Default: 1f
     */
    public static void addTrailMemberAdvanced(
            CombatEntityAPI linkedEntity, float ID, SpriteAPI sprite,
            Vector2f position, float startSpeed, float endSpeed,
            float angle, float startAngularVelocity, float endAngularVelocity,
            float startSize, float endSize,
            Color startColor, Color endColor, float opacity,
            float inDuration, float mainDuration, float outDuration,
            boolean additive,
            float textureLoopLength, float textureScrollSpeed, float textureOffset,
            @Nullable Vector2f offsetVelocity, @Nullable Map<String, Object> advancedOptions,
            @Nullable CombatEngineLayers layerToRenderOn, float frameOffsetMult) {

        //Converts our additive/non-additive option to true openGL stuff
        int blendModeSRC = GL_SRC_ALPHA;
        int blendModeDEST = additive ? GL_ONE : GL_ONE_MINUS_SRC_ALPHA;

        addTrailMemberAdvanced(linkedEntity, ID, sprite, position, startSpeed, endSpeed, angle, startAngularVelocity, endAngularVelocity, startSize, endSize,
                startColor, endColor, opacity, inDuration, mainDuration, outDuration, blendModeSRC, blendModeDEST, textureLoopLength, textureScrollSpeed, textureOffset,
                offsetVelocity, advancedOptions, layerToRenderOn, frameOffsetMult);
    }

    /**
     * Spawns a trail piece, which links up with other pieces with the same ID
     * to form a smooth trail. This function has all available functions; if you
     * just want to spawn a normal trail without all the extra configuration involved,
     * use AddTrailMemberSimple instead.
     *
     * @param linkedEntity         The entity this trail is attached to, used for cutting trails.
     *                             Can be Null, but that should really only be done in weird, edge-case scenarios
     * @param ID                   The ID for this specific trail. Preferably get this from getUniqueID,
     *                             but it's not required: just expect very weird results if you don't
     * @param sprite               Which sprite to draw for this trail: do *not* change this halfway through a trail,
     *                             as that will split it into two trails
     * @param position             Starting position for this piece of trail
     * @param startSpeed           The starting speed, in SU, this trail piece is moving at. The trail piece smoothly
     *                             transitions from its startSpeed to its endSpeed over its duration
     * @param endSpeed             The ending speed, in SU, this trail piece is moving at. The trail piece smoothly
     *                             transitions from its startSpeed to its endSpeed over its duration
     * @param angle                Which angle this piece of trail has in degrees; determines which direction it moves,
     *                             and which direction its size is measured over
     * @param startAngularVelocity The angular velocity this trail piece has when spawned. The angular velocity
     *                             of a trail piece smoothly transitions from startAngularVelocity to
     *                             endAngularVelocity over its duration
     * @param endAngularVelocity   The angular velocity this trail piece has just before disappearing.
     *                             The angular velocity of a trail piece smoothly transitions from
     *                             startAngularVelocity to endAngularVelocity over its duration
     * @param startSize            The starting size (or rather width) this piece of trail has. Measured in SU. A trail
     *                             smoothly transitions from its startSize to its endSize over its duration
     * @param endSize              The ending size (or rather width) this trail piece has. Measured in SU. A trail smoothly
     *                             transitions from its startSize to its endSize over its duration
     * @param startColor           The color this piece of trail has when spawned. Can be changed in the middle of a trail,
     *                             and will blend smoothly between pieces. Ignores alpha component entirely. Each trail piece
     *                             smoothly transitions from startColor to endColor over its duration
     * @param endColor             The color this piece of trail has just before disappearing. Can be changed in the middle of a
     *                             trail, and will blend smoothly between pieces. Ignores alpha component entirely. Each trail piece
     *                             smoothly transitions from startColor to endColor over its duration
     * @param opacity              The starting opacity of this piece of trail. Is a value between 0f and 1f. The opacity
     *                             gradually approaches 0f over the trail's duration
     * @param inDuration           How long this trail spends "fading in"; for this many seconds, the opacity of the trail piece
     *                             steadily increases until reaching "opacity". A trail's total duration is
     *                             inDuration + mainDuration + outDuration
     * @param mainDuration         How long a trail uses its maximum opacity. A trail's total duration is
     *                             inDuration + mainDuration + outDuration
     * @param outDuration          How long a trail spends "fading out"; over this many seconds at the end of the trail's
     *                             duration, its opacity goes from "opacity" to 0f. A trail's total duration is
     *                             inDuration + mainDuration + outDuration
     * @param blendModeSRC         Which SRD openGL blend mode to use for the trail. If you are unsure of what this means, just
     *                             put it as GL_SRC_ALPHA
     * @param blendModeDEST        Which DEST openGL blend mode to use for the trail. If you are unsure of what this means,
     *                             put it as GL_ONE_MINUS_SRC_ALPHA for normal blending and GL_ONE for additive blending
     * @param textureLoopLength    How many SU it takes for the texture to loop. Should preferably be non-zero. If the
     *                             trail is not supposed to loop, put this as -1f
     * @param textureScrollSpeed   How fast, and in which direction, the texture scrolls over the trail. Defined so that
     *                             1000 means scrolling the entire texture length once per second, and 2000 means
     *                             scrolling the entire texture length twice per second
     * @param textureOffset        Optional texture offset to prevent repetitions between trails, default:0, fixed random offset: -1;
     * @param offsetVelocity       The offset velocity of the trail; this is an additional velocity that is
     *                             unaffected by rotation and facing, and will never change over the trail's lifetime
     * @param advancedOptions      The most unique and special options go in a special Map<> here. Be careful to input the
     *                             correct type values and use the right data keys. Any new features will be added here to
     *                             keep compatibility with old mod versions. Can be null. Currently supported keys:
     *                             "SIZE_PULSE_WIDTH" :  Float - How much additional width the trail gains each "pulse", at
     *                             most. Used in conjunction with SIZE_PULSE_COUNT
     *                             "SIZE_PULSE_COUNT" :  Integer - How many times the trail "pulses" its width over its
     *                             lifetime. Used in conjunction with SIZE_PULSE_WIDTH
     *                             "FORWARD_PROPAGATION" :  Boolean - If the trail uses the legacy render method of
     *                             "forward propagation". Used to be the default. CANNOT be
     *                             changed mid-trail
     * @param layerToRenderOn      Which combat layer to render the trail on. All available layers are specified in
     *                             CombatEngineLayers. Old behaviour was CombatEngineLayers.BELOW_INDICATORS_LAYER.
     *                             CANNOT change mid-trail, under any circumstance
     * @param frameOffsetMult      The per-frame multiplier for the per-frame velocity offset magnitude. Used to finely
     *                             adjust trail offset at different speeds. Default: 1f
     */
    public static void addTrailMemberAdvanced(
            CombatEntityAPI linkedEntity, float ID, SpriteAPI sprite,
            Vector2f position, float startSpeed, float endSpeed,
            float angle, float startAngularVelocity, float endAngularVelocity,
            float startSize, float endSize,
            Color startColor, Color endColor, float opacity,
            float inDuration, float mainDuration, float outDuration,
            int blendModeSRC, int blendModeDEST,
            float textureLoopLength, float textureScrollSpeed, float textureOffset,
            @Nullable Vector2f offsetVelocity, @Nullable Map<String, Object> advancedOptions,
            @Nullable CombatEngineLayers layerToRenderOn, float frameOffsetMult) {

        //First, find the plugin, and if it doesn't exist do nothing
        MagicTrailPlugin plugin = getPlugin();
        if (plugin == null) return;


        Vector2f offsetVel = new Vector2f();
        if (offsetVelocity != null) {
            offsetVel = offsetVelocity;
        }

        CombatEngineLayers layer = CombatEngineLayers.CONTRAILS_LAYER;
        if (layerToRenderOn != null) {
            layer = layerToRenderOn;
        }

        float mult = 1;
        if (frameOffsetMult != 0) {
            mult = frameOffsetMult;
        }

        //Finds the correct maps, and ensures they are actually instantiated [and adds our ID to the cutting map]
        MagicTrailTracker tracker = addOrGetTrailTracker(plugin, ID, linkedEntity, layer, sprite, false);

        //Adjusts scroll speed to our most recent trail's value
        tracker.scrollSpeed = textureScrollSpeed;

        //--Reads in our special options, if we have any--
        float sizePulseWidth = 0f;
        int sizePulseCount = 0;
        if (advancedOptions != null) {
            if (advancedOptions.get("SIZE_PULSE_WIDTH") instanceof Float) {
                sizePulseWidth = (Float) advancedOptions.get("SIZE_PULSE_WIDTH");
            }
            if (advancedOptions.get("SIZE_PULSE_COUNT") instanceof Integer) {
                sizePulseCount = (Integer) advancedOptions.get("SIZE_PULSE_COUNT");
            }
            if (advancedOptions.get("FORWARD_PROPAGATION") instanceof Boolean && (boolean) advancedOptions.get("FORWARD_PROPAGATION")) {
                tracker.usesForwardPropagation = true;
            }
        }
        //--End of special options--

        //Offset tweaker to fix single frame delay for lateral movement
        Vector2f correctedPosition = new Vector2f(position);
        if (linkedEntity instanceof DamagingProjectileAPI) {
            DamagingProjectileAPI proj = (DamagingProjectileAPI) linkedEntity;

            Vector2f shipVelPerAdvance = (Vector2f) new Vector2f(proj.getSource().getVelocity()).scale(Global.getCombatEngine().getElapsedInLastFrame());
            shipVelPerAdvance.scale(mult);
            Vector2f.sub(position, shipVelPerAdvance, correctedPosition);
        }

        //check for specific texture offset or a random one
        float textOffset = 0;
        if (textureOffset == -1) {
            //the texture tracker keep a fixed random texture offset
            if (tracker.textureOffset == -1) {
                tracker.textureOffset = MathUtils.getRandomNumberInRange(0, textureLoopLength);
            }
            textOffset = tracker.textureOffset;
        } else if (textureOffset != 0) {
            textOffset = textureOffset;
        }

        //Creates the custom object we want
        MagicTrailObject objectToAdd = new MagicTrailObject(inDuration, mainDuration, outDuration, startSize, endSize, startAngularVelocity, endAngularVelocity,
                opacity, blendModeSRC, blendModeDEST, startSpeed, endSpeed, startColor, endColor, angle, correctedPosition, textureLoopLength, textOffset, offsetVel,
                sizePulseWidth, sizePulseCount);

        //And finally add it to the correct location in our maps
        tracker.addNewTrailObject(objectToAdd);
    }


    // DEPRECATED

    /**
     * Spawns a trail piece, which links up with other pieces with the same ID
     * to form a smooth trail. This function has all available functions; if you
     * just want to spawn a normal trail without all the extra configuration involved,
     * use AddTrailMemberSimple instead.
     *
     * @param linkedEntity         The entity this trail is attached to, used for cutting trails.
     *                             Can be Null, but that should really only be done in weird, edge-case scenarios
     * @param ID                   The ID for this specific trail. Preferably get this from getUniqueID,
     *                             but it's not required: just expect very weird results if you don't
     * @param sprite               Which sprite to draw for this trail: do *not* change this halfway through a trail,
     *                             as that will split it into two trails
     * @param position             Starting position for this piece of trail
     * @param startSpeed           The starting speed, in SU, this trail piece is moving at. The trail piece smoothly
     *                             transitions from its startSpeed to its endSpeed over its duration
     * @param endSpeed             The ending speed, in SU, this trail piece is moving at. The trail piece smoothly
     *                             transitions from its startSpeed to its endSpeed over its duration
     * @param angle                Which angle this piece of trail has in degrees; determines which direction it moves,
     *                             and which direction its size is measured over
     * @param startAngularVelocity The angular velocity this trail piece has when spawned. The angular velocity
     *                             of a trail piece smoothly transitions from startAngularVelocity to
     *                             endAngularVelocity over its duration
     * @param endAngularVelocity   The angular velocity this trail piece has just before disappearing.
     *                             The angular velocity of a trail piece smoothly transitions from
     *                             startAngularVelocity to endAngularVelocity over its duration
     * @param startSize            The starting size (or rather width) this piece of trail has. Measured in SU. A trail
     *                             smoothly transitions from its startSize to its endSize over its duration
     * @param endSize              The ending size (or rather width) this trail piece has. Measured in SU. A trail smoothly
     *                             transitions from its startSize to its endSize over its duration
     * @param startColor           The color this piece of trail has when spawned. Can be changed in the middle of a trail,
     *                             and will blend smoothly between pieces. Ignores alpha component entirely. Each trail piece
     *                             smoothly transitions from startColor to endColor over its duration
     * @param endColor             The color this piece of trail has just before disappearing. Can be changed in the middle of a
     *                             trail, and will blend smoothly between pieces. Ignores alpha component entirely. Each trail piece
     *                             smoothly transitions from startColor to endColor over its duration
     * @param opacity              The starting opacity of this piece of trail. Is a value between 0f and 1f. The opacity
     *                             gradually approaches 0f over the trail's duration
     * @param inDuration           How long this trail spends "fading in"; for this many seconds, the opacity of the trail piece
     *                             steadily increases until reaching "opacity". A trail's total duration is
     *                             inDuration + mainDuration + outDuration
     * @param mainDuration         How long a trail uses its maximum opacity. A trail's total duration is
     *                             inDuration + mainDuration + outDuration
     * @param outDuration          How long a trail spends "fading out"; over this many seconds at the end of the trail's
     *                             duration, its opacity goes from "opacity" to 0f. A trail's total duration is
     *                             inDuration + mainDuration + outDuration
     * @param additive             Whether this trail will use additive blending or not. Does not support being changed in
     *                             the middle of a trail
     * @param textureLoopLength    How many SU it takes for the texture to loop. Should preferably be non-zero. If the
     *                             trail is not supposed to loop, put this as -1f
     * @param textureScrollSpeed   How fast, and in which direction, the texture scrolls over the trail. Defined so that
     *                             1000 means scrolling the entire texture length once per second, and 2000 means
     *                             scrolling the entire texture length twice per second
     * @param offsetVelocity       The offset velocity of the trail; this is an additional velocity that is
     *                             unaffected by rotation and facing, and will never change over the trail's lifetime
     * @param advancedOptions      The most unique and special options go in a special Map<> here. Be careful to input the
     *                             correct type values and use the right data keys. Any new features will be added here to
     *                             keep compatibility with old mod versions. Can be null. Currently supported keys:
     *                             "SIZE_PULSE_WIDTH" :  Float - How much additional width the trail gains each "pulse", at
     *                             most. Used in conjunction with SIZE_PULSE_COUNT
     *                             "SIZE_PULSE_COUNT" :  Integer - How many times the trail "pulses" its width over its
     *                             lifetime. Used in conjunction with SIZE_PULSE_WIDTH
     *                             "FORWARD_PROPAGATION" :  Boolean - If the trail uses the legacy render method of
     *                             "forward propagation". Used to be the default. CANNOT be
     *                             changed mid-trail
     * @param layerToRenderOn      Which combat layer to render the trail on. All available layers are specified in
     *                             CombatEngineLayers. Old behaviour was CombatEngineLayers.BELOW_INDICATORS_LAYER.
     *                             CANNOT change mid-trail, under any circumstance
     * @param frameOffsetMult      The per-frame multiplier for the per-frame velocity offset magnitude. Used to finely
     *                             adjust trail offset at different speeds. Default: 1f
     */
    public static void addTrailMemberAdvanced(
            CombatEntityAPI linkedEntity, float ID, SpriteAPI sprite,
            Vector2f position, float startSpeed, float endSpeed,
            float angle, float startAngularVelocity, float endAngularVelocity,
            float startSize, float endSize,
            Color startColor, Color endColor, float opacity,
            float inDuration, float mainDuration, float outDuration,
            boolean additive,
            float textureLoopLength, float textureScrollSpeed,
            @Nullable Vector2f offsetVelocity, @Nullable Map<String, Object> advancedOptions,
            @Nullable CombatEngineLayers layerToRenderOn, float frameOffsetMult) {

        //Converts our additive/non-additive option to true openGL stuff
        int blendModeSRC = GL_SRC_ALPHA;
        int blendModeDEST = additive ? GL_ONE : GL_ONE_MINUS_SRC_ALPHA;

        addTrailMemberAdvanced(linkedEntity, ID, sprite, position, startSpeed, endSpeed, angle, startAngularVelocity, endAngularVelocity, startSize, endSize,
                startColor, endColor, opacity, inDuration, mainDuration, outDuration, blendModeSRC, blendModeDEST, textureLoopLength, textureScrollSpeed,
                offsetVelocity, advancedOptions, layerToRenderOn, frameOffsetMult);
    }

    /**
     * Spawns a trail piece, which links up with other pieces with the same ID
     * to form a smooth trail. This function has all available functions; if you
     * just want to spawn a normal trail without all the extra configuration involved,
     * use AddTrailMemberSimple instead.
     *
     * @param linkedEntity         The entity this trail is attached to, used for cutting trails.
     *                             Can be Null, but that should really only be done in weird, edge-case scenarios
     * @param ID                   The ID for this specific trail. Preferably get this from getUniqueID,
     *                             but it's not required: just expect very weird results if you don't
     * @param sprite               Which sprite to draw for this trail: do *not* change this halfway through a trail,
     *                             as that will split it into two trails
     * @param position             Starting position for this piece of trail
     * @param startSpeed           The starting speed, in SU, this trail piece is moving at. The trail piece smoothly
     *                             transitions from its startSpeed to its endSpeed over its duration
     * @param endSpeed             The ending speed, in SU, this trail piece is moving at. The trail piece smoothly
     *                             transitions from its startSpeed to its endSpeed over its duration
     * @param angle                Which angle this piece of trail has in degrees; determines which direction it moves,
     *                             and which direction its size is measured over
     * @param startAngularVelocity The angular velocity this trail piece has when spawned. The angular velocity
     *                             of a trail piece smoothly transitions from startAngularVelocity to
     *                             endAngularVelocity over its duration
     * @param endAngularVelocity   The angular velocity this trail piece has just before disappearing.
     *                             The angular velocity of a trail piece smoothly transitions from
     *                             startAngularVelocity to endAngularVelocity over its duration
     * @param startSize            The starting size (or rather width) this piece of trail has. Measured in SU. A trail
     *                             smoothly transitions from its startSize to its endSize over its duration
     * @param endSize              The ending size (or rather width) this trail piece has. Measured in SU. A trail smoothly
     *                             transitions from its startSize to its endSize over its duration
     * @param startColor           The color this piece of trail has when spawned. Can be changed in the middle of a trail,
     *                             and will blend smoothly between pieces. Ignores alpha component entirely. Each trail piece
     *                             smoothly transitions from startColor to endColor over its duration
     * @param endColor             The color this piece of trail has just before disappearing. Can be changed in the middle of a
     *                             trail, and will blend smoothly between pieces. Ignores alpha component entirely. Each trail piece
     *                             smoothly transitions from startColor to endColor over its duration
     * @param opacity              The starting opacity of this piece of trail. Is a value between 0f and 1f. The opacity
     *                             gradually approaches 0f over the trail's duration
     * @param inDuration           How long this trail spends "fading in"; for this many seconds, the opacity of the trail piece
     *                             steadily increases until reaching "opacity". A trail's total duration is
     *                             inDuration + mainDuration + outDuration
     * @param mainDuration         How long a trail uses its maximum opacity. A trail's total duration is
     *                             inDuration + mainDuration + outDuration
     * @param outDuration          How long a trail spends "fading out"; over this many seconds at the end of the trail's
     *                             duration, its opacity goes from "opacity" to 0f. A trail's total duration is
     *                             inDuration + mainDuration + outDuration
     * @param blendModeSRC         Which SRD openGL blend mode to use for the trail. If you are unsure of what this means, just
     *                             put it as GL_SRC_ALPHA
     * @param blendModeDEST        Which DEST openGL blend mode to use for the trail. If you are unsure of what this means,
     *                             put it as GL_ONE_MINUS_SRC_ALPHA for normal blending and GL_ONE for additive blending
     * @param textureLoopLength    How many SU it takes for the texture to loop. Should preferably be non-zero. If the
     *                             trail is not supposed to loop, put this as -1f
     * @param textureScrollSpeed   How fast, and in which direction, the texture scrolls over the trail. Defined so that
     *                             1000 means scrolling the entire texture length once per second, and 2000 means
     *                             scrolling the entire texture length twice per second
     * @param offsetVelocity       The offset velocity of the trail; this is an additional velocity that is
     *                             unaffected by rotation and facing, and will never change over the trail's lifetime
     * @param advancedOptions      The most unique and special options go in a special Map<> here. Be careful to input the
     *                             correct type values and use the right data keys. Any new features will be added here to
     *                             keep compatibility with old mod versions. Can be null. Currently supported keys:
     *                             "SIZE_PULSE_WIDTH" :  Float - How much additional width the trail gains each "pulse", at
     *                             most. Used in conjunction with SIZE_PULSE_COUNT
     *                             "SIZE_PULSE_COUNT" :  Integer - How many times the trail "pulses" its width over its
     *                             lifetime. Used in conjunction with SIZE_PULSE_WIDTH
     *                             "FORWARD_PROPAGATION" :  Boolean - If the trail uses the legacy render method of
     *                             "forward propagation". Used to be the default. CANNOT be
     *                             changed mid-trail
     * @param layerToRenderOn      Which combat layer to render the trail on. All available layers are specified in
     *                             CombatEngineLayers. Old behaviour was CombatEngineLayers.BELOW_INDICATORS_LAYER.
     *                             CANNOT change mid-trail, under any circumstance
     * @param frameOffsetMult      The per-frame multiplier for the per-frame velocity offset magnitude. Used to finely
     *                             adjust trail offset at different speeds. Default: 1f
     */
    @Deprecated
    public static void addTrailMemberAdvanced(
            CombatEntityAPI linkedEntity, float ID, SpriteAPI sprite,
            Vector2f position, float startSpeed, float endSpeed,
            float angle, float startAngularVelocity, float endAngularVelocity,
            float startSize, float endSize,
            Color startColor, Color endColor, float opacity,
            float inDuration, float mainDuration, float outDuration,
            int blendModeSRC, int blendModeDEST,
            float textureLoopLength, float textureScrollSpeed,
            @Nullable Vector2f offsetVelocity, @Nullable Map<String, Object> advancedOptions,
            @Nullable CombatEngineLayers layerToRenderOn, float frameOffsetMult) {

        //First, find the plugin, and if it doesn't exist do nothing
        MagicTrailPlugin plugin = getPlugin();
        if (plugin == null) return;


        Vector2f offset = new Vector2f();
        if (offsetVelocity != null) {
            offset = offsetVelocity;
        }

        CombatEngineLayers layer = CombatEngineLayers.CONTRAILS_LAYER;
        if (layerToRenderOn != null) {
            layer = layerToRenderOn;
        }

        float mult = 1;
        if (frameOffsetMult != 0) {
            mult = frameOffsetMult;
        }

        //Finds the correct maps, and ensures they are actually instantiated [and adds our ID to the cutting map]
        MagicTrailTracker tracker = addOrGetTrailTracker(plugin, ID, linkedEntity, layer, sprite, false);

        //Adjusts scroll speed to our most recent trail's value
        tracker.scrollSpeed = textureScrollSpeed;

        //--Reads in our special options, if we have any--
        float sizePulseWidth = 0f;
        int sizePulseCount = 0;
        if (advancedOptions != null) {
            if (advancedOptions.get("SIZE_PULSE_WIDTH") instanceof Float) {
                sizePulseWidth = (Float) advancedOptions.get("SIZE_PULSE_WIDTH");
            }
            if (advancedOptions.get("SIZE_PULSE_COUNT") instanceof Integer) {
                sizePulseCount = (Integer) advancedOptions.get("SIZE_PULSE_COUNT");
            }
            if (advancedOptions.get("FORWARD_PROPAGATION") instanceof Boolean && (boolean) advancedOptions.get("FORWARD_PROPAGATION")) {
                tracker.usesForwardPropagation = true;
            }
        }
        //--End of special options--

        //Offset tweaker to fix single frame delay for lateral movement
        Vector2f correctedPosition = new Vector2f(position);
        if (linkedEntity instanceof DamagingProjectileAPI) {
            DamagingProjectileAPI proj = (DamagingProjectileAPI) linkedEntity;

            Vector2f shipVelPerAdvance = (Vector2f) new Vector2f(proj.getSource().getVelocity()).scale(Global.getCombatEngine().getElapsedInLastFrame());
            shipVelPerAdvance.scale(mult);
            Vector2f.sub(position, shipVelPerAdvance, correctedPosition);
        }

        //Creates the custom object we want
        MagicTrailObject objectToAdd = new MagicTrailObject(inDuration, mainDuration, outDuration, startSize, endSize, startAngularVelocity, endAngularVelocity,
                opacity, blendModeSRC, blendModeDEST, startSpeed, endSpeed, startColor, endColor, angle, correctedPosition, textureLoopLength, 0, offset,
                sizePulseWidth, sizePulseCount);

        //And finally add it to the correct location in our maps
        tracker.addNewTrailObject(objectToAdd);
    }

    /**
     * Spawns a trail piece, which links up with other pieces with the same ID
     * to form a smooth trail. This function has all available functions; if you
     * just want to spawn a normal trail without all the extra configuration involved,
     * use AddTrailMemberSimple instead.
     *
     * @param linkedEntity         The entity this trail is attached to, used for cutting trails.
     *                             Can be Null, but that should really only be done in weird, edge-case scenarios
     * @param ID                   The ID for this specific trail. Preferably get this from getUniqueID,
     *                             but it's not required: just expect very weird results if you don't
     * @param sprite               Which sprite to draw for this trail: do *not* change this halfway through a trail,
     *                             as that will split it into two trails
     * @param position             Starting position for this piece of trail
     * @param startSpeed           The starting speed, in SU, this trail piece is moving at. The trail piece smoothly
     *                             transitions from its startSpeed to its endSpeed over its duration
     * @param endSpeed             The ending speed, in SU, this trail piece is moving at. The trail piece smoothly
     *                             transitions from its startSpeed to its endSpeed over its duration
     * @param angle                Which angle this piece of trail has in degrees; determines which direction it moves,
     *                             and which direction its size is measured over
     * @param startAngularVelocity The angular velocity this trail piece has when spawned. The angular velocity
     *                             of a trail piece smoothly transitions from startAngularVelocity to
     *                             endAngularVelocity over its duration
     * @param endAngularVelocity   The angular velocity this trail piece has just before disappearing.
     *                             The angular velocity of a trail piece smoothly transitions from
     *                             startAngularVelocity to endAngularVelocity over its duration
     * @param startSize            The starting size (or rather width) this piece of trail has. Measured in SU. A trail
     *                             smoothly transitions from its startSize to its endSize over its duration
     * @param endSize              The ending size (or rather width) this trail piece has. Measured in SU. A trail smoothly
     *                             transitions from its startSize to its endSize over its duration
     * @param startColor           The color this piece of trail has when spawned. Can be changed in the middle of a trail,
     *                             and will blend smoothly between pieces. Ignores alpha component entirely. Each trail piece
     *                             smoothly transitions from startColor to endColor over its duration
     * @param endColor             The color this piece of trail has just before disappearing. Can be changed in the middle of a
     *                             trail, and will blend smoothly between pieces. Ignores alpha component entirely. Each trail piece
     *                             smoothly transitions from startColor to endColor over its duration
     * @param opacity              The starting opacity of this piece of trail. Is a value between 0f and 1f. The opacity
     *                             gradually approaches 0f over the trail's duration
     * @param inDuration           How long this trail spends "fading in"; for this many seconds, the opacity of the trail piece
     *                             steadily increases until reaching "opacity". A trail's total duration is
     *                             inDuration + mainDuration + outDuration
     * @param mainDuration         How long a trail uses its maximum opacity. A trail's total duration is
     *                             inDuration + mainDuration + outDuration
     * @param outDuration          How long a trail spends "fading out"; over this many seconds at the end of the trail's
     *                             duration, its opacity goes from "opacity" to 0f. A trail's total duration is
     *                             inDuration + mainDuration + outDuration
     * @param blendModeSRC         Which SRD openGL blend mode to use for the trail. If you are unsure of what this means, just
     *                             put it as GL_SRC_ALPHA
     * @param blendModeDEST        Which DEST openGL blend mode to use for the trail. If you are unsure of what this means,
     *                             put it as GL_ONE_MINUS_SRC_ALPHA for normal blending and GL_ONE for additive blending
     * @param textureLoopLength    How many SU it takes for the texture to loop. Should preferably be non-zero. If the
     *                             trail is not supposed to loop, put this as -1f
     * @param textureScrollSpeed   How fast, and in which direction, the texture scrolls over the trail. Defined so that
     *                             1000 means scrolling the entire texture length once per second, and 2000 means
     *                             scrolling the entire texture length twice per second
     * @param offsetVelocity       The offset velocity of the trail; this is an additional velocity that is
     *                             unaffected by rotation and facing, and will never change over the trail's lifetime
     * @param advancedOptions      The most unique and special options go in a special Map<> here. Be careful to input the
     *                             correct type values and use the right data keys. Any new features will be added here to
     *                             keep compatibility with old mod versions. Can be null. Currently supported keys:
     *                             "SIZE_PULSE_WIDTH" :  Float - How much additional width the trail gains each "pulse", at
     *                             most. Used in conjunction with SIZE_PULSE_COUNT
     *                             "SIZE_PULSE_COUNT" :  Integer - How many times the trail "pulses" its width over its
     *                             lifetime. Used in conjunction with SIZE_PULSE_WIDTH
     *                             "FORWARD_PROPAGATION" :  Boolean - If the trail uses the legacy render method of
     *                             "forward propagation". Used to be the default. CANNOT be
     *                             changed mid-trail
     */
    @Deprecated
    public static void addTrailMemberAdvanced(
            CombatEntityAPI linkedEntity, float ID, SpriteAPI sprite,
            Vector2f position, float startSpeed, float endSpeed,
            float angle, float startAngularVelocity, float endAngularVelocity,
            float startSize, float endSize,
            Color startColor, Color endColor, float opacity,
            float inDuration, float mainDuration, float outDuration,
            int blendModeSRC, int blendModeDEST,
            float textureLoopLength, float textureScrollSpeed,
            Vector2f offsetVelocity, @Nullable Map<String, Object> advancedOptions) {
        addTrailMemberAdvanced(linkedEntity, ID, sprite, position, startSpeed, endSpeed, angle, startAngularVelocity, endAngularVelocity, startSize, endSize,
                startColor, endColor, opacity, inDuration, mainDuration, outDuration, blendModeSRC, blendModeDEST, textureLoopLength, textureScrollSpeed,
                offsetVelocity, advancedOptions, CombatEngineLayers.CONTRAILS_LAYER, 1f);
    }

    /**
     * Spawns a trail piece, which links up with other pieces with the same ID
     * to form a smooth trail. This function has all available functions; if you
     * just want to spawn a normal trail without all the extra configuration involved,
     * use AddTrailMemberSimple instead.
     *
     * @param linkedEntity         The entity this trail is attached to, used for cutting trails.
     *                             Can be Null, but that should really only be done in weird, edge-case scenarios
     * @param ID                   The ID for this specific trail. Preferably get this from getUniqueID,
     *                             but it's not required: just expect very weird results if you don't
     * @param sprite               Which sprite to draw for this trail: do *not* change this halfway through a trail,
     *                             as that will split it into two trails
     * @param position             Starting position for this piece of trail
     * @param startSpeed           The starting speed, in SU, this trail piece is moving at. The trail piece smoothly
     *                             transitions from its startSpeed to its endSpeed over its duration
     * @param endSpeed             The ending speed, in SU, this trail piece is moving at. The trail piece smoothly
     *                             transitions from its startSpeed to its endSpeed over its duration
     * @param angle                Which angle this piece of trail has in degrees; determines which direction it moves,
     *                             and which direction its size is measured over
     * @param startAngularVelocity The angular velocity this trail piece has when spawned. The angular velocity
     *                             of a trail piece smoothly transitions from startAngularVelocity to
     *                             endAngularVelocity over its duration
     * @param endAngularVelocity   The angular velocity this trail piece has just before disappearing.
     *                             The angular velocity of a trail piece smoothly transitions from
     *                             startAngularVelocity to endAngularVelocity over its duration
     * @param startSize            The starting size (or rather width) this piece of trail has. Measured in SU. A trail
     *                             smoothly transitions from its startSize to its endSize over its duration
     * @param endSize              The ending size (or rather width) this trail piece has. Measured in SU. A trail smoothly
     *                             transitions from its startSize to its endSize over its duration
     * @param startColor           The color this piece of trail has when spawned. Can be changed in the middle of a trail,
     *                             and will blend smoothly between pieces. Ignores alpha component entirely. Each trail piece
     *                             smoothly transitions from startColor to endColor over its duration
     * @param endColor             The color this piece of trail has just before disappearing. Can be changed in the middle of a
     *                             trail, and will blend smoothly between pieces. Ignores alpha component entirely. Each trail piece
     *                             smoothly transitions from startColor to endColor over its duration
     * @param opacity              The starting opacity of this piece of trail. Is a value between 0f and 1f. The opacity
     *                             gradually approaches 0f over the trail's duration
     * @param inDuration           How long this trail spends "fading in"; for this many seconds, the opacity of the trail piece
     *                             steadily increases until reaching "opacity". A trail's total duration is
     *                             inDuration + mainDuration + outDuration
     * @param mainDuration         How long a trail uses its maximum opacity. A trail's total duration is
     *                             inDuration + mainDuration + outDuration
     * @param outDuration          How long a trail spends "fading out"; over this many seconds at the end of the trail's
     *                             duration, its opacity goes from "opacity" to 0f. A trail's total duration is
     *                             inDuration + mainDuration + outDuration
     * @param blendModeSRC         Which SRD openGL blend mode to use for the trail. If you are unsure of what this means, just
     *                             put it as GL_SRC_ALPHA
     * @param blendModeDEST        Which DEST openGL blend mode to use for the trail. If you are unsure of what this means,
     *                             put it as GL_ONE_MINUS_SRC_ALPHA for normal blending and GL_ONE for additive blending
     * @param textureLoopLength    How many SU it takes for the texture to loop. Should preferably be non-zero. If the
     *                             trail is not supposed to loop, put this as -1f
     * @param textureScrollSpeed   How fast, and in which direction, the texture scrolls over the trail. Defined so that
     *                             1000 means scrolling the entire texture length once per second, and 2000 means
     *                             scrolling the entire texture length twice per second
     * @param offsetVelocity       The offset velocity of the trail; this is an additional velocity that is
     *                             unaffected by rotation and facing, and will never change over the trail's lifetime
     * @param advancedOptions      The most unique and special options go in a special Map<> here. Be careful to input the
     *                             correct type values and use the right data keys. Any new features will be added here to
     *                             keep compatibility with old mod versions. Can be null. Currently supported keys:
     *                             "SIZE_PULSE_WIDTH" :  Float - How much additional width the trail gains each "pulse", at
     *                             most. Used in conjunction with SIZE_PULSE_COUNT
     *                             "SIZE_PULSE_COUNT" :  Integer - How many times the trail "pulses" its width over its
     *                             lifetime. Used in conjunction with SIZE_PULSE_WIDTH
     *                             "FORWARD_PROPAGATION" :  Boolean - If the trail uses the legacy render method of
     *                             "forward propagation". Used to be the default. CANNOT be
     *                             changed mid-trail
     * @param layerToRenderOn      Which combat layer to render the trail on. All available layers are specified in
     *                             CombatEngineLayers. Old behaviour was CombatEngineLayers.BELOW_INDICATORS_LAYER.
     *                             CANNOT change mid-trail, under any circumstance
     */
    @Deprecated
    public static void addTrailMemberAdvanced(
            CombatEntityAPI linkedEntity, float ID, SpriteAPI sprite,
            Vector2f position, float startSpeed, float endSpeed,
            float angle, float startAngularVelocity, float endAngularVelocity,
            float startSize, float endSize,
            Color startColor, Color endColor, float opacity,
            float inDuration, float mainDuration, float outDuration,
            int blendModeSRC, int blendModeDEST,
            float textureLoopLength, float textureScrollSpeed,
            Vector2f offsetVelocity, @Nullable Map<String, Object> advancedOptions,
            CombatEngineLayers layerToRenderOn) {
        //First, find the plugin, and if it doesn't exist do nothing
        MagicTrailPlugin plugin = getPlugin();
        if (plugin == null) return;


        //Finds the correct maps, and ensures they are actually instantiated [and adds our ID to the cutting map]
        MagicTrailTracker tracker = addOrGetTrailTracker(plugin, ID, linkedEntity, layerToRenderOn, sprite, false);

        //Adjusts scroll speed to our most recent trail's value
        tracker.scrollSpeed = textureScrollSpeed;

        //--Reads in our special options, if we have any--
        float sizePulseWidth = 0f;
        int sizePulseCount = 0;
        if (advancedOptions != null) {
            if (advancedOptions.get("SIZE_PULSE_WIDTH") instanceof Float) {
                sizePulseWidth = (Float) advancedOptions.get("SIZE_PULSE_WIDTH");
            }
            if (advancedOptions.get("SIZE_PULSE_COUNT") instanceof Integer) {
                sizePulseCount = (Integer) advancedOptions.get("SIZE_PULSE_COUNT");
            }
            if (advancedOptions.get("FORWARD_PROPAGATION") instanceof Boolean && (boolean) advancedOptions.get("FORWARD_PROPAGATION")) {
                tracker.usesForwardPropagation = true;
            }
        }
        //--End of special options--

        //Creates the custom object we want
        MagicTrailObject objectToAdd = new MagicTrailObject(inDuration, mainDuration, outDuration, startSize, endSize, startAngularVelocity, endAngularVelocity,
                opacity, blendModeSRC, blendModeDEST, startSpeed, endSpeed, startColor, endColor, angle, position, textureLoopLength, 0, offsetVelocity,
                sizePulseWidth, sizePulseCount);

        //And finally add it to the correct location in our maps
        tracker.addNewTrailObject(objectToAdd);
    }


    // ANIMATED DECLARATION
    // ANIMATED DECLARATION
    // ANIMATED DECLARATION
    // ANIMATED DECLARATION
    // ANIMATED DECLARATION
    // ANIMATED DECLARATION


    /**
     * Spawns a trail piece, which links up with other pieces with the same ID
     * to form a smooth trail. This function is similar to the Advanced function, but
     * allows the trail to change its texture each time a new member is added. It will
     * always use the texture of the most recently-added member. If the texture is not
     * supposed to be animated, do NOT use this function: it runs notably slower.
     *
     * @param linkedEntity         The entity this trail is attached to, used for cutting trails.
     *                             Can be Null, but that should really only be done in weird, edge-case scenarios
     * @param ID                   The ID for this specific trail. Preferably get this from getUniqueID,
     *                             but it's not required: just expect very weird results if you don't
     * @param sprite               Which sprite to draw for this trail: if changed mid-trail, the entire trail uses the
     *                             new sprite.
     * @param position             Starting position for this piece of trail
     * @param startSpeed           The starting speed, in SU, this trail piece is moving at. The trail piece smoothly
     *                             transitions from its startSpeed to its endSpeed over its duration
     * @param endSpeed             The ending speed, in SU, this trail piece is moving at. The trail piece smoothly
     *                             transitions from its startSpeed to its endSpeed over its duration
     * @param angle                Which angle this piece of trail has in degrees; determines which direction it moves,
     *                             and which direction its size is measured over
     * @param startAngularVelocity The angular velocity this trail piece has when spawned. The angular velocity
     *                             of a trail piece smoothly transitions from startAngularVelocity to
     *                             endAngularVelocity over its duration
     * @param endAngularVelocity   The angular velocity this trail piece has just before disappearing.
     *                             The angular velocity of a trail piece smoothly transitions from
     *                             startAngularVelocity to endAngularVelocity over its duration
     * @param startSize            The starting size (or rather width) this piece of trail has. Measured in SU. A trail
     *                             smoothly transitions from its startSize to its endSize over its duration
     * @param endSize              The ending size (or rather width) this trail piece has. Measured in SU. A trail smoothly
     *                             transitions from its startSize to its endSize over its duration
     * @param startColor           The color this piece of trail has when spawned. Can be changed in the middle of a trail,
     *                             and will blend smoothly between pieces. Ignores alpha component entirely. Each trail piece
     *                             smoothly transitions from startColor to endColor over its duration
     * @param endColor             The color this piece of trail has just before disappearing. Can be changed in the middle of a
     *                             trail, and will blend smoothly between pieces. Ignores alpha component entirely. Each trail piece
     *                             smoothly transitions from startColor to endColor over its duration
     * @param opacity              The starting opacity of this piece of trail. Is a value between 0f and 1f. The opacity
     *                             gradually approaches 0f over the trail's duration
     * @param inDuration           How long this trail spends "fading in"; for this many seconds, the opacity of the trail piece
     *                             steadily increases until reaching "opacity". A trail's total duration is
     *                             inDuration + mainDuration + outDuration
     * @param mainDuration         How long a trail uses its maximum opacity. A trail's total duration is
     *                             inDuration + mainDuration + outDuration
     * @param outDuration          How long a trail spends "fading out"; over this many seconds at the end of the trail's
     *                             duration, its opacity goes from "opacity" to 0f. A trail's total duration is
     *                             inDuration + mainDuration + outDuration
     * @param additive             Whether this trail will use additive blending or not. Does not support being changed in
     *                             the middle of a trail
     * @param textureLoopLength    How many SU it takes for the texture to loop. Should preferably be non-zero. If the
     *                             trail is not supposed to loop, put this as -1f
     * @param textureScrollSpeed   How fast, and in which direction, the texture scrolls over the trail. Defined so that
     *                             1000 means scrolling the entire texture length once per second, and 2000 means
     *                             scrolling the entire texture length twice per second
     * @param textureOffset        Optional texture offset to prevent repetitions between trails, default:0, fixed random offset: -1;
     * @param offsetVelocity       The offset velocity of the trail; this is an additional velocity that is
     *                             unaffected by rotation and facing, and will never change over the trail's lifetime
     * @param advancedOptions      The most unique and special options go in a special Map<> here. Be careful to input the
     *                             correct type values and use the right data keys. Any new features will be added here to
     *                             keep compatibility with old mod versions. Can be null. Currently supported keys:
     *                             "SIZE_PULSE_WIDTH" :  Float - How much additional width the trail gains each "pulse", at
     *                             most. Used in conjunction with SIZE_PULSE_COUNT
     *                             "SIZE_PULSE_COUNT" :  Integer - How many times the trail "pulses" its width over its
     *                             lifetime. Used in conjunction with SIZE_PULSE_WIDTH
     *                             "FORWARD_PROPAGATION" :  Boolean - If the trail uses the legacy render method of
     *                             "forward propagation". Used to be the default. CANNOT be
     *                             changed mid-trail
     * @param layerToRenderOn      Which combat layer to render the trail on. All available layers are specified in
     *                             CombatEngineLayers. Old behaviour was CombatEngineLayers.BELOW_INDICATORS_LAYER.
     *                             CANNOT change mid-trail, under any circumstance
     * @param frameOffsetMult      The per-frame multiplier for the per-frame velocity offset magnitude. Used to finely
     *                             adjust trail offset at different speeds. Default: 1f
     */
    public static void addTrailMemberAnimated(
            CombatEntityAPI linkedEntity, float ID, SpriteAPI sprite,
            Vector2f position, float startSpeed, float endSpeed,
            float angle, float startAngularVelocity, float endAngularVelocity,
            float startSize, float endSize,
            Color startColor, Color endColor, float opacity,
            float inDuration, float mainDuration, float outDuration,
            boolean additive,
            float textureLoopLength, float textureScrollSpeed, float textureOffset,
            Vector2f offsetVelocity, @Nullable Map<String, Object> advancedOptions,
            @Nullable CombatEngineLayers layerToRenderOn, float frameOffsetMult) {

        //Converts our additive/non-additive option to true openGL stuff
        int blendModeSRC = GL_SRC_ALPHA;
        int blendModeDEST = additive ? GL_ONE : GL_ONE_MINUS_SRC_ALPHA;

        addTrailMemberAnimated(linkedEntity, ID, sprite, position, startSpeed, endSpeed, angle, startAngularVelocity, endAngularVelocity, startSize, endSize, startColor,
                endColor, opacity, inDuration, mainDuration, outDuration, blendModeSRC, blendModeDEST, textureLoopLength, textureScrollSpeed, textureOffset, offsetVelocity,
                advancedOptions, layerToRenderOn, frameOffsetMult);
    }


    /**
     * Spawns a trail piece, which links up with other pieces with the same ID
     * to form a smooth trail. This function is similar to the Advanced function, but
     * allows the trail to change its texture each time a new member is added. It will
     * always use the texture of the most recently-added member. If the texture is not
     * supposed to be animated, do NOT use this function: it runs notably slower.
     *
     * @param linkedEntity         The entity this trail is attached to, used for cutting trails.
     *                             Can be Null, but that should really only be done in weird, edge-case scenarios
     * @param ID                   The ID for this specific trail. Preferably get this from getUniqueID,
     *                             but it's not required: just expect very weird results if you don't
     * @param sprite               Which sprite to draw for this trail: if changed mid-trail, the entire trail uses the
     *                             new sprite.
     * @param position             Starting position for this piece of trail
     * @param startSpeed           The starting speed, in SU, this trail piece is moving at. The trail piece smoothly
     *                             transitions from its startSpeed to its endSpeed over its duration
     * @param endSpeed             The ending speed, in SU, this trail piece is moving at. The trail piece smoothly
     *                             transitions from its startSpeed to its endSpeed over its duration
     * @param angle                Which angle this piece of trail has in degrees; determines which direction it moves,
     *                             and which direction its size is measured over
     * @param startAngularVelocity The angular velocity this trail piece has when spawned. The angular velocity
     *                             of a trail piece smoothly transitions from startAngularVelocity to
     *                             endAngularVelocity over its duration
     * @param endAngularVelocity   The angular velocity this trail piece has just before disappearing.
     *                             The angular velocity of a trail piece smoothly transitions from
     *                             startAngularVelocity to endAngularVelocity over its duration
     * @param startSize            The starting size (or rather width) this piece of trail has. Measured in SU. A trail
     *                             smoothly transitions from its startSize to its endSize over its duration
     * @param endSize              The ending size (or rather width) this trail piece has. Measured in SU. A trail smoothly
     *                             transitions from its startSize to its endSize over its duration
     * @param startColor           The color this piece of trail has when spawned. Can be changed in the middle of a trail,
     *                             and will blend smoothly between pieces. Ignores alpha component entirely. Each trail piece
     *                             smoothly transitions from startColor to endColor over its duration
     * @param endColor             The color this piece of trail has just before disappearing. Can be changed in the middle of a
     *                             trail, and will blend smoothly between pieces. Ignores alpha component entirely. Each trail piece
     *                             smoothly transitions from startColor to endColor over its duration
     * @param opacity              The starting opacity of this piece of trail. Is a value between 0f and 1f. The opacity
     *                             gradually approaches 0f over the trail's duration
     * @param inDuration           How long this trail spends "fading in"; for this many seconds, the opacity of the trail piece
     *                             steadily increases until reaching "opacity". A trail's total duration is
     *                             inDuration + mainDuration + outDuration
     * @param mainDuration         How long a trail uses its maximum opacity. A trail's total duration is
     *                             inDuration + mainDuration + outDuration
     * @param outDuration          How long a trail spends "fading out"; over this many seconds at the end of the trail's
     *                             duration, its opacity goes from "opacity" to 0f. A trail's total duration is
     *                             inDuration + mainDuration + outDuration
     * @param blendModeSRC         Which SRD openGL blend mode to use for the trail. If you are unsure of what this means, just
     *                             put it as GL_SRC_ALPHA
     * @param blendModeDEST        Which DEST openGL blend mode to use for the trail. If you are unsure of what this means,
     *                             put it as GL_ONE_MINUS_SRC_ALPHA for normal blending and GL_ONE for additive blending
     * @param textureLoopLength    How many SU it takes for the texture to loop. Should preferably be non-zero. If the
     *                             trail is not supposed to loop, put this as -1f
     * @param textureScrollSpeed   How fast, and in which direction, the texture scrolls over the trail. Defined so that
     *                             1000 means scrolling the entire texture length once per second, and 2000 means
     *                             scrolling the entire texture length twice per second
     * @param textureOffset        Optional texture offset to prevent repetitions between trails, default:0, fixed random offset: -1;
     * @param offsetVelocity       The offset velocity of the trail; this is an additional velocity that is
     *                             unaffected by rotation and facing, and will never change over the trail's lifetime
     * @param advancedOptions      The most unique and special options go in a special Map<> here. Be careful to input the
     *                             correct type values and use the right data keys. Any new features will be added here to
     *                             keep compatibility with old mod versions. Can be null. Currently supported keys:
     *                             "SIZE_PULSE_WIDTH" :  Float - How much additional width the trail gains each "pulse", at
     *                             most. Used in conjunction with SIZE_PULSE_COUNT
     *                             "SIZE_PULSE_COUNT" :  Integer - How many times the trail "pulses" its width over its
     *                             lifetime. Used in conjunction with SIZE_PULSE_WIDTH
     *                             "FORWARD_PROPAGATION" :  Boolean - If the trail uses the legacy render method of
     *                             "forward propagation". Used to be the default. CANNOT be
     *                             changed mid-trail
     * @param layerToRenderOn      Which combat layer to render the trail on. All available layers are specified in
     *                             CombatEngineLayers. Old behaviour was CombatEngineLayers.BELOW_INDICATORS_LAYER.
     *                             CANNOT change mid-trail, under any circumstance
     * @param frameOffsetMult      The per-frame multiplier for the per-frame velocity offset magnitude. Used to finely
     *                             adjust trail offset at different speeds
     */
    public static void addTrailMemberAnimated(
            CombatEntityAPI linkedEntity, float ID, SpriteAPI sprite,
            Vector2f position, float startSpeed, float endSpeed,
            float angle, float startAngularVelocity, float endAngularVelocity,
            float startSize, float endSize,
            Color startColor, Color endColor, float opacity,
            float inDuration, float mainDuration, float outDuration,
            int blendModeSRC, int blendModeDEST,
            float textureLoopLength, float textureScrollSpeed, float textureOffset,
            @Nullable Vector2f offsetVelocity, @Nullable Map<String, Object> advancedOptions,
            CombatEngineLayers layerToRenderOn, float frameOffsetMult) {
        //First, find the plugin
        MagicTrailPlugin plugin = getPlugin();
        if (plugin == null) return;

        //Finds the correct maps, and ensures they are actually instantiated
        MagicTrailTracker tracker = addOrGetTrailTracker(plugin, ID, linkedEntity, layerToRenderOn, sprite, true);

        //--Reads in our special options, if we have any--
        float sizePulseWidth = 0f;
        int sizePulseCount = 0;
        if (advancedOptions != null) {
            if (advancedOptions.get("SIZE_PULSE_WIDTH") instanceof Float) {
                sizePulseWidth = (Float) advancedOptions.get("SIZE_PULSE_WIDTH");
            }
            if (advancedOptions.get("SIZE_PULSE_COUNT") instanceof Integer) {
                sizePulseCount = (Integer) advancedOptions.get("SIZE_PULSE_COUNT");
            }
            if (advancedOptions.get("FORWARD_PROPAGATION") instanceof Boolean && (boolean) advancedOptions.get("FORWARD_PROPAGATION")) {
                tracker.usesForwardPropagation = true;
            }
        }
        //--End of special options--

        //Offset tweaker to fix single frame delay for lateral movement
        Vector2f correctedPosition = new Vector2f(position);
        if (linkedEntity instanceof DamagingProjectileAPI) {
            DamagingProjectileAPI proj = (DamagingProjectileAPI) linkedEntity;

            Vector2f shipVelPerAdvance = (Vector2f) new Vector2f(proj.getSource().getVelocity()).scale(Global.getCombatEngine().getElapsedInLastFrame());
            shipVelPerAdvance.scale(frameOffsetMult);
            Vector2f.sub(position, shipVelPerAdvance, correctedPosition);
        }

        //Adjusts scroll speed to our most recent trail's value
        tracker.scrollSpeed = textureScrollSpeed;

        //Adjusts the texture in the Trail Tracker to our most recently received texture, and flags it as being an Animated trail
        int texID = sprite.getTextureId();
        tracker.isAnimated = true;
        tracker.currentAnimRenderTexture = texID;

        Vector2f offsetVel = new Vector2f();
        if (offsetVelocity != null) {
            offsetVel = offsetVelocity;
        }

        //check for specific texture offset or a random one
        float textOffset = 0;
        if (textureOffset == -1) {
            //the texture tracker keep a fixed random texture offset
            if (tracker.textureOffset == -1) {
                tracker.textureOffset = MathUtils.getRandomNumberInRange(0, textureLoopLength);
            }
            textOffset = tracker.textureOffset;
        } else if (textureOffset != 0) {
            textOffset = textureOffset;
        }


        //Creates the custom object we want
        MagicTrailObject objectToAdd = new MagicTrailObject(inDuration, mainDuration, outDuration, startSize, endSize, startAngularVelocity, endAngularVelocity,
                opacity, blendModeSRC, blendModeDEST, startSpeed, endSpeed, startColor, endColor, angle, correctedPosition, textureLoopLength, textOffset, offsetVel,
                sizePulseWidth, sizePulseCount);

        //And finally add it to the correct location in our maps
        tracker.addNewTrailObject(objectToAdd);
    }


    // DEPRECATED

    /**
     * Spawns a trail piece, which links up with other pieces with the same ID
     * to form a smooth trail. This function is similar to the Advanced function, but
     * allows the trail to change its texture each time a new member is added. It will
     * always use the texture of the most recently-added member. If the texture is not
     * supposed to be animated, do NOT use this function: it runs notably slower.
     *
     * @param linkedEntity         The entity this trail is attached to, used for cutting trails.
     *                             Can be Null, but that should really only be done in weird, edge-case scenarios
     * @param ID                   The ID for this specific trail. Preferably get this from getUniqueID,
     *                             but it's not required: just expect very weird results if you don't
     * @param sprite               Which sprite to draw for this trail: if changed mid-trail, the entire trail uses the
     *                             new sprite.
     * @param position             Starting position for this piece of trail
     * @param startSpeed           The starting speed, in SU, this trail piece is moving at. The trail piece smoothly
     *                             transitions from its startSpeed to its endSpeed over its duration
     * @param endSpeed             The ending speed, in SU, this trail piece is moving at. The trail piece smoothly
     *                             transitions from its startSpeed to its endSpeed over its duration
     * @param angle                Which angle this piece of trail has in degrees; determines which direction it moves,
     *                             and which direction its size is measured over
     * @param startAngularVelocity The angular velocity this trail piece has when spawned. The angular velocity
     *                             of a trail piece smoothly transitions from startAngularVelocity to
     *                             endAngularVelocity over its duration
     * @param endAngularVelocity   The angular velocity this trail piece has just before disappearing.
     *                             The angular velocity of a trail piece smoothly transitions from
     *                             startAngularVelocity to endAngularVelocity over its duration
     * @param startSize            The starting size (or rather width) this piece of trail has. Measured in SU. A trail
     *                             smoothly transitions from its startSize to its endSize over its duration
     * @param endSize              The ending size (or rather width) this trail piece has. Measured in SU. A trail smoothly
     *                             transitions from its startSize to its endSize over its duration
     * @param startColor           The color this piece of trail has when spawned. Can be changed in the middle of a trail,
     *                             and will blend smoothly between pieces. Ignores alpha component entirely. Each trail piece
     *                             smoothly transitions from startColor to endColor over its duration
     * @param endColor             The color this piece of trail has just before disappearing. Can be changed in the middle of a
     *                             trail, and will blend smoothly between pieces. Ignores alpha component entirely. Each trail piece
     *                             smoothly transitions from startColor to endColor over its duration
     * @param opacity              The starting opacity of this piece of trail. Is a value between 0f and 1f. The opacity
     *                             gradually approaches 0f over the trail's duration
     * @param inDuration           How long this trail spends "fading in"; for this many seconds, the opacity of the trail piece
     *                             steadily increases until reaching "opacity". A trail's total duration is
     *                             inDuration + mainDuration + outDuration
     * @param mainDuration         How long a trail uses its maximum opacity. A trail's total duration is
     *                             inDuration + mainDuration + outDuration
     * @param outDuration          How long a trail spends "fading out"; over this many seconds at the end of the trail's
     *                             duration, its opacity goes from "opacity" to 0f. A trail's total duration is
     *                             inDuration + mainDuration + outDuration
     * @param textureLoopLength    How many SU it takes for the texture to loop. Should preferably be non-zero. If the
     *                             trail is not supposed to loop, put this as -1f
     * @param textureScrollSpeed   How fast, and in which direction, the texture scrolls over the trail. Defined so that
     *                             1000 means scrolling the entire texture length once per second, and 2000 means
     *                             scrolling the entire texture length twice per second
     * @param offsetVelocity       The offset velocity of the trail; this is an additional velocity that is
     *                             unaffected by rotation and facing, and will never change over the trail's lifetime
     * @param advancedOptions      The most unique and special options go in a special Map<> here. Be careful to input the
     *                             correct type values and use the right data keys. Any new features will be added here to
     *                             keep compatibility with old mod versions. Can be null. Currently supported keys:
     *                             "SIZE_PULSE_WIDTH" :  Float - How much additional width the trail gains each "pulse", at
     *                             most. Used in conjunction with SIZE_PULSE_COUNT
     *                             "SIZE_PULSE_COUNT" :  Integer - How many times the trail "pulses" its width over its
     *                             lifetime. Used in conjunction with SIZE_PULSE_WIDTH
     *                             "FORWARD_PROPAGATION" :  Boolean - If the trail uses the legacy render method of
     *                             "forward propagation". Used to be the default. CANNOT be
     *                             changed mid-trail
     */
    @Deprecated
    public static void addTrailMemberAnimated(
            CombatEntityAPI linkedEntity, float ID, SpriteAPI sprite,
            Vector2f position, float startSpeed, float endSpeed,
            float angle, float startAngularVelocity, float endAngularVelocity,
            float startSize, float endSize,
            Color startColor, Color endColor, float opacity,
            float inDuration, float mainDuration, float outDuration,
            boolean additive,
            float textureLoopLength, float textureScrollSpeed,
            Vector2f offsetVelocity, @Nullable Map<String, Object> advancedOptions,
            CombatEngineLayers layerToRenderOn, float frameOffsetMult) {

        //Converts our additive/non-additive option to true openGL stuff
        int blendModeSRC = GL_SRC_ALPHA;
        int blendModeDEST = additive ? GL_ONE : GL_ONE_MINUS_SRC_ALPHA;

        addTrailMemberAnimated(linkedEntity, ID, sprite, position, startSpeed, endSpeed, angle, startAngularVelocity, endAngularVelocity, startSize, endSize, startColor,
                endColor, opacity, inDuration, mainDuration, outDuration, blendModeSRC, blendModeDEST, textureLoopLength, textureScrollSpeed, offsetVelocity,
                advancedOptions, layerToRenderOn, frameOffsetMult);
    }


    /**
     * Spawns a trail piece, which links up with other pieces with the same ID
     * to form a smooth trail. This function is similar to the Advanced function, but
     * allows the trail to change its texture each time a new member is added. It will
     * always use the texture of the most recently-added member. If the texture is not
     * supposed to be animated, do NOT use this function: it runs notably slower.
     *
     * @param linkedEntity         The entity this trail is attached to, used for cutting trails.
     *                             Can be Null, but that should really only be done in weird, edge-case scenarios
     * @param ID                   The ID for this specific trail. Preferably get this from getUniqueID,
     *                             but it's not required: just expect very weird results if you don't
     * @param sprite               Which sprite to draw for this trail: if changed mid-trail, the entire trail uses the
     *                             new sprite.
     * @param position             Starting position for this piece of trail
     * @param startSpeed           The starting speed, in SU, this trail piece is moving at. The trail piece smoothly
     *                             transitions from its startSpeed to its endSpeed over its duration
     * @param endSpeed             The ending speed, in SU, this trail piece is moving at. The trail piece smoothly
     *                             transitions from its startSpeed to its endSpeed over its duration
     * @param angle                Which angle this piece of trail has in degrees; determines which direction it moves,
     *                             and which direction its size is measured over
     * @param startAngularVelocity The angular velocity this trail piece has when spawned. The angular velocity
     *                             of a trail piece smoothly transitions from startAngularVelocity to
     *                             endAngularVelocity over its duration
     * @param endAngularVelocity   The angular velocity this trail piece has just before disappearing.
     *                             The angular velocity of a trail piece smoothly transitions from
     *                             startAngularVelocity to endAngularVelocity over its duration
     * @param startSize            The starting size (or rather width) this piece of trail has. Measured in SU. A trail
     *                             smoothly transitions from its startSize to its endSize over its duration
     * @param endSize              The ending size (or rather width) this trail piece has. Measured in SU. A trail smoothly
     *                             transitions from its startSize to its endSize over its duration
     * @param startColor           The color this piece of trail has when spawned. Can be changed in the middle of a trail,
     *                             and will blend smoothly between pieces. Ignores alpha component entirely. Each trail piece
     *                             smoothly transitions from startColor to endColor over its duration
     * @param endColor             The color this piece of trail has just before disappearing. Can be changed in the middle of a
     *                             trail, and will blend smoothly between pieces. Ignores alpha component entirely. Each trail piece
     *                             smoothly transitions from startColor to endColor over its duration
     * @param opacity              The starting opacity of this piece of trail. Is a value between 0f and 1f. The opacity
     *                             gradually approaches 0f over the trail's duration
     * @param inDuration           How long this trail spends "fading in"; for this many seconds, the opacity of the trail piece
     *                             steadily increases until reaching "opacity". A trail's total duration is
     *                             inDuration + mainDuration + outDuration
     * @param mainDuration         How long a trail uses its maximum opacity. A trail's total duration is
     *                             inDuration + mainDuration + outDuration
     * @param outDuration          How long a trail spends "fading out"; over this many seconds at the end of the trail's
     *                             duration, its opacity goes from "opacity" to 0f. A trail's total duration is
     *                             inDuration + mainDuration + outDuration
     * @param blendModeSRC         Which SRD openGL blend mode to use for the trail. If you are unsure of what this means, just
     *                             put it as GL_SRC_ALPHA
     * @param blendModeDEST        Which DEST openGL blend mode to use for the trail. If you are unsure of what this means,
     *                             put it as GL_ONE_MINUS_SRC_ALPHA for normal blending and GL_ONE for additive blending
     * @param textureLoopLength    How many SU it takes for the texture to loop. Should preferably be non-zero. If the
     *                             trail is not supposed to loop, put this as -1f
     * @param textureScrollSpeed   How fast, and in which direction, the texture scrolls over the trail. Defined so that
     *                             1000 means scrolling the entire texture length once per second, and 2000 means
     *                             scrolling the entire texture length twice per second
     * @param offsetVelocity       The offset velocity of the trail; this is an additional velocity that is
     *                             unaffected by rotation and facing, and will never change over the trail's lifetime
     * @param advancedOptions      The most unique and special options go in a special Map<> here. Be careful to input the
     *                             correct type values and use the right data keys. Any new features will be added here to
     *                             keep compatibility with old mod versions. Can be null. Currently supported keys:
     *                             "SIZE_PULSE_WIDTH" :  Float - How much additional width the trail gains each "pulse", at
     *                             most. Used in conjunction with SIZE_PULSE_COUNT
     *                             "SIZE_PULSE_COUNT" :  Integer - How many times the trail "pulses" its width over its
     *                             lifetime. Used in conjunction with SIZE_PULSE_WIDTH
     *                             "FORWARD_PROPAGATION" :  Boolean - If the trail uses the legacy render method of
     *                             "forward propagation". Used to be the default. CANNOT be
     *                             changed mid-trail
     * @param layerToRenderOn      Which combat layer to render the trail on. All available layers are specified in
     *                             CombatEngineLayers. Old behaviour was CombatEngineLayers.BELOW_INDICATORS_LAYER.
     *                             CANNOT change mid-trail, under any circumstance
     * @param frameOffsetMult      The per-frame multiplier for the per-frame velocity offset magnitude. Used to finely
     *                             adjust trail offset at different speeds
     */
    @Deprecated
    public static void addTrailMemberAnimated(
            CombatEntityAPI linkedEntity, float ID, SpriteAPI sprite,
            Vector2f position, float startSpeed, float endSpeed,
            float angle, float startAngularVelocity, float endAngularVelocity,
            float startSize, float endSize,
            Color startColor, Color endColor, float opacity,
            float inDuration, float mainDuration, float outDuration,
            int blendModeSRC, int blendModeDEST,
            float textureLoopLength, float textureScrollSpeed,
            Vector2f offsetVelocity, @Nullable Map<String, Object> advancedOptions,
            CombatEngineLayers layerToRenderOn, float frameOffsetMult) {
        //First, find the plugin
        MagicTrailPlugin plugin = getPlugin();
        if (plugin == null) return;


        //Finds the correct maps, and ensures they are actually instantiated
        MagicTrailTracker tracker = addOrGetTrailTracker(plugin, ID, linkedEntity, layerToRenderOn, sprite, true);

        //--Reads in our special options, if we have any--
        float sizePulseWidth = 0f;
        int sizePulseCount = 0;
        if (advancedOptions != null) {
            if (advancedOptions.get("SIZE_PULSE_WIDTH") instanceof Float) {
                sizePulseWidth = (Float) advancedOptions.get("SIZE_PULSE_WIDTH");
            }
            if (advancedOptions.get("SIZE_PULSE_COUNT") instanceof Integer) {
                sizePulseCount = (Integer) advancedOptions.get("SIZE_PULSE_COUNT");
            }
            if (advancedOptions.get("FORWARD_PROPAGATION") instanceof Boolean && (boolean) advancedOptions.get("FORWARD_PROPAGATION")) {
                tracker.usesForwardPropagation = true;
            }
        }
        //--End of special options--

        //Offset tweaker to fix single frame delay for lateral movement
        Vector2f correctedPosition = new Vector2f(position);
        if (linkedEntity instanceof DamagingProjectileAPI) {
            DamagingProjectileAPI proj = (DamagingProjectileAPI) linkedEntity;

            Vector2f shipVelPerAdvance = (Vector2f) new Vector2f(proj.getSource().getVelocity()).scale(Global.getCombatEngine().getElapsedInLastFrame());
            shipVelPerAdvance.scale(frameOffsetMult);
            Vector2f.sub(position, shipVelPerAdvance, correctedPosition);
        }

        //Adjusts scroll speed to our most recent trail's value
        tracker.scrollSpeed = textureScrollSpeed;

        //Adjusts the texture in the Trail Tracker to our most recently received texture, and flags it as being an Animated trail
        int texID = sprite.getTextureId();
        tracker.isAnimated = true;
        tracker.currentAnimRenderTexture = texID;

        //Creates the custom object we want
        MagicTrailObject objectToAdd = new MagicTrailObject(inDuration, mainDuration, outDuration, startSize, endSize, startAngularVelocity, endAngularVelocity,
                opacity, blendModeSRC, blendModeDEST, startSpeed, endSpeed, startColor, endColor, angle, correctedPosition, textureLoopLength, 0, offsetVelocity,
                sizePulseWidth, sizePulseCount);

        //And finally add it to the correct location in our maps
        tracker.addNewTrailObject(objectToAdd);
    }

    /**
     * Spawns a trail piece, which links up with other pieces with the same ID
     * to form a smooth trail. This function is similar to the Advanced function, but
     * allows the trail to change its texture each time a new member is added. It will
     * always use the texture of the most recently-added member. If the texture is not
     * supposed to be animated, do NOT use this function: it runs notably slower.
     *
     * @param linkedEntity         The entity this trail is attached to, used for cutting trails.
     *                             Can be Null, but that should really only be done in weird, edge-case scenarios
     * @param ID                   The ID for this specific trail. Preferably get this from getUniqueID,
     *                             but it's not required: just expect very weird results if you don't
     * @param sprite               Which sprite to draw for this trail: if changed mid-trail, the entire trail uses the
     *                             new sprite.
     * @param position             Starting position for this piece of trail
     * @param startSpeed           The starting speed, in SU, this trail piece is moving at. The trail piece smoothly
     *                             transitions from its startSpeed to its endSpeed over its duration
     * @param endSpeed             The ending speed, in SU, this trail piece is moving at. The trail piece smoothly
     *                             transitions from its startSpeed to its endSpeed over its duration
     * @param angle                Which angle this piece of trail has in degrees; determines which direction it moves,
     *                             and which direction its size is measured over
     * @param startAngularVelocity The angular velocity this trail piece has when spawned. The angular velocity
     *                             of a trail piece smoothly transitions from startAngularVelocity to
     *                             endAngularVelocity over its duration
     * @param endAngularVelocity   The angular velocity this trail piece has just before disappearing.
     *                             The angular velocity of a trail piece smoothly transitions from
     *                             startAngularVelocity to endAngularVelocity over its duration
     * @param startSize            The starting size (or rather width) this piece of trail has. Measured in SU. A trail
     *                             smoothly transitions from its startSize to its endSize over its duration
     * @param endSize              The ending size (or rather width) this trail piece has. Measured in SU. A trail smoothly
     *                             transitions from its startSize to its endSize over its duration
     * @param startColor           The color this piece of trail has when spawned. Can be changed in the middle of a trail,
     *                             and will blend smoothly between pieces. Ignores alpha component entirely. Each trail piece
     *                             smoothly transitions from startColor to endColor over its duration
     * @param endColor             The color this piece of trail has just before disappearing. Can be changed in the middle of a
     *                             trail, and will blend smoothly between pieces. Ignores alpha component entirely. Each trail piece
     *                             smoothly transitions from startColor to endColor over its duration
     * @param opacity              The starting opacity of this piece of trail. Is a value between 0f and 1f. The opacity
     *                             gradually approaches 0f over the trail's duration
     * @param inDuration           How long this trail spends "fading in"; for this many seconds, the opacity of the trail piece
     *                             steadily increases until reaching "opacity". A trail's total duration is
     *                             inDuration + mainDuration + outDuration
     * @param mainDuration         How long a trail uses its maximum opacity. A trail's total duration is
     *                             inDuration + mainDuration + outDuration
     * @param outDuration          How long a trail spends "fading out"; over this many seconds at the end of the trail's
     *                             duration, its opacity goes from "opacity" to 0f. A trail's total duration is
     *                             inDuration + mainDuration + outDuration
     * @param blendModeSRC         Which SRD openGL blend mode to use for the trail. If you are unsure of what this means, just
     *                             put it as GL_SRC_ALPHA
     * @param blendModeDEST        Which DEST openGL blend mode to use for the trail. If you are unsure of what this means,
     *                             put it as GL_ONE_MINUS_SRC_ALPHA for normal blending and GL_ONE for additive blending
     * @param textureLoopLength    How many SU it takes for the texture to loop. Should preferably be non-zero. If the
     *                             trail is not supposed to loop, put this as -1f
     * @param textureScrollSpeed   How fast, and in which direction, the texture scrolls over the trail. Defined so that
     *                             1000 means scrolling the entire texture length once per second, and 2000 means
     *                             scrolling the entire texture length twice per second
     * @param offsetVelocity       The offset velocity of the trail; this is an additional velocity that is
     *                             unaffected by rotation and facing, and will never change over the trail's lifetime
     * @param advancedOptions      The most unique and special options go in a special Map<> here. Be careful to input the
     *                             correct type values and use the right data keys. Any new features will be added here to
     *                             keep compatibility with old mod versions. Can be null. Currently supported keys:
     *                             "SIZE_PULSE_WIDTH" :  Float - How much additional width the trail gains each "pulse", at
     *                             most. Used in conjunction with SIZE_PULSE_COUNT
     *                             "SIZE_PULSE_COUNT" :  Integer - How many times the trail "pulses" its width over its
     *                             lifetime. Used in conjunction with SIZE_PULSE_WIDTH
     *                             "FORWARD_PROPAGATION" :  Boolean - If the trail uses the legacy render method of
     *                             "forward propagation". Used to be the default. CANNOT be
     *                             changed mid-trail
     */
    @Deprecated
    public static void addTrailMemberAnimated(
            CombatEntityAPI linkedEntity, float ID, SpriteAPI sprite,
            Vector2f position, float startSpeed, float endSpeed,
            float angle, float startAngularVelocity, float endAngularVelocity,
            float startSize, float endSize,
            Color startColor, Color endColor, float opacity,
            float inDuration, float mainDuration, float outDuration,
            int blendModeSRC, int blendModeDEST,
            float textureLoopLength, float textureScrollSpeed,
            Vector2f offsetVelocity, @Nullable Map<String, Object> advancedOptions) {
        addTrailMemberAnimated(linkedEntity, ID, sprite, position, startSpeed, endSpeed, angle, startAngularVelocity, endAngularVelocity, startSize, endSize, startColor,
                endColor, opacity, inDuration, mainDuration, outDuration, blendModeSRC, blendModeDEST, textureLoopLength, textureScrollSpeed, offsetVelocity,
                advancedOptions, CombatEngineLayers.BELOW_INDICATORS_LAYER, 1f);
    }

    /**
     * Spawns a trail piece, which links up with other pieces with the same ID
     * to form a smooth trail. This function is similar to the Advanced function, but
     * allows the trail to change its texture each time a new member is added. It will
     * always use the texture of the most recently-added member. If the texture is not
     * supposed to be animated, do NOT use this function: it runs notably slower.
     *
     * @param linkedEntity         The entity this trail is attached to, used for cutting trails.
     *                             Can be Null, but that should really only be done in weird, edge-case scenarios
     * @param ID                   The ID for this specific trail. Preferably get this from getUniqueID,
     *                             but it's not required: just expect very weird results if you don't
     * @param sprite               Which sprite to draw for this trail: if changed mid-trail, the entire trail uses the
     *                             new sprite.
     * @param position             Starting position for this piece of trail
     * @param startSpeed           The starting speed, in SU, this trail piece is moving at. The trail piece smoothly
     *                             transitions from its startSpeed to its endSpeed over its duration
     * @param endSpeed             The ending speed, in SU, this trail piece is moving at. The trail piece smoothly
     *                             transitions from its startSpeed to its endSpeed over its duration
     * @param angle                Which angle this piece of trail has in degrees; determines which direction it moves,
     *                             and which direction its size is measured over
     * @param startAngularVelocity The angular velocity this trail piece has when spawned. The angular velocity
     *                             of a trail piece smoothly transitions from startAngularVelocity to
     *                             endAngularVelocity over its duration
     * @param endAngularVelocity   The angular velocity this trail piece has just before disappearing.
     *                             The angular velocity of a trail piece smoothly transitions from
     *                             startAngularVelocity to endAngularVelocity over its duration
     * @param startSize            The starting size (or rather width) this piece of trail has. Measured in SU. A trail
     *                             smoothly transitions from its startSize to its endSize over its duration
     * @param endSize              The ending size (or rather width) this trail piece has. Measured in SU. A trail smoothly
     *                             transitions from its startSize to its endSize over its duration
     * @param startColor           The color this piece of trail has when spawned. Can be changed in the middle of a trail,
     *                             and will blend smoothly between pieces. Ignores alpha component entirely. Each trail piece
     *                             smoothly transitions from startColor to endColor over its duration
     * @param endColor             The color this piece of trail has just before disappearing. Can be changed in the middle of a
     *                             trail, and will blend smoothly between pieces. Ignores alpha component entirely. Each trail piece
     *                             smoothly transitions from startColor to endColor over its duration
     * @param opacity              The starting opacity of this piece of trail. Is a value between 0f and 1f. The opacity
     *                             gradually approaches 0f over the trail's duration
     * @param inDuration           How long this trail spends "fading in"; for this many seconds, the opacity of the trail piece
     *                             steadily increases until reaching "opacity". A trail's total duration is
     *                             inDuration + mainDuration + outDuration
     * @param mainDuration         How long a trail uses its maximum opacity. A trail's total duration is
     *                             inDuration + mainDuration + outDuration
     * @param outDuration          How long a trail spends "fading out"; over this many seconds at the end of the trail's
     *                             duration, its opacity goes from "opacity" to 0f. A trail's total duration is
     *                             inDuration + mainDuration + outDuration
     * @param blendModeSRC         Which SRD openGL blend mode to use for the trail. If you are unsure of what this means, just
     *                             put it as GL_SRC_ALPHA
     * @param blendModeDEST        Which DEST openGL blend mode to use for the trail. If you are unsure of what this means,
     *                             put it as GL_ONE_MINUS_SRC_ALPHA for normal blending and GL_ONE for additive blending
     * @param textureLoopLength    How many SU it takes for the texture to loop. Should preferably be non-zero. If the
     *                             trail is not supposed to loop, put this as -1f
     * @param textureScrollSpeed   How fast, and in which direction, the texture scrolls over the trail. Defined so that
     *                             1000 means scrolling the entire texture length once per second, and 2000 means
     *                             scrolling the entire texture length twice per second
     * @param offsetVelocity       The offset velocity of the trail; this is an additional velocity that is
     *                             unaffected by rotation and facing, and will never change over the trail's lifetime
     * @param advancedOptions      The most unique and special options go in a special Map<> here. Be careful to input the
     *                             correct type values and use the right data keys. Any new features will be added here to
     *                             keep compatibility with old mod versions. Can be null. Currently supported keys:
     *                             "SIZE_PULSE_WIDTH" :  Float - How much additional width the trail gains each "pulse", at
     *                             most. Used in conjunction with SIZE_PULSE_COUNT
     *                             "SIZE_PULSE_COUNT" :  Integer - How many times the trail "pulses" its width over its
     *                             lifetime. Used in conjunction with SIZE_PULSE_WIDTH
     *                             "FORWARD_PROPAGATION" :  Boolean - If the trail uses the legacy render method of
     *                             "forward propagation". Used to be the default. CANNOT be
     *                             changed mid-trail
     * @param layerToRenderOn      Which combat layer to render the trail on. All available layers are specified in
     *                             CombatEngineLayers. Old behaviour was CombatEngineLayers.BELOW_INDICATORS_LAYER.
     *                             CANNOT change mid-trail, under any circumstance
     */
    @Deprecated
    public static void addTrailMemberAnimated(
            CombatEntityAPI linkedEntity, float ID, SpriteAPI sprite,
            Vector2f position, float startSpeed, float endSpeed,
            float angle, float startAngularVelocity, float endAngularVelocity,
            float startSize, float endSize,
            Color startColor, Color endColor, float opacity,
            float inDuration, float mainDuration, float outDuration,
            int blendModeSRC, int blendModeDEST,
            float textureLoopLength, float textureScrollSpeed,
            Vector2f offsetVelocity, @Nullable Map<String, Object> advancedOptions,
            CombatEngineLayers layerToRenderOn) {
        //First, find the plugin
        MagicTrailPlugin plugin = getPlugin();
        if (plugin == null) return;

        //Finds the correct maps, and ensures they are actually instantiated

        MagicTrailTracker tracker = addOrGetTrailTracker(plugin, ID, linkedEntity, layerToRenderOn, sprite, true);

        //--Reads in our special options, if we have any--
        float sizePulseWidth = 0f;
        int sizePulseCount = 0;
        if (advancedOptions != null) {
            if (advancedOptions.get("SIZE_PULSE_WIDTH") instanceof Float) {
                sizePulseWidth = (Float) advancedOptions.get("SIZE_PULSE_WIDTH");
            }
            if (advancedOptions.get("SIZE_PULSE_COUNT") instanceof Integer) {
                sizePulseCount = (Integer) advancedOptions.get("SIZE_PULSE_COUNT");
            }
            if (advancedOptions.get("FORWARD_PROPAGATION") instanceof Boolean && (boolean) advancedOptions.get("FORWARD_PROPAGATION")) {
                tracker.usesForwardPropagation = true;
            }
        }
        //--End of special options--

        //Adjusts scroll speed to our most recent trail's value
        tracker.scrollSpeed = textureScrollSpeed;

        //Adjusts the texture in the Trail Tracker to our most recently received texture, and flags it as being an Animated trail
        int texID = sprite.getTextureId();
        tracker.isAnimated = true;
        tracker.currentAnimRenderTexture = texID;

        //Creates the custom object we want
        MagicTrailObject objectToAdd = new MagicTrailObject(inDuration, mainDuration, outDuration, startSize, endSize, startAngularVelocity, endAngularVelocity,
                opacity, blendModeSRC, blendModeDEST, startSpeed, endSpeed, startColor, endColor, angle, position, textureLoopLength, 0, offsetVelocity,
                sizePulseWidth, sizePulseCount);

        //And finally add it to the correct location in our maps
        tracker.addNewTrailObject(objectToAdd);
    }


    // OTHER TOOLS

    /**
     * A small function to get a unique ID for the trail member: *must* be saved in the function that generates the
     * trail, since if it changes it counts as a new trail altogether
     *
     * @return
     */
    public static float getUniqueID() {
        //Gets a value 0.1f higher than the previous maximum ID, and marks that as our previous maximum ID
        float toReturn = usedIDs + 0.1f;
        usedIDs = toReturn;
        return toReturn;
    }

    //Similar to above, but is *explicitly* intended for the cutTrailsOnEntity function, and is thus private
    private static float getUniqueCutterID() {
        //Gets a value 0.1f lower than the previous maximum ID, and marks that as our previous maximum ID
        float toReturn = usedCutterIDs - 0.1f;
        usedCutterIDs = toReturn;
        return toReturn;
    }


    /**
     * "Cuts" all trails on a designated entity, forcing new trail pieces to not link up with old ones. Should
     * be used before teleporting any entity, since it may have trails attached to it which will otherwise stretch
     * in unintended ways.
     * <p>
     * This can also be called (with a potential 1-frame delay) by adding an entity to any CustomData in
     * the CombatEngineAPI with a key containing the phrase "MagicTrailPlugin_LIB_FREE_TRAIL_CUT" (note: *containing*.
     * there must be more to the key than this, as it should be unique [optimally, use your modID and the ID of the
     * ship trying to cut the trails]). This alternate  calling method does not require MagicLib, and may thus
     * be optimal for smaller mods or mods that don't want any  other MagicLib features but still needs
     * trail-cutting support
     *
     * @param entity The entity you want to cut all trails on
     */
    public static void cutTrailsOnEntity(CombatEntityAPI entity) {
        //First, find the plugin
        MagicTrailPlugin plugin = getPlugin();
        if (plugin == null) return;

        //Iterate over all textures in the main map...
        for (Entry<CombatEngineLayers, Map<Integer, Map<CombatEntityAPI, List<Float>>>>
                cuttingLayerEntry : plugin.cuttingMap.entrySet()) {
            CombatEngineLayers layer = cuttingLayerEntry.getKey();
            Map<Integer, Map<CombatEntityAPI, List<Float>>> cuttingLayerMap = cuttingLayerEntry.getValue();
            for (Entry<Integer, Map<CombatEntityAPI, List<Float>>> cuttingEntityEntry : cuttingLayerMap.entrySet()) {
                //If our entity has any registered trails, cut them all off by giving them new, unique IDs
                int texID = cuttingEntityEntry.getKey();
                Map<CombatEntityAPI, List<Float>> cuttingEntityMap = cuttingEntityEntry.getValue();

                if (cuttingEntityMap.get(entity) != null) {
                    //Get the trackers from main trail map
                    Map<Float, MagicTrailTracker> trailTrackerMap = plugin.mainMap.get(layer).get(texID);
                    if (trailTrackerMap != null) {
                        for (Float trailID : cuttingEntityMap.get(entity)) {
                            MagicTrailTracker trailTracker = trailTrackerMap.get(trailID);
                            if (trailTracker != null) {
                                trailTrackerMap.put(getUniqueCutterID(), trailTracker);
                                trailTrackerMap.remove(trailID);
                            }
                        }
                    }
                }
            }
        }
    }


    // PLUGIN STUFF

    @Override
    public void init(CombatEngineAPI engine) {
        //Creates the render plugin and adds it to the engine
        MagicTrailRenderer renderer = new MagicTrailRenderer(this);
        engine.addLayeredRenderingPlugin(renderer);

        //Stores our plugins in easy-to-reach locations, so we can access it in different places
        engine.getCustomData().put(PLUGIN_KEY, this);
        engine.getCustomData().put("MagicTrailRenderer", renderer);

        usedIDs = 1f;
        usedCutterIDs = -1f;
        mainMap.clear();
        cuttingMap.clear();

        this.engine = engine;
    }

    /**
     * @return Get trail plugin from current CombatEngine.
     */
    public static MagicTrailPlugin getPlugin() {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) {
            return null;
        }
        return (MagicTrailPlugin) engine.getCustomData().get(PLUGIN_KEY);
    }


    //Ticks all maps, and checks for any entity that should recieve library-free cutting
    @Override
    public void advance(float amount, java.util.List<InputEventAPI> events) {
        if (engine == null || engine.isPaused()) {
            return;
        }

        cleanTimer.advance(amount);
        //Check if it's the clean time
        boolean shouldClean = cleanTimer.intervalElapsed();

        //Ticks the main map
        for (Map<Integer, Map<Float, MagicTrailTracker>> layerMap : mainMap.values()) {
            for (Map<Float, MagicTrailTracker> trailTrackerMap : layerMap.values()) {
                Iterator<Entry<Float, MagicTrailTracker>> trailTrackerIterator = trailTrackerMap.entrySet().iterator();
                while (trailTrackerIterator.hasNext()) {
                    Entry<Float, MagicTrailTracker> entry = trailTrackerIterator.next();
                    MagicTrailTracker tracker = entry.getValue();
                    if (shouldClean && tracker.isExpired()) {
                        trailTrackerIterator.remove();
                    }
                    tracker.tickTimersInTrail(amount);
                }
            }
        }

        //Clean the cutting map
        if (shouldClean) {
            for (Map<Integer, Map<CombatEntityAPI, List<Float>>> texMap : cuttingMap.values()) {
                for (Map<CombatEntityAPI, List<Float>> entityMap : texMap.values()) {
                    Iterator<Entry<CombatEntityAPI, List<Float>>> entryIterator = entityMap.entrySet().iterator();
                    //If the entity is not in the engine, just delete it
                    while (entryIterator.hasNext()) {
                        Entry<CombatEntityAPI, List<Float>> entry = entryIterator.next();
                        CombatEntityAPI entity = entry.getKey();
                        if (!engine.isEntityInPlay(entity)) {
                            entryIterator.remove();
                        }
                    }
                }
            }
        }

        //Checks our combat engine's customData and sees if someone has attempted a library-free trail cutting this frame
        //If they have, cut it properly and store the fact that cutting took place this frame
        final Map<String, Object> customData = engine.getCustomData();
        //Use iterator to remove at the same time
        Iterator<Entry<String, Object>> dataEntryIterator = customData.entrySet().iterator();

        while (dataEntryIterator.hasNext()) {
            Entry<String, Object> entry = dataEntryIterator.next();
            String key = entry.getKey();
            //If cutting took place this frame, we remove all CustomData key-data pairs that are used for lib-free cutting
            if (entry.getValue() instanceof CombatEntityAPI
                    && key.contains("MagicTrailPlugin_LIB_FREE_TRAIL_CUT")) {
                CombatEntityAPI combatEntity = (CombatEntityAPI) entry.getValue();
                cutTrailsOnEntity(combatEntity);
                dataEntryIterator.remove();
            }
        }


    }
}


//Handles all rendering of the trails, since this now has to be done on a separate plugin to use render layers
class MagicTrailRenderer extends BaseCombatLayeredRenderingPlugin {
    //Our parent plugin, which handles all trail activity *except* rendering
    private MagicTrailPlugin parentPlugin;

    //No render distance limit!
    @Override
    public float getRenderRadius() {
        return 999999999999999999999f;
    }

    //Our constructor takes our parent plugin, so we can access the trail data during rendering
    protected MagicTrailRenderer(MagicTrailPlugin parentPlugin) {
        this.parentPlugin = parentPlugin;
    }

    //Main render function: renders all trails of a given layer
    @Override
    public void render(CombatEngineLayers layer, ViewportAPI view) {
        //Iterates through all normal trails on this layer, and render them one at a time
        Map<Integer, Map<Float, MagicTrailTracker>> mainLayerMap = parentPlugin.mainMap.get(layer);
        if (mainLayerMap != null) {
            for (Entry<Integer, Map<Float, MagicTrailTracker>> entry : mainLayerMap.entrySet()) {
                int texID = entry.getKey();
                Map<Float, MagicTrailTracker> trailTrackerMap = entry.getValue();
                for (MagicTrailTracker trailTracker : trailTrackerMap.values()) {
                    // texID may be -1 which means the anime key
                    trailTracker.renderTrail(texID);
                }
            }
        }
    }

    //We render on all layers : ideally, we would render only on layers we have trails on, but this check only runs once
    //so that doesn't work. Also, the overhead is pretty negligable, so it should be fine anyway
    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return EnumSet.allOf(CombatEngineLayers.class);
    }
}