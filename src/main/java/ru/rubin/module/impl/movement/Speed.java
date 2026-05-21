package ru.rubin.module.impl.movement;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import ru.rubin.event.EventInit;
import ru.rubin.event.impl.EventUpdate;
import ru.rubin.module.api.Category;
import ru.rubin.module.api.IModule;
import ru.rubin.module.api.Module;
import ru.rubin.module.api.setting.Setting;
import ru.rubin.module.api.setting.impl.ModeSetting;
import ru.rubin.module.impl.combat.HitAura;
import ru.rubin.util.player.PlayerUtil;

@IModule(
   name = "Speed",
   description = "",
   category = Category.Movement,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class Speed extends Module {
   public static ModeSetting mode = new ModeSetting("Режим", "Ares-Entity", "Ares-Entity", "Grim-Entity");

   public Speed() {
      this.addSettings(new Setting[]{mode});
   }

   @EventInit
   public void onUpdate(EventUpdate e) {
      if (!PlayerUtil.nullCheck()) {
         if (mode.is("Grim-Entity")) {
            if (mc.player == null || mc.world == null) {
               return;
            }

            double finalSpeed = 6.0E-4F;
            if (finalSpeed <= 0.0) {
               return;
            }

            Entity nearest = null;
            double bestSq = Double.MAX_VALUE;
            double maxRangeSq = 0.2F;

            for (Entity ent : mc.world.getEntities()) {
               if (ent != mc.player) {
                  if (ent == HitAura.target) {
                     double dx = ent.getX() - mc.player.getX();
                     double dz = ent.getZ() - mc.player.getZ();
                     double sq = dx * dx + dz * dz;
                     if (sq <= maxRangeSq && sq < bestSq) {
                        bestSq = sq;
                        nearest = ent;
                     }
                  }

                  if (nearest != null) {
                     double[] dir = this.getDirectionToPoint(new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ()), new Vec3d(nearest.getX(), nearest.getY(), nearest.getZ()), finalSpeed);
                     mc.player.addVelocity(dir[0], 0.0, dir[1]);
                  }
               }
            }
         }
      }
   }

   private double[] getDirectionToPoint(Vec3d from, Vec3d to, double spd) {
      double dx = to.x - from.x;
      double dz = to.z - from.z;
      double len = Math.sqrt(dx * dx + dz * dz);
      return len == 0.0 ? new double[]{0.0, 0.0} : new double[]{dx / len * spd, dz / len * spd};
   }
}
