/*
By Tartiflette
 */
package data.scripts.util;

import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;

@Deprecated
public class MagicAnim {

    /**
     * Translates a value in a (0,1) range to a value in the same range with smooth ease in and ease out.
     * 0, 0.5 and 1 returns the same but 0.25 returns 0.11 and 0.75 return 0.89
     *
     * @param x Float clamped from 0 to 1
     * @return Smoothed float value
     */
    public static float smooth(float x) {
        if (x <= 0) {
            return 0;
        }
        if (x >= 1) {
            return 1;
        }
        return 0.5f - (float) (FastTrig.cos(x * MathUtils.FPI)) / 2;
    }

    /**
     * USE arbitrarySmooth() INSTEAD
     *
     * @deprecated use MagicAnim.arbitrarySmooth() instead
     */
    @Deprecated
    public static float AS(float x, float min, float max) {
        float value = Math.min(max, Math.max(min, x));
        value = (value - min) / (max - min);
        value = (0.5f - ((float) (FastTrig.cos(value * MathUtils.FPI) / 2)));
        value *= (max - min) + min;
        return value;
    }

    /**
     * Translates a value in a (min,max) range to a value in the same range with smooth ease in and ease out.
     *
     * @param x   Float clamped from min to max
     * @param min minimal range value
     * @param max maximal range value
     * @return smoothed float value in that range
     */
    public static float arbitrarySmooth(float x, float min, float max) {
        if (x <= min) {
            return min;
        }
        if (x > max) {
            return max;
        }
//        float value=Math.min(max, Math.max(min, x));
//        float value = (x-min)/(max-min);
//        value = (0.5f - ((float)(FastTrig.cos(value*MathUtils.FPI) /2 )));
//        value *= (max-min) + min;
//        return value;

        float magicNumber = -(min - max) / 2;
        return (float) (FastTrig.cos((x - min) * (1 / (max - min)) * MathUtils.FPI)) * magicNumber + magicNumber + min;
    }

    /**
     * USE offsetToRange() INSTEAD
     *
     * @deprecated use MagicAnim.offsetToRange() instead
     */
    @Deprecated
    public static float range(float x, float min, float max) {
        return Math.min(1, Math.max(0, x)) * (max - min) + min;
    }

    /**
     * Translates a value from a (0,1) range to a value in a (min,max) range.
     *
     * @param x   Float clamped from 0 to 1
     * @param min new range minimal value
     * @param max new range maximal value
     * @return float value in the new range
     */
    public static float offsetToRange(float x, float min, float max) {
        if (x <= 0) {
            return min;
        }
        if (x > 1) {
            return max;
        }
        return x * (max - min) + min;
    }

    /**
     * USE normalizeRange() INSTEAD
     *
     * @deprecated use normalizeRange() instead
     */
    @Deprecated
    public static float offset(float x, float start, float end) {
        return Math.min(1, Math.max(0, (x - start) * (1 / (end - start))));
    }

    /**
     * Translates a value in a (start,end) range into a value in a (0,1) range.
     *
     * @param x     Float clamped from 0 to 1
     * @param start new range minimal value
     * @param end   new range maximal value
     * @return float value in the new range
     */
    public static float normalizeRange(float x, float start, float end) {
        if (x <= start) {
            return 0;
        }
        if (x > end) {
            return 1;
        }
        return (x - start) * (1 / (end - start));
    }

    /**
     * USE smoothNormalizeRange INSTEAD
     *
     * @deprecated use smoothNormalizeRange()
     */
    @Deprecated
    public static float SO(float x, float start, float end) {
        return 0.5f - (float) (FastTrig.cos(Math.min(1, Math.max(0, (x - start) * (1 / (end - start)))) * MathUtils.FPI) / 2);
    }

    /**
     * Translates a value from a (start,end) range into a value in a (0,1) range with smooth ease in and ease out.
     * Allows to segment the value of a timer or an effect level into different shorter sliders
     *
     * @param x     Float clamped from 0 to 1
     * @param start new range minimal value
     * @param end   new range maximal value
     * @return smoothed float value in the new range
     */
    public static float smoothNormalizeRange(float x, float start, float end) {
        if (x <= start) {
            return 0;
        }
        if (x > end) {
            return 1;
        }
        return 0.5f - (float) (FastTrig.cos((x - start) * (1 / (end - start)) * MathUtils.FPI) / 2);
    }

    /**
     * Translates a value from a (fromMin,fromMax) range into a value in a (toMin,toMax) range with smooth ease in and ease out.
     * Allows to segment the value of a timer or an effect level into different shorter sliders
     *
     * @param x
     * @param fromMin
     * @param fromMax
     * @param toMin
     * @param toMax
     * @return smoothed float value clamped at the from range, into the new range
     */
    public static float smoothToRange(float x, float fromMin, float fromMax, float toMin, float toMax) {
        if (x <= fromMin) {
            return toMin;
        }
        if (x >= fromMax) {
            return toMax;
        }
        float magicNumber = -(toMax - toMin) / 2;
        return (float) (FastTrig.cos((x - fromMin) * (1 / (fromMax - fromMin)) * MathUtils.FPI)) * magicNumber + magicNumber + toMin;
    }

    /**
     * USE smoothReturnNormalizeRange() INSTEAD
     *
     * @deprecated use smoothReturnNormalizeRange()
     */
    @Deprecated
    public static float RSO(float x, float start, float end) {
        return 0.5f - (float) (FastTrig.cos(Math.min(1, Math.max(0, (x - start) * (1 / (end - start)))) * MathUtils.FPI * 2) / 2);
    }

    /**
     * Translates a value in a (start,end) range into a "back-and-forth" value in a (0,1) range with smooth ease in and ease out.
     *
     * @param x     Float clamped from start to end
     * @param start range minimal value
     * @param end   range maximal value
     * @return smooth "back-and-forth" float value equals to 0 at start and end, and 1 at the mid-point between them
     */
    public static float smoothReturnNormalizeRange(float x, float start, float end) {
        if (x <= start || x >= end) {
            return 0;
        }
        return 0.5f - (float) (FastTrig.cos((x - start) * (1 / (end - start)) * MathUtils.FPI * 2) / 2);
    }

    /**
     * Translates a value in a (fromMin,fromMax) range into a "back-and-forth" value in a (toMin,toMax) range with smooth ease in and ease out.
     *
     * @param x
     * @param fromMin
     * @param fromMax
     * @param toMin
     * @param toMax
     * @return smooth "back-and-forth" float value equals to toMin at start and toMin, and toMax at the mid-point between the (fromMin, fromMax) range
     */
    public static float smoothReturnToRange(float x, float fromMin, float fromMax, float toMin, float toMax) {
        if (x <= fromMin || x >= fromMax) {
            return toMin;
        }

        float magicNumber = -(toMax - toMin) / 2;
        return (float) (FastTrig.cos((x - fromMin) * (1 / (fromMax - fromMin)) * MathUtils.FPI * 2)) * magicNumber + magicNumber + toMin;
    }


    /**
     * Cycle within range
     * Restricts a value to a cycling range, produces a seesaw.
     *
     * @param x   Float
     * @param min cycle minimal value
     * @param max cycle maximal value
     * @return "seesaw" value that cycles within the (min,max) range
     */
    public static float cycle(float x, float min, float max) {
        float range = max - min;
        float i = (float) Math.floor((x - min) / range);
        return min + x - range * i;
    }
}