package deanxbox.xaeros_beacon_addon.beacon;

public enum BeaconPlanSnapMode {
    FREE("Free Placement"),
    CHUNK_GRID("Chunk Grid");

    private final String displayName;

    BeaconPlanSnapMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
