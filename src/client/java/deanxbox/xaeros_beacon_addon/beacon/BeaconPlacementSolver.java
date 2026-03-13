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
        BeaconPlacementPlan bestPlan = null;
        int maxColumns = Math.max(minimumColumns + 2, maxGridCount(area.minX(), area.maxX(), tier.horizontalRadius(), snapMode));
        int maxRows = Math.max(minimumRows + 2, maxGridCount(area.minZ(), area.maxZ(), tier.horizontalRadius(), BeaconPlanSnapMode.FREE));

        for (int columns = Math.max(1, minimumColumns - 1); columns <= maxColumns; columns++) {
            for (int rows = Math.max(1, minimumRows); rows <= maxRows; rows++) {
                BeaconPlacementPlan candidate = buildHexPlan(area, tier, snapMode, columns, rows);
                if (bestPlan == null
                    || candidate.coverageRatio() > bestPlan.coverageRatio()
                    || (sameCoverage(candidate, bestPlan) && candidate.beaconCount() < bestPlan.beaconCount())) {
                    bestPlan = candidate;
                }
            }
        }

        return bestPlan != null ? bestPlan : solveFullArea(area, tier, snapMode, minimumColumns, minimumRows);
    }

    private static boolean sameCoverage(BeaconPlacementPlan left, BeaconPlacementPlan right) {
        return Math.abs(left.coverageRatio() - right.coverageRatio()) < 0.0001D;
    }

    private static BeaconPlacementPlan buildGridPlan(
        BlockArea area,
        BeaconTier tier,
        BeaconPlanPreference preference,
        BeaconPlanSnapMode snapMode,
        int columns,
        int rows
    ) {
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

    private static BeaconPlacementPlan buildPlan(
        BlockArea area,
        BeaconTier tier,
        BeaconPlanPreference preference,
        BeaconPlanSnapMode snapMode,
        List<BeaconPlacement> placements
    ) {
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
        List<Integer> staggered = new ArrayList<>();
        for (int center : baseCenters) {
            int shiftedCenter = Math.max(minCenter, Math.min(maxCenter, center + shift));
            if (snapMode == BeaconPlanSnapMode.CHUNK_GRID) {
                shiftedCenter = nearestChunkCenter(shiftedCenter, minCenter, maxCenter);
            }
            if (unique.add(shiftedCenter)) {
                staggered.add(shiftedCenter);
            }
        }
        if (!staggered.isEmpty() && staggered.get(0) - radius > min && unique.add(minCenter)) {
            staggered.add(0, minCenter);
        }
        if (!staggered.isEmpty() && staggered.get(staggered.size() - 1) + radius < max && unique.add(maxCenter)) {
            staggered.add(maxCenter);
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

    private static int ceilDiv(int dividend, int divisor) {
        return (dividend + divisor - 1) / divisor;
    }
}
