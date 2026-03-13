package deanxbox.xaeros_beacon_addon;

import deanxbox.xaeros_beacon_addon.overlay.BeaconMinimapSync;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class XaerosOptimalBeaconsAddOnClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> BeaconMinimapSync.syncCurrentWorld());
		XaerosOptimalBeaconsAddOn.LOGGER.info("Xaero beacon helper client initialized.");
	}
}
