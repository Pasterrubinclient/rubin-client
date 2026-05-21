package ru.rubin.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.rubin.event.EventInit;
import ru.rubin.event.render.RenderEvent;
import ru.rubin.module.api.Category;
import ru.rubin.module.api.IModule;
import ru.rubin.module.api.Module;
import ru.rubin.module.api.setting.Setting;
import ru.rubin.module.api.setting.impl.SliderSetting;

@IModule(name = "Test", description = "", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class Test extends Module {
   public static SliderSetting a6 = new SliderSetting("Громкость", 5.0F, 1.0F, 100.0F, 1.0F, false);
   // public MP3Player player = new MP3Player();

   public Test() {
      this.addSettings(new Setting[] { a6 });
   }

   @Override
   public void onEnable() {
      super.onEnable();
   }

   @EventInit
   public void onRender(RenderEvent e) {
   }

   @Override
   public void onDisable() {
      super.onDisable();
   }
}
