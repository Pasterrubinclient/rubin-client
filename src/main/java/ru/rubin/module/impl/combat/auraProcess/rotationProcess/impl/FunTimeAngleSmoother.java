package ru.rubin.module.impl.combat.auraProcess.rotationProcess.impl;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.rubin.util.other.IMinecraft;

import java.security.SecureRandom;


@Environment(EnvType.CLIENT)
public class FunTimeAngleSmoother implements IMinecraft {

    private int count;
    private long lastAttackTime;
    private final SecureRandom random = new SecureRandom();

    public void onAttack() {
        count++;
        lastAttackTime = System.currentTimeMillis();
    }

    public boolean canAttack(long cooldownMs) {
        return System.currentTimeMillis() - lastAttackTime >= cooldownMs;
    }

    /**
     * брбр потопим
     *
     * @param currentYaw   current yaw
     * @param currentPitch current pitch
     * @param targetYaw    desired yaw
     * @param targetPitch  desired pitch
     * @param entity       target entity (жескибупас)
     * @return float[2] = {yaw, nитч тытыч}
     */
    public float[] limitAngleChange(float currentYaw, float currentPitch, float targetYaw, float targetPitch, Entity entity) {
        float yawDelta = MathHelper.wrapDegrees(targetYaw - currentYaw);
        float pitchDelta = targetPitch - currentPitch;
        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));

        if (rotationDifference < 0.001f) {
            return new float[]{currentYaw, currentPitch};
        }

        if (entity != null) {
            return smoothWithEntity(currentYaw, currentPitch, yawDelta, pitchDelta, rotationDifference);
        } else {
            return smoothIdle(currentYaw, currentPitch, yawDelta, pitchDelta, rotationDifference);
        }
    }

    private float[] smoothWithEntity(float currentYaw, float currentPitch, float yawDelta, float pitchDelta, float rotDiff) {
        float speed = canAttack(400) ? 0.85f + random.nextFloat() * 0.2f : (random.nextBoolean() ? 0.4f : 0.2f);

        float lineYaw = Math.abs(yawDelta / rotDiff) * 180.0f;
        float linePitch = Math.abs(pitchDelta / rotDiff) * 180.0f;

        float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
        float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);

        float lerpFactor = randomLerp(speed, speed + 0.3f);
        float newYaw = MathHelper.lerp(lerpFactor, currentYaw, currentYaw + moveYaw);
        float newPitch = MathHelper.lerp(randomLerp(speed, speed + 0.3f), currentPitch, currentPitch + movePitch);

        long time = System.currentTimeMillis();
        float yawNoise = (float) (Math.sin(time / (double) (300 + count % 100)) * 0.008 + Math.cos(time / (double) (500 + count % 80)) * 0.006);
        float pitchNoise = (float) (Math.sin(time / (double) (400 + count % 120)) * 0.005 + Math.cos(time / (double) (600 + count % 90)) * 0.004);

        newYaw += yawNoise;
        newPitch = MathHelper.clamp(newPitch + pitchNoise, -90.0f, 90.0f);

        return new float[]{newYaw, newPitch};
    }

    private float[] smoothIdle(float currentYaw, float currentPitch, float yawDelta, float pitchDelta, float rotDiff) {
        long elapsed = System.currentTimeMillis() - lastAttackTime;
        int suck = count % 3;

        float speed = elapsed < 400 ? (random.nextBoolean() ? 0.4f : 0.2f) : -0.2f;
        float randomPhase = (float) elapsed / 40.0f + (float) (count % 6);

        float randYawComponent;
        float randPitchComponent;
        switch (suck) {
            case 0 -> { randYawComponent = (float) Math.cos(randomPhase); randPitchComponent = (float) Math.sin(randomPhase); }
            case 1 -> { randYawComponent = (float) Math.sin(randomPhase); randPitchComponent = (float) Math.cos(randomPhase); }
            case 2 -> { randYawComponent = (float) Math.sin(randomPhase); randPitchComponent = (float) (-Math.cos(randomPhase)); }
            default -> { randYawComponent = (float) (-Math.cos(randomPhase)); randPitchComponent = (float) Math.sin(randomPhase); }
        }

        float yawExtra = elapsed < 2000 ? randomLerp(10.0f, 26.0f) * randYawComponent : 0.0f;
        float pitchCos = randomLerp(0.0f, 3.0f) * (float) Math.cos((double) System.currentTimeMillis() / 5000.0);
        float pitchExtra = elapsed < 2000 ? randomLerp(1.0f, 7.0f) * randPitchComponent + pitchCos : 0.0f;

        float lineYaw = Math.abs(yawDelta / rotDiff) * 180.0f;
        float linePitch = Math.abs(pitchDelta / rotDiff) * 180.0f;

        float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
        float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);

        float clampedSpeed = Math.clamp(randomLerp(speed, speed + 0.2f), 0.0f, 1.0f);
        float newYaw = MathHelper.lerp(clampedSpeed, currentYaw, currentYaw + moveYaw) + yawExtra;
        float newPitch = MathHelper.lerp(clampedSpeed, currentPitch, currentPitch + movePitch) + pitchExtra;

        return new float[]{newYaw, newPitch};
    }


    public Vec3d randomValue() {
        return new Vec3d(
                0.04 + (Math.random() - 0.5) * 0.04,
                0.07 + (Math.random() - 0.5) * 0.06,
                0.04 + (Math.random() - 0.5) * 0.04
        );
    }

    private float randomLerp(float min, float max) {
        float r = random.nextFloat();
        float gcdNoise = (float) (Math.random() * 0.004 - 0.002);
        return MathHelper.lerp(MathHelper.clamp(r + gcdNoise, 0.0f, 1.0f), min, max);
    }
}
