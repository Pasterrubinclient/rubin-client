package ru.rubin.module.impl.combat.auraProcess;

import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;
import lombok.Generated;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;
import ru.rubin.module.impl.combat.HitAura;
import ru.rubin.module.impl.combat.auraProcess.auraUtil.AuraUtil;
import ru.rubin.module.impl.combat.auraProcess.auraUtil.UBoxPoints;
import ru.rubin.module.impl.combat.auraProcess.rotationProcess.impl.FreeLookUtil;
import ru.rubin.module.impl.combat.auraProcess.rotationProcess.impl.Rotation;
import ru.rubin.module.impl.combat.auraProcess.rotationProcess.impl.RotationProcess;
import ru.rubin.util.other.IMinecraft;
import ru.rubin.util.other.Mathf;
import ru.rubin.util.other.TimerUtil;
import ru.rubin.util.render.math.animation.anim2.Interpolator;

@Environment(EnvType.CLIENT)
public final class Rotate implements IMinecraft {
   static int tick;
   static float tickF;
   static TimerUtil time1 = new TimerUtil();
   static TimerUtil time2 = new TimerUtil();

   public static void onFunTimeRotation(LivingEntity target, boolean isAttack, float attackDistance, boolean check) {
      long currentTime = System.currentTimeMillis();
      if (!HitAura.isLookingUp && currentTime - HitAura.lastLookUpTime >= HitAura.nextLookUpDelay) {
         HitAura.isLookingUp = true;
         HitAura.lookUpStartTime = currentTime;
         HitAura.lookUpDuration = ThreadLocalRandom.current().nextInt(270, 390);
         HitAura.lastLookUpTime = currentTime;
         HitAura.nextLookUpDelay = ThreadLocalRandom.current().nextLong(6500L, 7200L);
      }

      boolean fastspeed = false;
      if (HitAura.isLookingUp && currentTime - HitAura.lookUpStartTime >= HitAura.lookUpDuration) {
         HitAura.isLookingUp = false;
      }

      if (currentTime - HitAura.lookUpStartTime >= HitAura.lookUpDuration + 40L) {
         fastspeed = true;
      }

      Vec3d playerEyePos = mc.player.getEyePos();
      float oscillY = (float)Math.cos(System.currentTimeMillis() / 450.0);
      float offsetY = 0.06F * oscillY;
      float oscillZ = (float)Math.cos(System.currentTimeMillis() / 500.0);
      float offsetZ = 0.06F * oscillZ;
      float oscillX = (float)Math.cos(System.currentTimeMillis() / 14000.0);
      float oscillX3 = (float)Math.cos(System.currentTimeMillis() / 2500L);
      float offsetX = 0.5F * oscillX;
      Vec3d directionVec = AuraUtil.getVector3(target);
      float baseYaw = FreeLookUtil.freeYaw;
      if (isAttack && AuraUtil.getStrictDistance(target) < attackDistance && !check) {
         tickF = Mathf.randomValue(6.0F, 7.0F);
      }

      float yawChangeSpeed = Mathf.randomValue(22.0F, 28.0F);
      float randomAttackShift = 0.0F;
      float pitchChangeSpeed = Mathf.randomValue(0.0F, 3.5F);
      float waveA = (float)Math.cos(System.currentTimeMillis() / 40.0);
      float waveB = (float)Math.sin(System.currentTimeMillis() / 70.0);
      if (tickF > 0.0F) {
         yawChangeSpeed = Mathf.randomValue(70.0F, 120.0F);
         baseYaw = (float)Math.toDegrees(Math.atan2(-directionVec.x, directionVec.z));
         randomAttackShift = (waveA + waveB) * randomLerp(1.0F, 2.0F);
         tickF--;
      }

      float basePitch = (float)MathHelper.clamp(
         -Math.toDegrees(Math.atan2(directionVec.y, Math.hypot(directionVec.x, directionVec.z))), -90.0, 90.0
      );
      float yawJitter = waveA * randomLerp(13.0F, 15.0F) + randomAttackShift;
      float pitchJitter = waveB * randomLerp(5.0F, 7.0F) + randomAttackShift;
      float finalPitch = HitAura.isLookingUp ? -Mathf.randomValue(80.0F, 90.0F) : basePitch;
      Rotation newRotation = new Rotation(baseYaw + yawJitter, finalPitch + pitchJitter);
      RotationProcess.update(
         newRotation,
         yawChangeSpeed,
         HitAura.isLookingUp ? randomLerp(120.0F, 170.0F) : (fastspeed ? randomLerp(120.0F, 170.0F) : randomLerp(6.0F, 8.0F)),
         25.0F,
         25.0F,
         0,
         15,
         false
      );
   }

   public static float randomLerp(float min, float max) {
      return Interpolator.lerp(max, min, new SecureRandom().nextFloat());
   }

   public static void onSpookyRotation(LivingEntity target, boolean attack) {
      float addyVacY = 0.02F * (float)Math.sin(System.currentTimeMillis() / 1200.0);
      float addyVacZ = 0.03F * (float)Math.sin(System.currentTimeMillis() / 900.0) + 0.02F * (float)Math.cos(System.currentTimeMillis() / 1200.0);
      float addyVacX = 0.4F * (float)Math.cos(System.currentTimeMillis() / 700L) + 0.04F * (float)Math.sin(System.currentTimeMillis() / 900.0);
      Vec3d vec = UBoxPoints.getBestVector3dOnEntityBox(target.getBoundingBox(), false).subtract(mc.player.getEyePos());
      boolean attackF = false;
      if (attack) {
         tick = 4;
      }

      if (tick > 0) {
         attackF = true;
         tick--;
      }

      float yaw = (float)Math.toDegrees(Math.atan2(-vec.x, vec.z));
      float pitch = (float)MathHelper.clamp(-Math.toDegrees(Math.atan2(vec.y, Math.hypot(vec.x, vec.z))), -90.0, 90.0);
      float randomToAttack = 0.0F;
      if (attackF) {
         randomToAttack = Mathf.random(-3.0F, 4.0F) + (float)(2.0 * Math.sin(System.currentTimeMillis() / 30.0));
      }

      float randomXY = Mathf.random(-3.0F, 3.0F) + (float)(3.0 * Math.cos(System.currentTimeMillis() / 40.0));
      float randomX = Mathf.random(-1.0F, 1.0F) + (float)(6.0 * Math.sin(System.currentTimeMillis() / 240.0));
      Rotation newRotation = new Rotation(yaw + randomXY + randomToAttack, pitch + randomX);
      RotationProcess.update(newRotation, Mathf.randomInt(90, 110), Mathf.randomValue(16.0F, 21.0F), 30.0F, 30.0F, 1, 15, false);
   }

   private static float spookySnapTicks = 0.0f;

   public static void onSpookyTimeDeluxeRotation(LivingEntity target, boolean isAttack) {
      if (mc.player == null || target == null) return;

      Vec3d targetPos = AuraUtil.getVector3(target);
      float currentYaw = FreeLookUtil.freeYaw;
      float currentPitch = FreeLookUtil.freePitch;


      double distToTarget = mc.player.distanceTo(target);
      float targetYawRaw = target.getYaw();
      float yawDelta = net.minecraft.util.math.MathHelper.wrapDegrees(target.getYaw() - target.lastYaw);
      float predictStrength = distToTarget <= 2.0 ? 0.19f : 0.15f;
      if (distToTarget > 2.0) {
         targetYawRaw += yawDelta * 2.5f;
      }
      double yawRad = Math.toRadians(targetYawRaw);
      Vec3d forward = new Vec3d(-Math.sin(yawRad), 0.0, Math.cos(yawRad));
      Vec3d predictedPos = new Vec3d(target.getX(), target.getY(), target.getZ()).add(forward.multiply(predictStrength)).add(0.0, target.getHeight() * 0.7, 0.0);
      Vec3d aimVec = predictedPos.subtract(mc.player.getEyePos());

      float yawToTarget = (float) Math.toDegrees(Math.atan2(-aimVec.x, aimVec.z));
      float pitchToTarget = (float) MathHelper.clamp(-Math.toDegrees(Math.atan2(aimVec.y, Math.hypot(aimVec.x, aimVec.z))), -90.0, 90.0);

      float yawDiff = MathHelper.wrapDegrees(yawToTarget - currentYaw);
      float pitchDiff = MathHelper.wrapDegrees(pitchToTarget - currentPitch);

      // Speed clamping
      float clampedYaw = Math.min(Math.max(Math.abs(yawDiff), 1.0f), 50.2f);
      float clampedPitch = Math.min(Math.max(Math.abs(pitchDiff), 1.0f), 16.2f);

      float newTargetYaw = currentYaw + (yawDiff > 0 ? clampedYaw : -clampedYaw);
      float newTargetPitch = currentPitch + (pitchDiff > 0 ? clampedPitch : -clampedPitch);

      // Lerp for smoothness
      float yaw = MathHelper.lerp(0.977f, currentYaw, newTargetYaw);
      float pitch = MathHelper.lerp(0.977f, currentPitch, newTargetPitch);

      // Jitter
      yaw += ThreadLocalRandom.current().nextFloat(-3.0f, 3.0f);
      pitch += ThreadLocalRandom.current().nextFloat(-3.0f, 3.0f);
      pitch = MathHelper.clamp(pitch, -90.0f, 90.0f);

      Rotation newRotation = new Rotation(yaw, pitch);
      RotationProcess.update(newRotation, 360.0f, 360.0f, 23.0f, 23.0f, 0, 15, false);
   }

   public static void onHolyRotation(LivingEntity target, boolean attack) {
      float addyVacY = 0.3F * (float)Math.cos(System.currentTimeMillis() / 2200.0);
      float addyVacZ = 0.03F * (float)Math.sin(System.currentTimeMillis() / 900.0) + 0.06F * (float)Math.cos(System.currentTimeMillis() / 1200.0);
      float addyVacX = 0.2F * (float)Math.cos(System.currentTimeMillis() / 700.0) + 0.04F * (float)Math.sin(System.currentTimeMillis() / 900.0);
      Vec3d playerEyePos = mc.player.getEyePos();
      Vec3d vec = new Vec3d(target.getX(), target.getY(), target.getZ()).add(addyVacX, target.getHeight() - 0.35F - addyVacY, addyVacZ).subtract(playerEyePos).normalize();
      boolean attackF = false;
      if (attack) {
         tick = 4;
      }

      if (tick > 0) {
         attackF = true;
         tick--;
      }

      float yaw = (float)Math.toDegrees(Math.atan2(-vec.x, vec.z));
      float pitch = (float)MathHelper.clamp(-Math.toDegrees(Math.atan2(vec.y, Math.hypot(vec.x, vec.z))), -90.0, 90.0);
      float randomToAttack = 0.0F;
      if (attackF) {
         randomToAttack = (float)(3.0 * Math.sin(System.currentTimeMillis() / 30.0))
            + (float)(Mathf.randomInt(3, 4) * Math.cos(System.currentTimeMillis() / 60.0));
      }

      Rotation newRotation = new Rotation(
         yaw + randomToAttack + ThreadLocalRandom.current().nextFloat(-2.0F, 2.0F), pitch + ThreadLocalRandom.current().nextFloat(-2.0F, 2.0F) + randomToAttack
      );
      RotationProcess.update(
         newRotation, (float)Mathf.randomWithUpdate(60.0, 80.0, 70L, time1), (float)Mathf.randomWithUpdate(10.0, 20.0, 240L, time2), 30.0F, 30.0F, 1, 15, false
      );
   }

   public static void onAresRotation(LivingEntity target, boolean attack) {
      Vec3d playerEyePos = mc.player.getEyePos();
      Vec3d vec = new Vec3d(target.getX(), target.getY(), target.getZ()).add(0.0, target.getHeight() / 2.0F, 0.0).subtract(playerEyePos).normalize();
      float yaw = (float)Math.toDegrees(Math.atan2(-vec.x, vec.z));
      float pitch = (float)MathHelper.clamp(-Math.toDegrees(Math.atan2(vec.y, Math.hypot(vec.x, vec.z))), -90.0, 90.0);
      float spy = 180.0F;
      float spx = 45.0F;
      Rotation newRotation = new Rotation(yaw + ThreadLocalRandom.current().nextFloat(-2.0F, 2.0F), pitch + ThreadLocalRandom.current().nextFloat(-1.0F, 1.0F));
      RotationProcess.update(newRotation, spx, spy, spx, spy, 0, 15, false);
   }

   public static void onMatrixRotation(LivingEntity target, boolean attack) {
      float addyVacY = target.getHeight() / 2.0F * (float)Math.cos(System.currentTimeMillis() / 2200.0);
      float addyVacZ = 0.16F * (float)Math.cos(System.currentTimeMillis() / 1250.0);
      float addyVacX = 0.22F * (float)Math.sin(System.currentTimeMillis() / 1700.0);
      Vec3d vec = UBoxPoints.getBestVector3dOnEntityBox(target.getBoundingBox(), false)
         .add(addyVacX, addyVacY, addyVacZ)
         .subtract(mc.player.getEyePos());
      float yaw = (float)Math.toDegrees(Math.atan2(-vec.x, vec.z));
      float pitch = (float)MathHelper.clamp(-Math.toDegrees(Math.atan2(vec.y, Math.hypot(vec.x, vec.z))), -90.0, 90.0);
      float spy = Mathf.random(7.0F, 9.0F);
      float spx = Mathf.randomInt(60, 80);
      Rotation newRotation = new Rotation(yaw + Mathf.randomInt(-1, 2), pitch);
      RotationProcess.update(newRotation, spx, spy, Mathf.randomInt(20, 30), Mathf.randomInt(20, 30), 1, 15, false);
   }

   public static void onPolarRotation(LivingEntity target, boolean attack) {
   }

   public static void onSnapRotation(LivingEntity target, boolean attack, String type) {
      float addyVacY = 0.25F * (float)Math.cos(System.currentTimeMillis() / 1500L);
      float addyVacZ = 0.2F * (float)Math.cos(System.currentTimeMillis() / 700L);
      float addyVacX = 0.2F * (float)Math.cos(System.currentTimeMillis() / 900L);
      Vec3d playerEyePos = mc.player.getEyePos();
      Vec3d vec = new Vec3d(target.getX(), target.getY(), target.getZ())
         .add(addyVacX, MathHelper.clamp(playerEyePos.y - target.getY(), 0.0, 0.8) - addyVacY, addyVacZ)
         .subtract(playerEyePos)
         .normalize();
      if (type.contains("Fast")) {
         float yaw = FreeLookUtil.freeYaw;
         float pitch = FreeLookUtil.freePitch;
         float speed = Mathf.random(190.0F, 245.0F);
         if (attack) {
            yaw = (float)Math.toDegrees(Math.atan2(-vec.x, vec.z));
            pitch = (float)MathHelper.clamp(-Math.toDegrees(Math.atan2(vec.y, Math.hypot(vec.x, vec.z))), -90.0, 90.0);
         }

         float rx = 0.0F;
         float ry = 0.0F;
         RotationProcess.update(new Rotation(yaw + rx, pitch + ry), speed, speed, 40.0F, 40.0F, 1, 7, false);
      } else if (type.contains("Smooth")) {
         float yaw = FreeLookUtil.freeYaw;
         float pitch = FreeLookUtil.freePitch;
         float speed = 24.0F;
         if (attack) {
            tick = 3;
            speed = 88.0F;
         }

         if (tick > 0) {
            yaw = (float)Math.toDegrees(Math.atan2(-vec.x, vec.z));
            pitch = (float)MathHelper.clamp(-Math.toDegrees(Math.atan2(vec.y, Math.hypot(vec.x, vec.z))), -90.0, 90.0);
            tick--;
         }

         float rx = 0.0F;
         float ry = 0.0F;
         RotationProcess.update(new Rotation(yaw + rx, pitch + ry), speed, speed, 40.0F, 40.0F, 1, 7, false);
      } else if (type.contains("Random")) {
         float yawx = FreeLookUtil.freeYaw;
         float pitchx = FreeLookUtil.freePitch;
         float speedx = Mathf.random(30.0F, 35.0F);
         if (attack) {
            tick = Mathf.randomInt(2, 4);
         }

         if (tick > 0) {
            speedx = Mathf.random(140.0F, 220.0F);
            yawx = (float)Math.toDegrees(Math.atan2(-vec.x, vec.z));
            pitchx = (float)MathHelper.clamp(-Math.toDegrees(Math.atan2(vec.y, Math.hypot(vec.x, vec.z))), -90.0, 90.0);
            tick--;
         }

         float randomXY = ThreadLocalRandom.current().nextFloat(-3.0F, 3.0F)
            + (float)(Mathf.random(4.0F, 5.0F) * Math.cos(System.currentTimeMillis() / 150.0))
            + (float)(Mathf.random(4.0F, 5.0F) * Math.sin(System.currentTimeMillis() / 50.0))
            + (float)(Mathf.random(5.0F, 8.0F) * Math.sin(System.currentTimeMillis() / 130.0))
               * (float)(Mathf.random(4.0F, 7.0F) * Math.cos(System.currentTimeMillis() / 650.0))
            + (float)(Mathf.random(12.0F, 18.0F) * Math.sin(System.currentTimeMillis() / 80.0))
               * (float)(Mathf.random(2.0F, 3.0F) * Math.cos(System.currentTimeMillis() / 2650.0));
         float randomX = ThreadLocalRandom.current().nextFloat(-1.0F, 1.0F)
            + (float)(Mathf.random(2.0F, 3.0F) * Math.cos(System.currentTimeMillis() / 170.0))
            + (float)(Mathf.random(3.0F, 4.0F) * Math.sin(System.currentTimeMillis() / 70.0))
            + (float)(Mathf.random(1.0F, 2.0F) * Math.sin(System.currentTimeMillis() / 110.0))
               * (float)(Mathf.random(1.0F, 2.0F) * Math.cos(System.currentTimeMillis() / 350.0));
         RotationProcess.update(new Rotation(yawx + randomXY / 4.0F, pitchx + randomX), speedx, speedx, 40.0F, 40.0F, 1, 7, false);
      }
   }

   private static final ru.rubin.module.impl.combat.auraProcess.rotationProcess.impl.FunTimeAngleSmoother funTimeSmoother = new ru.rubin.module.impl.combat.auraProcess.rotationProcess.impl.FunTimeAngleSmoother();
   private static int ftSmoothCount;
   private static long ftLastAttackTime;

   public static void onFunTimeSmoothRotation(LivingEntity target, boolean canAttack) {
      if (mc.player == null || target == null) return;

      if (canAttack) {
         ftSmoothCount++;
         ftLastAttackTime = System.currentTimeMillis();
      }

      long elapsed = System.currentTimeMillis() - ftLastAttackTime;
      Vec3d directionVec = AuraUtil.getVector3(target);
      float baseYaw = (float) Math.toDegrees(Math.atan2(-directionVec.x, directionVec.z));
      float basePitch = (float) MathHelper.clamp(
              -Math.toDegrees(Math.atan2(directionVec.y, Math.hypot(directionVec.x, directionVec.z))), -90.0, 90.0
      );


      float yawSpeed;
      float pitchSpeed;
      if (canAttack) {
         yawSpeed = Mathf.randomValue(70.0f, 120.0f);
         pitchSpeed = Mathf.randomValue(15.0f, 25.0f);
      } else {
         yawSpeed = Mathf.randomValue(20.0f, 35.0f);
         pitchSpeed = Mathf.randomValue(5.0f, 10.0f);
      }


      long time = System.currentTimeMillis();
      float yawNoise = (float) (Math.sin(time / (double) (300 + ftSmoothCount % 100)) * 0.8 
              + Math.cos(time / (double) (500 + ftSmoothCount % 80)) * 0.6);
      float pitchNoise = (float) (Math.sin(time / (double) (400 + ftSmoothCount % 120)) * 0.5 
              + Math.cos(time / (double) (600 + ftSmoothCount % 90)) * 0.4);


      float idleYaw = 0.0f;
      float idlePitch = 0.0f;
      if (elapsed < 2000) {
         int suck = ftSmoothCount % 3;
         float phase = (float) elapsed / 40.0f + (float) (ftSmoothCount % 6);
         float randComp = switch (suck) {
            case 0 -> (float) Math.cos(phase);
            case 1 -> (float) Math.sin(phase);
            default -> (float) Math.sin(phase);
         };
         idleYaw = randomLerp(3.0f, 8.0f) * randComp;
         idlePitch = randomLerp(0.5f, 2.0f) * (float) Math.cos(phase * 0.7f);
      }

      Rotation newRotation = new Rotation(
              baseYaw + yawNoise + idleYaw,
              MathHelper.clamp(basePitch + pitchNoise + idlePitch, -90.0f, 90.0f)
      );
      RotationProcess.update(newRotation, yawSpeed, pitchSpeed, 25.0f, 25.0f, 0, 15, false);
   }

   @Generated
   private Rotate() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
