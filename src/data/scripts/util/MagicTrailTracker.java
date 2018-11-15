//By Nicke535
//This file isn't meant to be used directly; use the MagicTrailPlugin to actually do anything properly
package data.scripts.util;

import com.fs.starfarer.api.Global;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

//This class handles each "segment" of a trail: each MagicTrailObject within the MagicTrailTracker is considered to be linked to the other objects.
//To make a new "segment" of trail, unrelated to the others, you have to create a new Tracker. The trail is invisible until at least two objects are in it
public class MagicTrailTracker {
    //For scrolling textures - NOTE: we always use the most recent scroll speed for the trail, if it for some reason changes mid-trail
    private float scrollingTextureOffset = 0f;
    public float scrollSpeed = 0f;

    //For animated textures: the trail counts as animated only if isAnimated = true
    public boolean isAnimated = false;
    public int currentAnimRenderTexture = 0;

    private List<MagicTrailObject> allTrailParts = new ArrayList<MagicTrailObject>();

    //Adds a new object to the trail, at the end (start visually) of our existing ones
    public void addNewTrailObject (MagicTrailObject objectToAdd) {
        allTrailParts.add(objectToAdd);
    }

    //The heavy, main function: render the entire trail
    public void renderTrail (int textureID) {
        //First, clear all dead objects, as they can be a pain to calculate around
        clearAllDeadObjects();

        //Then, if we have too few segments to render properly, cancel the function
        if (allTrailParts.size() <= 1) {
            return;
        }

        //If we are animated, we use our "currentAnimRenderTexture" rather than the textureID we just got supplied
        int trueTextureID = textureID;
        if (isAnimated) {
            trueTextureID = currentAnimRenderTexture;
        }

        //Otherwise, we actually render the thing
        //This part instantiates OpenGL
        glEnable(GL_BLEND);
        glEnable(GL_TEXTURE_2D);
        glBlendFunc(allTrailParts.get(allTrailParts.size()-1).blendModeSRC, allTrailParts.get(allTrailParts.size()-1).blendModeDEST); //NOTE: uses the most recent blend mode added to the trail
        glBindTexture(GL_TEXTURE_2D, trueTextureID);
        glBegin(GL_QUADS);

        //Iterate through all trail parts except the most recent one: the idea is that each part renders in relation to the *next* part
        float texDistTracker = 0f;
        for (int i = 0; i < allTrailParts.size()-1; i++) {
            //First, get a handle for our parts so we can make the code shorter
            MagicTrailObject part1 = allTrailParts.get(i);   //Current part
            MagicTrailObject part2 = allTrailParts.get(i+1); //Next part

            //Then, determine the corner points of both this and the next trail part
            Vector2f point1Left = new Vector2f(part1.currentLocation.x + ((part1.currentSize / 2) * (float)FastTrig.cos(Math.toRadians(part1.angle + 90))), part1.currentLocation.y + ((part1.currentSize / 2) * (float)FastTrig.sin(Math.toRadians(part1.angle + 90))));
            Vector2f point1Right = new Vector2f(part1.currentLocation.x + ((part1.currentSize / 2) * (float)FastTrig.cos(Math.toRadians(part1.angle - 90))), part1.currentLocation.y + ((part1.currentSize / 2) * (float)FastTrig.sin(Math.toRadians(part1.angle - 90))));
            Vector2f point2Left = new Vector2f(part2.currentLocation.x + ((part2.currentSize / 2) * (float)FastTrig.cos(Math.toRadians(part2.angle + 90))), part2.currentLocation.y + ((part2.currentSize / 2) * (float)FastTrig.sin(Math.toRadians(part2.angle + 90))));
            Vector2f point2Right = new Vector2f(part2.currentLocation.x + ((part2.currentSize / 2) * (float)FastTrig.cos(Math.toRadians(part2.angle - 90))), part2.currentLocation.y + ((part2.currentSize / 2) * (float)FastTrig.sin(Math.toRadians(part2.angle - 90))));

            //Saves an easy value for the distance between the current two parts
            float partDistance = MathUtils.getDistance(part1.currentLocation, part2.currentLocation);

            //-------------------------------------------------------------------Actual rendering shenanigans------------------------------------------------------------------------------------------
            //If we are outside the viewport, don't render at all! Just tick along our texture tracker, and do nothing else
            if (!Global.getCombatEngine().getViewport().isNearViewport(part1.currentLocation, part1.currentSize + 1f)) {
                //Change our texture distance tracker depending on looping mode
                //  -If we have -1 as loop length, we ensure that the entire texture is used over the entire trail
                //  -Otherwise, we adjust the texture distance upward to account for how much distance there is between our two points
                if (part1.textureLoopLength <= 0f) {
                    texDistTracker = (float)(i + 1) / (float)allTrailParts.size();
                } else {
                    texDistTracker += partDistance / part1.textureLoopLength;
                }

                continue;
            }

            //Changes opacity slightly at beginning and end: the last and first 2 segments have lower opacity
            float opacityMult = 1f;
            if (i < 2) {
                opacityMult *= ((float)i/2f);
            } else if (i > allTrailParts.size()-3) {
                opacityMult *= ((float)allTrailParts.size()-1f-(float)i)/2f;
            }

            //Sets the current render color
            glColor4ub((byte)part1.currentColor.getRed(),(byte)part1.currentColor.getGreen(),(byte)part1.currentColor.getBlue(),(byte)(part1.currentOpacity * opacityMult * 255));

            //Sets corner 1, or the first left corner
            glTexCoord2f(0, texDistTracker + scrollingTextureOffset);
            glVertex2f(point1Left.getX(),point1Left.getY());

            //Sets corner 2, or the first right corner
            glTexCoord2f(1, texDistTracker + scrollingTextureOffset);
            glVertex2f(point1Right.getX(),point1Right.getY());

            //Change our texture distance tracker depending on looping mode
            //  -If we have -1 as loop length, we ensure that the entire texture is used over the entire trail
            //  -Otherwise, we adjust the texture distance upward to account for how much distance there is between our two points
            if (part1.textureLoopLength <= 0f) {
                texDistTracker = (float)(i + 1) / (float)allTrailParts.size();
            } else {
                texDistTracker += partDistance / part1.textureLoopLength;
            }

            //Changes opacity slightly at beginning and end: the last and first 2 segments have lower opacity
            opacityMult = 1f;
            if ((i + 1) < 2) {
                opacityMult *= ((float)(i+1)/2f);
            } else if ((i + 1) > allTrailParts.size()-3) {
                opacityMult *= ((float)allTrailParts.size()-2f-(float)i)/2f;
            }

            //Changes render color to our next segment's opacity
            glColor4ub((byte)part2.currentColor.getRed(),(byte)part2.currentColor.getGreen(),(byte)part2.currentColor.getBlue(),(byte)(part2.currentOpacity * opacityMult * 255));

            //Sets corner 3, or the second right corner
            glTexCoord2f(1, texDistTracker + scrollingTextureOffset);
            glVertex2f(point2Right.getX(),point2Right.getY());

            //Sets corner 4, or the second left corner
            glTexCoord2f(0, texDistTracker + scrollingTextureOffset);
            glVertex2f(point2Left.getX(),point2Left.getY());
        }

        //And finally stops OpenGL
        glEnd();
    }

    //Quickhand function to tick down all trail objects at once, by an equal amount of time. Also ticks texture scrolling, if we have it
    public void tickTimersInTrail (float amount) {
        for (MagicTrailObject part : allTrailParts) {
            part.tick(amount);
        }

        //Defines the scroll speed in 1/1000th of a full texture per second
        scrollingTextureOffset += (amount * scrollSpeed) / 1000f;
    }

    //Quickhand function to remove all trail objects which has timed out or should be auto-culled due to being too far offscreen
    public void clearAllDeadObjects (){
        List<MagicTrailObject> toRemove = new ArrayList<MagicTrailObject>();
        for (MagicTrailObject part : allTrailParts) {
            if (part.getSpentLifetime() >= part.getTotalLifetime() || (!Global.getSector().getViewport().isNearViewport(part.currentLocation, part.aggressiveCulling) && part.aggressiveCulling < 0f)) {
                toRemove.add(part);
            }
        }

        for (MagicTrailObject part : toRemove) {
            allTrailParts.remove(part);
        }
    }
}