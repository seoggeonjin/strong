package net.ua.mixin.entity.mob;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.control.LookControl;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.GoToWalkTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.ua.mixin2.inner.goals.blaze.ShootFireballGoal;
import net.ua.ips.bls;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BlazeEntity.class)
public class blz extends HostileEntity implements bls {
    @Shadow
    private float eyeOffset = 0.5f;
    @Shadow
    private int eyeOffsetCooldown;
    @Final
    @Shadow
    private static final TrackedData<Byte> BLAZE_FLAGS = DataTracker.registerData(blz.class, TrackedDataHandlerRegistry.BYTE);

    public blz(EntityType<? extends blz> entityType, World world) {
        super(entityType, world);
        this.setPathfindingPenalty(PathNodeType.WATER, -1.0f);
        this.setPathfindingPenalty(PathNodeType.LAVA, 8.0f);
        this.setPathfindingPenalty(PathNodeType.DANGER_FIRE, 0.0f);
        this.setPathfindingPenalty(PathNodeType.DAMAGE_FIRE, 0.0f);
        this.experiencePoints = 10;
    }

    @Overwrite
    public void initGoals() {
        this.goalSelector.add(4, new ShootFireballGoal(this));
        this.goalSelector.add(5, new GoToWalkTargetGoal(this, 1.0));
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 1.0, 0.0f));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 2147483647));
        this.goalSelector.add(8, new LookAtEntityGoal(this, SkeletonEntity.class, 2147483647));
        this.goalSelector.add(8, new LookAtEntityGoal(this, StrayEntity.class, 2147483647));
        this.goalSelector.add(8, new LookAtEntityGoal(this, WitherSkeletonEntity.class, 2147483647));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this).setGroupRevenge());
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, SkeletonEntity.class, true));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, StrayEntity.class, true));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, WitherSkeletonEntity.class, true));
    }

    @Overwrite
    public static DefaultAttributeContainer.Builder createBlazeAttributes() {
        return HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0).add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.23f).add(EntityAttributes.GENERIC_FOLLOW_RANGE, 2147483647);
    }

    @Overwrite
    public void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(BLAZE_FLAGS, (byte)0);
    }

    @Overwrite
    public SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_BLAZE_AMBIENT;
    }

    @Overwrite
    public SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_BLAZE_HURT;
    }

    @Overwrite
    public SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_BLAZE_DEATH;
    }

    @Overwrite
    public float getBrightnessAtEyes() {
        return 1.0f;
    }

    @Overwrite
    public void tickMovement() {
        if (!this.isOnGround() && this.getVelocity().y < 0.0) {
            this.setVelocity(this.getVelocity().multiply(1.0, 0.6, 1.0));
        }
        if (this.getWorld().isClient) {
            if (this.random.nextInt(24) == 0 && !this.isSilent()) {
                this.getWorld().playSound(this.getX() + 0.5, this.getY() + 0.5, this.getZ() + 0.5, SoundEvents.ENTITY_BLAZE_BURN, this.getSoundCategory(), 1.0f + this.random.nextFloat(), this.random.nextFloat() * 0.7f + 0.3f, false);
            }
            for (int i = 0; i < 2; ++i) {
                this.getWorld().addParticle(ParticleTypes.LARGE_SMOKE, this.getParticleX(0.5), this.getRandomBodyY(), this.getParticleZ(0.5), 0.0, 0.0, 0.0);
            }
        }
        super.tickMovement();
    }

    @Overwrite
    public boolean hurtByWater() {
        return true;
    }

    @Overwrite
    public void mobTick() {
        LivingEntity livingEntity;
        --this.eyeOffsetCooldown;
        if (this.eyeOffsetCooldown <= 0) {
            this.eyeOffsetCooldown = 100;
            this.eyeOffset = (float)this.random.nextTriangular(0.5, 6.891);
        }
        if ((livingEntity = this.getTarget()) != null && livingEntity.getEyeY() > this.getEyeY() + (double)this.eyeOffset && this.canTarget(livingEntity)) {
            Vec3d vec3d = this.getVelocity();
            this.setVelocity(this.getVelocity().add(0.0, ((double)0.3f - vec3d.y) * (double)0.3f, 0.0));
            this.velocityDirty = true;
        }
        super.mobTick();
    }

    @Overwrite
    public boolean isOnFire() {
        return this.isFireActive();
    }
    @Overwrite
    public boolean isFireActive() {
        return (this.dataTracker.get(BLAZE_FLAGS) & 1) != 0;
    }

    @Overwrite
    @Override
    public void setFireActive(boolean fireActive) {
        byte b = this.dataTracker.get(BLAZE_FLAGS);
        b = fireActive ? (byte)(b | 1) : (byte)(b & 0xFFFFFFFE);
        this.dataTracker.set(BLAZE_FLAGS, b);
    }
    @Override
    @Nullable
    public LivingEntity getTarget() {
        return super.getTarget();
    }
    @Override
    public boolean canTarget(LivingEntity target) {
        return super.canTarget(target);
    }
    @Override
    public MobVisibilityCache getVisibilityCache() {
        return super.getVisibilityCache();
    }
    @Override
    public double squaredDistanceTo(Entity entity) {
        return super.squaredDistanceTo(entity);
    }
    @Override
    public boolean tryAttack(Entity target) {
        return super.tryAttack(target);
    }
    @Override
    public MoveControl getMoveControl() {
        return super.getMoveControl();
    }
    @Override
    public World getWorld() {
        return super.getWorld();
    }
    @Override
    public boolean isSilent() {
        return super.isSilent();
    }
    @Override
    public BlockPos getBlockPos() {
        return super.getBlockPos();
    }
    @Override
    public Random getRandom() {
        return super.getRandom();
    }
    @Override
    public double getBodyY(double heightScale) {
        return super.getBodyY(heightScale);
    }
    @Override
    public double getX2() {
        return super.getX();
    }
    @Override
    public double getZ2() {
        return super.getZ();
    }
    @Override
    public LookControl getLookControl() {
        return super.getLookControl();
    }
    @Override
    public double getAttributeValue(EntityAttribute attribute) {
        return super.getAttributeValue(attribute);
    }
}
