package ru.rubin.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.rubin.module.api.Category;
import ru.rubin.module.api.IModule;
import ru.rubin.module.api.Module;
import ru.rubin.module.api.setting.Setting;
import ru.rubin.module.api.setting.impl.BooleanSetting;
import ru.rubin.module.api.setting.impl.ModeSetting;
import ru.rubin.module.api.setting.impl.SliderSetting;

/**
 * GlassHands — делает оружие/руки прозрачными или со свечением.
 * Работает через HeldItemRendererMixin — перед рендером меняет GL state.
 */
@IModule(name = "Glass Hands", description = "Прозрачные руки / свечение оружия", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class GlassHands extends Module {

    public static ModeSetting mode = new ModeSetting("Режим", "Glass", "Glass", "Glow", "Enchant");
    public static SliderSetting alpha = new SliderSetting("Прозрачность", 0.5F, 0.05F, 1.0F, 0.05F, false)
            .hidden(() -> mode.is("Enchant"));
    public static BooleanSetting onlyWeapon = new BooleanSetting("Только оружие", false);
    public static SliderSetting glowIntensity = new SliderSetting("Сила свечения", 1.5F, 0.5F, 5.0F, 0.1F, false)
            .hidden(() -> !mode.is("Glow"));

    public GlassHands() {
        this.addSettings(new Setting[]{mode, alpha, onlyWeapon, glowIntensity});
    }

    /**
     * Called BEFORE hand/item rendering from HeldItemRendererMixin.
     */
    public static void preRender() {
        if (!isActive()) return;

        if (mode.is("Glow")) {
            org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_BLEND);
            org.lwjgl.opengl.GL11.glBlendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA, org.lwjgl.opengl.GL11.GL_ONE);
        } else if (mode.is("Glass")) {
            org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_BLEND);
            org.lwjgl.opengl.GL11.glBlendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA, org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA);
            org.lwjgl.opengl.GL11.glDepthMask(false);
        }
    }

    /**
     * Called AFTER hand/item rendering from HeldItemRendererMixin.
     */
    public static void postRender() {
        if (!isActive()) return;

        org.lwjgl.opengl.GL11.glBlendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA, org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA);
        org.lwjgl.opengl.GL11.glDepthMask(true);
    }

    private static boolean isActive() {
        if (ru.rubin.Rubin.get == null || ru.rubin.Rubin.get.manager == null) return false;
        GlassHands module = (GlassHands) ru.rubin.Rubin.get.manager.getModule(GlassHands.class);
        return module != null && module.enable;
    }
}
