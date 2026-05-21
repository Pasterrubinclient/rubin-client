package ru.rubin.module.impl.visuals.beautifulhands;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.rubin.Rubin;
import ru.rubin.module.api.Category;
import ru.rubin.module.api.IModule;
import ru.rubin.module.api.Module;
import ru.rubin.module.api.setting.Setting;
import ru.rubin.module.api.setting.impl.ModeSetting;
import ru.rubin.module.api.setting.impl.SliderSetting;
import ru.rubin.module.impl.combat.HitAura;

@IModule(name = "Beautiful Hands", description = "Продвинутая анимация рук от первого лица", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class BeautifulHands extends Module {

    public static ModeSetting attackMode = new ModeSetting("Режим атаки", "Swing", "Swing", "Forward", "Normal");

    public static SliderSetting rightX = new SliderSetting("X правая", 0.0F, -2.0F, 2.0F, 0.1F, false);
    public static SliderSetting rightY = new SliderSetting("Y правая", 0.0F, -2.0F, 2.0F, 0.1F, false);
    public static SliderSetting rightZ = new SliderSetting("Z правая", 0.0F, -2.0F, 2.0F, 0.1F, false);
    public static SliderSetting leftX = new SliderSetting("X левая", 0.0F, -2.0F, 2.0F, 0.1F, false);
    public static SliderSetting leftY = new SliderSetting("Y левая", 0.0F, -2.0F, 2.0F, 0.1F, false);
    public static SliderSetting leftZ = new SliderSetting("Z левая", 0.0F, -2.0F, 2.0F, 0.1F, false);

    public BeautifulHands() {
        this.addSettings(new Setting[]{attackMode, rightX, rightY, rightZ, leftX, leftY, leftZ});
    }

    public boolean useSwingAttack() {
        return this.enable && attackMode.is("Swing");
    }

    public boolean useForwardAttack() {
        return this.enable && attackMode.is("Forward");
    }

    public boolean useNormalAttack() {
        return this.enable && attackMode.is("Normal");
    }

    public boolean hasAuraTarget() {
        HitAura hitAura = (HitAura) Rubin.get.manager.getModule(HitAura.class);
        return hitAura != null && hitAura.enable && HitAura.target != null;
    }
}
