package deanxbox.xaeros_beacon_addon.beacon;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class BeaconPlanExport {
    private static final Comparator<BeaconPlacement> PLACEMENT_ORDER = Comparator
        .comparingInt(BeaconPlacement::z)
        .thenComparingInt(BeaconPlacement::x);

    private BeaconPlanExport() {
    }

    public static List<String> numberedPlacementLines(BeaconPlacementPlan plan) {
        return numberedPlacementLines(plan, Integer.MAX_VALUE);
    }

    public static List<String> numberedPlacementLines(BeaconPlacementPlan plan, int maxLines) {
        List<BeaconPlacement> placements = sortedPlacements(plan);
        int lineCount = Math.min(maxLines, placements.size());
        List<String> lines = new ArrayList<>(lineCount);
        for (int index = 0; index < lineCount; index++) {
            BeaconPlacement placement = placements.get(index);
            lines.add(formatPlacementLine(index + 1, placement));
        }
        return lines;
    }

    public static String toClipboardText(BeaconPlacementPlan plan) {
        List<BeaconPlacement> placements = sortedPlacements(plan);
        StringBuilder builder = new StringBuilder(Math.max(128, placements.size() * 18));
        builder.append(plan.preference().displayName())
            .append(" | ")
            .append(plan.snapMode().displayName())
            .append(System.lineSeparator())
            .append("Tier ")
            .append(plan.tier().tier())
            .append(" | ")
            .append(plan.beaconCount())
            .append(" beacons | ")
            .append(String.format(Locale.ROOT, "%.1f%%", plan.coverageRatio() * 100.0D))
            .append(System.lineSeparator())
            .append("Blocks: ")
            .append(plan.totalPyramidBlocks())
            .append(" | ")
            .append(plan.stackBreakdown());

        for (int index = 0; index < placements.size(); index++) {
            builder.append(System.lineSeparator())
                .append(formatPlacementLine(index + 1, placements.get(index)));
        }
        return builder.toString();
    }

    public static List<BeaconPlacement> sortedPlacements(BeaconPlacementPlan plan) {
        ArrayList<BeaconPlacement> sorted = new ArrayList<>(plan.placements());
        sorted.sort(PLACEMENT_ORDER);
        return sorted;
    }

    private static String formatPlacementLine(int number, BeaconPlacement placement) {
        return "B" + number + ": " + placement.x() + ", " + placement.z();
    }
}
