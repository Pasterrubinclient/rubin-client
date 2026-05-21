package ru.rubin.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.rubin.Rubin;
import ru.rubin.module.impl.visuals.WavyCapes;

@Environment(EnvType.CLIENT)
@Mixin(CapeFeatureRenderer.class)
public class WavyCapesMixin {

    @Inject(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/PlayerEntityRenderState;FF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V"
            )
    )
    private void rubin$applyWavyCape(
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            int light,
            PlayerEntityRenderState state,
            float limbAngle,
            float limbDistance,
            CallbackInfo ci
    ) {
        if (!Rubin.isModInitialized() || Rubin.get == null || Rubin.get.manager == null) return;
        WavyCapes module = (WavyCapes) Rubin.get.manager.getModule(WavyCapes.class);
        if (module == null || !module.shouldAnimate(state)) return;

        float time = (System.currentTimeMillis() * 0.001F * module.getSpeed()) + (state.id * 0.31F);
        float wavePrimary = MathHelper.sin(time);
        float waveSecondary = MathHelper.cos(time * 0.72F + 1.12F);

        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(wavePrimary * module.getPitchAmplitude()));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(waveSecondary * module.getRollAmplitude()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(wavePrimary * module.getYawAmplitude()));
        matrices.translate(0.0F, MathHelper.sin(time * 0.9F) * module.getVerticalBobbing(), 0.0F);
    }
}
