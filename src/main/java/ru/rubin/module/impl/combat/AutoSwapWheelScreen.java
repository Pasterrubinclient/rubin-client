package ru.rubin.module.impl.combat;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Wheel UI for triple swap from rubin-client-1.21.11 (3).
 * - 3 configurable slots arranged in circle
 * - Empty slots show "+" and can be filled by clicking (opens inventory)
 * - Right click clears a slot
 * - Hold bind → hover → release = swap
 */
public class AutoSwapWheelScreen extends Screen {

    private static final int SEGMENT_COUNT = 3;
    private static final float PANEL_W = 66f;
    private static final float PANEL_H = 66f;
    private static final float PANEL_RADIUS = 6f;
    private static final int BG_PANEL = 0xE610141D;
    private static final int BG_SLOT = 0xFF121925;
    private static final int BG_SLOT_EMPTY = 0xCC0E131C;
    private static final int BG_HOVERED = 0xFF1E2A3D;
    private static final int BORDER = 0x267A72FF;
    private static final int TEXT_PRIMARY = 0xFFF2F4FF;
    private static final int TEXT_HINT = 0xFF9AA6C0;

    private final AutoSwap module;
    private boolean firstTick = true;
    private boolean keyWasHeld;
    private int hoveredSlot = -1;

    public AutoSwapWheelScreen(AutoSwap module) {
        super(Text.literal("AutoSwap"));
        this.module = module;
    }

    @Override
    public void tick() {
        if (client == null || client.player == null) {
            close();
            return;
        }

        boolean held = isBindHeld();
        if (firstTick) {
            firstTick = false;
            keyWasHeld = held;
            return;
        }

        // On release: swap to hovered item
        if (keyWasHeld && !held) {
            if (hoveredSlot >= 0 && hoveredSlot < SEGMENT_COUNT) {
                ItemStack stack = module.getWheelItem(hoveredSlot);
                if (!stack.isEmpty()) {
                    module.tripleSwapItem(stack.getItem(), stack.getName().getString());
                }
            }
            close();
            return;
        }

        keyWasHeld = held;
        if (!held) {
            close();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Dark overlay
        context.fill(0, 0, width, height, 0x65000000);

        float cx = width * 0.5f;
        float cy = height * 0.5f;

        hoveredSlot = getHoveredSegment(mouseX, mouseY);

        for (int i = 0; i < SEGMENT_COUNT; i++) {
            float[] pos = segmentPos(i, cx, cy);
            float sx = pos[0];
            float sy = pos[1];
            boolean hovered = hoveredSlot == i;

            int bg = hovered ? BG_HOVERED : BG_SLOT_EMPTY;
            ItemStack stack = module.getWheelItem(i);
            if (!stack.isEmpty()) bg = hovered ? BG_HOVERED : BG_SLOT;

            // Panel
            int x1 = Math.round(sx);
            int y1 = Math.round(sy);
            int x2 = Math.round(sx + PANEL_W);
            int y2 = Math.round(sy + PANEL_H);
            context.fill(x1, y1, x2, y2, BG_PANEL);
            context.fill(x1 + 2, y1 + 2, x2 - 2, y2 - 2, bg);

            // Border
            context.fill(x1, y1, x2, y1 + 1, BORDER);
            context.fill(x1, y2 - 1, x2, y2, BORDER);
            context.fill(x1, y1, x1 + 1, y2, BORDER);
            context.fill(x2 - 1, y1, x2, y2, BORDER);

            if (stack.isEmpty()) {
                // Draw "+"
                String plus = "+";
                int pw = client.textRenderer.getWidth(plus);
                context.drawText(client.textRenderer, plus,
                        x1 + (int)(PANEL_W / 2) - pw / 2,
                        y1 + (int)(PANEL_H / 2) - 4, TEXT_PRIMARY, false);
                // "Empty" text
                String empty = "Empty";
                int ew = client.textRenderer.getWidth(empty);
                context.drawText(client.textRenderer, empty,
                        x1 + (int)(PANEL_W / 2) - ew / 2,
                        y1 + (int)(PANEL_H) - 14, TEXT_HINT, false);
            } else {
                // Draw item
                context.drawItem(stack,
                        x1 + (int)(PANEL_W / 2) - 8,
                        y1 + 14);
                // Item name
                String name = stack.getName().getString();
                if (name.length() > 12) name = name.substring(0, 11) + "..";
                int nw = client.textRenderer.getWidth(name);
                context.drawText(client.textRenderer, name,
                        x1 + (int)(PANEL_W / 2) - nw / 2,
                        y1 + (int)(PANEL_H) - 14, TEXT_PRIMARY, false);
            }
        }

        // Hint
        String hint = "Зажми бинд, наведи, отпусти для свапа";
        int hw = client.textRenderer.getWidth(hint);
        context.drawText(client.textRenderer, hint,
                (int)(cx) - hw / 2, height - 24, TEXT_HINT, false);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        int mouseX = (int) click.x();
        int mouseY = (int) click.y();
        int button = click.button();
        int hovered = getHoveredSegment((int) mouseX, (int) mouseY);
        if (hovered >= 0 && hovered < SEGMENT_COUNT) {
            if (button == 0) {
                // Left click: open inventory to pick item for this slot
                ItemStack current = module.getWheelItem(hovered);
                if (current.isEmpty()) {
                    client.setScreen(new AutoSwapPickerScreen(this, module, hovered));
                    return true;
                }
            } else if (button == 1) {
                // Right click: clear slot
                module.setWheelItem(hovered, ItemStack.EMPTY);
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int hovered = getHoveredSegment((int) mouseX, (int) mouseY);
        if (hovered >= 0 && hovered < SEGMENT_COUNT) {
            ItemStack stack = module.getWheelItem(hovered);
            if (!stack.isEmpty()) {
                module.tripleSwapItem(stack.getItem(), stack.getName().getString());
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

    private boolean isBindHeld() {
        if (client == null || client.getWindow() == null) return false;
        int key = module.getBindKey();
        long handle = client.getWindow().getHandle();
        if (key >= GLFW.GLFW_MOUSE_BUTTON_1 && key <= GLFW.GLFW_MOUSE_BUTTON_LAST) {
            return GLFW.glfwGetMouseButton(handle, key) == GLFW.GLFW_PRESS;
        }
        if (key >= GLFW.GLFW_KEY_SPACE && key <= GLFW.GLFW_KEY_LAST) {
            return GLFW.glfwGetKey(handle, key) == GLFW.GLFW_PRESS;
        }
        return false;
    }

    private int getHoveredSegment(int mouseX, int mouseY) {
        float cx = width * 0.5f;
        float cy = height * 0.5f;
        for (int i = 0; i < SEGMENT_COUNT; i++) {
            float[] pos = segmentPos(i, cx, cy);
            if (mouseX >= pos[0] && mouseX <= pos[0] + PANEL_W
                    && mouseY >= pos[1] && mouseY <= pos[1] + PANEL_H) {
                return i;
            }
        }
        return -1;
    }

    private float[] segmentPos(int index, float cx, float cy) {
        float angle = switch (index) {
            case 0 -> -90f;
            case 1 -> 150f;
            default -> 30f;
        };
        float radius = 86f;
        float x = cx + (float) Math.cos(Math.toRadians(angle)) * radius - PANEL_W * 0.5f;
        float y = cy + (float) Math.sin(Math.toRadians(angle)) * radius - PANEL_H * 0.5f;
        return new float[]{x, y};
    }
}
