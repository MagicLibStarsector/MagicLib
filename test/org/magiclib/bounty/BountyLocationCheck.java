package org.magiclib.bounty;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
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
                return defaultValue(method.getReturnType());
            }));

            Vector2f playerPosition = new Vector2f(100000f, 0f);
            CampaignFleetAPI player = proxy(CampaignFleetAPI.class, (self, method, values) -> {
                if (method.getName().equals("getLocationInHyperspace")) return playerPosition;
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

            check(MagicBountyCoordinator.findLocationForPayout(-50000) == near, "negative payout clamps to 3 LY");
            check(MagicBountyCoordinator.findLocationForPayout(0) == near, "zero payout targets 3 LY from player, not origin");
            check(MagicBountyCoordinator.findLocationForPayout(50000) == low, "50000 credits targets 5.75 LY");
            check(MagicBountyCoordinator.findLocationForPayout(200000) == mid, "200000 credits targets 14 LY");
            check(MagicBountyCoordinator.findLocationForPayout(350000) == high, "350000 credits targets 22.25 LY");
            check(MagicBountyCoordinator.findLocationForPayout(400000) == far, "400000 credits targets 25 LY");
            check(MagicBountyCoordinator.findLocationForPayout(Integer.MAX_VALUE) == far, "high payout clamps to 25 LY");
            playerPosition.set(111000f, 0f);
            check(MagicBountyCoordinator.findLocationForPayout(0) == mid, "new placement follows current player position");
            playerPosition.set(100000f, 0f);

            SectorEntityToken first = token("first");
            SectorEntityToken second = token("second");
            PlanetAPI planet = proxy(PlanetAPI.class, (self, method, values) -> defaultValue(method.getReturnType()));
            systems.clear();
            systems.add(system(103f, true, Arrays.asList(near), Collections.emptyList(), null));
            systems.add(system(104f, false, Arrays.asList(first, second), Arrays.asList(planet), planet));
            check(MagicBountyCoordinator.findLocationForPayout(0) == first, "hidden systems excluded; first stable location wins");
            systems.clear();
            systems.add(system(104f, false, Collections.emptyList(), Arrays.asList(planet), null));
            check(MagicBountyCoordinator.findLocationForPayout(0) == planet, "planet used without stable location");
            systems.clear();
            systems.add(system(104f, false, Collections.emptyList(), Collections.emptyList(), planet));
            check(MagicBountyCoordinator.findLocationForPayout(0) == planet, "star used without planets or stable locations");
            systems.clear();
            systems.add(system(103f, false, Collections.emptyList(), Collections.emptyList(), null));
            systems.add(system(102f, false, Arrays.asList(first), Collections.emptyList(), null));
            systems.add(system(104f, false, Arrays.asList(second), Collections.emptyList(), null));
            check(MagicBountyCoordinator.findLocationForPayout(0) == first, "empty systems skipped; equal distances keep first system");
            systems.clear();
            check(MagicBountyCoordinator.findLocationForPayout(0) == null, "empty sector permits original resolver fallback");
            Global.setSector(proxy(SectorAPI.class, (self, method, values) -> defaultValue(method.getReturnType())));
            check(MagicBountyCoordinator.findLocationForPayout(0) == null, "absent player permits original resolver fallback");

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
            check(bounty.getShowDistance() == MagicBountyLoader.ShowDistance.Exact,
                    "authored mode is revealed exactly while the player shares the target system");
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

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
