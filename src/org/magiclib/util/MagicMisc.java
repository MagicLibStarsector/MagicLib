package org.magiclib.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.kotlin.MagicKotlinExtKt;
import org.magiclib.kotlin.OtherExtensionsKt;

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

    /**
     * Returns the angle (in degrees) between the <i>originShip</i>'s Forward Vector and <i>otherShip</i>>.
     * <p>
     * Contributed by rksharkz.
     *
     * @return the difference in degrees
     * @see MagicMisc#getForwardVector(ShipAPI)
     * @since 1.4.6
     */
    public static float getAngleToAnotherShip(ShipAPI originShip, ShipAPI otherShip) {
        return OtherExtensionsKt.getAngleToAnotherShip(originShip, otherShip);
    }

    /**
     * Returns the absolute angle (in degrees) between this ship and *otherShip*.
     * <p>
     * Contributed by rksharkz.
     *
     * @return the difference in degrees, as absolute value
     * @see MagicMisc#getAngleToAnotherShip(ShipAPI, ShipAPI)
     * @since 1.4.6
     */
    public static float getAbsoluteAngleToAnotherShip(ShipAPI originShip, ShipAPI otherShip) {
        return OtherExtensionsKt.getAbsoluteAngleToAnotherShip(originShip, otherShip);
    }

    /**
     * Returns the {@link Vector2f} of where the ship is looking (facing).
     * <p>
     * Contributed by rksharkz.
     *
     * @return the ship's forward vector, similar to {@link com.fs.starfarer.api.util.Misc#getUnitVectorAtDegreeAngle} used with the ship's {@link ShipAPI#getFacing}
     * @since 1.4.6
     */
    public static Vector2f getForwardVector(ShipAPI ship) {
        return OtherExtensionsKt.getForwardVector(ship);
    }
}
