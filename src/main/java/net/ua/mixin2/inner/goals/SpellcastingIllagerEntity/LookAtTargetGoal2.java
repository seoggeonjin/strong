package net.ua.mixin2.inner.goals.SpellcastingIllagerEntity;

import net.minecraft.entity.ai.goal.Goal;
import net.ua.ips.spellI;
import net.ua.mixin2.inner.Spell;

import java.util.EnumSet;

public class LookAtTargetGoal2 extends Goal {
    private final spellI ie;
    public LookAtTargetGoal2(spellI ie) {
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        this.ie = ie;
    }

    @Override
    public boolean canStart() {
        return ie.getSpellTicks() > 0;
    }

    @Override
    public void start() {
        super.start();
        ie.navigation().stop();
    }

    @Override
    public void stop() {
        super.stop();
        ie.setSpell(Spell.NONE);
    }

    @Override
    public void tick() {
        if (ie.getTarget() != null) {
            ie.getLookControl().lookAt(ie.getTarget(), ie.getMaxHeadRotation(), ie.getMaxLookPitchChange());
        }
    }
}