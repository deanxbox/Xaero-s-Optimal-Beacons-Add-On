package deanxbox.xaeros_beacon_addon.beacon;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class BeaconPlacementSolver {
    private static final double MINIMIZE_BEACONS_AREA_TARGET = 0.80D;

    private BeaconPlacementSolver() {
    }

    public static BeaconPlacementPlan solve(BlockArea area, BeaconTier tier) {
        return solve(area, tier, BeaconPlanPreference.FULL_AREA, BeaconPlanSnapMode.FREE);
    }

    public static BeaconPlacementPlan solve(BlockArea area, BeaconTier tier, BeaconPlanPreference preference) {
        return solve(area, tier, preference, BeaconPlanSnapMode.FREE);
    }

    public static BeaconPlacementPlan solve(BlockArea area, BeaconTier tier, BeaconPlanPreference preference, BeaconPlanSnapMode snapMode) {
        int diameter = tier.coverageDiameter();
        int minimumColumns = ceilDiv(area.width(), diameter);
        int minimumRows = ceilDiv(area.height(), diameter);

        return switch (preference) {
            case FULL_AREA -> solveFullArea(area, tier, snapMode, minimumColumns, minimumRows);
            case MINIMIZE_BEACONS -> solveMinimizeBeacons(area, tier, snapMode, minimumColumns, minimumRows);
            case HEX_OPTIMAL -> solveHexOptimal(area, tier, snapMode, minimumColumns, minimumRows);
        };
    }

    public static BeaconPlacementPlan translatePlan(BeaconPlacementPlan plan, int deltaX, int deltaZ) {
        if (plan.placements().isEmpty()) {
            return plan;
        }

        int snappedDeltaX = snapDelta(deltaX, plan.snapMode());
        int snappedDeltaZ = snapDelta(deltaZ, plan.snapMode());
        int clampedDeltaX = clampDeltaX(plan, snappedDeltaX);
        int clampedDeltaZ = clampDeltaZ(plan, snappedDeltaZ);

        if (clampedDeltaX == 0 && clampedDeltaZ == 0) {
            return plan;
        }

        List<BeaconPlacement> shiftedPlacements = new ArrayList<>(plan.placements().size());
        for (BeaconPlacement placement : plan.placements()) {
            shiftedPlacements.add(new BeaconPlacement(placement.x() + clampedDeltaX, placement.z() + clampedDeltaZ, placement.tier()));
        }

        return buildPlan(plan.targetArea(), plan.tier(), plan.preference(), plan.snapMode(), shiftedPlacements);
    }

    public static BeaconPlacementPlan prioritizeGapDirection(BeaconPlacementPlan plan, GapDirection direction) {
        if (plan.preference() == BeaconPlanPreference.FULL_AREA || plan.coverageRatio() >= 0.9999D) {
            return plan;
        }

        List<Integer> currentXCenters = sortedDistinctAxis(plan.placements(), true);
        List<Integer> currentZCenters = sortedDistinctAxis(plan.placements(), false);
        BeaconPlacementPlan candidate = switch (plan.preference()) {
            case MINIMIZE_BEACONS -> buildDirectionalGridPlan(plan, direction, currentXCenters, currentZCenters);
            case HEX_OPTIMAL -> buildDirectionalHexPlan(plan, direction, currentXCenters, currentZCenters);
            case FULL_AREA -> plan;
        };

        if (candidate.coverageRatio() + 0.0001D < plan.coverageRatio()) {
            return plan;
        }
        if (Math.abs(candidate.coverageRatio() - plan.coverageRatio()) < 0.0001D && overlapScore(candidate) > overlapScore(plan)) {
            return plan;
        }
        return candidate;
    }

    private static BeaconPlacementPlan buildDirectionalGridPlan(BeaconPlacementPlan plan, GapDirection direction, List<Integer> currentXCenters, List<Integer> currentZCenters) {
        boolean moveX = direction == GapDirection.LEFT || direction == GapDirection.RIGHT;
        boolean alignXToMax = direction == GapDirection.LEFT;
        boolean alignZToMax = direction == GapDirection.UP;
        List<Integer> xCenters = moveX
            ? distributeDirectionalCenters(plan.targetArea().minX(), plan.targetArea().maxX(), plan.tier().horizontalRadius(), currentXCenters.size(), plan.snapMode(), alignXToMax, plan.tier().coverageDiameter())
            : currentXCenters;
        List<Integer> zCenters = moveX
            ? currentZCenters
            : distributeDirectionalCenters(plan.targetArea().minZ(), plan.targetArea().maxZ(), plan.tier().horizontalRadius(), currentZCenters.size(), plan.snapMode(), alignZToMax, plan.tier().coverageDiameter());

        List<BeaconPlacement> placements = new ArrayList<>(xCenters.size() * zCenters.size());
        for (int x : xCenters) {
            for (int z : zCenters) {
                placements.add(new BeaconPlacement(x, z, plan.tier()));
            }
        }
        return buildPlan(plan.targetArea(), plan.tier(), plan.preference(), plan.snapMode(), placements);
    }

    private static BeaconPlacementPlan buildDirectionalHexPlan(BeaconPlacementPlan plan, GapDirection direction, List<Integer> currentXCenters, List<Integer> currentZCenters) {
        boolean moveX = direction == GapDirection.LEFT || direction == GapDirection.RIGHT;
        boolean alignXToMax = direction == GapDirection.LEFT;
        boolean alignZToMax = direction == GapDirection.UP;
        List<Integer> baseXCenters = moveX
            ? distributeDirectionalCenters(plan.targetArea().minX(), plan.targetArea().maxX(), plan.tier().horizontalRadius(), currentXCenters.size(), plan.snapMode(), alignXToMax, plan.tier().coverageDiameter())
            : currentXCenters;
        List<Integer> zCenters = moveX
            ? currentZCenters
            : distributeDirectionalCenters(plan.targetArea().minZ(), plan.targetArea().maxZ(), plan.tier().horizontalRadius(), currentZCenters.size(), BeaconPlanSnapMode.FREE, alignZToMax, plan.tier().coverageDiameter());
        List<BeaconPlacement> placements = new ArrayList<>();
        int horizontalShift = Math.max(1, plan.tier().coverageDiameter() / 2);

        for (int rowIndex = 0; rowIndex < zCenters.size(); rowIndex++) {
            List<Integer> rowXCenters = baseXCenters;
            if ((rowIndex & 1) == 1) {
                rowXCenters = staggeredCenters(baseXCenters, plan.targetArea().minX(), plan.targetArea().maxX(), plan.tier().horizontalRadius(), horizontalShift, plan.snapMode());
            }
            int z = zCenters.get(rowIndex);
            for (int x : rowXCenters) {
                placements.add(new BeaconPlacement(x, z, plan.tier()));
            }
        }
        return buildPlan(plan.targetArea(), plan.tier(), plan.preference(), plan.snapMode(), placements);
    }

    private static List<Integer> sortedDistinctAxis(List<BeaconPlacement> placements, boolean xAxis) {
        return placements.stream()
            .mapToInt(xAxis ? BeaconPlacement::x : BeaconPlacement::z)
            .distinct()
            .sorted()
            .boxed()
            .toList();
    }

    private static List<Integer> distributeDirectionalCenters(int min, int max, int radius, int count, BeaconPlanSnapMode snapMode, boolean alignToMax, int diameter) {
        if (count <= 0) {
            return List.of((min + max) / 2);
        }
        if (snapMode == BeaconPlanSnapMode.CHUNK_GRID) {
            List<Integer> validChunkCenters = validChunkCenters(min, max, radius);
            if (validChunkCenters.size() < count) {
                return distributeCenters(min, max, radius, count, snapMode);
            }
            if (alignToMax) {
                return List.copyOf(validChunkCenters.subList(validChunkCenters.size() - count, validChunkCenters.size()));
            }
            return List.copyOf(validChunkCenters.subList(0, count));
        }

        int firstCenter = min + radius;
        int lastCenter = max - radius;
        if (count == 1) {
            return List.of(alignToMax ? lastCenter : firstCenter);
        }

        int idealSpan = (count - 1) * diameter;
        int availableSpan = lastCenter - firstCenter;
        if (idealSpan > availableSpan) {
            return distributeCenters(min, max, radius, count);
        }

        int startCenter = alignToMax ? lastCenter - idealSpan : firstCenter;
        List<Integer> centers = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            centers.add(startCenter + index * diameter);
        }
        return centers;
    }

    private static BeaconPlacementPlan solveFullArea(BlockArea area, BeaconTier tier, BeaconPlanSnapMode snapMode, int minimumColumns, int minimumRows) {
        BeaconPlacementPlan bestPlan = buildGridPlan(area, tier, BeaconPlanPreference.FULL_AREA, snapMode, minimumColumns, minimumRows);
        if (bestPlan.coverageRatio() >= 0.9999D || snapMode == BeaconPlanSnapMode.FREE) {
            return bestPlan;
        }

        int maxColumns = maxGridCount(area.minX(), area.maxX(), tier.horizontalRadius(), snapMode);
        int maxRows = maxGridCount(area.minZ(), area.maxZ(), tier.horizontalRadius(), snapMode);

        for (int columns = minimumColumns; columns <= maxColumns; columns++) {
            for (int rows = minimumRows; rows <= maxRows; rows++) {
                BeaconPlacementPlan candidate = buildGridPlan(area, tier, BeaconPlanPreference.FULL_AREA, snapMode, columns, rows);
                if (candidate.coverageRatio() < 0.9999D) {
                    continue;
                }
                if (candidate.beaconCount() < bestPlan.beaconCount()) {
                    bestPlan = candidate;
                }
            }
        }

        if (bestPlan.coverageRatio() >= 0.9999D) {
            return bestPlan;
        }

        return buildGridPlan(area, tier, BeaconPlanPreference.FULL_AREA, BeaconPlanSnapMode.FREE, minimumColumns, minimumRows);
    }

    private static BeaconPlacementPlan solveMinimizeBeacons(BlockArea area, BeaconTier tier, BeaconPlanSnapMode snapMode, int fullColumns, int fullRows) {
        BeaconPlacementPlan bestPlan = null;
        int maxColumns = maxGridCount(area.minX(), area.maxX(), tier.horizontalRadius(), snapMode);
        int maxRows = maxGridCount(area.minZ(), area.maxZ(), tier.horizontalRadius(), snapMode);

        for (int columns = 1; columns <= Math.max(1, maxColumns); columns++) {
            for (int rows = 1; rows <= Math.max(1, maxRows); rows++) {
                BeaconPlacementPlan candidate = buildGridPlan(area, tier, BeaconPlanPreference.MINIMIZE_BEACONS, snapMode, columns, rows);
                if (candidate.coverageRatio() < MINIMIZE_BEACONS_AREA_TARGET) {
                    continue;
                }
                if (bestPlan == null
                    || candidate.beaconCount() < bestPlan.beaconCount()
                    || (candidate.beaconCount() == bestPlan.beaconCount() && candidate.coverageRatio() > bestPlan.coverageRatio())) {
                    bestPlan = candidate;
                }
            }
        }

        if (bestPlan != null) {
            return bestPlan;
        }

        return solveFullArea(area, tier, snapMode, fullColumns, fullRows);
    }

    private static BeaconPlacementPlan solveHexOptimal(BlockArea area, BeaconTier tier, BeaconPlanSnapMode snapMode, int minimumColumns, int minimumRows) {
        BeaconPlacementPlan baselinePlan = solveFullArea(area, tier, snapMode, minimumColumns, minimumRows);
        BeaconPlacementPlan bestPlan = baselinePlan;
        int beaconBudget = baselinePlan.beaconCount();
        int maxColumns = Math.min(beaconBudget, Math.max(minimumColumns + 2, maxGridCount(area.minX(), area.maxX(), tier.horizontalRadius(), snapMode)));
        int maxRows = Math.min(beaconBudget, Math.max(minimumRows + 2, maxGridCount(area.minZ(), area.maxZ(), tier.horizontalRadius(), BeaconPlanSnapMode.FREE)));

        for (int columns = Math.max(1, minimumColumns - 1); columns <= maxColumns; columns++) {
            for (int rows = Math.max(1, minimumRows - 1); rows <= maxRows; rows++) {
                BeaconPlacementPlan candidate = buildHexPlan(area, tier, snapMode, columns, rows);
                if (candidate.beaconCount() > beaconBudget) {
                    continue;
                }
                if (candidate.coverageRatio() > bestPlan.coverageRatio()
                    || (sameCoverage(candidate, bestPlan) && candidate.beaconCount() < bestPlan.beaconCount())
                    || (sameCoverage(candidate, bestPlan)
                    && candidate.beaconCount() == bestPlan.beaconCount()
                    && overlapScore(candidate) < overlapScore(bestPlan))) {
                    bestPlan = candidate;
                }
            }
        }

        return bestPlan.preference() == BeaconPlanPreference.HEX_OPTIMAL ? bestPlan : remapPreference(bestPlan, BeaconPlanPreference.HEX_OPTIMAL);
    }

    private static BeaconPlacementPlan remapPreference(BeaconPlacementPlan plan, BeaconPlanPreference preference) {
        return new BeaconPlacementPlan(plan.targetArea(), plan.tier(), preference, plan.snapMode(), plan.coverageRatio(), plan.placements());
    }

    private static BeaconPlacementPlan buildGridPlan(BlockArea area, BeaconTier tier, BeaconPlanPreference preference, BeaconPlanSnapMode snapMode, int columns, int rows) {
        List<Integer> xCenters = distributeCenters(area.minX(), area.maxX(), tier.horizontalRadius(), columns, snapMode);
        List<Integer> zCenters = distributeCenters(area.minZ(), area.maxZ(), tier.horizontalRadius(), rows, snapMode);
        List<BeaconPlacement> placements = new ArrayList<>(xCenters.size() * zCenters.size());
        for (int x : xCenters) {
            for (int z : zCenters) {
                placements.add(new BeaconPlacement(x, z, tier));
            }
        }
        return buildPlan(area, tier, preference, snapMode, placements);
    }

    private static BeaconPlacementPlan buildHexPlan(BlockArea area, BeaconTier tier, BeaconPlanSnapMode snapMode, int columns, int rows) {
        List<Integer> zCenters = distributeCenters(area.minZ(), area.maxZ(), tier.horizontalRadius(), rows, BeaconPlanSnapMode.FREE);
        List<BeaconPlacement> placements = new ArrayList<>();
        int horizontalShift = Math.max(1, tier.coverageDiameter() / 2);

        for (int rowIndex = 0; rowIndex < zCenters.size(); rowIndex++) {
            List<Integer> xCenters = distributeCenters(area.minX(), area.maxX(), tier.horizontalRadius(), columns, snapMode);
            if ((rowIndex & 1) == 1) {
                xCenters = staggeredCenters(xCenters, area.minX(), area.maxX(), tier.horizontalRadius(), horizontalShift, snapMode);
            }
            int z = zCenters.get(rowIndex);
            for (int x : xCenters) {
                placements.add(new BeaconPlacement(x, z, tier));
            }
        }

        return buildPlan(area, tier, BeaconPlanPreference.HEX_OPTIMAL, snapMode, placements);
    }

    private static BeaconPlacementPlan buildPlan(BlockArea area, BeaconTier tier, BeaconPlanPreference preference, BeaconPlanSnapMode snapMode, List<BeaconPlacement> placements) {
        double coverageRatio = calculateCoverageRatio(area, placements);
        return new BeaconPlacementPlan(area, tier, preference, snapMode, coverageRatio, List.copyOf(placements));
    }

    private static List<Integer> staggeredCenters(List<Integer> baseCenters, int min, int max, int radius, int shift, BeaconPlanSnapMode snapMode) {
        if (baseCenters.isEmpty()) {
            return baseCenters;
        }
        int minCenter = min + radius;
        int maxCenter = max - radius;
        Set<Integer> unique = new HashSet<>();
        List<Integer> staggered = new ArrayList<>(baseCenters.size());
        for (int center : baseCenters) {
            int shiftedCenter = Math.max(minCenter, Math.min(maxCenter, center + shift));
            if (snapMode == BeaconPlanSnapMode.CHUNK_GRID) {
                shiftedCenter = nearestChunkCenter(shiftedCenter, minCenter, maxCenter);
            }
            if (unique.add(shiftedCenter)) {
                staggered.add(shiftedCenter);
            }
        }
        if (staggered.size() != baseCenters.size()) {
            return baseCenters;
        }
        return staggered.stream().sorted().collect(Collectors.toList());
    }

    private static int nearestChunkCenter(int preferredCenter, int minCenter, int maxCenter) {
        int lower = ((preferredCenter - 8) / 16) * 16 + 8;
        int upper = lower + 16;
        int clampedLower = Math.max(minCenter, Math.min(maxCenter, lower));
        int clampedUpper = Math.max(minCenter, Math.min(maxCenter, upper));
        return Math.abs(preferredCenter - clampedLower) <= Math.abs(preferredCenter - clampedUpper) ? clampedLower : clampedUpper;
    }

    private static List<Integer> distributeCenters(int min, int max, int radius, int count, BeaconPlanSnapMode snapMode) {
        if (count <= 0) {
            return List.of((min + max) / 2);
        }
        return switch (snapMode) {
            case FREE -> distributeCenters(min, max, radius, count);
            case CHUNK_GRID -> distributeChunkCenters(min, max, radius, count);
        };
    }

    private static List<Integer> distributeCenters(int min, int max, int radius, int count) {
        List<Integer> centers = new ArrayList<>(count);
        int firstCenter = min + radius;
        int lastCenter = max - radius;

        if (count <= 1 || lastCenter <= firstCenter) {
            centers.add((min + max) / 2);
            return centers;
        }

        double span = lastCenter - firstCenter;
        for (int index = 0; index < count; index++) {
            double progress = (double) index / (count - 1);
            centers.add((int) Math.round(firstCenter + span * progress));
        }
        return centers;
    }

    private static List<Integer> distributeChunkCenters(int min, int max, int radius, int count) {
        List<Integer> validChunkCenters = validChunkCenters(min, max, radius);
        if (validChunkCenters.isEmpty() || validChunkCenters.size() < count) {
            return distributeCenters(min, max, radius, count);
        }
        if (count == 1) {
            return List.of(validChunkCenters.get(validChunkCenters.size() / 2));
        }

        List<Integer> centers = new ArrayList<>(count);
        int previousIndex = -1;
        for (int index = 0; index < count; index++) {
            int candidateIndex = (int) Math.round((double) index * (validChunkCenters.size() - 1) / (count - 1));
            candidateIndex = Math.max(candidateIndex, previousIndex + 1);
            int remainingSlots = count - index - 1;
            candidateIndex = Math.min(candidateIndex, validChunkCenters.size() - remainingSlots - 1);
            centers.add(validChunkCenters.get(candidateIndex));
            previousIndex = candidateIndex;
        }
        return centers;
    }

    private static List<Integer> validChunkCenters(int min, int max, int radius) {
        int firstCenter = min + radius;
        int lastCenter = max - radius;
        int firstChunkCenter = Math.floorDiv(firstCenter - 8 + 15, 16) * 16 + 8;
        List<Integer> chunkCenters = new ArrayList<>();
        for (int center = firstChunkCenter; center <= lastCenter; center += 16) {
            if (center >= firstCenter) {
                chunkCenters.add(center);
            }
        }
        return chunkCenters;
    }

    private static int maxGridCount(int min, int max, int radius, BeaconPlanSnapMode snapMode) {
        if (snapMode == BeaconPlanSnapMode.CHUNK_GRID) {
            return Math.max(1, validChunkCenters(min, max, radius).size());
        }
        return Math.max(1, ceilDiv(max - min + 1, radius * 2 + 1) + 2);
    }

    private static double calculateCoverageRatio(BlockArea area, List<BeaconPlacement> placements) {
        long coveredBlocks = 0L;
        for (int z = area.minZ(); z <= area.maxZ(); z++) {
            for (int x = area.minX(); x <= area.maxX(); x++) {
                if (coverageCount(placements, x, z) > 0) {
                    coveredBlocks++;
                }
            }
        }
        long totalArea = (long) area.width() * area.height();
        return totalArea == 0L ? 0.0D : (double) coveredBlocks / totalArea;
    }

    private static long overlapScore(BeaconPlacementPlan plan) {
        long overlap = 0L;
        BlockArea area = plan.targetArea();
        List<BeaconPlacement> placements = plan.placements();
        for (int z = area.minZ(); z <= area.maxZ(); z++) {
            for (int x = area.minX(); x <= area.maxX(); x++) {
                int coverage = coverageCount(placements, x, z);
                if (coverage > 1) {
                    overlap += coverage - 1L;
                }
            }
        }
        return overlap;
    }

    public static int coverageCount(List<BeaconPlacement> placements, int x, int z) {
        int count = 0;
        for (BeaconPlacement placement : placements) {
            int radius = placement.tier().horizontalRadius();
            if (x >= placement.x() - radius && x <= placement.x() + radius && z >= placement.z() - radius && z <= placement.z() + radius) {
                count++;
            }
        }
        return count;
    }

    private static int snapDelta(int delta, BeaconPlanSnapMode snapMode) {
        if (snapMode == BeaconPlanSnapMode.CHUNK_GRID) {
            return Math.round(delta / 16.0F) * 16;
        }
        return delta;
    }

    private static int clampDeltaX(BeaconPlacementPlan plan, int deltaX) {
        int minCenter = plan.targetArea().minX() + plan.tier().horizontalRadius();
        int maxCenter = plan.targetArea().maxX() - plan.tier().horizontalRadius();
        int minPlacementX = plan.placements().stream().mapToInt(BeaconPlacement::x).min().orElse(minCenter);
        int maxPlacementX = plan.placements().stream().mapToInt(BeaconPlacement::x).max().orElse(maxCenter);
        return Math.max(minCenter - minPlacementX, Math.min(maxCenter - maxPlacementX, deltaX));
    }

    private static int clampDeltaZ(BeaconPlacementPlan plan, int deltaZ) {
        int minCenter = plan.targetArea().minZ() + plan.tier().horizontalRadius();
        int maxCenter = plan.targetArea().maxZ() - plan.tier().horizontalRadius();
        int minPlacementZ = plan.placements().stream().mapToInt(BeaconPlacement::z).min().orElse(minCenter);
        int maxPlacementZ = plan.placements().stream().mapToInt(BeaconPlacement::z).max().orElse(maxCenter);
        return Math.max(minCenter - minPlacementZ, Math.min(maxCenter - maxPlacementZ, deltaZ));
    }

    private static boolean sameCoverage(BeaconPlacementPlan left, BeaconPlacementPlan right) {
        return Math.abs(left.coverageRatio() - right.coverageRatio()) < 0.0001D;
    }

    private static int ceilDiv(int dividend, int divisor) {
        return (dividend + divisor - 1) / divisor;
    }
}
