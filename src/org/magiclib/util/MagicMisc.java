package org.magiclib.util;

import com.fs.starfarer.api.Global;
import org.magiclib.kotlin.MagicKotlinExtKt;

/**
 * Miscellaneous utility methods.
 */
public class MagicMisc {
    public static float getElapsedDaysSinceGameStart() {
        return MagicKotlinExtKt.elapsedDaysSinceGameStart(Global.getSector().getClock());
    }

    public static String takeFirst(String str, int count) {
        return count > str.length()
                ? str
                : str.substring(0, count);
    }
}
