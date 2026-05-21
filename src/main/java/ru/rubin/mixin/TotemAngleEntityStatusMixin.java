package ru.rubin.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.rubin.Rubin;
import ru.rubin.module.impl.visuals.TotemAngle;

@Environment(EnvType.CLIENT)
@Mixin(ClientPlayNetworkHandler.class)
public class TotemAngleEntityStatusMixin {
    @Inject(method = "onEntityStatus", at = @At("HEAD"))
    private void rubin$onEntityStatus(EntityStatusS2CPacket packet, CallbackInfo ci) {
        if (!Rubin.isModInitialized() || Rubin.get == null || Rubin.get.manager == null) return;
        TotemAngle module = (TotemAngle) Rubin.get.manager.getModule(TotemAngle.class);
        if (module != null && module.enable) {
            module.onEntityStatusPacket(packet);
        }
    }
}
