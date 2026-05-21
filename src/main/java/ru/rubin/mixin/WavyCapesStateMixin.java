package ru.rubin.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.rubin.Rubin;
import ru.rubin.module.impl.visuals.WavyCapes;

@Environment(EnvType.CLIENT)
@Mixin(PlayerEntityRenderer.class)
public class WavyCapesStateMixin {

    @Inject(
            method = "updateRenderState(Lnet/minecraft/entity/PlayerLikeEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V",
            at = @At("TAIL")
    )
    private void rubin$applyCustomCapeTexture(
            PlayerLikeEntity player,
            PlayerEntityRenderState state,
            float tickDelta,
            CallbackInfo ci
    ) {
        if (!Rubin.isModInitialized() || Rubin.get == null || Rubin.get.manager == null) return;
        WavyCapes module = (WavyCapes) Rubin.get.manager.getModule(WavyCapes.class);
        if (module == null || !module.shouldUseCustomTexture(state)) return;

        SkinTextures original = state.skinTextures;
        if (original == null) return;

        AssetInfo.TextureAsset customCape = module.getCustomCapeAsset();
        state.skinTextures = new SkinTextures(
                original.body(),
                customCape,
                original.elytra(),
                original.model(),
                original.secure()
        );
        state.capeVisible = true;
    }
}
