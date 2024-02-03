package org.magiclib.util;

import com.fs.starfarer.api.Global;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.*;

import static org.magiclib.util.MagicVariables.MAGICLIB_ID;

/**
 * @author Tartiflette
 */
public class MagicSettings {

    private static final Logger LOG = Global.getLogger(MagicSettings.class);
    private static final String PATH = "data/config/modSettings.json";
    public static JSONObject modSettings;
    private static boolean devmode = false;

    /**
     * Gets a single boolean value from data/config/modSettings.json. WARNING, while this method should be fairly fast, I would advise against trying to parse a JSON file every frame.
     *
     * @param modId section of modSettings to look into.
     * @param id    name of the variable to look for
     * @return value from modSettings.json. Default to FALSE in case of failure.
     */
    public static boolean getBoolean(String modId, String id) {
        if (modSettings == null) {
            loadModSettings();
        }
        boolean value = false;
        //try to get the requested mod settings
        if (modSettings.has(modId)) {
            try {
                JSONObject reqSettings = modSettings.getJSONObject(modId);
                //try to get the requested value
                if (reqSettings.has(id)) {
                    value = reqSettings.getBoolean(id);
                    //log value if devMode is active
                    if (devmode) {
                        LOG.info(modId + " , " + id + " : " + value);
                    }
                } else {
                    LOG.warn("unable to find " + id + " within " + modId + " in modSettings.json");
                }
            } catch (JSONException ex) {
                LOG.warn("unable to read content of " + modId + " in modSettings.json", ex);
            }
        } else {
            LOG.warn("unable to find " + modId + " in modSettings.json");
        }
        return value;
    }

    /**
     * Gets a single string value from data/config/modSettings.json. WARNING, while this method should be fairly fast, I would advise against trying to parse a JSON file every frame.
     *
     * @param modId section of modSettings to look into.
     * @param id    name of the variable to look for
     * @return value from modSettings.json. Default to an empty String in case of failure.
     */
    public static String getString(String modId, String id) {
        if (modSettings == null) {
            loadModSettings();
        }
        String value = "";
        //try to get the requested mod settings
        if (modSettings.has(modId)) {
            try {
                JSONObject reqSettings = modSettings.getJSONObject(modId);
                //try to get the requested value
                if (reqSettings.has(id)) {
                    value = reqSettings.getString(id);
                    //log value if devMode is active
                    if (devmode) {
                        LOG.info(modId + " , " + id + " : " + value);
                    }
                } else {
                    LOG.warn("unable to find " + id + " within " + modId + " in modSettings.json");
                }
            } catch (JSONException ex) {
                LOG.warn("unable to read content of " + modId + " in modSettings.json", ex);
            }
        } else {
            LOG.warn("unable to find " + modId + " in modSettings.json");
        }
        return value;
    }

    /**
     * Gets a single float value from data/config/modSettings.json. WARNING, while this method should be fairly fast, I would advise against trying to parse a JSON file every frame.
     *
     * @param modId section of modSettings to look into.
     * @param id    name of the variable to look for
     * @return value from modSettings.json. Default to 0 in case of failure.
     */
    public static float getFloat(String modId, String id) {
        if (modSettings == null) {
            loadModSettings();
        }
        float value = 0;
        //try to get the requested mod settings
        if (modSettings.has(modId)) {
            try {
                JSONObject reqSettings = modSettings.getJSONObject(modId);
                //try to get the requested value
                if (reqSettings.has(id)) {
                    value = (float) reqSettings.getDouble(id);
                    //log value if devMode is active
                    if (devmode) {
                        LOG.info(modId + " , " + id + " : " + value);
                    }
                } else {
                    LOG.warn("unable to find " + id + " within " + modId + " in modSettings.json");
                }
            } catch (JSONException ex) {
                LOG.warn("unable to read content of " + modId + " in modSettings.json", ex);
            }
        } else {
            LOG.warn("unable to find " + modId + " in modSettings.json");
        }
        return value;
    }

    /**
     * Gets a single integer value from data/config/modSettings.json. WARNING, while this method should be fairly fast, I would advise against trying to parse a JSON file every frame.
     *
     * @param modId section of modSettings to look into.
     * @param id    name of the variable to look for
     * @return value from modSettings.json. Default to 0 in case of failure.
     */
    public static Integer getInteger(String modId, String id) {
        if (modSettings == null) {
            loadModSettings();
        }
        int value = 0;
        //try to get the requested mod settings
        if (modSettings.has(modId)) {
            try {
                JSONObject reqSettings = modSettings.getJSONObject(modId);
                //try to get the requested value
                if (reqSettings.has(id)) {
                    value = reqSettings.getInt(id);
                    //log value if devMode is active
                    if (devmode) {
                        LOG.info(modId + " , " + id + " : " + value);
                    }
                } else {
                    LOG.warn("unable to find " + id + " within " + modId + " in modSettings.json");
                }
            } catch (JSONException ex) {
                LOG.warn("unable to read content of " + modId + " in modSettings.json", ex);
            }
        } else {
            LOG.warn("unable to find " + modId + " in modSettings.json");
        }
        return value;
    }

    /**
     * Gets a single Color without alpha from data/config/modSettings.json. WARNING, while this method should be fairly fast, I would advise against trying to parse a JSON file every frame.
     *
     * @param modId section of modSettings to look into.
     * @param id    name of the variable to look for
     * @return Color from modSettings.json. Default to red in case of failure.
     */
    public static Color getColorRGB(String modId, String id) {
        if (modSettings == null) {
            loadModSettings();
        }
        Color value = Color.RED;
        //try to get the requested mod settings
        if (modSettings.has(modId)) {
            try {
                JSONObject reqSettings = modSettings.getJSONObject(modId);
                //try to get the requested value
                if (reqSettings.has(id)) {

                    value = toColor3(reqSettings.getString(id));
                    //log value if devMode is active
                    if (devmode) {
                        LOG.info(modId + " , " + id + " : " + value);
                    }
                } else {
                    LOG.warn("unable to find " + id + " within " + modId + " in modSettings.json");
                }
            } catch (JSONException ex) {
                LOG.warn("unable to read content of " + modId + " in modSettings.json", ex);
            }
        } else {
            LOG.warn("unable to find " + modId + " in modSettings.json");
        }
        return value;
    }

    /**
     * Gets a single Color with alpha from data/config/modSettings.json. WARNING, while this method should be fairly fast, I would advise against trying to parse a JSON file every frame.
     *
     * @param modId section of modSettings to look into.
     * @param id    name of the variable to look for
     * @return Color from modSettings.json. Default to red in case of failure.
     */
    public static Color getColorRGBA(String modId, String id) {
        if (modSettings == null) {
            loadModSettings();
        }
        Color value = Color.RED;
        //try to get the requested mod settings
        if (modSettings.has(modId)) {
            try {
                JSONObject reqSettings = modSettings.getJSONObject(modId);
                //try to get the requested value
                if (reqSettings.has(id)) {

                    value = toColor4(reqSettings.getString(id));
                    //log value if devMode is active
                    if (devmode) {
                        LOG.info(modId + " , " + id + " : " + value);
                    }
                } else {
                    LOG.warn("unable to find " + id + " within " + modId + " in modSettings.json");
                }
            } catch (JSONException ex) {
                LOG.warn("unable to read content of " + modId + " in modSettings.json", ex);
            }
        } else {
            LOG.warn("unable to find " + modId + " in modSettings.json");
        }
        return value;
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
        if (modSettings == null) {
            loadModSettings();
        }
        List<String> value = new ArrayList<>();
        //try to get the requested mod settings
        if (modSettings.has(modId)) {
            try {
                JSONObject reqSettings = modSettings.getJSONObject(modId);
                //try to get the requested value
                if (reqSettings.has(id)) {
                    JSONArray list = reqSettings.getJSONArray(id);
                    if (list.length() > 0) {
                        for (int j = 0; j < list.length(); j++) {
                            value.add(list.getString(j));
                            //log value if devMode is active
                            if (devmode) {
                                LOG.info(modId + " , " + id + " : found " + list.getString(j));
                            }
                        }
                    }
                } else {
                    LOG.warn("unable to find " + id + " within " + modId + " in modSettings.json");
                }
            } catch (JSONException ex) {
                LOG.warn("unable to read content of " + modId + " in modSettings.json", ex);
            }
        } else {
            LOG.warn("unable to find " + modId + " in modSettings.json");
        }
        return value;
    }

    /**
     * Gets a Map<String,Float> from data/config/modSettings.json. WARNING, while this method should be fairly fast, I would advise against trying to parse a JSON file every frame.
     *
     * @param modId section of modSettings to look into.
     * @param id    name of the variable to look for
     * @return Map<String, Float> from modSettings.json. Default to an empty map in case of failure.
     */
    public static Map<String, Float> getFloatMap(String modId, String id) {
        if (modSettings == null) {
            loadModSettings();
        }
        Map<String, Float> value = new HashMap<>();
        //try to get the requested mod settings
        if (modSettings.has(modId)) {
            try {
                JSONObject reqSettings = modSettings.getJSONObject(modId);
                //try to get the requested value
                if (reqSettings.has(id)) {
                    JSONObject list = reqSettings.getJSONObject(id);
                    if (list.length() > 0) {
                        for (Iterator<?> iter = list.keys(); iter.hasNext(); ) {
                            String key = (String) iter.next();
                            float data = (float) list.getDouble(key);
                            value.put(key, data);
                            //log value if devMode is active
                            if (devmode) {
                                LOG.info(modId + " , " + id + " : adding " + key + " with a value of " + data);
                            }
                        }
                    }
                } else {
                    LOG.warn("unable to find " + id + " within " + modId + " in modSettings.json");
                }
            } catch (JSONException ex) {
                LOG.warn("unable to read content of " + modId + " in modSettings.json", ex);
            }
        } else {
            LOG.warn("unable to find " + modId + " in modSettings.json");
        }
        return value;
    }

    /**
     * Gets a Map<String,String> from data/config/modSettings.json. WARNING, while this method should be fairly fast, I would advise against trying to parse a JSON file every frame.
     *
     * @param modId section of modSettings to look into.
     * @param id    name of the variable to look for
     * @return Map<String, String> from modSettings.json. Default to an empty map in case of failure.
     */
    public static Map<String, String> getStringMap(String modId, String id) {
        if (modSettings == null) {
            loadModSettings();
        }
        Map<String, String> value = new HashMap<>();
        //try to get the requested mod settings
        if (modSettings.has(modId)) {
            try {
                JSONObject reqSettings = modSettings.getJSONObject(modId);
                //try to get the requested value
                if (reqSettings.has(id)) {
                    JSONObject list = reqSettings.getJSONObject(id);
                    if (list.length() > 0) {
                        for (Iterator<?> iter = list.keys(); iter.hasNext(); ) {
                            String key = (String) iter.next();
                            String data = list.getString(key);
                            value.put(key, data);
                            //log value if devMode is active
                            if (devmode) {
                                LOG.info(modId + " , " + id + " : adding " + key + " with a value of " + data);
                            }
                        }
                    }
                } else {
                    LOG.warn("unable to find " + id + " within " + modId + " in modSettings.json");
                }
            } catch (JSONException ex) {
                LOG.warn("unable to read content of " + modId + " in modSettings.json", ex);
            }
        } else {
            LOG.warn("unable to find " + modId + " in modSettings.json");
        }
        return value;
    }

    /**
     * Gets a Map<String,Color(RGBA)> from data/config/modSettings.json. WARNING, while this method should be fairly fast, I would advise against trying to parse a JSON file every frame.
     *
     * @param modId section of modSettings to look into.
     * @param id    name of the variable to look for
     * @return Map<String, Color ( RGBA )> from modSettings.json. Default to an empty map in case of failure.
     */
    public static Map<String, Color> getColorMap(String modId, String id) {
        if (modSettings == null) {
            loadModSettings();
        }
        Map<String, Color> value = new HashMap<>();
        //try to get the requested mod settings
        if (modSettings.has(modId)) {
            try {
                JSONObject reqSettings = modSettings.getJSONObject(modId);
                //try to get the requested value
                if (reqSettings.has(id)) {
                    JSONObject list = reqSettings.getJSONObject(id);
                    if (list.length() > 0) {
                        for (Iterator<?> iter = list.keys(); iter.hasNext(); ) {
                            String key = (String) iter.next();
                            Color data = toColor4(list.getString(key));
                            value.put(key, data);
                            //log value if devMode is active
                            if (devmode) {
                                LOG.info(modId + " , " + id + " : adding " + key + " with a value of " + data);
                            }
                        }
                    }
                } else {
                    LOG.warn("unable to find " + id + " within " + modId + " in modSettings.json");
                }
            } catch (JSONException ex) {
                LOG.warn("unable to read content of " + modId + " in modSettings.json", ex);
            }
        } else {
            LOG.warn("unable to find " + modId + " in modSettings.json");
        }
        return value;
    }


    // OTHER UTILS

    public static void loadModSettings() {
        try {
            modSettings = Global.getSettings().getMergedJSONForMod(PATH, MAGICLIB_ID);
        } catch (IOException | JSONException ex) {
            LOG.fatal("unable to read modSettings.json", ex);
        }
        devmode = Global.getSettings().isDevMode();
    }

    public static Color toColor3(String in) {
        final String inPredicate = in.substring(1, in.length() - 1);
        final String[] array = inPredicate.split(",");
        return new Color(clamp255(Integer.parseInt(array[0])), clamp255(Integer.parseInt(array[1])), clamp255(Integer.parseInt(array[2])), 255);
    }

    public static Color toColor4(String in) {
        final String inPredicate = in.substring(1, in.length() - 1);
        final String[] array = inPredicate.split(",");
        return new Color(clamp255(Integer.parseInt(array[0])), clamp255(Integer.parseInt(array[1])), clamp255(Integer.parseInt(array[2])), clamp255(Integer.parseInt(array[3])));
    }

    public static int clamp255(int x) {
        return Math.max(0, Math.min(255, x));
    }
}
