package ru.rubin.module.impl.movement;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.rubin.Rubin;
import ru.rubin.event.EventInit;
import ru.rubin.event.lifecycle.ClientTickEvent;
import ru.rubin.event.player.EventInput;
import ru.rubin.module.api.Category;
import ru.rubin.module.api.IModule;
import ru.rubin.module.api.Module;
import ru.rubin.module.api.setting.Setting;
import ru.rubin.module.api.setting.impl.BooleanSetting;
import ru.rubin.module.api.setting.impl.ModeSetting;
import ru.rubin.module.api.setting.impl.SliderSetting;
import ru.rubin.module.impl.combat.HitAura;

@IModule(name = "Target Strafe", description = "Стрейф вокруг цели", category = Category.Movement, bind = -1)
@Environment(EnvType.CLIENT)
public class TargetStrafe extends Module {

    public static ModeSetting mode = new ModeSetting("Режим", "Matrix", "Matrix", "Grim");
    public static ModeSetting grimPoint = new ModeSetting("Grim Point", "Cube", "Cube", "Center", "Circle")
            .hidden(() -> mode.is("Matrix"));
    public static ModeSetting matrixPoint = new ModeSetting("Matrix Point", "Circle", "Circle", "Cube")
            .hidden(() -> mode.is("Grim"));
    public static ModeSetting direction = new ModeSetting("Направление", "Clockwise", "Clockwise", "Counterclockwise", "Random");
    public static BooleanSetting autoJump = new BooleanSetting("Авто прыжок", true);
    public static BooleanSetting onlyKeyPressed = new BooleanSetting("Только при движении", false);
    public static BooleanSetting inFrontOfTarget = new BooleanSetting("Перед целью", false);
    public static SliderSetting grimRadius = new SliderSetting("Grim радиус", 0.87F, 0.1F, 1.5F, 0.01F, false)
            .hidden(() -> mode.is("Matrix"));
    public static SliderSetting matrixRadius = new SliderSetting("Matrix радиус", 2.5F, 0.1F, 7.0F, 0.01F, false)
            .hidden(() -> mode.is("Grim"));
    public static SliderSetting matrixSpeed = new SliderSetting("Matrix скорость", 0.3F, 0.1F, 1.0F, 0.01F, false)
            .hidden(() -> mode.is("Grim"));

    private int grimPointIndex = 0;

    public TargetStrafe() {
        this.addSettings(new Setting[]{mode, grimPoint, matrixPoint, direction, autoJump, onlyKeyPressed, inFrontOfTarget, grimRadius, matrixRadius, matrixSpeed});
    }

    @Override
    public void onEnable() {
        super.onEnable();
        grimPointIndex = 0;
    }

    @EventInit
    public void onInput(EventInput event) {
        if (mc.player == null || mc.world == null) return;
        if (!mode.is("Grim")) return;

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) return;

        if (onlyKeyPressed.get() && !isMoving()) return;

        Vec3d playerPos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        Vec3d targetPos = new Vec3d(target.getX(), target.getY(), target.getZ());
        double r = grimRadius.get();
        int dir = getDirectionMultiplier();

        Vec3d nextPoint;

        if (inFrontOfTarget.get()) {
            float targetYaw = target.getYaw();
            if (grimPoint.is("Center")) {
                nextPoint = targetPos.add(
                        -Math.sin(Math.toRadians(targetYaw)) * r * dir, 0,
                        Math.cos(Math.toRadians(targetYaw)) * r * dir);
            } else {
                double offset = Math.cos(System.currentTimeMillis() / 500.0) * r * dir;
                nextPoint = targetPos.add(
                        -Math.sin(Math.toRadians(targetYaw)) * r + Math.cos(Math.toRadians(targetYaw)) * offset, 0,
                        Math.cos(Math.toRadians(targetYaw)) * r + Math.sin(Math.toRadians(targetYaw)) * offset);
            }
        } else if (grimPoint.is("Cube")) {
            Vec3d[] points = getCubePoints(targetPos, playerPos, r);
            if (playerPos.distanceTo(points[grimPointIndex]) < 0.5)
                grimPointIndex = (grimPointIndex + dir + points.length) % points.length;
            nextPoint = points[grimPointIndex];
        } else if (grimPoint.is("Circle")) {
            double baseAngle = (System.currentTimeMillis() % 3600L) / 3600.0 * 4 * Math.PI;
            double angle = dir > 0 ? baseAngle : (2 * Math.PI - baseAngle);
            nextPoint = new Vec3d(targetPos.x + Math.cos(angle) * r, playerPos.y, targetPos.z + Math.sin(angle) * r);
        } else {
            nextPoint = new Vec3d(targetPos.x, playerPos.y, targetPos.z);
        }

        Vec3d dirVec = nextPoint.subtract(playerPos).normalize();
        float desiredYaw = (float) Math.toDegrees(Math.atan2(dirVec.z, dirVec.x)) - 90f;

        // Convert desired movement direction to forward/strafe relative to player yaw
        float playerYaw = mc.player.getYaw();
        float angleDiff = MathHelper.wrapDegrees(desiredYaw - playerYaw);

        float fwd = (float) Math.cos(Math.toRadians(angleDiff));
        float strafe = (float) -Math.sin(Math.toRadians(angleDiff));

        event.setForward(fwd);
        event.setStrafe(strafe);

        if (autoJump.get() && mc.player.isOnGround()) {
            event.setJump(true);
        }
    }

    @EventInit
    public void onTick(ClientTickEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (!mode.is("Matrix")) return;

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) return;

        if (onlyKeyPressed.get() && !isMoving()) return;

        if (autoJump.get() && mc.player.isOnGround()) {
            mc.player.jump();
        }

        Vec3d playerPos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        Vec3d targetPos = new Vec3d(target.getX(), target.getY(), target.getZ());
        double r = matrixRadius.get();
        int dir = getDirectionMultiplier();
        double motionSpeed = matrixSpeed.get();

        if (inFrontOfTarget.get()) {
            float targetYaw = target.getYaw();
            double x = targetPos.x - Math.sin(Math.toRadians(targetYaw)) * r * dir;
            double z = targetPos.z + Math.cos(Math.toRadians(targetYaw)) * r * dir;
            float yaw = (float) Math.toDegrees(Math.atan2(z - playerPos.z, x - playerPos.x)) - 90f;
            mc.player.setVelocity(
                    -Math.sin(Math.toRadians(yaw)) * motionSpeed,
                    mc.player.getVelocity().y,
                    Math.cos(Math.toRadians(yaw)) * motionSpeed);
            return;
        }

        if (matrixPoint.is("Cube")) {
            Vec3d[] points = getCubePoints(targetPos, playerPos, r);
            if (playerPos.distanceTo(points[grimPointIndex]) < 0.5)
                grimPointIndex = (grimPointIndex + dir + points.length) % points.length;
            Vec3d next = points[grimPointIndex];
            Vec3d dv = next.subtract(playerPos).normalize();
            float yaw = (float) Math.toDegrees(Math.atan2(dv.z, dv.x)) - 90f;
            mc.player.setVelocity(
                    -Math.sin(Math.toRadians(yaw)) * motionSpeed,
                    mc.player.getVelocity().y,
                    Math.cos(Math.toRadians(yaw)) * motionSpeed);
        } else {
            double angle = Math.atan2(playerPos.z - targetPos.z, playerPos.x - targetPos.x);
            angle += dir * motionSpeed / Math.max(playerPos.distanceTo(targetPos), r);
            double x = targetPos.x + r * Math.cos(angle);
            double z = targetPos.z + r * Math.sin(angle);
            float yaw = (float) Math.toDegrees(Math.atan2(z - playerPos.z, x - playerPos.x)) - 90f;
            mc.player.setVelocity(
                    -Math.sin(Math.toRadians(yaw)) * motionSpeed,
                    mc.player.getVelocity().y,
                    Math.cos(Math.toRadians(yaw)) * motionSpeed);
        }
    }

    private LivingEntity getTarget() {
        return HitAura.target;
    }

    private int getDirectionMultiplier() {
        if (direction.is("Counterclockwise")) return -1;
        if (direction.is("Random")) return (System.currentTimeMillis() / 3000) % 2 == 0 ? 1 : -1;
        return 1;
    }

    private Vec3d[] getCubePoints(Vec3d targetPos, Vec3d playerPos, double r) {
        return new Vec3d[]{
                new Vec3d(targetPos.x - r, playerPos.y, targetPos.z - r),
                new Vec3d(targetPos.x - r, playerPos.y, targetPos.z + r),
                new Vec3d(targetPos.x + r, playerPos.y, targetPos.z + r),
                new Vec3d(targetPos.x + r, playerPos.y, targetPos.z - r)
        };
    }

    private boolean isMoving() {
        return mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed()
                || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed();
    }
}
