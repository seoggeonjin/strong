package net.ua.mixin.entity.mob;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.ProjectileAttackGoal;
import net.minecraft.entity.ai.goal.RaidGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
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
import net.ua.mixin2.inner.goals.enderman.DisableableFollowTargetGoal;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.*;

@Mixin(WitchEntity.class)
public class wit extends RaiderEntity implements RangedAttackMob {
    @Final
    @Shadow
    private static final UUID DRINKING_SPEED_PENALTY_MODIFIER_ID = UUID.fromString("5CD17E52-A79A-43D3-A529-90FDE04B181E");
    @Final
    @Shadow
    private static final EntityAttributeModifier DRINKING_SPEED_PENALTY_MODIFIER = new EntityAttributeModifier(DRINKING_SPEED_PENALTY_MODIFIER_ID, "Drinking speed penalty", -0.25, EntityAttributeModifier.Operation.ADDITION);
    @Final
    @Shadow
    private static final TrackedData<Boolean> DRINKING = DataTracker.registerData(wit.class, TrackedDataHandlerRegistry.BOOLEAN);
    @Shadow
    private int drinkTimeLeft;
    @Shadow
    private RaidGoal<RaiderEntity> raidGoal;
    @Unique
    private DisableableFollowTargetGoal<PlayerEntity> attackPlayerGoal;
    @Unique
    private DisableableFollowTargetGoal<SkeletonEntity> attackSkeletonGoal1;
    @Unique
    private DisableableFollowTargetGoal<StrayEntity> attackSkeletonGoal2;
    @Unique
    private DisableableFollowTargetGoal<WitherSkeletonEntity> attackSkeletonGoal3;

    public wit(EntityType<? extends wit> entityType, World world) {
        super(entityType, world);
    }

    @Overwrite
    public void initGoals() {
        super.initGoals();
        this.raidGoal = new RaidGoal<>(this, RaiderEntity.class, true, entity -> entity != null && this.hasActiveRaid() && entity.getType() != EntityType.WITCH);
        this.attackPlayerGoal = new DisableableFollowTargetGoal<>(this, PlayerEntity.class, 10, true, false, null);
        this.attackSkeletonGoal1 = new DisableableFollowTargetGoal<>(this, SkeletonEntity.class, 10, true, false, null);
        this.attackSkeletonGoal2 = new DisableableFollowTargetGoal<>(this, StrayEntity.class, 10, true, false, null);
        this.attackSkeletonGoal3 = new DisableableFollowTargetGoal<>(this, WitherSkeletonEntity.class, 10, true, false, null);
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new ProjectileAttackGoal(this, 1.0, 0, 50.0f));
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(3, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this, RaiderEntity.class));
        this.targetSelector.add(2, this.raidGoal);
        this.targetSelector.add(3, this.attackPlayerGoal);
        this.targetSelector.add(3, this.attackSkeletonGoal1);
        this.targetSelector.add(3, this.attackSkeletonGoal2);
        this.targetSelector.add(3, this.attackSkeletonGoal3);
    }

    @Overwrite
    public void initDataTracker() {
        super.initDataTracker();
        this.getDataTracker().startTracking(DRINKING, false);
    }

    @Overwrite
    public SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_WITCH_AMBIENT;
    }

    @Overwrite
    public SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_WITCH_HURT;
    }

    @Overwrite
    public SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_WITCH_DEATH;
    }
    @Overwrite
    public void setDrinking(boolean drinking) {
        this.getDataTracker().set(DRINKING, drinking);
    }
    @Overwrite
    public boolean isDrinking() {
        return this.getDataTracker().get(DRINKING);
    }

    @Overwrite
    public static DefaultAttributeContainer.Builder createWitchAttributes() {
        return HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 26.0).add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25);
    }

    @Overwrite
    public void tickMovement() {
        if (!this.getWorld().isClient && this.isAlive()) {
            this.raidGoal.decreaseCooldown();
            this.attackPlayerGoal.setEnabled(true);
            this.attackSkeletonGoal1.setEnabled(true);
            this.attackSkeletonGoal2.setEnabled(true);
            this.attackSkeletonGoal3.setEnabled(true);
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

    @Overwrite
    public SoundEvent getCelebratingSound() {
        return SoundEvents.ENTITY_WITCH_CELEBRATE;
    }

    @Overwrite
    public void handleStatus(byte status) {
        if (status == EntityStatuses.ADD_WITCH_PARTICLES) {
            for (int i = 0; i < this.random.nextInt(35) + 10; ++i) {
                this.getWorld().addParticle(ParticleTypes.WITCH, this.getX() + this.random.nextGaussian() * (double)0.13f, this.getBoundingBox().maxY + 0.5 + this.random.nextGaussian() * (double)0.13f, this.getZ() + this.random.nextGaussian() * (double)0.13f, 0.0, 0.0, 0.0);
            }
        } else {
            super.handleStatus(status);
        }
    }

    @Overwrite
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

    @Overwrite
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
        } else if (g >= 8.0 && !target.hasStatusEffect(StatusEffects.SLOWNESS)) {
            potion = Potions.SLOWNESS;
        } else if (target.getHealth() >= 8.0f && !target.hasStatusEffect(StatusEffects.POISON)) {
            potion = Potions.POISON;
        } else if (g <= 3.0 && !target.hasStatusEffect(StatusEffects.WEAKNESS) && this.random.nextFloat() < 0.25f) {
            potion = Potions.WEAKNESS;
        }
        PotionEntity potionEntity = new PotionEntity(this.getWorld(), this);
        potionEntity.setItem(PotionUtil.setPotion(new ItemStack(Items.SPLASH_POTION), potion));
        potionEntity.setPitch(potionEntity.getPitch() - -20.0f);
        potionEntity.setVelocity(d * 1.5, e + g * 0.2 * g / 10, f * 1.5, 1.5f, 8.0f);
        if (!this.isSilent()) {
            this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_WITCH_THROW, this.getSoundCategory(), 1.0f, 0.8f + this.random.nextFloat() * 0.4f);
        }
        this.getWorld().spawnEntity(potionEntity);
    }

    @Overwrite
    public float getActiveEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return 1.62f;
    }

    @Overwrite
    public Vector3f getPassengerAttachmentPos(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
        return new Vector3f(0.0f, dimensions.height + 0.3125f * scaleFactor, 0.0f);
    }

    @Overwrite
    public void addBonusForWave(int wave, boolean unused) {
    }

    @Overwrite
    public boolean canLead() {
        return false;
    }
}
