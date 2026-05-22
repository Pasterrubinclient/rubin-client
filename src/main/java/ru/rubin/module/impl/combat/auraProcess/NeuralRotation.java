package ru.rubin.module.impl.combat.auraProcess;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.rubin.module.impl.combat.auraProcess.auraUtil.AuraUtil;
import ru.rubin.module.impl.combat.auraProcess.rotationProcess.impl.FreeLookUtil;
import ru.rubin.module.impl.combat.auraProcess.rotationProcess.impl.Rotation;
import ru.rubin.module.impl.combat.auraProcess.rotationProcess.impl.RotationProcess;
import ru.rubin.util.other.IMinecraft;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Neural-bypass rotation — designed to evade ML/dataset-based anticheat.
 *
 * Key differences from standard rotations:
 * - Gaussian noise instead of uniform
 * - Momentum/inertia simulation (mouse physics)
 * - Acceleration curves (smooth ramp up/down)
 * - Periodic "distraction" phases (look away briefly)
 * - Variable aim height on hitbox
 * - Correlated yaw/pitch movement (diagonal mouse)
 * - Non-constant tracking (human reaction delay)
 */
@Environment(EnvType.CLIENT)
public final class NeuralRotation implements IMinecraft {

    // --- State ---
    private static float momentumYaw = 0.0f;
    private static float momentumPitch = 0.0f;
    private static float lastOutputYaw = 0.0f;
    private static float lastOutputPitch = 0.0f;
    private static float aimHeight = 0.7f;
    private static long lastAimHeightChange = 0;
    private static long distractionStartTime = 0;
    private static long distractionDuration = 0;
    private static boolean distracted = false;
    private static long nextDistractionTime = 0;
    private static float distractionYawOffset = 0;
    private static float distractionPitchOffset = 0;
    private static int ticksSinceTarget = 0;
    private static float accelerationPhase = 0.0f;
    private static final Random gaussRng = new Random();

    public static void reset() {
        momentumYaw = 0.0f;
        momentumPitch = 0.0f;
        lastOutputYaw = 0.0f;
        lastOutputPitch = 0.0f;
        distracted = false;
        distractionStartTime = 0;
        nextDistractionTime = System.currentTimeMillis() + randomLong(3000, 8000);
        ticksSinceTarget = 0;
        accelerationPhase = 0.0f;
    }

    public static void apply(LivingEntity target, boolean isAttack) {
        if (mc.player == null || target == null) return;

        long now = System.currentTimeMillis();
        ticksSinceTarget++;

        // --- Variable aim height (changes every 1-3 sec) ---
        if (now - lastAimHeightChange > randomLong(1000, 3000)) {
            aimHeight = 0.5f + gaussian() * 0.2f; // 0.3 - 0.7 range, gaussian distribution
            aimHeight = MathHelper.clamp(aimHeight, 0.3f, 0.85f);
            lastAimHeightChange = now;
        }

        // --- Distraction phase (periodic look-away) ---
        if (!isAttack && !distracted && now > nextDistractionTime) {
            distracted = true;
            distractionStartTime = now;
            distractionDuration = randomLong(150, 400);
            distractionYawOffset = gaussian() * 25.0f;
            distractionPitchOffset = gaussian() * 8.0f;
            nextDistractionTime = now + distractionDuration + randomLong(4000, 12000);
        }

        if (distracted && now - distractionStartTime > distractionDuration) {
            distracted = false;
        }

        // --- Predictive aim point ---
        double dist = mc.player.distanceTo(target);
        float targetYawRaw = target.getYaw();
        float yawDelta = MathHelper.wrapDegrees(target.getYaw() - target.lastYaw);

        // Prediction scales with distance
        float predictStrength = dist <= 2.0 ? 0.18f : (dist <= 4.0 ? 0.13f : 0.08f);
        if (Math.abs(yawDelta) > 2.0f) {
            targetYawRaw += yawDelta * (1.5f + gaussian() * 0.5f);
        }

        double yawRad = Math.toRadians(targetYawRaw);
        Vec3d forward = new Vec3d(-Math.sin(yawRad), 0.0, Math.cos(yawRad));
        Vec3d targetPos = new Vec3d(target.getX(), target.getY(), target.getZ())
                .add(forward.multiply(predictStrength))
                .add(0.0, target.getHeight() * aimHeight, 0.0);
        Vec3d aimVec = targetPos.subtract(mc.player.getEyePos());

        float targetYaw = (float) Math.toDegrees(Math.atan2(-aimVec.x, aimVec.z));
        float targetPitch = (float) MathHelper.clamp(
                -Math.toDegrees(Math.atan2(aimVec.y, Math.hypot(aimVec.x, aimVec.z))), -90.0, 90.0);

        // --- Apply distraction offset ---
        if (distracted) {
            float distractProgress = (float) (now - distractionStartTime) / distractionDuration;
            float distractFade = (float) Math.sin(distractProgress * Math.PI); // fade in/out
            targetYaw += distractionYawOffset * distractFade;
            targetPitch += distractionPitchOffset * distractFade;
        }

        // --- Gaussian noise (NOT uniform) ---
        float noiseYaw = gaussian() * 1.2f;
        float noisePitch = gaussian() * 0.7f;

        // Correlated noise: if yaw moves right, pitch tends to move slightly too (diagonal mouse)
        float correlation = 0.3f;
        noisePitch += noiseYaw * correlation * (gaussian() > 0 ? 0.4f : -0.4f);

        targetYaw += noiseYaw;
        targetPitch = MathHelper.clamp(targetPitch + noisePitch, -90.0f, 90.0f);

        // --- Direct aim (no momentum when close/circling) ---
        float currentYaw = FreeLookUtil.freeYaw;
        float currentPitch = FreeLookUtil.freePitch;

        // Speed based on distance to target angle
        float desiredDeltaYaw = MathHelper.wrapDegrees(targetYaw - currentYaw);
        float desiredDeltaPitch = targetPitch - currentPitch;
        float distToTarget = (float) Math.hypot(desiredDeltaYaw, desiredDeltaPitch);

        // --- Acceleration curve ---
        if (distToTarget > 8.0f) {
            accelerationPhase = Math.min(1.0f, accelerationPhase + 0.2f);
        } else {
            accelerationPhase = Math.max(0.0f, accelerationPhase - 0.1f);
        }

        float baseSpeed = 45.0f + accelerationPhase * 55.0f; // 45-100
        if (isAttack) {
            baseSpeed += 40.0f + gaussian() * 10.0f;
        }

        // Human-like speed variance
        float speedVariance = 1.0f + gaussian() * 0.12f;
        float yawSpeed = Math.max(20.0f, baseSpeed * speedVariance);
        float pitchSpeed = Math.max(12.0f, baseSpeed * 0.6f * speedVariance);

        // --- Reaction delay simulation ---
        if (ticksSinceTarget < 2) {
            float reactionMultiplier = 0.6f + (ticksSinceTarget * 0.2f);
            yawSpeed *= reactionMultiplier;
            pitchSpeed *= reactionMultiplier;
        }

        // --- Apply directly to RotationProcess (no momentum/lerp) ---
        Rotation newRotation = new Rotation(targetYaw, targetPitch);
        RotationProcess.update(newRotation, yawSpeed, pitchSpeed, 22.0f, 22.0f, 0, 15, false);
    }

    // --- Utility ---

    private static float gaussian() {
        return (float) gaussRng.nextGaussian();
    }

    private static long randomLong(long min, long max) {
        return ThreadLocalRandom.current().nextLong(min, max);
    }
}
