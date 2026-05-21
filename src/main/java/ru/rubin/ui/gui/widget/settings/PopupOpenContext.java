package ru.rubin.ui.gui.widget.settings;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.rubin.module.api.Module;
import ru.rubin.module.api.setting.Setting;

@FunctionalInterface
@Environment(EnvType.CLIENT)
public interface PopupOpenContext {
   void openForSetting(Module var1, Setting var2, double var3, double var5, Object var7);
}
