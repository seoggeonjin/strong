package net.ua.mixin.entity.abst;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Objects;

@Mixin(PersistentProjectileEntity.class)
public abstract class ppe extends ProjectileEntity {
    @Final
    @Shadow
    private static final TrackedData<Byte> PROJECTILE_FLAGS = DataTracker.registerData(ppe.class, TrackedDataHandlerRegistry.BYTE);
    @Final
    @Shadow
    private static final TrackedData<Byte> PIERCE_LEVEL = DataTracker.registerData(ppe.class, TrackedDataHandlerRegistry.BYTE);
    @Final
    @Shadow
    private static final int CRITICAL_FLAG = 1;
    @Final
    @Shadow
    private static final int NO_CLIP_FLAG = 2;
    @Final
    @Shadow
    private static final int SHOT_FROM_CROSSBOW_FLAG = 4;
    @Shadow
    @Nullable
    private BlockState inBlockState;
    @Shadow
    protected boolean inGround;
    @Shadow
    protected int inGroundTime;
    @Shadow
    public net.minecraft.entity.projectile.PersistentProjectileEntity.PickupPermission pickupType = net.minecraft.entity.projectile.PersistentProjectileEntity.PickupPermission.DISALLOWED;
    @Shadow
    public int shake;
    @Shadow
    private int life;
    @Shadow
    private double damage = 2.0;
    @Shadow
    private int punch;
    @Shadow
    private SoundEvent sound = this.getHitSound();
    @Nullable
    @Shadow
    private IntOpenHashSet piercedEntities;
    @Nullable
    @Shadow
    private List<Entity> piercingKilledEntities;

    public ppe(EntityType<? extends ppe> entityType, World world) {
        super(entityType, world);
    }

    public ppe(EntityType<? extends ppe> type, double x, double y, double z, World world) {
        this(type, world);
        this.setPosition(x, y, z);
    }

    public ppe(EntityType<? extends ppe> type, LivingEntity owner, World world) {
        this(type, owner.getX(), owner.getEyeY() - (double)0.1f, owner.getZ(), world);
        this.setOwner(owner);
        if (owner instanceof PlayerEntity) {
            this.pickupType = net.minecraft.entity.projectile.PersistentProjectileEntity.PickupPermission.ALLOWED;
        }
    }

    @Overwrite
    public void setSound(SoundEvent sound) {
        this.sound = sound;
    }

    @Overwrite
    public boolean shouldRender(double distance) {
        double d = this.getBoundingBox().getAverageSideLength() * 10.0;
        if (Double.isNaN(d)) {
            d = 1.0;
        }
        return distance < (d *= 64.0 * net.minecraft.entity.projectile.PersistentProjectileEntity.getRenderDistanceMultiplier()) * d;
    }

    @Overwrite
    public void initDataTracker() {
        this.dataTracker.startTracking(PROJECTILE_FLAGS, (byte)0);
        this.dataTracker.startTracking(PIERCE_LEVEL, (byte)0);
    }

    @Overwrite
    public void setVelocity(double x, double y, double z, float speed, float divergence) {
        super.setVelocity(x, y, z, speed, divergence);
        this.life = 0;
    }

    @Overwrite
    public void updateTrackedPositionAndAngles(double x, double y, double z, float yaw, float pitch, int interpolationSteps) {
        this.setPosition(x, y, z);
        this.setRotation(yaw, pitch);
    }

    @Overwrite
    public void setVelocityClient(double x, double y, double z) {
        super.setVelocityClient(x, y, z);
        this.life = 0;
    }

    @Overwrite
    public void tick() {
        Vec3d vec3d2;
        VoxelShape voxelShape;
        super.tick();
        boolean bl = this.isNoClip();
        Vec3d vec3d = this.getVelocity();
        if (this.prevPitch == 0.0f && this.prevYaw == 0.0f) {
            double d = vec3d.horizontalLength();
            this.setYaw((float)(MathHelper.atan2(vec3d.x, vec3d.z) * 57.2957763671875));
            this.setPitch((float)(MathHelper.atan2(vec3d.y, d) * 57.2957763671875));
            this.prevYaw = this.getYaw();
            this.prevPitch = this.getPitch();
        }
        BlockPos blockPos = this.getBlockPos();
        BlockState blockState = this.getWorld().getBlockState(blockPos);
        if (!(blockState.isAir() || bl || (voxelShape = blockState.getCollisionShape(this.getWorld(), blockPos)).isEmpty())) {
            vec3d2 = this.getPos();
            for (Box box : voxelShape.getBoundingBoxes()) {
                if (!box.offset(blockPos).contains(vec3d2)) continue;
                this.inGround = true;
                break;
            }
        }
        if (this.shake > 0) {
            --this.shake;
        }
        if (this.isTouchingWaterOrRain() || blockState.isOf(Blocks.POWDER_SNOW)) {
            this.extinguish();
        }
        if (this.inGround && !bl) {
            if (this.inBlockState != blockState && this.shouldFall()) {
                this.fall();
            } else if (!this.getWorld().isClient) {
                this.age();
            }
            ++this.inGroundTime;
            return;
        }
        this.inGroundTime = 0;
        Vec3d vec3d3 = this.getPos();
        vec3d2 = vec3d3.add(vec3d);
        HitResult hitResult = this.getWorld().raycast(new RaycastContext(vec3d3, vec3d2, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
        if (hitResult.getType() != HitResult.Type.MISS) {
            vec3d2 = hitResult.getPos();
        }
        while (!this.isRemoved()) {
            EntityHitResult entityHitResult = this.getEntityCollision(vec3d3, vec3d2);
            if (entityHitResult != null) {
                hitResult = entityHitResult;
            }
            if (hitResult != null && hitResult.getType() == HitResult.Type.ENTITY) {
                assert hitResult instanceof EntityHitResult;
                Entity entity = ((EntityHitResult)hitResult).getEntity();
                Entity entity2 = this.getOwner();
                if (entity instanceof PlayerEntity && entity2 instanceof PlayerEntity && !((PlayerEntity)entity2).shouldDamagePlayer((PlayerEntity)entity)) {
                    hitResult = null;
                    entityHitResult = null;
                }
            }
            if (hitResult != null && !bl) {
                this.onCollision(hitResult);
                this.velocityDirty = true;
            }
            if (entityHitResult == null || this.getPierceLevel() <= 0) break;
            hitResult = null;
        }
        vec3d = this.getVelocity();
        double e = vec3d.x;
        double f = vec3d.y;
        double g = vec3d.z;
        if (this.isCritical()) {
            for (int i = 0; i < 4; ++i) {
                this.getWorld().addParticle(ParticleTypes.CRIT, this.getX() + e * (double)i / 4.0, this.getY() + f * (double)i / 4.0, this.getZ() + g * (double)i / 4.0, -e, -f + 0.2, -g);
            }
        }
        double h = this.getX() + e;
        double j = this.getY() + f;
        double k = this.getZ() + g;
        double l = vec3d.horizontalLength();
        if (bl) {
            this.setYaw((float)(MathHelper.atan2(-e, -g) * 57.2957763671875));
        } else {
            this.setYaw((float)(MathHelper.atan2(e, g) * 57.2957763671875));
        }
        this.setPitch((float)(MathHelper.atan2(f, l) * 57.2957763671875));
        this.setPitch(net.minecraft.entity.projectile.PersistentProjectileEntity.updateRotation(this.prevPitch, this.getPitch()));
        this.setYaw(net.minecraft.entity.projectile.PersistentProjectileEntity.updateRotation(this.prevYaw, this.getYaw()));
        float m = 0.99f;
        if (this.isTouchingWater()) {
            for (int o = 0; o < 4; ++o) {
                this.getWorld().addParticle(ParticleTypes.BUBBLE, h - e * 0.25, j - f * 0.25, k - g * 0.25, e, f, g);
            }
            m = this.getDragInWater();
        }
        this.setVelocity(vec3d.multiply(m));
        if (!this.hasNoGravity() && !bl) {
            Vec3d vec3d4 = this.getVelocity();
            this.setVelocity(vec3d4.x, vec3d4.y - (double)0.05f, vec3d4.z);
        }
        this.setPosition(h, j, k);
        this.checkBlockCollision();
    }
    @Overwrite
    public boolean shouldFall() {
        return this.inGround && this.getWorld().isSpaceEmpty(new Box(this.getPos(), this.getPos()).expand(0.06));
    }
    @Overwrite
    public void fall() {
        this.inGround = false;
        Vec3d vec3d = this.getVelocity();
        this.setVelocity(vec3d.multiply(this.random.nextFloat() * 0.2f, this.random.nextFloat() * 0.2f, this.random.nextFloat() * 0.2f));
        this.life = 0;
    }

    @Overwrite
    public void move(MovementType movementType, Vec3d movement) {
        super.move(movementType, movement);
        if (movementType != MovementType.SELF && this.shouldFall()) {
            this.fall();
        }
    }
    @Overwrite
    public void age() {
        this.discard();
    }
    @Overwrite
    public void clearPiercingStatus() {
        if (this.piercingKilledEntities != null) {
            this.piercingKilledEntities.clear();
        }
        if (this.piercedEntities != null) {
            this.piercedEntities.clear();
        }
    }

    @Overwrite
    public void onEntityHit(EntityHitResult entityHitResult) {
        DamageSource damageSource;
        Entity entity2;
        super.onEntityHit(entityHitResult);
        Entity entity = entityHitResult.getEntity();
        float f = (float)this.getVelocity().length();
        int i = MathHelper.ceil(MathHelper.clamp((double)f * this.damage, 0.0, 2.147483647E9));
        if (this.getPierceLevel() > 0) {
            if (this.piercedEntities == null) {
                this.piercedEntities = new IntOpenHashSet(5);
            }
            if (this.piercingKilledEntities == null) {
                this.piercingKilledEntities = Lists.newArrayListWithCapacity(5);
            }
            if (this.piercedEntities.size() < this.getPierceLevel() + 1) {
                this.piercedEntities.add(entity.getId());
            } else {
                this.discard();
                return;
            }
        }
        if (this.isCritical()) {
            long l = this.random.nextInt(i / 2 + 2);
            i = (int) Math.min(l + (long)i, Integer.MAX_VALUE);
        }
        if ((entity2 = this.getOwner()) == null) {
            damageSource = this.getDamageSources().arrow((PersistentProjectileEntity) (Object) this, this);
        } else {
            damageSource = this.getDamageSources().arrow((PersistentProjectileEntity) (Object) this, entity2);
            if (entity2 instanceof LivingEntity) {
                ((LivingEntity)entity2).onAttacking(entity);
            }
        }
        boolean bl = entity.getType() == EntityType.ENDERMAN;
        int j = entity.getFireTicks();
        if (this.isOnFire() && !bl) {
            entity.setOnFireFor(5);
        }
        if (entity.damage(damageSource, i)) {
            if (bl) {
                return;
            }
            if (entity instanceof LivingEntity livingEntity) {
                if (!this.getWorld().isClient && this.getPierceLevel() <= 0) {
                    livingEntity.setStuckArrowCount(livingEntity.getStuckArrowCount() + 1);
                }
                if (this.punch > 0) {
                    double d = Math.max(0.0, 1.0 - livingEntity.getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE));
                    Vec3d vec3d = this.getVelocity().multiply(1.0, 0.0, 1.0).normalize().multiply((double)this.punch * 0.6 * d);
                    if (vec3d.lengthSquared() > 0.0) {
                        livingEntity.addVelocity(vec3d.x, 0.1, vec3d.z);
                    }
                }
                if (!this.getWorld().isClient && entity2 instanceof LivingEntity) {
                    EnchantmentHelper.onUserDamaged(livingEntity, entity2);
                    EnchantmentHelper.onTargetDamaged((LivingEntity)entity2, livingEntity);
                }
                this.onHit(livingEntity);
                if (livingEntity != entity2 && livingEntity instanceof PlayerEntity && entity2 instanceof ServerPlayerEntity && !this.isSilent()) {
                    ((ServerPlayerEntity)entity2).networkHandler.sendPacket(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.PROJECTILE_HIT_PLAYER, GameStateChangeS2CPacket.DEMO_OPEN_SCREEN));
                }
                if (!entity.isAlive() && this.piercingKilledEntities != null) {
                    this.piercingKilledEntities.add(livingEntity);
                }
                if (!this.getWorld().isClient && entity2 instanceof ServerPlayerEntity serverPlayerEntity) {
                    if (this.piercingKilledEntities != null && this.isShotFromCrossbow()) {
                        Criteria.KILLED_BY_CROSSBOW.trigger(serverPlayerEntity, this.piercingKilledEntities);
                    } else if (!entity.isAlive() && this.isShotFromCrossbow()) {
                        Criteria.KILLED_BY_CROSSBOW.trigger(serverPlayerEntity, List.of(entity));
                    }
                }
            }
            this.playSound(this.sound, 1.0f, 1.2f / (this.random.nextFloat() * 0.2f + 0.9f));
            if (this.getPierceLevel() <= 0) {
                this.discard();
            }
        } else {
            entity.setFireTicks(j);
            this.setVelocity(this.getVelocity().multiply(-0.1));
            this.setYaw(this.getYaw() + 180.0f);
            this.prevYaw += 180.0f;
            if (!this.getWorld().isClient && this.getVelocity().lengthSquared() < 1.0E-7) {
                if (this.pickupType == net.minecraft.entity.projectile.PersistentProjectileEntity.PickupPermission.ALLOWED) {
                    this.dropStack(this.asItemStack(), 0.1f);
                }
                this.discard();
            }
        }
    }

    @Overwrite
    public void onBlockHit(BlockHitResult blockHitResult) {
        this.inBlockState = this.getWorld().getBlockState(blockHitResult.getBlockPos());
        super.onBlockHit(blockHitResult);
        Vec3d vec3d = blockHitResult.getPos().subtract(this.getX(), this.getY(), this.getZ());
        this.setVelocity(vec3d);
        Vec3d vec3d2 = vec3d.normalize().multiply(0.05f);
        this.setPos(this.getX() - vec3d2.x, this.getY() - vec3d2.y, this.getZ() - vec3d2.z);
        this.playSound(this.getSound(), 1.0f, 1.2f / (this.random.nextFloat() * 0.2f + 0.9f));
        this.inGround = true;
        this.shake = 7;
        this.setCritical(false);
        this.setPierceLevel((byte)0);
        this.setSound(SoundEvents.ENTITY_ARROW_HIT);
        this.setShotFromCrossbow(false);
        this.clearPiercingStatus();
    }
    @Overwrite
    public SoundEvent getHitSound() {
        return SoundEvents.ENTITY_ARROW_HIT;
    }
    @Overwrite
    public final SoundEvent getSound() {
        return this.sound;
    }
    @Overwrite
    public void onHit(LivingEntity target) {
    }

    @Nullable
    @Overwrite
    public EntityHitResult getEntityCollision(Vec3d currentPosition, Vec3d nextPosition) {
        return ProjectileUtil.getEntityCollision(this.getWorld(), this, currentPosition, nextPosition, this.getBoundingBox().stretch(this.getVelocity()).expand(1.0), this::canHit);
    }

    @Overwrite
    public boolean canHit(Entity entity) {
        return super.canHit(entity) && (this.piercedEntities == null || !this.piercedEntities.contains(entity.getId()));
    }

    @Overwrite
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putShort("life", (short)this.life);
        if (this.inBlockState != null) {
            nbt.put("inBlockState", NbtHelper.fromBlockState(this.inBlockState));
        }
        nbt.putByte("shake", (byte)this.shake);
        nbt.putBoolean("inGround", this.inGround);
        nbt.putByte("pickup", (byte)this.pickupType.ordinal());
        nbt.putDouble("damage", this.damage);
        nbt.putBoolean("crit", this.isCritical());
        nbt.putByte("PierceLevel", this.getPierceLevel());
        nbt.putString("SoundEvent", Objects.requireNonNull(Registries.SOUND_EVENT.getId(this.sound)).toString());
        nbt.putBoolean("ShotFromCrossbow", this.isShotFromCrossbow());
    }

    @Overwrite
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.life = nbt.getShort("life");
        if (nbt.contains("inBlockState", NbtElement.COMPOUND_TYPE)) {
            this.inBlockState = NbtHelper.toBlockState(this.getWorld().createCommandRegistryWrapper(RegistryKeys.BLOCK), nbt.getCompound("inBlockState"));
        }
        this.shake = nbt.getByte("shake") & 0xFF;
        this.inGround = nbt.getBoolean("inGround");
        if (nbt.contains("damage", NbtElement.NUMBER_TYPE)) {
            this.damage = nbt.getDouble("damage");
        }
        this.pickupType = net.minecraft.entity.projectile.PersistentProjectileEntity.PickupPermission.fromOrdinal(nbt.getByte("pickup"));
        this.setCritical(nbt.getBoolean("crit"));
        this.setPierceLevel(nbt.getByte("PierceLevel"));
        if (nbt.contains("SoundEvent", NbtElement.STRING_TYPE)) {
            this.sound = Registries.SOUND_EVENT.getOrEmpty(new Identifier(nbt.getString("SoundEvent"))).orElse(this.getHitSound());
        }
        this.setShotFromCrossbow(nbt.getBoolean("ShotFromCrossbow"));
    }

    @Overwrite
    public void setOwner(@Nullable Entity entity) {
        super.setOwner(entity);
        if (entity instanceof PlayerEntity) {
            this.pickupType = ((PlayerEntity)entity).getAbilities().creativeMode ? net.minecraft.entity.projectile.PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY : net.minecraft.entity.projectile.PersistentProjectileEntity.PickupPermission.ALLOWED;
        }
    }

    @Overwrite
    public void onPlayerCollision(PlayerEntity player) {
        if (this.getWorld().isClient || !this.inGround && !this.isNoClip() || this.shake > 0) {
            return;
        }
        if (this.tryPickup(player)) {
            player.sendPickup(this, 1);
            this.discard();
        }
    }
    @Overwrite
    public boolean tryPickup(PlayerEntity player) {
        return switch (this.pickupType) {
            case ALLOWED -> player.getInventory().insertStack(this.asItemStack());
            case CREATIVE_ONLY -> player.getAbilities().creativeMode;
            default -> false;
        };
    }
    @Overwrite
    public ItemStack asItemStack() {
        return ((PersistentProjectileEntity) (Object) this).getItemStack().copy();
    }

    @Overwrite
    public Entity.MoveEffect getMoveEffect() {
        return Entity.MoveEffect.NONE;
    }
    @Overwrite
    public void setDamage(double damage) {
        this.damage = damage;
    }
    @Overwrite
    public double getDamage() {
        return this.damage;
    }
    @Overwrite
    public void setPunch(int punch) {
        this.punch = punch;
    }
    @Overwrite
    public int getPunch() {
        return this.punch;
    }

    @Overwrite
    public boolean isAttackable() {
        return false;
    }

    @Overwrite
    public float getEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return 0.13f;
    }
    @Overwrite
    public void setCritical(boolean critical) {
        this.setProjectileFlag(CRITICAL_FLAG, critical);
    }
    @Overwrite
    public void setPierceLevel(byte level) {
        this.dataTracker.set(PIERCE_LEVEL, level);
    }
    @Overwrite
    public void setProjectileFlag(int index, boolean flag) {
        byte b = this.dataTracker.get(PROJECTILE_FLAGS);
        if (flag) {
            this.dataTracker.set(PROJECTILE_FLAGS, (byte)(b | index));
        } else {
            this.dataTracker.set(PROJECTILE_FLAGS, (byte)(b & ~index));
        }
    }
    @Overwrite
    public boolean isCritical() {
        byte b = this.dataTracker.get(PROJECTILE_FLAGS);
        return (b & 1) != 0;
    }
    @Overwrite
    public boolean isShotFromCrossbow() {
        byte b = this.dataTracker.get(PROJECTILE_FLAGS);
        return (b & 4) != 0;
    }
    @Overwrite
    public byte getPierceLevel() {
        return this.dataTracker.get(PIERCE_LEVEL);
    }
    @Overwrite
    public void applyEnchantmentEffects(LivingEntity entity, float damageModifier) {
        int i = EnchantmentHelper.getEquipmentLevel(Enchantments.POWER, entity);
        int j = EnchantmentHelper.getEquipmentLevel(Enchantments.PUNCH, entity);
        this.setDamage((double)(damageModifier * 2.0f) + this.random.nextTriangular((double)this.getWorld().getDifficulty().getId() * 0.11, 0.57425));
        if (i > 0) {
            this.setDamage(this.getDamage() + (double)i * 0.5 + 0.5);
        }
        if (j > 0) {
            this.setPunch(j);
        }
        if (EnchantmentHelper.getEquipmentLevel(Enchantments.FLAME, entity) > 0) {
            this.setOnFireFor(100);
        }
    }
    @Overwrite
    public float getDragInWater() {
        return 0.6f;
    }
    @Overwrite
    public void setNoClip(boolean noClip) {
        this.noClip = noClip;
        this.setProjectileFlag(NO_CLIP_FLAG, noClip);
    }
    @Overwrite
    public boolean isNoClip() {
        if (!this.getWorld().isClient) {
            return this.noClip;
        }
        return (this.dataTracker.get(PROJECTILE_FLAGS) & 2) != 0;
    }
    @Overwrite
    public void setShotFromCrossbow(boolean shotFromCrossbow) {
        this.setProjectileFlag(SHOT_FROM_CROSSBOW_FLAG, shotFromCrossbow);
    }
}