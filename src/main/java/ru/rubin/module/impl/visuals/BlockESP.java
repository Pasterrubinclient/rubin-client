package ru.rubin.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.*;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;
import ru.rubin.event.EventInit;
import ru.rubin.event.render.EventRender3D;
import ru.rubin.module.api.Category;
import ru.rubin.module.api.IModule;
import ru.rubin.module.api.Module;
import ru.rubin.module.api.setting.Setting;
import ru.rubin.module.api.setting.impl.BooleanSetting;
import ru.rubin.util.render.world.WorldRenderLayers;

@IModule(name = "Block ESP", description = "Обводка блоков (сундуки, шалкеры, бочки)", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class BlockESP extends Module {

    public static BooleanSetting chest = new BooleanSetting("Сундук", true);
    public static BooleanSetting enderChest = new BooleanSetting("Эндер-Сундук", true);
    public static BooleanSetting shulker = new BooleanSetting("Шалкер", true);
    public static BooleanSetting barrel = new BooleanSetting("Бочка", true);
    public static BooleanSetting hopper = new BooleanSetting("Воронка", true);
    public static BooleanSetting furnace = new BooleanSetting("Печка", true);

    private static final int COLOR_CHEST = 0x40FFC254;
    private static final int COLOR_ENDER = 0x409931EE;
    private static final int COLOR_SHULKER = 0x40F67B7B;
    private static final int COLOR_BARREL = 0x40FAE13E;
    private static final int COLOR_HOPPER = 0x403E89FA;
    private static final int COLOR_FURNACE = 0x40737373;

    public BlockESP() {
        this.addSettings(new Setting[]{chest, enderChest, shulker, barrel, hopper, furnace});
    }

    @EventInit
    public void onRender(EventRender3D event) {
        if (mc.player == null || mc.world == null) return;

        MatrixStack matrices = event.getMatrixStack();
        Vec3d camPos = mc.gameRenderer.getCamera().getCameraPos();

        BufferAllocator allocator = new BufferAllocator(262144);
        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(allocator);

        try {
            RenderLayer layer = WorldRenderLayers.POSITION_COLOR_QUADS_NO_DEPTH();
            VertexConsumer buffer = immediate.getBuffer(layer);

            ChunkPos playerChunk = mc.player.getChunkPos();
            int renderDist = mc.options.getViewDistance().getValue();

            for (int cx = playerChunk.x - renderDist; cx <= playerChunk.x + renderDist; cx++) {
                for (int cz = playerChunk.z - renderDist; cz <= playerChunk.z + renderDist; cz++) {
                    WorldChunk chunk = mc.world.getChunk(cx, cz);
                    if (chunk == null) continue;

                    for (BlockEntity be : chunk.getBlockEntities().values()) {
                        if (be == null) continue;
                        int color = getColor(be);
                        if (color == 0) continue;

                        BlockPos pos = be.getPos();
                        float x1 = (float) (pos.getX() - camPos.x);
                        float y1 = (float) (pos.getY() - camPos.y);
                        float z1 = (float) (pos.getZ() - camPos.z);
                        float x2 = x1 + 1.0f;
                        float y2 = y1 + 1.0f;
                        float z2 = z1 + 1.0f;

                        Matrix4f matrix = matrices.peek().getPositionMatrix();
                        drawBox(buffer, matrix, x1, y1, z1, x2, y2, z2, color);
                    }
                }
            }

            immediate.draw();
        } finally {
            allocator.close();
        }
    }

    private int getColor(BlockEntity be) {
        if (be instanceof ChestBlockEntity && chest.get()) return COLOR_CHEST;
        if (be instanceof EnderChestBlockEntity && enderChest.get()) return COLOR_ENDER;
        if (be instanceof ShulkerBoxBlockEntity && shulker.get()) return COLOR_SHULKER;
        if (be instanceof BarrelBlockEntity && barrel.get()) return COLOR_BARREL;
        if (be instanceof HopperBlockEntity && hopper.get()) return COLOR_HOPPER;
        if (be instanceof AbstractFurnaceBlockEntity && furnace.get()) return COLOR_FURNACE;
        return 0;
    }

    private void drawBox(VertexConsumer buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, int color) {
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = ((color >> 24) & 0xFF) / 255.0f;

        // Bottom face (Y-)
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a);
        buffer.vertex(matrix, x2, y1, z1).color(r, g, b, a);
        buffer.vertex(matrix, x2, y1, z2).color(r, g, b, a);
        buffer.vertex(matrix, x1, y1, z2).color(r, g, b, a);
        // Top face (Y+)
        buffer.vertex(matrix, x1, y2, z2).color(r, g, b, a);
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a);
        buffer.vertex(matrix, x2, y2, z1).color(r, g, b, a);
        buffer.vertex(matrix, x1, y2, z1).color(r, g, b, a);
        // North face (Z-)
        buffer.vertex(matrix, x2, y1, z1).color(r, g, b, a);
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a);
        buffer.vertex(matrix, x1, y2, z1).color(r, g, b, a);
        buffer.vertex(matrix, x2, y2, z1).color(r, g, b, a);
        // South face (Z+)
        buffer.vertex(matrix, x1, y1, z2).color(r, g, b, a);
        buffer.vertex(matrix, x2, y1, z2).color(r, g, b, a);
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a);
        buffer.vertex(matrix, x1, y2, z2).color(r, g, b, a);
        // West face (X-)
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a);
        buffer.vertex(matrix, x1, y1, z2).color(r, g, b, a);
        buffer.vertex(matrix, x1, y2, z2).color(r, g, b, a);
        buffer.vertex(matrix, x1, y2, z1).color(r, g, b, a);
        // East face (X+)
        buffer.vertex(matrix, x2, y1, z2).color(r, g, b, a);
        buffer.vertex(matrix, x2, y1, z1).color(r, g, b, a);
        buffer.vertex(matrix, x2, y2, z1).color(r, g, b, a);
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a);
    }
}
