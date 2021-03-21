/*
By Tartiflette
 */
package data.scripts.util;

import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;

public class MagicAnim {    
    
    
    /**
     * Smooth
     * Translates a value in a (0,1) range to a value in the same range with smooth ease in and ease out.
     * 0, 0.5 and 1 returns the same but 0.25 returns 0.11 and 0.75 return 0.89
     * 
     * @param x
     * Float clamped from 0 to 1
     * 
     * @return 
     * Smoothed float value
     */
    public static float S (float x){
        return 0.5f - ((float)(FastTrig.cos(Math.min(1, Math.max(0, x))*MathUtils.FPI) /2 ));
    }
    /**
     * Translates a value in a (0,1) range to a value in the same range with smooth ease in and ease out.
     * 0, 0.5 and 1 returns the same but 0.25 returns 0.11 and 0.75 return 0.89
     * 
     * @param x
     * Float clamped from 0 to 1
     * 
     * @return 
     * Smoothed float value
     */
    public static float smooth (float x){
        return 0.5f - ((float)(FastTrig.cos(Math.min(1, Math.max(0, x))*MathUtils.FPI) /2 ));
    }
    
    /**
     * Arbitrary Smooth
     * Translates a value in a (min,max) range to a value in the same range with smooth ease in and ease out.
     * 
     * @param x
     * Float clamped from 0 to 1
     * 
     * @param min
     * minimal range value
     * 
     * @param max
     * maximal range value
     * 
     * @return 
     * smoothed float value in that range
     */
    public static float AS (float x, float min, float max){
        float value=Math.min(max, Math.max(min, x));
        value = (value-min)/(max-min);
        value = (0.5f - ((float)(FastTrig.cos(value*MathUtils.FPI) /2 )));
        value *= (max-min) + min;
        return value;
    }
    /**
     * Translates a value in a (min,max) range to a value in the same range with smooth ease in and ease out.
     * 
     * @param x
     * Float clamped from 0 to 1
     * 
     * @param min
     * minimal range value
     * 
     * @param max
     * maximal range value
     * 
     * @return 
     * smoothed float value in that range
     */
    public static float arbitrarySmooth (float x, float min, float max){
        float value=Math.min(max, Math.max(min, x));
        value = (value-min)/(max-min);
        value = (0.5f - ((float)(FastTrig.cos(value*MathUtils.FPI) /2 )));
        value *= (max-min) + min;
        return value;
    }
    
    /**
     * Translates a value from a (0,1) range to a value in a (min,max) range.
     * 
     * @param x
     * Float clamped from 0 to 1
     * 
     * @param min
     * new range minimal value
     * 
     * @param max
     * new range maximal value
     * 
     * @return 
     * float value in the new range
     */
    public static float range (float x, float min, float max){
        return (float) Math.min(1, Math.max( 0 , x))*(max-min) + min;
    }
    
    /**
     * USE normalizeRange() INSTEAD, will be removed with 0.95
     * @deprecated use {@link normalizeRange()}
     */
    @Deprecated
    public static float offset (float x, float start, float end){
        return (float) Math.min(1, Math.max( 0 , (x-start)*(1/(end-start))));
    }
    /**
     * Translates a value in a (start,end) range into a value in a (0,1) range.
     * 
     * @param x
     * Float clamped from 0 to 1
     * 
     * @param start
     * new range minimal value
     * 
     * @param end
     * new range maximal value
     * 
     * @return 
     * float value in the new range
     */
    public static float normalizeRange (float x, float start, float end){
        return (float) Math.min(1, Math.max( 0 , (x-start)*(1/(end-start))));
    }
    /**
     * Normalize Range
     * Translates a value in a (start,end) range into a value in a (0,1) range.
     * 
     * @param x
     * Float clamped from 0 to 1
     * 
     * @param start
     * new range minimal value
     * 
     * @param end
     * new range maximal value
     * 
     * @return 
     * float value in the new range
     */
    public static float NR (float x, float start, float end){
        return (float) Math.min(1, Math.max( 0 , (x-start)*(1/(end-start))));
    }
    
    /**
     * USE smoothRange() OR SR() INSTEAD, will be removed with 0.95
     * @deprecated use {@link SR()}
     */
    @Deprecated
    public static float SO (float x, float start, float end){
        return 0.5f - (float)( FastTrig.cos( Math.min( 1, Math.max( 0 , (x-start)*(1/(end-start)))) *MathUtils.FPI ) /2 );
    }
    /**
     * Smooth + Range
     * Translates a value from a (start,end) range into a value in a (0,1) range with smooth ease in and ease out.
     * Allows to segment the value of a timer or an effect level into different shorter sliders
     * 
     * @param x
     * Float clamped from 0 to 1
     * 
     * @param start
     * new range minimal value
     * 
     * @param end
     * new range maximal value
     * 
     * @return 
     * smoothed float value in the new range
     */
    public static float SR (float x, float start, float end){
        return 0.5f - (float)( FastTrig.cos( Math.min( 1, Math.max( 0 , (x-start)*(1/(end-start)))) *MathUtils.FPI ) /2 );
    }
    /**
     * Translates a value from a (start,end) range into a value in a (0,1) range with smooth ease in and ease out.
     * Allows to segment the value of a timer or an effect level into different shorter sliders
     * 
     * @param x
     * Float clamped from 0 to 1
     * 
     * @param start
     * new range minimal value
     * 
     * @param end
     * new range maximal value
     * 
     * @return 
     * smoothed float value in the new range
     */
    public static float smoothRange (float x, float start, float end){
        return 0.5f - (float)( FastTrig.cos( Math.min( 1, Math.max( 0 , (x-start)*(1/(end-start)))) *MathUtils.FPI ) /2 );
    }
    
    /**
     * USE smoothRangeReturn() OR SRR() INSTEAD, will be removed with 0.95
     * @deprecated use {@link SRR()}
     */
    @Deprecated
    public static float RSO (float x, float start, float end){
        return 0.5f - (float)( FastTrig.cos( Math.min( 1, Math.max( 0 , (x-start)*(1/(end-start)))) *MathUtils.FPI*2 ) /2 );
    }
    /**
     * Translates a value in a (start,end) range into a "back-and-forth" value in a (0,1) range with smooth ease in and ease out.
     * 
     * @param x
     * Float clamped from start to end
     * 
     * @param start
     * range minimal value
     * 
     * @param end
     * range maximal value
     * 
     * @return 
     * smooth "back-and-forth" float value equals to 0 at start and end, and 1 at the mid-point between them
     */
    public static float smoothRangeReturn (float x, float start, float end){
        return 0.5f - (float)( FastTrig.cos( Math.min( 1, Math.max( 0 , (x-start)*(1/(end-start)))) *MathUtils.FPI*2 ) /2 );
    }
    /**
     * Smooth + Range + Return
     * Translates a value in a (start,end) range into a "back-and-forth" value in a (0,1) range with smooth ease in and ease out.
     * 
     * @param x
     * Float clamped from start to end
     * 
     * @param start
     * range minimal value
     * 
     * @param end
     * range maximal value
     * 
     * @return 
     * smooth "back-and-forth" float value equals to 0 at start and end, and 1 at the mid-point between them
     */
    public static float RSR (float x, float start, float end){
        return 0.5f - (float)( FastTrig.cos( Math.min( 1, Math.max( 0 , (x-start)*(1/(end-start)))) *MathUtils.FPI*2 ) /2 );
    }
    
    /**
     * Cycle within range
     * Restricts a value to a cycling range, produces a seesaw.
     * 
     * @param x
     * Float
     * 
     * @param min
     * cycle minimal value
     * 
     * @param max
     * cycle maximal value
     * 
     * @return
     * "seesaw" value that cycles within the (min,max) range
     */
    public static float cycle (float x, float min, float max){
        float range = max - min;
        float i = (float)Math.floor((x - min) / range);
        return min + x - range*i;
    }
}