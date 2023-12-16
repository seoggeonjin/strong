package net.ua.mixin2.inner.goals.slime;

import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.Goal;
import net.ua.ips.sli;
import net.ua.mixin2.inner.control.SlimeMoveControl;

import java.util.EnumSet;

public class MoveGoal extends Goal {
    private final sli slime;

    public MoveGoal(sli slime) {
        this.slime = slime;
        this.setControls(EnumSet.of(Goal.Control.JUMP, Goal.Control.MOVE));
    }

    @Override
    public boolean canStart() {
        return !this.slime.hasVehicle();
    }

    @Override
    public void tick() {
        MoveControl moveControl = this.slime.getMoveControl();
        if (moveControl instanceof SlimeMoveControl slimeMoveControl) {
            slimeMoveControl.move(1.0);
        }
    }
}