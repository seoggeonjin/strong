package net.ua.mixin2.inner.goals.SpellcastingIllagerEntity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.sound.SoundEvent;
import net.ua.ips.spellI;
import net.ua.ips.spellI2;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Unique;

public abstract class CastSpellGoal2 extends Goal {
    protected int spellCooldown;
    protected int startTime;

    @Unique
    private spellI ie;
    public CastSpellGoal2(spellI ie) {
        this.ie = ie;
    }

    public boolean canStart() {
        LivingEntity livingEntity = ie.getTarget();
        if (livingEntity == null || !livingEntity.isAlive()) {
            return false;
        }
        if (ie.isSpellcasting()) {
            return false;
        }
        return ie.age() >= this.startTime;
    }

    public boolean shouldContinue() {
        LivingEntity livingEntity = ie.getTarget();
        return livingEntity != null && livingEntity.isAlive();    // && this.spellCooldown > 0
    }

    public void start() {
        this.spellCooldown = this.getTickCount(this.getInitialCooldown());
        ie.spellTicks(this.getSpellTicks());
        this.startTime = ie.age() + this.startTimeDelay();
        SoundEvent soundEvent = this.getSoundPrepare();
        if (soundEvent != null) {
            ie.playSound(soundEvent, 1.0f, 1.0f);
        }
        ie.setSpell(this.getSpell());
    }

    public void tick() {
        --this.spellCooldown;
        this.castSpell();
        ie.playSound(ie.getCastSpellSound(), 1.0f, 1.0f);
        if (this.spellCooldown == 0) {
        }
    }

    public abstract void castSpell();

    public int getInitialCooldown() {
        return 20;
    }

    public abstract int getSpellTicks();

    public abstract int startTimeDelay();

    @Nullable
    public abstract SoundEvent getSoundPrepare();
    @Unique
    public abstract spellI2 getSpell();
}
