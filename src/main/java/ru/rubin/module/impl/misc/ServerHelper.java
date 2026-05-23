package ru.rubin.module.impl.misc;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.CooldownUpdateS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import ru.rubin.event.EventInit;
import ru.rubin.event.impl.EventPacket;
import ru.rubin.event.impl.EventUpdate;
import ru.rubin.event.input.KeyInputEvent;
import ru.rubin.module.api.Category;
import ru.rubin.module.api.IModule;
import ru.rubin.module.api.Module;
import ru.rubin.module.api.setting.Setting;
import ru.rubin.module.api.setting.impl.BindSettings;
import ru.rubin.util.other.TimerUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@IModule(name = "Server Helper", description = "Быстрое использование серверных предметов по бинду", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class ServerHelper extends Module {

    public static BindSettings eyeKey = new BindSettings("Дезка", -1);
    public static BindSettings sugarKey = new BindSettings("Явка", -1);
    public static BindSettings snowKey = new BindSettings("Снежок", -1);
    public static BindSettings plastKey = new BindSettings("Пласт", -1);
    public static BindSettings trapKey = new BindSettings("Трапка", -1);
    public static BindSettings windKey = new BindSettings("З.Ветра", -1);
    public static BindSettings bojAuraKey = new BindSettings("Бож.Аура", -1);
    public static BindSettings keySerka = new BindSettings("Радейка", -1);
    public static BindSettings keyHilka = new BindSettings("Бож.Вода", -1);
    public static BindSettings palladinKey = new BindSettings("Палладин", -1);
    public static BindSettings assassinKey = new BindSettings("Ассасин", -1);
    public static BindSettings sleepKey = new BindSettings("Снотворное", -1);
    public static BindSettings angerKey = new BindSettings("Гнев", -1);
    public static BindSettings coordsKey = new BindSettings("Корды в КЧ", -1);

    private int itemSlot = -1;
    private int oldSlot = -1;
    private int step = 0;
    private Item targetItem = null;
    private String targetName = null;
    private final TimerUtil timer = new TimerUtil();
    private final Map<Item, TimedCooldown> cooldowns = new ConcurrentHashMap<>();

    public ServerHelper() {
        this.addSettings(new Setting[]{eyeKey, sugarKey, snowKey, plastKey, trapKey, windKey, bojAuraKey, keySerka, keyHilka, palladinKey, assassinKey, sleepKey, angerKey, coordsKey});
    }

    @Override
    public void onDisable() {
        super.onDisable();
        step = 0;
        cooldowns.clear();
    }

    @EventInit
    public void onPacket(EventPacket event) {
        if (mc.world == null) return;
        if (event.getPacket() instanceof CooldownUpdateS2CPacket pkt) {
            // Track cooldowns from server
        }
    }

    @EventInit
    public void onKey(KeyInputEvent event) {
        if (mc.player == null || mc.currentScreen != null || step != 0) return;
        int key = event.key();
        if (key == -1) return;

        Item toUse = null;
        if (key == sugarKey.get()) toUse = Items.SUGAR;
        else if (key == eyeKey.get()) toUse = Items.ENDER_EYE;
        else if (key == snowKey.get()) toUse = Items.SNOWBALL;
        else if (key == plastKey.get()) toUse = Items.CLAY_BALL;
        else if (key == trapKey.get()) toUse = Items.SCULK_SENSOR;
        else if (key == windKey.get()) toUse = Items.WIND_CHARGE;
        else if (key == bojAuraKey.get()) toUse = Items.NETHER_STAR;
        else if (key == coordsKey.get()) { sendCoords(); return; }

        if (key == keyHilka.get()) { startActionByName("Святая вода"); return; }
        if (key == keySerka.get()) { startActionByName("Зелье Радиации"); return; }
        if (key == palladinKey.get()) { startActionByName("Зелье Палладина"); return; }
        if (key == assassinKey.get()) { startActionByName("Зелье Ассасина"); return; }
        if (key == sleepKey.get()) { startActionByName("Снотворное"); return; }
        if (key == angerKey.get()) { startActionByName("Зелье Гнева"); return; }

        if (toUse != null) startAction(toUse);
    }

    private void startAction(Item item) {
        targetName = null;
        targetItem = item;
        findSlotAndStart();
    }

    private void startActionByName(String name) {
        targetItem = null;
        targetName = name.toLowerCase();
        findSlotAndStart();
    }

    private void findSlotAndStart() {
        itemSlot = -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            if (targetName != null) {
                if (stack.getName().getString().toLowerCase().contains(targetName)) {
                    itemSlot = i;
                    break;
                }
            } else if (targetItem != null && stack.getItem() == targetItem) {
                itemSlot = i;
                break;
            }
        }

        if (itemSlot == -1) {
            String msg = targetName != null ? targetName : targetItem.getName().getString();
            mc.player.sendMessage(Text.literal("Предмет [" + msg + "] не найден!").formatted(Formatting.RED), true);
            return;
        }

        oldSlot = mc.player.getInventory().getSelectedSlot();
        timer.reset();
        step = 1;
    }

    @EventInit
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.interactionManager == null || step == 0) return;

        if (!timer.hasReached(50)) return;
        timer.reset();

        switch (step) {
            case 1 -> {
                if (itemSlot < 9) {
                    mc.player.getInventory().setSelectedSlot(itemSlot);
                } else {
                    mc.interactionManager.clickSlot(
                            mc.player.playerScreenHandler.syncId, itemSlot, oldSlot,
                            SlotActionType.SWAP, mc.player);
                }
                step = 2;
            }
            case 2 -> {
                mc.player.swingHand(Hand.MAIN_HAND);
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                step = 3;
            }
            case 3 -> {
                if (itemSlot < 9) {
                    mc.player.getInventory().setSelectedSlot(oldSlot);
                } else {
                    mc.interactionManager.clickSlot(
                            mc.player.playerScreenHandler.syncId, itemSlot, oldSlot,
                            SlotActionType.SWAP, mc.player);
                }
                step = 4;
            }
            case 4 -> {
                step = 0;
                targetItem = null;
                targetName = null;
            }
        }
    }

    private void sendCoords() {
        if (mc.player != null) {
            int x = mc.player.getBlockX();
            int y = mc.player.getBlockY();
            int z = mc.player.getBlockZ();
            mc.player.networkHandler.sendChatMessage("/cc " + x + " " + y + " " + z);
        }
    }

    private static class TimedCooldown {
        long startTick;
        long durationTicks;
        TimedCooldown(long start, long duration) { startTick = start; durationTicks = duration; }
        boolean isFinished(long currentTick) { return currentTick >= startTick + durationTicks; }
    }
}
