package deanxbox.xaeros_beacon_addon.beacon;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class BeaconPlanExport {
    private BeaconPlanExport() {
    }

    public static List<String> numberedPlacementLines(BeaconPlacementPlan plan) {
        List<BeaconPlacement> placements = sortedPlacements(plan);
        List<String> lines = new ArrayList<>();
        for (int index = 0; index < placements.size(); index++) {
            BeaconPlacement placement = placements.get(index);
            lines.add("B" + (index + 1) + ": " + placement.x() + ", " + placement.z());
        }
        return lines;
    }

    public static String toClipboardText(BeaconPlacementPlan plan) {
        List<String> lines = new ArrayList<>();
        lines.add(plan.preference().displayName() + " | " + plan.snapMode().displayName());
        lines.add("Tier " + plan.tier().tier() + " | " + plan.beaconCount() + " beacons | " + String.format(Locale.ROOT, "%.1f%%", plan.coverageRatio() * 100.0D));
        lines.add("Blocks: " + plan.totalPyramidBlocks() + " | " + plan.stackBreakdown());
        lines.addAll(numberedPlacementLines(plan));
        return String.join(System.lineSeparator(), lines);
    }

    public static List<BeaconPlacement> sortedPlacements(BeaconPlacementPlan plan) {
        return plan.placements().stream()
            .sorted((left, right) -> {
                int zCompare = Integer.compare(left.z(), right.z());
                return zCompare != 0 ? zCompare : Integer.compare(left.x(), right.x());
            })
            .toList();
    }
}
