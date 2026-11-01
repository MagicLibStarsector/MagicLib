package org.magiclib.bounty;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.util.MutableValue;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import org.json.JSONObject;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Run with compiled MagicLib classes and the game dependencies on the classpath. */
public final class BountyLocationCheck {
    private static final boolean GATES_OFF = "off".equals(System.getProperty("bountyLocationGates"));

    public static void main(String[] args) {
        SettingsAPI previousSettings = Global.getSettings();
        SectorAPI previousSector = Global.getSector();
        try {
            Global.setSettings(proxy(SettingsAPI.class, (self, method, values) -> {
                if (method.getName().equals("getFloat")) return 1000f;
                if (method.getName().equals("getColor")) return Color.WHITE;
                if (method.getName().equals("getModManager")) {
                    return proxy(ModManagerAPI.class, (inner, innerMethod, innerValues) -> false);
                }
                if (GATES_OFF && method.getName().equals("doesFileExist")) return true;
                if (GATES_OFF && method.getName().equals("loadJSON")) {
                    return new JSONObject()
                            .put("magiclib_bountyLocationScaleWithReward", false)
                            .put("magiclib_bountyLocationRevealInSystem", false);
                }
                return defaultValue(method.getReturnType());
            }));

            check(MagicBountyCoordinator.isPayoutPlacementEnabled() != GATES_OFF,
                    "payout placement gate follows the setting");

            Vector2f playerPosition = new Vector2f(100000f, 0f);
            MutableValue credits = new MutableValue(0f);
            CampaignFleetAPI player = proxy(CampaignFleetAPI.class, (self, method, values) -> {
                if (method.getName().equals("getLocationInHyperspace")) return playerPosition;
                if (method.getName().equals("getCargo")) {
                    return proxy(CargoAPI.class, (inner, innerMethod, innerValues) ->
                            innerMethod.getName().equals("getCredits") ? credits : defaultValue(innerMethod.getReturnType()));
                }
                return defaultValue(method.getReturnType());
            });
            List<StarSystemAPI> systems = new ArrayList<>();
            Global.setSector(proxy(SectorAPI.class, (self, method, values) -> {
                if (method.getName().equals("getPlayerFleet")) return player;
                if (method.getName().equals("getStarSystems")) return systems;
                return defaultValue(method.getReturnType());
            }));

            SectorEntityToken near = token("near");
            SectorEntityToken low = token("low");
            SectorEntityToken mid = token("mid");
            SectorEntityToken high = token("high");
            SectorEntityToken far = token("far");
            systems.add(system(103f, false, Arrays.asList(near), Collections.emptyList(), null));
            systems.add(system(105.75f, false, Arrays.asList(low), Collections.emptyList(), null));
            systems.add(system(114f, false, Arrays.asList(mid), Collections.emptyList(), null));
            systems.add(system(122.25f, false, Arrays.asList(high), Collections.emptyList(), null));
            systems.add(system(125f, false, Arrays.asList(far), Collections.emptyList(), null));

            check(MagicBountyCoordinator.findLocationForPayout(-50000, 0) == near, "negative payout clamps to 3 LY");
            check(MagicBountyCoordinator.findLocationForPayout(0, 0) == near, "zero payout targets 3 LY from player, not origin");
            check(MagicBountyCoordinator.findLocationForPayout(50000, 0) == low, "50000 credits targets 5.75 LY");
            check(MagicBountyCoordinator.findLocationForPayout(200000, 0) == mid, "200000 credits targets 14 LY");
            check(MagicBountyCoordinator.findLocationForPayout(350000, 0) == high, "350000 credits targets 22.25 LY");
            check(MagicBountyCoordinator.findLocationForPayout(400000, 0) == far, "400000 credits targets 25 LY");
            check(MagicBountyCoordinator.findLocationForPayout(Integer.MAX_VALUE, 0) == far, "high payout clamps to 25 LY");
            playerPosition.set(111000f, 0f);
            check(MagicBountyCoordinator.findLocationForPayout(0, 0) == mid, "new placement follows current player position");
            playerPosition.set(100000f, 0f);

            check(MagicBountyCoordinator.targetDistanceLY(400000, 0, 0f) == 25f, "no difficulty keeps the payout curve");
            check(MagicBountyCoordinator.targetDistanceLY(400000, 150, 0f) == 14f, "half difficulty halves the payout distance");
            check(MagicBountyCoordinator.targetDistanceLY(400000, 300, 0f) == 3f, "300 FP pulls a rich bounty to the minimum");
            check(MagicBountyCoordinator.targetDistanceLY(400000, 1000, 0f) == 3f, "extreme difficulty clamps to the minimum");
            check(MagicBountyCoordinator.targetDistanceLY(200000, 300, 0f) == 3f, "difficulty overrides a mid payout too");
            check(MagicBountyCoordinator.targetDistanceLY(400000, 100, 0f) < MagicBountyCoordinator.targetDistanceLY(400000, 0, 0f),
                    "harder bounties spawn closer at equal payout");
            check(MagicBountyCoordinator.findLocationForPayout(400000, 300) == near, "hard rich bounty lands at the closest system");
            check(MagicBountyCoordinator.findLocationForPayout(400000, 150) == mid, "half difficulty lands halfway");

            checkNear(MagicBountyCoordinator.targetDistanceLY(400000, 0, 0f), 25f, "no credits keeps the absolute payout curve");
            checkNear(MagicBountyCoordinator.targetDistanceLY(400000, 0, 5000000f), 20.6f, "10 percent of 5M credits scales a 400k payout down");
            checkNear(MagicBountyCoordinator.targetDistanceLY(400000, 0, 20000000f), 7.4f, "20M credits makes a 400k payout short");
            check(MagicBountyCoordinator.targetDistanceLY(400000, 0, 1000000000f) < 4f, "wealth floors the payout distance");
            check(MagicBountyCoordinator.targetDistanceLY(400000, 0, 10000000f) < MagicBountyCoordinator.targetDistanceLY(400000, 0, 0f),
                    "richer players travel less for the same payout");
            credits.set(20000000f);
            check(MagicBountyCoordinator.findLocationForPayout(400000, 0) == low, "rich player placement uses the wealth-scaled distance");
            credits.set(0f);

            SectorEntityToken first = token("first");
            SectorEntityToken second = token("second");
            PlanetAPI planet = proxy(PlanetAPI.class, (self, method, values) -> defaultValue(method.getReturnType()));
            systems.clear();
            systems.add(system(103f, true, Arrays.asList(near), Collections.emptyList(), null));
            systems.add(system(104f, false, Arrays.asList(first, second), Arrays.asList(planet), planet));
            check(MagicBountyCoordinator.findLocationForPayout(0, 0) == first, "hidden systems excluded; first stable location wins");
            systems.clear();
            systems.add(system(104f, false, Collections.emptyList(), Arrays.asList(planet), null));
            check(MagicBountyCoordinator.findLocationForPayout(0, 0) == planet, "planet used without stable location");
            systems.clear();
            systems.add(system(104f, false, Collections.emptyList(), Collections.emptyList(), planet));
            check(MagicBountyCoordinator.findLocationForPayout(0, 0) == planet, "star used without planets or stable locations");
            systems.clear();
            systems.add(system(103f, false, Collections.emptyList(), Collections.emptyList(), null));
            systems.add(system(102f, false, Arrays.asList(first), Collections.emptyList(), null));
            systems.add(system(104f, false, Arrays.asList(second), Collections.emptyList(), null));
            check(MagicBountyCoordinator.findLocationForPayout(0, 0) == first, "empty systems skipped; equal distances keep first system");
            systems.clear();
            check(MagicBountyCoordinator.findLocationForPayout(0, 0) == null, "empty sector permits original resolver fallback");
            Global.setSector(proxy(SectorAPI.class, (self, method, values) -> defaultValue(method.getReturnType())));
            check(MagicBountyCoordinator.findLocationForPayout(0, 0) == null, "absent player permits original resolver fallback");

            SectorEntityToken spawn = proxy(SectorEntityToken.class, (self, method, values) -> defaultValue(method.getReturnType()));
            StarSystemAPI targetSystem = system(103f, false, Arrays.asList(spawn), Collections.emptyList(), null);
            SectorEntityToken spawnToken = proxy(SectorEntityToken.class, (self, method, values) -> {
                if (method.getName().equals("getContainingLocation")) return targetSystem;
                return defaultValue(method.getReturnType());
            });
            CampaignFleetAPI revealPlayer = proxy(CampaignFleetAPI.class, (self, method, values) -> {
                if (method.getName().equals("getContainingLocation")) return targetSystem;
                return defaultValue(method.getReturnType());
            });
            CampaignFleetAPI bountyFleet = proxy(CampaignFleetAPI.class, (self, method, values) -> {
                if (method.getName().equals("getFleetData")) {
                    return proxy(FleetDataAPI.class, (inner, innerMethod, innerValues) ->
                            innerMethod.getName().equals("getMembersListCopy") ? Collections.emptyList() : defaultValue(innerMethod.getReturnType()));
                }
                return defaultValue(method.getReturnType());
            });
            CampaignClockAPI clock = proxy(CampaignClockAPI.class, (self, method, values) -> defaultValue(method.getReturnType()));
            setSector(revealPlayer, clock);

            MagicBountySpec spec = specWithAuthoredDistance(MagicBountyLoader.ShowDistance.VanillaDistance);
            installCoordinator();
            ActiveBounty bounty = new ActiveBounty("check", bountyFleet, spawnToken, Collections.emptyList(), spec);
            check(bounty.getShowDistance() == (GATES_OFF
                            ? MagicBountyLoader.ShowDistance.VanillaDistance
                            : MagicBountyLoader.ShowDistance.Exact),
                    "in-system reveal follows the setting");
            setSector(proxy(CampaignFleetAPI.class, (self, method, values) -> defaultValue(method.getReturnType())), clock);
            check(bounty.getShowDistance() == MagicBountyLoader.ShowDistance.VanillaDistance,
                    "authored mode returns outside the target system");
            setSector(null, clock);
            check(bounty.getShowDistance() == MagicBountyLoader.ShowDistance.VanillaDistance,
                    "authored mode returns without a player fleet");

            System.out.println("BountyLocationCheck OK");
        } finally {
            Global.setSector(previousSector);
            Global.setSettings(previousSettings);
        }
    }

    private static StarSystemAPI system(float xLY, boolean hidden, List<SectorEntityToken> stable,
                                         List<PlanetAPI> planets, PlanetAPI star) {
        Vector2f position = new Vector2f(xLY * 1000f, 0f);
        SectorEntityToken center = proxy(SectorEntityToken.class, (self, method, values) -> {
            if (method.getName().equals("getLocationInHyperspace")) return position;
            return defaultValue(method.getReturnType());
        });
        return proxy(StarSystemAPI.class, (self, method, values) -> {
            switch (method.getName()) {
                case "hasTag": return hidden && Tags.THEME_HIDDEN.equals(values[0]);
                case "getEntitiesWithTag": return Tags.STABLE_LOCATION.equals(values[0]) ? stable : Collections.emptyList();
                case "getPlanets": return planets;
                case "getStar": return star;
                case "getCenter": return center;
                default: return defaultValue(method.getReturnType());
            }
        });
    }

    private static void setSector(CampaignFleetAPI player, CampaignClockAPI clock) {
        Global.setSector(proxy(SectorAPI.class, (self, method, values) -> {
            if (method.getName().equals("getPlayerFleet")) return player;
            if (method.getName().equals("getClock")) return clock;
            return defaultValue(method.getReturnType());
        }));
    }

    private static MagicBountySpec specWithAuthoredDistance(MagicBountyLoader.ShowDistance authored) {
        try {
            MagicBountySpec spec = (MagicBountySpec) allocate(MagicBountySpec.class);
            spec.job_show_distance = authored;
            return spec;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("cannot allocate MagicBountySpec", e);
        }
    }

    private static void installCoordinator() {
        try {
            Field instance = MagicBountyCoordinator.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, allocate(MagicBountyCoordinator.class));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("cannot install MagicBountyCoordinator", e);
        }
    }

    private static Object allocate(Class<?> type) throws ReflectiveOperationException {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field field = unsafeClass.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return unsafeClass.getMethod("allocateInstance", Class.class).invoke(field.get(null), type);
    }

    private static SectorEntityToken token(String id) {
        return proxy(SectorEntityToken.class, (self, method, values) -> {
            if (method.getName().equals("getId")) return id;
            return defaultValue(method.getReturnType());
        });
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0f;
        if (type == double.class) return 0d;
        return null;
    }

    private static void checkNear(float actual, float expected, String message) {
        check(Math.abs(actual - expected) < 0.01f, message + " (was " + actual + ")");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
