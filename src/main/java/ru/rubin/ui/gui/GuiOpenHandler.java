package ru.rubin.ui.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.rubin.event.EventInit;
import ru.rubin.event.input.KeyInputEvent;

@Environment(EnvType.CLIENT)
public class GuiOpenHandler {
   @EventInit
   public void onKeyInput(KeyInputEvent event) {
   }
}
