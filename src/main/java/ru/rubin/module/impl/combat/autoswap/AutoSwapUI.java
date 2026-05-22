package ru.rubin.module.impl.combat.autoswap;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class AutoSwapUI extends Screen {

    private static final int BG_PANEL = 0xE610141D;
    private static final int BG_SLOT = 0xFF121925;
    private static final int BG_SLOT_EMPTY = 0xCC0E131C;
    private static final int RADIUS = 86;
    private static final int SLOT_SIZE = 66;
    private static final double[] ANGLES = {-90, 30, 150};

    private final AutoSwapModule module;
    private int hoveredSlot = -1;

    public AutoSwapUI(AutoSwapModule module) {
        super(Text.literal("Auto Swap Wheel"));
        this.module = module;
    }

    @Override
    public void tick() {
        super.tick();
        int key = (int) AutoSwapModule.bindKey.get();
        if (key > 0 && GLFW.glfwGetKey(client.getWindow().getHandle(), key) != GLFW.GLFW_PRESS) {
            if (hoveredSlot >= 0 && hoveredSlot < AutoSwapModule.SEGMENT_COUNT) {
                ItemStack stack = module.getWheelItemStack(hoveredSlot);
                if (!stack.isEmpty()) {
                    module.swapToStack(stack);
                }
            }
            close();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Draw background overlay
        context.fill(0, 0, this.width, this.height, 0x88000000);

        hoveredSlot = -1;

        for (int i = 0; i < AutoSwapModule.SEGMENT_COUNT; i++) {
            double angle = Math.toRadians(ANGLES[i]);
            int slotX = centerX + (int) (Math.cos(angle) * RADIUS) - SLOT_SIZE / 2;
            int slotY = centerY + (int) (Math.sin(angle) * RADIUS) - SLOT_SIZE / 2;

            boolean hovered = mouseX >= slotX && mouseX <= slotX + SLOT_SIZE
                    && mouseY >= slotY && mouseY <= slotY + SLOT_SIZE;

            if (hovered) {
                hoveredSlot = i;
            }

            ItemStack stack = module.getWheelItemStack(i);
            boolean empty = stack.isEmpty();

            // Panel background
            context.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, BG_PANEL);
            // Slot background
            int innerPad = 4;
            int bgColor = empty ? BG_SLOT_EMPTY : BG_SLOT;
            if (hovered) bgColor = 0xFF1E2A3D;
            context.fill(slotX + innerPad, slotY + innerPad,
                    slotX + SLOT_SIZE - innerPad, slotY + SLOT_SIZE - innerPad, bgColor);

            if (empty) {
                // Draw "+" text
                context.drawText(client.textRenderer, "+", slotX + SLOT_SIZE / 2 - 3, slotY + SLOT_SIZE / 2 - 4, 0xFFAAAAAA, false);
            } else {
                // Draw item centered
                int itemX = slotX + SLOT_SIZE / 2 - 8;
                int itemY = slotY + SLOT_SIZE / 2 - 8;
                context.drawItem(stack, itemX, itemY);
                // Draw item name below
                String name = stack.getName().getString();
                int textWidth = client.textRenderer.getWidth(name);
                context.drawText(client.textRenderer, name,
                        slotX + SLOT_SIZE / 2 - textWidth / 2, slotY + SLOT_SIZE - 12, 0xFFFFFFFF, true);
            }

            // Draw slot number
            context.drawText(client.textRenderer, String.valueOf(i + 1), slotX + 6, slotY + 6, 0xFF888888, false);
        }

        // Draw title
        String title = "Auto Swap Wheel";
        int titleWidth = client.textRenderer.getWidth(title);
        context.drawText(client.textRenderer, title, centerX - titleWidth / 2, centerY - RADIUS - 40, 0xFFFFFFFF, true);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        int mouseX = (int) click.x();
        int mouseY = (int) click.y();
        int button = click.button();
        if (hoveredSlot >= 0 && hoveredSlot < AutoSwapModule.SEGMENT_COUNT) {
            if (button == 0) {
                ItemStack stack = module.getWheelItemStack(hoveredSlot);
                if (stack.isEmpty()) {
                    client.setScreen(new AutoSwapInventoryScreen(this, module, hoveredSlot));
                    return true;
                }
            } else if (button == 1) {
                module.setWheelItem(hoveredSlot, ItemStack.EMPTY);
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (hoveredSlot >= 0 && hoveredSlot < AutoSwapModule.SEGMENT_COUNT) {
            ItemStack stack = module.getWheelItemStack(hoveredSlot);
            if (!stack.isEmpty()) {
                module.swapToStack(stack);
                close();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
