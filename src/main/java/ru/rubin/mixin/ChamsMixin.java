package ru.rubin.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.rubin.Rubin;
import ru.rubin.module.impl.visuals.Chams;

@Environment(EnvType.CLIENT)
@Mixin(LivingEntityRenderer.class)
public class ChamsMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void rubin$preRender(LivingEntityRenderState state, MatrixStack matrices,
                                 OrderedRenderCommandQueue queue, CameraRenderState cameraRenderState,
                                 CallbackInfo ci) {
        if (!Rubin.isModInitialized() || Rubin.get == null || Rubin.get.manager == null) return;
        Chams chams = (Chams) Rubin.get.manager.getModule(Chams.class);
        if (chams == null || !chams.enable) return;
        if (!Chams.isThroughWalls()) return;
        GL11.glDisable(2929);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void rubin$postRender(LivingEntityRenderState state, MatrixStack matrices,
                                  OrderedRenderCommandQueue queue, CameraRenderState cameraRenderState,
                                  CallbackInfo ci) {
        if (!Rubin.isModInitialized() || Rubin.get == null || Rubin.get.manager == null) return;
        Chams chams = (Chams) Rubin.get.manager.getModule(Chams.class);
        if (chams == null || !chams.enable) return;
        if (!Chams.isThroughWalls()) return;
        GL11.glEnable(2929);
    }
}
