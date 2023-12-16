package net.ua.mixin.entity.mob;

import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Mixin(EndermanEntity.class)
public class En extends HostileEntity implements RangedAttackMob {
    @Shadow
    @Final
    private static final TrackedData<Optional<BlockState>> CARRIED_BLOCK = DataTracker.registerData(En.class, TrackedDataHandlerRegistry.OPTIONAL_BLOCK_STATE);
    @Shadow
    @Final
    private static final TrackedData<Boolean> ANGRY = DataTracker.registerData(En.class, TrackedDataHandlerRegistry.BOOLEAN);
    @Shadow
    @Final
    private static final TrackedData<Boolean> PROVOKED = DataTracker.registerData(En.class, TrackedDataHandlerRegistry.BOOLEAN);
    @Unique
    private static final UUID DRINKING_SPEED_PENALTY_MODIFIER_ID = UUID.fromString("5CD17E52-A79A-43D3-A529-90FDE04B181E");
    @Unique
    private static final EntityAttributeModifier DRINKING_SPEED_PENALTY_MODIFIER = new EntityAttributeModifier(DRINKING_SPEED_PENALTY_MODIFIER_ID, "Drinking speed penalty", -0.25, EntityAttributeModifier.Operation.ADDITION);
    @Unique
    private static final TrackedData<Boolean> DRINKING = DataTracker.registerData(En.class, TrackedDataHandlerRegistry.BOOLEAN);
    @Unique
    private int drinkTimeLeft;
    @Unique
    private net.ua.mixin2.inner.goals.enderman.DisableableFollowTargetGoal<PlayerEntity> attackPlayerGoal;
    @Unique
    private net.ua.mixin2.inner.goals.enderman.DisableableFollowTargetGoal<SkeletonEntity> attackSkeletonGoal1;
    @Unique
    private net.ua.mixin2.inner.goals.enderman.DisableableFollowTargetGoal<StrayEntity> attackSkeletonGoal2;
    @Unique
    private net.ua.mixin2.inner.goals.enderman.DisableableFollowTargetGoal<WitherSkeletonEntity> attackSkeletonGoal3;

    public En(EntityType<? extends En> entityType, World world) {
        super(entityType, world);
    }

    @Overwrite
    public void initGoals() {
        super.initGoals();
        this.attackPlayerGoal = new net.ua.mixin2.inner.goals.enderman.DisableableFollowTargetGoal<>(this, PlayerEntity.class, 10, true, false, null);
        this.attackSkeletonGoal1 = new net.ua.mixin2.inner.goals.enderman.DisableableFollowTargetGoal<>(this, SkeletonEntity.class, 10, true, false, null);
        this.attackSkeletonGoal2 = new net.ua.mixin2.inner.goals.enderman.DisableableFollowTargetGoal<>(this, StrayEntity.class, 10, true, false, null);
        this.attackSkeletonGoal3 = new net.ua.mixin2.inner.goals.enderman.DisableableFollowTargetGoal<>(this, WitherSkeletonEntity.class, 10, true, false, null);
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new ProjectileAttackGoal(this, 1.0, 0, 50.0f));
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(3, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this, EndermanEntity.class));
        this.targetSelector.add(3, this.attackPlayerGoal);
        this.targetSelector.add(3, this.attackSkeletonGoal1);
        this.targetSelector.add(3, this.attackSkeletonGoal2);
        this.targetSelector.add(3, this.attackSkeletonGoal3);
        this.goalSelector.getGoals().forEach(g -> {

        });
    }

    @Overwrite
    public void initDataTracker() {
        super.initDataTracker();
        this.getDataTracker().startTracking(DRINKING, false);
        this.dataTracker.startTracking(CARRIED_BLOCK, Optional.empty());
        this.dataTracker.startTracking(ANGRY, false);
        this.dataTracker.startTracking(PROVOKED, false);
    }

    @Overwrite
    public SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_ENDERMAN_AMBIENT;
    }

    @Overwrite
    public SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_ENDER_DRAGON_HURT;
    }

    @Unique
    public SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_ENDER_DRAGON_DEATH;
    }
    @Unique
    public void setDrinking(boolean drinking) {
        this.getDataTracker().set(DRINKING, drinking);
    }
    @Unique
    public boolean isDrinking() {
        return this.getDataTracker().get(DRINKING);
    }

    @Overwrite
    public static DefaultAttributeContainer.Builder createEndermanAttributes() {
        return HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 26.0).add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 2147483647);
    }

    @Overwrite
    public void tickMovement() {
        if (!this.getWorld().isClient && this.isAlive()) {
            if (this.isDrinking()) {
                if (this.drinkTimeLeft-- <= 0) {
                    List<StatusEffectInstance> list;
                    this.setDrinking(false);
                    ItemStack itemStack = this.getMainHandStack();
                    this.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                    if (itemStack.isOf(Items.POTION) && (list = PotionUtil.getPotionEffects(itemStack)) != null) {
                        for (StatusEffectInstance statusEffectInstance : list) {
                            this.addStatusEffect(new StatusEffectInstance(statusEffectInstance));
                        }
                    }
                    this.emitGameEvent(GameEvent.DRINK);
                    Objects.requireNonNull(this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)).removeModifier(DRINKING_SPEED_PENALTY_MODIFIER.getId());
                }
            } else {
                Potion potion = null;
                if (this.random.nextFloat() < 0.15f && this.isSubmergedIn(FluidTags.WATER) && !this.hasStatusEffect(StatusEffects.WATER_BREATHING)) {
                    potion = Potions.WATER_BREATHING;
                } else if (this.random.nextFloat() < 0.15f && (this.isOnFire() || this.getRecentDamageSource() != null && this.getRecentDamageSource().isIn(DamageTypeTags.IS_FIRE)) && !this.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
                    potion = Potions.FIRE_RESISTANCE;
                } else if (this.random.nextFloat() < 0.05f && this.getHealth() < this.getMaxHealth()) {
                    potion = Potions.HEALING;
                } else if (this.random.nextFloat() < 0.5f && this.getTarget() != null && !this.hasStatusEffect(StatusEffects.SPEED) && this.getTarget().squaredDistanceTo(this) > 121.0) {
                    potion = Potions.SWIFTNESS;
                }
                if (potion != null) {
                    this.equipStack(EquipmentSlot.MAINHAND, PotionUtil.setPotion(new ItemStack(Items.POTION), potion));
                    this.drinkTimeLeft = this.getMainHandStack().getMaxUseTime();
                    this.setDrinking(true);
                    if (!this.isSilent()) {
                        this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_WITCH_DRINK, this.getSoundCategory(), 1.0f, 0.8f + this.random.nextFloat() * 0.4f);
                    }
                    EntityAttributeInstance entityAttributeInstance = this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
                    assert entityAttributeInstance != null;
                    entityAttributeInstance.removeModifier(DRINKING_SPEED_PENALTY_MODIFIER.getId());
                    entityAttributeInstance.addTemporaryModifier(DRINKING_SPEED_PENALTY_MODIFIER);
                }
            }
            if (this.random.nextFloat() < 7.5E-4f) {
                this.getWorld().sendEntityStatus(this, EntityStatuses.ADD_WITCH_PARTICLES);
            }
        }
        super.tickMovement();
    }

    @Unique
    public void handleStatus(byte status) {
        if (status == EntityStatuses.ADD_WITCH_PARTICLES) {
            for (int i = 0; i < this.random.nextInt(35) + 10; ++i) {
                this.getWorld().addParticle(ParticleTypes.WITCH, this.getX() + this.random.nextGaussian() * (double)0.13f, this.getBoundingBox().maxY + 0.5 + this.random.nextGaussian() * (double)0.13f, this.getZ() + this.random.nextGaussian() * (double)0.13f, 0.0, 0.0, 0.0);
            }
        } else {
            super.handleStatus(status);
        }
    }

    @Unique
    public float modifyAppliedDamage(DamageSource source, float amount) {
        amount = super.modifyAppliedDamage(source, amount);
        if (source.getAttacker() == this) {
            amount = 0.0f;
        }
        if (source.isIn(DamageTypeTags.WITCH_RESISTANT_TO)) {
            amount *= 0.15f;
        }
        return amount;
    }

    @Unique
    public void shootAt(LivingEntity target, float pullProgress) {
        if (this.isDrinking()) {
            return;
        }
        Vec3d vec3d = target.getVelocity();
        double d = target.getX() + vec3d.x - this.getX();
        double e = target.getEyeY() - (double)1.1f - this.getY();
        double f = target.getZ() + vec3d.z - this.getZ();
        double g = Math.sqrt(d * d + f * f);
        Potion potion = Potions.HARMING;
        if (target instanceof RaiderEntity) {
            potion = target.getHealth() <= 4.0f ? Potions.HEALING : Potions.REGENERATION;
            this.setTarget(null);
        } else if (g <= 256) {
            potion = Potions.STRONG_HARMING;
        }
        PotionEntity potionEntity = new PotionEntity(this.getWorld(), this);
        potionEntity.setItem(PotionUtil.setPotion(new ItemStack(Items.SPLASH_POTION), potion));
        potionEntity.setPitch(potionEntity.getPitch() - -20.0f);
        potionEntity.setPosition(potionEntity.getPos().subtract(0, 0.9, 0));
        potionEntity.setVelocity(d * 1.5, e + g * 0.2 * g / 80, f * 1.5, 2.5f, 8.0f);
        if (!this.isSilent()) {
            this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_WITCH_THROW, this.getSoundCategory(), 1.0f, 0.8f + this.random.nextFloat() * 0.4f);
        }
        this.getWorld().spawnEntity(potionEntity);
    }

    @Unique
    public float getActiveEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return 1.62f;
    }

    @Unique
    public Vector3f getPassengerAttachmentPos(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
        return new Vector3f(0.0f, dimensions.height + 0.3125f * scaleFactor, 0.0f);
    }
    @Overwrite
    @Override
    public boolean damage(DamageSource source, float amount) {
        return super.damage(source, amount);
    }
}

