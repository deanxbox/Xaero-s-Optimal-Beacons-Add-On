package deanxbox.xaeros_beacon_addon.overlay;

import deanxbox.xaeros_beacon_addon.beacon.BeaconPlanSnapMode;
import deanxbox.xaeros_beacon_addon.beacon.BeaconPlacement;
import deanxbox.xaeros_beacon_addon.beacon.BeaconPlacementPlan;
import deanxbox.xaeros_beacon_addon.beacon.BeaconTier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaero.map.WorldMapSession;
import xaero.map.world.MapWorld;

public final class BeaconOverlayState {
    private static final BeaconOverlayState INSTANCE = new BeaconOverlayState();

    private final List<BeaconOverlay> manualBeacons = new ArrayList<>();
    private final List<BeaconOverlay> plannedBeacons = new ArrayList<>();
    private ResourceKey<Level> planDimension;
    private BeaconPlacementPlan plan;
    private BeaconTier selectedPlanTier = BeaconTier.max();
    private BeaconPlanSnapMode selectedPlanSnapMode = BeaconPlanSnapMode.FREE;

    private BeaconOverlayState() {
    }

    public static BeaconOverlayState getInstance() {
        return INSTANCE;
    }

    public synchronized BeaconOverlay placeManualBeacon(ResourceKey<Level> dimension, int x, int z, BeaconTier tier) {
        BeaconOverlay beacon = new BeaconOverlay(dimension, x, z, tier, BeaconOverlaySource.MANUAL);
        manualBeacons.removeIf(existing -> existing.dimension().equals(dimension) && existing.x() == x && existing.z() == z);
        manualBeacons.add(beacon);
        onStateChanged();
        return beacon;
    }

    public synchronized BeaconOverlay updateManualBeaconTier(BeaconOverlay beacon, BeaconTier tier) {
        manualBeacons.remove(beacon);
        BeaconOverlay updated = new BeaconOverlay(beacon.dimension(), beacon.x(), beacon.z(), tier, BeaconOverlaySource.MANUAL);
        manualBeacons.add(updated);
        onStateChanged();
        return updated;
    }

    public synchronized void removeManualBeacon(BeaconOverlay beacon) {
        if (manualBeacons.remove(beacon)) {
            onStateChanged();
        }
    }

    public synchronized void clearManualBeacons(ResourceKey<Level> dimension) {
        if (manualBeacons.removeIf(beacon -> beacon.dimension().equals(dimension))) {
            onStateChanged();
        }
    }

    public synchronized void setSelectedPlanTier(BeaconTier tier) {
        selectedPlanTier = tier;
    }

    public synchronized BeaconTier getSelectedPlanTier() {
        return selectedPlanTier;
    }

    public synchronized void setSelectedPlanSnapMode(BeaconPlanSnapMode snapMode) {
        selectedPlanSnapMode = snapMode;
    }

    public synchronized BeaconPlanSnapMode getSelectedPlanSnapMode() {
        return selectedPlanSnapMode;
    }

    public synchronized void setPlan(ResourceKey<Level> dimension, BeaconPlacementPlan plan) {
        BeaconMinimapSync.clearGeneratedPlanWaypoints();
        this.planDimension = dimension;
        this.plan = plan;
        plannedBeacons.clear();
        for (BeaconPlacement placement : plan.placements()) {
            plannedBeacons.add(new BeaconOverlay(dimension, placement.x(), placement.z(), placement.tier(), BeaconOverlaySource.PLAN));
        }
        onStateChanged();
    }

    public synchronized void clearPlan() {
        if (!plannedBeacons.isEmpty() || plan != null) {
            BeaconMinimapSync.clearGeneratedPlanWaypoints();
            plannedBeacons.clear();
            plan = null;
            planDimension = null;
            onStateChanged();
        }
    }

    public synchronized boolean hasPlan(ResourceKey<Level> dimension) {
        return plan != null && dimension.equals(planDimension);
    }

    public synchronized BeaconPlacementPlan getPlan(ResourceKey<Level> dimension) {
        return hasPlan(dimension) ? plan : null;
    }

    public synchronized boolean hasManualBeacons(ResourceKey<Level> dimension) {
        return manualBeacons.stream().anyMatch(beacon -> beacon.dimension().equals(dimension));
    }

    public synchronized List<BeaconOverlay> getOverlays(ResourceKey<Level> dimension) {
        List<BeaconOverlay> overlays = new ArrayList<>();
        for (BeaconOverlay beacon : manualBeacons) {
            if (beacon.dimension().equals(dimension)) {
                overlays.add(beacon);
            }
        }
        for (BeaconOverlay beacon : plannedBeacons) {
            if (beacon.dimension().equals(dimension)) {
                overlays.add(beacon);
            }
        }
        overlays.sort(Comparator.comparing(BeaconOverlay::source).thenComparingInt(BeaconOverlay::x).thenComparingInt(BeaconOverlay::z));
        return List.copyOf(overlays);
    }

    public synchronized BeaconOverlay findManualBeaconAt(ResourceKey<Level> dimension, int blockX, int blockZ) {
        return manualBeacons.stream()
            .filter(beacon -> beacon.dimension().equals(dimension) && beacon.containsBlock(blockX, blockZ))
            .min(Comparator.comparingInt(beacon -> Math.abs(beacon.x() - blockX) + Math.abs(beacon.z() - blockZ)))
            .orElse(null);
    }

    public synchronized BeaconOverlay findOverlayAt(ResourceKey<Level> dimension, int blockX, int blockZ) {
        return getOverlays(dimension).stream()
            .filter(beacon -> beacon.containsBlock(blockX, blockZ))
            .min(Comparator.comparingInt(beacon -> Math.abs(beacon.x() - blockX) + Math.abs(beacon.z() - blockZ)))
            .orElse(null);
    }

    public synchronized void invalidateHighlights() {
        WorldMapSession session = WorldMapSession.getCurrentSession();
        if (session == null || !session.isUsable()) {
            return;
        }
        if (session.getMapProcessor() == null || session.getMapProcessor().getMapWorld() == null) {
            return;
        }
        MapWorld mapWorld = session.getMapProcessor().getMapWorld();
        mapWorld.clearAllCachedHighlightHashes();
    }

    private void onStateChanged() {
        invalidateHighlights();
        BeaconMinimapSync.syncCurrentWorld();
    }
}
