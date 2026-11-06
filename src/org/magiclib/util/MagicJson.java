package org.magiclib.util;

import java.util.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.magiclib.kotlin.OtherExtensionsKt;

/**
 * A somewhat more friendly type of dictionary, for loading JSON files.
 * <p>
 * Loads a JSONObject into a hashmap with member functions that allow you to navigate the tree
 * without constantly having to cast.
 * <pre>
 *     myMap = MagicJson.fromJSON(Global.getSettings().loadJSON("data/my_json_file.json", true))
 *     myMap.getMap("key1").getIntList("my_ints").get(2);
 * </pre>
 *
 * @author Toaster
 */
@SuppressWarnings("unused")
public class MagicJson extends HashMap<String, Object> {
    /**
     * Loads JSON into a MagicJson, recursively going through the nodes.
     * <p>
     * Based on: <a href="https://stackoverflow.com/questions/21720759/convert-a-json-string-to-a-hashmap">...</a>
     *
     * @param json The JSON file.
     * @return The same thing, but turned into a map.
     * @throws JSONException If the JSON object isn't valid.
     */
    public static MagicJson fromJSON(JSONObject json) throws JSONException {
        MagicJson retMap = new MagicJson();

        if (json != null && json != JSONObject.NULL) retMap = jsonObjectToMap(json);
        return retMap;
    }

    /**
     * Parses the nodes, turning the objects into sensible values.
     *
     * @param object The JSON object that is the current node.
     * @return That same thing, but turned into a MagicJson.
     * @throws JSONException If the JSON object isn't valid.
     */
    @SuppressWarnings("unchecked")
    protected static MagicJson jsonObjectToMap(JSONObject object) throws JSONException {
        MagicJson map = new MagicJson();

        Iterator<String> keysItr = object.keys();
        while (keysItr.hasNext()) {
            String key = keysItr.next();
            Object value = object.get(key);

            if (value == JSONObject.NULL) {
                value = null;
            } else if (value instanceof JSONArray) {
                value = jsonArrayToList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = jsonObjectToMap((JSONObject) value);
            }
            map.put(key, value);
        }
        return map;
    }

    /**
     * Parses a node's value, turning it into a list.
     *
     * @param array The JSON array that's the current node's values.
     * @return That same thing, but turned into a regular list.
     * @throws JSONException If the JSON object isn't valid.
     */
    protected static List<Object> jsonArrayToList(JSONArray array) throws JSONException {
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if (value == JSONObject.NULL) {
                value = null;
            } else if (value instanceof JSONArray) {
                value = jsonArrayToList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = jsonObjectToMap((JSONObject) value);
            }
            list.add(value);
        }
        return list;
    }
    // ----------------------------------------------------------------

    public boolean getBool(String key) {
        if (this.get(key) instanceof Boolean value) return value;
        Boolean parsed = OtherExtensionsKt.toBooleanOrNull(this.get(key).toString());
        if (parsed == null)
            throw new IllegalArgumentException("Value for key '" + key + "' cannot be parsed as a boolean: " + this.get(key));
        return parsed;
    }

    public boolean getBoolOrDefault(String key, boolean defaultValue) {
        if (this.get(key) == null) return defaultValue;
        return getBool(key);
    }

    public int getInt(String key) {
        if (this.get(key) instanceof Number value) return value.intValue();
        return Integer.parseInt(this.get(key).toString());
    }

    public int getIntOrDefault(String key, int defaultValue) {
        if (this.get(key) == null) return defaultValue;
        return getInt(key);
    }

    @SuppressWarnings("unchecked")
    public List<Integer> getIntList(String key) {
        List<Object> listObj = (List<Object>) this.get(key);
        List<Integer> listOut = new ArrayList<>();
        for (Object o : listObj) {
            if (o instanceof Number value) {
                listOut.add(value.intValue());
            } else {
                listOut.add(Integer.parseInt(o.toString()));
            }
        }
        return listOut;
    }

    public List<Integer> getIntListOrDefault(String key, List<Integer> defaultValue) {
        if (this.get(key) == null) return defaultValue;
        return this.getIntList(key);
    }

    public float getFloat(String key) {
        if (this.get(key) instanceof Number value) return value.floatValue();
        return Float.parseFloat(this.get(key).toString());
    }

    public float getFloatOrDefault(String key, float defaultValue) {
        if (this.get(key) == null) return defaultValue;
        return getFloat(key);
    }

    @SuppressWarnings("unchecked")
    public List<Float> getFloatList(String key) {
        List<Object> listObj = (List<Object>) this.get(key);
        List<Float> listOut = new ArrayList<>();
        for (Object o : listObj) {
            if (o instanceof Number value) {
                listOut.add(value.floatValue());
            } else {
                listOut.add(Float.parseFloat(o.toString()));
            }
        }
        return listOut;
    }

    public List<Float> getFloatListOrDefault(String key, List<Float> defaultValue) {
        if (this.get(key) == null) return defaultValue;
        return this.getFloatList(key);
    }

    public String getString(String key) {
        return this.get(key).toString();
    }

    public String getStringOrDefault(String key, String defaultValue) {
        if (this.get(key) == null) return defaultValue;
        return this.get(key).toString();
    }

    @SuppressWarnings("unchecked")
    public List<String> getStringList(String key) {
        List<Object> listObj = (List<Object>) this.get(key);
        List<String> listOut = new ArrayList<>();
        for (Object o : listObj) {
            listOut.add(o.toString());
        }
        return listOut;
    }

    public List<String> getStringListOrDefault(String key, List<String> defaultValue) {
        if (this.get(key) == null) return defaultValue;
        return this.getStringList(key);
    }

    public HashSet<String> getStringSet(String key) {
        List<String> listString = getStringList(key);
        return new HashSet<>(listString);
    }

    public Set<String> getStringSetOrDefault(String key, Set<String> defaultValue) {
        if (this.get(key) == null) return defaultValue;
        return this.getStringSet(key);
    }

    public MagicJson getMap(String key) {
        return (MagicJson) this.get(key);
    }

    public MagicJson getMapOrDefault(String key, MagicJson defaultValue) {
        if (this.get(key) == null) return defaultValue;
        return this.getMap(key);
    }

    @SuppressWarnings("unchecked")
    public List<MagicJson> getMapList(String key) {
        List<Object> listObj = (List<Object>) this.get(key);
        List<MagicJson> listOut = new ArrayList<>();
        for (Object o : listObj) {
            listOut.add((MagicJson) o);
        }
        return listOut;
    }

    public List<MagicJson> getMapListOrDefault(String key, List<MagicJson> defaultValue) {
        if (this.get(key) == null) return defaultValue;
        return this.getMapList(key);
    }
}