package deanxbox.xaeros_beacon_addon.mixin.client;

import deanxbox.xaeros_beacon_addon.beacon.BeaconPlanExport;
import deanxbox.xaeros_beacon_addon.beacon.BeaconPlanPreference;
import deanxbox.xaeros_beacon_addon.beacon.BeaconPlanSnapMode;
import deanxbox.xaeros_beacon_addon.beacon.BeaconPlacement;
import deanxbox.xaeros_beacon_addon.beacon.BeaconPlacementPlan;
import deanxbox.xaeros_beacon_addon.beacon.BeaconPlacementSolver;
import deanxbox.xaeros_beacon_addon.beacon.BeaconTier;
import deanxbox.xaeros_beacon_addon.beacon.BlockArea;
import deanxbox.xaeros_beacon_addon.menu.BeaconRightClickOption;
import deanxbox.xaeros_beacon_addon.overlay.BeaconMinimapSync;
import deanxbox.xaeros_beacon_addon.overlay.BeaconOverlay;
import deanxbox.xaeros_beacon_addon.overlay.BeaconOverlaySource;
import deanxbox.xaeros_beacon_addon.overlay.BeaconOverlayState;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.gui.GuiMap;
import xaero.map.gui.MapTileSelection;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

@Mixin(GuiMap.class)
public abstract class GuiMapMixin {
    private static final double MIN_LABEL_PIXELS_PER_BLOCK = 0.75D;

    @Shadow
    private int rightClickX;

    @Shadow
    private int rightClickZ;

    @Shadow
    private ResourceKey<Level> rightClickDim;

    @Shadow
    private MapTileSelection mapTileSelection;

    @Shadow
    private double cameraX;

    @Shadow
    private double cameraZ;

    @Shadow
    private double scale;

    @Shadow
    private ResourceKey<Level> lastViewedDimensionId;

    @Inject(method = "getRightClickOptions", at = @At("RETURN"))
    private void addBeaconOptions(CallbackInfoReturnable<ArrayList<RightClickOption>> cir) {
        ResourceKey<Level> dimension = rightClickDim;
        if (dimension == null) {
            return;
        }

        BeaconOverlayState state = BeaconOverlayState.getInstance();
        ArrayList<RightClickOption> options = cir.getReturnValue();
        GuiMap guiMap = (GuiMap) (Object) this;
        int nextIndex = options.size();

        options.add(new BeaconRightClickOption("Place Beacon", nextIndex++, guiMap, screen ->
            state.placeManualBeacon(dimension, rightClickX, rightClickZ, BeaconTier.max())
        ));

        BeaconOverlay clickedOverlay = state.findOverlayAt(dimension, rightClickX, rightClickZ);
        BeaconOverlay clickedBeacon = state.findManualBeaconAt(dimension, rightClickX, rightClickZ);
        if (clickedBeacon != null) {
            for (BeaconTier tier : BeaconTier.values()) {
                BeaconTier selectedTier = tier;
                options.add(new BeaconRightClickOption("Set Beacon Tier " + tier.tier(), nextIndex++, guiMap, screen ->
                    state.updateManualBeaconTier(clickedBeacon, selectedTier)
                ));
            }
            options.add(new BeaconRightClickOption("Remove Beacon Preview", nextIndex++, guiMap, screen ->
                state.removeManualBeacon(clickedBeacon)
            ));
        }

        if (hasDraggedSelection()) {
            BlockArea selectedArea = selectedArea();
            BeaconTier selectedPlanTier = state.getSelectedPlanTier();
            BeaconPlanSnapMode selectedSnapMode = state.getSelectedPlanSnapMode();
            BeaconPlanPreference selectedPreference = state.getSelectedPlanPreference();
            BeaconPlacementPlan plannedLayout = BeaconPlacementSolver.solve(selectedArea, selectedPlanTier, selectedPreference, selectedSnapMode);

            options.add(inactiveInfoOption("Planner: " + selectedPreference.displayName() + " | Tier " + selectedPlanTier.tier(), nextIndex++, guiMap));
            options.add(inactiveInfoOption("Grid: " + selectedSnapMode.displayName(), nextIndex++, guiMap));
            options.add(new BeaconRightClickOption("Cycle Planning Mode", nextIndex++, guiMap, screen ->
                state.setSelectedPlanPreference(selectedPreference.next())
            ));
            options.add(new BeaconRightClickOption("Cycle Planning Tier", nextIndex++, guiMap, screen ->
                state.setSelectedPlanTier(nextTier(selectedPlanTier))
            ));
            options.add(new BeaconRightClickOption("Toggle Planning Grid", nextIndex++, guiMap, screen ->
                state.setSelectedPlanSnapMode(nextSnapMode(selectedSnapMode))
            ));
            options.add(new BeaconRightClickOption(planActionLabel(plannedLayout), nextIndex++, guiMap, screen ->
                state.setPlan(dimension, plannedLayout)
            ));
        }

        BeaconPlacementPlan currentPlan = state.getPlan(dimension);
        if (currentPlan != null && clickedOverlay != null && clickedOverlay.source() == BeaconOverlaySource.PLAN) {
            options.add(new BeaconRightClickOption(togglePlanLabel(currentPlan), nextIndex++, guiMap, screen ->
                state.setPlan(dimension, toggledPlan(currentPlan))
            ));
            options.add(new BeaconRightClickOption("Set Temporary Waypoints For Plan", nextIndex++, guiMap, screen ->
                BeaconMinimapSync.createPlanTemporaryWaypoints()
            ));
            options.add(new BeaconRightClickOption("Copy Plan Coordinates", nextIndex++, guiMap, screen ->
                copyPlanToClipboard(currentPlan)
            ));
            options.add(new BeaconRightClickOption("Send Plan Coordinates To Chat", nextIndex++, guiMap, screen ->
                sendPlanToChat(currentPlan)
            ));
            options.add(inactiveInfoOption(planSummary(currentPlan), nextIndex++, guiMap));
            options.add(inactiveInfoOption(blockSummary(currentPlan), nextIndex++, guiMap));
        }

        if (state.hasManualBeacons(dimension)) {
            options.add(new BeaconRightClickOption("Clear Beacon Previews", nextIndex++, guiMap, screen ->
                state.clearManualBeacons(dimension)
            ));
        }

        if (state.hasPlan(dimension)) {
            options.add(new BeaconRightClickOption("Clear Placement Plan", nextIndex++, guiMap, screen ->
                state.clearPlan()
            ));
        }
    }

    @Inject(method = "renderPreDropdown", at = @At("TAIL"))
    private void renderBeaconUi(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ResourceKey<Level> dimension = lastViewedDimensionId;
        if (dimension == null) {
            return;
        }

        BeaconPlacementPlan plan = BeaconOverlayState.getInstance().getPlan(dimension);
        if (plan == null) {
            return;
        }

        renderPlanStatsPanel(guiGraphics, plan);
        renderPlanLabels(guiGraphics, plan);
    }

    private void renderPlanStatsPanel(GuiGraphics guiGraphics, BeaconPlacementPlan plan) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        List<String> lines = new ArrayList<>();
        lines.add("Beacon Plan");
        lines.add(plan.preference().displayName() + " | " + plan.snapMode().displayName());
        lines.add("Tier " + plan.tier().tier() + " | Coverage " + coverageText(plan));
        lines.add(plan.beaconCount() + " beacons | " + plan.totalPyramidBlocks() + " blocks");
        lines.add(plan.stackBreakdown());

        List<String> placementLines = BeaconPlanExport.numberedPlacementLines(plan);
        int visiblePlacements = Math.min(placementLines.size(), 6);
        for (int index = 0; index < visiblePlacements; index++) {
            lines.add(placementLines.get(index));
        }
        if (placementLines.size() > visiblePlacements) {
            lines.add("+" + (placementLines.size() - visiblePlacements) + " more placements");
        }

        int width = 0;
        for (String line : lines) {
            width = Math.max(width, font.width(line));
        }

        int x = 8;
        int y = 8;
        int lineHeight = 10;
        int panelWidth = width + 12;
        int panelHeight = lines.size() * lineHeight + 8;
        guiGraphics.fill(x, y, x + panelWidth, y + panelHeight, 0xB0161A21);
        guiGraphics.fill(x, y, x + panelWidth, y + 1, 0xFF7FD8F6);
        guiGraphics.fill(x, y + panelHeight - 1, x + panelWidth, y + panelHeight, 0xFF7FD8F6);
        guiGraphics.fill(x, y, x + 1, y + panelHeight, 0xFF7FD8F6);
        guiGraphics.fill(x + panelWidth - 1, y, x + panelWidth, y + panelHeight, 0xFF7FD8F6);

        int lineY = y + 4;
        for (int index = 0; index < lines.size(); index++) {
            int color = index == 0 ? 0xFFF7FBFF : 0xFFDCE7EF;
            guiGraphics.drawString(font, lines.get(index), x + 6, lineY, color, false);
            lineY += lineHeight;
        }
    }

    private void renderPlanLabels(GuiGraphics guiGraphics, BeaconPlacementPlan plan) {
        if (scale <= 0.0D || 1.0D / scale < MIN_LABEL_PIXELS_PER_BLOCK) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        List<BeaconPlacement> placements = BeaconPlanExport.sortedPlacements(plan);

        for (int index = 0; index < placements.size(); index++) {
            BeaconPlacement placement = placements.get(index);
            String label = Integer.toString(index + 1);
            int labelWidth = font.width(label);
            int centerX = (int) Math.round(screenWidth / 2.0D + (placement.x() - cameraX) / scale);
            int centerY = (int) Math.round(screenHeight / 2.0D + (placement.z() - cameraZ) / scale);
            int boxLeft = centerX + 6;
            int boxTop = centerY - 6;
            int boxRight = boxLeft + labelWidth + 4;
            int boxBottom = boxTop + 9;

            if (boxRight < 0 || boxLeft > screenWidth || boxBottom < 0 || boxTop > screenHeight) {
                continue;
            }

            guiGraphics.fill(boxLeft, boxTop, boxRight, boxBottom, 0xC814171B);
            guiGraphics.drawString(font, label, boxLeft + 2, boxTop + 1, 0xFFFFF4C2, false);
        }
    }

    private BeaconRightClickOption inactiveInfoOption(String label, int index, GuiMap guiMap) {
        return (BeaconRightClickOption) new BeaconRightClickOption(label, index, guiMap, screen -> { }).setActive(false);
    }

    private void copyPlanToClipboard(BeaconPlacementPlan plan) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.keyboardHandler.setClipboard(BeaconPlanExport.toClipboardText(plan));
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.literal("Copied beacon plan to clipboard."), false);
        }
    }

    private void sendPlanToChat(BeaconPlacementPlan plan) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        minecraft.player.displayClientMessage(Component.literal(plan.preference().displayName() + " | " + plan.snapMode().displayName()), false);
        minecraft.player.displayClientMessage(Component.literal("Tier " + plan.tier().tier() + " | " + plan.beaconCount() + " beacons | " + coverageText(plan)), false);
        for (String line : BeaconPlanExport.numberedPlacementLines(plan)) {
            minecraft.player.displayClientMessage(Component.literal(line), false);
        }
    }

    private String planActionLabel(BeaconPlacementPlan plan) {
        return "Create " + plan.preference().displayName() + " Plan | " + plan.beaconCount() + " beacons | " + coverageText(plan);
    }

    private String togglePlanLabel(BeaconPlacementPlan plan) {
        return "Switch Plan To " + plan.preference().next().displayName();
    }

    private BeaconPlacementPlan toggledPlan(BeaconPlacementPlan plan) {
        return BeaconPlacementSolver.solve(plan.targetArea(), plan.tier(), plan.preference().next(), plan.snapMode());
    }

    private String planSummary(BeaconPlacementPlan plan) {
        return plan.preference().displayName() + " | " + plan.snapMode().displayName() + " | " + plan.beaconCount() + " beacons";
    }

    private String blockSummary(BeaconPlacementPlan plan) {
        return coverageText(plan) + " coverage | " + plan.totalPyramidBlocks() + " beacon blocks | " + plan.stackBreakdown();
    }

    private String coverageText(BeaconPlacementPlan plan) {
        return String.format(Locale.ROOT, "%.1f pct", plan.coverageRatio() * 100.0D);
    }

    private BeaconTier nextTier(BeaconTier currentTier) {
        BeaconTier[] tiers = BeaconTier.values();
        return tiers[(currentTier.ordinal() + 1) % tiers.length];
    }

    private BeaconPlanSnapMode nextSnapMode(BeaconPlanSnapMode currentMode) {
        return currentMode == BeaconPlanSnapMode.FREE ? BeaconPlanSnapMode.CHUNK_GRID : BeaconPlanSnapMode.FREE;
    }

    private boolean hasDraggedSelection() {
        return mapTileSelection != null
            && (mapTileSelection.getStartX() != mapTileSelection.getEndX()
            || mapTileSelection.getStartZ() != mapTileSelection.getEndZ());
    }

    private BlockArea selectedArea() {
        int minX = mapTileSelection.getLeft() << 4;
        int maxX = ((mapTileSelection.getRight() + 1) << 4) - 1;
        int minZ = mapTileSelection.getTop() << 4;
        int maxZ = ((mapTileSelection.getBottom() + 1) << 4) - 1;
        return new BlockArea(minX, minZ, maxX, maxZ);
    }
}
