package ru.rubin.module.impl.combat;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

/**
 * Wheel screen for triple swap - shows 3 slots with items to swap to offhand.
 * Hold bind to open, release to swap to hovered item.
 */
public class AutoSwapWheelScreen extends Screen {

    private static final int SEGMENT_COUNT = 3;
    private static final int SLOT_SIZE = 60;
    private static final int RADIUS = 80;
    private static final double[] ANGLES = {-90, 30, 150};

    private static final Item[] ITEMS = {Items.TOTEM_OF_UNDYING, Items.GOLDEN_APPLE, Items.PLAYER_HEAD};
    private static final String[] NAMES = {"Тотем", "Гепыч", "Шар"};

    private final AutoSwap module;
    private int hoveredSlot = -1;
    private boolean firstTick = true;

    public AutoSwapWheelScreen(AutoSwap module) {
        super(Text.literal("Auto Swap Wheel"));
        this.module = module;
    }

    @Override
    public void tick() {
        super.tick();
        if (client == null || client.player == null) {
            close();
            return;
        }

        if (firstTick) {
            firstTick = false;
            return;
        }

        // Close when bind released
        int key = module.getBindKey();
        if (key <= 0) { close(); return; }

        long handle = client.getWindow().getHandle();
        boolean held;
        if (key >= GLFW.GLFW_MOUSE_BUTTON_1 && key <= GLFW.GLFW_MOUSE_BUTTON_LAST) {
            held = GLFW.glfwGetMouseButton(handle, key) == GLFW.GLFW_PRESS;
        } else {
            held = GLFW.glfwGetKey(handle, key) == GLFW.GLFW_PRESS;
        }

        if (!held) {
            if (hoveredSlot >= 0 && hoveredSlot < SEGMENT_COUNT) {
                module.tripleSwapItem(ITEMS[hoveredSlot], NAMES[hoveredSlot]);
            }
            close();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Dark overlay
        context.fill(0, 0, this.width, this.height, 0x88000000);

        int cx = this.width / 2;
        int cy = this.height / 2;

        hoveredSlot = -1;

        for (int i = 0; i < SEGMENT_COUNT; i++) {
            double angle = Math.toRadians(ANGLES[i]);
            int slotX = cx + (int) (Math.cos(angle) * RADIUS) - SLOT_SIZE / 2;
            int slotY = cy + (int) (Math.sin(angle) * RADIUS) - SLOT_SIZE / 2;

            boolean hovered = mouseX >= slotX && mouseX <= slotX + SLOT_SIZE
                    && mouseY >= slotY && mouseY <= slotY + SLOT_SIZE;
            if (hovered) hoveredSlot = i;

            // Background
            int bg = hovered ? 0xFF1E2A3D : 0xCC0E131C;
            context.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, bg);
            // Border
            context.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + 1, 0xFF2A3A5A);
            context.fill(slotX, slotY + SLOT_SIZE - 1, slotX + SLOT_SIZE, slotY + SLOT_SIZE, 0xFF2A3A5A);
            context.fill(slotX, slotY, slotX + 1, slotY + SLOT_SIZE, 0xFF2A3A5A);
            context.fill(slotX + SLOT_SIZE - 1, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, 0xFF2A3A5A);

            // Item
            ItemStack stack = new ItemStack(ITEMS[i]);
            context.drawItem(stack, slotX + SLOT_SIZE / 2 - 8, slotY + 12);

            // Name
            String name = NAMES[i];
            int tw = client.textRenderer.getWidth(name);
            context.drawText(client.textRenderer, name, slotX + SLOT_SIZE / 2 - tw / 2, slotY + SLOT_SIZE - 14, 0xFFFFFFFF, true);
        }

        // Title
        String title = "Тройной свап";
        int titleW = client.textRenderer.getWidth(title);
        context.drawText(client.textRenderer, title, cx - titleW / 2, cy - RADIUS - 30, 0xFFFFFFFF, true);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
