package ru.rubin.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import ru.rubin.module.api.Category;
import ru.rubin.module.api.IModule;
import ru.rubin.module.api.Module;
import ru.rubin.module.api.setting.Setting;
import ru.rubin.module.api.setting.impl.BooleanSetting;
import ru.rubin.module.api.setting.impl.SliderSetting;
import ru.rubin.module.api.setting.impl.ModeSetting;

@IModule(name = "Totem Angle", description = "Кастомная анимация тотема", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class TotemAngle extends Module {
    public static ModeSetting mode = new ModeSetting("Режим", "Default", "Default", "Custom");
    public static SliderSetting scale = new SliderSetting("Масштаб", 1.0F, 0.1F, 3.0F, 0.1F, false);
    public static BooleanSetting onlyOwnTotem = new BooleanSetting("Только свой", true);

    private long lastTotemTime = 0;

    public TotemAngle() {
        this.addSettings(new Setting[]{mode, scale, onlyOwnTotem});
    }

    public void onEntityStatusPacket(EntityStatusS2CPacket packet) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        // Status 35 = totem of undying activation
        if (packet.getStatus() == 35) {
            if (onlyOwnTotem.get()) {
                if (packet.getEntity(mc.world) == mc.player) {
                    lastTotemTime = System.currentTimeMillis();
                }
            } else {
                lastTotemTime = System.currentTimeMillis();
            }
        }
    }

    public long getLastTotemTime() {
        return lastTotemTime;
    }
}
