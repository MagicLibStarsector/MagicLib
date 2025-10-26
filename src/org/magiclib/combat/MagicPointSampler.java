package org.magiclib.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lwjgl.util.vector.Vector2f;

import java.util.LinkedHashMap;

/**
 * Class that gets random points on a ship using its bounds, in a (relatively) efficient way.
 * <p>
 * Exists as some ships are very long, so just randomly sampling on a circle and accept/reject is inefficient.
 * It creates a sampler for a ship when called, and stores it in a variable on the combat engine.
 * This means you don't have have every different script that wants to use this feature creating its own copy.
 * <p>
 * Example usage:
 * <pre>
 *     Vector2f edgePoint = MagicPointSampler.getInternalPoint(ship);
 * </pre>
 *
 * @author Toaster
 */
@SuppressWarnings("unused")
public class MagicPointSampler {
    public static final String KEY_SHIP_MAP = "MagicPointSampler_shipMap";

    /**
     * Gets the ship map from global storage.
     *
     * @return The map of ships and their samplers.
     */
    @SuppressWarnings("unchecked")
    protected static LinkedHashMap<ShipAPI, ShipPointSampler> getShipSamplerMap() {
        LinkedHashMap<ShipAPI, ShipPointSampler> map =
                (LinkedHashMap<ShipAPI, ShipPointSampler>) Global.getCombatEngine().getCustomData().get(KEY_SHIP_MAP);
        if (map == null) {
            map = new LinkedHashMap<>();
            Global.getCombatEngine().getCustomData().put(KEY_SHIP_MAP, map);
        }
        return map;
    }

    /**
     * Gets the sampler for a ship, creating it if it doesn't exist.
     *
     * @param ship The ship to get a sampler for.
     * @return The ship's sampler.
     */
    protected static ShipPointSampler getShipSampler(ShipAPI ship) {
        ShipPointSampler sampler = getShipSamplerMap().getOrDefault(ship, null);
        if (sampler == null) {
            sampler = new ShipPointSampler(ship);
            getShipSamplerMap().put(ship, sampler);
        }
        return sampler;
    }

    /**
     * Gets one of the vertexes of the ship's bounding polygon at random.
     *
     * @param ship The ship to check.
     * @return A vertex on the bounding polygon.
     */
    public static Vector2f getBoundsPoint(ShipAPI ship) {
        return getShipSampler(ship).getBoundsPoint();
    }

    /**
     * Gets a point within the ship's bounding polygon.
     *
     * @param ship The ship to check.
     * @return A point within the bounding polygon.
     */
    public static Vector2f getInternalPoint(ShipAPI ship) {
        return getShipSampler(ship).getInternalPoint();
    }

    /**
     * Gets a point within the ship's bounding polygon, but not near another point.
     *
     * @param ship   The ship to check.
     * @param point  The point (battlespace coordinates) to exclude points near.
     * @param radius The radius away from that point the sample point must be.
     * @return A point within the ship but not within the exclusion radius. The centre of the ship if none is found.
     */
    public static Vector2f getInternalPointDistantFrom(ShipAPI ship, Vector2f point, float radius) {
        return getShipSampler(ship).getInternalPointDistantFrom(point, radius);
    }

    /**
     * Gets a point within the ship's bounding polygon, but not near another point.
     *
     * @param ship   The ship to check.
     * @param point  The point (battlespace coordinates) to require the point be near.
     * @param radius The radius within that point the sample point must be.
     * @return A point within the ship bounds and also within the specified radius of the given point.
     */
    public static Vector2f getInternalPointNearTo(ShipAPI ship, Vector2f point, float radius) {
        return getShipSampler(ship).getInternalPointNearTo(point, radius);
    }

    /**
     * Gets a random point on one of the ship's bounding polygon edges.
     *
     * @param ship The ship to check.
     * @return A point on the edges of the bounding polygon.
     */
    public static Vector2f getEdgePoint(ShipAPI ship) {
        return getShipSampler(ship).getEdgePoint();
    }

    /**
     * Gets the smallest value of either the ship's length or width.
     *
     * @param ship The ship to check.
     * @return The minimum of either its length or width.
     */
    public static float getMinDimension(ShipAPI ship) {
        return getShipSampler(ship).getMinDimension();
    }

    /**
     * Gets the largest value of either the ship's length or width.
     *
     * @param ship The ship to check.
     * @return The maximum of either its length or width.
     */
    public static float getMaxDimension(ShipAPI ship) {
        return getShipSampler(ship).getMaxDimension();
    }
}
