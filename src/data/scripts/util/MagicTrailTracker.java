//Optimized by Originem
//By Nicke535
//This file isn't meant to be used directly; use the MagicTrailPlugin to actually do anything properly. Your mod will
//most likely lose backwards-compatibility if you try to call this class' constructor manually, so don't.
package data.scripts.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

//This class handles each "segment" of a trail: each MagicTrailObject within the MagicTrailTracker is considered to be linked to the other objects.
//To make a new "segment" of trail, unrelated to the others, you have to create a new Tracker. The trail is invisible until at least two objects are in it
@Deprecated
public class MagicTrailTracker {
    //For scrolling textures - NOTE: we always use the most recent scroll speed for the trail, if it for some reason changes mid-trail
    private float scrollingTextureOffset = 0f;
    public float scrollSpeed = 0f;

    //For legacy forward-propagating trail scrolling; causes some issues, but might remove stuttering when spawning trails slower than once-per-frame
    public boolean usesForwardPropagation = false;

    public float textureOffset = -1;

    //For animated textures: the trail counts as animated only if isAnimated = true
    public boolean isAnimated = false;
    public int currentAnimRenderTexture = 0;

    //If the tracker remain empty for 3 seconds, then expire
    private boolean isExpired = false;
    private float remainEmptyElapsed = 0f;

    public boolean isExpired() {
        return isExpired;
    }

    private final List<MagicTrailObject> allTrailParts = new ArrayList<>();
    private MagicTrailObject latestTrailObject;

    //Adds a new object to the trail, at the end (start visually) of our existing ones
    public void addNewTrailObject(MagicTrailObject objectToAdd) {
        allTrailParts.add(objectToAdd);
    }

    //The heavy, main function: render the entire trail
    public void renderTrail(int textureID) {
        //First, clear all dead objects, as they can be a pain to calculate around
//        clearAllDeadObjects();

        //Then, if we have too few segments to render properly, cancel the function
        int size = allTrailParts.size();
        if (size <= 1) {
            return;
        }

        //New trail object's movement
        MagicTrailObject currentLatestTrailObject = allTrailParts.get(size - 1);
        if (latestTrailObject == null) {
            latestTrailObject = currentLatestTrailObject;
        } else if (latestTrailObject != currentLatestTrailObject) {
            float partDistance = MathUtils.getDistance(latestTrailObject.currentLocation, currentLatestTrailObject.currentLocation);
            //scroll back
            scrollingTextureOffset -= partDistance / currentLatestTrailObject.textureLoopLength;
            latestTrailObject = currentLatestTrailObject;
        }

        //If we are animated, we use our "currentAnimRenderTexture" rather than the textureID we just got supplied
        int trueTextureID = textureID;
        if (isAnimated) {
            trueTextureID = currentAnimRenderTexture;
        }

        //Otherwise, we actually render the thing
        //This part instantiates OpenGL
        glPushMatrix();
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, trueTextureID);
        glEnable(GL_BLEND);
        glBlendFunc(allTrailParts.get(size - 1).blendModeSRC, allTrailParts.get(size - 1).blendModeDEST); //NOTE: uses the most recent blend mode added to the trail
        glBegin(GL_QUADS);

        //ADDITION 28/2-2019 Nicke535: chooses a render mode depending on if we use the old "forward-propagating-render" option
        CombatEngineAPI engine = Global.getCombatEngine();
        if (usesForwardPropagation) {
            //Iterate through all trail parts except the most recent one: the idea is that each part renders in relation to the *next* part
            float texDistTracker = currentLatestTrailObject.textureOffset;
            float texLocator = 0f;
            for (int i = 0; i < size - 1; i++) {
                //First, get a handle for our parts so we can make the code shorter
                MagicTrailObject part1 = allTrailParts.get(i);   //Current part
                MagicTrailObject part2 = allTrailParts.get(i + 1); //Next part

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
                if (!engine.getViewport().isNearViewport(part1.currentLocation, partDistance * 2f)) {
                    //Change our texture distance tracker depending on looping mode
                    //  -If we have -1 as loop length, we ensure that the entire texture is used over the entire trail
                    //  -Otherwise, we adjust the texture distance upward to account for how much distance there is between our two points
                    if (part1.textureLoopLength <= 0f) {
                        texDistTracker = (float) (i + 1) / (float) size;
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
                    texDistTracker = (float) (i + 1) / (float) size;
                } else {
                    texDistTracker += partDistance / part1.textureLoopLength;
                }

                //Changes opacity slightly at beginning and end: the last and first 2 segments have lower opacity
                opacityMult = 1f;
                if ((i + 1) < 2) {
                    opacityMult *= ((float) (i + 1) / 2f);
                } else if ((i + 1) > size - 3) {
                    opacityMult *= ((float) size - 2f - (float) i) / 2f;
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
        } else {
            //Iterate through all trail parts except the oldest one: the idea is that each part renders in relation to the *previous* part
            //Note that this behaviour is inverted compared to forward-propagating render (the old method)
            float texDistTracker = currentLatestTrailObject.textureOffset;
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
                if (!engine.getViewport().isNearViewport(part1.currentLocation, partDistance * 2f)) {
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
        }

        //And finally stops OpenGL
        glEnd();
        glPopMatrix();
    }

    //Quickhand function to tick down all trail objects at once, by an equal amount of time. Also ticks texture scrolling, if we have it
    public void tickTimersInTrail(float amount) {
        if (isExpired) return;
        // Tick the objects or remove it while its time
        Iterator<MagicTrailObject> allTrailPartsIter = allTrailParts.iterator();
        while (allTrailPartsIter.hasNext()) {
            MagicTrailObject part = allTrailPartsIter.next();
            if (part.getSpentLifetime() >= part.getTotalLifetime()) {
                allTrailPartsIter.remove();
            } else {
                part.tick(amount);
            }
        }

        //If there is a new trail object, the texture will scroll back to make sure they keep in the same place
        if (!allTrailParts.isEmpty()) {
            remainEmptyElapsed = 0f;
        } else {
            //if the allTrailParts is empty, just make it expire after 3 seconds
            latestTrailObject = null;
            remainEmptyElapsed += amount;
            if (remainEmptyElapsed >= 3f) {
                isExpired = true;
            }
        }

        //Defines the scroll speed in 1/1000th of a full texture per second
        scrollingTextureOffset -= (amount * scrollSpeed) / 1000f;
    }

    //Quickhand function to remove all trail objects which has timed out
    @Deprecated
    public void clearAllDeadObjects() {
//        Iterator<MagicTrailObject> allTrailPartsIter = allTrailParts.iterator();
//        while (allTrailPartsIter.hasNext()) {
//            MagicTrailObject part = allTrailPartsIter.next();
//            if (part.getSpentLifetime() >= part.getTotalLifetime()) {
//                allTrailPartsIter.remove();
//            }
//        }
    }

}