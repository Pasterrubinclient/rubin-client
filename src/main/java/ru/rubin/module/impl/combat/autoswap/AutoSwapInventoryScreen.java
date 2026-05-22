package ru.rubin.module.impl.combat.autoswap;

import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import ru.rubin.mixin.HandledScreenAccessor;

public class AutoSwapInventoryScreen extends InventoryScreen {

    private final AutoSwapUI parentScreen;
    private final AutoSwapModule module;
    private final int slotIndex;

    public AutoSwapInventoryScreen(AutoSwapUI parentScreen, AutoSwapModule module, int slotIndex) {
        super(net.minecraft.client.MinecraftClient.getInstance().player);
        this.parentScreen = parentScreen;
        this.module = module;
        this.slotIndex = slotIndex;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            Slot focusedSlot = ((HandledScreenAccessor) this).rubin$getFocusedSlot();
            if (focusedSlot != null && focusedSlot.hasStack()) {
                ItemStack stack = focusedSlot.getStack();
                module.setWheelItem(slotIndex, stack);
                client.setScreen(parentScreen);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
