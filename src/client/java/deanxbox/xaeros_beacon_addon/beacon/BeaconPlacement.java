package deanxbox.xaeros_beacon_addon.beacon;

public record BeaconPlacement(int x, int z, BeaconTier tier) {
    public BlockArea coverage() {
        int radius = tier.horizontalRadius();
        return new BlockArea(x - radius, z - radius, x + radius, z + radius);
    }
}
