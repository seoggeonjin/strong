package net.ua.mixin2.inner.goals.slime;

import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.effect.StatusEffects;
import net.ua.ips.sli;
import net.ua.mixin2.inner.control.SlimeMoveControl;

import java.util.EnumSet;

public class RandomLookGoal
        extends Goal {
    private final sli slime;
    private float targetYaw;
    private int timer;

    public RandomLookGoal(sli slime) {
        this.slime = slime;
        this.setControls(EnumSet.of(Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        return this.slime.getTarget() == null && (this.slime.isOnGround() || this.slime.isTouchingWater() || this.slime.isInLava() || this.slime.hasStatusEffect(StatusEffects.LEVITATION)) && this.slime.getMoveControl() instanceof SlimeMoveControl;
    }

    @Override
    public void tick() {
        MoveControl moveControl;
        if (--this.timer <= 0) {
            this.timer = this.getTickCount(40 + this.slime.getRandom().nextInt(60));
            this.targetYaw = this.slime.getRandom().nextInt(360);
        }
        if ((moveControl = this.slime.getMoveControl()) instanceof SlimeMoveControl) {
            SlimeMoveControl slimeMoveControl = (SlimeMoveControl)moveControl;
            slimeMoveControl.look(this.targetYaw, false);
        }
    }
}
