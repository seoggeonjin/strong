package net.ua.mixin2.inner.goals.creeper;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.ua.ips.Ee;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class ChasePlayerGoal extends Goal {
    private final Ee enderman;
    @Nullable
    private LivingEntity target;

    public ChasePlayerGoal(Ee enderman) {
        this.enderman = enderman;
        this.setControls(EnumSet.of(Goal.Control.JUMP, Goal.Control.MOVE));
    }

    @Override
    public boolean canStart() {
        this.target = this.enderman.getTarget();
        if (!(this.target instanceof PlayerEntity)) {
            return false;
        }
        double d = this.target.squaredDistanceTo((Entity) this.enderman);
        if (d > 256.0) {
            return false;
        }
        return this.enderman.isPlayerStaring((PlayerEntity)this.target);
    }

    @Override
    public void start() {
        this.enderman.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.enderman.getLookControl().lookAt(this.target.getX(), this.target.getEyeY(), this.target.getZ());
    }
}