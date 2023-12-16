package net.ua.mixin.entity.mob;

import com.mojang.serialization.Dynamic;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.mob.BreezeEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.DebugInfoSender;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.ua.mixin2.inner.otherwise.T2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BreezeEntity.class)
public class br extends HostileEntity {
    // br.createBreezeAttributes().build()
    @Shadow
    public AnimationState field_47269 = new AnimationState();
    @Shadow
    public AnimationState slidingAnimationState = new AnimationState();
    @Shadow
    public AnimationState inhalingAnimationState = new AnimationState();
    @Shadow
    public AnimationState shootingAnimationState = new AnimationState();
    @Shadow
    public AnimationState field_47270 = new AnimationState();
    @Shadow
    private int longJumpingParticleAddCount = 0;

    @Overwrite
    public static DefaultAttributeContainer.Builder createBreezeAttributes() {
        return MobEntity.createMobAttributes().add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.6f).add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0).add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0).add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.0);
    }

    public br(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.setPathfindingPenalty(PathNodeType.DANGER_TRAPDOOR, -1.0f);
        this.setPathfindingPenalty(PathNodeType.DAMAGE_FIRE, -1.0f);
    }

    @Overwrite
    public Brain<?> deserializeBrain(Dynamic<?> dynamic) {
        return T2.create(this.createBrainProfile().deserialize(dynamic));
    }

    @Overwrite
    public Brain<net.minecraft.entity.mob.BreezeEntity> getBrain() {
        return (Brain<BreezeEntity>) super.getBrain();
    }

    @Overwrite
    public Brain.Profile<net.minecraft.entity.mob.BreezeEntity> createBrainProfile() {
        return Brain.createProfile(T2.MEMORY_MODULES, T2.SENSORS);
    }

    @Overwrite
    public boolean canTarget(LivingEntity target) {
        return target.getType() != EntityType.BREEZE && super.canTarget(target);
    }

    @Overwrite
    public void onTrackedDataSet(TrackedData<?> data) {
        if (this.getWorld().isClient() && POSE.equals(data)) {
            this.stopAnimations();
            EntityPose entityPose = this.getPose();
            switch (entityPose) {
                case SHOOTING: {
                    this.shootingAnimationState.startIfNotRunning(this.age);
                    break;
                }
                case INHALING: {
                    this.inhalingAnimationState.startIfNotRunning(this.age);
                    break;
                }
                case SLIDING: {
                    this.slidingAnimationState.startIfNotRunning(this.age);
                }
            }
        }
        super.onTrackedDataSet(data);
    }

    @Overwrite
    public void stopAnimations() {
        this.shootingAnimationState.stop();
        this.field_47269.stop();
        this.field_47270.stop();
        this.inhalingAnimationState.stop();
        this.slidingAnimationState.stop();
    }

    @Overwrite
    public void tick() {
        switch (this.getPose()) {
            case SLIDING: {
                this.addBlockParticles(20);
                break;
            }
            case SHOOTING:
            case INHALING:
            case STANDING: {
                this.resetLongJumpingParticleAddCount().addBlockParticles(1 + this.getRandom().nextInt(1));
                break;
            }
            case LONG_JUMPING: {
                this.addLongJumpingParticles();
            }
        }
        super.tick();
    }

    @Overwrite
    public net.minecraft.entity.mob.BreezeEntity resetLongJumpingParticleAddCount() {
        this.longJumpingParticleAddCount = 0;
        return (BreezeEntity) (Object) this;
    }

    @Overwrite
    public net.minecraft.entity.mob.BreezeEntity addGustDustParticles() {
        Vec3d vec3d = this.getPos().add(0.0, 0.1f, 0.0);
        for (int i = 0; i < 20; ++i) {
            this.getWorld().addParticle(ParticleTypes.GUST_DUST, vec3d.x, vec3d.y, vec3d.z, 0.0, 0.0, 0.0);
        }
        return (BreezeEntity) (Object) this;
    }

    @Overwrite
    public void addLongJumpingParticles() {
        if (++this.longJumpingParticleAddCount > 5) {
            return;
        }
        BlockState blockState = this.getWorld().getBlockState(this.getBlockPos().down());
        Vec3d vec3d = this.getVelocity();
        Vec3d vec3d2 = this.getPos().add(vec3d).add(0.0, 0.1f, 0.0);
        for (int i = 0; i < 3; ++i) {
            this.getWorld().addParticle(new BlockStateParticleEffect(ParticleTypes.BLOCK, blockState), vec3d2.x, vec3d2.y, vec3d2.z, 0.0, 0.0, 0.0);
        }
    }

    @Overwrite
    public void addBlockParticles(int count) {
        Vec3d vec3d = this.getBoundingBox().getCenter();
        Vec3d vec3d2 = new Vec3d(vec3d.x, this.getPos().y, vec3d.z);
        BlockState blockState = this.getWorld().getBlockState(this.getBlockPos().down());
        if (blockState.getRenderType() == BlockRenderType.INVISIBLE) {
            return;
        }
        for (int i = 0; i < count; ++i) {
            this.getWorld().addParticle(new BlockStateParticleEffect(ParticleTypes.BLOCK, blockState), vec3d2.x, vec3d2.y, vec3d2.z, 0.0, 0.0, 0.0);
        }
    }

    @Overwrite
    public void playAmbientSound() {
        this.getWorld().playSoundFromEntity(this, this.getAmbientSound(), this.getSoundCategory(), 1.0f, 1.0f);
    }

    @Overwrite
    public SoundCategory getSoundCategory() {
        return SoundCategory.HOSTILE;
    }

    @Overwrite
    public SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_BREEZE_DEATH;
    }

    @Overwrite
    public SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_BREEZE_HURT;
    }

    @Overwrite
    public SoundEvent getAmbientSound() {
        return this.isOnGround() ? SoundEvents.ENTITY_BREEZE_IDLE_GROUND : SoundEvents.ENTITY_BREEZE_IDLE_AIR;
    }

    @Overwrite
    public boolean isWithinLargeRange(Vec3d pos) {
        Vec3d vec3d = this.getBlockPos().toCenterPos();
        return pos.isWithinRangeOf(vec3d, 20.0, 10.0) && !pos.isWithinRangeOf(vec3d, 8.0, 10.0);
    }

    @Overwrite
    public boolean isWithinMediumRange(Vec3d pos) {
        Vec3d vec3d = this.getBlockPos().toCenterPos();
        return pos.isWithinRangeOf(vec3d, 8.0, 10.0) && !pos.isWithinRangeOf(vec3d, 4.0, 10.0);
    }

    @Overwrite
    public boolean isWithinShortRange(Vec3d pos) {
        Vec3d vec3d = this.getBlockPos().toCenterPos();
        return pos.isWithinRangeOf(vec3d, 4.0, 10.0);
    }

    @Overwrite
    public void mobTick() {
        this.getWorld().getProfiler().push("breezeBrain");
        this.getBrain().tick((ServerWorld)this.getWorld(), (BreezeEntity) (Object) this);
        this.getWorld().getProfiler().swap("breezeActivityUpdate");
        this.getWorld().getProfiler().pop();
        super.mobTick();
    }

    @Overwrite
    public void sendAiDebugData() {
        super.sendAiDebugData();
        DebugInfoSender.sendBrainDebugData(this);
        DebugInfoSender.sendBreezeDebugData((BreezeEntity) (Object) this);
    }

    @Overwrite
    public boolean canTarget(EntityType<?> type) {
        return type == EntityType.PLAYER;
    }

    @Overwrite
    public int getMaxHeadRotation() {
        return 30;
    }

    @Overwrite
    public int getMaxLookYawChange() {
        return 25;
    }

    @Overwrite
    public double getChargeY() {
        return this.getEyeY() - 0.4;
    }

    @Overwrite
    public boolean isInvulnerableTo(DamageSource damageSource) {
        return damageSource.isIn(DamageTypeTags.BREEZE_IMMUNE_TO) || damageSource.getAttacker() instanceof net.minecraft.entity.mob.BreezeEntity || super.isInvulnerableTo(damageSource);
    }

    @Overwrite
    public double getSwimHeight() {
        return this.getStandingEyeHeight();
    }

    @Overwrite
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        if (fallDistance > 3.0f) {
            this.playSound(SoundEvents.ENTITY_BREEZE_LAND, 1.0f, 1.0f);
        }
        return super.handleFallDamage(fallDistance, damageMultiplier, damageSource);
    }

    @Overwrite
    public Entity.MoveEffect getMoveEffect() {
        return Entity.MoveEffect.EVENTS;
    }
}