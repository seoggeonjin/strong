package net.ua.mixin2.inner.goals.illusionor;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.ua.ips.spellI2;
import net.ua.ips.spellI3;
import net.ua.mixin2.inner.Spell;
import net.ua.mixin2.inner.goals.SpellcastingIllagerEntity.CastSpellGoal2;
import org.jetbrains.annotations.Nullable;

public class GiveInvisibilityGoal2 extends CastSpellGoal2 {
    private final spellI3 ie;
    public GiveInvisibilityGoal2(spellI3 ie) {
        super(ie);
        this.ie = ie;
    }

    public boolean canStart() {
        if (!super.canStart()) {
            return false;
        }
        return !ie.hasStatusEffect(StatusEffects.INVISIBILITY);
    }

    public int getSpellTicks() {
        return 20;
    }

    public int startTimeDelay() {
        return 340;
    }

    public void castSpell() {
        ie.addStatusEffect2(new StatusEffectInstance(StatusEffects.INVISIBILITY, 1200));
    }

    @Nullable
    public SoundEvent getSoundPrepare() {
        return SoundEvents.ENTITY_ILLUSIONER_PREPARE_MIRROR;
    }

    @Override
    public spellI2 getSpell() {
        return Spell.DISAPPEAR;
    }
}