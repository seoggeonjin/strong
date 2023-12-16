package net.ua.mixin2.inner.goals.vex;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

public class ctg extends Goal {
    VexEntity v;
    public ctg(VexEntity v) {
        this.v = v;
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }

    @Override
    public boolean canStart() {
        LivingEntity livingEntity = v.getTarget();
        if (livingEntity != null && livingEntity.isAlive() && !v.getMoveControl().isMoving() && v.getRandom().nextInt(toGoalTicks(7)) == 0) {
            return v.squaredDistanceTo(livingEntity) > 4.0;
        }
        return false;
    }

    @Override
    public boolean shouldContinue() {
        return v.getMoveControl().isMoving() && v.isCharging() && v.getTarget() != null && v.getTarget().isAlive();
    }

    @Override
    public void start() {
        LivingEntity livingEntity = v.getTarget();
        if (livingEntity != null) {
            Vec3d vec3d = livingEntity.getEyePos();
            v.getMoveControl().moveTo(vec3d.x, vec3d.y, vec3d.z, 1.0);
        }
        v.setCharging(true);
        v.playSound(SoundEvents.ENTITY_VEX_CHARGE, 1.0f, 1.0f);
    }

    @Override
    public void stop() {
        v.setCharging(false);
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity livingEntity = v.getTarget();
        if (livingEntity == null) {
            return;
        }
        if (v.getBoundingBox().intersects(livingEntity.getBoundingBox())) {
            v.tryAttack(livingEntity);
            v.setCharging(false);
        } else {
            double d = v.squaredDistanceTo(livingEntity);
            if (d < 9.0) {
                Vec3d vec3d = livingEntity.getEyePos();
                v.getMoveControl().moveTo(vec3d.x, vec3d.y, vec3d.z, 1.0);
            }
        }
    }
}