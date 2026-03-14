package deanxbox.xaeros_beacon_addon.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import deanxbox.xaeros_beacon_addon.XaerosOptimalBeaconsAddOn;
import deanxbox.xaeros_beacon_addon.beacon.BeaconPlanPreference;
import deanxbox.xaeros_beacon_addon.beacon.BeaconPlanSnapMode;
import deanxbox.xaeros_beacon_addon.beacon.BeaconTier;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public final class BeaconClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("xaeros-optimal-beacons.json");
    private static BeaconClientConfig instance;

    public PlannerSettings planner = new PlannerSettings();
    public UiSettings ui = new UiSettings();
    public OverlayColors colors = new OverlayColors();

    private BeaconClientConfig() {
    }

    public static synchronized BeaconClientConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public synchronized void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException exception) {
            XaerosOptimalBeaconsAddOn.LOGGER.error("Failed to save beacon config.", exception);
        }
    }

    private static BeaconClientConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                BeaconClientConfig loaded = GSON.fromJson(reader, BeaconClientConfig.class);
                if (loaded != null) {
                    loaded.sanitize();
                    return loaded;
                }
            } catch (IOException | RuntimeException exception) {
                XaerosOptimalBeaconsAddOn.LOGGER.error("Failed to load beacon config.", exception);
            }
        }
        BeaconClientConfig created = new BeaconClientConfig();
        created.sanitize();
        created.save();
        return created;
    }

    private void sanitize() {
        if (planner == null) {
            planner = new PlannerSettings();
        }
        if (planner.selectedTier == null) {
            planner.selectedTier = BeaconTier.max();
        }
        if (planner.defaultManualTier == null) {
            planner.defaultManualTier = BeaconTier.max();
        }
        if (planner.selectedSnapMode == null) {
            planner.selectedSnapMode = BeaconPlanSnapMode.FREE;
        }
        if (planner.selectedPreference == null) {
            planner.selectedPreference = BeaconPlanPreference.FULL_AREA;
        }
        if (ui == null) {
            ui = new UiSettings();
        }
        if (colors == null) {
            colors = new OverlayColors();
        }
    }

    public static final class PlannerSettings {
        public BeaconTier selectedTier = BeaconTier.max();
        public BeaconTier defaultManualTier = BeaconTier.max();
        public BeaconPlanSnapMode selectedSnapMode = BeaconPlanSnapMode.FREE;
        public BeaconPlanPreference selectedPreference = BeaconPlanPreference.FULL_AREA;
    }

    public static final class UiSettings {
        public boolean showPlanLabels = true;
        public boolean showHeatmap = true;
        public boolean enableDirectionalGapAdjustment = true;
    }

    public static final class OverlayColors {
        public int manualFill = rgba(244, 197, 66, 24);
        public int manualInnerBorder = rgba(255, 223, 115, 96);
        public int manualBorder = rgba(255, 239, 181, 178);
        public int manualRing = rgba(36, 45, 58, 245);
        public int manualCore = rgba(255, 88, 51, 255);
        public int planCovered = rgba(63, 168, 214, 34);
        public int planOverlap = rgba(255, 223, 77, 88);
        public int planHeavyOverlap = rgba(212, 48, 48, 168);
        public int planUncovered = rgba(156, 92, 255, 118);
        public int planInnerBorder = rgba(122, 201, 235, 78);
        public int planBorder = rgba(196, 238, 250, 160);
        public int planRing = rgba(26, 34, 46, 245);
        public int planCore = rgba(255, 40, 40, 255);
    }

    private static int rgba(int red, int green, int blue, int alpha) {
        return (red << 24) | (green << 16) | (blue << 8) | alpha;
    }
}


