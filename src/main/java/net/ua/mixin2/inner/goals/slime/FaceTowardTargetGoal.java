package net.ua.mixin2.inner.goals.slime;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.Goal;
import net.ua.ips.sli;
import net.ua.mixin2.inner.control.SlimeMoveControl;

import java.util.EnumSet;

public class FaceTowardTargetGoal
        extends Goal {
    private final sli slime;
    private int ticksLeft;

    public FaceTowardTargetGoal(sli slime) {
        this.slime = slime;
        this.setControls(EnumSet.of(Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        LivingEntity livingEntity = this.slime.getTarget();
        if (livingEntity == null) {
            return false;
        }
        if (!this.slime.canTarget(livingEntity)) {
            return false;
        }
        return this.slime.getMoveControl() instanceof SlimeMoveControl;
    }

    @Override
    public void start() {
        this.ticksLeft = FaceTowardTargetGoal.toGoalTicks(300);
        super.start();
    }

    @Override
    public boolean shouldContinue() {
        LivingEntity livingEntity = this.slime.getTarget();
        if (livingEntity == null) {
            return false;
        }
        if (!this.slime.canTarget(livingEntity)) {
            return false;
        }
        return --this.ticksLeft > 0;
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        MoveControl moveControl;
        LivingEntity livingEntity = this.slime.getTarget();
        if (livingEntity != null) {
            this.slime.lookAtEntity(livingEntity, 10.0f, 10.0f);
        }
        if ((moveControl = this.slime.getMoveControl()) instanceof SlimeMoveControl) {
            SlimeMoveControl slimeMoveControl = (SlimeMoveControl)moveControl;
            slimeMoveControl.look(this.slime.getYaw(), this.slime.canAttack());
        }
    }
}