/*
By Tartiflette
 */
package data.scripts.util;

import com.fs.starfarer.api.Global;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

public class MagicTxt {
    private static final String ML="magicLib";

    public static String getString(String id){
        return Global.getSettings().getString(ML, id);
    }

    public static String nullStringIfEmpty(@Nullable String input) {
        return input != null && !input.isEmpty() ? input : null;
    }

    /**
     * Replaces all instances of the given regex with the string returned from stringCreator.
     * The difference from normal String.replaceAll is that stringCreator is only run if a match is found.
     */
    public static String replaceAllIfPresent(String stringToReplace, String regex, StringCreator stringCreator) {
        if (stringToReplace.contains(regex)) {
            try {
                String replacement = stringCreator.create();
                return stringToReplace.replaceAll(Pattern.quote(regex), replacement);
            } catch (Exception e) {
                Global.getLogger(MagicTxt.class).error("Error thrown while replacing " + stringToReplace, e);
                return stringToReplace.replaceAll(Pattern.quote(regex), "null");
            }
        } else {
            return stringToReplace;
        }
    }
}