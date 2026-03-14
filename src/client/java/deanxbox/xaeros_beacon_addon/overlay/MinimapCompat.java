package deanxbox.xaeros_beacon_addon.overlay;

import net.fabricmc.loader.api.FabricLoader;

public final class MinimapCompat {
    private static final boolean AVAILABLE = FabricLoader.getInstance().isModLoaded("xaerominimap");

    private MinimapCompat() {
    }

    public static boolean isAvailable() {
        return AVAILABLE;
    }

    public static void syncCurrentWorld() {
        if (AVAILABLE) {
            BeaconMinimapSync.syncCurrentWorld();
        }
    }

    public static void createPlanTemporaryWaypoints() {
        if (AVAILABLE) {
            BeaconMinimapSync.createPlanTemporaryWaypoints();
        }
    }

    public static void clearGeneratedPlanWaypoints() {
        if (AVAILABLE) {
            BeaconMinimapSync.clearGeneratedPlanWaypoints();
        }
    }
}
