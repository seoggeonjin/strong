package net.ua.mixin2.inner.goals.evoker;

import net.ua.ips.evi;
import net.ua.mixin2.inner.goals.SpellcastingIllagerEntity.LookAtTargetGoal2;

public class LookAtTargetOrWololoGoal extends LookAtTargetGoal2 {
    private evi t;
    public LookAtTargetOrWololoGoal(evi t) {
        super(t);
        this.t = t;
    }

    @Override
    public void tick() {
        if (t.getTarget() != null) {
            t.getLookControl().lookAt(t.getTarget(), t.getMaxHeadRotation(), t.getMaxLookPitchChange());
        } else if (t.getWololoTarget() != null) {
            t.getLookControl().lookAt(t.getWololoTarget(), t.getMaxHeadRotation(), t.getMaxLookPitchChange());
        }
    }
}