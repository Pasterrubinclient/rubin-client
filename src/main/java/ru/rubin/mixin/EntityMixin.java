package ru.rubin.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.rubin.Rubin;
import ru.rubin.module.impl.combat.HitBox;
import ru.rubin.module.impl.player.NoPush;

@Environment(EnvType.CLIENT)
@Mixin({ Entity.class })
public abstract class EntityMixin {
   @Inject(method = { "getTargetingMargin" }, at = { @At("RETURN") }, cancellable = true)
   private void client$getTargetingMargin(CallbackInfoReturnable<Float> cir) {
      Entity self = (Entity) (Object) this;
      if (self instanceof PlayerEntity) {
         if (Rubin.get.manager.get(HitBox.class).enable) {
            if (!(HitBox.ignFr.get() && self instanceof PlayerEntity player)
                  || !Rubin.get.friendManager.isFriend(player.getName().getString())) {
               float base = (Float) cir.getReturnValue();
               float extra = HitBox.expand.get();
               cir.setReturnValue(base + extra);
            }
         }
      }
   }

   @Inject(method = { "pushAwayFrom" }, at = { @At("HEAD") }, cancellable = true)
   private void onPushAwayFrom(Entity entity, CallbackInfo ci) {
      Entity self = (Entity) (Object) this;
      if (self instanceof ClientPlayerEntity) {
         if (Rubin.get != null && Rubin.get.manager != null) {
            NoPush noPush = (NoPush) Rubin.get.manager.getModule(NoPush.class);
            if (noPush != null && noPush.enable) {
               if (entity instanceof PlayerEntity && noPush.players.get()) {
                  ci.cancel();
               } else if (entity instanceof LivingEntity && !(entity instanceof PlayerEntity) && noPush.mobs.get()) {
                  ci.cancel();
               }
            }
         }
      }
   }
}
