package ru.rubin.module.impl.misc;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import ru.rubin.Rubin;
import ru.rubin.event.EventInit;
import ru.rubin.event.input.KeyInputEvent;
import ru.rubin.module.api.Category;
import ru.rubin.module.api.IModule;
import ru.rubin.module.api.Module;
import ru.rubin.module.api.setting.Setting;
import ru.rubin.module.api.setting.impl.BindSettings;
import ru.rubin.module.api.setting.impl.ModeSetting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@IModule(name = "Auto Warp", description = "Авто-варп: установка и отправка варпа", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class AutoWarp extends Module {

    public static BindSettings setWarpKey = new BindSettings("SetWarp бинд", -1);
    public static ModeSetting warpMode = new ModeSetting("Режим", "Clan", "Clan", "Friend", "Global");

    private static final String CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";

    public AutoWarp() {
        this.addSettings(new Setting[]{setWarpKey, warpMode});
    }

    @EventInit
    public void onKey(KeyInputEvent event) {
        if (mc.player == null || mc.currentScreen != null) return;
        if (event.key() == setWarpKey.get() && setWarpKey.get() != -1) {
            handleSetWarp();
        }
    }

    private void handleSetWarp() {
        String name = generateRandomName();
        mc.player.networkHandler.sendChatCommand("setwarp " + name);
        sendMsg("§aВарп установлен: §f" + name);

        if (warpMode.is("Clan")) {
            mc.player.networkHandler.sendChatMessage("/cc " + name);
        } else if (warpMode.is("Friend")) {
            List<String> onlineFriends = getOnlineFriends();
            if (onlineFriends.isEmpty()) {
                sendMsg("§cНет друзей онлайн!");
                return;
            }
            for (String friendName : onlineFriends) {
                mc.player.networkHandler.sendChatMessage("/msg " + friendName + " " + name);
            }
            sendMsg("§aОтправлено " + onlineFriends.size() + " друзьям");
        } else if (warpMode.is("Global")) {
            mc.player.networkHandler.sendChatMessage("!" + name);
        }
    }

    private String generateRandomName() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int length = rng.nextInt(4, 16);
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(rng.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private List<String> getOnlineFriends() {
        List<String> result = new ArrayList<>();
        if (mc.player == null || mc.getNetworkHandler() == null || Rubin.get == null || Rubin.get.friendManager == null) {
            return result;
        }

        Set<String> onlineNames = new HashSet<>();
        for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
            if (entry.getProfile() != null && entry.getProfile().name() != null) {
                onlineNames.add(entry.getProfile().name());
            }
        }

        String myName = mc.player.getName().getString();

        // Check friends that are online
        for (String onlineName : onlineNames) {
            if (!onlineName.equalsIgnoreCase(myName) && Rubin.get.friendManager.isFriend(onlineName)) {
                result.add(onlineName);
            }
        }

        return result;
    }

    private void sendMsg(String text) {
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("§b[AutoWarp] " + text), false);
        }
    }
}
