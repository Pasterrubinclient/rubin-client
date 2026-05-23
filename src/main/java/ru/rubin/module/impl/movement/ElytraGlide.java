package ru.rubin.module.impl.movement;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;
import ru.rubin.event.EventInit;
import ru.rubin.event.impl.EventPacket;
import ru.rubin.event.lifecycle.ClientTickEvent;
import ru.rubin.event.player.EventMotion;
import ru.rubin.module.api.Category;
import ru.rubin.module.api.IModule;
import ru.rubin.module.api.Module;
import ru.rubin.module.api.setting.Setting;
import ru.rubin.module.api.setting.impl.BooleanSetting;
import ru.rubin.module.api.setting.impl.ModeSetting;
import ru.rubin.module.api.setting.impl.SliderSetting;
import ru.rubin.module.impl.combat.auraProcess.rotationProcess.impl.FreeLookUtil;

@IModule(name = "Elytra Glide", description = "Прерывистое зависание в воздухе", category = Category.Movement, bind = -1)
@Environment(EnvType.CLIENT)
public class ElytraGlide extends Module {

    public static ModeSetting preset = new ModeSetting("Пресет", "Lony", "Lony", "Custom");
    public static SliderSetting speed = new SliderSetting("Скорость", 1.0F, 0.0F, 1.0F, 0.001F, false)
            .hidden(() -> preset.is("Lony"));
    public static BooleanSetting yMotion = new BooleanSetting("Менять Y motion", false)
            .hidden(() -> preset.is("Lony"));
    public static SliderSetting motionY = new SliderSetting("Motion Y", 0.0F, -1.0F, 1.0F, 0.001F, false)
            .hidden(() -> preset.is("Lony") || !yMotion.get());

    private int tick = 0;

    // Lony preset values
    private static final float LONY_SPEED = 1.0f;
    private static final boolean LONY_Y_MOTION = true;
    private static final float LONY_MOTION_Y = -0.017f;

    public ElytraGlide() {
        this.addSettings(new Setting[]{preset, speed, yMotion, motionY});
    }

    @Override
    public void onEnable() {
        super.onEnable();
        tick = 0;
        if (mc.player != null && mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, false));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(false, false));
        }
    }

    @EventInit
    public void onTick(ClientTickEvent event) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        // Post motion: send ground packets
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, false));
        if (tick % 2 == 0) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(false, false));
        }

        // Apply velocity
        boolean isLony = preset.is("Lony");
        double factor = 0.03;
        if (tick % 2 == 0) {
            factor = mc.player.isGliding() ? 0.085 : 0.03;
        }
        factor *= isLony ? LONY_SPEED : speed.get();

        float yaw = (FreeLookUtil.active ? FreeLookUtil.freeYaw : mc.player.getYaw()) + 90f;
        float yawRad = yaw * 0.017453292f;
        float sin = MathHelper.sin(yawRad);
        float cos = MathHelper.cos(yawRad);

        mc.player.setVelocity(
                mc.player.getVelocity().x + factor * cos,
                mc.player.getVelocity().y,
                mc.player.getVelocity().z + factor * sin
        );

        // Y motion
        boolean changeY = isLony ? LONY_Y_MOTION : yMotion.get();
        if (changeY && !mc.player.isGliding()
                && mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
            float yMot = isLony ? LONY_MOTION_Y : motionY.get();
            mc.player.setVelocity(
                    mc.player.getVelocity().x,
                    mc.player.getVelocity().y + yMot,
                    mc.player.getVelocity().z
            );
        }

        tick++;
    }

    @EventInit
    public void onPacket(EventPacket event) {
        if (event.getPacket() instanceof net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket) {
            if (tick % 2 == 1) {
                tick++;
            }
        }
    }
}
