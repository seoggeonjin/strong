package net.ua.mixin.entity.mob;


import com.google.common.collect.ImmutableList;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.entity.feature.SkinOverlayOwner;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.control.FlightMoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import net.ua.mixin2.inner.goals.wither.DescendAtHalfHealthGoal;
import net.ua.ips.wi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

@Mixin(WitherEntity.class)
public class Wi extends HostileEntity implements SkinOverlayOwner, RangedAttackMob, wi {
    @Unique
    private static final TrackedData<Integer> TRACKED_ENTITY_ID_1 = DataTracker.registerData(Wi.class, TrackedDataHandlerRegistry.INTEGER);
    @Unique
    private static final TrackedData<Integer> TRACKED_ENTITY_ID_2 = DataTracker.registerData(Wi.class, TrackedDataHandlerRegistry.INTEGER);
    @Unique
    private static final TrackedData<Integer> TRACKED_ENTITY_ID_3 = DataTracker.registerData(Wi.class, TrackedDataHandlerRegistry.INTEGER);
    @Unique
    private static final List<TrackedData<Integer>> TRACKED_ENTITY_IDS = ImmutableList.of(TRACKED_ENTITY_ID_1, TRACKED_ENTITY_ID_2, TRACKED_ENTITY_ID_3);
    @Unique
    private static final TrackedData<Integer> INVUL_TIMER = DataTracker.registerData(Wi.class, TrackedDataHandlerRegistry.INTEGER);
    @Unique
    private final float[] sideHeadPitches = new float[2];
    @Unique
    private final float[] sideHeadYaws = new float[2];
    @Unique
    private final float[] prevSideHeadPitches = new float[2];
    @Unique
    private final float[] prevSideHeadYaws = new float[2];
    @Unique
    private final int[] skullCooldowns = new int[2];
    @Unique
    private final int[] chargedSkullCooldowns = new int[2];
    @Unique
    private int blockBreakingCooldown;
    @Unique
    private final ServerBossBar bossBar = (ServerBossBar)new ServerBossBar(this.getDisplayName(), BossBar.Color.PURPLE, BossBar.Style.PROGRESS).setDarkenSky(true);
    @Unique
    private static final Predicate<LivingEntity> CAN_ATTACK_PREDICATE = entity -> (entity.getGroup() != EntityGroup.UNDEAD && entity.isMobOrPlayer()
            || Arrays.asList(EntityType.SKELETON, EntityType.STRAY, EntityType.WITHER_SKELETON).contains(entity.getType()));
    @Unique
    private static final TargetPredicate HEAD_TARGET_PREDICATE = TargetPredicate.createAttackable().setBaseMaxDistance(20.0).setPredicate(CAN_ATTACK_PREDICATE);

    public Wi(EntityType<? extends WitherEntity> entityType, World world) {
        super(entityType, world);
        this.moveControl = new FlightMoveControl(this, 10, false);
        this.setHealth(this.getMaxHealth());
        this.experiencePoints = 50;
    }

    @Overwrite
    public EntityNavigation createNavigation(World world) {
        BirdNavigation birdNavigation = new BirdNavigation(this, world);
        birdNavigation.setCanPathThroughDoors(false);
        birdNavigation.setCanSwim(true);
        birdNavigation.setCanEnterOpenDoors(true);
        return birdNavigation;
    }

    @Overwrite
    public void initGoals() {
        this.goalSelector.add(0, new DescendAtHalfHealthGoal(this));
        this.goalSelector.add(2, new ProjectileAttackGoal(this, 1.0, 0, 2147483647));
        this.goalSelector.add(5, new FlyGoal(this, 1.0));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(7, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, LivingEntity.class, 0, false, false, CAN_ATTACK_PREDICATE));
    }

    @Overwrite
    public void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(TRACKED_ENTITY_ID_1, 0);
        this.dataTracker.startTracking(TRACKED_ENTITY_ID_2, 0);
        this.dataTracker.startTracking(TRACKED_ENTITY_ID_3, 0);
        this.dataTracker.startTracking(INVUL_TIMER, 0);
    }

    @Overwrite
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Invul", this.getInvulnerableTimer());
    }

    @Overwrite
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.setInvulTimer(nbt.getInt("Invul"));
        if (this.hasCustomName()) {
            this.bossBar.setName(this.getDisplayName());
        }
    }

    @Overwrite
    public void setCustomName(@Nullable Text name) {
        super.setCustomName(name);
        this.bossBar.setName(this.getDisplayName());
    }

    @Overwrite
    public SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_WITHER_AMBIENT;
    }

    @Overwrite
    public SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_COW_HURT;
    }

    @Overwrite
    public SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_COW_DEATH;
    }

    @Overwrite
    public void tickMovement() {
        int j;
        int i;
        Entity entity;
        Vec3d vec3d = this.getVelocity().multiply(1.0, 0.6, 1.0);
        if (!this.getWorld().isClient && this.getTrackedEntityId(0) > 0 && (entity = this.getWorld().getEntityById(this.getTrackedEntityId(0))) != null) {
            double d = vec3d.y;
            if (this.getY() < entity.getY() || !this.shouldRenderOverlay() && this.getY() < entity.getY() + 5.0) {
                d = Math.max(0.0, d);
                d += 0.3 - d * (double)0.6f;
            }
            vec3d = new Vec3d(vec3d.x, d, vec3d.z);
            Vec3d vec3d2 = new Vec3d(entity.getX() - this.getX(), 0.0, entity.getZ() - this.getZ());
            if (vec3d2.horizontalLengthSquared() > 9.0) {
                Vec3d vec3d3 = vec3d2.normalize();
                vec3d = vec3d.add(vec3d3.x * 0.3 - vec3d.x * 0.6, 0.0, vec3d3.z * 0.3 - vec3d.z * 0.6);
            }
        }
        this.setVelocity(vec3d);
        if (vec3d.horizontalLengthSquared() > 0.05) {
            this.setYaw((float) MathHelper.atan2(vec3d.z, vec3d.x) * 57.295776f - 90.0f);
        }
        super.tickMovement();
        for (i = 0; i < 2; ++i) {
            this.prevSideHeadYaws[i] = this.sideHeadYaws[i];
            this.prevSideHeadPitches[i] = this.sideHeadPitches[i];
        }
        for (i = 0; i < 2; ++i) {
            int j2 = this.getTrackedEntityId(i + 1);
            Entity entity2 = null;
            if (j2 > 0) {
                entity2 = this.getWorld().getEntityById(j2);
            }
            if (entity2 != null) {
                double e = this.getHeadX(i + 1);
                double f = this.getHeadY(i + 1);
                double g = this.getHeadZ(i + 1);
                double h = entity2.getX() - e;
                double k = entity2.getEyeY() - f;
                double l = entity2.getZ() - g;
                double m = Math.sqrt(h * h + l * l);
                float n = (float)(MathHelper.atan2(l, h) * 57.2957763671875) - 90.0f;
                float o = (float)(-(MathHelper.atan2(k, m) * 57.2957763671875));
                this.sideHeadPitches[i] = this.getNextAngle(this.sideHeadPitches[i], o, 40.0f);
                this.sideHeadYaws[i] = this.getNextAngle(this.sideHeadYaws[i], n, 10.0f);
                continue;
            }
            this.sideHeadYaws[i] = this.getNextAngle(this.sideHeadYaws[i], this.bodyYaw, 10.0f);
        }
        boolean bl = this.shouldRenderOverlay();
        for (j = 0; j < 3; ++j) {
            double p = this.getHeadX(j);
            double q = this.getHeadY(j);
            double r = this.getHeadZ(j);
            this.getWorld().addParticle(ParticleTypes.SMOKE, p + this.random.nextGaussian() * (double)0.3f, q + this.random.nextGaussian() * (double)0.3f, r + this.random.nextGaussian() * (double)0.3f, 0.0, 0.0, 0.0);
            if (!bl || this.getWorld().random.nextInt(4) != 0) continue;
            this.getWorld().addParticle(ParticleTypes.ENTITY_EFFECT, p + this.random.nextGaussian() * (double)0.3f, q + this.random.nextGaussian() * (double)0.3f, r + this.random.nextGaussian() * (double)0.3f, 0.7f, 0.7f, 0.5);
        }
        if (this.getInvulnerableTimer() > 0) {
            for (j = 0; j < 3; ++j) {
                this.getWorld().addParticle(ParticleTypes.ENTITY_EFFECT, this.getX() + this.random.nextGaussian(), this.getY() + (double)(this.random.nextFloat() * 3.3f), this.getZ() + this.random.nextGaussian(), 0.7f, 0.7f, 0.9f);
            }
        }
    }

    @Overwrite
    public void mobTick() {
        int j;
        int i;
        if (this.getInvulnerableTimer() > 0) {
            int i2 = this.getInvulnerableTimer() - 1;
            this.bossBar.setPercent(1.0f - (float)i2 / 220.0f);
            if (i2 <= 0) {
                this.getWorld().createExplosion(this, this.getX(), this.getEyeY(), this.getZ(), 25.0f, false, World.ExplosionSourceType.MOB);
                if (!this.isSilent()) {
                    this.getWorld().syncGlobalEvent(WorldEvents.WITHER_SPAWNS, this.getBlockPos(), 0);
                }
            }
            this.setInvulTimer(i2);
            if (this.age % 10 == 0) {
                this.heal(10.0f);
            }
            return;
        }
        super.mobTick();
        for (i = 1; i < 3; ++i) {
            if (this.age < this.skullCooldowns[i - 1]) continue;
            this.skullCooldowns[i - 1] = this.age + 10 + this.random.nextInt(10);
            if (this.getWorld().getDifficulty() == Difficulty.NORMAL || this.getWorld().getDifficulty() == Difficulty.HARD) {
                int n = i - 1;
                int n2 = this.chargedSkullCooldowns[n];
                this.chargedSkullCooldowns[n] = n2 + 1;
                if (n2 > 15) {
                    double d = MathHelper.nextDouble(this.random, this.getX() - 10.0, this.getX() + 10.0);
                    double e = MathHelper.nextDouble(this.random, this.getY() - 5.0, this.getY() + 5.0);
                    double h = MathHelper.nextDouble(this.random, this.getZ() - 10.0, this.getZ() + 10.0);
                    this.shootSkullAt(i + 1, d, e, h, true);
                    this.chargedSkullCooldowns[i - 1] = 0;
                }
            }
            if ((j = this.getTrackedEntityId(i)) > 0) {
                LivingEntity livingEntity = (LivingEntity)this.getWorld().getEntityById(j);
                if (livingEntity == null || !this.canTarget(livingEntity) || this.squaredDistanceTo(livingEntity) > 900.0 || !this.canSee(livingEntity)) {
                    this.setTrackedEntityId(i, 0);
                    continue;
                }
                this.shootSkullAt(i + 1, livingEntity);
                this.skullCooldowns[i - 1] = this.age + 40 + this.random.nextInt(20);
                this.chargedSkullCooldowns[i - 1] = 0;
                continue;
            }
            List<LivingEntity> list = this.getWorld().getTargets(LivingEntity.class, HEAD_TARGET_PREDICATE, this, this.getBoundingBox().expand(20.0, 8.0, 20.0));
            if (list.isEmpty()) continue;
            LivingEntity livingEntity2 = list.get(this.random.nextInt(list.size()));
            this.setTrackedEntityId(i, livingEntity2.getId());
        }
        if (this.getTarget() != null) {
            this.setTrackedEntityId(0, this.getTarget().getId());
        } else {
            this.setTrackedEntityId(0, 0);
        }
        if (this.blockBreakingCooldown > 0) {
            --this.blockBreakingCooldown;
            if (this.blockBreakingCooldown == 0 && this.getWorld().getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
                i = MathHelper.floor(this.getY());
                j = MathHelper.floor(this.getX());
                int k = MathHelper.floor(this.getZ());
                boolean bl = false;
                for (int l = -1; l <= 1; ++l) {
                    for (int m = -1; m <= 1; ++m) {
                        for (int n = 0; n <= 3; ++n) {
                            int o = j + l;
                            int p = i + n;
                            int q = k + m;
                            BlockPos blockPos = new BlockPos(o, p, q);
                            BlockState blockState = this.getWorld().getBlockState(blockPos);
                            if (!WitherEntity.canDestroy(blockState)) continue;
                            bl = this.getWorld().breakBlock(blockPos, true, this) || bl;
                        }
                    }
                }
                if (bl) {
                    this.getWorld().syncWorldEvent(null, WorldEvents.WITHER_BREAKS_BLOCK, this.getBlockPos(), 0);
                }
            }
        }
        if (this.age % 20 == 0) {
            this.heal(1.0f);
        }
        this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());
    }

    @Inject(method = "canDestroy", at = @At("RETURN"), cancellable = true)
    private static void canDestroy(@NotNull BlockState block, @NotNull CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(!block.isAir() && !block.isIn(BlockTags.WITHER_IMMUNE));
    }

    @Overwrite
    public void onSummoned() {
        this.setInvulTimer(220);
        this.bossBar.setPercent(0.0f);
        this.setHealth(this.getMaxHealth() / 3.0f);
    }
    @Override
    public void refreshPositionAndAngles(double x, double y, double z, float yaw, float pitch) {
        super.refreshPositionAndAngles(x, y, z, yaw, pitch);
    }

    @Overwrite
    public void slowMovement(BlockState state, Vec3d multiplier) {
    }

    @Overwrite
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        this.bossBar.addPlayer(player);
    }

    @Overwrite
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        this.bossBar.removePlayer(player);
    }

    @Overwrite
    private double getHeadX(int headIndex) {
        if (headIndex <= 0) {
            return this.getX();
        }
        float f = (this.bodyYaw + (float)(180 * (headIndex - 1))) * ((float)Math.PI / 180);
        float g = MathHelper.cos(f);
        return this.getX() + (double)g * 1.3;
    }
    @Overwrite
    private double getHeadY(int headIndex) {
        if (headIndex <= 0) {
            return this.getY() + 3.0;
        }
        return this.getY() + 2.2;
    }

    @Overwrite
    private double getHeadZ(int headIndex) {
        if (headIndex <= 0) {
            return this.getZ();
        }
        float f = (this.bodyYaw + (float)(180 * (headIndex - 1))) * ((float)Math.PI / 180);
        float g = MathHelper.sin(f);
        return this.getZ() + (double)g * 1.3;
    }

    @Overwrite
    private float getNextAngle(float prevAngle, float desiredAngle, float maxDifference) {
        float f = MathHelper.wrapDegrees(desiredAngle - prevAngle);
        if (f > maxDifference) {
            f = maxDifference;
        }
        if (f < -maxDifference) {
            f = -maxDifference;
        }
        return prevAngle + f;
    }

    @Overwrite
    private void shootSkullAt(int headIndex, LivingEntity target) {
        this.shootSkullAt(headIndex, target.getX(), target.getY() + (double)target.getStandingEyeHeight() * 0.5, target.getZ(), headIndex == 0 && this.random.nextFloat() < 0.001f);
    }

    @Overwrite
    private void shootSkullAt(int headIndex, double targetX, double targetY, double targetZ, boolean charged) {
        if (!this.isSilent()) {
            this.getWorld().syncWorldEvent(null, WorldEvents.WITHER_SHOOTS, this.getBlockPos(), 0);
        }
        double d = this.getHeadX(headIndex);
        double e = this.getHeadY(headIndex);
        double f = this.getHeadZ(headIndex);
        double g = targetX - d;
        double h = targetY - e;
        double i = targetZ - f;
        WitherSkullEntity witherSkullEntity = new WitherSkullEntity(this.getWorld(), this, g, h, i);
        witherSkullEntity.setOwner(this);
        if (charged) {
            witherSkullEntity.setCharged(true);
        }
        witherSkullEntity.setPos(d, e, f);
        this.getWorld().spawnEntity(witherSkullEntity);
    }

    @Overwrite
    public void shootAt(LivingEntity target, float pullProgress) {
        this.shootSkullAt(0, target);
    }

    @Overwrite
    public boolean damage(DamageSource source, float amount) {
        Entity entity;
        if (this.isInvulnerableTo(source)) {
            return false;
        }
        if (source.isIn(DamageTypeTags.IS_PROJECTILE)) {
            return super.damage(source, amount);
        }
        if (source.isIn(DamageTypeTags.WITHER_IMMUNE_TO) || source.getAttacker() instanceof WitherEntity) {
            return false;
        }
        // if (this.getInvulnerableTimer() > 0 && !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
        //     return false;
        // }
        entity = source.getAttacker();
        if (!(entity instanceof PlayerEntity) && entity instanceof LivingEntity && ((LivingEntity) entity).getGroup() == this.getGroup()) {
            return false;
        }
        if (this.blockBreakingCooldown <= 0) {
            this.blockBreakingCooldown = 5;
        }
        int i = 0;
        while (i < this.chargedSkullCooldowns.length) {
            int n = i++;
            this.chargedSkullCooldowns[n] = this.chargedSkullCooldowns[n] + 3;
        }
        return super.damage(source, amount);
    }

    @Overwrite
    public void dropEquipment(DamageSource source, int lootingMultiplier, boolean allowDrops) {
        super.dropEquipment(source, lootingMultiplier, allowDrops);
        ItemEntity itemEntity = this.dropItem(Items.NETHER_STAR);
        ItemEntity itemEntity2 = this.dropItem(Items.DIAMOND);
        if (itemEntity != null && itemEntity2 != null) {
            itemEntity.setCovetedItem();
            itemEntity2.setCovetedItem();
        }
    }

    @Overwrite
    public void checkDespawn() {
        if (this.getWorld().getDifficulty() == Difficulty.PEACEFUL && this.isDisallowedInPeaceful()) {
            this.discard();
            return;
        }
        this.despawnCounter = 0;
    }

    @Overwrite
    public boolean addStatusEffect(StatusEffectInstance effect, @Nullable Entity source) {
        return false;
    }

    @Inject(method = "createWitherAttributes", at = @At("RETURN"), cancellable = true)
    private static void createWitherAttributes(CallbackInfoReturnable<DefaultAttributeContainer.Builder> cir) {
        cir.setReturnValue(HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 300.0).add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.6f).add(EntityAttributes.GENERIC_FLYING_SPEED, 0.6f).add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0).add(EntityAttributes.GENERIC_ARMOR, 4.0));
    }

    @Overwrite
    public float getHeadYaw(int headIndex) {
        return this.sideHeadYaws[headIndex];
    }

    @Overwrite
    public float getHeadPitch(int headIndex) {
        return this.sideHeadPitches[headIndex];
    }

    @Overwrite
    public int getInvulnerableTimer() {
        return this.dataTracker.get(INVUL_TIMER);
    }

    @Overwrite
    public void setInvulTimer(int ticks) {
        this.dataTracker.set(INVUL_TIMER, ticks);
    }

    @Overwrite
    public int getTrackedEntityId(int headIndex) {
        return this.dataTracker.get(TRACKED_ENTITY_IDS.get(headIndex));
    }

    @Overwrite
    public void setTrackedEntityId(int headIndex, int id) {
        this.dataTracker.set(TRACKED_ENTITY_IDS.get(headIndex), id);
    }

    @Overwrite
    public boolean shouldRenderOverlay() {
        return this.getHealth() <= this.getMaxHealth() / 2.0f;
    }

    @Overwrite
    public EntityGroup getGroup() {
        return EntityGroup.UNDEAD;
    }

    @Overwrite
    public boolean canStartRiding(Entity entity) {
        return false;
    }

    @Overwrite
    public boolean canUsePortals() {
        return false;
    }

    @Overwrite
    public boolean canHaveStatusEffect(StatusEffectInstance effect) {
        if (effect.getEffectType() == StatusEffects.WITHER) {
            return false;
        }
        return super.canHaveStatusEffect(effect);
    }
    public Box getBoundingBox2() {
        return this.getBoundingBox();
    }
    public void sdy(float d) {
        this.bodyYaw = d;
    }
    @Unique
    public LivingEntity getTarget() {
        return super.getTarget();
    }
}
