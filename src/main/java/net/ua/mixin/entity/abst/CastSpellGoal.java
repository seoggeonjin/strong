package net.ua.mixin.entity.abst;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.sound.SoundEvent;
import net.ua.ips.spellI;
import net.ua.ips.spellI2;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(targets = "net.minecraft.entity.mob.SpellcastingIllagerEntity$CastSpellGoal")
public abstract class CastSpellGoal extends Goal {
    @Shadow
    protected int spellCooldown;
    @Shadow
    protected int startTime;

    @Unique
    private spellI ie;
    public CastSpellGoal(spellI ie) {
        this.ie = ie;
    }

    @Overwrite
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

    @Overwrite
    public boolean shouldContinue() {
        LivingEntity livingEntity = ie.getTarget();
        return livingEntity != null && livingEntity.isAlive() && this.spellCooldown > 0;
    }

    @Overwrite
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

    @Overwrite
    public void tick() {
        --this.spellCooldown;
        if (this.spellCooldown == 0) {
            this.castSpell();
            ie.playSound(ie.getCastSpellSound(), 1.0f, 1.0f);
        }
    }

    @Overwrite
    public abstract void castSpell();

    @Overwrite
    public int getInitialCooldown() {
        return 20;
    }

    @Overwrite
    public abstract int getSpellTicks();

    @Overwrite
    public abstract int startTimeDelay();

    @Nullable
    @Overwrite
    public abstract SoundEvent getSoundPrepare();
    @Unique
    public abstract spellI2 getSpell();
}
