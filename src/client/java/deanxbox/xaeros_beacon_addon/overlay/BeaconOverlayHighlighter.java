package deanxbox.xaeros_beacon_addon.overlay;

import deanxbox.xaeros_beacon_addon.beacon.BeaconPlacement;
import deanxbox.xaeros_beacon_addon.beacon.BeaconPlacementPlan;
import deanxbox.xaeros_beacon_addon.beacon.BeaconPlacementSolver;
import java.util.Arrays;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaero.map.highlight.AbstractHighlighter;

public class BeaconOverlayHighlighter extends AbstractHighlighter {
    public static final BeaconOverlayHighlighter INSTANCE = new BeaconOverlayHighlighter();

    private static final int MANUAL_FILL = rgba(244, 197, 66, 24);
    private static final int MANUAL_INNER_BORDER = rgba(255, 223, 115, 96);
    private static final int MANUAL_BORDER = rgba(255, 239, 181, 178);
    private static final int MANUAL_RING = rgba(36, 45, 58, 245);
    private static final int MANUAL_CORE = rgba(255, 88, 51, 255);
    private static final int PLAN_SINGLE = rgba(63, 168, 214, 18);
    private static final int PLAN_OVERLAP_TWO = rgba(255, 174, 66, 70);
    private static final int PLAN_OVERLAP_THREE = rgba(255, 112, 67, 110);
    private static final int PLAN_OVERLAP_HEAVY = rgba(220, 53, 69, 150);
    private static final int PLAN_HOLE = rgba(255, 34, 34, 110);
    private static final int PLAN_INNER_BORDER = rgba(122, 201, 235, 78);
    private static final int PLAN_BORDER = rgba(196, 238, 250, 160);
    private static final int PLAN_RING = rgba(26, 34, 46, 245);
    private static final int PLAN_CORE = rgba(255, 40, 40, 255);

    private BeaconOverlayHighlighter() {
        super(true);
    }

    @Override
    public int calculateRegionHash(ResourceKey<Level> dimension, int regionX, int regionZ) {
        int hash = 1;
        BeaconPlacementPlan plan = BeaconOverlayState.getInstance().getPlan(dimension);
        if (plan != null && plan.targetArea().intersectsRegion(regionX, regionZ)) {
            hash = 31 * hash + plan.hashCode();
        }
        for (BeaconOverlay overlay : BeaconOverlayState.getInstance().getOverlays(dimension)) {
            if (overlay.intersectsRegion(regionX, regionZ)) {
                hash = 31 * hash + overlay.hashCode();
            }
        }
        return hash;
    }

    @Override
    public boolean regionHasHighlights(ResourceKey<Level> dimension, int regionX, int regionZ) {
        BeaconPlacementPlan plan = BeaconOverlayState.getInstance().getPlan(dimension);
        if (plan != null && plan.targetArea().intersectsRegion(regionX, regionZ)) {
            return true;
        }
        for (BeaconOverlay overlay : BeaconOverlayState.getInstance().getOverlays(dimension)) {
            if (overlay.intersectsRegion(regionX, regionZ)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean chunkIsHighlit(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        BeaconPlacementPlan plan = BeaconOverlayState.getInstance().getPlan(dimension);
        if (plan != null && plan.targetArea().intersectsChunk(chunkX, chunkZ)) {
            return true;
        }
        for (BeaconOverlay overlay : BeaconOverlayState.getInstance().getOverlays(dimension)) {
            if (overlay.intersectsChunk(chunkX, chunkZ)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int[] getChunkHighlitColor(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        Arrays.fill(resultStore, 0);
        int originX = chunkX << 4;
        int originZ = chunkZ << 4;
        boolean found = false;
        BeaconPlacementPlan plan = BeaconOverlayState.getInstance().getPlan(dimension);
        List<BeaconOverlay> overlays = BeaconOverlayState.getInstance().getOverlays(dimension);

        if (plan != null && plan.targetArea().intersectsChunk(chunkX, chunkZ)) {
            found = true;
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldZ = originZ + localZ;
                for (int localX = 0; localX < 16; localX++) {
                    int worldX = originX + localX;
                    if (!plan.targetArea().containsBlock(worldX, worldZ)) {
                        continue;
                    }
                    int index = (localZ << 4) | localX;
                    int overlap = BeaconPlacementSolver.coverageCount(plan.placements(), worldX, worldZ);
                    resultStore[index] = overlapColor(overlap);
                }
            }
        }

        for (BeaconOverlay overlay : overlays) {
            if (!overlay.intersectsChunk(chunkX, chunkZ)) {
                continue;
            }
            found = true;
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldZ = originZ + localZ;
                for (int localX = 0; localX < 16; localX++) {
                    int worldX = originX + localX;
                    if (!overlay.containsBlock(worldX, worldZ)) {
                        continue;
                    }
                    int color = colorFor(overlay, worldX, worldZ);
                    if (color == 0) {
                        continue;
                    }
                    int index = (localZ << 4) | localX;
                    int existing = resultStore[index];
                    resultStore[index] = existing == 0 ? color : getBlend(existing, color);
                }
            }
        }
        return found ? resultStore : null;
    }

    @Override
    public Component getBlockHighlightSubtleTooltip(ResourceKey<Level> dimension, int blockX, int blockZ) {
        BeaconOverlay overlay = findOverlay(dimension, blockX, blockZ);
        BeaconPlacementPlan plan = BeaconOverlayState.getInstance().getPlan(dimension);
        if (overlay != null) {
            return Component.literal("Place beacon here | tier " + overlay.tier().tier());
        }
        if (plan != null && plan.targetArea().containsBlock(blockX, blockZ)) {
            int overlap = BeaconPlacementSolver.coverageCount(plan.placements(), blockX, blockZ);
            if (overlap == 0) {
                return Component.literal("Coverage hole");
            }
            if (overlap > 1) {
                return Component.literal("Overlap x" + overlap);
            }
            return Component.literal("Covered once");
        }
        return null;
    }

    @Override
    public Component getBlockHighlightBluntTooltip(ResourceKey<Level> dimension, int blockX, int blockZ) {
        BeaconOverlay overlay = findOverlay(dimension, blockX, blockZ);
        BeaconPlacementPlan plan = BeaconOverlayState.getInstance().getPlan(dimension);
        if (overlay != null) {
            return Component.literal("Beacon center: " + overlay.x() + ", " + overlay.z());
        }
        if (plan != null && plan.targetArea().containsBlock(blockX, blockZ)) {
            int overlap = BeaconPlacementSolver.coverageCount(plan.placements(), blockX, blockZ);
            return Component.literal(overlap == 0 ? "No beacon coverage here" : "Coverage layers: " + overlap);
        }
        return null;
    }

    @Override
    public void addMinimapBlockHighlightTooltips(List<Component> tooltips, ResourceKey<Level> dimension, int blockX, int blockZ, int ignored) {
        Component tooltip = getBlockHighlightSubtleTooltip(dimension, blockX, blockZ);
        if (tooltip != null) {
            tooltips.add(tooltip);
        }
    }

    private BeaconOverlay findOverlay(ResourceKey<Level> dimension, int blockX, int blockZ) {
        for (BeaconOverlay overlay : BeaconOverlayState.getInstance().getOverlays(dimension)) {
            if (overlay.containsBlock(blockX, blockZ)) {
                return overlay;
            }
        }
        return null;
    }

    private static int colorFor(BeaconOverlay overlay, int worldX, int worldZ) {
        if (overlay.isCenterMarkerCore(worldX, worldZ)) {
            return coreColor(overlay);
        }
        if (overlay.isCenterMarkerInnerRing(worldX, worldZ)) {
            return ringColor(overlay);
        }
        if (overlay.isCenterMarkerOuterRing(worldX, worldZ)) {
            return ringColor(overlay);
        }
        if (overlay.isBorderBlock(worldX, worldZ)) {
            return borderColor(overlay);
        }
        if (overlay.isInnerBorderBlock(worldX, worldZ)) {
            return innerBorderColor(overlay);
        }
        return overlay.source() == BeaconOverlaySource.MANUAL ? MANUAL_FILL : 0;
    }

    private static int overlapColor(int overlap) {
        return switch (overlap) {
            case 0 -> PLAN_HOLE;
            case 1 -> PLAN_SINGLE;
            case 2 -> PLAN_OVERLAP_TWO;
            case 3 -> PLAN_OVERLAP_THREE;
            default -> PLAN_OVERLAP_HEAVY;
        };
    }

    private static int innerBorderColor(BeaconOverlay overlay) {
        return overlay.source() == BeaconOverlaySource.MANUAL ? MANUAL_INNER_BORDER : PLAN_INNER_BORDER;
    }

    private static int borderColor(BeaconOverlay overlay) {
        return overlay.source() == BeaconOverlaySource.MANUAL ? MANUAL_BORDER : PLAN_BORDER;
    }

    private static int ringColor(BeaconOverlay overlay) {
        return overlay.source() == BeaconOverlaySource.MANUAL ? MANUAL_RING : PLAN_RING;
    }

    private static int coreColor(BeaconOverlay overlay) {
        return overlay.source() == BeaconOverlaySource.MANUAL ? MANUAL_CORE : PLAN_CORE;
    }

    private static int rgba(int red, int green, int blue, int alpha) {
        return (red << 24) | (green << 16) | (blue << 8) | alpha;
    }
}
