package deanxbox.xaeros_beacon_addon.overlay;

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
    private static final int PLAN_FILL = rgba(63, 168, 214, 18);
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
        for (BeaconOverlay overlay : BeaconOverlayState.getInstance().getOverlays(dimension)) {
            if (overlay.intersectsRegion(regionX, regionZ)) {
                hash = 31 * hash + overlay.hashCode();
            }
        }
        return hash;
    }

    @Override
    public boolean regionHasHighlights(ResourceKey<Level> dimension, int regionX, int regionZ) {
        for (BeaconOverlay overlay : BeaconOverlayState.getInstance().getOverlays(dimension)) {
            if (overlay.intersectsRegion(regionX, regionZ)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean chunkIsHighlit(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
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
        List<BeaconOverlay> overlays = BeaconOverlayState.getInstance().getOverlays(dimension);
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
        if (overlay == null) {
            return null;
        }
        return Component.literal("Place beacon here | tier " + overlay.tier().tier());
    }

    @Override
    public Component getBlockHighlightBluntTooltip(ResourceKey<Level> dimension, int blockX, int blockZ) {
        BeaconOverlay overlay = findOverlay(dimension, blockX, blockZ);
        if (overlay == null) {
            return null;
        }
        return Component.literal("Beacon center: " + overlay.x() + ", " + overlay.z());
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
        return fillColor(overlay);
    }

    private static int fillColor(BeaconOverlay overlay) {
        return overlay.source() == BeaconOverlaySource.MANUAL ? MANUAL_FILL : PLAN_FILL;
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
