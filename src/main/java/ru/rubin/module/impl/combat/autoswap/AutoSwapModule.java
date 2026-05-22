package ru.rubin.module.impl.combat.autoswap;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;
import ru.rubin.Rubin;
import ru.rubin.event.EventInit;
import ru.rubin.event.lifecycle.ClientTickEvent;
import ru.rubin.module.api.Category;
import ru.rubin.module.api.IModule;
import ru.rubin.module.api.Module;
import ru.rubin.module.api.setting.Setting;
import ru.rubin.module.api.setting.impl.BooleanSetting;
import ru.rubin.module.api.setting.impl.ModeSetting;
import ru.rubin.module.api.setting.impl.SliderSetting;
import ru.rubin.module.impl.visuals.Hud;
import ru.rubin.util.render.core.Renderer2D;

@IModule(
    name = "Auto Swap",
    description = "Тройной свап по бинду",
    category = Category.Combat,
    bind = -1
)
@Environment(EnvType.CLIENT)
public class AutoSwapModule extends Module {

    public static final int SEGMENT_COUNT = 3;
    public static final int OFFHAND_SLOT_ID = 45;

    public static SliderSetting bindKey = new SliderSetting("Бинд (GLFW код)", 0F, 0F, 350F, 1F, false);
    public static ModeSetting swapMode = new ModeSetting("Режим", "Normal", "Normal", "Wheel");
    public static ModeSetting itemType = new ModeSetting("Свап с", "Shield", "Shield", "Gapple", "Totem", "Head")
            .hidden(() -> swapMode.is("Wheel"));
    public static ModeSetting swapType = new ModeSetting("Свап на", "Gapple", "Shield", "Gapple", "Totem", "Head")
            .hidden(() -> swapMode.is("Wheel"));
    public static BooleanSetting notifications = new BooleanSetting("Уведомления", true);

    private final String[] wheelItems = new String[SEGMENT_COUNT];
    private boolean keyWasPressed = false;

    public AutoSwapModule() {
        this.addSettings(new Setting[]{bindKey, swapMode, itemType, swapType, notifications});
        for (int i = 0; i < SEGMENT_COUNT; i++) {
            wheelItems[i] = "";
        }
    }

    @EventInit
    public void onTick(ClientTickEvent event) {
        if (mc.player == null || mc.world == null) return;

        int key = (int) bindKey.get();
        if (key <= 0) return;

        boolean pressed = GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == GLFW.GLFW_PRESS;

        if (pressed && !keyWasPressed) {
            keyWasPressed = true;
            if (swapMode.is("Normal")) {
                performNormalSwap();
            } else if (swapMode.is("Wheel")) {
                mc.setScreen(new AutoSwapUI(this));
            }
        }

        if (!pressed) {
            keyWasPressed = false;
        }
    }

    private void performNormalSwap() {
        Item from = getItemFromMode(itemType.get());
        Item to = getItemFromMode(swapType.get());

        if (from == null || to == null) return;

        int fromSlot = findItemSlot(from);
        if (fromSlot == -1) {
            int toSlot = findItemSlot(to);
            if (toSlot != -1) {
                swapToSlot(toSlot);
                if (notifications.get()) {
                    notify("Свапнул на " + swapType.get());
                }
            }
        } else {
            swapToSlot(fromSlot);
            if (notifications.get()) {
                notify("Свапнул на " + itemType.get());
            }
        }
    }

    private void swapToSlot(int sourceSlot) {
        if (mc.player == null) return;
        int syncId = mc.player.currentScreenHandler.syncId;
        int adjustedSlot = sourceSlot < 9 ? sourceSlot + 36 : sourceSlot;

        // Triple swap: pickup source → place in offhand → pickup what was in offhand
        mc.interactionManager.clickSlot(syncId, adjustedSlot, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(syncId, OFFHAND_SLOT_ID, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(syncId, adjustedSlot, 0, SlotActionType.PICKUP, mc.player);
    }

    public void swapToStack(ItemStack stack) {
        if (mc.player == null || stack == null || stack.isEmpty()) return;
        Item target = stack.getItem();
        int slot = findItemSlot(target);
        if (slot != -1) {
            swapToSlot(slot);
            if (notifications.get()) {
                notify("Свапнул на " + stack.getName().getString());
            }
        }
    }

    private int findItemSlot(Item item) {
        if (mc.player == null) return -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private Item getItemFromMode(String mode) {
        return switch (mode) {
            case "Shield" -> Items.SHIELD;
            case "Gapple" -> Items.GOLDEN_APPLE;
            case "Totem" -> Items.TOTEM_OF_UNDYING;
            case "Head" -> Items.PLAYER_HEAD;
            default -> null;
        };
    }

    private void notify(String message) {
        if (mc.player == null) return;
        Rubin.get.manager.get(Hud.class)
                .showNotification("warn", "AutoSwap - " + message, 1200L, Renderer2D.ColorUtil.getTextTwoColor(1, 1));
        Text msg = Text.literal("AutoSwap - ")
                .formatted(Formatting.WHITE)
                .append(Text.literal(message).formatted(Formatting.RED));
        mc.player.sendMessage(msg, false);
    }

    public String getWheelItem(int index) {
        if (index < 0 || index >= SEGMENT_COUNT) return "";
        return wheelItems[index];
    }

    public void setWheelItem(int index, ItemStack stack) {
        if (index < 0 || index >= SEGMENT_COUNT) return;
        if (stack == null || stack.isEmpty()) {
            wheelItems[index] = "";
        } else {
            wheelItems[index] = stack.getItem().toString();
        }
    }

    public ItemStack getWheelItemStack(int index) {
        if (index < 0 || index >= SEGMENT_COUNT) return ItemStack.EMPTY;
        String id = wheelItems[index];
        if (id == null || id.isEmpty()) return ItemStack.EMPTY;
        Item item = getItemFromString(id);
        if (item == null || item == Items.AIR) return ItemStack.EMPTY;
        return new ItemStack(item);
    }

    private Item getItemFromString(String id) {
        return switch (id) {
            case "shield" -> Items.SHIELD;
            case "golden_apple" -> Items.GOLDEN_APPLE;
            case "totem_of_undying" -> Items.TOTEM_OF_UNDYING;
            case "player_head" -> Items.PLAYER_HEAD;
            default -> {
                // Try matching item toString
                if (id.contains("shield")) yield Items.SHIELD;
                else if (id.contains("golden_apple")) yield Items.GOLDEN_APPLE;
                else if (id.contains("totem")) yield Items.TOTEM_OF_UNDYING;
                else if (id.contains("player_head")) yield Items.PLAYER_HEAD;
                else yield Items.AIR;
            }
        };
    }
}
