/*
By Tartiflette
 */
package data.scripts.util;

import com.fs.starfarer.api.Global;

import java.util.regex.Pattern;

public class MagicTxt {   
    private static final String ML="magicLib";    
    
    public static String getString(String id){
        return Global.getSettings().getString(ML, id);
    }   
    
    public static String nullStringIfEmpty(String input) {
        return input != null && !input.isEmpty() ? input : null;
    }

    /**
     * Replaces all instances of the given regex with the string returned from stringCreator.
     * The difference from normal String.replaceAll is that stringCreator is only run if a match is found.
     */
    public static String replaceAllIfPresent(String stringToReplace, String regex, StringCreator stringCreator) {
        if (stringToReplace.contains(regex)) {
            return stringToReplace.replaceAll(Pattern.quote(regex), stringCreator.create());
        } else {
            return stringToReplace;
        }
    }
}