package ru.rubin.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.rubin.Rubin;
import ru.rubin.module.impl.visuals.beautifulhands.BeautifulHands;
import ru.rubin.module.impl.visuals.beautifulhands.BeautifulHandsRenderer;

@Environment(EnvType.CLIENT)
@Mixin(HeldItemRenderer.class)
public class BeautifulHandsMixin {

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void rubin$renderBeautifulHands(
            AbstractClientPlayerEntity player,
            float tickProgress,
            float pitch,
            Hand hand,
            float swingProgress,
            ItemStack item,
            float equipProgress,
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            int light,
            CallbackInfo ci
    ) {
        if (!Rubin.isModInitialized() || Rubin.get == null || Rubin.get.manager == null) return;

        BeautifulHands module = (BeautifulHands) Rubin.get.manager.getModule(BeautifulHands.class);
        if (module == null || !module.enable || player == null) return;

        BeautifulHandsRenderer renderer = BeautifulHandsRenderer.getInstance();
        renderer.updateDelta();
        renderer.onNewSwing(swingProgress);

        Arm side = hand == Hand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        renderer.render(
                player,
                tickProgress,
                hand,
                swingProgress,
                item,
                equipProgress,
                matrices,
                queue,
                light,
                side,
                (HeldItemRenderer) (Object) this
        );
        ci.cancel();
    }
}
