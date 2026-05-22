package ru.rubin.module.impl.combat.auraProcess;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.rubin.module.impl.combat.auraProcess.rotationProcess.impl.FreeLookUtil;
import ru.rubin.module.impl.combat.auraProcess.rotationProcess.impl.Rotation;
import ru.rubin.module.impl.combat.auraProcess.rotationProcess.impl.RotationProcess;
import ru.rubin.util.other.IMinecraft;

/**
 * SlothRotation — spring-based rotation from Wonder client.
 * Uses velocity/spring interpolation, random aim points on hitbox,
 * noise generation, hit phases, and reaction time simulation.
 */
@Environment(EnvType.CLIENT)
public final class SlothRotation implements IMinecraft {

    private static LivingEntity trackedTarget;
    private static float currentYaw;
    private static float currentPitch;
    private static float velocityYaw;
    private static float velocityPitch;
    private static double aimPointX;
    private static double aimPointY;
    private static double aimPointZ;
    private static float noiseAngle;
    private static final float noiseAmplitude = 1.8f;
    private static int hitPhase;
    private static int hitTimer;
    private static long firstSeenTime;
    private static int reactionMs;
    private static boolean reactionComplete;
    private static float smoothYaw;
    private static float smoothPitch;

    public static void reset() {
        trackedTarget = null;
        velocityYaw = 0;
        velocityPitch = 0;
        aimPointX = 0;
        aimPointY = 0;
        aimPointZ = 0;
        noiseAngle = 0;
        hitPhase = 0;
        hitTimer = 0;
        firstSeenTime = 0;
        reactionComplete = false;
        reactionMs = 0;
        if (mc.player != null) {
            currentYaw = mc.player.getYaw();
            currentPitch = mc.player.getPitch();
            smoothYaw = currentYaw;
            smoothPitch = currentPitch;
        }
    }

    public static void onAttack() {
        hitPhase = 1;
        hitTimer = 0;
    }

    public static void apply(LivingEntity target, boolean isAttack) {
        if (mc.player == null || target == null) return;

        // New target — start reaction
        if (target != trackedTarget) {
            trackedTarget = target;
            firstSeenTime = System.currentTimeMillis();
            reactionMs = 80 + (int) (Math.random() * 120); // 80-200ms
            reactionComplete = false;
            currentYaw = FreeLookUtil.freeYaw;
            currentPitch = FreeLookUtil.freePitch;
            smoothYaw = currentYaw;
            smoothPitch = currentPitch;
            pickAimPoint(target);
        }

        // Reaction delay
        if (!reactionComplete) {
            if (System.currentTimeMillis() - firstSeenTime < reactionMs) {
                // During reaction: just hold current angles with tiny drift
                float[] noise = generateNoise(noiseAngle);
                noiseAngle += 0.07f;
                Rotation hold = new Rotation(currentYaw + noise[0] * 0.3f, currentPitch + noise[1] * 0.2f);
                RotationProcess.update(hold, 5.0f, 5.0f, 20.0f, 20.0f, 0, 15, false);
                return;
            }
            reactionComplete = true;
        }

        // Hit phase: after attacking, slight overshoot then settle
        if (hitPhase > 0) {
            hitTimer++;
            if (hitTimer > 5) {
                hitPhase = 0;
                hitTimer = 0;
            }
        }

        // Pick new aim point periodically
        if (Math.random() < 0.03) { // ~3% chance per tick = every ~30 ticks avg
            pickAimPoint(target);
        }

        // Calculate target angles from aim point
        Box box = target.getBoundingBox();
        double targetX = box.minX + (box.maxX - box.minX) * (0.5 + aimPointX);
        double targetY = box.minY + (box.maxY - box.minY) * (0.5 + aimPointY);
        double targetZ = box.minZ + (box.maxZ - box.minZ) * (0.5 + aimPointZ);

        Vec3d eyes = mc.player.getEyePos();
        Vec3d aimVec = new Vec3d(targetX - eyes.x, targetY - eyes.y, targetZ - eyes.z);

        float targetYaw = (float) Math.toDegrees(Math.atan2(-aimVec.x, aimVec.z));
        float targetPitch = (float) MathHelper.clamp(
                -Math.toDegrees(Math.atan2(aimVec.y, Math.hypot(aimVec.x, aimVec.z))), -90.0, 90.0);

        // Add noise
        float[] noise = generateNoise(noiseAngle);
        noiseAngle += 0.12f + (float) Math.random() * 0.03f;
        targetYaw += noise[0] * noiseAmplitude;
        targetPitch += noise[1] * noiseAmplitude * 0.5f;

        // Hit phase overshoot
        if (hitPhase > 0) {
            float overshoot = (5 - hitTimer) * 0.4f;
            targetPitch -= overshoot; // slight upward flick
        }

        // Spring interpolation
        float deltaYaw = MathHelper.wrapDegrees(targetYaw - currentYaw);
        float deltaPitch = targetPitch - currentPitch;

        float stiffness = 0.18f + (float) Math.random() * 0.04f; // spring constant
        float damping = 0.62f + (float) Math.random() * 0.06f; // damping ratio

        velocityYaw = springInterp(velocityYaw, deltaYaw, stiffness, damping);
        velocityPitch = springInterp(velocityPitch, deltaPitch, stiffness, damping);

        currentYaw += velocityYaw;
        currentPitch = MathHelper.clamp(currentPitch + velocityPitch, -90.0f, 90.0f);

        // Smooth step for final output (removes micro-jitter)
        smoothYaw = smoothLerp(smoothYaw, currentYaw, 0.7f);
        smoothPitch = smoothLerp(smoothPitch, currentPitch, 0.7f);

        // GCD correction
        float gcd = calcGcd();
        float finalYaw = smoothYaw - (smoothYaw - FreeLookUtil.freeYaw) % gcd;
        float finalPitch = smoothPitch - (smoothPitch - FreeLookUtil.freePitch) % gcd;

        Rotation newRotation = new Rotation(finalYaw, MathHelper.clamp(finalPitch, -90.0f, 90.0f));
        RotationProcess.update(newRotation, 360.0f, 360.0f, 22.0f, 22.0f, 0, 15, false);
    }

    private static void pickAimPoint(LivingEntity target) {
        Box box = target.getBoundingBox();
        double w = box.maxX - box.minX;
        double h = box.maxY - box.minY;
        double d = box.maxZ - box.minZ;
        aimPointX = (Math.random() - 0.5) * w * 0.12;
        aimPointY = (Math.random() - 0.5) * h * 0.18 + 0.15; // bias upper body
        aimPointZ = (Math.random() - 0.5) * d * 0.12;
    }

    private static float[] generateNoise(float angle) {
        float yawNoise = (float) (Math.sin(angle) * 0.6 + Math.sin(angle * 2.3f) * 0.3 + Math.cos(angle * 0.7f) * 0.1);
        float pitchNoise = (float) (Math.cos(angle * 1.1f) * 0.4 + Math.sin(angle * 1.7f) * 0.2);
        return new float[]{yawNoise, pitchNoise};
    }

    private static float springInterp(float velocity, float delta, float stiffness, float damping) {
        float force = delta * stiffness;
        velocity += force;
        velocity *= damping;
        return velocity;
    }

    private static float smoothLerp(float current, float target, float factor) {
        return current + (target - current) * factor;
    }

    private static float calcGcd() {
        double sens = mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2;
        return (float) (sens * sens * sens * 1.2);
    }
}
