package net.ua.mixin.entity.animal;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.LivingTargetCache;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.sensor.GolemLastSeenSensor;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.DebugInfoSender;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.village.*;
import net.minecraft.world.*;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraft.world.poi.PointOfInterestTypes;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Mixin(VillagerEntity.class)
public class vi extends PathAwareEntity implements Monster {
    @Final
    @Shadow
    private static final Logger LOGGER = LogUtils.getLogger();
    @Shadow
    @Final
    private static final TrackedData<VillagerData> VILLAGER_DATA = DataTracker.registerData(vi.class, TrackedDataHandlerRegistry.VILLAGER_DATA);
    @Shadow
    @Final
    public static final Map<Item, Integer> ITEM_FOOD_VALUES = ImmutableMap.of(Items.BREAD, 4, Items.POTATO, 1, Items.CARROT, 1, Items.BEETROOT, 1);
    @Shadow
    @Final
    private static final Set<Item> GATHERABLE_ITEMS = ImmutableSet.of(Items.BREAD, Items.POTATO, Items.CARROT, Items.WHEAT, Items.WHEAT_SEEDS, Items.BEETROOT, Items.BEETROOT_SEEDS, Items.TORCHFLOWER_SEEDS, Items.PITCHER_POD);
    @Shadow
    private int levelUpTimer;
    @Shadow
    private boolean levelingUp;
    @Nullable
    @Shadow
    private PlayerEntity lastCustomer;
    @Shadow
    private boolean field_30612;
    @Shadow
    private int foodLevel;
    @Final
    @Shadow
    private final VillagerGossips gossip = new VillagerGossips();
    @Shadow
    private long gossipStartTime;
    @Shadow
    private long lastGossipDecayTime;
    @Shadow
    private int experience;
    @Shadow
    private long lastRestockTime;
    @Shadow
    private int restocksToday;
    @Shadow
    private long lastRestockCheckTime;
    @Shadow
    private boolean natural;
    @Shadow
    @Final
    private static final ImmutableList<MemoryModuleType<?>> MEMORY_MODULES = ImmutableList.of(MemoryModuleType.HOME, MemoryModuleType.JOB_SITE, MemoryModuleType.POTENTIAL_JOB_SITE, MemoryModuleType.MEETING_POINT, MemoryModuleType.MOBS, MemoryModuleType.VISIBLE_MOBS, MemoryModuleType.VISIBLE_VILLAGER_BABIES, MemoryModuleType.NEAREST_PLAYERS, MemoryModuleType.NEAREST_VISIBLE_PLAYER, MemoryModuleType.NEAREST_VISIBLE_TARGETABLE_PLAYER, MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM, MemoryModuleType.ITEM_PICKUP_COOLDOWN_TICKS, new MemoryModuleType[]{MemoryModuleType.WALK_TARGET, MemoryModuleType.LOOK_TARGET, MemoryModuleType.INTERACTION_TARGET, MemoryModuleType.BREED_TARGET, MemoryModuleType.PATH, MemoryModuleType.DOORS_TO_CLOSE, MemoryModuleType.NEAREST_BED, MemoryModuleType.HURT_BY, MemoryModuleType.HURT_BY_ENTITY, MemoryModuleType.NEAREST_HOSTILE, MemoryModuleType.SECONDARY_JOB_SITE, MemoryModuleType.HIDING_PLACE, MemoryModuleType.HEARD_BELL_TIME, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryModuleType.LAST_SLEPT, MemoryModuleType.LAST_WOKEN, MemoryModuleType.LAST_WORKED_AT_POI, MemoryModuleType.GOLEM_DETECTED_RECENTLY});
    @Shadow
    @Final
    private static final ImmutableList<SensorType<? extends Sensor<? super VillagerEntity>>> SENSORS = ImmutableList.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS, SensorType.NEAREST_ITEMS, SensorType.NEAREST_BED, SensorType.HURT_BY, SensorType.VILLAGER_HOSTILES, SensorType.VILLAGER_BABIES, SensorType.SECONDARY_POIS, SensorType.GOLEM_DETECTED);
    @Final
    @Shadow
    public static final Map<MemoryModuleType<GlobalPos>, BiPredicate<VillagerEntity, RegistryEntry<PointOfInterestType>>> POINTS_OF_INTEREST = ImmutableMap.of(MemoryModuleType.HOME, (villager, registryEntry) -> registryEntry.matchesKey(PointOfInterestTypes.HOME), MemoryModuleType.JOB_SITE, (villager, registryEntry) -> villager.getVillagerData().getProfession().heldWorkstation().test((RegistryEntry<PointOfInterestType>)registryEntry), MemoryModuleType.POTENTIAL_JOB_SITE, (villager, registryEntry) -> VillagerProfession.IS_ACQUIRABLE_JOB_SITE.test((RegistryEntry<PointOfInterestType>)registryEntry), MemoryModuleType.MEETING_POINT, (villager, registryEntry) -> registryEntry.matchesKey(PointOfInterestTypes.MEETING));

    public vi(EntityType<? extends vi> entityType, World world) {
        this(entityType, world, VillagerType.PLAINS);
    }

    public vi(EntityType<? extends vi> entityType, World world, VillagerType type) {
        super(entityType, world);
        ((MobNavigation)this.getNavigation()).setCanPathThroughDoors(true);
        this.getNavigation().setCanSwim(true);
        this.setCanPickUpLoot(true);
        this.setVillagerData(this.getVillagerData().withType(type).withProfession(VillagerProfession.NONE));
    }

    @Overwrite
    public Brain<VillagerEntity> getBrain() {
        return (Brain<VillagerEntity>) super.getBrain();
    }

    @Overwrite
    public Brain.Profile<VillagerEntity> createBrainProfile() {
        return Brain.createProfile(MEMORY_MODULES, SENSORS);
    }

    @Overwrite
    public Brain<?> deserializeBrain(Dynamic<?> dynamic) {
        Brain<VillagerEntity> brain = this.createBrainProfile().deserialize(dynamic);
        this.initBrain(brain);
        return brain;
    }

    @Overwrite
    public void reinitializeBrain(ServerWorld world) {
        Brain<VillagerEntity> brain = this.getBrain();
        brain.stopAllTasks(world, (VillagerEntity) (Object) this);
        this.brain = brain.copy();
        this.initBrain(this.getBrain());
    }

    @Overwrite
    private void initBrain(Brain<VillagerEntity> brain) {

    }

    @Overwrite
    public void onGrowUp() {
    }
    @Overwrite
    public static DefaultAttributeContainer.Builder createVillagerAttributes() {
        return HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.4).add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 10)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 2147483647).add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0);
    }
    @Override
    public void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 1.0, 0.0f));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 1.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.targetSelector.add(2, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true, false));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, SkeletonEntity.class, true));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, WitherSkeletonEntity.class, true));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, StrayEntity.class, true));
    }

    @Overwrite
    public boolean isNatural() {
        return this.natural;
    }
    @Override
    public boolean tryAttack(Entity target) {
        return super.tryAttack(target);
    }
    @Override
    public SoundCategory getSoundCategory() {
        return SoundCategory.HOSTILE;
    }
    @Override
    public void tickMovement() {
        this.tickHandSwing();
        this.updateDespawnCounter();
        super.tickMovement();
    }
    @Unique
    public void updateDespawnCounter() {
        float f = this.getBrightnessAtEyes();
        if (f > 0.5f) {
            this.despawnCounter += 2;
        }
    }
    @Override
    public boolean isDisallowedInPeaceful() {
        return true;
    }
    @Override
    public SoundEvent getSwimSound() {
        return SoundEvents.ENTITY_HOSTILE_SWIM;
    }
    @Override
    public SoundEvent getSplashSound() {
        return SoundEvents.ENTITY_HOSTILE_SPLASH;
    }
    @Override
    public LivingEntity.FallSounds getFallSounds() {
        return new LivingEntity.FallSounds(SoundEvents.ENTITY_HOSTILE_SMALL_FALL, SoundEvents.ENTITY_HOSTILE_BIG_FALL);
    }
    @Override
    public float getPathfindingFavor(BlockPos pos, WorldView world) {
        return -world.getPhototaxisFavor(pos);
    }
    @Override
    public ItemStack getProjectileType(ItemStack stack) {
        if (stack.getItem() instanceof RangedWeaponItem) {
            Predicate<ItemStack> predicate = ((RangedWeaponItem)stack.getItem()).getHeldProjectiles();
            ItemStack itemStack = RangedWeaponItem.getHeldProjectile(this, predicate);
            return itemStack.isEmpty() ? new ItemStack(Items.ARROW) : itemStack;
        }
        return ItemStack.EMPTY;
    }

    @Overwrite
    public void mobTick() {
        this.getWorld().getProfiler().push("villagerBrain");
        this.getBrain().tick((ServerWorld)this.getWorld(), (VillagerEntity) (Object) this);
        this.getWorld().getProfiler().pop();
        if (this.natural) {
            this.natural = false;
        }
        super.mobTick();
    }

    @Overwrite
    public void tick() {
        super.tick();
    }

    @Overwrite
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        return super.interactMob(player, hand);
    }

    @Overwrite
    private void sayNo() {
    }

    @Overwrite
    private void beginTradeWith(PlayerEntity customer) {
    }

    @Overwrite
    public void setCustomer(@Nullable PlayerEntity customer) {
    }

    @Overwrite
    public void resetCustomer() {
    }

    @Overwrite
    private void clearSpecialPrices() {
    }

    @Overwrite
    public boolean canRefreshTrades() {
        return true;
    }

    @Overwrite
    public boolean isClient() {
        return this.getWorld().isClient;
    }

    @Overwrite
    public void restock() {
    }

    @Overwrite
    private void sendOffersToCustomer() {
    }

    @Overwrite
    private boolean needsRestock() {
        return false;
    }

    @Overwrite
    private boolean canRestock() {
        return true;
    }

    @Overwrite
    public boolean shouldRestock() {
        return false;
    }

    @Overwrite
    private void restockAndUpdateDemandBonus() {
    }

    @Overwrite
    private void updateDemandBonus() {
    }

    @Overwrite
    private void prepareOffersFor(PlayerEntity player) {
    }

    @Overwrite
    public void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(VILLAGER_DATA, new VillagerData(VillagerType.PLAINS, VillagerProfession.NONE, 1));
    }

    @Overwrite
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        VillagerData.CODEC.encodeStart(NbtOps.INSTANCE, this.getVillagerData()).resultOrPartial(LOGGER::error).ifPresent(nbtElement -> nbt.put("VillagerData", (NbtElement)nbtElement));
        nbt.putByte("FoodLevel", (byte)this.foodLevel);
        nbt.put("Gossips", this.gossip.serialize(NbtOps.INSTANCE));
        nbt.putInt("Xp", this.experience);
        nbt.putLong("LastRestock", this.lastRestockTime);
        nbt.putLong("LastGossipDecay", this.lastGossipDecayTime);
        nbt.putInt("RestocksToday", this.restocksToday);
        if (this.natural) {
            nbt.putBoolean("AssignProfessionWhenSpawned", true);
        }
    }

    @Overwrite
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.setCanPickUpLoot(true);
        if (this.getWorld() instanceof ServerWorld) {
            this.reinitializeBrain((ServerWorld)this.getWorld());
        }
    }

    @Overwrite
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    @Overwrite
    @Nullable
    public SoundEvent getAmbientSound() {
        if (this.isSleeping()) {
            return null;
        }
        return SoundEvents.ENTITY_VILLAGER_AMBIENT;
    }

    @Overwrite
    public SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_VILLAGER_HURT;
    }

    @Overwrite
    public SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_VILLAGER_DEATH;
    }

    @Overwrite
    public void playWorkSound() {
        SoundEvent soundEvent = this.getVillagerData().getProfession().workSound();
        if (soundEvent != null) {
            this.playSound(soundEvent, this.getSoundVolume(), this.getSoundPitch());
        }
    }

    @Overwrite
    public void setVillagerData(VillagerData villagerData) {
        this.dataTracker.set(VILLAGER_DATA, villagerData);
    }

    @Overwrite
    public VillagerData getVillagerData() {
        return this.dataTracker.get(VILLAGER_DATA);
    }

    @Overwrite
    public void afterUsing(TradeOffer offer) {
    }

    @Overwrite
    public void method_35201(boolean bl) {
        this.field_30612 = bl;
    }

    @Overwrite
    public boolean method_35200() {
        return this.field_30612;
    }

    @Overwrite
    public void setAttacker(@Nullable LivingEntity attacker) {
        if (attacker != null && this.getWorld() instanceof ServerWorld) {
            ((ServerWorld)this.getWorld()).handleInteraction(EntityInteraction.VILLAGER_HURT, attacker, (VillagerEntity) (Object) this);
            if (this.isAlive() && attacker instanceof PlayerEntity) {
                this.getWorld().sendEntityStatus(this, EntityStatuses.ADD_VILLAGER_ANGRY_PARTICLES);
            }
        }
        super.setAttacker(attacker);
    }

    @Overwrite
    public void onDeath(DamageSource damageSource) {
        LOGGER.info("Villager {} died, message: '{}'", this, damageSource.getDeathMessage(this).getString());
        Entity entity = damageSource.getAttacker();
        if (entity != null) {
            this.notifyDeath(entity);
        }
        this.releaseAllTickets();
        super.onDeath(damageSource);
    }

    @Overwrite
    private void releaseAllTickets() {
    }

    @Overwrite
    private void notifyDeath(Entity killer) {
        World world = this.getWorld();
        if (!(world instanceof ServerWorld)) {
            return;
        }
        ServerWorld serverWorld = (ServerWorld)world;
        Optional<LivingTargetCache> optional = this.brain.getOptionalRegisteredMemory(MemoryModuleType.VISIBLE_MOBS);
        if (optional.isEmpty()) {
            return;
        }
        optional.get().iterate(InteractionObserver.class::isInstance).forEach(observer -> serverWorld.handleInteraction(EntityInteraction.VILLAGER_KILLED, killer, (InteractionObserver)((Object)observer)));
    }

    @Overwrite
    public void releaseTicketFor(MemoryModuleType<GlobalPos> pos) {
        if (!(this.getWorld() instanceof ServerWorld)) {
            return;
        }
        MinecraftServer minecraftServer = ((ServerWorld)this.getWorld()).getServer();
        this.brain.getOptionalRegisteredMemory(pos).ifPresent(posx -> {
            ServerWorld serverWorld = minecraftServer.getWorld(posx.getDimension());
            if (serverWorld == null) {
                return;
            }
            PointOfInterestStorage pointOfInterestStorage = serverWorld.getPointOfInterestStorage();
            Optional<RegistryEntry<PointOfInterestType>> optional = pointOfInterestStorage.getType(posx.getPos());
            BiPredicate<VillagerEntity, RegistryEntry<PointOfInterestType>> biPredicate = POINTS_OF_INTEREST.get(pos);
            if (optional.isPresent() && biPredicate.test((VillagerEntity) (Object) this, optional.get())) {
                pointOfInterestStorage.releaseTicket(posx.getPos());
                DebugInfoSender.sendPointOfInterest(serverWorld, posx.getPos());
            }
        });
    }

    @Overwrite
    public boolean isReadyToBreed() {
        return false;
    }

    @Overwrite
    private boolean lacksFood() {
        return this.foodLevel < 12;
    }

    @Overwrite
    private void consumeAvailableFood() {
    }

    @Overwrite
    public int getReputation(PlayerEntity player) {
        return this.gossip.getReputationFor(player.getUuid(), gossipType -> true);
    }

    @Overwrite
    private void depleteFood(int amount) {
        this.foodLevel -= amount;
    }

    @Overwrite
    public void eatForBreeding() {
    }

    @Overwrite
    public void setOffers(TradeOfferList offers) {
    }

    @Overwrite
    private boolean canLevelUp() {
        return false;
    }

    @Overwrite
    private void levelUp() {
    }

    @Overwrite
    public Text getDefaultName() {
        return Text.translatable(this.getType().getTranslationKey() + "." + Registries.VILLAGER_PROFESSION.getId(this.getVillagerData().getProfession()).getPath());
    }

    @Overwrite
    public void handleStatus(byte status) {
    }

    @Overwrite
    @Nullable
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
    }

    @Overwrite
    @Nullable
    public VillagerEntity createChild(ServerWorld serverWorld, PassiveEntity passiveEntity) {
        return null;
    }

    @Override
    public void onStruckByLightning(ServerWorld world, LightningEntity lightning) {
        if (world.getDifficulty() != Difficulty.PEACEFUL) {
            LOGGER.info("Villager {} was struck by lightning {}.", this, lightning);
            WitchEntity witchEntity = EntityType.WITCH.create(world);
            if (witchEntity != null) {
                witchEntity.refreshPositionAndAngles(this.getX(), this.getY(), this.getZ(), this.getYaw(), this.getPitch());
                witchEntity.initialize(world, world.getLocalDifficulty(witchEntity.getBlockPos()), SpawnReason.CONVERSION, null, null);
                witchEntity.setAiDisabled(this.isAiDisabled());
                if (this.hasCustomName()) {
                    witchEntity.setCustomName(this.getCustomName());
                    witchEntity.setCustomNameVisible(this.isCustomNameVisible());
                }
                witchEntity.setPersistent();
                world.spawnEntityAndPassengers(witchEntity);
                this.releaseAllTickets();
                this.discard();
            } else {
                super.onStruckByLightning(world, lightning);
            }
        } else {
            super.onStruckByLightning(world, lightning);
        }
    }

    @Overwrite
    public void loot(ItemEntity item) {
        InventoryOwner.pickUpItem(this, (VillagerEntity) (Object) this, item);
    }

    @Overwrite
    public boolean canGather(ItemStack stack) {
        return false;
    }

    @Overwrite
    public boolean wantsToStartBreeding() {
        return this.getAvailableFood() >= 24;
    }

    @Overwrite
    public boolean canBreed() {
        return false;
    }

    @Overwrite
    private int getAvailableFood() {
        return 0;
    }

    @Overwrite
    public boolean hasSeedToPlant() {
        return false;
    }

    @Overwrite
    public void fillRecipes() {
    }

    @Overwrite
    public void talkWithVillager(ServerWorld world, VillagerEntity villager, long time) {
    }

    @Overwrite
    private void decayGossip() {
    }

    @Overwrite
    public void summonGolem(ServerWorld world, long time, int requiredCount) {
        if (!this.canSummonGolem(time)) {
            return;
        }
        Box box = this.getBoundingBox().expand(10.0, 10.0, 10.0);
        List<VillagerEntity> list = world.getNonSpectatingEntities(VillagerEntity.class, box);
        List list2 = list.stream().filter(villager -> villager.canSummonGolem(time)).limit(5L).collect(Collectors.toList());
        if (list2.size() < requiredCount) {
            return;
        }
        if (LargeEntitySpawnHelper.trySpawnAt(EntityType.IRON_GOLEM, SpawnReason.MOB_SUMMONED, world, this.getBlockPos(), 10, 8, 6, LargeEntitySpawnHelper.Requirements.IRON_GOLEM).isEmpty()) {
            return;
        }
        list.forEach(GolemLastSeenSensor::rememberIronGolem);
    }

    @Overwrite
    public boolean canSummonGolem(long time) {
        if (!this.hasRecentlySlept(this.getWorld().getTime())) {
            return false;
        }
        return !this.brain.hasMemoryModule(MemoryModuleType.GOLEM_DETECTED_RECENTLY);
    }

    @Overwrite
    public void onInteractionWith(EntityInteraction interaction, Entity entity) {
    }

    @Overwrite
    public int getExperience() {
        return this.experience;
    }

    @Overwrite
    public void setExperience(int experience) {
        this.experience = experience;
    }

    @Overwrite
    private void clearDailyRestockCount() {
    }

    @Overwrite
    public VillagerGossips getGossip() {
        return this.gossip;
    }

    @Overwrite
    public void readGossipDataNbt(NbtElement nbt) {
    }

    @Overwrite
    public void sendAiDebugData() {
        super.sendAiDebugData();
        DebugInfoSender.sendBrainDebugData(this);
    }

    @Overwrite
    public void sleep(BlockPos pos) {
        super.sleep(pos);
        this.brain.remember(MemoryModuleType.LAST_SLEPT, this.getWorld().getTime());
        this.brain.forget(MemoryModuleType.WALK_TARGET);
        this.brain.forget(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
    }

    @Overwrite
    public void wakeUp() {
        super.wakeUp();
        this.brain.remember(MemoryModuleType.LAST_WOKEN, this.getWorld().getTime());
    }

    @Overwrite
    private boolean hasRecentlySlept(long worldTime) {
        Optional<Long> optional = this.brain.getOptionalRegisteredMemory(MemoryModuleType.LAST_SLEPT);
        return optional.filter(aLong -> worldTime - aLong < 24000L).isPresent();
    }
}