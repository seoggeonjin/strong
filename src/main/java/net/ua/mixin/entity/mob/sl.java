package net.ua.mixin.entity.mob;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.control.JumpControl;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import net.ua.ips.sli;
import net.ua.mixin2.inner.control.SlimeMoveControl;
import net.ua.mixin2.inner.goals.slime.FaceTowardTargetGoal;
import net.ua.mixin2.inner.goals.slime.MoveGoal;
import net.ua.mixin2.inner.goals.slime.RandomLookGoal;
import net.ua.mixin2.inner.goals.slime.SwimmingGoal;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.*;

@Mixin(SlimeEntity.class)
public class sl extends MobEntity implements Monster, sli {
    @Final
    @Shadow
    private static final TrackedData<Integer> SLIME_SIZE = DataTracker.registerData(sl.class, TrackedDataHandlerRegistry.INTEGER);
    @Final
    @Shadow
    public static final int MIN_SIZE = 1;
    @Final
    @Shadow
    public static final int MAX_SIZE = 127;
    @Shadow
    public float targetStretch;
    @Shadow
    public float stretch;
    @Shadow
    public float lastStretch;
    @Shadow
    private boolean onGroundLastTick;

    public sl(EntityType<? extends sl> entityType, World world) {
        super(entityType, world);
        this.reinitDimensions();
        this.moveControl = new SlimeMoveControl(this);
    }

    @Overwrite
    public void initGoals() {
        this.goalSelector.add(1, new SwimmingGoal(this));
        this.goalSelector.add(2, new FaceTowardTargetGoal(this));
        this.goalSelector.add(3, new RandomLookGoal(this));
        this.goalSelector.add(5, new MoveGoal(this));
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false, livingEntity -> Math.abs(livingEntity.getY() - this.getY()) <= 4.0));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, IronGolemEntity.class, true));
    }

    @Overwrite
    public SoundCategory getSoundCategory() {
        return SoundCategory.HOSTILE;
    }

    @Overwrite
    public void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(SLIME_SIZE, 1);
    }

    @Overwrite
    @VisibleForTesting
    public void setSize(int size, boolean heal) {
        int i = MathHelper.clamp(size, MIN_SIZE, MAX_SIZE);
        this.dataTracker.set(SLIME_SIZE, i);
        this.refreshPosition();
        this.calculateDimensions();
        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(i * i);
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.2f + 0.1f * (float)i);
        this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(i);
        if (heal) {
            this.setHealth(this.getMaxHealth());
        }
        this.experiencePoints = i;
    }

    @Overwrite
    public int getSize() {
        return this.dataTracker.get(SLIME_SIZE);
    }

    @Overwrite
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Size", this.getSize() - 1);
        nbt.putBoolean("wasOnGround", this.onGroundLastTick);
    }

    @Overwrite
    public void readCustomDataFromNbt(NbtCompound nbt) {
        this.setSize(nbt.getInt("Size") + 1, false);
        super.readCustomDataFromNbt(nbt);
        this.onGroundLastTick = nbt.getBoolean("wasOnGround");
    }

    @Overwrite
    public boolean isSmall() {
        return this.getSize() <= 1;
    }

    @Overwrite
    public ParticleEffect getParticles() {
        return ParticleTypes.ITEM_SLIME;
    }

    @Overwrite
    public boolean isDisallowedInPeaceful() {
        return this.getSize() > 0;
    }

    @Overwrite
    public void tick() {
        this.stretch += (this.targetStretch - this.stretch) * 0.5f;
        this.lastStretch = this.stretch;
        super.tick();
        if (this.isOnGround() && !this.onGroundLastTick) {
            int i = this.getSize();
            for (int j = 0; j < i * 8; ++j) {
                float f = this.random.nextFloat() * ((float)Math.PI * 2);
                float g = this.random.nextFloat() * 0.5f + 0.5f;
                float h = MathHelper.sin(f) * (float)i * 0.5f * g;
                float k = MathHelper.cos(f) * (float)i * 0.5f * g;
                this.getWorld().addParticle(this.getParticles(), this.getX() + (double)h, this.getY(), this.getZ() + (double)k, 0.0, 0.0, 0.0);
            }
            this.playSound(this.getSquishSound(), this.getSoundVolume(), ((this.random.nextFloat() - this.random.nextFloat()) * 0.2f + 1.0f) / 0.8f);
            this.targetStretch = -0.5f;
        } else if (!this.isOnGround() && this.onGroundLastTick) {
            this.targetStretch = 1.0f;
        }
        this.onGroundLastTick = this.isOnGround();
        this.updateStretch();
    }

    @Overwrite
    public void updateStretch() {
        this.targetStretch *= 0.6f;
    }

    @Overwrite
    public int getTicksUntilNextJump() {
        return this.random.nextInt(20) + 10;
    }
    @Unique
    public LivingEntity getTarget() {
        return super.getTarget();
    }

    @Overwrite
    public void calculateDimensions() {
        double d = this.getX();
        double e = this.getY();
        double f = this.getZ();
        super.calculateDimensions();
        this.setPosition(d, e, f);
    }

    @Overwrite
    public void onTrackedDataSet(TrackedData<?> data) {
        if (SLIME_SIZE.equals(data)) {
            this.calculateDimensions();
            this.setYaw(this.headYaw);
            this.bodyYaw = this.headYaw;
            if (this.isTouchingWater() && this.random.nextInt(20) == 0) {
                this.onSwimmingStart();
            }
        }
        super.onTrackedDataSet(data);
    }

    @Overwrite
    public EntityType<? extends SlimeEntity> getType() {
        return EntityType.SLIME;
    }

    @Overwrite
    public void remove(Entity.RemovalReason reason) {
        int i = this.getSize();
        if (!this.getWorld().isClient && i > 1 && this.isDead()) {
            Text text = this.getCustomName();
            boolean bl = this.isAiDisabled();
            float f = (float)i / 4.0f;
            int j = i / 2;
            for (int l = 0; l < 10; ++l) {
                float g = ((float)(l % 2) - 0.5f) * f;
                float h = ((float)(l / 2) - 0.5f) * f;
                sli slimeEntity = (sli) this.getType().create(this.getWorld());
                if (slimeEntity == null) continue;
                if (this.isPersistent()) {
                    slimeEntity.setPersistent();
                }
                slimeEntity.setCustomName(text);
                slimeEntity.setAiDisabled(bl);
                slimeEntity.setInvulnerable(this.isInvulnerable());
                slimeEntity.setSize(j, true);
                slimeEntity.refreshPositionAndAngles(this.getX() + (double)g, this.getY() + 0.5, this.getZ() + (double)h, this.random.nextFloat() * 360.0f, 0.0f);
                this.getWorld().spawnEntity((Entity) slimeEntity);
            }
        }
        super.remove(reason);
    }

    @Overwrite
    public void pushAwayFrom(Entity entity) {
        super.pushAwayFrom(entity);
        if (entity instanceof IronGolemEntity && this.canAttack()) {
            this.damage((LivingEntity)entity);
        }
    }

    @Overwrite
    public void onPlayerCollision(PlayerEntity player) {
        if (this.canAttack()) {
            this.damage(player);
        }
    }

    @Overwrite
    public void damage(LivingEntity target) {
        if (this.isAlive()) {
            int i = this.getSize();
            if (this.squaredDistanceTo(target) < 0.6 * (double)i * (0.6 * (double)i) && this.canSee(target) && target.damage(this.getDamageSources().mobAttack(this), this.getDamageAmount())) {
                this.playSound(SoundEvents.ENTITY_SLIME_ATTACK, 1.0f, (this.random.nextFloat() - this.random.nextFloat()) * 0.2f + 1.0f);
                this.applyDamageEffects(this, target);
            }
        }
    }

    @Overwrite
    public float getActiveEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return 0.625f * dimensions.height;
    }

    @Overwrite
    public Vector3f getPassengerAttachmentPos(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
        return new Vector3f(0.0f, dimensions.height - 0.015625f * (float)this.getSize() * scaleFactor, 0.0f);
    }

    @Overwrite
    public boolean canAttack() {
        return !this.isSmall() && this.canMoveVoluntarily();
    }

    @Overwrite
    public float getDamageAmount() {
        return (float)this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
    }

    @Overwrite
    public SoundEvent getHurtSound(DamageSource source) {
        if (this.isSmall()) {
            return SoundEvents.ENTITY_SLIME_HURT_SMALL;
        }
        return SoundEvents.ENTITY_SLIME_HURT;
    }

    @Overwrite
    public SoundEvent getDeathSound() {
        if (this.isSmall()) {
            return SoundEvents.ENTITY_SLIME_DEATH_SMALL;
        }
        return SoundEvents.ENTITY_SLIME_DEATH;
    }

    @Overwrite
    public SoundEvent getSquishSound() {
        if (this.isSmall()) {
            return SoundEvents.ENTITY_SLIME_SQUISH_SMALL;
        }
        return SoundEvents.ENTITY_SLIME_SQUISH;
    }

    @Overwrite
    public static boolean canSpawn(EntityType<SlimeEntity> type, WorldAccess world, SpawnReason spawnReason, BlockPos pos, Random random) {
        if (world.getDifficulty() != Difficulty.PEACEFUL) {
            boolean bl;
            if (world.getBiome(pos).isIn(BiomeTags.ALLOWS_SURFACE_SLIME_SPAWNS) && pos.getY() > 50 && pos.getY() < 70 && random.nextFloat() < 0.5f && random.nextFloat() < world.getMoonSize() && world.getLightLevel(pos) <= random.nextInt(8)) {
                return SlimeEntity.canMobSpawn(type, world, spawnReason, pos, random);
            }
            if (!(world instanceof StructureWorldAccess)) {
                return false;
            }
            ChunkPos chunkPos = new ChunkPos(pos);
            boolean bl2 = bl = ChunkRandom.getSlimeRandom(chunkPos.x, chunkPos.z, ((StructureWorldAccess)world).getSeed(), 987234911L).nextInt(10) == 0;
            if (random.nextInt(10) == 0 && bl && pos.getY() < 40) {
                return SlimeEntity.canMobSpawn(type, world, spawnReason, pos, random);
            }
        }
        return false;
    }

    @Overwrite
    public float getSoundVolume() {
        return 0.4f * (float)this.getSize();
    }

    @Overwrite
    public int getMaxLookPitchChange() {
        return 0;
    }

    @Overwrite
    public boolean makesJumpSound() {
        return this.getSize() > 0;
    }

    @Overwrite
    public void jump() {
        Vec3d vec3d = this.getVelocity();
        this.setVelocity(vec3d.x, this.getJumpVelocity(), vec3d.z);
        this.velocityDirty = true;
    }

    @Overwrite
    @Nullable
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        Random random = world.getRandom();
        int i = random.nextInt(2) + 2;
        if (i < 2 && random.nextFloat() < 0.5f * difficulty.getClampedLocalDifficulty()) {
            ++i;
        }
        int j = 1 << i;
        this.setSize(j, true);
        return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
    }

    @Overwrite
    public float getJumpSoundPitch() {
        float f = this.isSmall() ? 1.4f : 0.8f;
        return ((this.random.nextFloat() - this.random.nextFloat()) * 0.2f + 1.0f) * f;
    }

    @Overwrite
    public SoundEvent getJumpSound() {
        return this.isSmall() ? SoundEvents.ENTITY_SLIME_JUMP_SMALL : SoundEvents.ENTITY_SLIME_JUMP;
    }

    @Overwrite
    public EntityDimensions getDimensions(EntityPose pose) {
        return super.getDimensions(pose).scaled(0.255f * (float)this.getSize());
    }
    @Unique
    public EntityNavigation getNavigation() {
        return super.getNavigation();
    }
    @Unique
    public boolean isTouchingWater() {
        return super.isTouchingWater();
    }
    @Unique
    public boolean isInLava() {
        return super.isInLava();
    }
    @Unique
    public MoveControl getMoveControl() {
        return super.getMoveControl();
    }
    @Unique
    public Random getRandom() {
        return super.getRandom();
    }
    @Unique
    public JumpControl getJumpControl() {
        return super.getJumpControl();
    }
    @Unique
    public boolean isOnGround() {
        return super.isOnGround();
    }
    @Unique
    public boolean hasStatusEffect(StatusEffect effect) {
        return super.hasStatusEffect(effect);
    }
    @Unique
    public boolean hasVehicle() {
        return super.hasVehicle();
    }
    @Unique
    public boolean canTarget(LivingEntity target) {
        return super.canTarget(target);
    }
    @Unique
    public void lookAtEntity(Entity targetEntity, float maxYawChange, float maxPitchChange) {
        super.lookAtEntity(targetEntity, maxYawChange, maxPitchChange);
    }
    @Unique
    public float getYaw() {
        return super.getYaw();
    }
    @Unique
    public void playSound(SoundEvent sound, float volume, float pitch) {
        super.playSound(sound, volume, pitch);
    }
    @Unique
    public void sidewaysSpeed(float sidewaysSpeed) {
        this.sidewaysSpeed = sidewaysSpeed;
    }
    @Unique
    public void forwardSpeed(float forwardSpeed) {
        this.forwardSpeed = forwardSpeed;
    }
    @Unique
    public void setPersistent() {
        super.setPersistent();
    }
    @Unique
    public void setCustomName(@Nullable Text name) {
        super.setCustomName(name);
    }
    @Unique
    public void setAiDisabled(boolean aiDisabled) {
        super.setAiDisabled(aiDisabled);
    }
    @Unique
    public void setInvulnerable(boolean invulnerable) {
        super.setInvulnerable(invulnerable);
    }
    @Unique
    public void refreshPositionAndAngles(double x, double y, double z, float yaw, float pitch) {
        super.refreshPositionAndAngles(x, y, z, yaw, pitch);
    }
}
