package org.magiclib.combat;

import com.fs.starfarer.api.combat.BoundsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

/**
 * Slightly more efficiently samples points within or on a ship's bounds.
 * <p>
 * Generally, just use `MagicPointSampler`. If you really need to, try:
 * <pre>
 *     ShipPointSampler mySampler = new ShipPointSampler(myShip);
 *     Vector2f myShipPoint = mySampler.getInternalPoint();
 * </pre>
 *
 * @author Toaster
 */
public class ShipPointSampler {
    public static int RANDOM_POINT_MAX_TRIES = 8;
    /// How many times to try sampling before giving up.

    protected final float x_base, y_base, x_mult, y_mult;
    /// The base values on each axis, and the range they span.

    protected final ShipAPI ship;
    /// The ship this is for.

    /**
     * Declares the sampler for a specific ship.
     *
     * @param shipAPI The entity to use the bounds of.
     */
    public ShipPointSampler(ShipAPI shipAPI) {
        float x_min = 9999f, x_max = -9999f, y_min = 9999f, y_max = -9999f;
        BoundsAPI bounds = shipAPI.getExactBounds();
        for (BoundsAPI.SegmentAPI segment : bounds.getOrigSegments()) {
            if (segment.getP1().x > x_max) x_max = segment.getP1().x;
            if (segment.getP1().x < x_min) x_min = segment.getP1().x;
            if (segment.getP1().y > y_max) y_max = segment.getP1().y;
            if (segment.getP1().y < y_min) y_min = segment.getP1().y;
            if (segment.getP2().x > x_max) x_max = segment.getP2().x;
            if (segment.getP2().x < x_min) x_min = segment.getP2().x;
            if (segment.getP2().y > y_max) y_max = segment.getP2().y;
            if (segment.getP2().y < y_min) y_min = segment.getP2().y;
        }
        ship = shipAPI;
        x_base = x_min;
        x_mult = x_max - x_min;
        y_base = y_min;
        y_mult = y_max - y_min;
    }

    /**
     * Gets a point within the collision bounds.
     *
     * @return A point within the ship's bounds, or its centre if sampling fails.
     */
    protected Vector2f getInternalPoint() {
        Vector2f point = new Vector2f();
        float sin = (float) FastTrig.sin(Math.toRadians(ship.getFacing()));
        float cos = (float) FastTrig.cos(Math.toRadians(ship.getFacing()));
        float x_random, y_random;

        for (int i = 0; i < RANDOM_POINT_MAX_TRIES; i++) {
            x_random = x_base + x_mult * (float) Math.random();
            y_random = y_base + y_mult * (float) Math.random();

            Vector2f.add(
                    ship.getLocation(),
                    new Vector2f(
                            cos * x_random + sin * y_random,
                            sin * x_random + cos * y_random
                    ),
                    point
            );
            if (CollisionUtils.isPointWithinBounds(point, ship)) return point;
        }
        return new Vector2f(ship.getLocation());
    }

    /**
     * Gets a point in the bounds, but not within a given distance of another point.
     *
     * @param point  The point to not be near.
     * @param radius The radius away from the point the sample must be.
     * @return The location if successful, or the ship's centre if not.
     */
    protected Vector2f getInternalPointDistantFrom(Vector2f point, float radius) {
        Vector2f pointOut = new Vector2f();
        float x_random, y_random;
        float radiusSq = radius * radius;
        float sin = (float) FastTrig.sin(Math.toRadians(ship.getFacing()));
        float cos = (float) FastTrig.cos(Math.toRadians(ship.getFacing()));

        for (int i = 0; i < RANDOM_POINT_MAX_TRIES; i++) {
            x_random = x_base + x_mult * (float) Math.random();
            y_random = y_base + y_mult * (float) Math.random();

            Vector2f.add(
                    ship.getLocation(),
                    new Vector2f(
                            cos * x_random + sin * y_random,
                            sin * x_random + cos * y_random
                    ),
                    pointOut
            );
            if (Misc.getDistanceSq(point, pointOut) > radiusSq && CollisionUtils.isPointWithinBounds(pointOut, ship)) {
                return pointOut;
            }
        }
        return new Vector2f(ship.getLocation());
    }

    /**
     * Gets a point in the bounds, within a given distance of another point.
     *
     * @param point  The point to be near.
     * @param radius The radius within the point the sample must be.
     * @return The location if successful, or the ship's centre if not.
     */
    protected Vector2f getInternalPointNearTo(Vector2f point, float radius) {
        Vector2f pointOut = new Vector2f();
        float x_random, y_random;
        float radiusSq = radius * radius;
        float sin = (float) FastTrig.sin(Math.toRadians(ship.getFacing()));
        float cos = (float) FastTrig.cos(Math.toRadians(ship.getFacing()));

        for (int i = 0; i < RANDOM_POINT_MAX_TRIES; i++) {
            x_random = x_base + x_mult * (float) Math.random();
            y_random = y_base + y_mult * (float) Math.random();

            Vector2f.add(
                    ship.getLocation(),
                    new Vector2f(
                            cos * x_random + sin * y_random,
                            sin * x_random + cos * y_random
                    ),
                    pointOut
            );

            if (Misc.getDistanceSq(point, pointOut) <= radiusSq && CollisionUtils.isPointWithinBounds(pointOut, ship)) {
                return pointOut;
            }
        }
        return new Vector2f(ship.getLocation());
    }

    /**
     * Gets one of the bounding box points.
     *
     * @return One of the bounds points.
     */
    protected Vector2f getBoundsPoint() {
        List<BoundsAPI.SegmentAPI> segments = ship.getExactBounds().getSegments();
        return segments.get(MathUtils.getRandomNumberInRange(0, segments.size() - 1)).getP1();
    }

    /**
     * Gets a point on the edge of the ship's bounding box.
     *
     * @return A point somewhere along one of the bounding box segments.
     */
    protected Vector2f getEdgePoint() {
        List<BoundsAPI.SegmentAPI> segments = ship.getExactBounds().getSegments();
        BoundsAPI.SegmentAPI segment = segments.get(MathUtils.getRandomNumberInRange(0, segments.size() - 1));
        float random = (float) Math.random();

        return Vector2f.add(
                (Vector2f) new Vector2f(segment.getP1()).scale(random),
                (Vector2f) new Vector2f(segment.getP2()).scale(1f - random),
                new Vector2f()
        );
    }

    /**
     * Gets the smallest scale of the ship, more usefully than getCollisionRadius.
     *
     * @return The minimum of the X and Y dimensions.
     */
    protected float getMinDimension() {
        return Math.min(
                x_base + x_mult,
                y_base + y_mult
        );
    }

    /**
     * Gets the largest scale of the ship, more usefully than getCollisionRadius.
     *
     * @return The maximum of the X and Y dimensions.
     */
    protected float getMaxDimension() {
        return Math.max(
                x_base + x_mult,
                y_base + y_mult
        );
    }
}
