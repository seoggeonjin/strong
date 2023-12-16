package net.ua.mixin.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.block.entity.*;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.*;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stat;
import net.minecraft.stat.Stats;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.village.TradeOfferList;
import net.minecraft.world.*;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.*;

import java.util.*;
import java.util.function.Predicate;

@Mixin(PlayerEntity.class)
public abstract class pl extends LivingEntity {
    @Unique
    public PlayerEntity p = (PlayerEntity) (Object) this;
    @Shadow
    @Final
    private static final Logger LOGGER = LogUtils.getLogger();
    @Shadow
    @Final
    public static final Arm DEFAULT_MAIN_ARM = Arm.RIGHT;
    @Shadow
    @Final
    public static final EntityDimensions STANDING_DIMENSIONS = EntityDimensions.changing(0.6f, 1.8f);
    @Unique
    private static final ImmutableMap<Object, Object> POSE_DIMENSIONS = ImmutableMap.builder().put(EntityPose.STANDING, STANDING_DIMENSIONS).put(EntityPose.SLEEPING, SLEEPING_DIMENSIONS).put(EntityPose.FALL_FLYING, EntityDimensions.changing(0.6f, 0.6f)).put(EntityPose.SWIMMING, EntityDimensions.changing(0.6f, 0.6f)).put(EntityPose.SPIN_ATTACK, EntityDimensions.changing(0.6f, 0.6f)).put(EntityPose.CROUCHING, EntityDimensions.changing(0.6f, 1.5f)).put(EntityPose.DYING, EntityDimensions.fixed(0.2f, 0.2f)).build();
    @Shadow
    @Final
    private static final TrackedData<Float> ABSORPTION_AMOUNT = DataTracker.registerData(pl.class, TrackedDataHandlerRegistry.FLOAT);
    @Shadow
    @Final
    private static final TrackedData<Integer> SCORE = DataTracker.registerData(pl.class, TrackedDataHandlerRegistry.INTEGER);
    @Shadow
    @Final
    protected static final TrackedData<Byte> PLAYER_MODEL_PARTS = DataTracker.registerData(pl.class, TrackedDataHandlerRegistry.BYTE);
    @Shadow
    @Final
    protected static final TrackedData<Byte> MAIN_ARM = DataTracker.registerData(pl.class, TrackedDataHandlerRegistry.BYTE);
    @Shadow
    @Final
    protected static final TrackedData<NbtCompound> LEFT_SHOULDER_ENTITY = DataTracker.registerData(pl.class, TrackedDataHandlerRegistry.NBT_COMPOUND);
    @Shadow
    @Final
    protected static final TrackedData<NbtCompound> RIGHT_SHOULDER_ENTITY = DataTracker.registerData(pl.class, TrackedDataHandlerRegistry.NBT_COMPOUND);
    @Shadow
    private long shoulderEntityAddedTime;
    @Shadow
    @Final
    private final PlayerInventory inventory = new PlayerInventory(p);
    @Shadow
    protected EnderChestInventory enderChestInventory = new EnderChestInventory();
    @Mutable
    @Shadow
    @Final
    public final PlayerScreenHandler playerScreenHandler;
    @Shadow
    public ScreenHandler currentScreenHandler;
    @Shadow
    protected HungerManager hungerManager = new HungerManager();
    @Shadow
    protected int abilityResyncCountdown;
    @Shadow
    public float prevStrideDistance;
    @Shadow
    public float strideDistance;
    @Shadow
    public double prevCapeX;
    @Shadow
    public double prevCapeY;
    @Shadow
    public double prevCapeZ;
    @Shadow
    public double capeX;
    @Shadow
    public double capeY;
    @Shadow
    public double capeZ;
    @Shadow
    private int sleepTimer;
    @Shadow
    protected boolean isSubmergedInWater;
    @Shadow
    @Final
    private final PlayerAbilities abilities = new PlayerAbilities();
    @Shadow
    public int experienceLevel;
    @Shadow
    public int totalExperience;
    @Shadow
    public float experienceProgress;
    @Shadow
    protected int enchantmentTableSeed;
    @Shadow
    private int lastPlayedLevelUpSoundTime;
    @Mutable
    @Shadow
    @Final
    private final GameProfile gameProfile;
    @Shadow
    private boolean reducedDebugInfo;
    @Final
    @Shadow
    private final ItemCooldownManager itemCooldownManager = this.createCooldownManager();
    @Shadow
    private Optional<GlobalPos> lastDeathPos = Optional.empty();
    @Shadow
    protected float damageTiltYaw;

    public pl(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(EntityType.PLAYER, world);
        this.setUuid(gameProfile.getId());
        this.gameProfile = gameProfile;
        this.playerScreenHandler = new PlayerScreenHandler(this.inventory, !world.isClient, p);
        this.currentScreenHandler = this.playerScreenHandler;
        this.refreshPositionAndAngles((double)pos.getX() + 0.5, pos.getY() + 1, (double)pos.getZ() + 0.5, yaw, 0.0f);
        this.field_6215 = 180.0f;
    }

    @Overwrite
    public boolean isBlockBreakingRestricted(World world, BlockPos pos, GameMode gameMode) {
        if (!gameMode.isBlockBreakingRestricted()) {
            return false;
        }
        if (gameMode == GameMode.SPECTATOR) {
            return true;
        }
        if (this.canModifyBlocks()) {
            return false;
        }
        ItemStack itemStack = this.getMainHandStack();
        return itemStack.isEmpty() || !itemStack.canDestroy(world.getRegistryManager().get(RegistryKeys.BLOCK), new CachedBlockPosition(world, pos, false));
    }

    @Overwrite
    public static DefaultAttributeContainer.Builder createPlayerAttributes() {
        return LivingEntity.createLivingAttributes().add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1.0).add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.1f).add(EntityAttributes.GENERIC_ATTACK_SPEED).add(EntityAttributes.GENERIC_LUCK);
    }

    // @Override
    // public float getHealth() {
    //     return 19.0f;
    // }
    @Overwrite
    public void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(ABSORPTION_AMOUNT, (float) 0.0);
        this.dataTracker.startTracking(SCORE, 0);
        this.dataTracker.startTracking(PLAYER_MODEL_PARTS, (byte)0);
        this.dataTracker.startTracking(MAIN_ARM, (byte)DEFAULT_MAIN_ARM.getId());
        this.dataTracker.startTracking(LEFT_SHOULDER_ENTITY, new NbtCompound());
        this.dataTracker.startTracking(RIGHT_SHOULDER_ENTITY, new NbtCompound());
    }


    @Overwrite
    public boolean shouldCancelInteraction() {
        return this.isSneaking();
    }
    @Overwrite
    public boolean shouldDismount() {
        return this.isSneaking();
    }

    @Overwrite
    public boolean clipAtLedge() {
        return this.isSneaking();
    }

    @Overwrite
    public boolean updateWaterSubmersionState() {
        this.isSubmergedInWater = this.isSubmergedIn(FluidTags.WATER);
        return this.isSubmergedInWater;
    }
    @Overwrite
    public void updateTurtleHelmet() {
        ItemStack itemStack = this.getEquippedStack(EquipmentSlot.HEAD);
        if (itemStack.isOf(Items.TURTLE_HELMET) && !this.isSubmergedIn(FluidTags.WATER)) {
            this.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 200, 0, false, false, true));
        }
    }
    @Overwrite
    public ItemCooldownManager createCooldownManager() {
        return new ItemCooldownManager();
    }
    @Overwrite
    public void updateCapeAngles() {
        this.prevCapeX = this.capeX;
        this.prevCapeY = this.capeY;
        this.prevCapeZ = this.capeZ;
        double d = this.getX() - this.capeX;
        double e = this.getY() - this.capeY;
        double f = this.getZ() - this.capeZ;
        if (d > 10.0) {
            this.prevCapeX = this.capeX = this.getX();
        }
        if (f > 10.0) {
            this.prevCapeZ = this.capeZ = this.getZ();
        }
        if (e > 10.0) {
            this.prevCapeY = this.capeY = this.getY();
        }
        if (d < -10.0) {
            this.prevCapeX = this.capeX = this.getX();
        }
        if (f < -10.0) {
            this.prevCapeZ = this.capeZ = this.getZ();
        }
        if (e < -10.0) {
            this.prevCapeY = this.capeY = this.getY();
        }
        this.capeX += d * 0.25;
        this.capeZ += f * 0.25;
        this.capeY += e * 0.25;
    }

    @Overwrite
    public void updatePose() {
        if (!this.canChangeIntoPose(EntityPose.SWIMMING)) {
            return;
        }
        EntityPose entityPose = this.isFallFlying() ? EntityPose.FALL_FLYING : (this.isSleeping() ? EntityPose.SLEEPING : (this.isSwimming() ? EntityPose.SWIMMING : (this.isUsingRiptide() ? EntityPose.SPIN_ATTACK : (this.isSneaking() && !this.abilities.flying ? EntityPose.CROUCHING : EntityPose.STANDING))));
        EntityPose entityPose2 = this.isSpectator() || this.hasVehicle() || this.canChangeIntoPose(entityPose) ? entityPose : (this.canChangeIntoPose(EntityPose.CROUCHING) ? EntityPose.CROUCHING : EntityPose.SWIMMING);
        this.setPose(entityPose2);
    }

    @Overwrite
    public boolean canChangeIntoPose(EntityPose pose) {
        return this.getWorld().isSpaceEmpty(this, this.getDimensions(pose).getBoxAt(this.getPos()).contract(1.0E-7));
    }

    @Overwrite
    public int getMaxNetherPortalTime() {
        return this.abilities.invulnerable ? 1 : 80;
    }

    @Overwrite
    public SoundEvent getSwimSound() {
        return SoundEvents.ENTITY_PLAYER_SWIM;
    }

    @Overwrite
    public SoundEvent getSplashSound() {
        return SoundEvents.ENTITY_PLAYER_SPLASH;
    }

    @Overwrite
    public SoundEvent getHighSpeedSplashSound() {
        return SoundEvents.ENTITY_PLAYER_SPLASH_HIGH_SPEED;
    }

    @Overwrite
    public int getDefaultPortalCooldown() {
        return 10;
    }

    @Overwrite
    public void playSound(SoundEvent sound, float volume, float pitch) {
        this.getWorld().playSound(p, this.getX(), this.getY(), this.getZ(), sound, this.getSoundCategory(), volume, pitch);
    }

    @Overwrite
    public void playSound(SoundEvent event, SoundCategory category, float volume, float pitch) {
    }

    @Overwrite
    public SoundCategory getSoundCategory() {
        return SoundCategory.PLAYERS;
    }

    @Overwrite
    public int getBurningDuration() {
        return 20;
    }

    @Overwrite
    public void handleStatus(byte status) {
        if (status == EntityStatuses.CONSUME_ITEM) {
            this.consumeItem();
        } else if (status == EntityStatuses.USE_FULL_DEBUG_INFO) {
            this.reducedDebugInfo = false;
        } else if (status == EntityStatuses.USE_REDUCED_DEBUG_INFO) {
            this.reducedDebugInfo = true;
        } else if (status == EntityStatuses.ADD_CLOUD_PARTICLES) {
            this.spawnParticles(ParticleTypes.CLOUD);
        } else {
            super.handleStatus(status);
        }
    }
    @Overwrite
    public void spawnParticles(ParticleEffect parameters) {
        for (int i = 0; i < 5; ++i) {
            double d = this.random.nextGaussian() * 0.02;
            double e = this.random.nextGaussian() * 0.02;
            double f = this.random.nextGaussian() * 0.02;
            this.getWorld().addParticle(parameters, this.getParticleX(1.0), this.getRandomBodyY() + 1.0, this.getParticleZ(1.0), d, e, f);
        }
    }

    @Overwrite
    public void closeHandledScreen() {
        this.currentScreenHandler = this.playerScreenHandler;
    }

    @Overwrite
    public void onHandledScreenClosed() {
    }

    @Overwrite
    public void tickRiding() {
        if (!this.getWorld().isClient && this.shouldDismount() && this.hasVehicle()) {
            this.stopRiding();
            this.setSneaking(false);
            return;
        }
        double d = this.getX();
        double e = this.getY();
        double f = this.getZ();
        super.tickRiding();
        this.prevStrideDistance = this.strideDistance;
        this.strideDistance = 0.0f;
        this.increaseRidingMotionStats(this.getX() - d, this.getY() - e, this.getZ() - f);
    }

    @Overwrite
    public void tickNewAi() {
        super.tickNewAi();
        this.tickHandSwing();
        this.headYaw = this.getYaw();
    }

    @Overwrite
    public void tickMovement() {
        if (this.abilityResyncCountdown > 0) {
            --this.abilityResyncCountdown;
        }
        if (this.getWorld().getDifficulty() == Difficulty.PEACEFUL && this.getWorld().getGameRules().getBoolean(GameRules.NATURAL_REGENERATION)) {
            if (this.getHealth() < this.getMaxHealth() && this.age % 20 == 0) {
                this.heal(1.0f);
            }
            if (this.hungerManager.isNotFull() && this.age % 10 == 0) {
                this.hungerManager.setFoodLevel(this.hungerManager.getFoodLevel() + 1);
            }
        }
        this.inventory.updateItems();
        this.prevStrideDistance = this.strideDistance;
        super.tickMovement();
        this.setMovementSpeed((float)this.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED));
        float f = !this.isOnGround() || this.isDead() || this.isSwimming() ? 0.0f : Math.min(0.1f, (float)this.getVelocity().horizontalLength());
        this.strideDistance += (f - this.strideDistance) * 0.4f;
        if (this.getHealth() > 0.0f && !this.isSpectator()) {
            Box box = this.hasVehicle() && !Objects.requireNonNull(this.getVehicle()).isRemoved() ? this.getBoundingBox().union(this.getVehicle().getBoundingBox()).expand(1.0, 0.0, 1.0) : this.getBoundingBox().expand(1.0, 0.5, 1.0);
            List<Entity> list = this.getWorld().getOtherEntities(this, box);
            ArrayList<Entity> list2 = Lists.newArrayList();
            for (Entity entity : list) {
                if (entity.getType() == EntityType.EXPERIENCE_ORB) {
                    list2.add(entity);
                    continue;
                }
                if (entity.isRemoved()) continue;
                this.collideWithEntity(entity);
            }
            if (!list2.isEmpty()) {
                this.collideWithEntity(Util.getRandom(list2, this.random));
            }
        }
        this.updateShoulderEntity(this.getShoulderEntityLeft());
        this.updateShoulderEntity(this.getShoulderEntityRight());
        if (!this.getWorld().isClient && (this.fallDistance > 0.5f || this.isTouchingWater()) || this.abilities.flying || this.isSleeping() || this.inPowderSnow) {
            this.dropShoulderEntities();
        }
    }
    @Overwrite
    public void updateShoulderEntity(@Nullable NbtCompound entityNbt) {
        if (!(entityNbt == null || entityNbt.contains("Silent") && entityNbt.getBoolean("Silent") || this.getWorld().random.nextInt(200) != 0)) {
            String string = entityNbt.getString("id");
            EntityType.get(string).filter(entityType -> entityType == EntityType.PARROT).ifPresent(parrotType -> {
                if (!ParrotEntity.imitateNearbyMob(this.getWorld(), this)) {
                    this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), ParrotEntity.getRandomSound(this.getWorld(), this.getWorld().random), this.getSoundCategory(), 1.0f, ParrotEntity.getSoundPitch(this.getWorld().random));
                }
            });
        }
    }
    @Overwrite
    public void collideWithEntity(Entity entity) {
        entity.onPlayerCollision(p);
    }
    @Overwrite
    public int getScore() {
        return this.dataTracker.get(SCORE);
    }
    @Overwrite
    public void setScore(int score) {
        this.dataTracker.set(SCORE, score);
    }
    @Overwrite
    public void addScore(int score) {
        int i = this.getScore();
        this.dataTracker.set(SCORE, i + score);
    }
    @Overwrite
    public void useRiptide(int riptideTicks) {
        this.riptideTicks = riptideTicks;
        if (!this.getWorld().isClient) {
            this.dropShoulderEntities();
            this.setLivingFlag(LivingEntity.USING_RIPTIDE_FLAG, true);
        }
    }

    @Overwrite
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);
        this.refreshPosition();
        if (!this.isSpectator()) {
            this.drop(damageSource);
        }
        if (damageSource != null) {
            this.setVelocity(-MathHelper.cos((this.getDamageTiltYaw() + this.getYaw()) * ((float)Math.PI / 180)) * 0.1f, 0.1f, -MathHelper.sin((this.getDamageTiltYaw() + this.getYaw()) * ((float)Math.PI / 180)) * 0.1f);
        } else {
            this.setVelocity(0.0, 0.1, 0.0);
        }
        this.incrementStat(Stats.DEATHS);
        this.resetStat(Stats.CUSTOM.getOrCreateStat(Stats.TIME_SINCE_DEATH));
        this.resetStat(Stats.CUSTOM.getOrCreateStat(Stats.TIME_SINCE_REST));
        this.extinguish();
        this.setOnFire(false);
        this.setLastDeathPos(Optional.of(GlobalPos.create(this.getWorld().getRegistryKey(), this.getBlockPos())));
    }

    @Overwrite
    public void dropInventory() {
        super.dropInventory();
        if (!this.getWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) {
            this.vanishCursedItems();
            this.inventory.dropAll();
        }
    }
    @Overwrite
    public void vanishCursedItems() {
        for (int i = 0; i < this.inventory.size(); ++i) {
            ItemStack itemStack = this.inventory.getStack(i);
            if (itemStack.isEmpty() || !EnchantmentHelper.hasVanishingCurse(itemStack)) continue;
            this.inventory.removeStack(i);
        }
    }

    @Overwrite
    public SoundEvent getHurtSound(DamageSource source) {
        return source.getType().effects().getSound();
    }

    @Overwrite
    public SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_PLAYER_DEATH;
    }

    @Overwrite
    @Nullable
    public ItemEntity dropItem(ItemStack stack, boolean retainOwnership) {
        return this.dropItem(stack, false, retainOwnership);
    }

    @Overwrite
    @Nullable
    public ItemEntity dropItem(ItemStack stack, boolean throwRandomly, boolean retainOwnership) {
        if (stack.isEmpty()) {
            return null;
        }
        if (this.getWorld().isClient) {
            this.swingHand(Hand.MAIN_HAND);
        }
        double d = this.getEyeY() - (double)0.3f;
        ItemEntity itemEntity = new ItemEntity(this.getWorld(), this.getX(), d, this.getZ(), stack);
        itemEntity.setPickupDelay(40);
        if (retainOwnership) {
            itemEntity.setThrower(this);
        }
        if (throwRandomly) {
            float f = this.random.nextFloat() * 0.5f;
            float g = this.random.nextFloat() * ((float)Math.PI * 2);
            itemEntity.setVelocity(-MathHelper.sin(g) * f, 0.2f, MathHelper.cos(g) * f);
        } else {
            float g = MathHelper.sin(this.getPitch() * ((float)Math.PI / 180));
            float h = MathHelper.cos(this.getPitch() * ((float)Math.PI / 180));
            float i = MathHelper.sin(this.getYaw() * ((float)Math.PI / 180));
            float j = MathHelper.cos(this.getYaw() * ((float)Math.PI / 180));
            float k = this.random.nextFloat() * ((float)Math.PI * 2);
            float l = 0.02f * this.random.nextFloat();
            itemEntity.setVelocity((double)(-i * h * 0.3f) + Math.cos(k) * (double)l, -g * 0.3f + 0.1f + (this.random.nextFloat() - this.random.nextFloat()) * 0.1f, (double)(j * h * 0.3f) + Math.sin(k) * (double)l);
        }
        return itemEntity;
    }
    @Overwrite
    public float getBlockBreakingSpeed(BlockState block) {
        float f = this.inventory.getBlockBreakingSpeed(block);
        if (f > 1.0f) {
            int i = EnchantmentHelper.getEfficiency(this);
            ItemStack itemStack = this.getMainHandStack();
            if (i > 0 && !itemStack.isEmpty()) {
                f += (float)(i * i + 1);
            }
        }
        if (StatusEffectUtil.hasHaste(this)) {
            f *= 1.0f + (float)(StatusEffectUtil.getHasteAmplifier(this) + 1) * 0.2f;
        }
        if (this.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            f *= (switch (Objects.requireNonNull(this.getStatusEffect(StatusEffects.MINING_FATIGUE)).getAmplifier()) {
                case 0 -> 0.3f;
                case 1 -> 0.09f;
                case 2 -> 0.0027f;
                default -> 8.1E-4f;
            });
        }
        if (this.isSubmergedIn(FluidTags.WATER) && !EnchantmentHelper.hasAquaAffinity(this)) {
            f /= 5.0f;
        }
        if (!this.isOnGround()) {
            f /= 5.0f;
        }
        return f;
    }

    @Overwrite
    public boolean canHarvest(BlockState state) {
        return !state.isToolRequired() || this.inventory.getMainHandStack().isSuitableFor(state);
    }

    @Overwrite
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.setUuid(this.gameProfile.getId());
        NbtList nbtList = nbt.getList("Inventory", NbtElement.COMPOUND_TYPE);
        this.inventory.readNbt(nbtList);
        this.inventory.selectedSlot = nbt.getInt("SelectedItemSlot");
        this.sleepTimer = nbt.getShort("SleepTimer");
        this.experienceProgress = nbt.getFloat("XpP");
        this.experienceLevel = nbt.getInt("XpLevel");
        this.totalExperience = nbt.getInt("XpTotal");
        this.enchantmentTableSeed = nbt.getInt("XpSeed");
        if (this.enchantmentTableSeed == 0) {
            this.enchantmentTableSeed = this.random.nextInt();
        }
        this.setScore(nbt.getInt("Score"));
        this.hungerManager.readNbt(nbt);
        this.abilities.readNbt(nbt);
        Objects.requireNonNull(this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)).setBaseValue(this.abilities.getWalkSpeed());
        if (nbt.contains("EnderItems", NbtElement.LIST_TYPE)) {
            this.enderChestInventory.readNbtList(nbt.getList("EnderItems", NbtElement.COMPOUND_TYPE));
        }
        if (nbt.contains("ShoulderEntityLeft", NbtElement.COMPOUND_TYPE)) {
            this.setShoulderEntityLeft(nbt.getCompound("ShoulderEntityLeft"));
        }
        if (nbt.contains("ShoulderEntityRight", NbtElement.COMPOUND_TYPE)) {
            this.setShoulderEntityRight(nbt.getCompound("ShoulderEntityRight"));
        }
        if (nbt.contains("LastDeathLocation", NbtElement.COMPOUND_TYPE)) {
            this.setLastDeathPos(GlobalPos.CODEC.parse(NbtOps.INSTANCE, nbt.get("LastDeathLocation")).resultOrPartial(LOGGER::error));
        }
    }

    @Overwrite
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        NbtHelper.putDataVersion(nbt);
        nbt.put("Inventory", this.inventory.writeNbt(new NbtList()));
        nbt.putInt("SelectedItemSlot", this.inventory.selectedSlot);
        nbt.putShort("SleepTimer", (short)this.sleepTimer);
        nbt.putFloat("XpP", this.experienceProgress);
        nbt.putInt("XpLevel", this.experienceLevel);
        nbt.putInt("XpTotal", this.totalExperience);
        nbt.putInt("XpSeed", this.enchantmentTableSeed);
        nbt.putInt("Score", this.getScore());
        this.hungerManager.writeNbt(nbt);
        this.abilities.writeNbt(nbt);
        nbt.put("EnderItems", this.enderChestInventory.toNbtList());
        if (!this.getShoulderEntityLeft().isEmpty()) {
            nbt.put("ShoulderEntityLeft", this.getShoulderEntityLeft());
        }
        if (!this.getShoulderEntityRight().isEmpty()) {
            nbt.put("ShoulderEntityRight", this.getShoulderEntityRight());
        }
        this.getLastDeathPos().flatMap(pos -> GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).resultOrPartial(LOGGER::error)).ifPresent(pos -> nbt.put("LastDeathLocation", pos));
    }

    @Overwrite
    public boolean isInvulnerableTo(DamageSource damageSource) {
        if (super.isInvulnerableTo(damageSource)) {
            return true;
        }
        if (damageSource.isIn(DamageTypeTags.IS_DROWNING)) {
            return !this.getWorld().getGameRules().getBoolean(GameRules.DROWNING_DAMAGE);
        }
        if (damageSource.isIn(DamageTypeTags.IS_FALL)) {
            return !this.getWorld().getGameRules().getBoolean(GameRules.FALL_DAMAGE);
        }
        if (damageSource.isIn(DamageTypeTags.IS_FIRE)) {
            return !this.getWorld().getGameRules().getBoolean(GameRules.FIRE_DAMAGE);
        }
        if (damageSource.isIn(DamageTypeTags.IS_FREEZING)) {
            return !this.getWorld().getGameRules().getBoolean(GameRules.FREEZE_DAMAGE);
        }
        return false;
    }

    @Overwrite
    public boolean damage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        }
        if (this.abilities.invulnerable && !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        this.despawnCounter = 0;
        if (this.isDead()) {
            return false;
        }
        if (!this.getWorld().isClient) {
            this.dropShoulderEntities();
        }
        if (source.isScaledWithDifficulty()) {
            if (this.getWorld().getDifficulty() == Difficulty.PEACEFUL) {
                amount = 0.0f;
            }
            if (this.getWorld().getDifficulty() == Difficulty.EASY) {
                amount = Math.min(amount / 2.0f + 1.0f, amount);
            }
            if (this.getWorld().getDifficulty() == Difficulty.HARD) {
                amount = amount * 3.0f / 2.0f;
            }
        }
        if (amount == 0.0f) {
            return false;
        }
        return super.damage(source, amount);
    }

    @Overwrite
    public void takeShieldHit(LivingEntity attacker) {
        super.takeShieldHit(attacker);
        if (attacker.disablesShield()) {
            this.disableShield(true);
        }
    }

    @Overwrite
    public boolean canTakeDamage() {
        return !this.getAbilities().invulnerable && super.canTakeDamage();
    }
    @Overwrite
    public boolean shouldDamagePlayer(net.minecraft.entity.player.PlayerEntity player) {
        AbstractTeam abstractTeam = this.getScoreboardTeam();
        AbstractTeam abstractTeam2 = player.getScoreboardTeam();
        if (abstractTeam == null) {
            return true;
        }
        if (!abstractTeam.isEqual(abstractTeam2)) {
            return true;
        }
        return abstractTeam.isFriendlyFireAllowed();
    }

    @Overwrite
    public void damageArmor(DamageSource source, float amount) {
        this.inventory.damageArmor(source, amount, PlayerInventory.ARMOR_SLOTS);
    }

    @Overwrite
    public void damageHelmet(DamageSource source, float amount) {
        this.inventory.damageArmor(source, amount, PlayerInventory.HELMET_SLOTS);
    }

    @Overwrite
    public void damageShield(float amount) {
        if (!this.activeItemStack.isOf(Items.SHIELD)) {
            return;
        }
        if (!this.getWorld().isClient) {
            this.incrementStat(Stats.USED.getOrCreateStat(this.activeItemStack.getItem()));
        }
        if (amount >= 3.0f) {
            int i = 1 + MathHelper.floor(amount);
            Hand hand = this.getActiveHand();
            this.activeItemStack.damage(i, this, player -> player.sendToolBreakStatus(hand));
            if (this.activeItemStack.isEmpty()) {
                if (hand == Hand.MAIN_HAND) {
                    this.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                } else {
                    this.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                }
                this.activeItemStack = ItemStack.EMPTY;
                this.playSound(SoundEvents.ITEM_SHIELD_BREAK, 0.8f, 0.8f + this.getWorld().random.nextFloat() * 0.4f);
            }
        }
    }

    @Overwrite
    public void applyDamage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return;
        }
        amount = this.applyArmorToDamage(source, amount);
        float f = amount = this.modifyAppliedDamage(source, amount);
        amount = Math.max(amount - this.getAbsorptionAmount(), 0.0f);
        this.setAbsorptionAmount(this.getAbsorptionAmount() - (f - amount));
        float g = f - amount;
        if (g > 0.0f && g < 3.4028235E37f) {
            this.increaseStat(Stats.DAMAGE_ABSORBED, Math.round(g * 10.0f));
        }
        if (amount == 0.0f) {
            return;
        }
        this.addExhaustion(source.getExhaustion());
        this.getDamageTracker().onDamage(source, amount);
        this.setHealth(this.getHealth() - amount);
        if (amount < 3.4028235E37f) {
            this.increaseStat(Stats.DAMAGE_TAKEN, Math.round(amount * 10.0f));
        }
        this.emitGameEvent(GameEvent.ENTITY_DAMAGE);
    }

    @Overwrite
    public boolean isOnSoulSpeedBlock() {
        return !this.abilities.flying && super.isOnSoulSpeedBlock();
    }
    @Overwrite
    public boolean shouldFilterText() {
        return false;
    }
    @Overwrite
    public void openEditSignScreen(SignBlockEntity sign, boolean front) {
    }
    @Overwrite
    public void openCommandBlockMinecartScreen(CommandBlockExecutor commandBlockExecutor) {
    }
    @Overwrite
    public void openCommandBlockScreen(CommandBlockBlockEntity commandBlock) {
    }
    @Overwrite
    public void openStructureBlockScreen(StructureBlockBlockEntity structureBlock) {
    }
    @Overwrite
    public void openJigsawScreen(JigsawBlockEntity jigsaw) {
    }
    @Overwrite
    public void openHorseInventory(AbstractHorseEntity horse, Inventory inventory) {
    }
    @Overwrite
    public OptionalInt openHandledScreen(@Nullable NamedScreenHandlerFactory factory) {
        return OptionalInt.empty();
    }
    @Overwrite
    public void sendTradeOffers(int syncId, TradeOfferList offers, int levelProgress, int experience, boolean leveled, boolean refreshable) {
    }

    @Overwrite
    public void useBook(ItemStack book, Hand hand) {
    }
    @Overwrite
    public ActionResult interact(Entity entity, Hand hand) {
        if (this.isSpectator()) {
            if (entity instanceof NamedScreenHandlerFactory) {
                this.openHandledScreen((NamedScreenHandlerFactory) entity);
            }
            return ActionResult.PASS;
        }
        ItemStack itemStack = this.getStackInHand(hand);
        ItemStack itemStack2 = itemStack.copy();
        ActionResult actionResult = entity.interact(p, hand);
        if (actionResult.isAccepted()) {
            if (this.abilities.creativeMode && itemStack == this.getStackInHand(hand) && itemStack.getCount() < itemStack2.getCount()) {
                itemStack.setCount(itemStack2.getCount());
            }
            return actionResult;
        }
        if (!itemStack.isEmpty() && entity instanceof LivingEntity) {
            ActionResult actionResult2;
            if (this.abilities.creativeMode) {
                itemStack = itemStack2;
            }
            if ((actionResult2 = itemStack.useOnEntity(p, (LivingEntity)entity, hand)).isAccepted()) {
                this.getWorld().emitGameEvent(GameEvent.ENTITY_INTERACT, entity.getPos(), GameEvent.Emitter.of(this));
                if (itemStack.isEmpty() && !this.abilities.creativeMode) {
                    this.setStackInHand(hand, ItemStack.EMPTY);
                }
                return actionResult2;
            }
        }
        return ActionResult.PASS;
    }

    @Overwrite
    public float getUnscaledRidingOffset(Entity vehicle) {
        return -0.6f;
    }

    @Overwrite
    public void dismountVehicle() {
        super.dismountVehicle();
        this.ridingCooldown = 0;
    }

    @Overwrite
    public boolean isImmobile() {
        return super.isImmobile() || this.isSleeping();
    }

    @Overwrite
    public boolean shouldSwimInFluids() {
        return !this.abilities.flying;
    }

    @Overwrite
    public Vec3d adjustMovementForSneaking(Vec3d movement, MovementType type) {
        if (!this.abilities.flying && movement.y <= 0.0 && (type == MovementType.SELF || type == MovementType.PLAYER) && this.clipAtLedge() && this.method_30263()) {
            double d = movement.x;
            double e = movement.z;
            while (d != 0.0 && this.getWorld().isSpaceEmpty(this, this.getBoundingBox().offset(d, -this.getStepHeight(), 0.0))) {
                if (d < 0.05 && d >= -0.05) {
                    d = 0.0;
                    continue;
                }
                if (d > 0.0) {
                    d -= 0.05;
                    continue;
                }
                d += 0.05;
            }
            while (e != 0.0 && this.getWorld().isSpaceEmpty(this, this.getBoundingBox().offset(0.0, -this.getStepHeight(), e))) {
                if (e < 0.05 && e >= -0.05) {
                    e = 0.0;
                    continue;
                }
                if (e > 0.0) {
                    e -= 0.05;
                    continue;
                }
                e += 0.05;
            }
            while (d != 0.0 && e != 0.0 && this.getWorld().isSpaceEmpty(this, this.getBoundingBox().offset(d, -this.getStepHeight(), e))) {
                d = d < 0.05 && d >= -0.05 ? 0.0 : (d > 0.0 ? d - 0.05 : d + 0.05);
                if (e < 0.05 && e >= -0.05) {
                    e = 0.0;
                    continue;
                }
                if (e > 0.0) {
                    e -= 0.05;
                    continue;
                }
                e += 0.05;
            }
            movement = new Vec3d(d, movement.y, e);
        }
        return movement;
    }
    @Overwrite
    public boolean method_30263() {
        return this.isOnGround() || this.fallDistance < this.getStepHeight() && !this.getWorld().isSpaceEmpty(this, this.getBoundingBox().offset(0.0, this.fallDistance - this.getStepHeight(), 0.0));
    }
    @Overwrite
    public void attack(Entity target) {
        if (!target.isAttackable()) {
            return;
        }
        if (target.handleAttack(this)) {
            return;
        }
        float f = (float)this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        float g = target instanceof LivingEntity ? EnchantmentHelper.getAttackDamage(this.getMainHandStack(), ((LivingEntity)target).getGroup()) : EnchantmentHelper.getAttackDamage(this.getMainHandStack(), EntityGroup.DEFAULT);
        float h = this.getAttackCooldownProgress(0.5f);
        g *= h;
        this.resetLastAttackedTicks();
        if ((f *= 0.2f + h * h * 0.8f) > 0.0f || g > 0.0f) {
            ItemStack itemStack;
            boolean bl = h > 0.9f;
            boolean bl2 = false;
            int i = 0;
            i += EnchantmentHelper.getKnockback(this);
            if (this.isSprinting() && bl) {
                this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, this.getSoundCategory(), 1.0f, 1.0f);
                ++i;
                bl2 = true;
            }
            boolean bl3 = bl && this.fallDistance > 0.0f && !this.isOnGround() && !this.isClimbing() && !this.isTouchingWater() && !this.hasStatusEffect(StatusEffects.BLINDNESS) && !this.hasVehicle() && target instanceof LivingEntity;
            bl3 = bl3 && !this.isSprinting();
            if (bl3) {
                f *= 1.5f;
            }
            f += g;
            boolean bl42 = false;
            double d = this.horizontalSpeed - this.prevHorizontalSpeed;
            if (bl && !bl3 && !bl2 && this.isOnGround() && d < (double)this.getMovementSpeed() && (itemStack = this.getStackInHand(Hand.MAIN_HAND)).getItem() instanceof SwordItem) {
                bl42 = true;
            }
            float j = 0.0f;
            boolean bl5 = false;
            int k = EnchantmentHelper.getFireAspect(this);
            if (target instanceof LivingEntity) {
                j = ((LivingEntity)target).getHealth();
                if (k > 0 && !target.isOnFire()) {
                    bl5 = true;
                    target.setOnFireFor(1);
                }
            }
            Vec3d vec3d = target.getVelocity();
            boolean bl6 = target.damage(this.getDamageSources().playerAttack(p), f);
            if (bl6) {
                if (i > 0) {
                    if (target instanceof LivingEntity) {
                        ((LivingEntity)target).takeKnockback((float)i * 0.5f, MathHelper.sin(this.getYaw() * ((float)Math.PI / 180)), -MathHelper.cos(this.getYaw() * ((float)Math.PI / 180)));
                    } else {
                        target.addVelocity(-MathHelper.sin(this.getYaw() * ((float)Math.PI / 180)) * (float)i * 0.5f, 0.1, MathHelper.cos(this.getYaw() * ((float)Math.PI / 180)) * (float)i * 0.5f);
                    }
                    this.setVelocity(this.getVelocity().multiply(0.6, 1.0, 0.6));
                    this.setSprinting(false);
                }
                if (bl42) {
                    float l = 1.0f + EnchantmentHelper.getSweepingMultiplier(this) * f;
                    List<LivingEntity> list = this.getWorld().getNonSpectatingEntities(LivingEntity.class, target.getBoundingBox().expand(1.0, 0.25, 1.0));
                    for (LivingEntity livingEntity : list) {
                        if (livingEntity == this || livingEntity == target || this.isTeammate(livingEntity) || livingEntity instanceof ArmorStandEntity && ((ArmorStandEntity)livingEntity).isMarker() || !(this.squaredDistanceTo(livingEntity) < 9.0)) continue;
                        livingEntity.takeKnockback(0.4f, MathHelper.sin(this.getYaw() * ((float)Math.PI / 180)), -MathHelper.cos(this.getYaw() * ((float)Math.PI / 180)));
                        livingEntity.damage(this.getDamageSources().playerAttack(p), l);
                    }
                    this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, this.getSoundCategory(), 1.0f, 1.0f);
                    this.spawnSweepAttackParticles();
                }
                if (target instanceof ServerPlayerEntity && target.velocityModified) {
                    ((ServerPlayerEntity)target).networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(target));
                    target.velocityModified = false;
                    target.setVelocity(vec3d);
                }
                if (bl3) {
                    this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, this.getSoundCategory(), 1.0f, 1.0f);
                    this.addCritParticles(target);
                }
                if (!bl3 && !bl42) {
                    if (bl) {
                        this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, this.getSoundCategory(), 1.0f, 1.0f);
                    } else {
                        this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_WEAK, this.getSoundCategory(), 1.0f, 1.0f);
                    }
                }
                if (g > 0.0f) {
                    this.addEnchantedHitParticles(target);
                }
                this.onAttacking(target);
                if (target instanceof LivingEntity) {
                    EnchantmentHelper.onUserDamaged((LivingEntity)target, this);
                }
                EnchantmentHelper.onTargetDamaged(this, target);
                ItemStack itemStack2 = this.getMainHandStack();
                Entity entity = target;
                if (target instanceof EnderDragonPart) {
                    entity = ((EnderDragonPart)target).owner;
                }
                if (!this.getWorld().isClient && !itemStack2.isEmpty() && entity instanceof LivingEntity) {
                    itemStack2.postHit((LivingEntity)entity, p);
                    if (itemStack2.isEmpty()) {
                        this.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                    }
                }
                if (target instanceof LivingEntity) {
                    float m = j - ((LivingEntity)target).getHealth();
                    this.increaseStat(Stats.DAMAGE_DEALT, Math.round(m * 10.0f));
                    if (k > 0) {
                        target.setOnFireFor(k * 4);
                    }
                    if (this.getWorld() instanceof ServerWorld && m > 2.0f) {
                        int m2 = (int) m;
                        int n = m2 / 2;
                        ((ServerWorld)this.getWorld()).spawnParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getBodyY(0.5), target.getZ(), n, 0.1, 0.0, 0.1, 0.2);
                    }
                }
                this.addExhaustion(0.1f);
            } else {
                this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE, this.getSoundCategory(), 1.0f, 1.0f);
                if (bl5) {
                    target.extinguish();
                }
            }
        }
    }

    @Overwrite
    public void attackLivingEntity(LivingEntity target) {
        this.attack(target);
    }
    @Overwrite
    public void disableShield(boolean sprinting) {
        float f = 0.25f + (float)EnchantmentHelper.getEfficiency(this) * 0.05f;
        if (sprinting) {
            f += 0.75f;
        }
        if (this.random.nextFloat() < f) {
            this.getItemCooldownManager().set(Items.SHIELD, 100);
            this.clearActiveItem();
            this.getWorld().sendEntityStatus(this, EntityStatuses.BREAK_SHIELD);
        }
    }

    @Overwrite
    public void addCritParticles(Entity target) {
    }

    @Overwrite
    public void addEnchantedHitParticles(Entity target) {
    }

    @Overwrite
    public void spawnSweepAttackParticles() {
        double d = -MathHelper.sin(this.getYaw() * ((float)Math.PI / 180));
        double e = MathHelper.cos(this.getYaw() * ((float)Math.PI / 180));
        if (this.getWorld() instanceof ServerWorld) {
            ((ServerWorld)this.getWorld()).spawnParticles(ParticleTypes.SWEEP_ATTACK, this.getX() + d, this.getBodyY(0.5), this.getZ() + e, 0, d, 0.0, e, 0.0);
        }
    }

    @Overwrite
    public void requestRespawn() {
    }

    @Overwrite
    public void remove(Entity.RemovalReason reason) {
        super.remove(reason);
        this.playerScreenHandler.onClosed(p);
        if (this.currentScreenHandler != null && this.shouldCloseHandledScreenOnRespawn()) {
            this.onHandledScreenClosed();
        }
    }

    @Overwrite
    public boolean isMainPlayer() {
        return false;
    }

    @Overwrite
    public GameProfile getGameProfile() {
        return this.gameProfile;
    }

    @Overwrite
    public PlayerInventory getInventory() {
        return this.inventory;
    }

    @Overwrite
    public PlayerAbilities getAbilities() {
        return this.abilities;
    }

    @Overwrite
    public void onPickupSlotClick(ItemStack cursorStack, ItemStack slotStack, ClickType clickType) {
    }

    @Overwrite
    public boolean shouldCloseHandledScreenOnRespawn() {
        return this.currentScreenHandler != this.playerScreenHandler;
    }

    @Overwrite
    public Either<net.minecraft.entity.player.PlayerEntity.SleepFailureReason, Unit> trySleep(BlockPos pos) {
        this.sleep(pos);
        this.sleepTimer = 0;
        return Either.right(Unit.INSTANCE);
    }

    @Overwrite
    public void wakeUp(boolean skipSleepTimer, boolean updateSleepingPlayers) {
        super.wakeUp();
        if (this.getWorld() instanceof ServerWorld && updateSleepingPlayers) {
            ((ServerWorld)this.getWorld()).updateSleepingPlayers();
        }
        this.sleepTimer = skipSleepTimer ? 0 : 100;
    }

    @Overwrite
    public void wakeUp() {
        this.wakeUp(true, true);
    }

    @Overwrite
    public static Optional<Vec3d> findRespawnPosition(ServerWorld world, BlockPos pos, float angle, boolean forced, boolean alive) {
        BlockState blockState = world.getBlockState(pos);
        Block block = blockState.getBlock();
        if (block instanceof RespawnAnchorBlock && (forced || blockState.get(RespawnAnchorBlock.CHARGES) > 0) && RespawnAnchorBlock.isNether(world)) {
            Optional<Vec3d> optional = RespawnAnchorBlock.findRespawnPosition(EntityType.PLAYER, world, pos);
            if (!forced && !alive && optional.isPresent()) {
                world.setBlockState(pos, blockState.with(RespawnAnchorBlock.CHARGES, blockState.get(RespawnAnchorBlock.CHARGES) - 1), Block.NOTIFY_ALL);
            }
            return optional;
        }
        if (block instanceof BedBlock && BedBlock.isBedWorking(world)) {
            return BedBlock.findWakeUpPosition(EntityType.PLAYER, world, pos, blockState.get(BedBlock.FACING), angle);
        }
        if (!forced) {
            return Optional.empty();
        }
        boolean bl = block.canMobSpawnInside(blockState);
        BlockState blockState2 = world.getBlockState(pos.up());
        boolean bl2 = blockState2.getBlock().canMobSpawnInside(blockState2);
        if (bl && bl2) {
            return Optional.of(new Vec3d((double)pos.getX() + 0.5, (double)pos.getY() + 0.1, (double)pos.getZ() + 0.5));
        }
        return Optional.empty();
    }

    @Overwrite
    public boolean canResetTimeBySleeping() {
        return this.isSleeping() && this.sleepTimer >= 100;
    }

    @Overwrite
    public int getSleepTimer() {
        return this.sleepTimer;
    }

    @Overwrite
    public void sendMessage(Text message, boolean overlay) {
    }

    @Overwrite
    public void incrementStat(Identifier stat) {
        this.incrementStat(Stats.CUSTOM.getOrCreateStat(stat));
    }

    @Overwrite
    public void increaseStat(Identifier stat, int amount) {
        this.increaseStat(Stats.CUSTOM.getOrCreateStat(stat), amount);
    }

    @Overwrite
    public void incrementStat(Stat<?> stat) {
        this.increaseStat(stat, 1);
    }

    @Overwrite
    public void increaseStat(Stat<?> stat, int amount) {
    }

    @Overwrite
    public void resetStat(Stat<?> stat) {
    }

    @Overwrite
    public int unlockRecipes(Collection<RecipeEntry<?>> recipes) {
        return 0;
    }

    @Overwrite
    public void onRecipeCrafted(RecipeEntry<?> recipe, List<ItemStack> ingredients) {
    }


    @Overwrite
    public int lockRecipes(Collection<RecipeEntry<?>> recipes) {
        return 0;
    }

    @Overwrite
    public void jump() {
        super.jump();
        this.incrementStat(Stats.JUMP);
        if (this.isSprinting()) {
            this.addExhaustion(0.2f);
        } else {
            this.addExhaustion(0.05f);
        }
    }

    @Overwrite
    public void travel(Vec3d movementInput) {
        double g;
        double d = this.getX();
        double e = this.getY();
        double f = this.getZ();
        if (this.isSwimming() && !this.hasVehicle()) {
            double h;
            g = this.getRotationVector().y;
            h = g < -0.2 ? 0.085 : 0.06;
            if (g <= 0.0 || this.jumping || !this.getWorld().getBlockState(BlockPos.ofFloored(this.getX(), this.getY() + 1.0 - 0.1, this.getZ())).getFluidState().isEmpty()) {
                Vec3d vec3d = this.getVelocity();
                this.setVelocity(vec3d.add(0.0, (g - vec3d.y) * h, 0.0));
            }
        }
        if (this.abilities.flying && !this.hasVehicle()) {
            g = this.getVelocity().y;
            super.travel(movementInput);
            Vec3d vec3d2 = this.getVelocity();
            this.setVelocity(vec3d2.x, g * 0.6, vec3d2.z);
            this.onLanding();
            this.setFlag(Entity.FALL_FLYING_FLAG_INDEX, false);
        } else {
            super.travel(movementInput);
        }
        this.increaseTravelMotionStats(this.getX() - d, this.getY() - e, this.getZ() - f);
    }

    @Overwrite
    public void updateSwimming() {
        if (this.abilities.flying) {
            this.setSwimming(false);
        } else {
            super.updateSwimming();
        }
    }

    @Overwrite
    public boolean doesNotSuffocate(BlockPos pos) {
        return !this.getWorld().getBlockState(pos).shouldSuffocate(this.getWorld(), pos);
    }

    @Overwrite
    public float getMovementSpeed() {
        return (float)this.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
    }

    public void increaseTravelMotionStats(double dx, double dy, double dz) {
        if (this.hasVehicle()) {
            return;
        }
        double sqrt = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (this.isSwimming()) {
            int i = Math.round((float) sqrt * 100.0f);
            if (i > 0) {
                this.increaseStat(Stats.SWIM_ONE_CM, i);
                this.addExhaustion(0.01f * (float)i * 0.01f);
            }
        } else if (this.isSubmergedIn(FluidTags.WATER)) {
            int i = Math.round((float) sqrt * 100.0f);
            if (i > 0) {
                this.increaseStat(Stats.WALK_UNDER_WATER_ONE_CM, i);
                this.addExhaustion(0.01f * (float)i * 0.01f);
            }
        } else {
            double sqrt1 = Math.sqrt(dx * dx + dz * dz);
            if (this.isTouchingWater()) {
                int i = Math.round((float) sqrt1 * 100.0f);
                if (i > 0) {
                    this.increaseStat(Stats.WALK_ON_WATER_ONE_CM, i);
                    this.addExhaustion(0.01f * (float)i * 0.01f);
                }
            } else if (this.isClimbing()) {
                if (dy > 0.0) {
                    this.increaseStat(Stats.CLIMB_ONE_CM, (int) Math.round(dy * 100));
                }
            } else if (this.isOnGround()) {
                int i = Math.round((float) sqrt1 * 100.0f);
                if (i > 0) {
                    if (this.isSprinting()) {
                        this.increaseStat(Stats.SPRINT_ONE_CM, i);
                        this.addExhaustion(0.1f * (float)i * 0.01f);
                    } else if (this.isInSneakingPose()) {
                        this.increaseStat(Stats.CROUCH_ONE_CM, i);
                        this.addExhaustion(0.0f * (float)i * 0.01f);
                    } else {
                        this.increaseStat(Stats.WALK_ONE_CM, i);
                        this.addExhaustion(0.0f * (float)i * 0.01f);
                    }
                }
            } else if (this.isFallFlying()) {
                int i = Math.round((float) sqrt * 100.0f);
                this.increaseStat(Stats.AVIATE_ONE_CM, i);
            } else {
                int i = Math.round((float) sqrt1 * 100.0f);
                if (i > 25) {
                    this.increaseStat(Stats.FLY_ONE_CM, i);
                }
            }
        }
    }

    public void increaseRidingMotionStats(double dx, double dy, double dz) {
        int i;
        if (this.hasVehicle() && (i = Math.round((float)Math.sqrt(dx * dx + dy * dy + dz * dz) * 100.0f)) > 0) {
            Entity entity = this.getVehicle();
            if (entity instanceof AbstractMinecartEntity) {
                this.increaseStat(Stats.MINECART_ONE_CM, i);
            } else if (entity instanceof BoatEntity) {
                this.increaseStat(Stats.BOAT_ONE_CM, i);
            } else if (entity instanceof PigEntity) {
                this.increaseStat(Stats.PIG_ONE_CM, i);
            } else if (entity instanceof AbstractHorseEntity) {
                this.increaseStat(Stats.HORSE_ONE_CM, i);
            } else if (entity instanceof StriderEntity) {
                this.increaseStat(Stats.STRIDER_ONE_CM, i);
            }
        }
    }

    @Overwrite
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        if (this.abilities.allowFlying) {
            return false;
        }
        if (fallDistance >= 2.0f) {
            this.increaseStat(Stats.FALL_ONE_CM, Math.round(fallDistance * 100));
        }
        return super.handleFallDamage(fallDistance, damageMultiplier, damageSource);
    }

    @Overwrite
    public void startFallFlying() {
        this.setFlag(Entity.FALL_FLYING_FLAG_INDEX, true);
    }

    @Overwrite
    public void stopFallFlying() {
        this.setFlag(Entity.FALL_FLYING_FLAG_INDEX, true);
        this.setFlag(Entity.FALL_FLYING_FLAG_INDEX, false);
    }

    @Overwrite
    public void onSwimmingStart() {
        if (!this.isSpectator()) {
            super.onSwimmingStart();
        }
    }

    @Overwrite
    public void playStepSound(BlockPos pos, BlockState state) {
        if (this.isTouchingWater()) {
            this.playSwimSound();
            this.playSecondaryStepSound(state);
        } else {
            BlockPos blockPos = this.getStepSoundPos(pos);
            if (!pos.equals(blockPos)) {
                BlockState blockState = this.getWorld().getBlockState(blockPos);
                if (blockState.isIn(BlockTags.COMBINATION_STEP_SOUND_BLOCKS)) {
                    this.playCombinationStepSounds(blockState, state);
                } else {
                    super.playStepSound(blockPos, blockState);
                }
            } else {
                super.playStepSound(pos, state);
            }
        }
    }

    @Overwrite
    public LivingEntity.FallSounds getFallSounds() {
        return new LivingEntity.FallSounds(SoundEvents.ENTITY_PLAYER_SMALL_FALL, SoundEvents.ENTITY_PLAYER_BIG_FALL);
    }

    @Overwrite
    public boolean onKilledOther(ServerWorld world, LivingEntity other) {
        this.incrementStat(Stats.KILLED.getOrCreateStat(other.getType()));
        return true;
    }

    @Overwrite
    public void slowMovement(BlockState state, Vec3d multiplier) {
        if (!this.abilities.flying) {
            super.slowMovement(state, multiplier);
        }
    }

    @Overwrite
    public void addExperience(int experience) {
        this.addScore(experience);
        this.experienceProgress += (float)experience / (float)this.getNextLevelExperience();
        this.totalExperience = MathHelper.clamp(this.totalExperience + experience, 0, Integer.MAX_VALUE);
        while (this.experienceProgress < 0.0f) {
            float f = this.experienceProgress * (float)this.getNextLevelExperience();
            if (this.experienceLevel > 0) {
                this.addExperienceLevels(-1);
                this.experienceProgress = 1.0f + f / (float)this.getNextLevelExperience();
                continue;
            }
            this.addExperienceLevels(-1);
            this.experienceProgress = 0.0f;
        }
        while (this.experienceProgress >= 1.0f) {
            this.experienceProgress = (this.experienceProgress - 1.0f) * (float)this.getNextLevelExperience();
            this.addExperienceLevels(1);
            this.experienceProgress /= (float)this.getNextLevelExperience();
        }
    }

    @Overwrite
    public int getEnchantmentTableSeed() {
        return this.enchantmentTableSeed;
    }

    @Overwrite
    public void applyEnchantmentCosts(ItemStack enchantedItem, int experienceLevels) {
        this.experienceLevel -= experienceLevels;
        if (this.experienceLevel < 0) {
            this.experienceLevel = 0;
            this.experienceProgress = 0.0f;
            this.totalExperience = 0;
        }
        this.enchantmentTableSeed = this.random.nextInt();
    }

    @Overwrite
    public void addExperienceLevels(int levels) {
        this.experienceLevel += levels;
        if (this.experienceLevel < 0) {
            this.experienceLevel = 0;
            this.experienceProgress = 0.0f;
            this.totalExperience = 0;
        }
        if (levels > 0 && this.experienceLevel % 5 == 0 && (float)this.lastPlayedLevelUpSoundTime < (float)this.age - 100.0f) {
            float f = this.experienceLevel > 30 ? 1.0f : (float)this.experienceLevel / 30.0f;
            this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_PLAYER_LEVELUP, this.getSoundCategory(), f * 0.75f, 1.0f);
            this.lastPlayedLevelUpSoundTime = this.age;
        }
    }

    @Overwrite
    public int getNextLevelExperience() {
        if (this.experienceLevel >= 30) {
            return 112 + (this.experienceLevel - 30) * 9;
        }
        if (this.experienceLevel >= 15) {
            return 37 + (this.experienceLevel - 15) * 5;
        }
        return 7 + this.experienceLevel * 2;
    }

    @Overwrite
    public void addExhaustion(float exhaustion) {
        if (this.abilities.invulnerable) {
            return;
        }
        if (!this.getWorld().isClient) {
            this.hungerManager.addExhaustion(exhaustion);
        }
    }

    @Overwrite
    public Optional<SculkShriekerWarningManager> getSculkShriekerWarningManager() {
        return Optional.empty();
    }

    @Overwrite
    public HungerManager getHungerManager() {
        return this.hungerManager;
    }

    @Overwrite
    public boolean canConsume(boolean ignoreHunger) {
        return this.abilities.invulnerable || ignoreHunger || this.hungerManager.isNotFull();
    }

    @Overwrite
    public boolean canFoodHeal() {
        return this.getHealth() > 0.0f && this.getHealth() < this.getMaxHealth();
    }

    @Overwrite
    public boolean canModifyBlocks() {
        return this.abilities.allowModifyWorld;
    }

    @Overwrite
    public boolean canPlaceOn(BlockPos pos, Direction facing, ItemStack stack) {
        if (this.abilities.allowModifyWorld) {
            return true;
        }
        BlockPos blockPos = pos.offset(facing.getOpposite());
        CachedBlockPosition cachedBlockPosition = new CachedBlockPosition(this.getWorld(), blockPos, false);
        return stack.canPlaceOn(this.getWorld().getRegistryManager().get(RegistryKeys.BLOCK), cachedBlockPosition);
    }

    @Overwrite
    public int getXpToDrop() {
        if (this.getWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY) || this.isSpectator()) {
            return 0;
        }
        int i = this.experienceLevel * 7;
        return Math.min(i, 100);
    }

    @Overwrite
    public boolean shouldAlwaysDropXp() {
        return true;
    }

    @Overwrite
    public boolean shouldRenderName() {
        return true;
    }

    @Overwrite
    public Entity.MoveEffect getMoveEffect() {
        return !this.abilities.flying && (!this.isOnGround() || !this.isSneaky()) ? Entity.MoveEffect.ALL : Entity.MoveEffect.NONE;
    }

    @Overwrite
    public void sendAbilitiesUpdate() {
    }

    @Overwrite
    public Text getName() {
        return Text.literal(this.gameProfile.getName());
    }

    @Overwrite
    public EnderChestInventory getEnderChestInventory() {
        return this.enderChestInventory;
    }

    @Overwrite
    public ItemStack getEquippedStack(EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND) {
            return this.inventory.getMainHandStack();
        }
        if (slot == EquipmentSlot.OFFHAND) {
            return this.inventory.offHand.get(0);
        }
        if (slot.getType() == EquipmentSlot.Type.ARMOR) {
            return this.inventory.armor.get(slot.getEntitySlotId());
        }
        return ItemStack.EMPTY;
    }

    @Overwrite
    public boolean isArmorSlot(EquipmentSlot slot) {
        return slot.getType() == EquipmentSlot.Type.ARMOR;
    }

    @Overwrite
    public void equipStack(EquipmentSlot slot, ItemStack stack) {
        this.processEquippedStack(stack);
        if (slot == EquipmentSlot.MAINHAND) {
            this.onEquipStack(slot, this.inventory.main.set(this.inventory.selectedSlot, stack), stack);
        } else if (slot == EquipmentSlot.OFFHAND) {
            this.onEquipStack(slot, this.inventory.offHand.set(0, stack), stack);
        } else if (slot.getType() == EquipmentSlot.Type.ARMOR) {
            this.onEquipStack(slot, this.inventory.armor.set(slot.getEntitySlotId(), stack), stack);
        }
    }

    @Overwrite
    public boolean giveItemStack(ItemStack stack) {
        return this.inventory.insertStack(stack);
    }

    @Overwrite
    public Iterable<ItemStack> getHandItems() {
        return Lists.newArrayList(this.getMainHandStack(), this.getOffHandStack());
    }

    @Overwrite
    public Iterable<ItemStack> getArmorItems() {
        return this.inventory.armor;
    }

    @Overwrite
    public boolean addShoulderEntity(NbtCompound entityNbt) {
        if (this.hasVehicle() || !this.isOnGround() || this.isTouchingWater() || this.inPowderSnow) {
            return false;
        }
        if (this.getShoulderEntityLeft().isEmpty()) {
            this.setShoulderEntityLeft(entityNbt);
            this.shoulderEntityAddedTime = this.getWorld().getTime();
            return true;
        }
        if (this.getShoulderEntityRight().isEmpty()) {
            this.setShoulderEntityRight(entityNbt);
            this.shoulderEntityAddedTime = this.getWorld().getTime();
            return true;
        }
        return false;
    }

    @Overwrite
    public void dropShoulderEntities() {
        if (this.shoulderEntityAddedTime + 20L < this.getWorld().getTime()) {
            this.dropShoulderEntity(this.getShoulderEntityLeft());
            this.setShoulderEntityLeft(new NbtCompound());
            this.dropShoulderEntity(this.getShoulderEntityRight());
            this.setShoulderEntityRight(new NbtCompound());
        }
    }

    @Overwrite
    public void dropShoulderEntity(NbtCompound entityNbt) {
        if (!this.getWorld().isClient && !entityNbt.isEmpty()) {
            EntityType.getEntityFromNbt(entityNbt, this.getWorld()).ifPresent(entity -> {
                if (entity instanceof TameableEntity) {
                    ((TameableEntity)entity).setOwnerUuid(this.uuid);
                }
                entity.setPosition(this.getX(), this.getY() + (double)0.7f, this.getZ());
                ((ServerWorld)this.getWorld()).tryLoadEntity(entity);
            });
        }
    }

    @Overwrite
    public abstract boolean isSpectator();

    @Overwrite
    public boolean canBeHitByProjectile() {
        return !this.isSpectator() && super.canBeHitByProjectile();
    }

    @Overwrite
    public boolean isSwimming() {
        return !this.abilities.flying && !this.isSpectator() && super.isSwimming();
    }

    @Overwrite
    public abstract boolean isCreative();

    @Overwrite
    public boolean isPushedByFluids() {
        return !this.abilities.flying;
    }

    @Overwrite
    public Scoreboard getScoreboard() {
        return this.getWorld().getScoreboard();
    }

    @Overwrite
    public Text getDisplayName() {
        MutableText mutableText = Team.decorateName(this.getScoreboardTeam(), this.getName());
        return this.addTellClickEvent(mutableText);
    }

    @Overwrite
    public MutableText addTellClickEvent(MutableText component) {
        String string = this.getGameProfile().getName();
        return component.styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tell " + string + " ")).withHoverEvent(this.getHoverEvent()).withInsertion(string));
    }

    @Overwrite
    public float getActiveEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return switch (pose) {
            case SWIMMING, FALL_FLYING, SPIN_ATTACK -> 0.4f;
            case CROUCHING -> 1.27f;
            default -> 1.62f;
        };
    }

    @Overwrite
    public void setAbsorptionAmountUnclamped(float absorptionAmount) {
        this.getDataTracker().set(ABSORPTION_AMOUNT, absorptionAmount);
    }

    @Overwrite
    public float getAbsorptionAmount() {
        return this.getDataTracker().get(ABSORPTION_AMOUNT);
    }

    @Overwrite
    public boolean isPartVisible(PlayerModelPart modelPart) {
        return (this.getDataTracker().get(PLAYER_MODEL_PARTS) & modelPart.getBitFlag()) == modelPart.getBitFlag();
    }

    @Overwrite
    public StackReference getStackReference(int mappedIndex) {
        if (mappedIndex >= 0 && mappedIndex < this.inventory.main.size()) {
            return StackReference.of(this.inventory, mappedIndex);
        }
        int i = mappedIndex - 200;
        if (i >= 0 && i < this.enderChestInventory.size()) {
            return StackReference.of(this.enderChestInventory, i);
        }
        return super.getStackReference(mappedIndex);
    }

    @Overwrite
    public boolean hasReducedDebugInfo() {
        return this.reducedDebugInfo;
    }

    @Overwrite
    public void setReducedDebugInfo(boolean reducedDebugInfo) {
        this.reducedDebugInfo = reducedDebugInfo;
    }

    @Overwrite
    public void setFireTicks(int fireTicks) {
        super.setFireTicks(this.abilities.invulnerable ? Math.min(fireTicks, 1) : fireTicks);
    }

    @Overwrite
    public Arm getMainArm() {
        return this.dataTracker.get(MAIN_ARM) == 0 ? Arm.LEFT : Arm.RIGHT;
    }

    @Overwrite
    public void setMainArm(Arm arm) {
        this.dataTracker.set(MAIN_ARM, (byte)(arm != Arm.LEFT ? 1 : 0));
    }

    @Overwrite
    public NbtCompound getShoulderEntityLeft() {
        return this.dataTracker.get(LEFT_SHOULDER_ENTITY);
    }

    @Overwrite
    public void setShoulderEntityLeft(NbtCompound entityNbt) {
        this.dataTracker.set(LEFT_SHOULDER_ENTITY, entityNbt);
    }
    @Overwrite
    public NbtCompound getShoulderEntityRight() {
        return this.dataTracker.get(RIGHT_SHOULDER_ENTITY);
    }

    @Overwrite
    public void setShoulderEntityRight(NbtCompound entityNbt) {
        this.dataTracker.set(RIGHT_SHOULDER_ENTITY, entityNbt);
    }

    @Overwrite
    public float getAttackCooldownProgressPerTick() {
        return (float)(1.0 / this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED) * 20.0);
    }

    @Overwrite
    public float getAttackCooldownProgress(float baseTime) {
        return MathHelper.clamp(((float)this.lastAttackedTicks + baseTime) / this.getAttackCooldownProgressPerTick(), 0.0f, 1.0f);
    }

    @Overwrite
    public void resetLastAttackedTicks() {
        this.lastAttackedTicks = 0;
    }

    @Overwrite
    public ItemCooldownManager getItemCooldownManager() {
        return this.itemCooldownManager;
    }

    @Overwrite
    public float getVelocityMultiplier() {
        return this.abilities.flying || this.isFallFlying() ? 1.0f : super.getVelocityMultiplier();
    }

    @Overwrite
    public float getLuck() {
        return (float)this.getAttributeValue(EntityAttributes.GENERIC_LUCK);
    }

    @Overwrite
    public boolean isCreativeLevelTwoOp() {
        return this.abilities.creativeMode && this.getPermissionLevel() >= 2;
    }

    @Overwrite
    public boolean canEquip(ItemStack stack) {
        EquipmentSlot equipmentSlot = MobEntity.getPreferredEquipmentSlot(stack);
        return this.getEquippedStack(equipmentSlot).isEmpty();
    }

    @Overwrite
    public EntityDimensions getDimensions(EntityPose pose) {
        return (EntityDimensions) POSE_DIMENSIONS.getOrDefault(pose, STANDING_DIMENSIONS);
    }

    @Overwrite
    public ImmutableList<EntityPose> getPoses() {
        return ImmutableList.of(EntityPose.STANDING, EntityPose.CROUCHING, EntityPose.SWIMMING);
    }

    @Overwrite
    public ItemStack getProjectileType(ItemStack stack) {
        if (!(stack.getItem() instanceof RangedWeaponItem)) {
            return ItemStack.EMPTY;
        }
        Predicate<ItemStack> predicate = ((RangedWeaponItem)stack.getItem()).getHeldProjectiles();
        ItemStack itemStack = RangedWeaponItem.getHeldProjectile(this, predicate);
        if (!itemStack.isEmpty()) {
            return itemStack;
        }
        predicate = ((RangedWeaponItem)stack.getItem()).getProjectiles();
        for (int i = 0; i < this.inventory.size(); ++i) {
            ItemStack itemStack2 = this.inventory.getStack(i);
            if (!predicate.test(itemStack2)) continue;
            return itemStack2;
        }
        return this.abilities.creativeMode ? new ItemStack(Items.ARROW) : ItemStack.EMPTY;
    }

    @Overwrite
    public ItemStack eatFood(World world, ItemStack stack) {
        this.getHungerManager().eat(stack.getItem(), stack);
        this.incrementStat(Stats.USED.getOrCreateStat(stack.getItem()));
        world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 0.5f, world.random.nextFloat() * 0.1f + 0.9f);
        if (p instanceof ServerPlayerEntity) {
            Criteria.CONSUME_ITEM.trigger((ServerPlayerEntity)p, stack);
        }
        return super.eatFood(world, stack);
    }

    @Overwrite
    public boolean shouldRemoveSoulSpeedBoost(BlockState landingState) {
        return this.abilities.flying || super.shouldRemoveSoulSpeedBoost(landingState);
    }

    @Overwrite
    public Vec3d getLeashPos(float delta) {
        double d = 0.22 * (this.getMainArm() == Arm.RIGHT ? -1.0 : 1.0);
        float f = MathHelper.lerp(delta * 0.5f, this.getPitch(), this.prevPitch) * ((float)Math.PI / 180);
        float g = MathHelper.lerp(delta, this.prevBodyYaw, this.bodyYaw) * ((float)Math.PI / 180);
        if (this.isFallFlying() || this.isUsingRiptide()) {
            float k;
            Vec3d vec3d = this.getRotationVec(delta);
            Vec3d vec3d2 = this.getVelocity();
            double e = vec3d2.horizontalLengthSquared();
            double h = vec3d.horizontalLengthSquared();
            if (e > 0.0 && h > 0.0) {
                double i = (vec3d2.x * vec3d.x + vec3d2.z * vec3d.z) / Math.sqrt(e * h);
                double j = vec3d2.x * vec3d.z - vec3d2.z * vec3d.x;
                k = (float)(Math.signum(j) * Math.acos(i));
            } else {
                k = 0.0f;
            }
            return this.getLerpedPos(delta).add(new Vec3d(d, -0.11, 0.85).rotateZ(-k).rotateX(-f).rotateY(-g));
        }
        if (this.isInSwimmingPose()) {
            return this.getLerpedPos(delta).add(new Vec3d(d, 0.2, -0.15).rotateX(-f).rotateY(-g));
        }
        double l = this.getBoundingBox().getLengthY() - 1.0;
        double e = this.isInSneakingPose() ? -0.2 : 0.07;
        return this.getLerpedPos(delta).add(new Vec3d(d, l, e).rotateY(-g));
    }

    @Overwrite
    public boolean isPlayer() {
        return true;
    }

    @Overwrite
    public boolean isUsingSpyglass() {
        return this.isUsingItem() && this.getActiveItem().isOf(Items.SPYGLASS);
    }

    @Overwrite
    public boolean shouldSave() {
        return false;
    }

    @Overwrite
    public Optional<GlobalPos> getLastDeathPos() {
        return this.lastDeathPos;
    }

    @Overwrite
    public void setLastDeathPos(Optional<GlobalPos> lastDeathPos) {
        this.lastDeathPos = lastDeathPos;
    }

    @Overwrite
    public float getDamageTiltYaw() {
        return this.damageTiltYaw;
    }

    @Overwrite
    public void animateDamage(float yaw) {
        super.animateDamage(yaw);
        this.damageTiltYaw = yaw;
    }

    @Overwrite
    public boolean canSprintAsVehicle() {
        return true;
    }

    @Overwrite
    public float getOffGroundSpeed() {
        if (this.abilities.flying && !this.hasVehicle()) {
            return this.isSprinting() ? this.abilities.getFlySpeed() * 2.0f : this.abilities.getFlySpeed();
        }
        return this.isSprinting() ? 0.025999999f : 0.02f;
    }
}