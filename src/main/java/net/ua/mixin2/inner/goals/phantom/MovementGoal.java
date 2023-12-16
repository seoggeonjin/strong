package net.ua.mixin2.inner.goals.phantom;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PhantomEntity;
import net.ua.ips.phi;

import java.util.EnumSet;

public abstract class MovementGoal extends Goal {
    private final phi ph;
    public MovementGoal(phi ph) {
        this.setControls(EnumSet.of(Goal.Control.MOVE));
        this.ph = ph;
    }

    protected boolean isNearTarget() {
        return ph.targetPosition().squaredDistanceTo(ph.getX2(), ph.getY2(), ph.getZ2()) < 4.0;
    }
}