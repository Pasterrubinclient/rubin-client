package ru.rubin.module.impl.misc;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import ru.rubin.event.EventInit;
import ru.rubin.event.lifecycle.ClientTickEvent;
import ru.rubin.module.api.Category;
import ru.rubin.module.api.IModule;
import ru.rubin.module.api.Module;
import ru.rubin.module.api.setting.Setting;
import ru.rubin.module.api.setting.impl.BooleanSetting;
import ru.rubin.module.api.setting.impl.ModeSetting;
import ru.rubin.module.api.setting.impl.SliderSetting;

@IModule(name = "Auto Leave", description = "Автоматический выход при опасности", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class AutoLeave extends Module {

    public static ModeSetting leaveMode = new ModeSetting("Команда", "/hub", "/hub", "/spawn", "/darena");
    public static BooleanSetting onPlayerNearby = new BooleanSetting("Игрок рядом", true);
    public static SliderSetting playerRange = new SliderSetting("Дистанция игрока", 20.0F, 10.0F, 50.0F, 1.0F, false);
    public static BooleanSetting onPvpEnd = new BooleanSetting("Конец PvP", false);

    private long lastLeaveTime = 0L;
    private long antiSpamTime = 0L;
    private boolean waitingForDarenaMenu = false;
    private int darenaClickTicks = 0;
    private boolean didLeave = false;

    private static final long ANTI_SPAM_DELAY = 5000L;
    private static final long POST_LEAVE_COOLDOWN = 10000L;

    public AutoLeave() {
        this.addSettings(new Setting[]{leaveMode, onPlayerNearby, playerRange, onPvpEnd});
    }

    @Override
    public void onDisable() {
        super.onDisable();
        waitingForDarenaMenu = false;
        darenaClickTicks = 0;
        didLeave = false;
    }

    @EventInit
    public void onTick(ClientTickEvent event) {
        if (mc.player == null || mc.world == null) return;

        // After /hub or /spawn — check if teleported to spawn
        if (didLeave && !leaveMode.is("/darena")) {
            double x = Math.abs(mc.player.getX());
            double z = Math.abs(mc.player.getZ());
            if (x <= 5.0 && z <= 5.0) {
                mc.player.sendMessage(net.minecraft.text.Text.literal("§e[AutoLeave] §aТелепортация успешна, выключаюсь."), false);
                didLeave = false;
                this.enable = false;
                this.onDisable();
                return;
            }
        }

        // Waiting for darena menu to click barrier
        if (waitingForDarenaMenu) {
            darenaClickTicks++;
            if (mc.currentScreen instanceof GenericContainerScreen screen) {
                GenericContainerScreenHandler handler = screen.getScreenHandler();
                int containerSlots = handler.getRows() * 9;

                for (int i = 0; i < containerSlots; i++) {
                    Slot slot = handler.getSlot(i);
                    if (slot.hasStack() && slot.getStack().getItem() == Items.BARRIER) {
                        mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
                        waitingForDarenaMenu = false;
                        darenaClickTicks = 0;
                        return;
                    }
                }

                if (darenaClickTicks > 40) {
                    waitingForDarenaMenu = false;
                    darenaClickTicks = 0;
                }
            } else if (darenaClickTicks > 60) {
                waitingForDarenaMenu = false;
                darenaClickTicks = 0;
            }
            return;
        }

        // Cooldown
        if (System.currentTimeMillis() - lastLeaveTime < POST_LEAVE_COOLDOWN) return;
        if (System.currentTimeMillis() - antiSpamTime < ANTI_SPAM_DELAY) return;

        boolean shouldLeave = false;
        String reason = "";

        // Check players nearby
        if (onPlayerNearby.get()) {
            float maxRange = playerRange.get();
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == mc.player) continue;
                if (!player.isAlive()) continue;
                double dist = mc.player.distanceTo(player);
                if (dist <= maxRange) {
                    shouldLeave = true;
                    reason = "Игрок " + player.getName().getString() + " рядом (" + (int) dist + "м)";
                    break;
                }
            }
        }

        if (shouldLeave) {
            executeLeave(reason);
        }
    }

    private void executeLeave(String reason) {
        antiSpamTime = System.currentTimeMillis();
        lastLeaveTime = System.currentTimeMillis();
        String mode = leaveMode.get();

        mc.player.sendMessage(net.minecraft.text.Text.literal("§e[AutoLeave] §c" + reason + " §7→ §a" + mode), false);

        switch (mode) {
            case "/hub" -> {
                mc.player.networkHandler.sendChatCommand("hub");
                didLeave = true;
            }
            case "/spawn" -> {
                mc.player.networkHandler.sendChatCommand("spawn");
                didLeave = true;
            }
            case "/darena" -> {
                mc.player.networkHandler.sendChatCommand("darena");
                waitingForDarenaMenu = true;
                darenaClickTicks = 0;
            }
        }
    }
}
