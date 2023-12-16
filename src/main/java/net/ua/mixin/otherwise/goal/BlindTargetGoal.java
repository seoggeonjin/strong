package net.ua.mixin.otherwise.goal;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.Difficulty;
import net.ua.ips.spellI2;
import net.ua.ips.spellI3;
import net.ua.mixin.entity.abst.CastSpellGoal;
import net.ua.mixin2.inner.Spell;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(targets = "net.minecraft.entity.mob.IllusionerEntity$BlindTargetGoal")
public class BlindTargetGoal extends CastSpellGoal {
    @Shadow
    private int targetId;
    @Unique
    private spellI3 ie;

    public BlindTargetGoal(spellI3 ie) {
        super(ie);
        this.ie = ie;
    }

    @Overwrite
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

    @Overwrite
    public void start() {
        super.start();
        LivingEntity livingEntity = ie.getTarget();
        if (livingEntity != null) {
            this.targetId = livingEntity.getId();
        }
    }

    @Overwrite
    public int getSpellTicks() {
        return 20;
    }

    @Overwrite
    public int startTimeDelay() {
        return 180;
    }

    @Overwrite
    public void castSpell() {
        ie.getTarget().addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 400), (Entity) ie);
    }

    @Overwrite
    public SoundEvent getSoundPrepare() {
        return SoundEvents.ENTITY_ILLUSIONER_PREPARE_BLINDNESS;
    }

    @Override
    public spellI2 getSpell() {
        return Spell.BLINDNESS;
    }
}
