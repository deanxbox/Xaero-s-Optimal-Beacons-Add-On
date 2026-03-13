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
}
