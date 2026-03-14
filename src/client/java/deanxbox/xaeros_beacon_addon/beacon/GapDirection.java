package deanxbox.xaeros_beacon_addon.beacon;

public enum GapDirection {
    UP("Move Gaps Up"),
    DOWN("Move Gaps Down"),
    LEFT("Move Gaps Left"),
    RIGHT("Move Gaps Right");

    private final String menuLabel;

    GapDirection(String menuLabel) {
        this.menuLabel = menuLabel;
    }

    public String menuLabel() {
        return menuLabel;
    }
}
