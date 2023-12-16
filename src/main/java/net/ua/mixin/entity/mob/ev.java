package net.ua.mixin.entity.mob;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.EvokerEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.scoreboard.Team;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.ua.ips.evi;
import net.ua.mixin.entity.abst.si;
import net.ua.mixin2.inner.goals.evoker.ConjureFangsGoal;
import net.ua.mixin2.inner.goals.evoker.LookAtTargetOrWololoGoal;
import net.ua.mixin2.inner.goals.evoker.WololoGoal;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EvokerEntity.class)
public class ev extends si implements evi {
    @Nullable
    @Shadow
    private SheepEntity wololoTarget;

    public ev(EntityType<? extends ev> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 10;
    }

    @Overwrite
    public void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new LookAtTargetOrWololoGoal(this));
        this.goalSelector.add(2, new FleeEntityGoal<>(this, PlayerEntity.class, 8.0f, 0.6, 1.0));
        // this.goalSelector.add(4, new SummonVexGoal(this));
        this.goalSelector.add(3, new ConjureFangsGoal(this));
        this.goalSelector.add(6, new WololoGoal(this));
        this.goalSelector.add(8, new WanderAroundGoal(this, 0.6));
        this.goalSelector.add(9, new LookAtEntityGoal(this, PlayerEntity.class, 3.0f, 1.0f));
        this.goalSelector.add(10, new LookAtEntityGoal(this, MobEntity.class, 8.0f));
        this.targetSelector.add(1, new RevengeGoal(this, RaiderEntity.class).setGroupRevenge());
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true).setMaxTimeWithoutVisibility(300));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, MerchantEntity.class, false).setMaxTimeWithoutVisibility(300));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, IronGolemEntity.class, false));
    }

    @Overwrite
    public static DefaultAttributeContainer.Builder createEvokerAttributes() {
        return HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.5).add(EntityAttributes.GENERIC_FOLLOW_RANGE, 12.0).add(EntityAttributes.GENERIC_MAX_HEALTH, 24.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 2147483647);
    }

    @Overwrite
    public void initDataTracker() {
        super.initDataTracker();
    }

    @Overwrite
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
    }

    @Overwrite
    public SoundEvent getCelebratingSound() {
        return SoundEvents.ENTITY_EVOKER_CELEBRATE;
    }

    @Overwrite
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
    }

    @Overwrite
    public void mobTick() {
        super.mobTick();
    }

    @Overwrite
    public boolean isTeammate(Entity other) {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }
        if (super.isTeammate(other)) {
            return true;
        }
        if (other instanceof VexEntity) {
            return this.isTeammate(((VexEntity)other).getOwner());
        }
        if (other instanceof LivingEntity && ((LivingEntity)other).getGroup() == EntityGroup.ILLAGER) {
            return this.getScoreboardTeam() == null && other.getScoreboardTeam() == null;
        }
        return false;
    }

    @Overwrite
    public SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_EVOKER_AMBIENT;
    }

    @Overwrite
    public SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_EVOKER_DEATH;
    }

    @Overwrite
    public SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_EVOKER_HURT;
    }

    @Overwrite
    public void setWololoTarget(@Nullable SheepEntity wololoTarget) {
        this.wololoTarget = wololoTarget;
    }

    @Nullable
    @Overwrite
    public SheepEntity getWololoTarget() {
        return this.wololoTarget;
    }

    @Overwrite
    public SoundEvent getCastSpellSound() {
        return SoundEvents.ENTITY_EVOKER_CAST_SPELL;
    }

    @Overwrite
    public void addBonusForWave(int wave, boolean unused) {
    }
    @Unique
    public int age() {
        return super.age();
    }
    @Unique
    public World getWorld() {
        return super.getWorld();
    }
    @Unique
    public Random random() {
        return this.random;
    }
    @Unique
    public double squaredDistanceTo(Entity entity) {
        return super.squaredDistanceTo(entity);
    }
    @Unique
    public Team getScoreboardTeam() {
        return super.getScoreboardTeam();
    }
}
