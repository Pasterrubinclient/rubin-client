package ru.rubin.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.rubin.Rubin;
import ru.rubin.module.impl.visuals.Chams;

@Environment(EnvType.CLIENT)
@Mixin(LivingEntityRenderer.class)
public class ChamsMixin {

    @Inject(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/LivingEntityRenderState;FF)V",
            at = @At("HEAD")
    )
    private void rubin$preRender(MatrixStack matrices, OrderedRenderCommandQueue queue, int light,
                                 LivingEntityRenderState state, float limbAngle, float limbDistance,
                                 CallbackInfo ci) {
        if (!Rubin.isModInitialized() || Rubin.get == null || Rubin.get.manager == null) return;
        Chams chams = (Chams) Rubin.get.manager.getModule(Chams.class);
        if (chams == null || !chams.enable) return;
        if (!Chams.isThroughWalls()) return;

        // Check by entity ID from state
        RenderSystem.disableDepthTest();
    }

    @Inject(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/LivingEntityRenderState;FF)V",
            at = @At("RETURN")
    )
    private void rubin$postRender(MatrixStack matrices, OrderedRenderCommandQueue queue, int light,
                                  LivingEntityRenderState state, float limbAngle, float limbDistance,
                                  CallbackInfo ci) {
        if (!Rubin.isModInitialized() || Rubin.get == null || Rubin.get.manager == null) return;
        Chams chams = (Chams) Rubin.get.manager.getModule(Chams.class);
        if (chams == null || !chams.enable) return;
        if (!Chams.isThroughWalls()) return;

        RenderSystem.enableDepthTest();
    }
}
