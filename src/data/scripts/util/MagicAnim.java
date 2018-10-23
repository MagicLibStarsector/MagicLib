/*
By Tartiflette
 */
package data.scripts.util;

import org.lazywizard.lazylib.FastTrig;

public class MagicAnim {    
    
    /**
     * Smooth
     * Translation of a 0 to 1 value to the same range with smooth ease in and ease out.
     * 0, 0.5 and 1 returns the same but 0.25 returns 0.11 and 0.75 return 0.89
     * 
     * @param x
     * Float clamped from 0 to 1
     * 
     * @return 
     * Smoothed float value
     */
    public static float smooth (float x){
        return 0.5f - ((float)(FastTrig.cos(Math.min(1, Math.max(0, x))*Math.PI) /2 ));
    }
    
    /**
     * Arbitrary Smooth
     * Translation of an arbitrary value in a min-max range to the same range with smooth ease in and ease out.
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
        value = (value/(max-min))-min;
        value = (0.5f - ((float)(FastTrig.cos(value*Math.PI) /2 )));
        value *= (max-min) + min;
        return value;
    }
    
    
    /**
     * Offset
     * Linear translation of a 0 to 1 value to a different arbitrary range.
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
    public static float offset (float x, float start, float end){
        return (float) Math.min(1, Math.max( 0 , x))*(end-start) + start;
    }
    
    /**
     * Smooth + offset
     * Translation of a 0 to 1 value to a different arbitrary range with smooth ease in and ease out.
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
    public static float SO (float x, float start, float end){
        return (0.5f - ((float)(FastTrig.cos(Math.min(1, Math.max(0, x))*Math.PI) /2 ))) * (end-start) + start;
    }
    
    /**
     * Return + Smooth + Offset
     * Translation of a 0 to 1 to a "back-and-forth" value in a different arbitrary range with smooth ease in and ease out.
     * The output will be a the minimal value with 0 and 1 inputs, and maximum value with 0.5 input.
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
     * smooth "back-and-forth" float value in the new range
     */
    public static float RSO (float x, float start, float end){
        return (0.5f - ((float)(FastTrig.cos(Math.min(1, Math.max(0, x))*Math.PI*2) /2 ))) * (end-start) + start;
    }  
}