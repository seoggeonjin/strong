package net.ua.mixin2.inner.goals.vindicator;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.mob.MobEntity;
import net.ua.ips.ivi;

public class TargetGoal
        extends ActiveTargetGoal<LivingEntity> {
    public TargetGoal(ivi vindicator) {
        super((MobEntity) vindicator, LivingEntity.class, 0, true, true, LivingEntity::isMobOrPlayer);
    }

    @Override
    public boolean canStart() {
        return ((ivi)this.mob).johnny() && super.canStart();
    }

    @Override
    public void start() {
        super.start();
        this.mob.setDespawnCounter(0);
    }
}