package net.ua.mixin.entity.mob;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.WardenAngerManager;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.SonicBoomTask;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.DebugInfoSender;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.event.Vibrations;
import net.minecraft.world.event.listener.EntityGameEventHandler;
import net.minecraft.world.explosion.Explosion;
import net.ua.ips.wai;
import net.ua.mixin2.inner.otherwise.T1;
import net.ua.mixin2.inner.otherwise.VibrationCallback;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.*;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

@Mixin(WardenEntity.class)
public class wa extends HostileEntity implements Vibrations, wai {
    @Final
    @Shadow
    private static final Logger LOGGER = LogUtils.getLogger();
    @Final
    @Shadow
    private static final int MAX_HEALTH = 500;
    @Final
    @Shadow
    private static final float MOVEMENT_SPEED = 0.3F;
    @Final
    @Shadow
    private static final float KNOCKBACK_RESISTANCE = 1.0F;
    @Final
    @Shadow
    private static final float ATTACK_KNOCKBACK = 1.5F;
    @Final
    @Shadow
    private static final int ATTACK_DAMAGE = 30;
    @Mutable
    @Final
    @Shadow
    private static final TrackedData<Integer> ANGER;
    @Final
    @Shadow
    private static final int DARKNESS_EFFECT_DURATION = 260;
    @Final
    @Shadow
    private static final int ANGRINESS_AMOUNT = 35;
    @Shadow
    private int tendrilPitch;
    @Shadow
    private int lastTendrilPitch;
    @Shadow
    private int heartbeatCooldown;
    @Shadow
    private int lastHeartbeatCooldown;
    @Shadow
    public AnimationState roaringAnimationState = new AnimationState();
    @Shadow
    public AnimationState sniffingAnimationState = new AnimationState();
    @Shadow
    public AnimationState emergingAnimationState = new AnimationState();
    @Shadow
    public AnimationState diggingAnimationState = new AnimationState();
    @Shadow
    public AnimationState attackingAnimationState = new AnimationState();
    @Shadow
    public AnimationState chargingSonicBoomAnimationState = new AnimationState();
    @Final
    @Shadow
    private final EntityGameEventHandler<Vibrations.VibrationListener> gameEventHandler = new EntityGameEventHandler<>(new Vibrations.VibrationListener(this));
    @Final
    @Shadow
    private final Vibrations.Callback vibrationCallback = new VibrationCallback((WardenEntity) (Object) this);
    @Shadow
    private Vibrations.ListenerData vibrationListenerData = new Vibrations.ListenerData();
    @Shadow
    WardenAngerManager angerManager = new WardenAngerManager(this::isValidTarget, Collections.emptyList());

    public wa(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 5;
        this.getNavigation().setCanSwim(true);
        this.setPathfindingPenalty(PathNodeType.UNPASSABLE_RAIL, 0.0F);
        this.setPathfindingPenalty(PathNodeType.DAMAGE_OTHER, 8.0F);
        this.setPathfindingPenalty(PathNodeType.POWDER_SNOW, 8.0F);
        this.setPathfindingPenalty(PathNodeType.LAVA, 8.0F);
        this.setPathfindingPenalty(PathNodeType.DAMAGE_FIRE, 0.0F);
        this.setPathfindingPenalty(PathNodeType.DANGER_FIRE, 0.0F);
    }

    @Overwrite
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        return new EntitySpawnS2CPacket(this, this.isInPose(EntityPose.EMERGING) ? 1 : 0);
    }

    @Overwrite
    public void onSpawnPacket(EntitySpawnS2CPacket packet) {
        super.onSpawnPacket(packet);
        if (packet.getEntityData() == 1) {
            this.setPose(EntityPose.EMERGING);
        }

    }

    @Overwrite
    public boolean canSpawn(WorldView world) {
        return super.canSpawn(world) && world.isSpaceEmpty(this, this.getType().getDimensions().getBoxAt(this.getPos()));
    }

    @Overwrite
    public float getPathfindingFavor(BlockPos pos, WorldView world) {
        return 0.0F;
    }

    @Overwrite
    public boolean isInvulnerableTo(DamageSource damageSource) {
        return this.isDiggingOrEmerging() && !damageSource.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY) || super.isInvulnerableTo(damageSource);
    }

    @Overwrite
    public boolean isDiggingOrEmerging() {
        return this.isInPose(EntityPose.DIGGING) || this.isInPose(EntityPose.EMERGING);
    }

    @Overwrite
    public boolean canStartRiding(Entity entity) {
        return false;
    }

    @Overwrite
    public boolean disablesShield() {
        return true;
    }

    @Overwrite
    public float calculateNextStepSoundDistance() {
        return this.distanceTraveled + 0.55F;
    }

    @Overwrite
    public static DefaultAttributeContainer.Builder addAttributes() {
        return HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 500.0).add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.30000001192092896).add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0).add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 1.5).add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 30.0);
    }

    @Overwrite
    public boolean occludeVibrationSignals() {
        return true;
    }

    @Overwrite
    public float getSoundVolume() {
        return 4.0F;
    }

    @Nullable
    @Overwrite
    public SoundEvent getAmbientSound() {
        return !this.isInPose(EntityPose.ROARING) && !this.isDiggingOrEmerging() ? this.getAngriness().getSound() : null;
    }

    @Overwrite
    public SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_WARDEN_HURT;
    }

    @Overwrite
    public SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_WARDEN_DEATH;
    }

    @Overwrite
    public void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.ENTITY_WARDEN_STEP, 10.0F, 1.0F);
    }

    // @Overwrite
    // public boolean tryAttack(Entity target) {
    //     this.getWorld().sendEntityStatus(this, (byte)4);
    //     this.playSound(SoundEvents.ENTITY_WARDEN_ATTACK_IMPACT, 10.0F, this.getSoundPitch());
    //     SonicBoomTask.cooldown(this, 0);
    //     return super.tryAttack(target);
    // }
    @Overwrite
    @Override
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
            if (target instanceof PlayerEntity) {
                PlayerEntity playerEntity = (PlayerEntity)target;
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
    @Overwrite
    public void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(ANGER, 0);
    }

    @Overwrite
    public int getAnger() {
        return this.dataTracker.get(ANGER);
    }

    @Overwrite
    public void updateAnger() {
        this.dataTracker.set(ANGER, this.getAngerAtTarget());
    }

    // @Overwrite
    // public void tick() {
    //     World var2 = this.getWorld();
    //     if (var2 instanceof ServerWorld serverWorld) {
    //         Ticker.tick(serverWorld, this.vibrationListenerData, this.vibrationCallback);
    //         if (this.isPersistent() || this.cannotDespawn()) {
    //             WardenBrain.resetDigCooldown(this);
    //         }
    //     }
//
    //     super.tick();
    //     if (this.getWorld().isClient()) {
    //         if (this.age % this.getHeartRate() == 0) {
    //             this.heartbeatCooldown = 10;
    //             if (!this.isSilent()) {
    //                 this.getWorld().playSound(this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_WARDEN_HEARTBEAT, this.getSoundCategory(), 5.0F, this.getSoundPitch(), false);
    //             }
    //         }
//
    //         this.lastTendrilPitch = this.tendrilPitch;
    //         if (this.tendrilPitch > 0) {
    //             --this.tendrilPitch;
    //         }
//
    //         this.lastHeartbeatCooldown = this.heartbeatCooldown;
    //         if (this.heartbeatCooldown > 0) {
    //             --this.heartbeatCooldown;
    //         }
//
    //         switch (this.getPose()) {
    //             case EMERGING:
    //                 this.addDigParticles(this.emergingAnimationState);
    //                 break;
    //             case DIGGING:
    //                 this.addDigParticles(this.diggingAnimationState);
    //         }
    //     }
    // }
    @Overwrite
    public void tick() {
        super.tick();
    }
    @Overwrite
    public Brain<WardenEntity> getBrain() {
        return this.deserializeBrain(new Dynamic<>(NbtOps.INSTANCE, NbtOps.INSTANCE.createMap(ImmutableMap.of(NbtOps.INSTANCE.createString("memories"), NbtOps.INSTANCE.emptyMap()))));
    }
    @Overwrite
    public void mobTick() {
        ServerWorld serverWorld = (ServerWorld)this.getWorld();
        serverWorld.getProfiler().push("wardenBrain");
        this.getBrain().tick(serverWorld, (WardenEntity) (Object) this);
        this.getWorld().getProfiler().pop();
        super.mobTick();

        if (this.age % 20 == 0) {
            this.angerManager.tick(serverWorld, this::isValidTarget);
            this.updateAnger();
        }

        WardenBrain.updateActivities((WardenEntity) (Object) this);
    }

    @Overwrite
    public void handleStatus(byte status) {
        if (status == 4) {
            this.roaringAnimationState.stop();
            this.attackingAnimationState.start(this.age);
        } else if (status == 61) {
            this.tendrilPitch = 10;
        } else if (status == 62) {
            this.chargingSonicBoomAnimationState.start(this.age);
        } else {
            super.handleStatus(status);
        }
    }

    @Overwrite
    public int getHeartRate() {
        float f = (float)this.getAnger() / (float) Angriness.ANGRY.getThreshold();
        return 40 - MathHelper.floor(MathHelper.clamp(f, 0.0F, 1.0F) * 30.0F);
    }

    @Overwrite
    public float getTendrilPitch(float tickDelta) {
        return MathHelper.lerp(tickDelta, (float)this.lastTendrilPitch, (float)this.tendrilPitch) / 10.0F;
    }

    @Overwrite
    public float getHeartPitch(float tickDelta) {
        return MathHelper.lerp(tickDelta, (float)this.lastHeartbeatCooldown, (float)this.heartbeatCooldown) / 10.0F;
    }

    @Overwrite
    public void addDigParticles(AnimationState animationState) {
        if ((float)animationState.getTimeRunning() < 4500.0F) {
            Random random = this.getRandom();
            BlockState blockState = this.getSteppingBlockState();
            if (blockState.getRenderType() != BlockRenderType.INVISIBLE) {
                for(int i = 0; i < 30; ++i) {
                    double d = this.getX() + (double)MathHelper.nextBetween(random, -0.7F, 0.7F);
                    double e = this.getY();
                    double f = this.getZ() + (double)MathHelper.nextBetween(random, -0.7F, 0.7F);
                    this.getWorld().addParticle(new BlockStateParticleEffect(ParticleTypes.BLOCK, blockState), d, e, f, 0.0, 0.0, 0.0);
                }
            }
        }
    }

    @Overwrite
    public void onTrackedDataSet(TrackedData<?> data) {
        if (POSE.equals(data)) {
            switch (this.getPose()) {
                case EMERGING:
                    this.emergingAnimationState.start(this.age);
                    break;
                case DIGGING:
                    this.diggingAnimationState.start(this.age);
                    break;
                case ROARING:
                    this.roaringAnimationState.start(this.age);
                    break;
                case SNIFFING:
                    this.sniffingAnimationState.start(this.age);
            }
        }

        super.onTrackedDataSet(data);
    }

    @Overwrite
    public boolean isImmuneToExplosion(Explosion explosion) {
        return this.isDiggingOrEmerging();
    }

    @Overwrite
    public Brain<WardenEntity> deserializeBrain(Dynamic<?> dynamic) {
        return (Brain<WardenEntity>) T1.create((WardenEntity) (Object) (this), dynamic);
    }

    @Overwrite
    public void sendAiDebugData() {
        super.sendAiDebugData();
        DebugInfoSender.sendBrainDebugData(this);
    }

    @Overwrite
    public void updateEventHandler(BiConsumer<EntityGameEventHandler<?>, ServerWorld> callback) {
        World var3 = this.getWorld();
        if (var3 instanceof ServerWorld serverWorld) {
            callback.accept(this.gameEventHandler, serverWorld);
        }

    }

    @Contract("null->false")
    @Overwrite
    public boolean isValidTarget(@Nullable Entity entity) {
        if (entity instanceof LivingEntity livingEntity) {
            return this.getWorld() == entity.getWorld() && EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.test(entity) && !this.isTeammate(entity) && livingEntity.getType() != EntityType.ARMOR_STAND && livingEntity.getType() != EntityType.WARDEN && !livingEntity.isInvulnerable() && !livingEntity.isDead() && this.getWorld().getWorldBorder().contains(livingEntity.getBoundingBox());
        }
        return false;
    }

    @Overwrite
    public static void addDarknessToClosePlayers(ServerWorld world, Vec3d pos, @Nullable Entity entity, int range) {
        StatusEffectInstance statusEffectInstance = new StatusEffectInstance(StatusEffects.DARKNESS, 260, 0, false, false);
        StatusEffectUtil.addEffectToPlayersWithinDistance(world, entity, pos, (double)range, statusEffectInstance, 200);
    }

    @Overwrite
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        DataResult<NbtElement> var10000 = WardenAngerManager.createCodec(this::isValidTarget).encodeStart(NbtOps.INSTANCE, this.angerManager);
        Logger var10001 = LOGGER;
        Objects.requireNonNull(var10001);
        var10000.resultOrPartial(var10001::error).ifPresent((angerNbt) -> {
            nbt.put("anger", angerNbt);
        });
        var10000 = ListenerData.CODEC.encodeStart(NbtOps.INSTANCE, this.vibrationListenerData);
        var10001 = LOGGER;
        Objects.requireNonNull(var10001);
        var10000.resultOrPartial(var10001::error).ifPresent((listenerData) -> {
            nbt.put("listener", listenerData);
        });
    }

    @Overwrite
    private void playListeningSound() {
        if (!this.isInPose(EntityPose.ROARING)) {
            this.playSound(this.getAngriness().getListeningSound(), 10.0F, this.getSoundPitch());
        }

    }

    @Overwrite
    public Angriness getAngriness() {
        return Angriness.getForAnger(this.getAngerAtTarget());
    }

    @Overwrite
    public int getAngerAtTarget() {
        return this.angerManager.getAngerFor(this.getTarget());
    }

    @Overwrite
    public void removeSuspect(Entity entity) {
        this.angerManager.removeSuspect(entity);
    }

    @Overwrite
    public void increaseAngerAt(@Nullable Entity entity) {
        this.increaseAngerAt(entity, 35, true);
    }

    @VisibleForTesting
    @Overwrite
    public void increaseAngerAt(@Nullable Entity entity, int amount, boolean listening) {
        if (!this.isAiDisabled() && this.isValidTarget(entity)) {
            WardenBrain.resetDigCooldown(this);
            boolean bl = !(this.getBrain().getOptionalRegisteredMemory(MemoryModuleType.ATTACK_TARGET).orElse(null) instanceof PlayerEntity);
            int i = this.angerManager.increaseAngerAt(entity, amount);
            if (entity instanceof PlayerEntity && bl && Angriness.getForAnger(i).isAngry()) {
                this.getBrain().forget(MemoryModuleType.ATTACK_TARGET);
            }

            if (listening) {
                this.playListeningSound();
            }
        }

    }

    @Overwrite
    public Optional<LivingEntity> getPrimeSuspect() {
        return this.getAngriness().isAngry() ? this.angerManager.getPrimeSuspect() : Optional.empty();
    }

    // @Nullable
    // @Overwrite
    // public LivingEntity getTarget() {
    //     return (LivingEntity)this.getBrain().getOptionalRegisteredMemory(MemoryModuleType.ATTACK_TARGET).orElse((LivingEntity) null);
    // }
    @Overwrite
    @Nullable
    public LivingEntity getTarget() {
        return super.getTarget();
    }

    @Overwrite
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    @Nullable
    @Overwrite
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        this.getBrain().remember(MemoryModuleType.DIG_COOLDOWN, Unit.INSTANCE, 1200L);
        if (spawnReason == SpawnReason.TRIGGERED) {
            this.setPose(EntityPose.EMERGING);
            this.getBrain().remember(MemoryModuleType.IS_EMERGING, Unit.INSTANCE, (long)WardenBrain.EMERGE_DURATION);
            this.playSound(SoundEvents.ENTITY_WARDEN_AGITATED, 5.0F, 1.0F);
        }

        return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
    }

    @Overwrite
    public boolean damage(DamageSource source, float amount) {
        boolean bl = super.damage(source, amount);
        if (!this.getWorld().isClient && !this.isAiDisabled() && !this.isDiggingOrEmerging()) {
            Entity entity = source.getAttacker();
            this.increaseAngerAt(entity, Angriness.ANGRY.getThreshold() + 20, false);
            if (this.brain.getOptionalRegisteredMemory(MemoryModuleType.ATTACK_TARGET).isEmpty() && entity instanceof LivingEntity livingEntity) {
                if (!source.isIndirect() || this.isInRange(livingEntity, 5.0)) {
                    this.updateAttackTarget(livingEntity);
                }
            }
        }

        return bl;
    }

    @Overwrite
    public void updateAttackTarget(LivingEntity target) {
        this.getBrain().forget(MemoryModuleType.ROAR_TARGET);
        this.getBrain().remember(MemoryModuleType.ATTACK_TARGET, target);
        this.getBrain().forget(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        SonicBoomTask.cooldown(this, 200);
    }

    @Overwrite
    public EntityDimensions getDimensions(EntityPose pose) {
        EntityDimensions entityDimensions = super.getDimensions(pose);
        return this.isDiggingOrEmerging() ? EntityDimensions.fixed(entityDimensions.width, 1.0F) : entityDimensions;
    }

    @Overwrite
    public boolean isPushable() {
        return !this.isDiggingOrEmerging() && super.isPushable();
    }

    @Overwrite
    public void pushAway(Entity entity) {
        if (!this.isAiDisabled() && !this.getBrain().hasMemoryModule(MemoryModuleType.TOUCH_COOLDOWN)) {
            this.getBrain().remember(MemoryModuleType.TOUCH_COOLDOWN, Unit.INSTANCE, 20L);
            this.increaseAngerAt(entity);
            WardenBrain.lookAtDisturbance((WardenEntity) (Object) this, entity.getBlockPos());
        }

        super.pushAway(entity);
    }

    @VisibleForTesting
    @Overwrite
    public WardenAngerManager getAngerManager() {
        return this.angerManager;
    }

    @Overwrite
    public EntityNavigation createNavigation(World world) {
        return new MobNavigation(this, world){

            @Override
            protected PathNodeNavigator createPathNodeNavigator(int range) {
                this.nodeMaker = new LandPathNodeMaker();
                this.nodeMaker.setCanEnterOpenDoors(true);
                return t(this.nodeMaker, range);
            }
        };
    }
    @Unique
    private PathNodeNavigator t(PathNodeMaker nodeMaker, int range) {
        return new PathNodeNavigator(nodeMaker, range){
            @Override
            protected float getDistance(PathNode a, PathNode b) {
                return a.getHorizontalDistance(b);
            }
        };
    }

    @Overwrite
    public Vector3f getPassengerAttachmentPos(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
        return new Vector3f(0.0F, dimensions.height + 0.25F * scaleFactor, 0.0F);
    }

    @Overwrite
    public Vibrations.ListenerData getVibrationListenerData() {
        return this.vibrationListenerData;
    }

    @Overwrite
    public Vibrations.Callback getVibrationCallback() {
        return this.vibrationCallback;
    }
    @Override
    public void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(4, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.goalSelector.add(8, new WanderAroundGoal(this, 0.6));
        this.goalSelector.add(9, new LookAtEntityGoal(this, PlayerEntity.class, 3.0f, 1.0f));
        this.goalSelector.add(10, new LookAtEntityGoal(this, MobEntity.class, 8.0f));
        this.targetSelector.add(1, new RevengeGoal(this, WardenEntity.class));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, EndermiteEntity.class, true, false));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, SkeletonEntity.class, true));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, WitherSkeletonEntity.class, true));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, StrayEntity.class, true));
    }

    static {
        ANGER = DataTracker.registerData(wa.class, TrackedDataHandlerRegistry.INTEGER);
    }
}