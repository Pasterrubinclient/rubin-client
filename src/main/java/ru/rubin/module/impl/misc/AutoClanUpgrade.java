package ru.rubin.module.impl.misc;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import ru.rubin.event.EventInit;
import ru.rubin.event.impl.EventUpdate;
import ru.rubin.module.api.Category;
import ru.rubin.module.api.IModule;
import ru.rubin.module.api.Module;
import ru.rubin.module.impl.combat.auraProcess.rotationProcess.impl.Rotation;
import ru.rubin.module.impl.combat.auraProcess.rotationProcess.impl.RotationProcess;
import ru.rubin.util.other.TimerUtil;
import ru.rubin.util.player.InvUtil;

@IModule(name = "Auto Clan Upgrade", description = "Качает клан автоматически", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class AutoClanUpgrade extends Module {

    private final TimerUtil timer = new TimerUtil();

    @EventInit
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        // Check for torch in hotbar
        int slot = findTorchSlot();
        if (mc.world.getRegistryKey().getValue().getPath().equals("lobby")) {
            if (mc.player.age % 200 == 0) {
                mc.player.sendMessage(net.minecraft.text.Text.literal("§e[AutoClanUpgrade] §fТепнитесь на РТП"), false);
            }
            return;
        }

        if (slot == -1) {
            if (mc.player.age % 200 == 0) {
                mc.player.sendMessage(net.minecraft.text.Text.literal("§e[AutoClanUpgrade] §cНужен факел в хотбаре"), false);
            }
            return;
        }

        BlockPos pos = mc.player.getBlockPos().down();
        BlockPos torchPos = pos.up();

        // Check if block below is replaceable (can place torch)
        if (mc.world.getBlockState(pos).isFullCube(mc.world, pos)) {
            // Rotate down
            if (mc.player.age % 1 == 0) {
                float yaw = (float) (Math.cos(System.currentTimeMillis() / 140.0) * 2.0);
                float pitch = 87.5f + (float) (Math.sin(System.currentTimeMillis() / 140.0) * 2.0);
                RotationProcess.update(new Rotation(yaw, pitch), 255.0f, 255.0f, 0, 100);
            }

            if (timer.hasReached(5)) {
                timer.reset();

                // Place torch if air above
                if (mc.world.getBlockState(torchPos).isAir()) {
                    mc.player.getInventory().setSelectedSlot(slot);
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                            new BlockHitResult(pos.toCenterPos(), Direction.UP, pos, false));
                }

                // Break torch if it's there
                if (mc.world.getBlockState(torchPos).getBlock() == Blocks.TORCH) {
                    mc.interactionManager.attackBlock(torchPos, Direction.DOWN);
                    mc.player.swingHand(Hand.MAIN_HAND);
                }
            }
        }
    }

    private int findTorchSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.TORCH) {
                return i;
            }
        }
        return -1;
    }
}
