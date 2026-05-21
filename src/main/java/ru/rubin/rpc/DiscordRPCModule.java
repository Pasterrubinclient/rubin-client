package ru.rubin.rpc;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import ru.rubin.event.EventInit;
import ru.rubin.event.lifecycle.ClientTickEvent;
import ru.rubin.module.api.Category;
import ru.rubin.module.api.IModule;
import ru.rubin.module.api.Module;
import ru.rubin.module.api.setting.Setting;
import ru.rubin.module.api.setting.impl.ModeSetting;

@IModule(name = "DiscordRPC", description = "Discord Rich Presence", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class DiscordRPCModule extends Module {

    private static final String APP_ID_CHEAT = "1426977740967514333";
    private static final String APP_ID_VISUALS = "1495212121003131020";
    private static final String MODE_CHEAT = "Cheat";
    private static final String MODE_VISUALS = "Visuals";

    public static ModeSetting appMode = new ModeSetting("RPC приложение", MODE_CHEAT, MODE_CHEAT, MODE_VISUALS);

    private String lastAppliedAppId = "";

    public DiscordRPCModule() {
        this.addSettings(new Setting[]{appMode});
    }

    @Override
    public void onEnable() {
        super.onEnable();
        applyRpcApp(true);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        lastAppliedAppId = "";
        DiscordManager.getInstance().stopRPC();
    }

    @EventInit
    public void onTick(ClientTickEvent event) {
        if (this.enable) {
            applyRpcApp(false);
        }
    }

    private void applyRpcApp(boolean force) {
        String appId = resolveAppId();
        DiscordManager manager = DiscordManager.getInstance();

        if (!force && appId.equals(lastAppliedAppId) && manager.isRunning()) {
            return;
        }

        manager.startRPC(appId);
        lastAppliedAppId = appId;
    }

    private String resolveAppId() {
        return appMode.is(MODE_VISUALS) ? APP_ID_VISUALS : APP_ID_CHEAT;
    }
}
