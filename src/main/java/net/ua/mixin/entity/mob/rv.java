package net.ua.mixin.entity.mob;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.*;

import java.util.List;
import java.util.function.Predicate;

@Mixin(RavagerEntity.class)
public class rv extends RaiderEntity {
    @Final
    @Shadow
    private static final Predicate<Entity> IS_NOT_RAVAGER = entity -> entity.isAlive() && !(entity instanceof rv);
    @Final
    @Shadow
    private static final double STUNNED_PARTICLE_Z_VELOCITY = 0.5725490196078431;
    @Final
    @Shadow
    private static final double STUNNED_PARTICLE_Y_VELOCITY = 0.5137254901960784;
    @Final
    @Shadow
    private static final double STUNNED_PARTICLE_X_VELOCITY = 0.4980392156862745;
    @Shadow
    private int attackTick;
    @Shadow
    private int stunTick;
    @Shadow
    private int roarTick;

    public rv(EntityType<? extends rv> entityType, World world) {
        super(entityType, world);
        this.setStepHeight(1.0f);
        this.experiencePoints = 20;
        this.setPathfindingPenalty(PathNodeType.LEAVES, 0.0f);
    }

    @Overwrite
    public void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(4, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.4));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 6.0f));
        this.goalSelector.add(10, new LookAtEntityGoal(this, MobEntity.class, 8.0f));
        this.targetSelector.add(2, new RevengeGoal(this, RaiderEntity.class).setGroupRevenge());
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, SkeletonEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, WitherSkeletonEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, StrayEntity.class, true));
        this.targetSelector.add(4, new ActiveTargetGoal<>(this, MerchantEntity.class, true, entity -> !entity.isBaby()));
        this.targetSelector.add(4, new ActiveTargetGoal<>(this, IronGolemEntity.class, true));
    }

    @Overwrite
    public void updateGoalControls() {
        boolean bl = !(this.getControllingPassenger() instanceof MobEntity) || this.getControllingPassenger().getType().isIn(EntityTypeTags.RAIDERS);
        boolean bl2 = !(this.getVehicle() instanceof BoatEntity);
        this.goalSelector.setControlEnabled(Goal.Control.MOVE, bl);
        this.goalSelector.setControlEnabled(Goal.Control.JUMP, bl && bl2);
        this.goalSelector.setControlEnabled(Goal.Control.LOOK, bl);
        this.goalSelector.setControlEnabled(Goal.Control.TARGET, bl);
    }

    @Overwrite
    public static DefaultAttributeContainer.Builder createRavagerAttributes() {
        return HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 100.0).add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.6).add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.75).add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 12.0).add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 512).add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0);
    }

    @Overwrite
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("AttackTick", this.attackTick);
        nbt.putInt("StunTick", this.stunTick);
        nbt.putInt("RoarTick", this.roarTick);
    }

    @Overwrite
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.attackTick = nbt.getInt("AttackTick");
        this.stunTick = nbt.getInt("StunTick");
        this.roarTick = nbt.getInt("RoarTick");
    }

    @Overwrite
    public SoundEvent getCelebratingSound() {
        return SoundEvents.ENTITY_RAVAGER_CELEBRATE;
    }

    @Overwrite
    public int getMaxHeadRotation() {
        return 45;
    }

    @Overwrite
    public Vector3f getPassengerAttachmentPos(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
        return new Vector3f(0.0f, dimensions.height + 0.0625f * scaleFactor, -0.0625f * scaleFactor);
    }

    @Overwrite
    public void tickMovement() {
        super.tickMovement();
        if (!this.isAlive()) {
            return;
        }
        if (this.isImmobile()) {
            this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.0);
        } else {
            double d = this.getTarget() != null ? 0.35 : 0.3;
            double e = this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).getBaseValue();
            this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(MathHelper.lerp(0.1, e, d));
        }
        if (this.horizontalCollision && this.getWorld().getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
            boolean bl = false;
            Box box = this.getBoundingBox().expand(0.2);
            for (BlockPos blockPos : BlockPos.iterate(MathHelper.floor(box.minX), MathHelper.floor(box.minY), MathHelper.floor(box.minZ), MathHelper.floor(box.maxX), MathHelper.floor(box.maxY), MathHelper.floor(box.maxZ))) {
                BlockState blockState = this.getWorld().getBlockState(blockPos);
                Block block = blockState.getBlock();
                if (!(block instanceof LeavesBlock)) continue;
                bl = this.getWorld().breakBlock(blockPos, true, this) || bl;
            }
            if (!bl && this.isOnGround()) {
                this.jump();
            }
        }
        if (this.roarTick > 0) {
            --this.roarTick;
            if (this.roarTick == 10) {
                this.roar();
            }
        }
        if (this.attackTick > 0) {
            --this.attackTick;
        }
        if (this.stunTick > 0) {
            --this.stunTick;
            this.spawnStunnedParticles();
            if (this.stunTick == 0) {
                this.playSound(SoundEvents.ENTITY_RAVAGER_ROAR, 1.0f, 1.0f);
                this.roarTick = 20;
            }
        }
    }

    @Overwrite
    public void spawnStunnedParticles() {
        if (this.random.nextInt(6) == 0) {
            double d = this.getX() - (double)this.getWidth() * Math.sin(this.bodyYaw * ((float)Math.PI / 180)) + (this.random.nextDouble() * 0.6 - 0.3);
            double e = this.getY() + (double)this.getHeight() - 0.3;
            double f = this.getZ() + (double)this.getWidth() * Math.cos(this.bodyYaw * ((float)Math.PI / 180)) + (this.random.nextDouble() * 0.6 - 0.3);
            this.getWorld().addParticle(ParticleTypes.ENTITY_EFFECT, d, e, f, STUNNED_PARTICLE_X_VELOCITY, STUNNED_PARTICLE_Y_VELOCITY, STUNNED_PARTICLE_Z_VELOCITY);
        }
    }

    @Overwrite
    public boolean isImmobile() {
        return super.isImmobile() || this.attackTick > 0 || this.stunTick > 0 || this.roarTick > 0;
    }

    @Overwrite
    public boolean canSee(Entity entity) {
        if (this.stunTick > 0 || this.roarTick > 0) {
            return false;
        }
        return super.canSee(entity);
    }

    @Overwrite
    public void knockback(LivingEntity target) {
        if (this.roarTick == 0) {
            if (this.random.nextDouble() < 0.5) {
                this.stunTick = 40;
                this.playSound(SoundEvents.ENTITY_RAVAGER_STUNNED, 1.0f, 1.0f);
                this.getWorld().sendEntityStatus(this, EntityStatuses.STUN_RAVAGER);
                target.pushAwayFrom(this);
            } else {
                this.knockBack(target);
            }
            target.velocityModified = true;
        }
    }

    @Overwrite
    public void roar() {
        if (this.isAlive()) {
            int var3_5 = 0;
            List<Entity> list = this.getWorld().getEntitiesByClass(Entity.class, this.getBoundingBox().expand(4.0), IS_NOT_RAVAGER);
            for (Entity livingEntity : list) {
                if (!(livingEntity instanceof IllagerEntity)) {
                    livingEntity.damage(this.getDamageSources().mobAttack(this), 6.0f);
                }
                this.knockBack(livingEntity);
            }
            Vec3d vec3d = this.getBoundingBox().getCenter();
            while (var3_5 < 40) {
                double d = this.random.nextGaussian() * 0.2;
                double e = this.random.nextGaussian() * 0.2;
                double f = this.random.nextGaussian() * 0.2;
                this.getWorld().addParticle(ParticleTypes.POOF, vec3d.x, vec3d.y, vec3d.z, d, e, f);
                ++var3_5;
            }
            this.emitGameEvent(GameEvent.ENTITY_ACTION);
        }
    }

    @Overwrite
    public void knockBack(Entity entity) {
        double d = entity.getX() - this.getX();
        double e = entity.getZ() - this.getZ();
        double f = Math.max(d * d + e * e, 0.001);
        entity.addVelocity(d / f * 4.0, 0.2, e / f * 4.0);
    }

    @Overwrite
    public void handleStatus(byte status) {
        if (status == EntityStatuses.PLAY_ATTACK_SOUND) {
            this.attackTick = 0;
            this.playSound(SoundEvents.ENTITY_RAVAGER_ATTACK, 1.0f, 1.0f);
        } else if (status == EntityStatuses.STUN_RAVAGER) {
            this.stunTick = 40;
        }
        super.handleStatus(status);
    }

    @Overwrite
    public int getAttackTick() {
        return this.attackTick;
    }

    @Overwrite
    public int getStunTick() {
        return this.stunTick;
    }

    @Overwrite
    public int getRoarTick() {
        return this.roarTick;
    }

    @Overwrite
    public boolean tryAttack(Entity target) {
        this.attackTick = 0;
        this.getWorld().sendEntityStatus(this, EntityStatuses.PLAY_ATTACK_SOUND);
        this.playSound(SoundEvents.ENTITY_RAVAGER_ATTACK, 1.0f, 1.0f);
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
                target.velocityDirty = true;
                ((LivingEntity)target).takeKnockback(700, MathHelper.sin(this.getYaw() * ((float)Math.PI / 180)), -MathHelper.cos(this.getYaw() * ((float)Math.PI / 180)));
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


    @Overwrite
    @Nullable
    public SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_RAVAGER_AMBIENT;
    }

    @Overwrite
    public SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_RAVAGER_HURT;
    }

    @Overwrite
    public SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_RAVAGER_DEATH;
    }

    @Overwrite
    public void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.ENTITY_RAVAGER_STEP, 0.15f, 1.0f);
    }

    @Overwrite
    public boolean canSpawn(WorldView world) {
        return !world.containsFluid(this.getBoundingBox());
    }

    @Overwrite
    public void addBonusForWave(int wave, boolean unused) {
    }

    @Overwrite
    public boolean canLead() {
        return false;
    }

    @Overwrite
    public Box getAttackBox() {
        Box box = super.getAttackBox();
        return box.contract(0.05, 0.0, 0.05);
    }
}