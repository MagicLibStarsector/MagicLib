//By Nicke535, handles customizable "fake trails" similar to the vanilla QUAD_STRIP smoke implementation.
//Note that any sprites that use this plugin must have a multiple of 2 as size (so 16, 32, 64, 128 etc.), both in width and height
//This implementation of the plugin works on the campaign layer, and thus has some adjustments to its specific function.
package org.magiclib.plugins;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicTrailObject;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;

public class MagicCampaignTrailPlugin implements EveryFrameScript {
    //Tracker for unique ID getting. Only use for this script, though: it's dangerous to use for other ID purposes, since it is so simple
    //NOTE: IDs should be bigger than 0; lower than 0 IDs are used by the script for "cut" trails
    private static float usedIDs = 0f;
    private static float usedCutterIDs = -1f;

    //Map which handles all the trails: takes in an integer (the texture) and a map of NicToyCustomTrailTrackers, identified by a unique ID which must be tracked for each source independently
    private Map<Integer, Map<Float, MagicCampaignTrailTracker>> mainMap = new HashMap<>();

    //Map similar to mainMap, but for animated trails
    private Map<Float, MagicCampaignTrailTracker> animMap = new HashMap<>();

    //Map for "cutting" trails: if a trail belongs to an entity (it hasn't already been cut) its ID is here
    private Map<Integer, Map<SectorEntityToken, List<Float>>> cuttingMap = new HashMap<>();
    private Map<SectorEntityToken, List<Float>> cuttingMapAnimated = new HashMap<>();

    //A unique CustomCampaignEntity, which allows us to properly call render calls
    private CustomCampaignEntityAPI associatedEntity = null;

    //Ticks all maps, and ensures only the currently-loaded locationAPI has its maps properly loaded
    @Override
    public void advance(float amount) {
        //Returns if we detect a seemingly impossible situation (no player fleet, for example)
        if (Global.getSector() == null || Global.getSector().getPlayerFleet() == null || Global.getSector().getPlayerFleet().getContainingLocation() == null) {
            return;
        }

        //If we don't have an associated entity, or it is in the wrong locationAPI, kill any old one and generate a new one at our new locationAPI
        if (associatedEntity == null || associatedEntity.getContainingLocation() != Global.getSector().getPlayerFleet().getContainingLocation()) {
            if (associatedEntity != null) {
                associatedEntity.getContainingLocation().removeEntity(associatedEntity);
            }
            associatedEntity = Global.getSector().getPlayerFleet().getContainingLocation().addCustomEntity("magiclib_campaign_trail_tracker_object", "YOU SHOULD NOT SEE THIS",
                    "magiclib_campaign_trail_custom_entity", Factions.INDEPENDENT, this);
            associatedEntity.setFixedLocation(-100000, -100000);
            associatedEntity.setRadius(0);
        }

        //Ticks the main map
        for (Integer texID : mainMap.keySet()) {
            for (Float ID : new ArrayList<>(mainMap.get(texID).keySet())) {
                //If the tracker we just found is in the wrong LocationAPI, we either delete it or don't tick it, depending on its setting
                if (mainMap.get(texID).get(ID).locationAPI != Global.getSector().getPlayerFleet().getContainingLocation()) {
                    if (mainMap.get(texID).get(ID).locationAPICulling) {
                        mainMap.get(texID).remove(ID);
                    }
                } else {
                    mainMap.get(texID).get(ID).tickTimersInTrail(amount);
                    mainMap.get(texID).get(ID).clearAllDeadObjects();
                }
            }
        }

        //Ticks the animated map
        for (Float ID : animMap.keySet()) {
            //If the tracker we just found is in the wrong LocationAPI, we either delete it or don't tick it, depending on its setting
            if (animMap.get(ID).locationAPI != Global.getSector().getPlayerFleet().getContainingLocation()) {
                if (animMap.get(ID).locationAPICulling) {
                    animMap.remove(ID);
                }
            } else {
                animMap.get(ID).tickTimersInTrail(amount);
                animMap.get(ID).clearAllDeadObjects();
            }
        }
    }

    //Renders things; this is called by our designated CustomCampaignEntity
    public void render(CampaignEngineLayers currentRenderLayer, ViewportAPI viewPort) {
        //Returns if we detect a seemingly impossible situation (no player fleet, for example)
        if (Global.getSector() == null || Global.getSector().getPlayerFleet() == null || Global.getSector().getPlayerFleet().getContainingLocation() == null) {
            return;
        }

        //Iterates through all normal trails, and render them one at a time
        for (Integer texID : mainMap.keySet()) {
            for (Float ID : new ArrayList<>(mainMap.get(texID).keySet())) {
                //Only render if the tracker is in our current render layer and in our current render location
                if (mainMap.get(texID).get(ID).renderLayer == currentRenderLayer && mainMap.get(texID).get(ID).locationAPI == Global.getSector().getPlayerFleet().getContainingLocation()) {
                    mainMap.get(texID).get(ID).renderTrail(texID);
                }
            }
        }

        //If we have any animated trails, render those too
        for (Float ID : animMap.keySet()) {
            //Only render if the tracker is in our current render layer and in our current render location
            if (animMap.get(ID).renderLayer == currentRenderLayer && animMap.get(ID).locationAPI == Global.getSector().getPlayerFleet().getContainingLocation()) {
                animMap.get(ID).renderTrail(0);
            }
        }
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    /**
     * Spawns a trail piece, which links up with other pieces with the same ID
     * to form a smooth trail. This function has most of the functionality you
     * need; should you want more configurability, use AddTrailMemberAdvanced
     * instead
     *
     * @param linkedEntity   The entity this trail is attached to, used for cutting trails.
     *                       Can be Null, but that should really only be done in weird, edge-case scenarios
     * @param ID             The ID for this specific trail. Preferably get this from getUniqueID,
     *                       but it's not required: just expect very weird results if you don't
     * @param sprite         Which sprite to draw for this trail: do *not* change this halfway through a trail,
     *                       as that will split it into two trails
     * @param position       Starting position for this piece of trail
     * @param speed          The speed, in SU, this trail piece is moving at
     * @param angle          Which angle this piece of trail has in degrees; determines which direction it moves,
     *                       and which direction its size is measured over
     * @param startSize      The starting size (or rather width) this piece of trail has. Measured in SU. A trail
     *                       smoothly transitions from its startSize to its endSize over its duration
     * @param endSize        The ending size (or rather width) this trail piece has. Measured in SU. A trail smoothly
     *                       transitions from its startSize to its endSize over its duration
     * @param color          The color of this piece of trail. Can be changed in the middle of a trail, and will blend
     *                       smoothly between pieces. Ignores alpha component entirely
     * @param opacity        The starting opacity of this piece of trail. Is a value between 0f and 1f. The opacity
     *                       gradually approaches 0f over the trail's duration
     * @param duration       The duration of the trail, in seconds
     * @param additive       Whether this trail will use additive blending or not. Does not support being changed in
     *                       the middle of a trail
     * @param offsetVelocity The offset velocity of the trail; this is an additional velocity that is
     *                       unaffected by rotation and facing, and will never change over the trail's lifetime
     */
    public static void addTrailMemberSimple(SectorEntityToken linkedEntity, float ID, SpriteAPI sprite, Vector2f position, float speed, float angle, float startSize, float endSize, Color color,
                                            float opacity, float duration, boolean additive, Vector2f offsetVelocity) {
        //Runs the same function, but only on the specific script instead of the static interface
        for (EveryFrameScript everyFrameScript : Global.getSector().getTransientScripts()) {
            if (everyFrameScript instanceof MagicCampaignTrailPlugin) {
                ((MagicCampaignTrailPlugin) everyFrameScript).addTrailMemberSimpleInternal(linkedEntity, ID, sprite, position, speed, angle,
                        startSize, endSize, color, opacity, duration, additive, offsetVelocity);
            }
        }
    }

    private void addTrailMemberSimpleInternal(SectorEntityToken linkedEntity, float ID, SpriteAPI sprite, Vector2f position, float speed, float angle, float startSize, float endSize, Color color,
                                              float opacity, float duration, boolean additive, Vector2f offsetVelocity) {
        //Finds the correct maps, and ensures they are actually instantiated [and adds our ID to the cutting map]
        int texID = sprite.getTextureId();
        if (mainMap.get(texID) == null) {
            mainMap.put(texID, new HashMap<Float, MagicCampaignTrailTracker>());
        }
        if (mainMap.get(texID).get(ID) == null) {
            mainMap.get(texID).put(ID, new MagicCampaignTrailTracker());
        }
        if (linkedEntity != null) {
            if (cuttingMap.get(texID) == null) {
                cuttingMap.put(texID, new HashMap<SectorEntityToken, List<Float>>());
            }
            if (cuttingMap.get(texID).get(linkedEntity) == null) {
                cuttingMap.get(texID).put(linkedEntity, new ArrayList<Float>());
            }
            if (!cuttingMap.get(texID).get(linkedEntity).contains(ID)) {
                cuttingMap.get(texID).get(linkedEntity).add(ID);
            }
        }

        //Converts our additive/non-additive option to true openGL stuff
        int srcBlend = GL_SRC_ALPHA;
        int destBlend = GL_ONE_MINUS_SRC_ALPHA;

        if (additive) {
            destBlend = GL_ONE;
        }

        //Gives our tracker the correct LocationAPI (we'll have to guess it's the player's current location [and if that doesn't exist? don't change the location])
        if (Global.getSector() != null && Global.getSector().getPlayerFleet() != null && Global.getSector().getPlayerFleet().getContainingLocation() != null) {
            mainMap.get(texID).get(ID).locationAPI = Global.getSector().getPlayerFleet().getContainingLocation();
        }

        //Creates the custom object we want
        MagicTrailObject objectToAdd = new MagicTrailObject(0f, 0f, duration, startSize, endSize, 0f, 0f,
                opacity, srcBlend, destBlend, speed, speed, color, color, angle, position, -1f, 0, offsetVelocity,
                0f, 0f);

        //And finally add it to the correct location in our maps
        mainMap.get(texID).get(ID).addNewTrailObject(objectToAdd);
    }

    /**
     * Spawns a trail piece, which links up with other pieces with the same ID
     * to form a smooth trail. This function has all available functions; if you
     * just want to spawn a normal trail without all the extra configuration involved,
     * use AddTrailMemberSimple instead.
     *
     * @param linkedEntity         The campaign entity this trail is attached to, used for cutting trails.
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
     * @param locationAPICulling   If true, the trail is removed from memory as soon as the player fleet leaves the
     *                             location the trail is in. If false, the trail is only "frozen" when the player leaves
     * @param locationAPI          Which locationAPI this trail is in; should ideally be in the same as the location the
     *                             linkedEntity is in.
     */
    public static void addTrailMemberAdvanced(SectorEntityToken linkedEntity, float ID, SpriteAPI sprite, Vector2f position, float startSpeed, float endSpeed, float angle,
                                              float startAngularVelocity, float endAngularVelocity, float startSize, float endSize, Color startColor, Color endColor, float opacity,
                                              float inDuration, float mainDuration, float outDuration, int blendModeSRC, int blendModeDEST, float textureLoopLength, float textureScrollSpeed,
                                              Vector2f offsetVelocity, boolean locationAPICulling, LocationAPI locationAPI) {
        //Runs the same function, but only on the specific script instead of the static interface
        for (EveryFrameScript everyFrameScript : Global.getSector().getTransientScripts()) {
            if (everyFrameScript instanceof MagicCampaignTrailPlugin) {
                ((MagicCampaignTrailPlugin) everyFrameScript).addTrailMemberAdvancedInternal(linkedEntity, ID, sprite, position, startSpeed, endSpeed, angle,
                        startAngularVelocity, endAngularVelocity, startSize, endSize, startColor, endColor, opacity,
                        inDuration, mainDuration, outDuration, blendModeSRC, blendModeDEST, textureLoopLength, textureScrollSpeed,
                        offsetVelocity, locationAPICulling, locationAPI);
            }
        }
    }

    private void addTrailMemberAdvancedInternal(SectorEntityToken linkedEntity, float ID, SpriteAPI sprite, Vector2f position, float startSpeed, float endSpeed, float angle,
                                                float startAngularVelocity, float endAngularVelocity, float startSize, float endSize, Color startColor, Color endColor, float opacity,
                                                float inDuration, float mainDuration, float outDuration, int blendModeSRC, int blendModeDEST, float textureLoopLength, float textureScrollSpeed,
                                                Vector2f offsetVelocity, boolean locationAPICulling, LocationAPI locationAPI) {
        //Finds the correct maps, and ensures they are actually instantiated
        int texID = sprite.getTextureId();
        if (mainMap.get(texID) == null) {
            mainMap.put(texID, new HashMap<Float, MagicCampaignTrailTracker>());
        }
        if (mainMap.get(texID).get(ID) == null) {
            mainMap.get(texID).put(ID, new MagicCampaignTrailTracker());
        }
        if (linkedEntity != null) {
            if (cuttingMap.get(texID) == null) {
                cuttingMap.put(texID, new HashMap<SectorEntityToken, List<Float>>());
            }
            if (cuttingMap.get(texID).get(linkedEntity) == null) {
                cuttingMap.get(texID).put(linkedEntity, new ArrayList<Float>());
            }
            if (!cuttingMap.get(texID).get(linkedEntity).contains(ID)) {
                cuttingMap.get(texID).get(linkedEntity).add(ID);
            }
        }

        //Adds the correct locationAPI and locationAPICulling values to our tracker
        mainMap.get(texID).get(ID).locationAPI = locationAPI;
        mainMap.get(texID).get(ID).locationAPICulling = locationAPICulling;

        //Adjusts scroll speed to our most recent trail's value
        mainMap.get(texID).get(ID).scrollSpeed = textureScrollSpeed;

        //Creates the custom object we want
        MagicTrailObject objectToAdd = new MagicTrailObject(inDuration, mainDuration, outDuration, startSize, endSize, startAngularVelocity, endAngularVelocity,
                opacity, blendModeSRC, blendModeDEST, startSpeed, endSpeed, startColor, endColor, angle, position, textureLoopLength, 0, offsetVelocity,
                0f, 0f);

        //And finally add it to the correct location in our maps
        mainMap.get(texID).get(ID).addNewTrailObject(objectToAdd);
    }

    /**
     * Spawns a trail piece, which links up with other pieces with the same ID
     * to form a smooth trail. This function is similar to the Advanced function, but
     * allows the trail to change its texture each time a new member is added. It will
     * always use the texture of the most recently-added member. If the texture is not
     * supposed to be animated, do NOT use this function: it runs notably slower.
     *
     * @param linkedEntity         The campaign entity this trail is attached to, used for cutting trails.
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
     * @param locationAPICulling   If true, the trail is removed from memory as soon as the player fleet leaves the
     *                             location the trail is in. If false, the trail is only "frozen" when the player leaves
     * @param locationAPI          Which locationAPI this trail is in; should ideally be in the same as the location the
     *                             linkedEntity is in.
     */
    public static void addTrailMemberAnimated(SectorEntityToken linkedEntity, float ID, SpriteAPI sprite, Vector2f position, float startSpeed, float endSpeed, float angle,
                                              float startAngularVelocity, float endAngularVelocity, float startSize, float endSize, Color startColor, Color endColor, float opacity,
                                              float inDuration, float mainDuration, float outDuration, int blendModeSRC, int blendModeDEST, float textureLoopLength, float textureScrollSpeed,
                                              Vector2f offsetVelocity, boolean locationAPICulling, LocationAPI locationAPI) {
        //Runs the same function, but only on the specific script instead of the static interface
        for (EveryFrameScript everyFrameScript : Global.getSector().getTransientScripts()) {
            if (everyFrameScript instanceof MagicCampaignTrailPlugin) {
                ((MagicCampaignTrailPlugin) everyFrameScript).addTrailMemberAnimatedInternal(linkedEntity, ID, sprite, position, startSpeed, endSpeed, angle,
                        startAngularVelocity, endAngularVelocity, startSize, endSize, startColor, endColor, opacity,
                        inDuration, mainDuration, outDuration, blendModeSRC, blendModeDEST, textureLoopLength, textureScrollSpeed,
                        offsetVelocity, locationAPICulling, locationAPI);
                break;
            }
        }
    }

    private void addTrailMemberAnimatedInternal(SectorEntityToken linkedEntity, float ID, SpriteAPI sprite, Vector2f position, float startSpeed, float endSpeed, float angle,
                                                float startAngularVelocity, float endAngularVelocity, float startSize, float endSize, Color startColor, Color endColor, float opacity,
                                                float inDuration, float mainDuration, float outDuration, int blendModeSRC, int blendModeDEST, float textureLoopLength, float textureScrollSpeed,
                                                Vector2f offsetVelocity, boolean locationAPICulling, LocationAPI locationAPI) {
        //Finds the correct maps, and ensures they are actually instantiated
        if (animMap.get(ID) == null) {
            animMap.put(ID, new MagicCampaignTrailTracker());
        }
        if (linkedEntity != null) {
            if (cuttingMapAnimated.get(linkedEntity) == null) {
                cuttingMapAnimated.put(linkedEntity, new ArrayList<Float>());
            }
            if (!cuttingMapAnimated.get(linkedEntity).contains(ID)) {
                cuttingMapAnimated.get(linkedEntity).add(ID);
            }
        }

        //Adds the correct locationAPI and locationAPICulling values to our tracker
        animMap.get(ID).locationAPI = locationAPI;
        animMap.get(ID).locationAPICulling = locationAPICulling;

        //Adjusts scroll speed to our most recent trail's value
        animMap.get(ID).scrollSpeed = textureScrollSpeed;

        //Adjusts the texture in the Trail Tracker to our most recently received texture, and flags it as being an Animated trail
        int texID = sprite.getTextureId();
        animMap.get(ID).isAnimated = true;
        animMap.get(ID).currentAnimRenderTexture = texID;

        //Creates the custom object we want
        MagicTrailObject objectToAdd = new MagicTrailObject(inDuration, mainDuration, outDuration, startSize, endSize, startAngularVelocity, endAngularVelocity,
                opacity, blendModeSRC, blendModeDEST, startSpeed, endSpeed, startColor, endColor, angle, position, textureLoopLength, 0, offsetVelocity,
                0f, 0f);

        //And finally add it to the correct location in our maps
        animMap.get(ID).addNewTrailObject(objectToAdd);
    }


    /**
     * A small function to get a unique ID for the trail member: *must* be saved in the function that generates the
     * trail, since if it changes it counts as a new trail altogether
     */
    public static float getUniqueID() {
        //Gets a value 0.1f higher than the previous maximum ID, and marks that as our previous maximum ID
        float toReturn = usedIDs + 0.1f;
        usedIDs = toReturn;
        return toReturn;
    }

    //Similar to above, but is *explicitly* intended for the cutTrailsOnEntity function, so is private
    private static float getUniqueCutterID() {

        //Gets a value 0.1f lower than the previous maximum ID, and marks that as our previous maximum ID
        float toReturn = usedCutterIDs - 0.1f;
        usedCutterIDs = toReturn;
        return toReturn;
    }


    /**
     * "Cuts" all trails on a designated entity, forcing new trail pieces to not link up with old ones. Should
     * be used before teleporting any entity, since it may have trails attached to it which will otherwise stretch
     * in unintended ways
     *
     * @param entity The entity you want to cut all trails on
     */
    public static void cutTrailsOnEntity(SectorEntityToken entity) {
        //Runs the same function, but only on the specific script instead of the static interface
        for (EveryFrameScript everyFrameScript : Global.getSector().getTransientScripts()) {
            if (everyFrameScript instanceof MagicCampaignTrailPlugin) {
                ((MagicCampaignTrailPlugin) everyFrameScript).cutTrailsOnEntityInternal(entity);
                break;
            }
        }
    }

    private void cutTrailsOnEntityInternal(SectorEntityToken entity) {
        //Iterate over all textures in the main map...
        for (Integer key : cuttingMap.keySet()) {
            //If our entity has any registered trails, cut them all off by giving them new, unique IDs
            if (cuttingMap.get(key).get(entity) != null) {
                for (float thisID : cuttingMap.get(key).get(entity)) {
                    if (mainMap.get(key).get(thisID) != null) {
                        mainMap.get(key).put(getUniqueCutterID(), mainMap.get(key).get(thisID));
                        mainMap.get(key).remove(thisID);
                    }
                }
            }
        }

        //Then, do something similar for the animated map
        if (cuttingMapAnimated.get(entity) != null) {
            for (float thisID : cuttingMapAnimated.get(entity)) {
                if (animMap.get(thisID) != null) {
                    animMap.put(getUniqueCutterID(), animMap.get(thisID));
                    animMap.remove(thisID);
                }
            }
        }
    }

    /**
     * @deprecated Use {@link #addTrailMemberSimple(SectorEntityToken, float, SpriteAPI, Vector2f, float, float, float, float, Color, float, float, boolean, Vector2f)} instead.
     */
    public static void AddTrailMemberSimple(SectorEntityToken linkedEntity, float ID, SpriteAPI sprite, Vector2f position, float speed, float angle, float startSize, float endSize, Color color,
                                            float opacity, float duration, boolean additive, Vector2f offsetVelocity) {
        addTrailMemberSimple(linkedEntity, ID, sprite, position, speed, angle, startSize, endSize, color, opacity, duration, additive, offsetVelocity);
    }

    /**
     * @deprecated Use {@link addTrailMemberAnimated(SectorEntityToken, float, SpriteAPI, Vector2f, float, float, float, float, float, float, float, Color, Color, float, float, float, float, int, int, float, float, Vector2f, boolean, LocationAPI)} instead.
     */
    public static void AddTrailMemberAnimated(SectorEntityToken linkedEntity, float ID, SpriteAPI sprite, Vector2f position, float startSpeed, float endSpeed, float angle,
                                              float startAngularVelocity, float endAngularVelocity, float startSize, float endSize, Color startColor, Color endColor, float opacity,
                                              float inDuration, float mainDuration, float outDuration, int blendModeSRC, int blendModeDEST, float textureLoopLength, float textureScrollSpeed,
                                              Vector2f offsetVelocity, boolean locationAPICulling, LocationAPI locationAPI) {
        addTrailMemberAnimated(linkedEntity, ID, sprite, position, startSpeed, endSpeed, angle, startAngularVelocity, endAngularVelocity, startSize, endSize, startColor, endColor, opacity,
                inDuration, mainDuration, outDuration, blendModeSRC, blendModeDEST, textureLoopLength, textureScrollSpeed, offsetVelocity, locationAPICulling, locationAPI);
    }

    /**
     * @deprecated Use {@link addTrailMemberAdvanced(SectorEntityToken, float, SpriteAPI, Vector2f, float, float, float, float, float, float, float, Color, Color, float, float, float, float, int, int, float, float, Vector2f, boolean, LocationAPI)} instead.
     */
    public static void AddTrailMemberAdvanced(SectorEntityToken linkedEntity, float ID, SpriteAPI sprite, Vector2f position, float startSpeed, float endSpeed, float angle,
                                              float startAngularVelocity, float endAngularVelocity, float startSize, float endSize, Color startColor, Color endColor, float opacity,
                                              float inDuration, float mainDuration, float outDuration, int blendModeSRC, int blendModeDEST, float textureLoopLength, float textureScrollSpeed,
                                              Vector2f offsetVelocity, boolean locationAPICulling, LocationAPI locationAPI) {
        addTrailMemberAdvanced(linkedEntity, ID, sprite, position, startSpeed, endSpeed, angle,
                startAngularVelocity, endAngularVelocity, startSize, endSize, startColor, endColor, opacity,
                inDuration, mainDuration, outDuration, blendModeSRC, blendModeDEST, textureLoopLength, textureScrollSpeed,
                offsetVelocity, locationAPICulling, locationAPI);
    }

    // ---------------------------------------- UTILITY CLASS DECLARATIONS --------------------------------------------

    /*-- Trail tracker class; close to how the in-combat implementation works, with the twist of keeping track of auto-culling, render layers and other minor adjustments --*/
    private class MagicCampaignTrailTracker {
        //For scrolling textures - NOTE: we always use the most recent scroll speed for the trail, if it for some reason changes mid-trail
        private float scrollingTextureOffset = 0f;
        private float scrollSpeed = 0f;

        //For animated textures: the trail counts as animated only if isAnimated = true
        private boolean isAnimated = false;
        private int currentAnimRenderTexture = 0;

        //For keeping track of LocationAPI culling; this should ideally *never* change, but if it somehow does it uses the most recent values
        private LocationAPI locationAPI = null;
        private boolean locationAPICulling = true;

        //For tracking the render layer
        private CampaignEngineLayers renderLayer = CampaignEngineLayers.ABOVE;

        private List<MagicTrailObject> allTrailParts = new ArrayList<>();
        private MagicTrailObject latestTrailObject;


        //Adds a new object to the trail, at the end (start visually) of our existing ones
        private void addNewTrailObject(MagicTrailObject objectToAdd) {
            allTrailParts.add(objectToAdd);
        }

        //The heavy, main function: render the entire trail
        private void renderTrail(int textureID) {
            //First, clear all dead objects, as they can be a pain to calculate around
            clearAllDeadObjects();

            //Then, if we have too few segments to render properly, cancel the function
            int size = allTrailParts.size();
            if (size <= 1) {
                return;
            }

            //If we are animated, we use our "currentAnimRenderTexture" rather than the textureID we just got supplied
            int trueTextureID = textureID;
            if (isAnimated) {
                trueTextureID = currentAnimRenderTexture;
            }

            //Otherwise, we actually render the thing
            //This part instantiates OpenGL
            glPushMatrix();
            glEnable(GL_BLEND);
            glEnable(GL_TEXTURE_2D);
            glBlendFunc(allTrailParts.get(allTrailParts.size() - 1).blendModeSRC, allTrailParts.get(allTrailParts.size() - 1).blendModeDEST); //NOTE: uses the most recent blend mode added to the trail
            glBindTexture(GL_TEXTURE_2D, trueTextureID);
            glBegin(GL_QUADS);


            //Iterate through all trail parts except the oldest one: the idea is that each part renders in relation to the *previous* part
            //Note that this behaviour is inverted compared to forward-propagating render (the old method)
            float texDistTracker = 0f;
            float texLocator = 0f;
            for (int i = size - 1; i > 0; i--) {
                //First, get a handle for our parts so we can make the code shorter
                MagicTrailObject part1 = allTrailParts.get(i);    //Current part
                MagicTrailObject part2 = allTrailParts.get(i - 1);    //Next part

                //Then, determine the corner points of both this and the next trail part
                float partRadius = part1.currentSize * 0.5f;
                Vector2f point1Left = MathUtils.getPointOnCircumference(part1.currentLocation, partRadius, part1.angle - 90f);
                Vector2f point1Right = MathUtils.getPointOnCircumference(part1.currentLocation, partRadius, part1.angle + 90f);
                partRadius = part2.currentSize * 0.5f;
                Vector2f point2Left = MathUtils.getPointOnCircumference(part2.currentLocation, partRadius, part2.angle - 90f);
                Vector2f point2Right = MathUtils.getPointOnCircumference(part2.currentLocation, partRadius, part2.angle + 90f);

                //Saves an easy value for the distance between the current two parts
                float partDistance = MathUtils.getDistance(part1.currentLocation, part2.currentLocation);

                //-------------------------------------------------------------------Actual rendering shenanigans------------------------------------------------------------------------------------------
                //If we are outside the viewport, don't render at all! Just tick along our texture tracker, and do nothing else
                if (!Global.getSector().getViewport().isNearViewport(part1.currentLocation, partDistance * 3f)) {
                    //Change our texture distance tracker depending on looping mode
                    //  -If we have -1 as loop length, we ensure that the entire texture is used over the entire trail
                    //  -Otherwise, we adjust the texture distance upward to account for how much distance there is between our two points
                    if (part1.textureLoopLength <= 0f) {
                        texDistTracker = (float) (i - 1) / (float) size;
                    } else {
                        texDistTracker += partDistance / part1.textureLoopLength;
                    }

                    continue;
                }

                //Changes opacity slightly at beginning and end: the last and first 2 segments have lower opacity
                float opacityMult = 1f;
                if (i < 2) {
                    opacityMult *= ((float) i / 2f);
                } else if (i > size - 3) {
                    opacityMult *= ((float) size - 1f - (float) i) / 2f;
                }

                //Sets the current render color
                glColor4ub((byte) part1.currentColor.getRed(), (byte) part1.currentColor.getGreen(), (byte) part1.currentColor.getBlue(), (byte) (part1.currentOpacity * opacityMult * 255));

                texLocator = texDistTracker + scrollingTextureOffset;

                //Sets corner 1, or the first left corner
                glTexCoord2f(0f, texLocator);
                glVertex2f(point1Left.getX(), point1Left.getY());

                //Sets corner 2, or the first right corner
                glTexCoord2f(1f, texLocator);
                glVertex2f(point1Right.getX(), point1Right.getY());

                //Change our texture distance tracker depending on looping mode
                //  -If we have -1 as loop length, we ensure that the entire texture is used over the entire trail
                //  -Otherwise, we adjust the texture distance upward to account for how much distance there is between our two points
                if (part1.textureLoopLength <= 0f) {
                    texDistTracker = (float) (i - 1) / (float) size;
                } else {
                    texDistTracker += partDistance / part1.textureLoopLength;
                }

                //Changes opacity slightly at beginning and end: the last and first 2 segments have lower opacity
                opacityMult = 1f;
                if ((i - 1) < 2) {
                    opacityMult *= ((float) (i - 1) / 2f);
                } else if ((i - 1) > size - 3) {
                    opacityMult *= ((float) size - 1f - ((float) i - 1f)) / 2f;
                }

                //Changes render color to our next segment's opacity
                glColor4ub((byte) part2.currentColor.getRed(),
                        (byte) part2.currentColor.getGreen(),
                        (byte) part2.currentColor.getBlue(),
                        (byte) (part2.currentOpacity * opacityMult * 255));

                texLocator = texDistTracker + scrollingTextureOffset;

                //Sets corner 3, or the second right corner
                glTexCoord2f(1f, texLocator);
                glVertex2f(point2Right.getX(), point2Right.getY());

                //Sets corner 4, or the second left corner
                glTexCoord2f(0f, texLocator);
                glVertex2f(point2Left.getX(), point2Left.getY());
            }

            //And finally stops OpenGL
            glEnd();
            glPopMatrix();
        }

        //Quickhand function to tick down all trail objects at once, by an equal amount of time. Also ticks texture scrolling, if we have it
        private void tickTimersInTrail(float amount) {
            for (MagicTrailObject part : allTrailParts) {
                part.tick(amount);
            }

            //Defines the scroll speed in 1/1000th of a full texture per second
            scrollingTextureOffset += (amount * scrollSpeed) / 1000f;
        }

        //Quickhand function to remove all trail objects which has timed out
        private void clearAllDeadObjects() {
            List<MagicTrailObject> toRemove = new ArrayList<>();
            for (MagicTrailObject part : allTrailParts) {
                if (part.getSpentLifetime() >= part.getTotalLifetime()) {
                    toRemove.add(part);
                }
            }

            //If there is a new trail object, the texture will scroll back to make sure they keep in the same place
            if (allTrailParts.size() > 0) {
                MagicTrailObject currentLatestTrailObject = allTrailParts.get(allTrailParts.size() - 1);
                if (latestTrailObject == null) {
                    latestTrailObject = currentLatestTrailObject;
                } else if (latestTrailObject != currentLatestTrailObject) {
                    float partDistance = MathUtils.getDistance(latestTrailObject.currentLocation, currentLatestTrailObject.currentLocation);
                    //scroll back
                    scrollingTextureOffset -= partDistance / latestTrailObject.textureLoopLength;
                    latestTrailObject = currentLatestTrailObject;
                }
            }

            for (MagicTrailObject partToRemove : toRemove) {
                allTrailParts.remove(partToRemove);
            }
        }
    }


    /*-- Trail object class; very close in nature to the combat-engine version, though with some minor variations and fixes --*/
    private class NicToyCustomCampaignTrailObject {
        //Private, non-varying values
        private float inDuration = 0f;
        private float mainDuration = 0f;
        private float outDuration = 0f;
        private float startSize = 0f;
        private float endSize = 0f;
        private float startAngleVelocity = 0f;
        private float endAngleVelocity = 0f;
        private float mainOpacity = 0f;
        private float startSpeed = 0f;
        private float endSpeed = 0f;
        private Color startColor = new Color(255, 255, 255);
        private Color endColor = new Color(255, 255, 255);

        //Public, non-varying values
        public int blendModeSRC = 0;
        public int blendModeDEST = 0;
        public float textureLoopLength = 0;
        public Vector2f offsetVelocity = new Vector2f(0f, 0f);

        //Public, varying values
        public Color currentColor = new Color(255, 255, 255);
        public float currentSize = 0f;
        public float spentLifetime = 0f;
        public float currentAngularVelocity = 0f;
        public float angle = 0f;
        public float currentSpeed = 0f;
        public Vector2f currentLocation = new Vector2f(0f, 0f);
        public float currentOpacity = 0f;

        //Main instantiator: generates a full CustomCampaignTrailObject with all necessary values
        public NicToyCustomCampaignTrailObject(float inDuration, float mainDuration, float outDuration, float startSize, float endSize, float startAngleVelocity,
                                               float endAngleVelocity, float mainOpacity, int blendModeSRC, int blendModeDEST, float startSpeed, float endSpeed,
                                               Color startColor, Color endColor, float angle, Vector2f spawnLocation, float textureLoopLength, Vector2f offsetVelocity) {
            this.inDuration = inDuration;
            this.mainDuration = mainDuration;
            this.outDuration = outDuration;
            this.startSize = startSize;
            this.endSize = endSize;
            this.startAngleVelocity = startAngleVelocity;
            this.endAngleVelocity = endAngleVelocity;
            this.mainOpacity = mainOpacity;
            this.blendModeSRC = blendModeSRC;
            this.blendModeDEST = blendModeDEST;
            this.startSpeed = startSpeed;
            this.endSpeed = endSpeed;
            this.startColor = startColor;
            this.endColor = endColor;
            this.angle = angle;
            this.currentLocation.x = spawnLocation.x;
            this.currentLocation.y = spawnLocation.y;
            this.textureLoopLength = textureLoopLength;

            this.currentColor = startColor;
            this.currentSize = startSize;
            this.spentLifetime = 0f;
            this.currentAngularVelocity = startAngleVelocity;
            this.currentSpeed = startSpeed;
            if (inDuration > 0) {
                this.currentOpacity = 0f;
            } else {
                this.currentOpacity = mainOpacity;
            }

            this.offsetVelocity.x = offsetVelocity.x;
            this.offsetVelocity.y = offsetVelocity.y;
        }

        //Modifies lifetime, position and all other things time-related
        public void tick(float amount) {
            //Increases lifetime
            spentLifetime += amount;

            //If our spent lifetime is higher than our total lifetime, set it to our total lifetime
            if (spentLifetime > getTotalLifetime()) {
                spentLifetime = getTotalLifetime();
            }

            //Slides all values along depending on lifetime
            currentSize = startSize * (1 - (spentLifetime / getTotalLifetime())) + endSize * (spentLifetime / getTotalLifetime());
            currentSpeed = startSpeed * (1 - (spentLifetime / getTotalLifetime())) + endSpeed * (spentLifetime / getTotalLifetime());
            currentAngularVelocity = startAngleVelocity * (1 - (spentLifetime / getTotalLifetime())) + endAngleVelocity * (spentLifetime / getTotalLifetime());
            int red = ((int) (startColor.getRed() * (1 - (spentLifetime / getTotalLifetime())) + endColor.getRed() * (spentLifetime / getTotalLifetime())));
            int green = ((int) (startColor.getGreen() * (1 - (spentLifetime / getTotalLifetime())) + endColor.getGreen() * (spentLifetime / getTotalLifetime())));
            int blue = ((int) (startColor.getBlue() * (1 - (spentLifetime / getTotalLifetime())) + endColor.getBlue() * (spentLifetime / getTotalLifetime())));
            currentColor = new Color(red, green, blue);

            //Adjusts opacity: slightly differently handled than the otherwise pure linear value sliding
            currentOpacity = mainOpacity;
            if (spentLifetime < inDuration) {
                currentOpacity = mainOpacity * spentLifetime / inDuration;
            } else if (spentLifetime > (inDuration + mainDuration)) {
                currentOpacity = mainOpacity * (1f - ((spentLifetime - (inDuration + mainDuration)) / outDuration));
            }

            //Calculates new position and angle from respective velocities
            angle += currentAngularVelocity * amount;
            currentLocation.x += (FastTrig.cos(Math.toRadians(angle)) * currentSpeed + offsetVelocity.x) * amount;
            currentLocation.y += (FastTrig.sin(Math.toRadians(angle)) * currentSpeed + offsetVelocity.y) * amount;
        }

        public float getSpentLifetime() {
            return spentLifetime;
        }

        public float getTotalLifetime() {
            return inDuration + mainDuration + outDuration;
        }
    }
}