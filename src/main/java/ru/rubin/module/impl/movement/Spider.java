package ru.rubin.module.impl.movement;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import ru.rubin.event.EventInit;
import ru.rubin.event.lifecycle.ClientTickEvent;
import ru.rubin.module.api.Category;
import ru.rubin.module.api.IModule;
import ru.rubin.module.api.Module;
import ru.rubin.module.api.setting.Setting;
import ru.rubin.module.api.setting.impl.ModeSetting;

@IModule(name = "Spider", description = "Карабканье по стенам", category = Category.Movement, bind = -1)
@Environment(EnvType.CLIENT)
public class Spider extends Module {

    public static ModeSetting mode = new ModeSetting("Режим", "FunTime", "FunTime", "SpookyTime", "FunSky", "Water", "Slime Block", "FunTime Fly");

    private long lastActionTime;
    private int slimeCounter;
    private long lastTimerReset;

    public Spider() {
        this.addSettings(new Setting[]{mode});
    }

    @Override
    public void onDisable() {
        super.onDisable();
        mc.options.jumpKey.setPressed(false);
        mc.options.sneakKey.setPressed(false);
        lastActionTime = 0L;
        slimeCounter = 0;
        lastTimerReset = 0L;
    }

    @EventInit
    public void onTick(ClientTickEvent event) {
        if (!this.enable || mc.player == null || mc.world == null) return;

        switch (mode.get()) {
            case "FunTime" -> handleFunTime();
            case "SpookyTime" -> handleSpookyTime();
            case "FunSky" -> handleFunSky();
            case "Water" -> handleWater();
            case "Slime Block" -> handleSlimeBlock();
            case "FunTime Fly" -> handleFunTimeFly();
        }
    }

    private void handleFunTime() {
        mc.options.jumpKey.setPressed(mc.options.forwardKey.isPressed());

        if (!mc.player.horizontalCollision) return;

        long now = System.currentTimeMillis();
        if (now - lastTimerReset < 110L) return;

        mc.player.setOnGround(true);
        mc.player.jump();

        int leverSlot = findHotbarItem(Items.LEVER);
        if (leverSlot == -1) {
            mc.player.sendMessage(net.minecraft.text.Text.literal("§cSpider: нужны рычаги!"), false);
            this.enable = false;
            this.onDisable();
            return;
        }

        useLeverAtWall(leverSlot);
        mc.player.fallDistance = 0.0f;
        lastTimerReset = now;
    }

    private void handleSpookyTime() {
        int bucketSlot = findHotbarItem(Items.WATER_BUCKET);
        if (bucketSlot == -1) return;
        if (!mc.player.horizontalCollision) return;

        mc.player.getInventory().setSelectedSlot(bucketSlot);
        mc.player.setVelocity(mc.player.getVelocity().x, 0.29, mc.player.getVelocity().z);
    }

    private void handleFunSky() {
        boolean collision = mc.player.horizontalCollision;

        if (!collision) {
            mc.options.sneakKey.setPressed(false);
            mc.options.jumpKey.setPressed(false);
            return;
        }

        int bucketSlot = findHotbarItem(Items.WATER_BUCKET);
        if (bucketSlot == -1) return;

        if (mc.player.isSubmergedInWater()) {
            mc.player.setVelocity(mc.player.getVelocity().x, 0.46, mc.player.getVelocity().z);
            return;
        }

        if (mc.player.isOnGround()) {
            lastActionTime = 0L;
            lastTimerReset = System.currentTimeMillis();
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastTimerReset < 120L) return;

        long interval = calculateInterval();
        if (now - lastActionTime < interval) return;

        mc.options.sneakKey.setPressed(true);
        useWaterBucketUp(bucketSlot);
        mc.options.jumpKey.setPressed(true);
        lastActionTime = now;
    }

    private void handleWater() {
        long now = System.currentTimeMillis();
        if (now - lastTimerReset < 110L) return;

        int bucketSlot = findHotbarItem(Items.WATER_BUCKET);
        if (bucketSlot == -1) return;

        mc.player.getInventory().setSelectedSlot(bucketSlot);
        if (mc.player.horizontalCollision) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        }
    }

    private void handleSlimeBlock() {
        if (!hasAdjacentSlimeBlock()) return;
        if (!mc.player.horizontalCollision) return;
        if (mc.player.getVelocity().y > -1.0) return;

        if (!(mc.crosshairTarget instanceof BlockHitResult blockHit)) return;

        BlockPos hitPos = blockHit.getBlockPos();
        if (mc.world.getBlockState(hitPos).getBlock() == Blocks.SLIME_BLOCK) return;

        int slimeSlot = findHotbarSlimeBlock();
        if (slimeSlot == -1) return;

        mc.player.getInventory().setSelectedSlot(slimeSlot);
        mc.player.setPitch(54.0f);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHit);
        mc.player.swingHand(Hand.MAIN_HAND);

        if (slimeCounter >= 1) {
            mc.player.setVelocity(mc.player.getVelocity().x, 0.63, mc.player.getVelocity().z);
            slimeCounter = 0;
        } else {
            slimeCounter++;
        }
    }

    private void handleFunTimeFly() {
        if (!mc.player.horizontalCollision) return;

        long now = System.currentTimeMillis();
        if (now - lastTimerReset < 1L) return;

        mc.player.setOnGround(true);
        mc.player.jump();

        int rodSlot = findOrSwapItem(Items.LIGHTNING_ROD);
        if (rodSlot != -1) {
            placeRodAbove(rodSlot);
            mc.player.fallDistance = 0.0f;
            lastTimerReset = now;
        } else {
            mc.player.sendMessage(net.minecraft.text.Text.literal("§cSpider: нужен громоотвод!"), false);
        }
    }

    // --- Utility methods ---

    private int findHotbarItem(Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) return i;
        }
        return -1;
    }

    private int findInventoryItem(Item item) {
        for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) return i;
        }
        return -1;
    }

    private int findOrSwapItem(Item item) {
        int hotbar = findHotbarItem(item);
        if (hotbar != -1) return hotbar;

        int inv = findInventoryItem(item);
        if (inv == -1) return -1;

        int selectedSlot = mc.player.getInventory().getSelectedSlot();
        mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId, inv, selectedSlot,
                SlotActionType.SWAP, mc.player);
        return selectedSlot;
    }

    private int findHotbarSlimeBlock() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.SLIME_BLOCK) return i;
        }
        return -1;
    }

    private boolean hasAdjacentSlimeBlock() {
        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos[] adjacent = {playerPos.north(), playerPos.south(), playerPos.east(), playerPos.west()};
        for (BlockPos pos : adjacent) {
            if (mc.world.getBlockState(pos).getBlock() == Blocks.SLIME_BLOCK) return true;
        }
        return false;
    }

    private void useLeverAtWall(int slot) {
        int prevSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(slot);

        float yaw = mc.player.getYaw();
        BlockHitResult hit = raycastWall(yaw, 75.0f, 4.5);
        if (hit.getType() == HitResult.Type.BLOCK) {
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        }

        mc.player.getInventory().setSelectedSlot(prevSlot);
    }

    private void useWaterBucketUp(int slot) {
        int prevSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(slot);

        float prevPitch = mc.player.getPitch();
        mc.player.setPitch(-90.0f);
        mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(
                Hand.MAIN_HAND, 0, mc.player.getYaw(), mc.player.getPitch()));
        mc.player.setVelocity(mc.player.getVelocity().x, 0.45, mc.player.getVelocity().z);
        mc.player.setPitch(prevPitch);

        mc.player.getInventory().setSelectedSlot(prevSlot);
    }

    private void placeRodAbove(int slot) {
        int prevSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(slot);

        BlockPos playerPos = mc.player.getBlockPos();
        for (int i = 1; i <= 2; i++) {
            BlockPos above = playerPos.up(i);
            if (mc.world.getBlockState(above).isAir()) {
                Vec3d center = new Vec3d(above.getX() + 0.5, above.getY() + 0.5, above.getZ() + 0.5);
                BlockHitResult hit = new BlockHitResult(center, Direction.UP, above.down(), false);
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        }

        mc.player.getInventory().setSelectedSlot(prevSlot);
    }

    private BlockHitResult raycastWall(float yaw, float pitch, double distance) {
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d direction = Vec3d.fromPolar(pitch, yaw).normalize();
        Vec3d end = eyePos.add(direction.multiply(distance));
        RaycastContext context = new RaycastContext(eyePos, end,
                RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player);
        return mc.world.raycast(context);
    }

    private long calculateInterval() {
        double height = getHeightAboveGround();
        if (height < 5.0) return 450L;
        if (height < 20.0) return 550L;
        return 650L;
    }

    private double getHeightAboveGround() {
        double y = mc.player.getY();
        while (y > mc.world.getBottomY()) {
            BlockPos pos = BlockPos.ofFloored(mc.player.getX(), y, mc.player.getZ());
            if (!mc.world.getBlockState(pos).isAir()) {
                return Math.max(mc.player.getY() - (y + 1.0), 0.0);
            }
            y -= 0.1;
        }
        return 0.0;
    }
}
