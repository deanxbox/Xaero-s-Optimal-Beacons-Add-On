package deanxbox.xaeros_beacon_addon.overlay;

import deanxbox.xaeros_beacon_addon.beacon.BeaconPlacementPlan;
import deanxbox.xaeros_beacon_addon.beacon.BeaconPlacementSolver;
import deanxbox.xaeros_beacon_addon.config.BeaconClientConfig;
import java.util.Arrays;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaero.map.highlight.AbstractHighlighter;

public class BeaconOverlayHighlighter extends AbstractHighlighter {
    public static final BeaconOverlayHighlighter INSTANCE = new BeaconOverlayHighlighter();

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
        if (plan != null && BeaconClientConfig.get().ui.showHeatmap && plan.targetArea().intersectsChunk(chunkX, chunkZ)) {
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
        BeaconClientConfig.OverlayColors colors = BeaconClientConfig.get().colors;
        Arrays.fill(resultStore, 0);
        int originX = chunkX << 4;
        int originZ = chunkZ << 4;
        boolean found = false;
        BeaconPlacementPlan plan = BeaconOverlayState.getInstance().getPlan(dimension);
        List<BeaconOverlay> overlays = BeaconOverlayState.getInstance().getOverlays(dimension);

        if (plan != null && BeaconClientConfig.get().ui.showHeatmap && plan.targetArea().intersectsChunk(chunkX, chunkZ)) {
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
                    resultStore[index] = overlapColor(colors, overlap);
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
                    int color = colorFor(colors, overlay, worldX, worldZ);
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

    private static int colorFor(BeaconClientConfig.OverlayColors colors, BeaconOverlay overlay, int worldX, int worldZ) {
        if (overlay.isPreviewTop(worldX, worldZ) || overlay.isPreviewBase(worldX, worldZ)) {
            return ringColor(colors, overlay);
        }
        if (overlay.isPreviewPillar(worldX, worldZ)) {
            return coreColor(colors, overlay);
        }
        if (overlay.isCenterMarkerCore(worldX, worldZ)) {
            return coreColor(colors, overlay);
        }
        if (overlay.isCenterMarkerInnerRing(worldX, worldZ)) {
            return ringColor(colors, overlay);
        }
        if (overlay.isCenterMarkerOuterRing(worldX, worldZ)) {
            return ringColor(colors, overlay);
        }
        if (overlay.isBorderBlock(worldX, worldZ)) {
            return borderColor(colors, overlay);
        }
        if (overlay.isInnerBorderBlock(worldX, worldZ)) {
            return innerBorderColor(colors, overlay);
        }
        return overlay.source() == BeaconOverlaySource.MANUAL ? colors.manualFill : 0;
    }

    private static int overlapColor(BeaconClientConfig.OverlayColors colors, int overlap) {
        return switch (overlap) {
            case 0 -> colors.planUncovered;
            case 1 -> colors.planCovered;
            case 2 -> colors.planOverlap;
            default -> colors.planHeavyOverlap;
        };
    }

    private static int innerBorderColor(BeaconClientConfig.OverlayColors colors, BeaconOverlay overlay) {
        return overlay.source() == BeaconOverlaySource.MANUAL ? colors.manualInnerBorder : colors.planInnerBorder;
    }

    private static int borderColor(BeaconClientConfig.OverlayColors colors, BeaconOverlay overlay) {
        return overlay.source() == BeaconOverlaySource.MANUAL ? colors.manualBorder : colors.planBorder;
    }

    private static int ringColor(BeaconClientConfig.OverlayColors colors, BeaconOverlay overlay) {
        return overlay.source() == BeaconOverlaySource.MANUAL ? colors.manualRing : colors.planRing;
    }

    private static int coreColor(BeaconClientConfig.OverlayColors colors, BeaconOverlay overlay) {
        return overlay.source() == BeaconOverlaySource.MANUAL ? colors.manualCore : colors.planCore;
    }
}
