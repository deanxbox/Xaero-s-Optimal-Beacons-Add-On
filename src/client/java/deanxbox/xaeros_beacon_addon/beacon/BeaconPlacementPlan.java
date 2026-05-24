package deanxbox.xaeros_beacon_addon.beacon;

import java.util.List;
import java.util.Locale;

public record BeaconPlacementPlan(
    BlockArea targetArea,
    BeaconTier tier,
    BeaconPlanPreference preference,
    BeaconPlanSnapMode snapMode,
    double coverageRatio,
    List<BeaconPlacement> placements
) {
    public int beaconCount() {
        return placements.size();
    }

    public int pyramidBlocksPerBeacon() {
        return switch (tier) {
            case ONE -> 9;
            case TWO -> 34;
            case THREE -> 83;
            case FOUR -> 164;
        };
    }

    public int totalPyramidBlocks() {
        return beaconCount() * pyramidBlocksPerBeacon();
    }

    public int fullStacks() {
        return totalPyramidBlocks() / 64;
    }

    public int remainingBlocks() {
        return totalPyramidBlocks() % 64;
    }

    public String stackBreakdown() {
        int fullStacks = fullStacks();
        int remainder = remainingBlocks();
        if (fullStacks == 0) {
            return remainder + (remainder == 1 ? " block" : " blocks");
        }
        String stackText = fullStacks + (fullStacks == 1 ? " stack" : " stacks");
        if (remainder == 0) {
            return stackText;
        }
        return stackText + " + " + remainder;
    }

    public String coveragePercent() {
        return String.format(Locale.ROOT, "%.1f%%", coverageRatio * 100.0D);
    }
}
