package deanxbox.xaeros_beacon_addon.menu;

import java.util.function.Consumer;
import net.minecraft.client.gui.screens.Screen;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

public class BeaconRightClickOption extends RightClickOption {
    private final Consumer<Screen> action;

    public BeaconRightClickOption(String name, int index, IRightClickableElement target, Consumer<Screen> action) {
        super(name, index, target);
        this.action = action;
    }

    @Override
    public void onAction(Screen screen) {
        action.accept(screen);
    }
}
