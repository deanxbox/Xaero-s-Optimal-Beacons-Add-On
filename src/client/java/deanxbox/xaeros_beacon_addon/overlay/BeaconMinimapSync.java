package deanxbox.xaeros_beacon_addon.overlay;

import deanxbox.xaeros_beacon_addon.XaerosOptimalBeaconsAddOn;
import deanxbox.xaeros_beacon_addon.beacon.BeaconPlacementPlan;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.Identifier;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.WaypointPurpose;
import xaero.hud.minimap.world.MinimapWorld;

public final class BeaconMinimapSync {
    private static final Identifier CUSTOM_WAYPOINTS_KEY = Identifier.fromNamespaceAndPath(XaerosOptimalBeaconsAddOn.MOD_ID, "beacon_markers");
    private static final Identifier GENERATED_WAYPOINTS_ORIGIN = Identifier.fromNamespaceAndPath(XaerosOptimalBeaconsAddOn.MOD_ID, "generated_plan_waypoints");
    private static final String GENERATED_TEMP_PREFIX = "[Beacon Plan] ";
    private static final long UNCHANGED_REFRESH_INTERVAL_NANOS = 5_000_000_000L;
    private static MinimapWorld lastSyncedWorld;
    private static int lastSyncedHash;
    private static long lastSyncedNanos;

    private BeaconMinimapSync() {
    }

    public static void syncCurrentWorld() {
        MinimapSession session = BuiltInHudModules.MINIMAP.getCurrentSession();
        if (session == null) {
            return;
        }

        MinimapWorld currentWorld = session.getWorldManager().getCurrentWorld();
        if (currentWorld == null || currentWorld.getDimId() == null) {
            resetSyncCache();
            return;
        }

        List<BeaconOverlay> overlays = new ArrayList<>(BeaconOverlayState.getInstance().getOverlays(currentWorld.getDimId()));
        overlays.sort(Comparator
            .comparing(BeaconOverlay::source)
            .thenComparingInt(BeaconOverlay::z)
            .thenComparingInt(BeaconOverlay::x));

        int syncHash = Objects.hash(currentWorld.getDimId(), overlays);
        long now = System.nanoTime();
        if (currentWorld == lastSyncedWorld
            && syncHash == lastSyncedHash
            && now - lastSyncedNanos < UNCHANGED_REFRESH_INTERVAL_NANOS) {
            return;
        }

        var customWaypoints = session.getWorldManager().getCustomWaypoints(CUSTOM_WAYPOINTS_KEY);
        customWaypoints.clear();

        int waypointId = 0;
        int planIndex = 1;
        int manualIndex = 1;
        for (BeaconOverlay overlay : overlays) {
            int index = overlay.source() == BeaconOverlaySource.MANUAL ? manualIndex++ : planIndex++;
            Waypoint waypoint = new Waypoint(
                overlay.x(),
                64,
                overlay.z(),
                nameFor(overlay, index),
                symbolFor(overlay, index),
                colorFor(overlay),
                WaypointPurpose.NORMAL,
                true,
                false
            );
            waypoint.setTemporary(true);
            customWaypoints.put(waypointId++, waypoint);
        }
        lastSyncedWorld = currentWorld;
        lastSyncedHash = syncHash;
        lastSyncedNanos = now;
    }

    public static void createPlanTemporaryWaypoints() {
        MinimapWorld currentWorld = currentWorld();
        if (currentWorld == null || currentWorld.getDimId() == null) {
            return;
        }

        BeaconPlacementPlan plan = BeaconOverlayState.getInstance().getPlan(currentWorld.getDimId());
        if (plan == null) {
            return;
        }

        clearGeneratedPlanWaypoints();

        int index = 1;
        for (BeaconOverlay overlay : BeaconOverlayState.getInstance().getOverlays(currentWorld.getDimId())) {
            if (overlay.source() != BeaconOverlaySource.PLAN) {
                continue;
            }
            Waypoint waypoint = new Waypoint(
                overlay.x(),
                64,
                overlay.z(),
                GENERATED_TEMP_PREFIX + index,
                Integer.toString(index % 10),
                WaypointColor.WHITE,
                WaypointPurpose.NORMAL,
                true,
                false
            );
            waypoint.setTemporary(true);
            waypoint.setThirdPartyOrigin(GENERATED_WAYPOINTS_ORIGIN);
            currentWorld.getCurrentWaypointSet().add(waypoint);
            index++;
        }
    }

    public static void clearGeneratedPlanWaypoints() {
        MinimapWorld currentWorld = currentWorld();
        if (currentWorld == null) {
            return;
        }
        removeGeneratedPlanWaypoints(currentWorld);
    }

    private static MinimapWorld currentWorld() {
        MinimapSession session = BuiltInHudModules.MINIMAP.getCurrentSession();
        if (session == null) {
            return null;
        }

        return session.getWorldManager().getCurrentWorld();
    }

    private static void removeGeneratedPlanWaypoints(MinimapWorld currentWorld) {
        List<Waypoint> toRemove = new ArrayList<>();
        for (Waypoint waypoint : currentWorld.getCurrentWaypointSet().getWaypoints()) {
            if (GENERATED_WAYPOINTS_ORIGIN.equals(waypoint.getThirdPartyOrigin())) {
                toRemove.add(waypoint);
            }
        }
        currentWorld.getCurrentWaypointSet().removeAll(toRemove);
    }

    private static void resetSyncCache() {
        lastSyncedWorld = null;
        lastSyncedHash = 0;
        lastSyncedNanos = 0L;
    }

    private static String nameFor(BeaconOverlay overlay, int index) {
        String prefix = overlay.source() == BeaconOverlaySource.MANUAL ? "Place Beacon Here" : "Optimal Beacon Placement";
        return prefix + " " + index + " (Tier " + overlay.tier().tier() + ")";
    }

    private static String symbolFor(BeaconOverlay overlay, int index) {
        return overlay.source() == BeaconOverlaySource.MANUAL ? "!" : Integer.toString(index % 10);
    }

    private static WaypointColor colorFor(BeaconOverlay overlay) {
        return overlay.source() == BeaconOverlaySource.MANUAL ? WaypointColor.RED : WaypointColor.WHITE;
    }
}
