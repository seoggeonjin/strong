package net.ua.mixin2.inner.goals.guardian;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.ElderGuardianEntity;
import net.minecraft.entity.mob.GiantEntity;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.entity.passive.AxolotlEntity;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.ua.ips.gui;

import java.util.EnumSet;

public class FireBeamGoal extends Goal {
    private LivingEntity prev = null;
    private final gui guardian;
    public int beamTicks;
    private final boolean elder;

    public FireBeamGoal(gui guardian) {
        this.guardian = guardian;
        this.elder = guardian instanceof ElderGuardianEntity;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        LivingEntity livingEntity = this.guardian.getTarget();
        return livingEntity != null && livingEntity.isAlive();
    }

    @Override
    public boolean shouldContinue() {
        return super.shouldContinue() && (this.guardian.getTarget() != null);
    }

    @Override
    public void start() {
        if (!this.elder){
            this.beamTicks = -10;
            ((GuardianEntity) this.guardian).getNavigation().stop();
            LivingEntity livingEntity = this.guardian.getTarget();
            if (livingEntity != null) {
                ((GuardianEntity) this.guardian).getLookControl().lookAt(livingEntity, 90.0f, 90.0f);
            }
            ((GuardianEntity) this.guardian).velocityDirty = true;
        } else {
            this.beamTicks = -10;
            ((ElderGuardianEntity) this.guardian).getNavigation().stop();
            LivingEntity livingEntity = this.guardian.getTarget();
            if (livingEntity != null) {
                ((ElderGuardianEntity) this.guardian).getLookControl().lookAt(livingEntity, 90.0f, 90.0f);
            }
            ((ElderGuardianEntity) this.guardian).velocityDirty = true;
        }
    }

    @Override
    public void stop() {
        if (!this.elder) {
            this.guardian.setBeamTarget(0);
            ((GuardianEntity) this.guardian).setTarget(null);
            this.guardian.wanderGoal().ignoreChanceOnce();
        } else {
            this.guardian.setBeamTarget(0);
            ((ElderGuardianEntity) this.guardian).setTarget(null);
            this.guardian.wanderGoal().ignoreChanceOnce();
        }
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.elder) {
            LivingEntity livingEntity = this.guardian.getTarget();
            if (livingEntity == null) {
                return;
            }
            if (livingEntity.isDead()) {
                this.beamTicks = -1;
            }
            if (prev != this.guardian.getTarget()) {
                this.beamTicks = -1;
            }
            prev = this.guardian.getTarget();
            ((ElderGuardianEntity)this.guardian).getNavigation().stop();
            ((ElderGuardianEntity)this.guardian).getLookControl().lookAt(livingEntity, 90.0f, 90.0f);
            if (!((ElderGuardianEntity)this.guardian).canSee(livingEntity)) {
                ((ElderGuardianEntity)this.guardian).setTarget(null);
                return;
            }
            ++this.beamTicks;
            if (this.beamTicks == 0) {
                this.guardian.setBeamTarget(livingEntity.getId());
                if (!((ElderGuardianEntity)this.guardian).isSilent()) {
                    ((ElderGuardianEntity)this.guardian).getWorld().sendEntityStatus((Entity) this.guardian, EntityStatuses.PLAY_GUARDIAN_ATTACK_SOUND);
                }
            } else {
                final double x = 30;
                final double y = 30;
                final double z = 30;
                livingEntity.getWorld().getOtherEntities((Entity) this.guardian, Box.of(((Entity) guardian).getPos(), x, y, z)).forEach(e -> {
                    if ((livingEntity instanceof PlayerEntity || livingEntity instanceof SquidEntity || livingEntity instanceof AxolotlEntity || livingEntity instanceof GiantEntity || livingEntity instanceof AbstractSkeletonEntity) && livingEntity.squaredDistanceTo((Entity) this.guardian) > 4.0)
                        e.damage(((ElderGuardianEntity) this.guardian).getDamageSources().indirectMagic(((ElderGuardianEntity) this.guardian), ((ElderGuardianEntity) this.guardian)), dam(beamTicks));
                });
                // livingEntity.damage(((GuardianEntity)this.guardian).getDamageSources().mobAttack(((GuardianEntity)this.guardian)), (float)((GuardianEntity)this.guardian).getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
                // ((ElderGuardianEntity)this.guardian).setTarget(null);
            }
            super.tick();
        } else {
            LivingEntity livingEntity = this.guardian.getTarget();
            if (livingEntity == null) {
                return;
            }
            if (livingEntity.isDead()) {
                this.beamTicks = -1;
            }
            if (prev != this.guardian.getTarget()) {
                this.beamTicks = -1;
            }
            prev = this.guardian.getTarget();
            ((GuardianEntity)this.guardian).getNavigation().stop();
            ((GuardianEntity)this.guardian).getLookControl().lookAt(livingEntity, 90.0f, 90.0f);
            if (!((GuardianEntity)this.guardian).canSee(livingEntity)) {
                ((GuardianEntity)this.guardian).setTarget(null);
                return;
            }
            ++this.beamTicks;
            if (this.beamTicks == 0) {
                this.guardian.setBeamTarget(livingEntity.getId());
                if (!((GuardianEntity)this.guardian).isSilent()) {
                    ((GuardianEntity)this.guardian).getWorld().sendEntityStatus((Entity) this.guardian, EntityStatuses.PLAY_GUARDIAN_ATTACK_SOUND);
                }
            } else {
                livingEntity.damage(((GuardianEntity) this.guardian).getDamageSources().indirectMagic(((GuardianEntity) this.guardian), ((GuardianEntity) this.guardian)), dam(beamTicks));
                // livingEntity.damage(((GuardianEntity)this.guardian).getDamageSources().mobAttack(((GuardianEntity)this.guardian)), (float)((GuardianEntity)this.guardian).getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
                // ((GuardianEntity)this.guardian).setTarget(null);
            }
            super.tick();
        }
    }
    public float dam(int tick) {
        int result;
        int t1 = (this.elder ? tick / 5 : tick / 12) + 1;
        result = (int) Math.pow(t1, t1);
        return result;
    }
}