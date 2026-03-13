package deanxbox.xaeros_beacon_addon.beacon;

public record BlockArea(int minX, int minZ, int maxX, int maxZ) {
    public BlockArea {
        if (minX > maxX) {
            throw new IllegalArgumentException("minX must be <= maxX");
        }
        if (minZ > maxZ) {
            throw new IllegalArgumentException("minZ must be <= maxZ");
        }
    }

    public int width() {
        return maxX - minX + 1;
    }

    public int height() {
        return maxZ - minZ + 1;
    }

    public boolean containsBlock(int x, int z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    public boolean intersectsChunk(int chunkX, int chunkZ) {
        int chunkMinX = chunkX << 4;
        int chunkMaxX = chunkMinX + 15;
        int chunkMinZ = chunkZ << 4;
        int chunkMaxZ = chunkMinZ + 15;
        return chunkMaxX >= minX && chunkMinX <= maxX && chunkMaxZ >= minZ && chunkMinZ <= maxZ;
    }

    public boolean intersectsRegion(int regionX, int regionZ) {
        int regionMinX = regionX << 9;
        int regionMaxX = regionMinX + 511;
        int regionMinZ = regionZ << 9;
        int regionMaxZ = regionMinZ + 511;
        return regionMaxX >= minX && regionMinX <= maxX && regionMaxZ >= minZ && regionMinZ <= maxZ;
    }
}
