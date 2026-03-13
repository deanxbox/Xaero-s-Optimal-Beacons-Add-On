package deanxbox.xaeros_beacon_addon.beacon;

public enum BeaconPlanPreference {
    FULL_AREA("Full Coverage"),
    MINIMIZE_BEACONS("Minimize Beacons");

    private final String displayName;

    BeaconPlanPreference(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
