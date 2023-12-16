package net.ua.mixin.entity.mob;

import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.SwimNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.ElderGuardianEntity;
import net.minecraft.entity.mob.GiantEntity;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.ua.ips.gui;
import net.ua.mixin2.inner.goals.guardian.FireBeamGoal;
import net.ua.mixin2.inner.otherwise.GuardianTargetPredicate;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.EnumSet;
import java.util.List;

@Mixin(ElderGuardianEntity.class)
public class egu extends gu implements gui {
    @Unique
    private static final TrackedData<Boolean> SPIKES_RETRACTED = DataTracker.registerData(egu.class, TrackedDataHandlerRegistry.BOOLEAN);
    @Unique
    private static final TrackedData<Integer> BEAM_TARGET_ID = DataTracker.registerData(egu.class, TrackedDataHandlerRegistry.INTEGER);
    @Final
    @Shadow
    public static final float SCALE = EntityType.ELDER_GUARDIAN.getWidth() / EntityType.GUARDIAN.getWidth();
    @Final
    @Shadow
    private static final int field_38119 = 1200;
    @Final
    @Shadow
    private static final int AFFECTED_PLAYER_RANGE = 50;
    @Final
    @Shadow
    private static final int MINING_FATIGUE_DURATION = 6000;
    @Final
    @Shadow
    private static final int MINING_FATIGUE_AMPLIFIER = 2;
    @Final
    @Shadow
    private static final int field_38118 = 1200;

    public egu(EntityType<? extends egu> entityType, World world) {
        super(entityType, world);
    }
    @Inject(method = "<init>", at = @At("RETURN"))
    public void ggu(EntityType<? extends ElderGuardianEntity> entityType, World world, CallbackInfo ci) {
        this.setPersistent();
        if (this.wanderGoal != null) {
            this.wanderGoal.setChance(400);
        }
    }

    @Overwrite
    public static DefaultAttributeContainer.Builder createElderGuardianAttributes() {
        return GuardianEntity.createGuardianAttributes().add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3f).add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0).add(EntityAttributes.GENERIC_MAX_HEALTH, 80.0);
    }
    @Override
    public void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(SPIKES_RETRACTED, false);
        this.dataTracker.startTracking(BEAM_TARGET_ID, 0);
    }
    @Override
    public void initGoals() {
        GoToWalkTargetGoal goToWalkTargetGoal = new GoToWalkTargetGoal(this, 1.0);
        this.wanderGoal = new WanderAroundGoal(this, 1.0, 80);
        bg = new FireBeamGoal(this);
        this.goalSelector.add(4, bg);
        this.goalSelector.add(5, goToWalkTargetGoal);
        this.goalSelector.add(7, this.wanderGoal);
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(8, new LookAtEntityGoal(this, GiantEntity.class, 8.0f));
        this.goalSelector.add(8, new LookAtEntityGoal(this, net.minecraft.entity.mob.GuardianEntity.class, 12.0f, 0.01f));
        this.goalSelector.add(9, new LookAroundGoal(this));
        this.wanderGoal.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        goToWalkTargetGoal.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, LivingEntity.class, 10, true, false, new GuardianTargetPredicate(this)));
    }

    @Overwrite
    public int getWarmupTime() {
        return 2147483647;
    }

    @Overwrite
    public SoundEvent getAmbientSound() {
        return this.isInsideWaterOrBubbleColumn() ? SoundEvents.ENTITY_ELDER_GUARDIAN_AMBIENT : SoundEvents.ENTITY_ELDER_GUARDIAN_AMBIENT_LAND;
    }

    @Overwrite
    public SoundEvent getHurtSound(DamageSource source) {
        return this.isInsideWaterOrBubbleColumn() ? SoundEvents.ENTITY_ELDER_GUARDIAN_HURT : SoundEvents.ENTITY_ELDER_GUARDIAN_HURT_LAND;
    }

    @Overwrite
    public SoundEvent getDeathSound() {
        return this.isInsideWaterOrBubbleColumn() ? SoundEvents.ENTITY_ELDER_GUARDIAN_DEATH : SoundEvents.ENTITY_ELDER_GUARDIAN_DEATH_LAND;
    }

    @Overwrite
    public SoundEvent getFlopSound() {
        return SoundEvents.ENTITY_ELDER_GUARDIAN_FLOP;
    }

    @Overwrite
    public void mobTick() {
        super.mobTick();
        if ((this.age + this.getId()) % 1200 == 0) {
            StatusEffectInstance statusEffectInstance = new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 6000, 2);
            List<ServerPlayerEntity> list = StatusEffectUtil.addEffectToPlayersWithinDistance((ServerWorld)this.getWorld(), this, this.getPos(), 50.0, statusEffectInstance, 1200);
            list.forEach(serverPlayerEntity -> serverPlayerEntity.networkHandler.sendPacket(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.ELDER_GUARDIAN_EFFECT, this.isSilent() ? GameStateChangeS2CPacket.DEMO_OPEN_SCREEN : 1)));
        }
        if (!this.hasPositionTarget()) {
            this.setPositionTarget(this.getBlockPos(), 16);
        }
    }

    @Overwrite
    public Vector3f getPassengerAttachmentPos(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
        return new Vector3f(0.0f, dimensions.height + 0.353125f * scaleFactor, 0.0f);
    }
    @Override
    public void setBeamTarget(int entityId) {
        this.dataTracker.set(BEAM_TARGET_ID, entityId);
    }
    @Override
    public WanderAroundGoal wanderGoal() {
        return wanderGoal;
    }
    @Override
    public void setSpikesRetracted(boolean retracted) {
        this.dataTracker.set(SPIKES_RETRACTED, retracted);
    }
    @Override
    public void setTarget(LivingEntity entity) {
        super.setTarget(entity);
    }
    @Unique
    public FireBeamGoal bg = new FireBeamGoal(this);
    @Final
    @Unique
    private static final int WARMUP_TIME = 2147483647;
    @Unique
    private float tailAngle;
    @Unique
    private float prevTailAngle;
    @Unique
    private float spikesExtensionRate;
    @Unique
    private float spikesExtension;
    @Unique
    private float prevSpikesExtension;
    @Nullable
    @Unique
    private LivingEntity cachedBeamTarget;
    @Unique
    private int beamTicks;
    @Unique
    private boolean flopping;
    @Unique
    protected WanderAroundGoal wanderGoal;

    @Override
    public EntityNavigation createNavigation(World world) {
        return new SwimNavigation(this, world);
    }

    @Override
    public EntityGroup getGroup() {
        return EntityGroup.AQUATIC;
    }

    @Override
    public boolean areSpikesRetracted() {
        return this.dataTracker.get(SPIKES_RETRACTED);
    }

    @Override
    public boolean hasBeamTarget() {
        return this.dataTracker.get(BEAM_TARGET_ID) != 0;
    }

    @Nullable
    public LivingEntity getBeamTarget() {
        if (!this.hasBeamTarget()) {
            return null;
        }
        if (this.getWorld().isClient) {
            if (this.cachedBeamTarget != null) {
                return this.cachedBeamTarget;
            }
            Entity entity = this.getWorld().getEntityById(this.dataTracker.get(BEAM_TARGET_ID));
            if (entity instanceof LivingEntity) {
                this.cachedBeamTarget = (LivingEntity)entity;
                return this.cachedBeamTarget;
            }
            return null;
        }
        return this.getTarget();
    }

    @Override
    public void onTrackedDataSet(TrackedData<?> data) {
        super.onTrackedDataSet(data);
        if (BEAM_TARGET_ID.equals(data)) {
            this.beamTicks = 0;
            this.cachedBeamTarget = null;
        }
    }

    @Override
    public int getMinAmbientSoundDelay() {
        return 160;
    }

    @Override
    public Entity.MoveEffect getMoveEffect() {
        return Entity.MoveEffect.EVENTS;
    }

    @Override
    public float getActiveEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return dimensions.height * 0.5f;
    }

    @Override
    public float getPathfindingFavor(BlockPos pos, WorldView world) {
        if (world.getFluidState(pos).isIn(FluidTags.WATER)) {
            return 10.0f + world.getPhototaxisFavor(pos);
        }
        return super.getPathfindingFavor(pos, world);
    }

    @Override
    public void tickMovement() {
        if (this.isAlive()) {
            if (this.getWorld().isClient) {
                Vec3d vec3d;
                this.prevTailAngle = this.tailAngle;
                if (!this.isTouchingWater()) {
                    this.spikesExtensionRate = 2.0f;
                    vec3d = this.getVelocity();
                    if (vec3d.y > 0.0 && this.flopping && !this.isSilent()) {
                        this.getWorld().playSound(this.getX(), this.getY(), this.getZ(), this.getFlopSound(), this.getSoundCategory(), 1.0f, 1.0f, false);
                    }
                    this.flopping = vec3d.y < 0.0 && this.getWorld().isTopSolid(this.getBlockPos().down(), this);
                } else {
                    this.spikesExtensionRate = this.areSpikesRetracted() ? (this.spikesExtensionRate < 0.5f ? 4.0f : (this.spikesExtensionRate += (0.5f - this.spikesExtensionRate) * 0.1f)) : (this.spikesExtensionRate += (0.125f - this.spikesExtensionRate) * 0.2f);
                }
                this.tailAngle += this.spikesExtensionRate;
                this.prevSpikesExtension = this.spikesExtension;
                this.spikesExtension = !this.isInsideWaterOrBubbleColumn() ? this.random.nextFloat() : (this.areSpikesRetracted() ? (this.spikesExtension += (0.0f - this.spikesExtension) * 0.25f) : (this.spikesExtension += (1.0f - this.spikesExtension) * 0.06f));
                if (this.areSpikesRetracted() && this.isTouchingWater()) {
                    vec3d = this.getRotationVec(0.0f);
                    for (int i = 0; i < 2; ++i) {
                        this.getWorld().addParticle(ParticleTypes.BUBBLE, this.getParticleX(0.5) - vec3d.x * 1.5, this.getRandomBodyY() - vec3d.y * 1.5, this.getParticleZ(0.5) - vec3d.z * 1.5, 0.0, 0.0, 0.0);
                    }
                }
                if (this.hasBeamTarget()) {
                    LivingEntity livingEntity;
                    if (this.beamTicks < this.getWarmupTime()) {
                        ++this.beamTicks;
                    }
                    if ((livingEntity = this.getBeamTarget()) != null) {
                        this.getLookControl().lookAt(livingEntity, 90.0f, 90.0f);
                        this.getLookControl().tick();
                        double d = this.getBeamProgress(0.0f);
                        double e = livingEntity.getX() - this.getX();
                        double f = livingEntity.getBodyY(0.5) - this.getEyeY();
                        double g = livingEntity.getZ() - this.getZ();
                        double h = Math.sqrt(e * e + f * f + g * g);
                        e /= h;
                        f /= h;
                        g /= h;
                        double j = this.random.nextDouble();
                        while (j < h) {
                            this.getWorld().addParticle(ParticleTypes.BUBBLE, this.getX() + e * (j += 1.8 - d + this.random.nextDouble() * (1.7 - d)), this.getEyeY() + f * j, this.getZ() + g * j, 0.0, 0.0, 0.0);
                        }
                    }
                }
            }
            if (this.isInsideWaterOrBubbleColumn()) {
                this.setAir(300);
            } else if (this.isOnGround()) {
                this.setVelocity(this.getVelocity().add((this.random.nextFloat() * 2.0f - 1.0f) * 0.4f, 0.5, (this.random.nextFloat() * 2.0f - 1.0f) * 0.4f));
                this.setYaw(this.random.nextFloat() * 360.0f);
                this.setOnGround(false);
                this.velocityDirty = true;
            }
            if (this.hasBeamTarget()) {
                this.setYaw(this.headYaw);
            }
        }
        super.tickMovement();
    }

    @Override
    public float getTailAngle(float tickDelta) {
        return MathHelper.lerp(tickDelta, this.prevTailAngle, this.tailAngle);
    }

    @Override
    public float getSpikesExtension(float tickDelta) {
        return MathHelper.lerp(tickDelta, this.prevSpikesExtension, this.spikesExtension);
    }

    @Override
    public float getBeamProgress(float tickDelta) {
        return ((float)this.beamTicks + tickDelta) / (float)this.getWarmupTime();
    }

    @Override
    public float getBeamTicks() {
        return this.beamTicks;
    }

    @Override
    public boolean canSpawn(WorldView world) {
        return world.doesNotIntersectEntities(this);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        Entity entity;
        if (this.getWorld().isClient) {
            return false;
        }
        if (!this.areSpikesRetracted() && !source.isIn(DamageTypeTags.AVOIDS_GUARDIAN_THORNS) && !source.isOf(DamageTypes.THORNS) && (entity = source.getSource()) instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity)entity;
            livingEntity.damage(this.getDamageSources().thorns(this), 2.0f);
        }
        if (this.wanderGoal != null) {
            this.wanderGoal.ignoreChanceOnce();
        }
        return super.damage(source, amount);
    }

    @Override
    public int getMaxLookPitchChange() {
        return 180;
    }

    @Override
    public void travel(Vec3d movementInput) {
        if (this.isLogicalSideForUpdatingMovement() && this.isTouchingWater()) {
            this.updateVelocity(0.1f, movementInput);
            this.move(MovementType.SELF, this.getVelocity());
            this.setVelocity(this.getVelocity().multiply(0.9));
            if (!this.areSpikesRetracted() && this.getTarget() == null) {
                this.setVelocity(this.getVelocity().add(0.0, -0.005, 0.0));
            }
        } else {
            super.travel(movementInput);
        }
    }
    @Override
    public void tick() {
        if (this.getTarget() == null) {
            bg.beamTicks = 0;
        } else if (this.getTarget().isDead()) {
            bg.beamTicks = 0;
        }
        super.tick();
    }
    @Override
    public LivingEntity getTarget() {
        return super.getTarget();
    }
}
