package deanxbox.xaeros_beacon_addon.integration;

import deanxbox.xaeros_beacon_addon.beacon.BeaconPlanPreference;
import deanxbox.xaeros_beacon_addon.beacon.BeaconPlanSnapMode;
import deanxbox.xaeros_beacon_addon.beacon.BeaconTier;
import deanxbox.xaeros_beacon_addon.config.BeaconClientConfig;
import deanxbox.xaeros_beacon_addon.overlay.BeaconOverlayState;
import deanxbox.xaeros_beacon_addon.overlay.MinimapCompat;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class BeaconConfigScreen {
    private BeaconConfigScreen() {
    }

    public static Screen create(Screen parent) {
        BeaconClientConfig config = BeaconClientConfig.get();
        BeaconOverlayState state = BeaconOverlayState.getInstance();

        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.literal("Xaero's Optimal Beacons"))
            .setSavingRunnable(() -> {
                config.save();
                state.invalidateHighlights();
                MinimapCompat.syncCurrentWorld();
            });

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));
        ConfigCategory colors = builder.getOrCreateCategory(Component.literal("Colors"));

        general.addEntry(entryBuilder.startEnumSelector(Component.literal("Default Planning Tier"), BeaconTier.class, state.getSelectedPlanTier())
            .setDefaultValue(BeaconTier.max())
            .setSaveConsumer(state::setSelectedPlanTier)
            .build());
        general.addEntry(entryBuilder.startEnumSelector(Component.literal("Default Manual Beacon Tier"), BeaconTier.class, state.getDefaultManualTier())
            .setDefaultValue(BeaconTier.max())
            .setSaveConsumer(state::setDefaultManualTier)
            .build());
        general.addEntry(entryBuilder.startEnumSelector(Component.literal("Default Plan Mode"), BeaconPlanPreference.class, state.getSelectedPlanPreference())
            .setDefaultValue(BeaconPlanPreference.FULL_AREA)
            .setSaveConsumer(state::setSelectedPlanPreference)
            .build());
        general.addEntry(entryBuilder.startEnumSelector(Component.literal("Default Planning Grid"), BeaconPlanSnapMode.class, state.getSelectedPlanSnapMode())
            .setDefaultValue(BeaconPlanSnapMode.FREE)
            .setSaveConsumer(state::setSelectedPlanSnapMode)
            .build());
        general.addEntry(entryBuilder.startBooleanToggle(Component.literal("Show Plan Labels"), config.ui.showPlanLabels)
            .setDefaultValue(true)
            .setSaveConsumer(value -> config.ui.showPlanLabels = value)
            .build());
        general.addEntry(entryBuilder.startBooleanToggle(Component.literal("Show Overlap Heatmap"), config.ui.showHeatmap)
            .setDefaultValue(true)
            .setSaveConsumer(value -> config.ui.showHeatmap = value)
            .build());
        general.addEntry(entryBuilder.startBooleanToggle(Component.literal("Enable Gap Direction Controls"), config.ui.enableDirectionalGapAdjustment)
            .setDefaultValue(true)
            .setSaveConsumer(value -> config.ui.enableDirectionalGapAdjustment = value)
            .build());

        addColorEntry(colors, entryBuilder, "Manual Fill", config.colors.manualFill, value -> config.colors.manualFill = value);
        addColorEntry(colors, entryBuilder, "Manual Inner Border", config.colors.manualInnerBorder, value -> config.colors.manualInnerBorder = value);
        addColorEntry(colors, entryBuilder, "Manual Border", config.colors.manualBorder, value -> config.colors.manualBorder = value);
        addColorEntry(colors, entryBuilder, "Manual Ring", config.colors.manualRing, value -> config.colors.manualRing = value);
        addColorEntry(colors, entryBuilder, "Manual Core", config.colors.manualCore, value -> config.colors.manualCore = value);
        addColorEntry(colors, entryBuilder, "Covered", config.colors.planCovered, value -> config.colors.planCovered = value);
        addColorEntry(colors, entryBuilder, "Overlap", config.colors.planOverlap, value -> config.colors.planOverlap = value);
        addColorEntry(colors, entryBuilder, "Heavy Overlap", config.colors.planHeavyOverlap, value -> config.colors.planHeavyOverlap = value);
        addColorEntry(colors, entryBuilder, "Uncovered", config.colors.planUncovered, value -> config.colors.planUncovered = value);
        addColorEntry(colors, entryBuilder, "Plan Inner Border", config.colors.planInnerBorder, value -> config.colors.planInnerBorder = value);
        addColorEntry(colors, entryBuilder, "Plan Border", config.colors.planBorder, value -> config.colors.planBorder = value);
        addColorEntry(colors, entryBuilder, "Plan Ring", config.colors.planRing, value -> config.colors.planRing = value);
        addColorEntry(colors, entryBuilder, "Plan Core", config.colors.planCore, value -> config.colors.planCore = value);

        return builder.build();
    }

    private static void addColorEntry(ConfigCategory category, ConfigEntryBuilder entryBuilder, String label, int value, java.util.function.Consumer<Integer> saveConsumer) {
        category.addEntry(entryBuilder.startAlphaColorField(Component.literal(label), value)
            .setDefaultValue(value)
            .setSaveConsumer(saveConsumer)
            .build());
    }
}
