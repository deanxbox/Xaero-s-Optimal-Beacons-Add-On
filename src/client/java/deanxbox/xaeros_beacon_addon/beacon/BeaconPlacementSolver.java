package deanxbox.xaeros_beacon_addon.beacon;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
        };
    }

    private static BeaconPlacementPlan solveFullArea(BlockArea area, BeaconTier tier, BeaconPlanSnapMode snapMode, int minimumColumns, int minimumRows) {
        BeaconPlacementPlan bestPlan = buildPlan(area, tier, BeaconPlanPreference.FULL_AREA, snapMode, minimumColumns, minimumRows);
        if (bestPlan.coverageRatio() >= 0.9999D || snapMode == BeaconPlanSnapMode.FREE) {
            return bestPlan;
        }

        int maxColumns = maxGridCount(area.minX(), area.maxX(), tier.horizontalRadius());
        int maxRows = maxGridCount(area.minZ(), area.maxZ(), tier.horizontalRadius());

        for (int columns = minimumColumns; columns <= maxColumns; columns++) {
            for (int rows = minimumRows; rows <= maxRows; rows++) {
                BeaconPlacementPlan candidate = buildPlan(area, tier, BeaconPlanPreference.FULL_AREA, snapMode, columns, rows);
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

        return buildPlan(area, tier, BeaconPlanPreference.FULL_AREA, BeaconPlanSnapMode.FREE, minimumColumns, minimumRows);
    }

    private static BeaconPlacementPlan solveMinimizeBeacons(BlockArea area, BeaconTier tier, BeaconPlanSnapMode snapMode, int fullColumns, int fullRows) {
        BeaconPlacementPlan bestPlan = null;
        int maxColumns = snapMode == BeaconPlanSnapMode.CHUNK_GRID ? maxGridCount(area.minX(), area.maxX(), tier.horizontalRadius()) : fullColumns;
        int maxRows = snapMode == BeaconPlanSnapMode.CHUNK_GRID ? maxGridCount(area.minZ(), area.maxZ(), tier.horizontalRadius()) : fullRows;

        for (int columns = 1; columns <= Math.max(1, maxColumns); columns++) {
            for (int rows = 1; rows <= Math.max(1, maxRows); rows++) {
                BeaconPlacementPlan candidate = buildPlan(area, tier, BeaconPlanPreference.MINIMIZE_BEACONS, snapMode, columns, rows);
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

    private static BeaconPlacementPlan buildPlan(
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

        double coverageRatio = calculateCoverageRatio(area, tier.horizontalRadius(), xCenters, zCenters);
        return new BeaconPlacementPlan(area, tier, preference, snapMode, coverageRatio, List.copyOf(placements));
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

    private static int maxGridCount(int min, int max, int radius) {
        return Math.max(1, validChunkCenters(min, max, radius).size());
    }

    private static double calculateCoverageRatio(BlockArea area, int radius, List<Integer> xCenters, List<Integer> zCenters) {
        long coveredX = coveredAxisLength(area.minX(), area.maxX(), radius, xCenters);
        long coveredZ = coveredAxisLength(area.minZ(), area.maxZ(), radius, zCenters);
        long totalArea = (long) area.width() * area.height();
        return totalArea == 0L ? 0.0D : (double) (coveredX * coveredZ) / totalArea;
    }

    private static long coveredAxisLength(int min, int max, int radius, List<Integer> centers) {
        List<Integer> sortedCenters = centers.stream().sorted().collect(Collectors.toList());
        long covered = 0L;
        int currentStart = Integer.MIN_VALUE;
        int currentEnd = Integer.MIN_VALUE;

        for (int center : sortedCenters) {
            int intervalStart = Math.max(min, center - radius);
            int intervalEnd = Math.min(max, center + radius);
            if (intervalEnd < intervalStart) {
                continue;
            }
            if (currentStart == Integer.MIN_VALUE) {
                currentStart = intervalStart;
                currentEnd = intervalEnd;
                continue;
            }
            if (intervalStart > currentEnd + 1) {
                covered += currentEnd - currentStart + 1L;
                currentStart = intervalStart;
                currentEnd = intervalEnd;
            } else {
                currentEnd = Math.max(currentEnd, intervalEnd);
            }
        }

        if (currentStart != Integer.MIN_VALUE) {
            covered += currentEnd - currentStart + 1L;
        }
        return covered;
    }

    private static int ceilDiv(int dividend, int divisor) {
        return (dividend + divisor - 1) / divisor;
    }
}
