package deanxbox.xaeros_beacon_addon.beacon;

public enum BeaconPlanPreference {
    FULL_AREA("Full Coverage"),
    MINIMIZE_BEACONS("Minimize Beacons"),
    HEX_OPTIMAL("Hex-Optimal");

    private final String displayName;

    BeaconPlanPreference(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public BeaconPlanPreference next() {
        BeaconPlanPreference[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
