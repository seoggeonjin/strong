package net.ua.mixin.otherwise.goal;

import java.util.EnumSet;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;

@Mixin(net.minecraft.entity.ai.goal.ProjectileAttackGoal.class)
public class ProjectileAttackGoal extends Goal {
    @Mutable
    @Final
    @Shadow
    private final MobEntity mob;
    @Mutable
    @Final
    @Shadow
    private final RangedAttackMob owner;
    @Shadow
    @Nullable
    private LivingEntity target;
    @Shadow
    private int updateCountdownTicks = -1;
    @Mutable
    @Final
    @Shadow
    private final double mobSpeed;
    @Shadow
    private int seenTargetTicks;
    @Mutable
    @Final
    @Shadow
    private final int minIntervalTicks;
    @Mutable
    @Final
    @Shadow
    private final int maxIntervalTicks;
    @Mutable
    @Final
    @Shadow
    private final float maxShootRange;
    @Mutable
    @Final
    @Shadow
    private final float squaredMaxShootRange;

    public ProjectileAttackGoal(RangedAttackMob mob, double mobSpeed, int intervalTicks, float maxShootRange) {
        this(mob, mobSpeed, intervalTicks, intervalTicks, maxShootRange);
    }

    public ProjectileAttackGoal(RangedAttackMob mob, double mobSpeed, int minIntervalTicks, int maxIntervalTicks, float maxShootRange) {
        if (!(mob instanceof LivingEntity)) {
            throw new IllegalArgumentException("ArrowAttackGoal requires Mob implements RangedAttackMob");
        }
        this.owner = mob;
        this.mob = (MobEntity) mob;
        this.mobSpeed = mobSpeed;
        this.minIntervalTicks = minIntervalTicks;
        this.maxIntervalTicks = maxIntervalTicks;
        this.maxShootRange = maxShootRange;
        this.squaredMaxShootRange = maxShootRange * maxShootRange;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Overwrite
    public boolean canStart() {
        LivingEntity livingEntity = this.mob.getTarget();
        if (livingEntity == null || !livingEntity.isAlive()) {
            return false;
        }
        this.target = livingEntity;
        return true;
    }

    @Overwrite
    public boolean shouldContinue() {
        if (this.canStart()) return true;
        assert this.target != null;
        return this.target.isAlive() && !this.mob.getNavigation().isIdle();
    }

    @Overwrite
    public void stop() {
        this.target = null;
        this.seenTargetTicks = 0;
        this.updateCountdownTicks = -1;
    }

    @Overwrite
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Overwrite
    public void tick() {
        assert this.target != null;
        double d = this.mob.squaredDistanceTo(this.target.getX(), this.target.getY(), this.target.getZ());
        boolean bl = this.mob.getVisibilityCache().canSee(this.target);
        this.seenTargetTicks = bl ? ++this.seenTargetTicks : 0;
        if (d > (double)this.squaredMaxShootRange || this.seenTargetTicks < 5) {
            this.mob.getNavigation().startMovingTo(this.target, this.mobSpeed);
        } else {
            this.mob.getNavigation().stop();
        }
        this.mob.getLookControl().lookAt(this.target, 30.0f, 30.0f);
        // if (--this.updateCountdownTicks == 0) {
        //     if (!bl) {
        //         return;
        //     }
        //     float f = (float)Math.sqrt(d) / this.maxShootRange;
        //     float g = MathHelper.clamp(f, 0.1f, 1.0f);
        //     this.owner.shootAt(this.target, g);
        //     this.updateCountdownTicks = MathHelper.floor(f * (float)(this.maxIntervalTicks - this.minIntervalTicks) + (float)this.minIntervalTicks);
        // } else if (this.updateCountdownTicks < 0) {
        //     this.updateCountdownTicks = MathHelper.floor(MathHelper.lerp(Math.sqrt(d) / (double)this.maxShootRange, this.minIntervalTicks, this.maxIntervalTicks));
        // }
        for (int i = 0; i < 5; i++) {
            this.owner.shootAt(this.target, 1);
        }
    }
}
