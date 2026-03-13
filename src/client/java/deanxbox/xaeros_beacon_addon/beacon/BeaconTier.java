package deanxbox.xaeros_beacon_addon.beacon;

public enum BeaconTier {
    ONE(1, 20),
    TWO(2, 30),
    THREE(3, 40),
    FOUR(4, 50);

    private final int tier;
    private final int horizontalRadius;

    BeaconTier(int tier, int horizontalRadius) {
        this.tier = tier;
        this.horizontalRadius = horizontalRadius;
    }

    public int tier() {
        return tier;
    }

    public int horizontalRadius() {
        return horizontalRadius;
    }

    public int coverageDiameter() {
        return horizontalRadius * 2 + 1;
    }

    public static BeaconTier max() {
        return FOUR;
    }
}
