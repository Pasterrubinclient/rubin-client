package ru.rubin.ui.gui.component.main;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.rubin.Rubin;
import ru.rubin.ui.gui.GuiScreen;

@Environment(EnvType.CLIENT)
public class GuiMouseReleased extends GuiScreen {
   public static void mouseReleased() {
      if ((GuiScreen.activeSliderSetting != null || GuiScreen.pickingSaturationBrightness || GuiScreen.pickingHue) && Rubin.get.configManager != null) {
         Rubin.get.configManager.autoSave();
      }

      GuiScreen.pickingSaturationBrightness = false;
      GuiScreen.pickingHue = false;
      GuiScreen.pickingAlpha = false;
      GuiScreen.activeSliderSetting = null;
      GuiScreen.sliderX = 0.0F;
      GuiScreen.sliderY = 0.0F;
      GuiScreen.sliderWidth = 0.0F;
   }
}
