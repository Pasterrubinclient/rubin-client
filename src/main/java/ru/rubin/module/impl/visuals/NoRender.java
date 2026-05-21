package ru.rubin.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.effect.StatusEffects;
import ru.rubin.event.EventInit;
import ru.rubin.event.impl.EventUpdate;
import ru.rubin.module.api.Category;
import ru.rubin.module.api.IModule;
import ru.rubin.module.api.Module;
import ru.rubin.module.api.setting.Setting;

@IModule(
   name = "No Render",
   description = " ",
   category = Category.Visuals,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class NoRender extends Module {
   public NoRender() {
      this.addSettings(new Setting[0]);
   }

   @EventInit
   public void onUpdate(EventUpdate e) {
      if (mc.player != null) {
         if (mc.player.hasStatusEffect(StatusEffects.DARKNESS)) {
            mc.player.removeStatusEffect(StatusEffects.DARKNESS);
         }
      }
   }
}
