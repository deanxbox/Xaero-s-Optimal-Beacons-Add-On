package deanxbox.xaeros_beacon_addon.beacon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BeaconPlacementSolverTest {
    @Test
    void fullAreaCoversNegativeCoordinates() {
        BlockArea area = new BlockArea(-64, -48, 96, 80);

        BeaconPlacementPlan plan = BeaconPlacementSolver.solve(
            area,
            BeaconTier.FOUR,
            BeaconPlanPreference.FULL_AREA,
            BeaconPlanSnapMode.FREE
        );

        assertEquals(1.0D, plan.coverageRatio(), 0.0001D);
        assertTrue(BeaconPlacementSolver.coverageCount(plan.placements(), area.minX(), area.minZ()) > 0);
        assertTrue(BeaconPlacementSolver.coverageCount(plan.placements(), area.maxX(), area.maxZ()) > 0);
    }

    @Test
    void chunkGridKeepsChunkCenteredPlacements() {
        BlockArea area = new BlockArea(-42, -42, 154, 154);

        BeaconPlacementPlan plan = BeaconPlacementSolver.solve(
            area,
            BeaconTier.FOUR,
            BeaconPlanPreference.FULL_AREA,
            BeaconPlanSnapMode.CHUNK_GRID
        );

        assertEquals(BeaconPlanSnapMode.CHUNK_GRID, plan.snapMode());
        assertEquals(1.0D, plan.coverageRatio(), 0.0001D);
        assertTrue(plan.placements().stream().allMatch(placement ->
            Math.floorMod(placement.x() - 8, 16) == 0
                && Math.floorMod(placement.z() - 8, 16) == 0
        ));
    }

    @Test
    void minimizeBeaconsAcceptsTheDocumentedEightyPercentTarget() {
        BlockArea area = new BlockArea(0, 0, 109, 109);

        BeaconPlacementPlan fullPlan = BeaconPlacementSolver.solve(
            area,
            BeaconTier.FOUR,
            BeaconPlanPreference.FULL_AREA,
            BeaconPlanSnapMode.FREE
        );
        BeaconPlacementPlan minimizedPlan = BeaconPlacementSolver.solve(
            area,
            BeaconTier.FOUR,
            BeaconPlanPreference.MINIMIZE_BEACONS,
            BeaconPlanSnapMode.FREE
        );

        assertEquals(4, fullPlan.beaconCount());
        assertEquals(1, minimizedPlan.beaconCount());
        assertTrue(minimizedPlan.coverageRatio() >= 0.80D);
        assertTrue(minimizedPlan.coverageRatio() < 1.0D);
    }

    @Test
    void selectionSmallerThanBeaconCoverageNeedsOneBeacon() {
        BeaconPlacementPlan plan = BeaconPlacementSolver.solve(
            new BlockArea(-5, -5, 5, 5),
            BeaconTier.ONE,
            BeaconPlanPreference.FULL_AREA,
            BeaconPlanSnapMode.FREE
        );

        assertEquals(1, plan.beaconCount());
        assertEquals(1.0D, plan.coverageRatio(), 0.0001D);
    }
}
