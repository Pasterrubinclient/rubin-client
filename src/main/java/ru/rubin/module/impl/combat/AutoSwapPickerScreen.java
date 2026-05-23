package ru.rubin.module.impl.combat;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/**
 * Simple picker: shows player inventory items, click to select for wheel slot.
 */
public class AutoSwapPickerScreen extends Screen {

    private final AutoSwapWheelScreen parent;
    private final AutoSwap module;
    private final int slotIndex;

    public AutoSwapPickerScreen(AutoSwapWheelScreen parent, AutoSwap module, int slotIndex) {
        super(Text.literal("Выбери предмет"));
        this.parent = parent;
        this.module = module;
        this.slotIndex = slotIndex;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xCC000000);

        if (client == null || client.player == null) return;

        String title = "Кликни на предмет для слота " + (slotIndex + 1);
        int tw = client.textRenderer.getWidth(title);
        context.drawText(client.textRenderer, title, width / 2 - tw / 2, 20, 0xFFFFFFFF, true);

        // Draw inventory grid (slots 0-35)
        int startX = width / 2 - 9 * 20 / 2;
        int startY = 50;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            int row = i < 9 ? 3 : (i - 9) / 9;
            int col = i < 9 ? i : (i - 9) % 9;
            int x = startX + col * 20;
            int y = startY + row * 20;

            boolean hovered = mouseX >= x && mouseX <= x + 18 && mouseY >= y && mouseY <= y + 18;
            context.fill(x, y, x + 18, y + 18, hovered ? 0xFF2A3A5A : 0xFF1A1A2E);
            context.fill(x, y, x + 18, y + 1, 0xFF333355);

            if (!stack.isEmpty()) {
                context.drawItem(stack, x + 1, y + 1);
            }
        }

        // Offhand slot
        ItemStack offhand = client.player.getOffHandStack();
        int ox = width / 2 - 9;
        int oy = startY + 4 * 20 + 10;
        boolean oHovered = mouseX >= ox && mouseX <= ox + 18 && mouseY >= oy && mouseY <= oy + 18;
        context.fill(ox, oy, ox + 18, oy + 18, oHovered ? 0xFF2A3A5A : 0xFF1A1A2E);
        if (!offhand.isEmpty()) {
            context.drawItem(offhand, ox + 1, oy + 1);
        }
        context.drawText(client.textRenderer, "Offhand", ox - 20, oy + 5, 0xFF888888, false);

        String hint = "ПКМ для отмены";
        int hw = client.textRenderer.getWidth(hint);
        context.drawText(client.textRenderer, hint, width / 2 - hw / 2, height - 20, 0xFF888888, false);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        if (button == 1) {
            client.setScreen(parent);
            return true;
        }

        if (button == 0 && client != null && client.player != null) {
            int startX = width / 2 - 9 * 20 / 2;
            int startY = 50;

            for (int i = 0; i < 36; i++) {
                int row = i < 9 ? 3 : (i - 9) / 9;
                int col = i < 9 ? i : (i - 9) % 9;
                int x = startX + col * 20;
                int y = startY + row * 20;

                if (mouseX >= x && mouseX <= x + 18 && mouseY >= y && mouseY <= y + 18) {
                    ItemStack stack = client.player.getInventory().getStack(i);
                    if (!stack.isEmpty()) {
                        module.setWheelItem(slotIndex, stack.copy());
                        client.setScreen(parent);
                        return true;
                    }
                }
            }

            // Offhand
            int ox = width / 2 - 9;
            int oy = startY + 4 * 20 + 10;
            if (mouseX >= ox && mouseX <= ox + 18 && mouseY >= oy && mouseY <= oy + 18) {
                ItemStack offhand = client.player.getOffHandStack();
                if (!offhand.isEmpty()) {
                    module.setWheelItem(slotIndex, offhand.copy());
                    client.setScreen(parent);
                    return true;
                }
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
