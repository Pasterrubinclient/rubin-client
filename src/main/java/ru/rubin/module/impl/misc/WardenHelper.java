package ru.rubin.module.impl.misc;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import ru.rubin.Rubin;
import ru.rubin.event.EventInit;
import ru.rubin.event.input.KeyInputEvent;
import ru.rubin.event.lifecycle.ClientTickEvent;
import ru.rubin.event.render.EventRender3D;
import ru.rubin.util.render.world.WorldRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import ru.rubin.module.api.Category;
import ru.rubin.module.api.IModule;
import ru.rubin.module.api.Module;
import ru.rubin.module.api.setting.Setting;
import ru.rubin.module.api.setting.impl.BindSettings;
import ru.rubin.module.api.setting.impl.BooleanSetting;
import ru.rubin.module.impl.visuals.Hud;
import ru.rubin.util.render.core.Renderer2D;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@IModule(name = "Warden Helper", description = "ESP сундуков Warden: таймеры, авто GPS", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class WardenHelper extends Module {

    public static BooleanSetting autoGps = new BooleanSetting("Авто GPS", true);
    public static BooleanSetting chatNotify = new BooleanSetting("Уведомления", true);
    public static BindSettings resetBind = new BindSettings("Ресет сундуков", -1);

    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,2}):(\\d{2})(?::(\\d{2}))?");
    private static final Pattern SECONDS_PATTERN = Pattern.compile("(\\d+)\\s*(с|s|сек|sec)");

    private final Map<BlockPos, ChestData> trackedChests = new ConcurrentHashMap<>();
    private final Set<BlockPos> gpsNotified = new HashSet<>();
    private final Set<BlockPos> chatNotified = new HashSet<>();
    private BlockPos lastGpsPos = null;

    private static final double WARDEN_CENTER_X = 2000.0;
    private static final double WARDEN_CENTER_Z = 2000.0;
    private static final double WARDEN_RADIUS = 250.0;

    public WardenHelper() {
        this.addSettings(new Setting[]{autoGps, chatNotify, resetBind});
    }

    @Override
    public void onEnable() {
        super.onEnable();
        trackedChests.clear();
        gpsNotified.clear();
        chatNotified.clear();
        lastGpsPos = null;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        trackedChests.clear();
        gpsNotified.clear();
        chatNotified.clear();
        lastGpsPos = null;
    }

    @EventInit
    public void onKey(KeyInputEvent event) {
        if (mc.player == null) return;
        if (resetBind.get() != -1 && event.key() == resetBind.get()) {
            resetChests();
        }
    }

    @EventInit
    public void onTick(ClientTickEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (!isInWardenZone()) return;

        scanChests();
        updateTimersFromHolograms();
        checkNotifications();
    }

    @EventInit
    public void onWorldRender(EventRender3D event) {
        if (mc.player == null || mc.world == null) return;
        if (!isInWardenZone()) return;
        if (trackedChests.isEmpty()) return;

        Vec3d camPos = mc.gameRenderer.getCamera().getCameraPos();
        net.minecraft.client.util.math.MatrixStack matrices = event.getMatrixStack();
        net.minecraft.client.util.BufferAllocator allocator = new net.minecraft.client.util.BufferAllocator(65536);
        net.minecraft.client.render.VertexConsumerProvider.Immediate immediate = net.minecraft.client.render.VertexConsumerProvider.immediate(allocator);
        try {
            net.minecraft.client.render.VertexConsumer buffer = immediate.getBuffer(ru.rubin.util.render.world.WorldRenderLayers.POSITION_COLOR_QUADS_NO_DEPTH());
            org.joml.Matrix4f matrix = matrices.peek().getPositionMatrix();
            for (Map.Entry<BlockPos, ChestData> entry : trackedChests.entrySet()) {
                BlockPos pos = entry.getKey();
                ChestData data = entry.getValue();
                int color = getTimerColor(data);
                float r = ((color >> 16) & 0xFF) / 255f, g = ((color >> 8) & 0xFF) / 255f, b = (color & 0xFF) / 255f, a = ((color >> 24) & 0xFF) / 255f;
                float x1 = (float)(pos.getX() - camPos.x), y1 = (float)(pos.getY() - camPos.y), z1 = (float)(pos.getZ() - camPos.z);
                float x2 = x1 + 1, y2 = y1 + 1, z2 = z1 + 1;
                buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a); buffer.vertex(matrix, x2, y1, z1).color(r, g, b, a); buffer.vertex(matrix, x2, y1, z2).color(r, g, b, a); buffer.vertex(matrix, x1, y1, z2).color(r, g, b, a);
                buffer.vertex(matrix, x1, y2, z1).color(r, g, b, a); buffer.vertex(matrix, x1, y2, z2).color(r, g, b, a); buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a); buffer.vertex(matrix, x2, y2, z1).color(r, g, b, a);
            }
            immediate.draw();
        } finally { allocator.close(); }
    }

    private void scanChests() {
        ChunkPos playerChunk = mc.player.getChunkPos();
        int renderDist = mc.options.getViewDistance().getValue();

        for (int cx = playerChunk.x - renderDist; cx <= playerChunk.x + renderDist; cx++) {
            for (int cz = playerChunk.z - renderDist; cz <= playerChunk.z + renderDist; cz++) {
                WorldChunk chunk = mc.world.getChunk(cx, cz);
                if (chunk == null) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof ChestBlockEntity) {
                        BlockPos pos = be.getPos();
                        trackedChests.computeIfAbsent(pos, ChestData::new);
                    }
                }
            }
        }
    }

    private void updateTimersFromHolograms() {
        for (Map.Entry<BlockPos, ChestData> entry : trackedChests.entrySet()) {
            BlockPos pos = entry.getKey();
            ChestData data = entry.getValue();
            Box searchBox = new Box(pos).expand(1.0, 3.0, 1.0);
            List<ArmorStandEntity> stands = mc.world.getEntitiesByClass(ArmorStandEntity.class, searchBox, ArmorStandEntity::isInvisible);

            boolean foundTimer = false;
            for (ArmorStandEntity stand : stands) {
                String name = stand.getDisplayName().getString();
                String clean = net.minecraft.util.Formatting.strip(name);
                if (clean == null) continue;
                long seconds = parseTimeToSeconds(clean);
                if (seconds >= 0) {
                    data.updateFromHologram(seconds);
                    foundTimer = true;
                    break;
                }
            }
            if (!foundTimer) data.hologramVisible = false;
        }
    }

    private void checkNotifications() {
        if (lastGpsPos != null && mc.player.getBlockPos().isWithinDistance(lastGpsPos, 10.0)) {
            mc.player.networkHandler.sendChatMessage(".gps off");
            lastGpsPos = null;
        }

        for (Map.Entry<BlockPos, ChestData> entry : trackedChests.entrySet()) {
            BlockPos pos = entry.getKey();
            ChestData data = entry.getValue();
            if (!data.hasTimer()) continue;

            float remaining = data.getRemainingSeconds();
            if (remaining <= 0.0f) {
                if (autoGps.get() && !gpsNotified.contains(pos)) {
                    mc.player.networkHandler.sendChatMessage(".gps set " + pos.getX() + " " + pos.getZ());
                    gpsNotified.add(pos);
                    lastGpsPos = pos;
                }
                if (chatNotify.get() && !chatNotified.contains(pos)) {
                    Rubin.get.manager.get(Hud.class).showNotification("on",
                            "Сундук готов! [" + pos.getX() + " " + pos.getZ() + "]", 5000L);
                    chatNotified.add(pos);
                }
            } else if (remaining <= 20.0f) {
                if (autoGps.get() && !gpsNotified.contains(pos)) {
                    mc.player.networkHandler.sendChatMessage(".gps set " + pos.getX() + " " + pos.getZ());
                    gpsNotified.add(pos);
                    lastGpsPos = pos;
                }
                if (chatNotify.get() && !chatNotified.contains(pos)) {
                    Rubin.get.manager.get(Hud.class).showNotification("warn",
                            "Сундук через " + (int) remaining + "с! [" + pos.getX() + " " + pos.getZ() + "]", 5000L);
                    chatNotified.add(pos);
                }
            }
        }
    }

    private long parseTimeToSeconds(String text) {
        Matcher timeMatcher = TIME_PATTERN.matcher(text);
        if (timeMatcher.find()) {
            if (timeMatcher.group(3) != null) {
                return Long.parseLong(timeMatcher.group(1)) * 3600 + Long.parseLong(timeMatcher.group(2)) * 60 + Long.parseLong(timeMatcher.group(3));
            }
            return Long.parseLong(timeMatcher.group(1)) * 60 + Long.parseLong(timeMatcher.group(2));
        }
        Matcher secMatcher = SECONDS_PATTERN.matcher(text);
        if (secMatcher.find()) return Long.parseLong(secMatcher.group(1));
        return -1;
    }

    private int getTimerColor(ChestData data) {
        if (!data.hasTimer()) return 0x78B4B4B4;
        float remaining = data.getRemainingSeconds();
        if (remaining <= 0.0f) return 0xDC00FF80;
        if (remaining > 120.0f) return 0xB4FF3C3C;
        if (remaining > 20.0f) return 0xB4FFDC32;
        return 0xB43CFF3C;
    }

    private boolean isInWardenZone() {
        double x = mc.player.getX();
        double z = mc.player.getZ();
        return Math.abs(x - WARDEN_CENTER_X) <= WARDEN_RADIUS && Math.abs(z - WARDEN_CENTER_Z) <= WARDEN_RADIUS;
    }

    private void resetChests() {
        trackedChests.clear();
        gpsNotified.clear();
        chatNotified.clear();
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("§7[§6WardenHelper§7] §fСундуки сброшены!"), false);
        }
    }

    private static class ChestData {
        final BlockPos pos;
        long readyAtMs = -1;
        boolean hologramVisible = false;

        ChestData(BlockPos pos) { this.pos = pos; }

        void updateFromHologram(long remainingSeconds) {
            hologramVisible = true;
            long newReadyAt = System.currentTimeMillis() + remainingSeconds * 1000;
            if (readyAtMs == -1) {
                readyAtMs = newReadyAt;
            } else {
                float diff = Math.abs(getRemainingSeconds() - remainingSeconds);
                if (diff > 3.0f) readyAtMs = newReadyAt;
            }
        }

        boolean hasTimer() { return readyAtMs > 0; }

        float getRemainingSeconds() {
            if (readyAtMs <= 0) return -1;
            return Math.max(0, (readyAtMs - System.currentTimeMillis()) / 1000.0f);
        }
    }
}
