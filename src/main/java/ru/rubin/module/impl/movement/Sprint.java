package ru.rubin.module.impl.movement;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.rubin.event.EventInit;
import ru.rubin.event.impl.EventUpdate;
import ru.rubin.event.input.KeepSprintEvent;
import ru.rubin.module.api.Category;
import ru.rubin.module.api.IModule;
import ru.rubin.module.api.Module;
import ru.rubin.module.api.setting.Setting;
import ru.rubin.module.api.setting.impl.BooleanSetting;
import ru.rubin.module.api.setting.impl.SliderSetting;
import ru.rubin.module.impl.combat.HitAura;
import ru.rubin.module.impl.combat.auraProcess.Attack;

@IModule(
   name = "Sprint",
   description = "",
   category = Category.Movement,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class Sprint extends Module {
   public static BooleanSetting keepSprint = new BooleanSetting("Сохранять спринт", false);
   public static SliderSetting keepSprintSpeed = new SliderSetting("Сила сохранения", 0.6F, 0.2F, 1.0F, 0.1F, false).hidden(() -> !keepSprint.get());
   public static int tick = 0;

   public Sprint() {
      this.addSettings(new Setting[]{keepSprint, keepSprintSpeed});
   }

   @EventInit
   public void onSprint(KeepSprintEvent e) {
      if (keepSprint.get()) {
         mc.player
            .setVelocity(
               mc.player.getVelocity().x / keepSprintSpeed.get(),
               mc.player.getVelocity().y,
               mc.player.getVelocity().z / keepSprintSpeed.get()
            );
         mc.player.setSprinting(true);
      }
   }

   @EventInit
   public void onUpdate(EventUpdate event) {
      if (mc.player != null && mc.world != null) {
         boolean horizontal = mc.player.horizontalCollision && !mc.player.collidedSoftly;
         if (tick != 0) {
            mc.player.setSprinting(false);
            mc.options.sprintKey.setPressed(false);
            tick--;
         } else {
            if (Attack.resetSprintTick(HitAura.target, HitAura.getRanges())) {
               mc.player.setSprinting(false);
               mc.options.sprintKey.setPressed(false);
            }

            if (!mc.player.isSneaking() && !horizontal && mc.options.forwardKey.isPressed()) {
               mc.player.setSprinting(true);
               mc.options.sprintKey.setPressed(true);
            }
         }
      }
   }

   @Override
   public void onDisable() {
      super.onDisable();
      if (mc.player != null) {
         mc.player.setSprinting(false);
      }
   }
}
