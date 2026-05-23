package ru.rubin.mixin;

import com.google.common.base.MoreObjects;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Hand;
import net.minecraft.util.Arm;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.rubin.Rubin;
import ru.rubin.event.EventManager;
import ru.rubin.event.render.HandAnimationEvent;
import ru.rubin.event.render.RenderItemEvent;
import ru.rubin.module.impl.combat.auraProcess.rotationProcess.impl.FreeLookUtil;
import ru.rubin.module.impl.visuals.SwingAnimation;

@Environment(EnvType.CLIENT)
@Mixin({HeldItemRenderer.class})
public abstract class HeldItemRendererMixin {
    @Shadow
    private ItemStack mainHand;
    @Shadow
    private ItemStack offHand;
    @Shadow
    private float equipProgressMainHand;
    @Shadow
    private float lastEquipProgressMainHand;
    @Shadow
    private float equipProgressOffHand;
    @Shadow
    private float lastEquipProgressOffHand;

    @Shadow
    protected abstract void renderFirstPersonItem(
            AbstractClientPlayerEntity var1, float var2, float var3, Hand var4, float var5, ItemStack var6, float var7, MatrixStack var8, OrderedRenderCommandQueue var9, int var10
    );

    @Inject(
            method = {"renderFirstPersonItem"},
            at = {@At("HEAD")}
    )
    private void onRenderFirstPersonItem(
            AbstractClientPlayerEntity player, float tickProgress, float pitch, Hand hand, float swingProgress, ItemStack stack, float equipProgress, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, CallbackInfo ci
    ) {
        RenderItemEvent renderItemEvent = new RenderItemEvent(matrices, hand);
        EventManager.call(renderItemEvent);
        ru.rubin.module.impl.visuals.GlassHands.preRender();
    }

    @Inject(
            method = {"renderFirstPersonItem"},
            at = {@At("TAIL")}
    )
    private void onRenderFirstPersonItemTail(
            AbstractClientPlayerEntity player, float tickProgress, float pitch, Hand hand, float swingProgress, ItemStack stack, float equipProgress, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, CallbackInfo ci
    ) {
        ru.rubin.module.impl.visuals.GlassHands.postRender();
    }

    @Redirect(
            method = {"renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw(F)F"
            )
    )
    private float redirectGetYaw(ClientPlayerEntity instance, float tickProgress) {
        return MinecraftClient.getInstance().gameRenderer.getCamera().getYaw();
    }

    @Redirect(
            method = {"renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch(F)F"
            )
    )
    private float redirectGetPitch(ClientPlayerEntity instance, float tickProgress) {
        return MinecraftClient.getInstance().gameRenderer.getCamera().getPitch();
    }

    @WrapOperation(
            method = {"renderFirstPersonItem"},
            at = {@At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/HeldItemRenderer;swingArm(FLnet/minecraft/client/util/math/MatrixStack;ILnet/minecraft/util/Arm;)V"
            )}
    )
    private void handAnimationHook(
            HeldItemRenderer instance,
            float swingProgress,
            MatrixStack matrices,
            int armX,
            Arm arm,
            Operation<Void> original,
            @Local(ordinal = 0,argsOnly = true) AbstractClientPlayerEntity player,
            @Local(ordinal = 0,argsOnly = true) Hand hand
    ) {
        HandAnimationEvent event = new HandAnimationEvent(matrices, hand, swingProgress);
        EventManager.call(event);

        if (!event.isCancelled()) {
            original.call(instance, swingProgress, matrices, armX, arm);
        }
    }

    @Unique
    private boolean night$isChargedCrossbow(ItemStack stack) {
        return stack.isOf(Items.CROSSBOW) && CrossbowItem.isCharged(stack);
    }

    @Inject(
            method = {"updateHeldItems"},
            at = {@At("TAIL")}
    )
    private void onUpdateHeldItems(CallbackInfo ci) {
        if (Rubin.isModInitialized() && Rubin.get != null && Rubin.get.manager != null) {
            SwingAnimation swingModule = (SwingAnimation) Rubin.get.manager.getModule(SwingAnimation.class);
            if (swingModule != null && swingModule.enable && !SwingAnimation.swingMode.is("Off") && SwingAnimation.auraCheck()) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player != null) {
                    ItemStack currentMain = mc.player.getMainHandStack();
                    if (ItemStack.areEqual(currentMain, this.mainHand)) {
                        this.lastEquipProgressMainHand = 1.0F;
                        this.equipProgressMainHand = 1.0F;
                        this.mainHand = currentMain;
                    }
                }
            }
        }
    }
}

