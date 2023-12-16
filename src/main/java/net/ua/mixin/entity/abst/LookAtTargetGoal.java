package net.ua.mixin.entity.abst;

import net.minecraft.entity.ai.goal.Goal;
import net.ua.ips.spellI;
import net.ua.mixin2.inner.Spell;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.EnumSet;

@Mixin(targets = "net.minecraft.entity.mob.SpellcastingIllagerEntity%LookAtTargetGoal")
public class LookAtTargetGoal extends Goal {
    @Shadow
    private spellI ie;
    public LookAtTargetGoal(spellI ie) {
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
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