package net.ua.mixin2.inner.goals.slime;

import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.Goal;
import net.ua.ips.sli;
import net.ua.mixin2.inner.control.SlimeMoveControl;

import java.util.EnumSet;

public class SwimmingGoal extends Goal {
    private final sli slime;

    public SwimmingGoal(sli slime) {
        this.slime = slime;
        this.setControls(EnumSet.of(Goal.Control.JUMP, Goal.Control.MOVE));
        slime.getNavigation().setCanSwim(true);
    }

    @Override
    public boolean canStart() {
        return (this.slime.isTouchingWater() || this.slime.isInLava()) && this.slime.getMoveControl() instanceof SlimeMoveControl;
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        MoveControl moveControl;
        if (this.slime.getRandom().nextFloat() < 0.8f) {
            this.slime.getJumpControl().setActive();
        }
        if ((moveControl = this.slime.getMoveControl()) instanceof SlimeMoveControl) {
            SlimeMoveControl slimeMoveControl = (SlimeMoveControl)moveControl;
            slimeMoveControl.move(1.2);
        }
    }
}
