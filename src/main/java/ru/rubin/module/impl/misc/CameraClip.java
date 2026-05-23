package ru.rubin.module.impl.misc;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.rubin.module.api.Category;
import ru.rubin.module.api.IModule;
import ru.rubin.module.api.Module;
import ru.rubin.module.api.setting.Setting;
import ru.rubin.module.api.setting.impl.SliderSetting;

/**
 * CameraClip — камера проходит сквозь блоки в третьем лице.
 * Работает через CameraMixin — когда модуль включён, clipToSpace возвращает полную дистанцию.
 */
@IModule(name = "Camera Clip", description = "Камера сквозь блоки", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class CameraClip extends Module {

    public static SliderSetting distance = new SliderSetting("Дистанция", 4.0F, 1.0F, 10.0F, 0.1F, false);

    public CameraClip() {
        this.addSettings(new Setting[]{distance});
    }

    public static boolean isActive() {
        if (ru.rubin.Rubin.get == null || ru.rubin.Rubin.get.manager == null) return false;
        CameraClip module = (CameraClip) ru.rubin.Rubin.get.manager.getModule(CameraClip.class);
        return module != null && module.enable;
    }

    public static float getDistance() {
        return distance.get();
    }
}
