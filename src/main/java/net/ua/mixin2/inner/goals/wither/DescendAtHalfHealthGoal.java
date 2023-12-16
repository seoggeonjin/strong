package net.ua.mixin2.inner.goals.wither;

import net.minecraft.entity.ai.goal.Goal;
import net.ua.ips.wi;

import java.util.EnumSet;

public class DescendAtHalfHealthGoal extends Goal {
    private wi w;
    public DescendAtHalfHealthGoal(wi w) {
        this.w = w;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.JUMP, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        return w.getInvulnerableTimer() > 0;
    }
}

