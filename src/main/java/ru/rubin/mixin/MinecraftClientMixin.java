package ru.rubin.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.rubin.Rubin;
import ru.rubin.event.EventManager;
import ru.rubin.event.impl.EventChangeWorld;
import ru.rubin.event.lifecycle.ClientTickEvent;

@Environment(EnvType.CLIENT)
@Mixin({ MinecraftClient.class })
public abstract class MinecraftClientMixin {
   @Inject(method = { "tick" }, at = { @At("HEAD") })
   private void initRenderer(CallbackInfo ci) {
      if (Rubin.isModInitialized()) {
         Rubin.ensureRendererInitialized();
      }
   }

   @Inject(method = { "tick" }, at = { @At("TAIL") })
   private void publishClientTick(CallbackInfo ci) {
      if (Rubin.isModInitialized()) {
         MinecraftClient client = (MinecraftClient) (Object) this;
         if (!client.isPaused()) {
            ClientPlayerEntity player = client.player;
            ClientWorld world = client.world;
            if (player != null && world != null) {
               EventManager.call(new ClientTickEvent(client));
            }
         }
      }
   }

   @Inject(method = { "joinWorld" }, at = { @At("TAIL") })
   public void loadWorld(CallbackInfo ci) {
      EventManager.call(new EventChangeWorld());
   }
}
