package ru.rubin.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.rubin.event.EventManager;
import ru.rubin.event.player.EventRotation;

@Environment(EnvType.CLIENT)
@Mixin({Camera.class})
public abstract class CameraMixin {
   @Unique
   private EventRotation Rubin$rotationEvent;
   @Unique
   private float Rubin$originalYaw;
   @Unique
   private float Rubin$originalPitch;

   @Shadow
   protected abstract void setRotation(float var1, float var2);

   @Inject(
      method = {"update"},
      at = {@At("HEAD")}
   )
   private void onUpdateHead(World area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickProgress, CallbackInfo ci) {
      if (focusedEntity != null) {
         this.Rubin$originalYaw = focusedEntity.getYaw(tickProgress);
         this.Rubin$originalPitch = focusedEntity.getPitch(tickProgress);
         this.Rubin$rotationEvent = new EventRotation(this.Rubin$originalYaw, this.Rubin$originalPitch, tickProgress);
         EventManager.call(this.Rubin$rotationEvent);
      } else {
         this.Rubin$rotationEvent = null;
      }
   }

   @Redirect(
      method = {"update"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V"
      )
   )
   private void redirectSetRotation(Camera instance, float yaw, float pitch) {
      if (this.Rubin$rotationEvent == null
         || this.Rubin$rotationEvent.getYaw() == this.Rubin$originalYaw && this.Rubin$rotationEvent.getPitch() == this.Rubin$originalPitch) {
         this.setRotation(yaw, pitch);
      } else {
         this.setRotation(this.Rubin$rotationEvent.getYaw(), this.Rubin$rotationEvent.getPitch());
      }
   }

   @Inject(
      method = {"update"},
      at = {@At("RETURN")}
   )
   private void onUpdateReturn(CallbackInfo ci) {
      this.Rubin$rotationEvent = null;
   }

   @Inject(
      method = {"clipToSpace"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onClipToSpace(float desiredCameraDistance, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Float> cir) {
      if (ru.rubin.module.impl.misc.CameraClip.isActive()) {
         cir.setReturnValue(ru.rubin.module.impl.misc.CameraClip.getDistance());
      }
   }
}
