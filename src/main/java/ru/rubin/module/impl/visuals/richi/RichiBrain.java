package ru.rubin.module.impl.visuals.richi;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.rubin.Rubin;
import ru.rubin.module.impl.combat.HitAura;

@Environment(EnvType.CLIENT)
public class RichiBrain {
    private Vec3d pos;
    private Vec3d renderPos;
    private Vec3d prevRenderPos;
    private Vec3d motion = Vec3d.ZERO;

    private float bodyYaw;
    private float headYawOffset;
    private float pitch;

    private boolean lay;
    private long lastMoveTime = System.currentTimeMillis();

    public float prevLimbSwingAmount;
    public float limbSwingAmount;
    public float limbSwing;

    private PlayerEntity owner;

    public void setEntity(PlayerEntity owner) {
        this.owner = owner;
    }

    public void tick(MinecraftClient mc) {
        if (owner == null || mc == null || mc.world == null) return;

        Vec3d ownerPos = new Vec3d(owner.getX(), owner.getY(), owner.getZ());
        if (pos == null || pos.distanceTo(ownerPos) > 12.0) {
            pos = ownerPos.add(-0.8, 0.0, -0.8);
            renderPos = pos;
            prevRenderPos = pos;
            motion = Vec3d.ZERO;
        }

        LivingEntity auraTarget = null;
        if (Rubin.get != null && Rubin.get.manager != null) {
            HitAura hitAura = (HitAura) Rubin.get.manager.getModule(HitAura.class);
            if (hitAura != null && hitAura.enable) {
                auraTarget = HitAura.target;
            }
        }

        Vec3d desiredPos;
        Vec3d lookPos;
        double desiredDistance;
        boolean hasAuraTarget = auraTarget != null && auraTarget.isAlive();

        if (hasAuraTarget) {
            desiredPos = new Vec3d(auraTarget.getX(), auraTarget.getY(), auraTarget.getZ());
            lookPos = new Vec3d(auraTarget.getX(), auraTarget.getY(), auraTarget.getZ())
                    .add(0.0, auraTarget.getStandingEyeHeight(), 0.0);
            desiredDistance = 1.8;
        } else {
            desiredPos = ownerPos;
            lookPos = owner.getEyePos();
            desiredDistance = 2.0;
        }

        Vec3d toTarget = desiredPos.subtract(pos);
        Vec3d toTargetFlat = new Vec3d(toTarget.x, 0.0, toTarget.z);
        double flatDistance = toTargetFlat.length();

        Vec3d accel = Vec3d.ZERO;
        if (flatDistance > desiredDistance) {
            accel = toTargetFlat.normalize().multiply(hasAuraTarget ? 0.10 : 0.08);
        } else if (!hasAuraTarget && flatDistance < 0.1) {
            double randomYaw = Math.random() * Math.PI * 2.0;
            accel = new Vec3d(Math.cos(randomYaw) * 0.1, 0.0, Math.sin(randomYaw) * 0.1);
        }

        motion = motion.add(accel);
        motion = new Vec3d(motion.x * 0.84, motion.y, motion.z * 0.84);

        Vec3d next = pos.add(motion);
        next = keepAboveGround(mc, next);

        if (isSolid(mc, next)) {
            next = pos;
            motion = new Vec3d(-motion.x * 0.35, 0.0, -motion.z * 0.35);
        }

        pos = next;

        prevRenderPos = renderPos == null ? pos : renderPos;
        renderPos = renderPos == null ? pos : renderPos.lerp(pos, 0.35);

        updateRotation(lookPos);
        updateLimbSwing();
        updateLayState(ownerPos, hasAuraTarget);
    }

    private void updateRotation(Vec3d lookPos) {
        if (motion.horizontalLengthSquared() > 1.0E-4) {
            double angle = Math.atan2(motion.z, motion.x);
            float wantedBody = (float) Math.toDegrees(angle) - 90.0f;
            float delta = MathHelper.wrapDegrees(wantedBody - bodyYaw);
            bodyYaw += delta * 0.35f;
        }

        Vec3d from = pos.add(0.0, 0.5, 0.0);
        Vec3d dir = lookPos.subtract(from);
        double distXZ = Math.sqrt(dir.x * dir.x + dir.z * dir.z);

        float wantedYaw = (float) (Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90.0);
        float wantedPitch = (float) (-Math.toDegrees(Math.atan2(dir.y, Math.max(1.0E-4, distXZ))));
        float yawDelta = MathHelper.wrapDegrees(wantedYaw - bodyYaw);
        headYawOffset += (MathHelper.clamp(yawDelta, -80.0f, 80.0f) - headYawOffset) * 0.35f;
        pitch += (wantedPitch - pitch) * 0.25f;
    }

    private void updateLimbSwing() {
        prevLimbSwingAmount = limbSwingAmount;

        if (renderPos == null || prevRenderPos == null) {
            limbSwingAmount *= 0.9f;
            limbSwing += limbSwingAmount;
            return;
        }

        double dx = renderPos.x - prevRenderPos.x;
        double dz = renderPos.z - prevRenderPos.z;
        float renderSpeed = MathHelper.sqrt((float) (dx * dx + dz * dz)) * 9.0f;
        float motionSpeed = (float) Math.sqrt(motion.x * motion.x + motion.z * motion.z) * 7.5f;
        float speed = Math.max(renderSpeed, motionSpeed);
        speed = Math.min(speed, 1.0f);

        limbSwingAmount += (speed - limbSwingAmount) * 0.4f;
        limbSwing += limbSwingAmount;
    }

    private void updateLayState(Vec3d ownerPos, boolean hasAuraTarget) {
        if (hasAuraTarget) {
            lay = false;
            lastMoveTime = System.currentTimeMillis();
            return;
        }

        double distToOwner = pos.distanceTo(ownerPos);
        boolean moving = motion.horizontalLengthSquared() > 0.0008;

        if (moving || distToOwner > 2.0) {
            lastMoveTime = System.currentTimeMillis();
            lay = false;
            return;
        }

        lay = System.currentTimeMillis() - lastMoveTime > 1000L;
    }

    private Vec3d keepAboveGround(MinecraftClient mc, Vec3d value) {
        BlockPos below = BlockPos.ofFloored(value.x, value.y - 0.15, value.z);
        if (mc.world.getBlockState(below).blocksMovement()) {
            double y = below.getY() + 1.02;
            return new Vec3d(value.x, y, value.z);
        }
        BlockPos twoBelow = BlockPos.ofFloored(value.x, value.y - 1.15, value.z);
        if (mc.world.getBlockState(twoBelow).blocksMovement()) {
            double y = twoBelow.getY() + 1.02;
            return new Vec3d(value.x, y, value.z);
        }
        return value.add(0.0, -0.04, 0.0);
    }

    private boolean isSolid(MinecraftClient mc, Vec3d value) {
        BlockPos pos = BlockPos.ofFloored(value.x, value.y + 0.2, value.z);
        return mc.world.getBlockState(pos).blocksMovement();
    }

    public Vec3d getPos(float tickDelta) {
        if (renderPos == null) return pos;
        if (prevRenderPos == null) return renderPos;
        return prevRenderPos.lerp(renderPos, MathHelper.clamp(tickDelta, 0.0f, 1.0f));
    }

    public float getBodyYaw() { return bodyYaw; }
    public float getHeadYawOffset() { return headYawOffset; }
    public float getPitch() { return pitch; }
    public boolean isLay() { return lay; }
}
