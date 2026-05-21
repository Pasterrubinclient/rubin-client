package ru.rubin.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.rubin.Rubin;
import ru.rubin.event.EventInit;
import ru.rubin.event.impl.EventUpdate;
import ru.rubin.module.api.Category;
import ru.rubin.module.api.IModule;
import ru.rubin.module.api.Module;
import ru.rubin.module.api.setting.Setting;
import ru.rubin.module.api.setting.impl.BooleanSetting;
import ru.rubin.module.api.setting.impl.ModeSetting;

@IModule(
   name = "RTX Sounds",
   description = " ",
   category = Category.Visuals,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class RTXSounds extends Module {
   public static ModeSetting performancePriority = new ModeSetting("Качество звука", "Производительность", "Производительность", "Качество");
   public static BooleanSetting stereo = new BooleanSetting("3д стерео", true);
   public static BooleanSetting tone = new BooleanSetting("Тон", true);

   public RTXSounds() {
      this.addSettings(new Setting[]{performancePriority, stereo, tone});
   }

   @Override
   public void onEnable() {
      super.onEnable();
      if (mc.player != null) {
         Rubin.rtx.updateMixer();
      }
   }

   @EventInit
   public void onUpdate(EventUpdate e) {
      if (Rubin.rtx != null) {
         Rubin.rtx.updateMixer();
      }
   }

   @Override
   public void onDisable() {
      super.onDisable();
      Rubin.rtx.setState(false);
   }
}
