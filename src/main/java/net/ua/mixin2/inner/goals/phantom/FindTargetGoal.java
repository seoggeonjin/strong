package net.ua.mixin2.inner.goals.phantom;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.ua.ips.phi;

import java.util.Comparator;
import java.util.List;

public class FindTargetGoal
        extends Goal {
    private final TargetPredicate PLAYERS_IN_RANGE_PREDICATE = TargetPredicate.createAttackable().setBaseMaxDistance(64.0);
    private int delay = FindTargetGoal.toGoalTicks(20);
    private final phi ph;
    public FindTargetGoal(phi ph) {
        this.ph = ph;
    }

    @Override
    public boolean canStart() {
        if (this.delay > 0) {
            --this.delay;
            return false;
        }
        this.delay = FindTargetGoal.toGoalTicks(60);
        List<PlayerEntity> list = ph.getWorld().getPlayers(this.PLAYERS_IN_RANGE_PREDICATE, (LivingEntity) ph, ph.getBoundingBox2().expand(16.0, 64.0, 16.0));
        if (!list.isEmpty()) {
            list.sort(Comparator.comparing(Entity::getY).reversed());
            for (PlayerEntity playerEntity : list) {
                if (!ph.isTarget(playerEntity, TargetPredicate.DEFAULT)) continue;
                ph.setTarget(playerEntity);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean shouldContinue() {
        LivingEntity livingEntity = ph.getTarget();
        if (livingEntity != null) {
            return ph.isTarget(livingEntity, TargetPredicate.DEFAULT);
        }
        return false;
    }
}