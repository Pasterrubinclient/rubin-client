package ru.rubin.module.impl.combat;

import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Hand;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.ShieldItem;
import net.minecraft.util.math.Vec3d;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.network.ClientPlayerEntity;
import ru.rubin.Rubin;
import ru.rubin.event.EventInit;
import ru.rubin.event.impl.EventUpdate;
import ru.rubin.event.lifecycle.ClientTickEvent;
import ru.rubin.module.api.Category;
import ru.rubin.module.api.IModule;
import ru.rubin.module.api.Module;
import ru.rubin.module.api.setting.Setting;
import ru.rubin.module.api.setting.impl.BooleanSetting;
import ru.rubin.module.api.setting.impl.ModeSetting;
import ru.rubin.module.api.setting.impl.MultiBooleanSetting;
import ru.rubin.module.api.setting.impl.SliderSetting;
import ru.rubin.module.impl.combat.auraProcess.Attack;
import ru.rubin.module.impl.combat.auraProcess.Rotate;
import ru.rubin.module.impl.combat.auraProcess.UFunTimeRotations;
import ru.rubin.module.impl.combat.auraProcess.auraUtil.AuraUtil;
import ru.rubin.util.player.MoveUtil;
import ru.rubin.util.player.MovementManager;
import ru.rubin.util.player.PlayerUtil;

@IModule(
   name = "Hit Aura",
   description = "Обходил жозкий POLAR/FUNTIME/POKITIME",
   category = Category.Combat,
   bind = 82
)
@Environment(EnvType.CLIENT)
public class HitAura extends Module {
   public static SliderSetting attackRange = new SliderSetting("Радиус атаки", 3.0F, 3.0F, 6.0F, 0.1F, false);
   public static SliderSetting preRange = new SliderSetting("Радиус обнаружения", 1.0F, 0.0F, 5.0F, 0.1F, false);
   public static ModeSetting rotationType = new ModeSetting("Режим ротации", "Adapted", "Adapted", "Polar", "Snap", "TriggerBot", "FunTime Smooth");
   public static ModeSetting snapSetting = new ModeSetting("Режим снапа", "Fast", "Fast", "Smooth", "Random").hidden(() -> !rotationType.is("Snap"));
   public static MultiBooleanSetting targets = new MultiBooleanSetting(
      "Цели", new BooleanSetting("Игроки", true), new BooleanSetting("Голые", true), new BooleanSetting("Мобы", true)
   );
   public static ModeSetting attackDelay = new ModeSetting("Тайминг удара", "Быстрый", "Быстрый", "Динамичный");
   public static MultiBooleanSetting misc = new MultiBooleanSetting(
      "Проверки до удара",
      new BooleanSetting("Бить через блоки", false),
      new BooleanSetting("Бить только оружием", false),
      new BooleanSetting("Не бить если кушаеш", true),
      new BooleanSetting("Не атакавать в контейнере", false),
      new BooleanSetting("Автоматичиски ломать щит", false),
      new BooleanSetting("Отжимать щит при ударе", false)
   );
   public static MultiBooleanSetting extraSettings = new MultiBooleanSetting(
      "Доп.настройка", new BooleanSetting("Легитный спринт", false), new BooleanSetting("Умные криты", false)
   );
   public static ModeSetting motion = new ModeSetting("Режим движения", "Default", "Default", "Free", "Target");
   public static LivingEntity target;
   public static boolean canSwap = false;
   public static long lastLookUpTime = 0L;
   public static long nextLookUpDelay = ThreadLocalRandom.current().nextLong(90000L, 180000L);
   public static boolean isLookingUp = false;
   public static long lookUpStartTime = 0L;
   public static int lookUpDuration = 0;

   public HitAura() {
      this.addSettings(new Setting[]{attackRange, preRange, rotationType, snapSetting, targets, attackDelay, misc, extraSettings, motion});
   }

   @EventInit
   public void onEvent(ClientTickEvent e) {
      if (target != null && mc.player != null && mc.world != null) {
         this.updateRotation();
      }
   }

   @EventInit
   public void onEvent(EventUpdate e) {
      if (!mc.player.isAlive()) {
         this.toggle();
      } else {
         if (target == null || !this.isValidTarget(target)) {
            this.updateTarget();
         }

         if (target != null && mc.player != null && mc.world != null) {
            if (motion.is("Free") && !canSwap) {
               MoveUtil.fixMovement(mc.gameRenderer.getCamera().getYaw());
            }

            if (motion.is("Target") && !canSwap) {
               MoveUtil.targetMovement(mc.player.getYaw(), new Vec3d(target.getX(), target.getY(), target.getZ()));
            }

            if (Attack.resetSprintTick(target, getRanges())) {
               mc.player.setSprinting(false);
               mc.options.sprintKey.setPressed(false);
            }

            if (!this.checkToAttack()) {
               this.attackEntity();
            }
         } else {
            this.reset();
         }
      }
   }

   public static float[] getRanges() {
      return new float[]{attackRange.get(), preRange.get()};
   }

   public boolean isRayCastRuleToAttack() {
      return true;
   }

   public void attackEntity() {
      if (!(AuraUtil.getStrictDistance(target) >= attackRange.get())) {
         float[] ranges = getRanges();
         ranges = new float[]{ranges[0], ranges[1], ranges[0] + ranges[1]};
         boolean canPacket = !mc.player.hasStatusEffect(StatusEffects.BLINDNESS) && !mc.player.isFlyingVehicle() && !extraSettings.get("Легитный спринт");
         if (target != null) {
            Attack.antiMissesHittingUpdate(target, true, this.isRayCastRuleToAttack(), false);
            boolean canAttack = Attack.shouldAttack(target, this.isRayCastRuleToAttack(), true, true, 0L, ranges);
            if (canAttack) {
               Runnable[] shieldBreak = Attack.hitShieldBreakTaskForUse(target, misc.get("Автоматичиски ломать щит"));
               Runnable[] shieldPressBypass = Attack.resetShieldSilentTaskForUse(true);
               Runnable[] skipSilentSprint = Attack.skipSilentSprintingTaskForUse(canPacket);
               Runnable preHitSendCodeSingleTick = () -> {
                  skipSilentSprint[0].run();
                  shieldPressBypass[0].run();
                  shieldBreak[0].run();
               };
               Runnable postHitSendCodeSingleTick = () -> {
                  shieldBreak[1].run();
                  shieldPressBypass[1].run();
                  skipSilentSprint[1].run();
               };
               if (misc.get("Отжимать щит при ударе") && mc.player.getActiveItem().getItem().equals(Items.SHIELD) && mc.player.isUsingItem()
                  )
                {
                  mc.interactionManager.stopUsingItem(mc.player);
               }

               mc.player.setSprinting(false);
               mc.options.sprintKey.setPressed(false);
               Attack.useEntity(target, preHitSendCodeSingleTick, postHitSendCodeSingleTick, Hand.MAIN_HAND, true);
            }
         }
      }
   }

   private void updateRotation() {
      float[] ranges = getRanges();
      ranges = new float[]{ranges[0], ranges[1], ranges[0] + ranges[1]};
      boolean canAttack = Attack.shouldAttack(target, false, true, true, 0L, ranges);
      String var3 = rotationType.get();
      switch (var3) {
         case "Polar":
            UFunTimeRotations.rotation(target, Attack.shouldAttack(target, false, true, true, -50L, ranges), attackRange.get(), this.checkToAttack());
            break;
         case "Adapted":
            if (PlayerUtil.isServerContains("spookytime")) {
               Rotate.onSpookyRotation(target, canAttack);
            } else if (PlayerUtil.isServerContains("holy")) {
               Rotate.onHolyRotation(target, canAttack);
            } else if (PlayerUtil.isServerContains("ares")) {
               Rotate.onAresRotation(target, canAttack);
            } else {
               Rotate.onMatrixRotation(target, canAttack);
            }
            break;
         case "Snap":
            Rotate.onSnapRotation(target, Attack.shouldAttack(target, false, true, true, -50L, ranges), snapSetting.get());
            break;
         case "FunTime Smooth":
            Rotate.onFunTimeSmoothRotation(target, canAttack);
            break;
      }
   }

   private void updateTarget() {
      LivingEntity bestTarget = null;
      double bestAngle = Double.MAX_VALUE;
      Vec3d eyePos = mc.player.getEyePos();
      Vec3d lookVec = mc.player.getRotationVec(1.0F).normalize();

      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof LivingEntity living && this.isValidTarget(living)) {
            Vec3d targetPos = new Vec3d(living.getX(), living.getY(), living.getZ()).add(0.0, living.getHeight() * 0.5, 0.0);
            Vec3d toTarget = targetPos.subtract(eyePos).normalize();
            double angle = Math.acos(MathHelper.clamp(lookVec.dotProduct(toTarget), -1.0, 1.0));
            if (angle < bestAngle) {
               bestAngle = angle;
               bestTarget = living;
            }
         }
      }

      target = bestTarget;
   }

   private float auraDist() {
      return attackRange.get() + preRange.get();
   }

   private boolean isValidTarget(LivingEntity entity) {
      if (entity instanceof ClientPlayerEntity) {
         return false;
      } else if (mc.player.distanceTo(entity) > this.auraDist()) {
         return false;
      } else if (!misc.get("Бить через блоки") && !mc.player.canSee(entity)) {
         return false;
      } else if (entity instanceof PlayerEntity p && Rubin.get.friendManager.isFriend(p.getName().getString())) {
         return false;
      } else if (entity instanceof PlayerEntity && !targets.get("Игроки")) {
         return false;
      } else if (entity instanceof PlayerEntity && entity.getArmor() == 0 && !targets.get("Голые")) {
         return false;
      } else if (entity instanceof PlayerEntity && ((PlayerEntity)entity).isCreative()) {
         return false;
      } else {
         return (entity instanceof Monster || entity instanceof SlimeEntity || entity instanceof VillagerEntity || entity instanceof AnimalEntity)
               && !targets.get("Мобы")
            ? false
            : !entity.isInvulnerable() && entity.isAlive() && !(entity instanceof ArmorStandEntity);
      }
   }

   @Override
   public void toggle() {
      super.toggle();
      this.reset();
   }

   private void reset() {
      target = null;
      MovementManager.getInstance().unlockMovement("Aura");
      if (mc.player != null) {
         isLookingUp = false;
         lookUpStartTime = 0L;
      }
   }

   private boolean checkToAttack() {
      return mc.player.isUsingItem() && misc.get("Не бить если кушаеш") && !(mc.player.getActiveItem().getItem() instanceof ShieldItem)
         || mc.currentScreen != null && misc.get("Не атакавать в контейнере")
         || !mc.player.getMainHandStack().isIn(ItemTags.SWORDS)
            && !mc.player.getMainHandStack().isIn(ItemTags.AXES)
            && misc.get("Бить только оружием");
   }

   @Override
   public void onDisable() {
      super.onDisable();
   }
}
