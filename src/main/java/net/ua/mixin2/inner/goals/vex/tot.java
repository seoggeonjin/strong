package net.ua.mixin2.inner.goals.vex;

import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.TrackTargetGoal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.mob.VexEntity;

import java.util.Objects;

public class tot extends TrackTargetGoal {
    private final TargetPredicate targetPredicate;
    VexEntity v;

    public tot(PathAwareEntity mob, VexEntity v) {
        super(mob, false);
        this.v = v;
        this.targetPredicate = TargetPredicate.createNonAttackable().ignoreVisibility().ignoreDistanceScalingFactor();
    }

    @Override
    public boolean canStart() {
        return v.getOwner() != null && v.getOwner().getTarget() != null && this.canTrack(v.getOwner().getTarget(), this.targetPredicate);
    }

    @Override
    public void start() {
        v.setTarget(Objects.requireNonNull(v.getOwner()).getTarget());
        super.start();
    }
}
