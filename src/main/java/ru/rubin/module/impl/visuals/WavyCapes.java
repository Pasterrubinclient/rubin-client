package ru.rubin.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;
import ru.rubin.module.api.Category;
import ru.rubin.module.api.IModule;
import ru.rubin.module.api.Module;
import ru.rubin.module.api.setting.Setting;
import ru.rubin.module.api.setting.impl.BooleanSetting;
import ru.rubin.module.api.setting.impl.SliderSetting;

@IModule(name = "Wavy Capes", description = "Волнообразная анимация плаща", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class WavyCapes extends Module {
    private static final Identifier CUSTOM_CAPE_ID = Identifier.of("rubin", "cape/custom");
    private static final Identifier CUSTOM_CAPE_TEXTURE = Identifier.of("rubin", "textures/cape.png");
    private static final AssetInfo.TextureAsset CUSTOM_CAPE_ASSET =
            new AssetInfo.TextureAssetInfo(CUSTOM_CAPE_ID, CUSTOM_CAPE_TEXTURE);

    public static BooleanSetting onlySelf = new BooleanSetting("Только свой плащ", false);
    public static BooleanSetting customTexture = new BooleanSetting("Кастомный плащ", true);
    public static BooleanSetting customTextureForAll = new BooleanSetting("Плащ для всех", false)
            .hidden(() -> !customTexture.get());

    public static SliderSetting speed = new SliderSetting("Скорость", 2.1F, 0.2F, 6.0F, 0.1F, false);
    public static SliderSetting pitchAmplitude = new SliderSetting("X амплитуда", 5.8F, 0.0F, 20.0F, 0.1F, false);
    public static SliderSetting rollAmplitude = new SliderSetting("Z амплитуда", 2.7F, 0.0F, 20.0F, 0.1F, false);
    public static SliderSetting yawAmplitude = new SliderSetting("Y амплитуда", 1.8F, 0.0F, 20.0F, 0.1F, false);
    public static SliderSetting verticalBobbing = new SliderSetting("Вертикаль", 0.018F, 0.0F, 0.12F, 0.001F, false);

    public WavyCapes() {
        this.addSettings(new Setting[]{onlySelf, customTexture, customTextureForAll, speed, pitchAmplitude, rollAmplitude, yawAmplitude, verticalBobbing});
    }

    public boolean shouldAnimate(PlayerEntityRenderState state) {
        if (!this.enable) return false;
        if (!onlySelf.get()) return true;
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.player != null && state != null && state.id == mc.player.getId();
    }

    public boolean shouldUseCustomTexture(PlayerEntityRenderState state) {
        if (!this.enable || !customTexture.get() || state == null) return false;
        if (customTextureForAll.get()) return true;
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.player != null && state.id == mc.player.getId();
    }

    public AssetInfo.TextureAsset getCustomCapeAsset() {
        return CUSTOM_CAPE_ASSET;
    }

    public float getSpeed() { return speed.get(); }
    public float getPitchAmplitude() { return pitchAmplitude.get(); }
    public float getRollAmplitude() { return rollAmplitude.get(); }
    public float getYawAmplitude() { return yawAmplitude.get(); }
    public float getVerticalBobbing() { return verticalBobbing.get(); }
}
