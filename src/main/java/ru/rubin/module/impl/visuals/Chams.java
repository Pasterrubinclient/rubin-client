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
 * Chams - рисует подсветку моделей игроков (через стены).
 * Работает через мixin в EntityRendererMixin / LivingEntityMixin.
 * Включает/выключает depth test при рендере entity.
 */
@IModule(name = "Chams", description = "Подсветка игроков через стены", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class Chams extends Module {

    public static ModeSetting targets = new ModeSetting("Цели", "All", "All", "Enemies", "Friends");
    public static BooleanSetting themeColor = new BooleanSetting("Тема клиента", true);
    public static SliderSetting red = new SliderSetting("R", 255.0F, 0.0F, 255.0F, 1.0F, false)
            .hidden(themeColor::get);
    public static SliderSetting green = new SliderSetting("G", 100.0F, 0.0F, 255.0F, 1.0F, false)
            .hidden(themeColor::get);
    public static SliderSetting blue = new SliderSetting("B", 100.0F, 0.0F, 255.0F, 1.0F, false)
            .hidden(themeColor::get);
    public static SliderSetting alpha = new SliderSetting("Прозрачность", 160.0F, 10.0F, 255.0F, 1.0F, false);
    public static SliderSetting fillAlpha = new SliderSetting("Заливка", 50.0F, 5.0F, 200.0F, 1.0F, false);
    public static SliderSetting lineWidth = new SliderSetting("Толщина линий", 1.5F, 0.5F, 5.0F, 0.1F, false);
    public static BooleanSetting throughWalls = new BooleanSetting("Через стены", true);

    public Chams() {
        this.addSettings(new Setting[]{targets, themeColor, red, green, blue, alpha, fillAlpha, lineWidth, throughWalls});
    }

    public static boolean shouldRender(net.minecraft.entity.Entity entity) {
        if (!(entity instanceof net.minecraft.entity.player.PlayerEntity player)) return false;
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        if (player == mc.player && mc.options.getPerspective() == net.minecraft.client.option.Perspective.FIRST_PERSON) {
            return false;
        }

        if (ru.rubin.Rubin.get == null || ru.rubin.Rubin.get.manager == null) return false;
        Chams chams = (Chams) ru.rubin.Rubin.get.manager.getModule(Chams.class);
        if (chams == null || !chams.enable) return false;

        boolean isFriend = Rubin.get.friendManager.isFriend(player.getName().getString());
        if (targets.is("Enemies") && isFriend) return false;
        if (targets.is("Friends") && !isFriend) return false;

        return true;
    }

    public static int getColor(net.minecraft.entity.Entity entity) {
        boolean isFriend = false;
        if (entity instanceof net.minecraft.entity.player.PlayerEntity player) {
            isFriend = Rubin.get.friendManager.isFriend(player.getName().getString());
        }

        int a = (int) alpha.get();
        if (isFriend) {
            return (a << 24) | 0x00FF00; // green for friends
        }

        if (themeColor.get()) {
            int clientColor = ru.rubin.util.render.core.Renderer2D.ColorUtil.getMainColor(1, 1);
            return (a << 24) | (clientColor & 0x00FFFFFF);
        }

        int r = (int) red.get();
        int g = (int) green.get();
        int b = (int) blue.get();
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static boolean isThroughWalls() {
        return throughWalls.get();
    }
}
