package net.ua.mixin.otherwise.goal;

import java.util.EnumSet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.*;

@Mixin(net.minecraft.entity.ai.goal.BowAttackGoal.class)
public class BowAttackGoal<T extends HostileEntity> extends Goal {
    @Mutable
    @Final
    @Shadow
    private final T actor;
    @Mutable
    @Final
    @Shadow
    private final double speed;
    @Shadow
    private int attackInterval;
    @Mutable
    @Final
    @Shadow
    private final float squaredRange;
    @Shadow
    private int cooldown = -1;
    @Shadow
    private int targetSeeingTicker;
    @Shadow
    private boolean movingToLeft;
    @Shadow
    private boolean backward;
    @Shadow
    private int combatTicks = -1;

    public BowAttackGoal(T actor, double speed, int attackInterval, float range) {
        this.actor = actor;
        this.speed = speed;
        this.attackInterval = attackInterval;
        this.squaredRange = range * range;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Overwrite
    public void setAttackInterval(int attackInterval) {
        this.attackInterval = attackInterval;
    }

    @Overwrite
    public boolean canStart() {
        if (this.actor.getTarget() == null) {
            return false;
        }
        return this.isHoldingBow();
    }

    @Overwrite
    public boolean isHoldingBow() {
        return this.actor.isHolding(Items.BOW);
    }

    @Overwrite
    public boolean shouldContinue() {
        return (this.canStart() || !this.actor.getNavigation().isIdle()) && this.isHoldingBow();
    }

    @Overwrite
    public void start() {
        super.start();
        this.actor.setAttacking(true);
    }

    @Overwrite
    public void stop() {
        super.stop();
        this.actor.setAttacking(false);
        this.targetSeeingTicker = 0;
        this.cooldown = -1;
        this.actor.clearActiveItem();
    }

    @Overwrite
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Overwrite
    public void tick() {
        boolean bl2;
        LivingEntity livingEntity = this.actor.getTarget();
        if (livingEntity == null) {
            return;
        }
        double d = this.actor.squaredDistanceTo(livingEntity.getX(), livingEntity.getY(), livingEntity.getZ());
        boolean bl = this.actor.getVisibilityCache().canSee(livingEntity);
        bl2 = this.targetSeeingTicker > 0;
        if (bl != bl2) {
            this.targetSeeingTicker = 0;
        }
        if (bl) {
            ++this.targetSeeingTicker;
        } else {
            --this.targetSeeingTicker;
        }
        if (d > (double)this.squaredRange || this.targetSeeingTicker < 20) {
            this.actor.getNavigation().startMovingTo(livingEntity, this.speed);
            this.combatTicks = -1;
        } else {
            this.actor.getNavigation().stop();
            ++this.combatTicks;
        }
        if (this.combatTicks >= 20) {
            if ((double) this.actor.getRandom().nextFloat() < 0.3) {
                this.movingToLeft = !this.movingToLeft;
            }
            if ((double) this.actor.getRandom().nextFloat() < 0.3) {
                this.backward = !this.backward;
            }
            this.combatTicks = 0;
        }
        if (this.combatTicks > -1) {
            if (d > (double)(this.squaredRange * 0.75f)) {
                this.backward = false;
            } else if (d < (double)(this.squaredRange * 0.25f)) {
                this.backward = true;
            }
            this.actor.getMoveControl().strafeTo(this.backward ? -0.5f : 0.5f, this.movingToLeft ? 0.5f : -0.5f);
            Entity entity = this.actor.getControllingVehicle();
            if (entity instanceof MobEntity mobEntity) {
                mobEntity.lookAtEntity(livingEntity, 30.0f, 30.0f);
            }
            this.actor.lookAtEntity(livingEntity, 30.0f, 30.0f);
        } else {
            this.actor.getLookControl().lookAt(livingEntity, 30.0f, 30.0f);
        }
        for (int i = 0; i < 1; i++) {
            ((RangedAttackMob)this.actor).shootAt(livingEntity, BowItem.getPullProgress(3));
        }
    }
}