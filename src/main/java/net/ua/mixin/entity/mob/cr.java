package net.ua.mixin.entity.mob;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.entity.feature.SkinOverlayOwner;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.control.LookControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.ua.mixin2.inner.goals.creeper.ChasePlayerGoal;
import net.ua.mixin2.inner.goals.creeper.PickUpBlockGoal;
import net.ua.mixin2.inner.goals.creeper.PlaceBlockGoal;
import net.ua.mixin2.inner.goals.creeper.TeleportTowardsPlayerGoal;
import net.ua.ips.Ee;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Mixin(CreeperEntity.class)
public class cr extends HostileEntity implements Angerable, Ee, SkinOverlayOwner {
    /**
     * @see net.minecraft.entity.mob.EndermanEntity
     */
    @Unique
    @Nullable
    private UUID angryAt;
    @Unique
    private static final UUID ATTACKING_SPEED_BOOST_ID = UUID.fromString("020E0DFB-87AE-4653-9556-831010E291A0");
    @Unique
    private static final EntityAttributeModifier ATTACKING_SPEED_BOOST = new EntityAttributeModifier(ATTACKING_SPEED_BOOST_ID, "Attacking speed boost", 0.15f, EntityAttributeModifier.Operation.ADDITION);
    @Unique
    private static final TrackedData<Optional<BlockState>> CARRIED_BLOCK = DataTracker.registerData(cr.class, TrackedDataHandlerRegistry.OPTIONAL_BLOCK_STATE);
    @Unique
    private static final TrackedData<Boolean> ANGRY = DataTracker.registerData(cr.class, TrackedDataHandlerRegistry.BOOLEAN);
    @Unique
    private static final TrackedData<Boolean> PROVOKED = DataTracker.registerData(cr.class, TrackedDataHandlerRegistry.BOOLEAN);
    @Unique
    private int lastAngrySoundAge = Integer.MIN_VALUE;
    @Unique
    private int ageWhenTargetSet;
    @Unique
    private static final UniformIntProvider ANGER_TIME_RANGE = TimeHelper.betweenSeconds(20, 39);
    @Unique
    private int angerTime;


    @Inject(method = "createCreeperAttributes", at = @At("RETURN"), cancellable = true)
    private static void onCreateCreeperAttributes(CallbackInfoReturnable<DefaultAttributeContainer.Builder> info) {
        info.setReturnValue(createHostileAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0).add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.4f).add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0).add(EntityAttributes.GENERIC_FOLLOW_RANGE, 2147483647));
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tick(CallbackInfo info) {
        if (!this.getWorld().isClient && this.getWorld().getDifficulty() != Difficulty.PEACEFUL) {
            if (this.getTarget() == null) {
                this.setTarget(this.getWorld().getClosestPlayer(this, 64.0));
            }
        }
        if (this.getTarget() instanceof PlayerEntity) {
            if (((PlayerEntity) this.getTarget()).isCreative()) {
                this.setTarget(null);
            }
        }
        if (this.getTarget() instanceof PlayerEntity) {
            Objects.requireNonNull(this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)).setBaseValue(0.1f);
            Objects.requireNonNull(this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE)).setBaseValue(16.0D);
            Objects.requireNonNull(this.getAttributeInstance(EntityAttributes.GENERIC_FOLLOW_RANGE)).setBaseValue(64.0D);
        }
    }

    public cr(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.setStepHeight(1.0f);
        this.setPathfindingPenalty(PathNodeType.WATER, -1.0f);
    }

    public void setCarriedBlock(@Nullable BlockState state) {
        this.dataTracker.set(CARRIED_BLOCK, Optional.ofNullable(state));
    }
    @Override
    public boolean isUniversallyAngry(World world) {
        return world.getGameRules().getBoolean(GameRules.UNIVERSAL_ANGER) && this.hasAngerTime() && this.getAngryAt() == null;
    }
    @Override
    public boolean hasAngerTime() {
        return this.getAngerTime() > 0;
    }
    public double getAttackDistanceScalingFactor(@Nullable Entity entity) {
        double d = 1.0;
        if (this.isSneaky()) {
            d *= 0.8;
        }
        if (this.isInvisible()) {
            float f = this.getArmorVisibility();
            if (f < 0.1f) {
                f = 0.1f;
            }
            d *= 0.7 * (double)f;
        }
        if (entity != null) {
            ItemStack itemStack = this.getEquippedStack(EquipmentSlot.HEAD);
            EntityType<?> entityType = entity.getType();
            if (entityType == EntityType.SKELETON && itemStack.isOf(Items.SKELETON_SKULL) || entityType == EntityType.ZOMBIE && itemStack.isOf(Items.ZOMBIE_HEAD) || entityType == EntityType.PIGLIN && itemStack.isOf(Items.PIGLIN_HEAD) || entityType == EntityType.PIGLIN_BRUTE && itemStack.isOf(Items.PIGLIN_HEAD) || entityType == EntityType.CREEPER && itemStack.isOf(Items.CREEPER_HEAD)) {
                d *= 0.5;
            }
        }
        return d;
    }
    @Nullable
    public BlockState getCarriedBlock() {
        return this.dataTracker.get(CARRIED_BLOCK).orElse(null);
    }
    @Overwrite
    public void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(CARRIED_BLOCK, Optional.empty());
        this.dataTracker.startTracking(ANGRY, false);
        this.dataTracker.startTracking(PROVOKED, false);
    }
    public boolean shouldAngerAt(LivingEntity entity) {
        if (!this.canTarget(entity)) {
            return false;
        }
        if (entity.getType() == EntityType.PLAYER && this.isUniversallyAngry(entity.getWorld())) {
            return true;
        }
        return entity.getUuid().equals(this.getAngryAt());
    }
    @Overwrite
    public void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new ChasePlayerGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 1.0, 0.0f));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 1.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.goalSelector.add(10, new PlaceBlockGoal(this));
        this.goalSelector.add(11, new PickUpBlockGoal(this));
        this.targetSelector.add(1, new TeleportTowardsPlayerGoal(this, this::shouldAngerAt));
        this.targetSelector.add(2, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, EndermiteEntity.class, true, false));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, SkeletonEntity.class, true));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, WitherSkeletonEntity.class, true));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, StrayEntity.class, true));
        this.targetSelector.add(4, new UniversalAngerGoal<>(this, false));
    }
    public boolean teleportTo(Entity entity) {
        Vec3d vec3d = new Vec3d(this.getX2() - entity.getX(), this.getBodyY(0.5) - entity.getEyeY(), this.getZ() - entity.getZ());
        vec3d = vec3d.normalize();
        double e = this.getX2() + (this.random.nextDouble() - 0.5) * 8.0 - vec3d.x * 16.0;
        double f = this.getY() + (double)(this.random.nextInt(16) - 8) - vec3d.y * 16.0;
        double g = this.getZ() + (this.random.nextDouble() - 0.5) * 8.0 - vec3d.z * 16.0;
        return this.teleportTo(e, f, g);
    }
    public void setProvoked() {
        this.dataTracker.set(PROVOKED, true);
    }

    @Unique
    private boolean teleportTo(double x, double y, double z) {
        BlockPos.Mutable mutable = new BlockPos.Mutable(x, y, z);
        while (mutable.getY() > this.getWorld().getBottomY() && !this.getWorld().getBlockState(mutable).blocksMovement()) {
            mutable.move(Direction.DOWN);
        }
        BlockState blockState = this.getWorld().getBlockState(mutable);
        boolean bl = blockState.blocksMovement();
        boolean bl2 = blockState.getFluidState().isIn(FluidTags.WATER);
        if (!bl || bl2) {
            return false;
        }
        Vec3d vec3d = this.getPos();
        boolean bl3 = this.teleport(x, y, z, true);
        if (bl3) {
            this.getWorld().emitGameEvent(GameEvent.TELEPORT, vec3d, GameEvent.Emitter.of(this));
            if (!this.isSilent()) {
                this.getWorld().playSound(null, this.prevX, this.prevY, this.prevZ, SoundEvents.ENTITY_ENDERMAN_TELEPORT, this.getSoundCategory(), 1.0f, 1.0f);
                this.playSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            }
        }
        return bl3;
    }

    @Override
    public boolean isPlayerStaring(PlayerEntity player) {
        ItemStack itemStack = player.getInventory().armor.get(3);
        if (itemStack.isOf(Blocks.CARVED_PUMPKIN.asItem())) {
            return false;
        }
        Vec3d vec3d = player.getRotationVec(1.0f).normalize();
        Vec3d vec3d2 = new Vec3d(this.getX2() - player.getX(), this.getEyeY() - player.getEyeY(), this.getZ() - player.getZ());
        double d = vec3d2.length();
        double e = vec3d.dotProduct(vec3d2.normalize());
        if (e > 1.0 - 0.025 / d) {
            return player.canSee(this);
        }
        return false;
    }
    //
    @Overwrite
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target);
        EntityAttributeInstance entityAttributeInstance = this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (target == null) {
            this.ageWhenTargetSet = 0;
            this.dataTracker.set(ANGRY, false);
            this.dataTracker.set(PROVOKED, false);
            assert entityAttributeInstance != null;
            entityAttributeInstance.removeModifier(ATTACKING_SPEED_BOOST.getId());
        } else {
            if (target instanceof PlayerEntity) {
                if (((PlayerEntity) target).isCreative()){
                    this.ageWhenTargetSet = this.age;
                    this.dataTracker.set(ANGRY, true);
                    assert entityAttributeInstance != null;
                    if (!entityAttributeInstance.hasModifier(ATTACKING_SPEED_BOOST)) {
                        entityAttributeInstance.addTemporaryModifier(ATTACKING_SPEED_BOOST);
                    }
                } else {
                    this.ageWhenTargetSet = this.age;
                    this.dataTracker.set(ANGRY, true);
                    assert entityAttributeInstance != null;
                    if (!entityAttributeInstance.hasModifier(ATTACKING_SPEED_BOOST)) {
                        entityAttributeInstance.addTemporaryModifier(ATTACKING_SPEED_BOOST);
                    }
                }
            } else {
                this.ageWhenTargetSet = this.age;
                this.dataTracker.set(ANGRY, true);
                assert entityAttributeInstance != null;
                if (!entityAttributeInstance.hasModifier(ATTACKING_SPEED_BOOST)) {
                    entityAttributeInstance.addTemporaryModifier(ATTACKING_SPEED_BOOST);
                }
            }
        }
    }

    @Override
    public void chooseRandomAngerTime() {
        this.setAngerTime(ANGER_TIME_RANGE.get(this.random));
    }

    @Override
    public void setAngerTime(int angerTime) {
        this.angerTime = angerTime;
    }

    @Override
    public int getAngerTime() {
        return this.angerTime;
    }

    @Override
    public void setAngryAt(@Nullable UUID angryAt) {
        this.angryAt = angryAt;
    }

    @Override
    @Nullable
    public UUID getAngryAt() {
        return this.angryAt;
    }

    @Unique
    public void playAngrySound() {
        if (this.age >= this.lastAngrySoundAge + 400) {
            this.lastAngrySoundAge = this.age;
            if (!this.isSilent()) {
                this.getWorld().playSound(this.getX2(), this.getEyeY(), this.getZ(), SoundEvents.ENTITY_ENDERMAN_STARE, this.getSoundCategory(), 2.5f, 1.0f, false);
            }
        }
    }

    @Override
    public void onTrackedDataSet(TrackedData<?> data) {
        if (ANGRY.equals(data) && this.isProvoked() && this.getWorld().isClient) {
            this.playAngrySound();
        }
        super.onTrackedDataSet(data);
    }

    @Overwrite
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        BlockState blockState = this.getCarriedBlock();
        if (blockState != null) {
            nbt.put("carriedBlockState", NbtHelper.fromBlockState(blockState));
        }
        this.writeAngerToNbt(nbt);
    }

    @Overwrite
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        BlockState blockState = null;
        if (nbt.contains("carriedBlockState", NbtElement.COMPOUND_TYPE) && (blockState = NbtHelper.toBlockState(this.getWorld().createCommandRegistryWrapper(RegistryKeys.BLOCK), nbt.getCompound("carriedBlockState"))).isAir()) {
            blockState = null;
        }
        this.setCarriedBlock(blockState);
        this.readAngerFromNbt(this.getWorld(), nbt);
    }

    @Override
    protected float getActiveEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return 1.45f;
    }

    @Override
    public void tickMovement() {
        if (this.getWorld().isClient) {
            for (int i = 0; i < 2; ++i) {
                this.getWorld().addParticle(ParticleTypes.PORTAL, this.getParticleX(0.5), this.getRandomBodyY() - 0.25, this.getParticleZ(0.5), (this.random.nextDouble() - 0.5) * 2.0, -this.random.nextDouble(), (this.random.nextDouble() - 0.5) * 2.0);
            }
        }
        this.jumping = false;
        if (!this.getWorld().isClient) {
            this.tickAngerLogic((ServerWorld)this.getWorld(), true);
        }
        super.tickMovement();
    }

    @Override
    public boolean hurtByWater() {
        return true;
    }

    @Override
    protected void mobTick() {
        float f;
        if (this.getWorld().isDay() && this.age >= this.ageWhenTargetSet + 600 && (f = this.getBrightnessAtEyes()) > 0.5f && this.getWorld().isSkyVisible(this.getBlockPos()) && this.random.nextFloat() * 30.0f < (f - 0.4f) * 2.0f) {
            this.setTarget(null);
            this.teleportRandomly();
        }
        super.mobTick();
    }

    @Override
    public boolean teleportRandomly() {
        if (this.getWorld().isClient() || !this.isAlive()) {
            return false;
        }
        double d = this.getX2() + (this.random.nextDouble() - 0.5) * 64.0;
        double e = this.getY() + (double)(this.random.nextInt(64) - 32);
        double f = this.getZ() + (this.random.nextDouble() - 0.5) * 64.0;
        return this.teleportTo(d, e, f);
    }

    @Override
    public SoundEvent getAmbientSound() {
        return this.isAngry() ? SoundEvents.ENTITY_ENDERMAN_SCREAM : null;
    }

    @Overwrite
    public SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_CREEPER_HURT;
    }

    @Overwrite
    public SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_CREEPER_DEATH;
    }

    @Overwrite
    public void dropEquipment(DamageSource source, int lootingMultiplier, boolean allowDrops) {
        CreeperEntity creeperEntity;
        super.dropEquipment(source, lootingMultiplier, allowDrops);
        Entity entity = source.getAttacker();
        if (entity instanceof CreeperEntity && (creeperEntity = (CreeperEntity) entity).shouldDropHead()) {
            creeperEntity.onHeadDropped();
            this.dropItem(Items.CREEPER_HEAD);
        }
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        }
        boolean bl = source.getSource() instanceof PotionEntity;
        // if (source.isIn(DamageTypeTags.IS_PROJECTILE) || bl) {
        //     boolean bl2 = bl && this.damageFromPotion(source, (PotionEntity)source.getSource(), amount);
        //     for (int i = 0; i < 64; ++i) {
        //         if (!this.teleportRandomly()) continue;
        //         return true;
        //     }
        //     return bl2;
        // }
        boolean bl2 = super.damage(source, amount);
        if (!this.getWorld().isClient() && !(source.getAttacker() instanceof LivingEntity) && this.random.nextInt(10) != 0) {
            this.teleportRandomly();
        }
        return bl2;
    }

    @Unique
    private boolean damageFromPotion(DamageSource source, PotionEntity potion, float amount) {
        boolean bl;
        ItemStack itemStack = potion.getStack();
        Potion potion2 = PotionUtil.getPotion(itemStack);
        List<StatusEffectInstance> list = PotionUtil.getPotionEffects(itemStack);
        bl = potion2 == Potions.WATER && list.isEmpty();
        if (bl) {
            return super.damage(source, amount);
        }
        return false;
    }
    @Overwrite
    public ActionResult interactMob(PlayerEntity player2, Hand hand) {
        return super.interactMob(player2, hand);
    }

    @Unique
    public boolean isAngry() {
        return this.dataTracker.get(ANGRY);
    }

    @Unique
    public boolean isProvoked() {
        return this.dataTracker.get(PROVOKED);
    }

    @Override
    public boolean cannotDespawn() {
        return super.cannotDespawn() || this.getCarriedBlock() != null;
    }
    @Overwrite
    public boolean shouldRenderOverlay() {
        return true;
    }
    /**
     * @author Seoggeonjin
     * @reason test
     */
    @Overwrite
    public void onStruckByLightning(ServerWorld world, LightningEntity lightning) {
        super.onStruckByLightning(world, lightning);
    }
    /**
     * @author Seoggeonjin
     * @reason test
     */
    @Overwrite
    public boolean isIgnited() {
        return false;
    }

    /**
     * @author Seoggeonjin
     * @reason test
     */
    @Overwrite
    public void ignite() {
    }
    @Overwrite
    public boolean tryAttack(Entity target) {
        boolean bl;
        int i;
        float f = (float)this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        float g = (float)this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_KNOCKBACK);
        if (target instanceof LivingEntity) {
            f += EnchantmentHelper.getAttackDamage(this.getMainHandStack(), ((LivingEntity)target).getGroup());
            g += (float)EnchantmentHelper.getKnockback(this);
        }
        if ((i = EnchantmentHelper.getFireAspect(this)) > 0) {
            target.setOnFireFor(i * 4);
        }
        bl = target.damage(this.getDamageSources().mobAttack(this), f);
        if (bl) {
            if (g > 0.0f && target instanceof LivingEntity) {
                ((LivingEntity)target).takeKnockback(g * 0.5f, MathHelper.sin(this.getYaw() * ((float)Math.PI / 180)), -MathHelper.cos(this.getYaw() * ((float)Math.PI / 180)));
                this.setVelocity(this.getVelocity().multiply(0.6, 1.0, 0.6));
            }
            if (target instanceof PlayerEntity playerEntity) {
                this.disablePlayerShield(playerEntity, this.getMainHandStack(), playerEntity.isUsingItem() ? playerEntity.getActiveItem() : ItemStack.EMPTY);
            }
            this.applyDamageEffects(this, target);
            this.onAttacking(target);
        }
        return bl;
    }
    @Unique
    private void disablePlayerShield(PlayerEntity player, ItemStack mobStack, ItemStack playerStack) {
        if (!mobStack.isEmpty() && !playerStack.isEmpty() && mobStack.getItem() instanceof AxeItem && playerStack.isOf(Items.SHIELD)) {
            float f = 0.25f + (float)EnchantmentHelper.getEfficiency(this) * 0.05f;
            if (this.random.nextFloat() < f) {
                player.getItemCooldownManager().set(Items.SHIELD, 100);
                this.getWorld().sendEntityStatus(player, EntityStatuses.BREAK_SHIELD);
            }
        }
    }
    @Override
    public World getWorld() {
        return super.getWorld();
    }
    @Override
    public boolean canTarget(LivingEntity target) {
        return super.canTarget(target);
    }
    @Overwrite
    public int getFuseSpeed() {
        return 0;
    }

    @Overwrite
    public void setFuseSpeed(int fuseSpeed) {}
    @Override
    public LivingEntity getTarget() {
        return super.getTarget();
    }
    @Override
    public EntityNavigation getNavigation() {
        return super.getNavigation();
    }
    @Override
    public LookControl getLookControl() {
        return super.getLookControl();
    }
    @Override
    public Random getRandom() {
        return super.getRandom();
    }
    @Override
    public double getX2() {
        return this.getX();
    }
    @Override
    public double getY2() {
        return this.getY();
    }
    @Override
    public double getZ2() {
        return this.getZ();
    }
    @Override
    public int getBlockX2() {
        return this.getBlockX();
    }
    @Override
    public int getBlockZ2() {
        return this.getBlockZ();
    }
    @Override
    public void lookAtEntity(Entity targetEntity, float maxYawChange, float maxPitchChange) {
        super.lookAtEntity(targetEntity, maxYawChange, maxPitchChange);
    }
    @Override
    public boolean hasPassengerDeep(Entity passenger) {
        return super.hasPassengerDeep(passenger);
    }
    @Override
    public boolean hasVehicle() {
        return super.hasVehicle();
    }
}
