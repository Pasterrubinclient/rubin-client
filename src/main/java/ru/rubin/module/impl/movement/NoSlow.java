package ru.rubin.module.impl.movement;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Hand;
import ru.rubin.event.EventInit;
import ru.rubin.event.lifecycle.ClientTickEvent;
import ru.rubin.event.player.SlowWalkingEvent;
import ru.rubin.module.api.Category;
import ru.rubin.module.api.IModule;
import ru.rubin.module.api.Module;
import ru.rubin.module.api.setting.Setting;
import ru.rubin.module.api.setting.impl.ModeSetting;

@IModule(
   name = "No Slow",
   description = "",
   category = Category.Movement,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class NoSlow extends Module {
   public static ModeSetting mode = new ModeSetting("Режим", "Spookytime", "Spookytime", "GrimTick", "SpookytimeV2");
   private float ticks = 0.0F;

   public NoSlow() {
      this.addSettings(new Setting[]{mode});
   }

   @EventInit
   public void onTick(ClientTickEvent event) {
      if (mc.player != null) {
         if (mode.is("GrimTick") || mode.is("SpookytimeV2")) {
            if (mc.player.isUsingItem()) {
               this.ticks++;
            } else {
               this.ticks = 0.0F;
            }
         }
      }
   }

   @EventInit
   public void onSlowWalking(SlowWalkingEvent event) {
      Hand first = mc.player.getActiveHand();
      Hand second = first.equals(Hand.MAIN_HAND) ? Hand.OFF_HAND : Hand.MAIN_HAND;
      if (mode.is("Spookytime")) {
         if (mc.player.getActiveHand() == Hand.MAIN_HAND) {
            mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
         } else {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
         }

         event.cancel();
      }

      if (mode.is("SpookytimeV2") && mc.player != null && mc.player.isUsingItem() && !mc.player.hasVehicle() && this.ticks >= 1.3F) {
         event.cancel();
         this.ticks = 0.26F;
      }

      if (mode.is("GrimTick") && mc.player != null && mc.player.isUsingItem() && !mc.player.hasVehicle() && this.ticks >= 1.2F) {
         event.cancel();
         this.ticks = 0.0F;
      }
   }
}
