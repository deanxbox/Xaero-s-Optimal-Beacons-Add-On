package deanxbox.xaeros_beacon_addon.overlay;

import deanxbox.xaeros_beacon_addon.beacon.BeaconTier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record BeaconOverlay(ResourceKey<Level> dimension, int x, int z, BeaconTier tier, BeaconOverlaySource source) {
    public int minX() {
        return x - tier.horizontalRadius();
    }

    public int maxX() {
        return x + tier.horizontalRadius();
    }

    public int minZ() {
        return z - tier.horizontalRadius();
    }

    public int maxZ() {
        return z + tier.horizontalRadius();
    }

    public boolean containsBlock(int blockX, int blockZ) {
        return blockX >= minX() && blockX <= maxX() && blockZ >= minZ() && blockZ <= maxZ();
    }

    public boolean intersectsChunk(int chunkX, int chunkZ) {
        int chunkMinX = chunkX << 4;
        int chunkMaxX = chunkMinX + 15;
        int chunkMinZ = chunkZ << 4;
        int chunkMaxZ = chunkMinZ + 15;
        return chunkMaxX >= minX() && chunkMinX <= maxX() && chunkMaxZ >= minZ() && chunkMinZ <= maxZ();
    }

    public boolean intersectsRegion(int regionX, int regionZ) {
        int regionMinX = regionX << 9;
        int regionMaxX = regionMinX + 511;
        int regionMinZ = regionZ << 9;
        int regionMaxZ = regionMinZ + 511;
        return regionMaxX >= minX() && regionMinX <= maxX() && regionMaxZ >= minZ() && regionMinZ <= maxZ();
    }

    public boolean isBorderBlock(int blockX, int blockZ) {
        return containsBlock(blockX, blockZ) && distanceToBorder(blockX, blockZ) == 0;
    }

    public boolean isInnerBorderBlock(int blockX, int blockZ) {
        return containsBlock(blockX, blockZ) && distanceToBorder(blockX, blockZ) == 1;
    }

    public boolean isCenterMarkerCore(int blockX, int blockZ) {
        int dx = Math.abs(blockX - x);
        int dz = Math.abs(blockZ - z);
        return dx <= 1 && dz <= 1;
    }

    public boolean isCenterMarkerInnerRing(int blockX, int blockZ) {
        int dx = Math.abs(blockX - x);
        int dz = Math.abs(blockZ - z);
        return Math.max(dx, dz) == 2 && (dx == 0 || dz == 0 || dx == dz);
    }

    public boolean isCenterMarkerOuterRing(int blockX, int blockZ) {
        int dx = Math.abs(blockX - x);
        int dz = Math.abs(blockZ - z);
        return Math.max(dx, dz) == 3 && (dx == 0 || dz == 0 || dx == dz);
    }

    public boolean isPreviewPillar(int blockX, int blockZ) {
        int dx = Math.abs(blockX - x);
        int dz = Math.abs(blockZ - z);
        return dx <= 1 && dz <= 4;
    }

    public boolean isPreviewTop(int blockX, int blockZ) {
        int dx = Math.abs(blockX - x);
        int dz = blockZ - z;
        return dz >= -6 && dz <= -4 && dx <= 2 - Math.max(0, -5 - dz);
    }

    public boolean isPreviewBase(int blockX, int blockZ) {
        int dx = Math.abs(blockX - x);
        int dz = blockZ - z;
        return dz >= 2 && dz <= 4 && dx <= 3 - (dz - 2);
    }

    private int distanceToBorder(int blockX, int blockZ) {
        int distanceX = Math.min(blockX - minX(), maxX() - blockX);
        int distanceZ = Math.min(blockZ - minZ(), maxZ() - blockZ);
        return Math.min(distanceX, distanceZ);
    }
}
