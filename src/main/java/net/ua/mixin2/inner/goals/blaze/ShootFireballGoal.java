package net.ua.mixin2.inner.goals.blaze;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.world.WorldEvents;
import net.ua.ips.bls;

import java.util.EnumSet;

public class ShootFireballGoal extends Goal {
    private final bls blaze;
    private int fireballsFired;
    private int targetNotVisibleTicks;

    public ShootFireballGoal(bls blaze) {
        this.blaze = blaze;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        LivingEntity livingEntity = this.blaze.getTarget();
        return livingEntity != null && livingEntity.isAlive() && this.blaze.canTarget(livingEntity);
    }

    @Override
    public void start() {
        this.fireballsFired = 0;
    }

    @Override
    public void stop() {
        this.blaze.setFireActive(false);
        this.targetNotVisibleTicks = 0;
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity livingEntity = this.blaze.getTarget();
        if (livingEntity == null) {
            return;
        }
        boolean bl = this.blaze.getVisibilityCache().canSee(livingEntity);
        this.targetNotVisibleTicks = bl ? 0 : ++this.targetNotVisibleTicks;
        double d = this.blaze.squaredDistanceTo(livingEntity);
        if (d < 4.0) {
            if (!bl) {
                return;
            }
            this.blaze.tryAttack(livingEntity);
            this.blaze.getMoveControl().moveTo(livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), 1.0);
        } else if (d < this.getFollowRange() * this.getFollowRange() && bl) {
            double e = livingEntity.getX() - this.blaze.getX2();
            double f = livingEntity.getBodyY(0.5) - this.blaze.getBodyY(0.5);
            double g = livingEntity.getZ() - this.blaze.getZ2();
            ++this.fireballsFired;
            if (this.fireballsFired == 1) {
                this.blaze.setFireActive(true);
            } else if (this.fireballsFired <= 4) {
            } else {
                this.fireballsFired = 0;
                this.blaze.setFireActive(false);
            }
            if (this.fireballsFired > 1) {
                double h = Math.sqrt(Math.sqrt(d)) * 0.5;
                if (!this.blaze.isSilent()) {
                    this.blaze.getWorld().syncWorldEvent(null, WorldEvents.BLAZE_SHOOTS, this.blaze.getBlockPos(), 0);
                }
                for (int i = 0; i < 8; ++i) {
                    SmallFireballEntity smallFireballEntity = new SmallFireballEntity(this.blaze.getWorld(), (LivingEntity) this.blaze, this.blaze.getRandom().nextTriangular(e, 2.297 * h), f, this.blaze.getRandom().nextTriangular(g, 2.297 * h));
                    smallFireballEntity.setPosition(smallFireballEntity.getX(), this.blaze.getBodyY(0.5) + 0.5, smallFireballEntity.getZ());
                    this.blaze.getWorld().spawnEntity(smallFireballEntity);
                }
            }
            this.blaze.getLookControl().lookAt(livingEntity, 10.0f, 10.0f);
        } else if (this.targetNotVisibleTicks < 5) {
            this.blaze.getMoveControl().moveTo(livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), 1.0);
        }
        super.tick();
    }

    private double getFollowRange() {
        return this.blaze.getAttributeValue(EntityAttributes.GENERIC_FOLLOW_RANGE);
    }
}
