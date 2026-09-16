package deanxbox.xaeros_beacon_addon.menu;

import deanxbox.xaeros_beacon_addon.beacon.BeaconTier;
import deanxbox.xaeros_beacon_addon.overlay.BeaconOverlay;
import deanxbox.xaeros_beacon_addon.overlay.BeaconOverlayState;
import java.util.ArrayList;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

/**
 * Synthetic right-click submenu listing the available beacon tiers.
 * Opened in place of the flat "Set Beacon Tier N" options when the user
 * picks "Set Beacon Tier" from a beacon's right-click menu.
 */
public class BeaconTierMenu implements IRightClickableElement {
    private final BeaconOverlay beacon;
    private final BeaconOverlayState state;

    public BeaconTierMenu(BeaconOverlay beacon, BeaconOverlayState state) {
        this.beacon = beacon;
        this.state = state;
    }

    @Override
    public ArrayList<RightClickOption> getRightClickOptions() {
        ArrayList<RightClickOption> options = new ArrayList<>();
        int index = 0;
        for (BeaconTier tier : BeaconTier.values()) {
            BeaconTier selectedTier = tier;
            options.add(new BeaconRightClickOption("Tier " + tier.tier(), index++, this, screen ->
                state.updateManualBeaconTier(beacon, selectedTier)
            ));
        }
        return options;
    }

    @Override
    public boolean isRightClickValid() {
        return true;
    }
}
