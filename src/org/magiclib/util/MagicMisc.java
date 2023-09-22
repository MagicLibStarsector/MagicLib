package org.magiclib.util;

import com.fs.starfarer.api.Global;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.magiclib.kotlin.MagicKotlinExtKt;

import java.util.*;

/**
 * Miscellaneous utility methods.
 */
public class MagicMisc {
    public static float getElapsedDaysSinceGameStart() {
        return MagicKotlinExtKt.elapsedDaysSinceGameStart(Global.getSector().getClock());
    }

    /**
     * Returns the first {@code count} characters of the given string.
     */
    public static String takeFirst(String str, int count) {
        return count > str.length()
                ? str
                : str.substring(0, count);
    }

    /**
     * Converts a {@link JSONObject} to a {@link Map}.
     *
     * @since 1.3.0
     */
    public static Map<String, Object> toMap(JSONObject jsonObj) throws JSONException {
        Map<String, Object> map = new HashMap<>();
        Iterator<String> keys = jsonObj.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = jsonObj.get(key);
            if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            map.put(key, value);
        }
        return map;
    }

    /**
     * Converts a {@link JSONArray} to a {@link List}.
     *
     * @since 1.3.0
     */
    public static List<Object> toList(JSONArray array) throws JSONException {
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            list.add(value);
        }
        return list;
    }
}
