package ru.rubin.module.impl.movement;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.rubin.event.EventInit;
import ru.rubin.event.impl.EventUpdate;
import ru.rubin.module.api.Category;
import ru.rubin.module.api.IModule;
import ru.rubin.module.api.Module;
import ru.rubin.util.other.Mathf;
import ru.rubin.util.player.MoveUtil;
import ru.rubin.util.player.PlayerUtil;

@IModule(
   name = "No Web",
   description = "",
   category = Category.Movement,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class NoWeb extends Module {
   @EventInit
   public void onEvent(EventUpdate e) {
      if (!PlayerUtil.nullCheck() && PlayerUtil.isPlayerInWeb()) {
         double[] speed = MoveUtil.calculateDirection(Mathf.random(0.62F, 0.64F));
         mc.player.setVelocity(speed[0], mc.options.jumpKey.isPressed() ? 1.2 : (mc.options.sneakKey.isPressed() ? -2.0 : 0.0), speed[1]);
      }
   }
}
