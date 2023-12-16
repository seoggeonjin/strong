package net.ua.ips;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.control.JumpControl;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

public interface sli extends mixins {
    EntityNavigation getNavigation();
    boolean isTouchingWater();
    boolean isInLava();
    MoveControl getMoveControl();
    Random getRandom();
    JumpControl getJumpControl();
    boolean isOnGround();
    boolean hasStatusEffect(StatusEffect effect);
    boolean hasVehicle();
    boolean canTarget(LivingEntity target);
    void lookAtEntity(Entity targetEntity, float maxYawChange, float maxPitchChange);
    float getYaw();
    boolean canAttack();
    int getTicksUntilNextJump();
    boolean makesJumpSound();
    void playSound(SoundEvent sound, float volume, float pitch);
    SoundEvent getJumpSound();
    float getSoundVolume();
    float getJumpSoundPitch();
    void sidewaysSpeed(float sidewaysSpeed);
    void forwardSpeed(float forwardSpeed);
    void setPersistent();
    void setCustomName(@Nullable Text name);
    void setAiDisabled(boolean aiDisabled);
    void setInvulnerable(boolean invulnerable);
    void setSize(int size, boolean heal);
    void refreshPositionAndAngles(double x, double y, double z, float yaw, float pitch);
}
