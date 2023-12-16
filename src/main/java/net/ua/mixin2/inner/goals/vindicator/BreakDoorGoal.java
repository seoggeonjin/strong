package net.ua.mixin2.inner.goals.vindicator;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.ua.ips.ivi;

import java.util.EnumSet;

import static net.ua.util.Variables.DIFFICULTY_ALLOWS_DOOR_BREAKING_PREDICATE;

public class BreakDoorGoal extends net.minecraft.entity.ai.goal.BreakDoorGoal {
    public BreakDoorGoal(MobEntity mobEntity) {
        super(mobEntity, 6, DIFFICULTY_ALLOWS_DOOR_BREAKING_PREDICATE);
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }

    @Override
    public boolean shouldContinue() {
        ivi vindicatorEntity = (ivi)this.mob;
        return vindicatorEntity.hasActiveRaid() && super.shouldContinue();
    }

    @Override
    public boolean canStart() {
        ivi vindicatorEntity = (ivi)this.mob;
        return vindicatorEntity.hasActiveRaid() && vindicatorEntity.random().nextInt(toGoalTicks(10)) == 0 && super.canStart();
    }

    @Override
    public void start() {
        super.start();
        this.mob.setDespawnCounter(0);
    }
}