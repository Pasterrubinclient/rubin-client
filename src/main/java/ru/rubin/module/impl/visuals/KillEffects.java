package ru.rubin.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.rubin.module.api.Category;
import ru.rubin.module.api.IModule;
import ru.rubin.module.api.Module;
import ru.rubin.module.api.setting.Setting;
import ru.rubin.module.api.setting.impl.SliderSetting;
import ru.rubin.module.api.setting.impl.ModeSetting;

@IModule(name = "Kill Effects", description = "Эффекты при убийстве", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class KillEffects extends Module {
    public static ModeSetting mode = new ModeSetting("Режим", "Particles", "Particles", "Lightning");
    public static SliderSetting size = new SliderSetting("Размер", 1.0F, 0.1F, 3.0F, 0.1F, false);

    public KillEffects() {
        this.addSettings(new Setting[]{mode, size});
    }
}
