package deanxbox.xaeros_beacon_addon.mixin.client;

import deanxbox.xaeros_beacon_addon.overlay.BeaconOverlayHighlighter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.highlight.HighlighterRegistry;

@Mixin(HighlighterRegistry.class)
public abstract class HighlighterRegistryMixin {
    @Inject(method = "end", at = @At("HEAD"))
    private void registerBeaconHighlighterBeforeFreeze(CallbackInfo ci) {
        HighlighterRegistry registry = (HighlighterRegistry) (Object) this;
        if (!registry.getHighlighters().contains(BeaconOverlayHighlighter.INSTANCE)) {
            registry.register(BeaconOverlayHighlighter.INSTANCE);
        }
    }
}
