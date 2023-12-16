package net.ua.ips;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.control.LookControl;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.sound.SoundEvent;

public interface spellI extends mixins {
    void spellTicks(int tick);
    void setSpell(spellI2 spell);
    int getSpellTicks();
    EntityNavigation navigation();
    LookControl getLookControl();
    int getMaxHeadRotation();
    int getMaxLookPitchChange();
    boolean isSpellcasting();
    int age();
    void playSound(SoundEvent sound, float volume, float pitch);
    SoundEvent getCastSpellSound();
}
