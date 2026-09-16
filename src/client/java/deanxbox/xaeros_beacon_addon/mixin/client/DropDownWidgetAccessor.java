package deanxbox.xaeros_beacon_addon.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.lib.client.gui.widget.dropdown.DropDownWidget;

@Mixin(DropDownWidget.class)
public interface DropDownWidgetAccessor {
    @Accessor("selected")
    void setSelected(int selected);
}
