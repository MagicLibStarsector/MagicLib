/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data.scripts.util;

import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * @author Tartiflette
 */
@Deprecated
public class MagicSettings {
    /**
     * Gets a single boolean value from data/config/modSettings.json. WARNING, while this method should be fairly fast, I would advise against trying to parse a JSON file every frame.
     *
     * @param modId section of modSettings to look into.
     * @param id    name of the variable to look for
     * @return value from modSettings.json. Default to FALSE in case of failure.
     */
    public static boolean getBoolean(String modId, String id) {
        return org.magiclib.util.MagicSettings.getBoolean(modId, id);
    }

    /**
     * Gets a single string value from data/config/modSettings.json. WARNING, while this method should be fairly fast, I would advise against trying to parse a JSON file every frame.
     *
     * @param modId section of modSettings to look into.
     * @param id    name of the variable to look for
     * @return value from modSettings.json. Default to an empty String in case of failure.
     */
    public static String getString(String modId, String id) {
        return org.magiclib.util.MagicSettings.getString(modId, id);
    }

    /**
     * Gets a single float value from data/config/modSettings.json. WARNING, while this method should be fairly fast, I would advise against trying to parse a JSON file every frame.
     *
     * @param modId section of modSettings to look into.
     * @param id    name of the variable to look for
     * @return value from modSettings.json. Default to 0 in case of failure.
     */
    public static float getFloat(String modId, String id) {
        return org.magiclib.util.MagicSettings.getFloat(modId, id);
    }

    /**
     * Gets a single integer value from data/config/modSettings.json. WARNING, while this method should be fairly fast, I would advise against trying to parse a JSON file every frame.
     *
     * @param modId section of modSettings to look into.
     * @param id    name of the variable to look for
     * @return value from modSettings.json. Default to 0 in case of failure.
     */
    public static Integer getInteger(String modId, String id) {
        return org.magiclib.util.MagicSettings.getInteger(modId, id);
    }

    /**
     * Gets a single Color without alpha from data/config/modSettings.json. WARNING, while this method should be fairly fast, I would advise against trying to parse a JSON file every frame.
     *
     * @param modId section of modSettings to look into.
     * @param id    name of the variable to look for
     * @return Color from modSettings.json. Default to red in case of failure.
     */
    public static Color getColorRGB(String modId, String id) {
        return org.magiclib.util.MagicSettings.getColorRGB(modId, id);
    }

    /**
     * Gets a single Color with alpha from data/config/modSettings.json. WARNING, while this method should be fairly fast, I would advise against trying to parse a JSON file every frame.
     *
     * @param modId section of modSettings to look into.
     * @param id    name of the variable to look for
     * @return Color from modSettings.json. Default to red in case of failure.
     */
    public static Color getColorRGBA(String modId, String id) {
        return org.magiclib.util.MagicSettings.getColorRGBA(modId, id);
    }

    /**
     * Gets a list of Strings from data/config/modSettings.json. WARNING, while this method should be fairly fast, I would advise against trying to parse a JSON file every frame.
     *
     * @param modId section of modSettings to look into.
     * @param id    name of the variable to look for
     * @return List<String> from modSettings.json. Default to an empty list in case of failure.
     */
    @NotNull
    public static List<String> getList(String modId, String id) {
        return org.magiclib.util.MagicSettings.getList(modId, id);
    }

    /**
     * Gets a Map<String,Float> from data/config/modSettings.json. WARNING, while this method should be fairly fast, I would advise against trying to parse a JSON file every frame.
     *
     * @param modId section of modSettings to look into.
     * @param id    name of the variable to look for
     * @return Map<String, Float> from modSettings.json. Default to an empty map in case of failure.
     */
    public static Map<String, Float> getFloatMap(String modId, String id) {
        return org.magiclib.util.MagicSettings.getFloatMap(modId, id);
    }

    /**
     * Gets a Map<String,String> from data/config/modSettings.json. WARNING, while this method should be fairly fast, I would advise against trying to parse a JSON file every frame.
     *
     * @param modId section of modSettings to look into.
     * @param id    name of the variable to look for
     * @return Map<String, String> from modSettings.json. Default to an empty map in case of failure.
     */
    public static Map<String, String> getStringMap(String modId, String id) {
        return org.magiclib.util.MagicSettings.getStringMap(modId, id);
    }

    /**
     * Gets a Map<String,Color(RGBA)> from data/config/modSettings.json. WARNING, while this method should be fairly fast, I would advise against trying to parse a JSON file every frame.
     *
     * @param modId section of modSettings to look into.
     * @param id    name of the variable to look for
     * @return Map<String, Color ( RGBA )> from modSettings.json. Default to an empty map in case of failure.
     */
    public static Map<String, Color> getColorMap(String modId, String id) {
        return org.magiclib.util.MagicSettings.getColorMap(modId, id);
    }


    // OTHER UTILS

    public static void loadModSettings() {
        org.magiclib.util.MagicSettings.loadModSettings();
    }

    public static Color toColor3(String in) {
        return org.magiclib.util.MagicSettings.toColor3(in);
    }

    public static Color toColor4(String in) {
        return org.magiclib.util.MagicSettings.toColor4(in);
    }

    public static int clamp255(int x) {
        return org.magiclib.util.MagicSettings.clamp255(x);
    }
}
