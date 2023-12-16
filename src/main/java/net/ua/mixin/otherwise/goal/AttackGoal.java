package net.ua.mixin.otherwise.goal;

import java.util.EnumSet;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.*;

/**
 * Goal that causes its mob to follow and attack its selected target.
 */
@Mixin(net.minecraft.entity.ai.goal.AttackGoal.class)
public class AttackGoal extends Goal {
    @Mutable
    @Final
    @Shadow
    private final MobEntity mob;
    @Shadow
    private LivingEntity target;
    @Shadow
    private int cooldown;

    public AttackGoal(MobEntity mob) {
        this.mob = mob;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Overwrite
    public boolean canStart() {
        LivingEntity livingEntity = this.mob.getTarget();
        if (livingEntity == null) {
            return false;
        }
        this.target = livingEntity;
        return true;
    }

    @Overwrite
    public boolean shouldContinue() {
        if (!this.target.isAlive()) {
            return false;
        }
        if (this.mob.squaredDistanceTo(this.target) > 225.0) {
            return false;
        }
        return !this.mob.getNavigation().isIdle() || this.canStart();
    }

    @Overwrite
    public void stop() {
        this.target = null;
        this.mob.getNavigation().stop();
    }

    @Overwrite
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Overwrite
    public void tick() {
        this.mob.getLookControl().lookAt(this.target, 30.0f, 30.0f);
        double d = this.mob.getWidth() * 2.0f * (this.mob.getWidth() * 2.0f);
        double e = this.mob.squaredDistanceTo(this.target.getX(), this.target.getY(), this.target.getZ());
        double f = 0.8;
        if (e > d && e < 16.0) {
            f = 1.33;
        } else if (e < 225.0) {
            f = 0.6;
        }
        this.mob.getNavigation().startMovingTo(this.target, f);
        this.cooldown = Math.max(this.cooldown - 1, 0);
        if (e > d) {
            return;
        }
        if (this.cooldown > 0) {
            return;
        }
        if (mob.getType() == EntityType.SKELETON) {
            this.cooldown = 5000;
        } else {
            this.cooldown = 5000;
        }
        this.mob.tryAttack(this.target);
    }
}