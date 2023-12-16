package net.ua.mixin.otherwise.goal;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.ua.ips.spellI2;
import net.ua.ips.spellI3;
import net.ua.mixin.entity.abst.CastSpellGoal;
import net.ua.mixin2.inner.Spell;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(targets = "net.minecraft.entity.mob.IllusionerEntity$GiveInvisibilityGoal")
public class GiveInvisibilityGoal extends CastSpellGoal {
    @Unique
    private spellI3 ie;
    public GiveInvisibilityGoal(spellI3 ie) {
        super(ie);
        this.ie = ie;
    }

    @Overwrite
    public boolean canStart() {
        if (!super.canStart()) {
            return false;
        }
        return !ie.hasStatusEffect(StatusEffects.INVISIBILITY);
    }

    @Overwrite
    public int getSpellTicks() {
        return 20;
    }

    @Overwrite
    public int startTimeDelay() {
        return 340;
    }

    @Overwrite
    public void castSpell() {
        ie.addStatusEffect2(new StatusEffectInstance(StatusEffects.INVISIBILITY, 1200));
    }

    @Overwrite
    @Nullable
    public SoundEvent getSoundPrepare() {
        return SoundEvents.ENTITY_ILLUSIONER_PREPARE_MIRROR;
    }

    @Override
    public spellI2 getSpell() {
        return Spell.DISAPPEAR;
    }
}