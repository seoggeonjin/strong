package net.ua.mixin.entity.abst;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.TurtleEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.time.LocalDate;

@Mixin(AbstractSkeletonEntity.class)
public abstract class AbstractSk extends HostileEntity implements RangedAttackMob {
    @Unique
    private final BowAttackGoal<AbstractSkeletonEntity> bowAttackGoal = new BowAttackGoal<>((AbstractSkeletonEntity) (Object) this, 1, 0, (float)(3.4*1e38-1));
    @Unique
    private final net.ua.mixin2.inner.goals.MeleeAttackGoal meleeAttackGoal = new net.ua.mixin2.inner.goals.MeleeAttackGoal(this, 1.0, false) {
        @Override
        public void stop() {
            super.stop();
            AbstractSk.this.setAttacking(false);
        }

        @Override
        public void start() {
            super.start();
            AbstractSk.this.setAttacking(true);
        }
    };
    public AbstractSk(EntityType<? extends AbstractSkeletonEntity> entityType, World world) {
        super(entityType, world);
        this.updateAttackType();
    }
    @Overwrite
    public void initGoals() {
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 10));
        this.goalSelector.add(6, new LookAtEntityGoal(this, GiantEntity.class, 10));
        this.goalSelector.add(6, new LookAtEntityGoal(this, WitherEntity.class, 10));
        this.goalSelector.add(6, new LookAtEntityGoal(this, BlazeEntity.class, 10));
        this.goalSelector.add(6, new LookAtEntityGoal(this, WitchEntity.class, 10));
        this.goalSelector.add(6, new LookAtEntityGoal(this, CreeperEntity.class, 10));
        this.goalSelector.add(6, new LookAtEntityGoal(this, ZombieEntity.class, 10));
        this.goalSelector.add(6, new LookAtEntityGoal(this, EndermanEntity.class, 10));
        this.goalSelector.add(6, new LookAtEntityGoal(this, VindicatorEntity.class, 10));
        this.goalSelector.add(6, new LookAtEntityGoal(this, IronGolemEntity.class, 10));
        this.goalSelector.add(6, new LookAtEntityGoal(this, TurtleEntity.class, 10));
        this.goalSelector.add(6, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this, AbstractSk.class));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, RavagerEntity.class, 10, true, false, null));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false, null));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, WitherEntity.class, 10, true, false, null));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, GiantEntity.class, 10, true, false, null));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, WolfEntity.class, 10, true, false, null));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, BlazeEntity.class, 10, true, false, null));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, WitchEntity.class, 10, true, false, null));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, CreeperEntity.class, 10, true, false, null));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, EndermanEntity.class, 10, true, false, null));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, ZombieEntity.class, 10, true, false, null));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, VindicatorEntity.class, 10, true, false, null));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, IronGolemEntity.class, 10, true, false, null));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, TurtleEntity.class, 10, true, false, null));
    }

    @Inject(method = "createAbstractSkeletonAttributes", at = @At("RETURN"), cancellable = true)
    private static void createAbstractSkeletonAttributes(CallbackInfoReturnable<DefaultAttributeContainer.Builder> cir) {
        cir.setReturnValue(HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.4).add(EntityAttributes.GENERIC_ATTACK_SPEED,2147483647)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 2147483647).add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 0));
    }

    @Overwrite
    public void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(this.getStepSound(), 0.15f, 1.0f);
    }

    @Overwrite
    public abstract SoundEvent getStepSound();

    @Overwrite
    public EntityGroup getGroup() {
        return EntityGroup.UNDEAD;
    }

    @Overwrite
    public void tickMovement() {
        super.tickMovement();
    }

    @Overwrite
    public void tickRiding() {
        super.tickRiding();
        Entity entity = this.getControllingVehicle();
        if (entity instanceof PathAwareEntity pathAwareEntity) {
            this.bodyYaw = pathAwareEntity.bodyYaw;
        }
    }

    @Overwrite
    public void initEquipment(Random random, LocalDifficulty localDifficulty) {
        super.initEquipment(random, localDifficulty);
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
    }

    @Overwrite
    @Nullable
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        entityData = super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
        Random random = world.getRandom();
        this.initEquipment(random, difficulty);
        this.updateEnchantments(random, difficulty);
        this.updateAttackType();
        this.setCanPickUpLoot(random.nextFloat() < 0.55f * difficulty.getClampedLocalDifficulty());
        if (this.getEquippedStack(EquipmentSlot.HEAD).isEmpty()) {
            LocalDate localDate = LocalDate.now();
            int i = localDate.getDayOfMonth();
            int j = localDate.getMonth().getValue();
            // int j = localDate.get(ChronoField.MONTH_OF_YEAR);
            if (j == 10 && i == 31 && random.nextFloat() < 0.25f) {
                this.equipStack(EquipmentSlot.HEAD, new ItemStack(random.nextFloat() < 0.1f ? Blocks.JACK_O_LANTERN : Blocks.CARVED_PUMPKIN));
                this.armorDropChances[EquipmentSlot.HEAD.getEntitySlotId()] = 0.0f;
            }
        }
        return entityData;
    }

    @Overwrite
    public void updateAttackType() {
        if (this.getWorld() == null || this.getWorld().isClient) {
            return;
        }
        this.goalSelector.remove(this.meleeAttackGoal);
        this.goalSelector.remove(this.bowAttackGoal);
        ItemStack itemStack = this.getStackInHand(ProjectileUtil.getHandPossiblyHolding(this, Items.BOW));
        if (itemStack.isOf(Items.BOW)) {
            this.bowAttackGoal.setAttackInterval(0);
            this.goalSelector.add(4, this.bowAttackGoal);
        } else {
            this.goalSelector.add(4, this.meleeAttackGoal);
        }
    }

    @Overwrite
    public void shootAt(LivingEntity target, float pullProgress) {
        ItemStack itemStack = this.getProjectileType(this.getStackInHand(ProjectileUtil.getHandPossiblyHolding(this, Items.BOW)));
        PersistentProjectileEntity persistentProjectileEntity = this.createArrowProjectile(itemStack, pullProgress);
        double d = target.getX() - this.getX();
        double e = target.getBodyY(0.3333333333333333) - persistentProjectileEntity.getY();
        double f = target.getZ() - this.getZ();
        double g = Math.sqrt(d * d + f * f);
        persistentProjectileEntity.setVelocity(vel(d), e + g * (double)0.2f * g / 800, vel(f), 10.0f, 14 - this.getWorld().getDifficulty().getId() * 4);
        this.playSound(SoundEvents.ENTITY_SKELETON_SHOOT, 1.0f, 1.0f / (this.getRandom().nextFloat() * 0.4f + 0.8f));
        this.getWorld().spawnEntity(persistentProjectileEntity);
    }
    @Unique
    private double vel(double n) {
        return n * 1.5;
    }

    @Overwrite
    public PersistentProjectileEntity createArrowProjectile(ItemStack arrow, float damageModifier) {
        return ProjectileUtil.createArrowProjectile(this, arrow, damageModifier);
    }

    @Overwrite
    public boolean canUseRangedWeapon(RangedWeaponItem weapon) {
        return weapon == Items.BOW;
    }

    @Overwrite
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.updateAttackType();
    }

    @Overwrite
    public void equipStack(EquipmentSlot slot, ItemStack stack) {
        super.equipStack(slot, stack);
        if (!this.getWorld().isClient) {
            this.updateAttackType();
        }
    }

    @Overwrite
    public float getActiveEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return 1.74f;
    }

    @Overwrite
    public float getUnscaledRidingOffset(Entity vehicle) {
        return -0.7f;
    }

    @Overwrite
    public boolean isShaking() {
        return this.isFrozen();
    }
    @Override
    public boolean isAffectedByDaylight() {
        return false;
    }
}
