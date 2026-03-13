package deanxbox.xaeros_beacon_addon.overlay;

import deanxbox.xaeros_beacon_addon.XaerosOptimalBeaconsAddOn;
import deanxbox.xaeros_beacon_addon.beacon.BeaconPlacementPlan;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.resources.Identifier;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.WaypointPurpose;
import xaero.hud.minimap.world.MinimapWorld;

public final class BeaconMinimapSync {
    private static final Identifier CUSTOM_WAYPOINTS_KEY = Identifier.fromNamespaceAndPath(XaerosOptimalBeaconsAddOn.MOD_ID, "beacon_markers");
    private static final String GENERATED_TEMP_PREFIX = "[Beacon Plan] ";

    private BeaconMinimapSync() {
    }

    public static void syncCurrentWorld() {
        XaeroMinimapSession minimapSession = XaeroMinimapSession.getCurrentSession();
        if (minimapSession == null) {
            return;
        }

        MinimapSession session = minimapSession.getMinimapProcessor().getSession();
        if (session == null) {
            return;
        }

        MinimapWorld currentWorld = session.getWorldManager().getCurrentWorld();
        var customWaypoints = session.getWorldManager().getCustomWaypoints(CUSTOM_WAYPOINTS_KEY);
        customWaypoints.clear();
        if (currentWorld == null || currentWorld.getDimId() == null) {
            return;
        }

        List<BeaconOverlay> overlays = new ArrayList<>(BeaconOverlayState.getInstance().getOverlays(currentWorld.getDimId()));
        overlays.sort(Comparator
            .comparing(BeaconOverlay::source)
            .thenComparingInt(BeaconOverlay::z)
            .thenComparingInt(BeaconOverlay::x));

        int planIndex = 1;
        int manualIndex = 1;
        for (BeaconOverlay overlay : overlays) {
            int index = overlay.source() == BeaconOverlaySource.MANUAL ? manualIndex++ : planIndex++;
            Waypoint waypoint = new Waypoint(
                overlay.x(),
                64,
                overlay.z(),
                nameFor(overlay, index),
                initialsFor(overlay, index),
                colorFor(overlay),
                WaypointPurpose.NORMAL,
                true,
                false
            );
            waypoint.setSymbol(symbolFor(overlay, index));
            waypoint.setTemporary(true);
            customWaypoints.put(uniqueId(overlay), waypoint);
        }
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
                "P" + index,
                WaypointColor.WHITE,
                WaypointPurpose.NORMAL,
                true,
                false
            );
            waypoint.setSymbol(Integer.toString(index % 10));
            waypoint.setTemporary(true);
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
        XaeroMinimapSession minimapSession = XaeroMinimapSession.getCurrentSession();
        if (minimapSession == null) {
            return null;
        }

        MinimapSession session = minimapSession.getMinimapProcessor().getSession();
        if (session == null) {
            return null;
        }

        return session.getWorldManager().getCurrentWorld();
    }

    private static void removeGeneratedPlanWaypoints(MinimapWorld currentWorld) {
        List<Waypoint> toRemove = new ArrayList<>();
        for (Waypoint waypoint : currentWorld.getCurrentWaypointSet().getWaypoints()) {
            if (waypoint.getName() != null && waypoint.getName().startsWith(GENERATED_TEMP_PREFIX)) {
                toRemove.add(waypoint);
            }
        }
        currentWorld.getCurrentWaypointSet().removeAll(toRemove);
    }

    private static int uniqueId(BeaconOverlay overlay) {
        return 31 * overlay.source().ordinal() + 31 * overlay.x() + overlay.z();
    }

    private static String nameFor(BeaconOverlay overlay, int index) {
        String prefix = overlay.source() == BeaconOverlaySource.MANUAL ? "Place Beacon Here" : "Optimal Beacon Placement";
        return prefix + " " + index + " (Tier " + overlay.tier().tier() + ")";
    }

    private static String initialsFor(BeaconOverlay overlay, int index) {
        return overlay.source() == BeaconOverlaySource.MANUAL ? "B" + index : "O" + index;
    }

    private static String symbolFor(BeaconOverlay overlay, int index) {
        return overlay.source() == BeaconOverlaySource.MANUAL ? "!" : Integer.toString(index % 10);
    }

    private static WaypointColor colorFor(BeaconOverlay overlay) {
        return overlay.source() == BeaconOverlaySource.MANUAL ? WaypointColor.RED : WaypointColor.WHITE;
    }
}
