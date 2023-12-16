package net.ua.mixin.entity.mob;

import net.minecraft.entity.*;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.control.BodyControl;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.FlyingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.ua.ips.phi;
import net.ua.mixin2.inner.goals.phantom.CircleMovementGoal;
import net.ua.mixin2.inner.goals.phantom.FindTargetGoal;
import net.ua.mixin2.inner.goals.phantom.StartAttackGoal;
import net.ua.mixin2.inner.goals.phantom.SwoopMovementGoal;
import net.ua.mixin2.inner.control.PhantomBodyControl;
import net.ua.mixin2.inner.control.PhantomLookControl;
import net.ua.mixin2.inner.control.PhantomMoveControl;
import net.ua.mixin2.inner.otherwise.PhantomMovementType;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.*;

@Mixin(PhantomEntity.class)
public class pe extends FlyingEntity implements Monster, phi {
    @Unique
    boolean b = false;
    @Unique
    LivingEntity l;
    @Unique
    SwoopMovementGoal g = new SwoopMovementGoal(this);
    @Unique
    private int a = 0;
    @Final
    @Shadow
    public static final float field_30475 = 7.448451f;
    @Final
    @Shadow
    public static final int WING_FLAP_TICKS = MathHelper.ceil(24.166098f);
    @Final
    @Shadow
    private static final TrackedData<Integer> SIZE = DataTracker.registerData(pe.class, TrackedDataHandlerRegistry.INTEGER);
    @Unique
    PhantomMovementType movementType = PhantomMovementType.CIRCLE;
    @Shadow
    Vec3d targetPosition = Vec3d.ZERO;
    @Shadow
    BlockPos circlingCenter = BlockPos.ORIGIN;

    public pe(EntityType<? extends pe> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 5;
        this.moveControl = new PhantomMoveControl(this);
        this.lookControl = new PhantomLookControl(this);
    }
    public boolean horizontalCollision() {
        return this.horizontalCollision;
    }
    @Nullable
    public LivingEntity getTarget() {
        return super.getTarget();
    }

    @Overwrite
    public boolean isFlappingWings() {
        return (this.getWingFlapTickOffset() + this.age) % WING_FLAP_TICKS == 0;
    }

    @Overwrite
    public BodyControl createBodyControl() {
        return new PhantomBodyControl(this);
    }

    @Overwrite
    public void initGoals() {
        g = new SwoopMovementGoal(this);
        this.goalSelector.add(1, new StartAttackGoal(this));
        this.goalSelector.add(2, g);
        this.goalSelector.add(3, new CircleMovementGoal(this));
        this.targetSelector.add(1, new FindTargetGoal(this));
    }

    @Overwrite
    public void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(SIZE, 0);
    }

    @Overwrite
    public void setPhantomSize(int size) {
        this.dataTracker.set(SIZE, MathHelper.clamp(size, 0, 64));
    }

    @Overwrite
    public void onSizeChanged() {
        this.calculateDimensions();
        this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(6 + this.getPhantomSize());
    }

    @Overwrite
    public int getPhantomSize() {
        return this.dataTracker.get(SIZE);
    }

    @Overwrite
    public float getActiveEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return dimensions.height * 0.35f;
    }

    @Overwrite
    public void onTrackedDataSet(TrackedData<?> data) {
        if (SIZE.equals(data)) {
            this.onSizeChanged();
        }
        super.onTrackedDataSet(data);
    }

    @Overwrite
    public int getWingFlapTickOffset() {
        return this.getId() * 3;
    }

    @Overwrite
    public boolean isDisallowedInPeaceful() {
        return true;
    }

    @Overwrite
    public void tick() {
        super.tick();
        if (this.getTarget() != null) {
            l = this.getTarget();
        }
        if (b) {
            if (l != null) {
                if (a == 0) {
                    new Thread(() -> {
                        while (b) l.setPos(this.getX2(), this.getPos().y, this.getZ2());
                    }).start();
                }
                a++;
                if (a == 60) {
                    a = 0;
                    b = false;
                    l = null;
                }
            }
        }
        if (this.getWorld().isClient) {
            float f = MathHelper.cos((float)(this.getWingFlapTickOffset() + this.age) * field_30475 * ((float)Math.PI / 180) + (float)Math.PI);
            float g = MathHelper.cos((float)(this.getWingFlapTickOffset() + this.age + 1) * field_30475 * ((float)Math.PI / 180) + (float)Math.PI);
            if (f > 0.0f && g <= 0.0f) {
                this.getWorld().playSound(this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_PHANTOM_FLAP, this.getSoundCategory(), 0.95f + this.random.nextFloat() * 0.05f, 0.95f + this.random.nextFloat() * 0.05f, false);
            }
            int i = this.getPhantomSize();
            float h = MathHelper.cos(this.getYaw() * ((float)Math.PI / 180)) * (1.3f + 0.21f * (float)i);
            float j = MathHelper.sin(this.getYaw() * ((float)Math.PI / 180)) * (1.3f + 0.21f * (float)i);
            float k = (0.3f + f * 0.45f) * ((float)i * 0.2f + 1.0f);
            this.getWorld().addParticle(ParticleTypes.MYCELIUM, this.getX() + (double)h, this.getY() + (double)k, this.getZ() + (double)j, 0.0, 0.0, 0.0);
            this.getWorld().addParticle(ParticleTypes.MYCELIUM, this.getX() - (double)h, this.getY() + (double)k, this.getZ() - (double)j, 0.0, 0.0, 0.0);
        }
    }

    @Overwrite
    public void tickMovement() {
        if (this.isAlive() && this.isAffectedByDaylight()) {
            this.setOnFireFor(8);
        }
        super.tickMovement();
    }

    @Overwrite
    public void mobTick() {
        super.mobTick();
    }

    @Overwrite
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        this.circlingCenter = this.getBlockPos().up(5);
        this.setPhantomSize(0);
        return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
    }

    @Overwrite
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("AX")) {
            this.circlingCenter = new BlockPos(nbt.getInt("AX"), nbt.getInt("AY"), nbt.getInt("AZ"));
        }
        this.setPhantomSize(nbt.getInt("Size"));
    }

    @Overwrite
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("AX", this.circlingCenter.getX());
        nbt.putInt("AY", this.circlingCenter.getY());
        nbt.putInt("AZ", this.circlingCenter.getZ());
        nbt.putInt("Size", this.getPhantomSize());
    }

    @Overwrite
    public boolean shouldRender(double distance) {
        return true;
    }

    @Overwrite
    public SoundCategory getSoundCategory() {
        return SoundCategory.HOSTILE;
    }

    @Overwrite
    public SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_PHANTOM_AMBIENT;
    }

    @Overwrite
    public SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_PHANTOM_HURT;
    }

    @Overwrite
    public SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_PHANTOM_DEATH;
    }

    @Overwrite
    public EntityGroup getGroup() {
        return EntityGroup.UNDEAD;
    }

    @Overwrite
    public float getSoundVolume() {
        return 1.0f;
    }

    @Overwrite
    public boolean canTarget(EntityType<?> type) {
        return true;
    }

    @Overwrite
    public EntityDimensions getDimensions(EntityPose pose) {
        int i = this.getPhantomSize();
        EntityDimensions entityDimensions = super.getDimensions(pose);
        return entityDimensions.scaled(1.0f + 0.15f * (float)i);
    }

    @Overwrite
    public Vector3f getPassengerAttachmentPos(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
        return new Vector3f(0.0f, dimensions.height * 0.675f, 0.0f);
    }

    @Overwrite
    public float getUnscaledRidingOffset(Entity vehicle) {
        return -0.125f;
    }
    @Override
    public boolean isAffectedByDaylight() {
        return false;
    }
    @Unique
    public Vec3d targetPosition() {
        return targetPosition;
    }
    @Unique
    public BlockPos circlingCenter() {
        return circlingCenter;
    }
    @Unique
    public void circlingCenter(BlockPos b) {
        circlingCenter = b;
    }
    @Unique
    public void targetPosition(Vec3d v) {
        targetPosition = v;
    }
    @Unique
    public World getWorld() {
        return super.getWorld();
    }
    @Unique
    public BlockPos getBlockPos() {
        return super.getBlockPos();
    }
    @Unique
    public double getY2() {
        return super.getY();
    }
    @Unique
    public Random random() {
        return this.random;
    }
    @Unique
    public double getX2() {
        return super.getX();
    }
    @Unique
    public double getZ2() {
        return super.getZ();
    }
    @Unique
    public void setYaw(float yaw) {
        super.setYaw(yaw);
    }
    @Unique
    public float getYaw() {
        return super.getYaw();
    }
    @Unique
    public void setPitch(float pitch) {
        super.setPitch(pitch);
    }
    @Unique
    public Vec3d getVelocity() {
        return super.getVelocity();
    }
    @Unique
    public void setVelocity(Vec3d velocity) {
        super.setVelocity(velocity);
    }
    @Unique
    public void bodyYaw(float yaw) {
        super.setBodyYaw(yaw);
    }
    @Unique
    public float bodyYaw() {
        return bodyYaw;
    }
    @Unique
    public int age() {
        return age;
    }
    @Unique
    public Box getBoundingBox2() {
        return super.getBoundingBox();
    }
    @Unique
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target);
    }
    @Unique
    public void movementType(PhantomMovementType movementType) {
        this.movementType = movementType;
    }
    @Unique
    public PhantomMovementType movementType() {
        return movementType;
    }
    @Unique
    public boolean tryAttack(Entity target) {
        return super.tryAttack(target);
    }
    @Unique
    public boolean isSilent() {
        return super.isSilent();
    }
    @Unique
    public int hurtTime() {
        return this.hurtTime;
    }
    @Unique
    public boolean isTarget(LivingEntity entity, TargetPredicate predicate) {
        return super.isTarget(entity, predicate);
    }
    @Unique
    public void headYaw(float headYaw) {
        this.headYaw = headYaw;
    }
    @Unique
    public void playSound(SoundEvent sound, float volume, float pitch) {
        super.playSound(sound, volume, pitch);
    }
    @Unique
    public void setG(LivingEntity l) {
        // this.l = l;
        b = true;
    }
}
