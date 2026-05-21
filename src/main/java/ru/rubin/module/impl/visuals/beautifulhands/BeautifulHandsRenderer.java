package ru.rubin.module.impl.visuals.beautifulhands;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.item.*;
import net.minecraft.item.consume.UseAction;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import ru.rubin.Rubin;

@Environment(EnvType.CLIENT)
public class BeautifulHandsRenderer {

    private static final BeautifulHandsRenderer INSTANCE = new BeautifulHandsRenderer();

    public static BeautifulHandsRenderer getInstance() {
        return INSTANCE;
    }

    private static final double ANIMATION_SPEED = 30.0;
    private static final double MAX_DELTA = 0.05;

    private double prevFrameTime = System.nanoTime() / 1e9;
    private double deltaTime;
    private double prevSwimRotation;

    private float swingAngleY;
    private float swingAngleX;
    private float swingVelY;
    private float swingVelX;
    private float swingVelZ;
    private float vertAngleY;
    private float vertVelYSlime;
    private float vertAngleYSlime;
    private float climbBlend;
    private float dirCrawlCount;
    private float climbCount;
    private float inWaterCounter;
    private boolean physicsUpdated;
    private boolean swingLeft;
    private float prevSwingProgress;

    private final MinecraftClient mc = MinecraftClient.getInstance();

    public void updateDelta() {
        double now = System.nanoTime() / 1e9;
        deltaTime = Math.min(MAX_DELTA, Math.max(0, now - prevFrameTime));
        prevFrameTime = now;
        physicsUpdated = false;
    }

    public void onNewSwing(float swingProgress) {
        if (swingProgress > 0f && prevSwingProgress == 0f) {
            swingLeft = !swingLeft;
        }
        prevSwingProgress = swingProgress;
    }

    public void renderArmFirstPerson(MatrixStack matrices, OrderedRenderCommandQueue queue, int light,
                                     float equipProgress, float swingProgress, Arm side) {
        AbstractClientPlayerEntity player = mc.player;
        if (player == null) return;

        boolean right = side != Arm.LEFT;
        float f = right ? 1f : -1f;
        float f1 = MathHelper.sqrt(swingProgress);
        float f2 = -0.3f * MathHelper.sin(f1 * (float) Math.PI);
        float f3 = 0.4f * MathHelper.sin(f1 * (float) (Math.PI * 2));
        float f4 = -0.4f * MathHelper.sin(swingProgress * (float) Math.PI);

        matrices.translate(f * (f2 + 0.64f), f3 + -0.6f + equipProgress * -0.6f, f4 + -0.72f);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(f * 45f));

        float f5 = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        float f6 = MathHelper.sin(f1 * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(f * f6 * 70f));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(f * f5 * -20f));
        matrices.translate(f * -1f, 3.6f, 3.5);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(f * 120f));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(200f));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(f * -135f));
        matrices.translate(f * 5.6f, 0f, 0f);

        applyHandOffsets(matrices, side);

        PlayerEntityRenderer renderer = (PlayerEntityRenderer) mc.getEntityRenderDispatcher().getRenderer(player);
        Identifier skin = player.getSkin().body().texturePath();
        boolean slim = player.getSkin().model() == PlayerSkinType.SLIM;
        if (right) {
            renderer.renderRightArm(matrices, queue, light, skin, slim);
        } else {
            renderer.renderLeftArm(matrices, queue, light, skin, slim);
        }
    }

    private void applyHandOffsets(MatrixStack matrices, Arm side) {
        BeautifulHands module = (BeautifulHands) Rubin.get.manager.getModule(BeautifulHands.class);
        if (module == null || !module.enable) return;

        if (side == Arm.RIGHT) {
            matrices.translate(BeautifulHands.rightX.get(), BeautifulHands.rightY.get(), BeautifulHands.rightZ.get());
        } else {
            matrices.translate(BeautifulHands.leftX.get(), BeautifulHands.leftY.get(), BeautifulHands.leftZ.get());
        }
    }

    public void renderItem(AbstractClientPlayerEntity player, ItemStack stack,
                           ItemDisplayContext mode, MatrixStack matrices,
                           OrderedRenderCommandQueue queue, int light,
                           HeldItemRenderer heldItemRenderer) {
        if (!stack.isEmpty()) {
            heldItemRenderer.renderItem(player, stack, mode, matrices, queue, light);
        }
    }

    private float ease(float v) {
        float c1 = 1.70158f;
        float c2 = c1 * 1.525f;
        if (v < 0.5f) {
            float d = 2f * v;
            return d * d * ((c2 + 1f) * d - c2) * 0.5f;
        }
        float s = 2f * v - 2f;
        return (s * s * ((c2 + 1f) * s + c2) + 2f) * 0.5f;
    }

    private float swingRot(float p) {
        if (p < 0.6f) {
            return MathHelper.sin(MathHelper.clamp(p, 0f, 0.12506f) * 12.56f);
        }
        return MathHelper.sin(MathHelper.clamp(p, 0.62532f, 0.75038f) * 12.56f);
    }

    private boolean isLantern(ItemStack s) {
        return s.isOf(Items.LANTERN) || s.isOf(Items.SOUL_LANTERN);
    }

    private boolean isThinBlock(ItemStack s) {
        if (!(s.getItem() instanceof BlockItem)) return false;
        return s.isOf(Items.STRING) || s.isOf(Items.REDSTONE) || s.isOf(Items.LEVER) || s.isOf(Items.TRIPWIRE_HOOK);
    }

    private boolean isTorch(ItemStack s) {
        String n = s.getName().getString().toLowerCase();
        return n.contains("torch") || n.contains("факел");
    }

    private boolean isSmallItem(ItemStack s) {
        return !(s.getItem() instanceof BlockItem)
                && !s.isIn(ItemTags.PICKAXES)
                && !s.isIn(ItemTags.SHOVELS)
                && !s.isIn(ItemTags.HOES)
                && !s.isIn(ItemTags.SWORDS)
                && !s.isIn(ItemTags.AXES)
                && !(s.getItem() instanceof AxeItem)
                && !(s.getItem() instanceof FishingRodItem)
                && !(s.getItem() instanceof BucketItem)
                && s.getUseAction() != UseAction.BOW
                && s.getUseAction() != UseAction.SPEAR
                && s.getUseAction() != UseAction.BLOCK;
    }

    private boolean isSword(ItemStack s) {
        return s.isIn(ItemTags.SWORDS);
    }

    private boolean isWeapon(ItemStack s) {
        return s.isIn(ItemTags.SWORDS) || s.isIn(ItemTags.AXES) || s.getItem() instanceof MaceItem;
    }

    private boolean isTool(ItemStack s) {
        return s.isIn(ItemTags.PICKAXES)
                || s.isIn(ItemTags.SHOVELS)
                || s.isIn(ItemTags.HOES)
                || s.isIn(ItemTags.AXES)
                || s.getUseAction() == UseAction.SPEAR;
    }

    private boolean isShovel(ItemStack s) {
        return s.isIn(ItemTags.SHOVELS) || s.getItem() instanceof ShovelItem;
    }

    private void applySwing(MatrixStack matrices, AbstractClientPlayerEntity player,
                            Hand handIn, ItemStack stack, float swingProgress) {
        boolean mainHand = handIn == Hand.MAIN_HAND;
        if (player.getMainArm() == Arm.LEFT) {
            mainHand = !mainHand;
        }

        BeautifulHands module = (BeautifulHands) Rubin.get.manager.getModule(BeautifulHands.class);
        float ll = mainHand ? 1f : -1f;
        float handDir = handIn == Hand.MAIN_HAND ? 1f : -1f;
        float swingR = swingRot(swingProgress);
        float swing = ease(MathHelper.sin(swingProgress * (float) Math.PI));
        boolean forward = module != null && module.useForwardAttack();
        boolean normal = module != null && module.useNormalAttack();

        if (isSword(stack) && forward) {
            matrices.translate(0.12 * ll * swingR, 0.04 * swingR, -0.95 * swing);
            matrices.translate(0.02 * ll * swing, 0.10 * swing, -0.10 * swingR);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(8f * swingR * ll));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(14f * swingR));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-18f * swingR * ll));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-32f * swing));
            return;
        }

        if (isSword(stack) && normal) {
            applyGenericSwing(matrices, ll, swingR, swing);
            return;
        }

        if (swingLeft || isWeapon(stack) || stack.getUseAction() == UseAction.SPEAR || stack.getUseAction() == UseAction.BLOCK) {
            if (!isShovel(stack)) {
                if (isWeapon(stack)) {
                    matrices.translate(0.8 * ll * swingR, 0.3 * swingR, -0.5 * swing);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15f * swingR * ll));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(20f * swingR));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-70f * swingR * ll));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-(isSword(stack) ? 40f : 30f) * swing));
                    return;
                }
                if (stack.getUseAction() == UseAction.SPEAR) {
                    matrices.translate(0f, 0f, 0.45 * swingR);
                    matrices.translate(-0.25 * handDir * swing, -0.35 * swingR, -0.6 * swing);
                    matrices.translate(0f, 0.1 * swing, 0f);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15f * swingR * ll));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(30f * swingR * ll));
                    return;
                }
                if (isTool(stack) && stack.getUseAction() != UseAction.BLOCK) {
                    matrices.translate(0.1 * ll * swingR, 0.1 * swingR, -0.5 * swing);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30f * swingR));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20f * swingR * ll));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-40f * swing));
                    return;
                }
                if (stack.getUseAction() != UseAction.BLOCK) {
                    matrices.translate(0.1 * ll * swingR, 0.1 * swingR, -0.1 * swing);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30f * swingR));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-10f * swingR * ll));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-40f * swing));
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(10f * swing * ll));
                    return;
                }
                matrices.translate(0.1 * ll * swingR, 0.1 * swingR, -0.2 * swing);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(10f * swingR));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-10f * swingR * ll));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-20f * swing));
                return;
            }
        }

        if (isShovel(stack)) {
            matrices.translate(0f, 0.15 * swingR, -0.25 * swingR);
            matrices.translate(0f, 0f, -0.2 * swing);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15f * swingR));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(35f * swingR));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-30f * swing));
            return;
        }

        if (isSword(stack)) {
            matrices.translate(-0.55 * ll * swingR, -0.8 * swingR, -0.77 * swing);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(5f * swingR * ll));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30f * swingR));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(70f * swingR * ll));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-50f * swing));
            return;
        }

        if (isTool(stack)) {
            matrices.translate(0.1 * ll * swingR, 0.1 * swingR, -0.5 * swing);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30f * swingR));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20f * swingR * ll));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-40f * swing));
            return;
        }

        applyGenericSwing(matrices, ll, swingR, swing);
    }

    private void applyGenericSwing(MatrixStack matrices, float dir, float swingR, float swing) {
        matrices.translate(0.1 * dir * swingR, 0.1 * swingR, -0.1 * swing);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30f * swingR));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-10f * swingR * dir));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-40f * swing));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(10f * swing * dir));
    }

    private void applyBaseHandPose(MatrixStack matrices, Arm side, float equipProgress) {
        int dir = side == Arm.RIGHT ? 1 : -1;
        matrices.translate(dir, -equipProgress * 0.3, 0.3);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45f * dir));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40f * dir));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30f));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(dir * 45f));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(dir * -45f));
        matrices.scale(0.9f, 0.9f, 0.9f);
    }

    private void applyArmPrePose(MatrixStack matrices, ItemStack stack) {
        if (isLantern(stack)) {
            matrices.translate(0.1, 0f, -0.1);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(10f));
            return;
        }
        if (stack.getUseAction() == UseAction.BLOCK) {
            matrices.translate(0f, -0.2f, 0f);
        }
    }

    private void applyEnvironment(MatrixStack matrices, AbstractClientPlayerEntity player,
                                  Hand handIn, Arm side, ItemStack stack,
                                  float swingProgress, float partialTicks) {
        double tt = deltaTime * ANIMATION_SPEED;
        float handDir = handIn == Hand.MAIN_HAND ? 1f : -1f;
        int armDir = side == Arm.RIGHT ? 1 : -1;
        boolean climbing = player.isClimbing() && !player.isOnGround() && Math.abs(player.getVelocity().y) > 0;
        boolean swimming = player.isSwimming();
        boolean elytra = player.isGliding();
        var motion = player.getVelocity();

        if (elytra) {
            if (!physicsUpdated) {
                climbBlend = 0f;
                inWaterCounter = 0f;
                vertAngleY *= (float) Math.pow(0.72, tt);
                vertVelYSlime *= (float) Math.pow(0.72, tt);
                vertAngleYSlime *= (float) Math.pow(0.72, tt);
                physicsUpdated = true;
            }
            if (!stack.isEmpty() && stack.getUseAction() != UseAction.BLOCK) {
                matrices.translate(0f, -0.1f, 0.1f);
            }
            if (isLantern(stack)) {
                matrices.translate(0f, 0.1f, 0f);
            }
            return;
        }

        if (!physicsUpdated) {
            double speed = motion.length();
            if (speed >= 0.08) {
                double cs = Math.min(speed, 0.22);
                double yaw = Math.toRadians(MathHelper.lerp(partialTicks, player.lastYaw, player.getYaw()));
                double fx = -Math.sin(yaw);
                double fz = Math.cos(yaw);
                double dot = motion.x * fx + motion.z * fz;
                double cross = motion.x * fz - motion.z * fx;
                dirCrawlCount += (float) (0.1 * dot * 4 * tt);
                dirCrawlCount += (float) (dot > 0 ? 0.1 * Math.abs(cross) * 4 * tt : 0.1 * Math.abs(cross) * -4 * tt);
                climbCount += (float) (0.1 * cs * 2 * tt);
            }
            if (motion.y > 0) climbCount += (float) (0.1 * tt);
            if (motion.y < 0) climbCount -= (float) (0.1 * tt);

            float motY = player.isOnGround() ? 0f : (float) MathHelper.clamp(motion.y, -0.42, 0.42);
            vertAngleY += motY * 0.015f * tt;
            vertAngleY -= 0.1f * vertAngleY * tt;
            vertAngleY *= (float) Math.pow(0.88, tt);
            vertVelYSlime += motY * 0.015f * tt;
            vertVelYSlime -= 0.1f * vertAngleYSlime * tt;
            vertVelYSlime *= (float) Math.pow(0.88, tt);
            vertAngleYSlime += vertVelYSlime * tt;
            if (player.isSubmergedInWater()) {
                inWaterCounter += (float) (0.1 * tt);
                if (inWaterCounter > 1f) inWaterCounter = 1f;
            } else {
                inWaterCounter *= (float) Math.pow(0.88, tt);
            }
            physicsUpdated = true;
        }

        if (climbing && !player.isUsingItem() && swingProgress == 0f) {
            climbBlend += (float) (0.1 * tt);
            if (climbBlend > 1f) climbBlend = 1f;
            if (!isLantern(stack)) {
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-20f * climbBlend));
            }
        } else {
            climbBlend *= (float) Math.pow(0.88, tt);
        }

        if (swingProgress == 0f) {
            float pitch = player.getPitch();
            matrices.translate(handDir > 0 ? pitch / 650f * climbBlend * -1f : pitch / 650f * climbBlend, 0f, 0f);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch * climbBlend));
        }

        if (!isLantern(stack)) {
            matrices.translate(0f, 0f, player.getPitch() / 120f * climbBlend);
        } else if (swingProgress == 0f) {
            matrices.translate(0f, 0f, player.getPitch() / 80f * climbBlend);
        }

        if (climbing && !isLantern(stack) && !player.isUsingItem()) {
            matrices.translate(0f, 0.1f, -0.2f);
        }

        matrices.translate(0f, 0.02 * inWaterCounter, 0f);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(8f * handDir * inWaterCounter));
        matrices.translate(0f, -vertAngleY, 0f);
        matrices.translate(0f, Math.sin(player.age * 0.1) * 0.007 * armDir, 0f);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(0.15f * MathHelper.sin(player.age * 0.15f) * armDir));

        if (!stack.isEmpty() || climbing || swimming) {
            if (stack.getUseAction() != UseAction.BLOCK) {
                matrices.translate(0f, -0.1f, 0.1f);
            }
        }

        if (isLantern(stack)) {
            matrices.translate(0f, 0.1f, 0f);
            if (swimming) matrices.translate(0f, -0.1f, 0.1f);
        }

        if (swimming && swingProgress == 0f) {
            double dist = (player.age + partialTicks) * 0.2;
            double rot = Math.sin(dist) * 1.5;
            double smooth = rot * 0.8 + prevSwimRotation * 0.2;
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) (handIn == Hand.MAIN_HAND ? smooth : -smooth)));
            matrices.translate(0f, 0f, smooth * 0.2);
            prevSwimRotation = smooth;
        }

        if (climbing && !player.isUsingItem() && swingProgress == 0f) {
            float cp = MathHelper.sin(dirCrawlCount * 4f);
            float ud = MathHelper.cos(dirCrawlCount * 4f);
            if (isLantern(stack)) {
                cp *= 0.14f;
                ud *= 0.14f;
            }
            matrices.translate(0.2 * cp, 0.3 * cp * armDir, -0.2 * cp * armDir);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(25f * cp));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(MathHelper.clamp(20f * ud * armDir, 0f, 20f)));
        }
    }

    private void applyLanternPose(MatrixStack matrices, AbstractClientPlayerEntity player,
                                  Arm side, float swingProgress) {
        float dt = (float) (deltaTime * ANIMATION_SPEED);
        int dir = side == Arm.RIGHT ? 1 : -1;
        float yawDelta = player.lastHeadYaw - player.headYaw;
        float pitchDelta = player.lastPitch - player.getPitch();
        swingVelY += yawDelta * 0.015f * dt;
        swingVelY += swingProgress * 2f * dt;
        swingVelX += pitchDelta * 0.015f * dt;
        swingVelY -= 0.1f * swingAngleY * dt;
        swingVelX -= 0.1f * swingAngleX * dt;
        swingVelY *= (float) Math.pow(0.88, dt);
        swingVelX *= (float) Math.pow(0.88, dt);
        swingAngleY += swingVelY * dt;
        swingAngleX += swingVelX * dt;
        double speed = player.getVelocity().length();
        swingVelZ += (float) (dir > 0 ? ((speed * -15) - swingVelZ) * 0.1 * dt : ((speed * 15) - swingVelZ) * 0.1 * dt);
        if (speed > 0.09 && (player.isOnGround() || player.isSwimming() || player.isClimbing()) && mc.options.getBobView().getValue()) {
            swingVelY += (float) ((Math.random() < 0.5 ? -5.5 : 5.5) * speed * dt);
        }
        matrices.translate(0f, 0f, -0.1f);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(35f * dir + swingAngleY));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15f + swingAngleX));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(75f * dir + swingVelZ));
        matrices.translate(0.3 * dir, -0.35, 0f);
        matrices.translate(0f, 0f, 0.1f);
        matrices.scale(1.5f, 1.5f, 1.5f);
    }

    private void applyItemPose(MatrixStack matrices, AbstractClientPlayerEntity player,
                               Hand handIn, Arm side, ItemStack stack, float swingProgress) {
        int dir = side == Arm.RIGHT ? 1 : -1;
        boolean mainHand = handIn == Hand.MAIN_HAND;
        if (player.getMainArm() == Arm.LEFT) mainHand = !mainHand;

        matrices.translate(-0.3 * dir, 0.65, -0.1);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-65f * dir));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(10f));

        if (stack.getItem() instanceof BlockItem && !(stack.getItem() instanceof BucketItem) && stack.getUseAction() != UseAction.EAT) {
            if (isTorch(stack)) {
                matrices.scale(1.5f, 1.5f, 1.5f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-25f * dir));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(5f));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(75f * dir));
                matrices.translate(0.2 * dir, 0.2, 0.05);
                return;
            }
            if (isThinBlock(stack)) {
                matrices.translate(0f, 0f, -0.1f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-5f * dir));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15f));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(75f * dir));
                return;
            }
            if (isLantern(stack)) {
                applyLanternPose(matrices, player, side, swingProgress);
                return;
            }
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-25f * dir));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(5f));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(75f * dir));
            matrices.translate(0.2 * dir, 0.2, 0.05);
            return;
        }

        if (isSmallItem(stack)) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-5f * dir));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15f));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(75f * dir));
            matrices.translate(0f, -0.05f, -0.1f);
            matrices.scale(0.7f, 0.7f, 0.7f);
            return;
        }

        if (stack.getUseAction() == UseAction.BLOCK && stack.getUseAction() != UseAction.SPEAR) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(160f * dir));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-60f * dir));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(70f));
            matrices.scale(0.75f, 0.75f, 0.75f);
            matrices.translate(0.15 * dir, mainHand ? 0.35 : 0.45, mainHand ? -0.15 : -0.1);
            matrices.translate(0.17 * dir, 0f, 0.3);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90f * dir));
            return;
        }

        if (stack.getUseAction() == UseAction.SPEAR) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-75f * dir));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(45f * dir));
            matrices.translate(-0.3 * dir, 0f, 0f);
            return;
        }

        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-75f * dir));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(70f));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(45f * dir));
        if (stack.getUseAction() != UseAction.BLOCK) {
            matrices.scale(1.2f, 1.2f, 1.2f);
        }
        if (stack.getUseAction() == UseAction.BOW && !player.isUsingItem()) {
            matrices.translate(-0.1 * dir, -0.2, 0f);
        }
    }

    public void render(AbstractClientPlayerEntity player, float partialTicks, Hand handIn,
                       float swingProgress, ItemStack stack, float equipProgress,
                       MatrixStack matrices, OrderedRenderCommandQueue queue, int light, Arm side,
                       HeldItemRenderer heldItemRenderer) {
        boolean right = side == Arm.RIGHT;
        matrices.push();

        if (stack.isEmpty()) {
            applySwing(matrices, player, handIn, stack, swingProgress);
            applyEnvironment(matrices, player, handIn, side, stack, swingProgress, partialTicks);
            applyBaseHandPose(matrices, side, equipProgress);
            renderArmFirstPerson(matrices, queue, light, 0f, 0f, side);
            matrices.pop();
            return;
        }

        applySwing(matrices, player, handIn, stack, swingProgress);
        applyEnvironment(matrices, player, handIn, side, stack, swingProgress, partialTicks);
        applyArmPrePose(matrices, stack);
        applyBaseHandPose(matrices, side, equipProgress);
        renderArmFirstPerson(matrices, queue, light, 0f, 0f, side);
        applyItemPose(matrices, player, handIn, side, stack, swingProgress);
        renderItem(player, stack,
                right ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                matrices, queue, light, heldItemRenderer);
        matrices.pop();
    }
}
