package ru.rubin.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.rubin.event.EventManager;
import ru.rubin.event.input.KeepSprintEvent;
import ru.rubin.util.other.IMinecraft;

@Environment(EnvType.CLIENT)
@Mixin({PlayerEntity.class})
public abstract class PlayerEntityMixin implements IMinecraft {
   @Inject(
      method = {"attack"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/player/PlayerEntity;setSprinting(Z)V",
         shift = Shift.AFTER
      )}
   )
   public void attackHook(CallbackInfo callbackInfo) {
      EventManager.call(new KeepSprintEvent());
   }
}
