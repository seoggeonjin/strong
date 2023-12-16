package net.ua.mixin2.inner.goals.illusionor;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.Difficulty;
import net.ua.ips.spellI2;
import net.ua.ips.spellI3;
import net.ua.mixin2.inner.Spell;
import net.ua.mixin2.inner.goals.SpellcastingIllagerEntity.CastSpellGoal2;

public class BlindTargetGoal2 extends CastSpellGoal2 {
    private int targetId;
    private final spellI3 ie;

    public BlindTargetGoal2(spellI3 ie) {
        super(ie);
        this.ie = ie;
    }
    public boolean canStart() {
        if (!super.canStart()) {
            return false;
        }
        if (ie.getTarget() == null) {
            return false;
        }
        if (ie.getTarget().getId() == this.targetId) {
            return false;
        }
        return ie.getWorld().getLocalDifficulty(ie.getBlockPos()).isHarderThan(Difficulty.NORMAL.ordinal());
    }

    public void start() {
        super.start();
        LivingEntity livingEntity = ie.getTarget();
        if (livingEntity != null) {
            this.targetId = livingEntity.getId();
        }
    }

    public int getSpellTicks() {
        return 20;
    }

    public int startTimeDelay() {
        return 180;
    }

    public void castSpell() {
        ie.getTarget().addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 400), (Entity) ie);
    }

    public SoundEvent getSoundPrepare() {
        return SoundEvents.ENTITY_ILLUSIONER_PREPARE_BLINDNESS;
    }

    @Override
    public spellI2 getSpell() {
        return Spell.BLINDNESS;
    }
}
