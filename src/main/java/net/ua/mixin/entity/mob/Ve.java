package net.ua.mixin.entity.mob;

import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.ua.mixin2.inner.goals.vex.ctg;
import net.ua.mixin2.inner.goals.vex.lat;
import net.ua.mixin2.inner.goals.vex.tot;
import net.ua.mixin2.inner.goals.vex.vmc;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VexEntity.class)
public class Ve extends HostileEntity implements Ownable {
    @Final
    @Shadow
    public static final int field_28645 = MathHelper.ceil(3.9269907f);
    @Final
    @Shadow
    protected static final TrackedData<Byte> VEX_FLAGS = DataTracker.registerData(Ve.class, TrackedDataHandlerRegistry.BYTE);
    @Final
    @Shadow
    private static final int CHARGING_FLAG = 1;
    @Shadow
    @Nullable
    MobEntity owner;
    @Nullable
    @Shadow
    private BlockPos bounds;
    @Shadow
    private boolean alive;
    @Shadow
    private int lifeTicks;

    public Ve(EntityType<? extends VexEntity> entityType, World world) {
        super(entityType, world);
        this.moveControl = new vmc((VexEntity)(Object)this);
        this.experiencePoints = 3;
    }

    @Overwrite
    public float getActiveEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return dimensions.height - 0.28125f;
    }

    @Overwrite
    public boolean isFlappingWings() {
        return this.age % field_28645 == 0;
    }

    @Overwrite
    public void move(MovementType movementType, Vec3d movement) {
        super.move(movementType, movement);
        this.checkBlockCollision();
    }

    @Overwrite
    public void tick() {
        this.noClip = true;
        super.tick();
        this.noClip = false;
        this.setNoGravity(true);
        if (this.alive && --this.lifeTicks <= 0) {
            this.lifeTicks = 20;
            this.damage(this.getDamageSources().starve(), 1.0f);
        }
    }

    @Overwrite
    public void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(4, new ctg((VexEntity)(Object)this));
        this.goalSelector.add(8, new lat((VexEntity)(Object)this));
        this.goalSelector.add(9, new LookAtEntityGoal(this, PlayerEntity.class, 3.0f, 1.0f));
        this.goalSelector.add(10, new LookAtEntityGoal(this, MobEntity.class, 8.0f));
        this.targetSelector.add(1, new RevengeGoal(this, RaiderEntity.class).setGroupRevenge());
        this.targetSelector.add(2, new tot(this, (VexEntity)(Object)this));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Inject(method = "createVexAttributes", at = @At("RETURN"), cancellable = true)
    private static void createVexAttributes(CallbackInfoReturnable<DefaultAttributeContainer.Builder> cir) {
        cir.setReturnValue(HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 14.0).add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 30.0));
    }

    @Overwrite
    public void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(VEX_FLAGS, (byte)0);
    }

    @Overwrite
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("BoundX")) {
            this.bounds = new BlockPos(nbt.getInt("BoundX"), nbt.getInt("BoundY"), nbt.getInt("BoundZ"));
        }
        if (nbt.contains("LifeTicks")) {
            this.setLifeTicks(nbt.getInt("LifeTicks"));
        }
    }

    @Overwrite
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (this.bounds != null) {
            nbt.putInt("BoundX", this.bounds.getX());
            nbt.putInt("BoundY", this.bounds.getY());
            nbt.putInt("BoundZ", this.bounds.getZ());
        }
        if (this.alive) {
            nbt.putInt("LifeTicks", this.lifeTicks);
        }
    }

    @Overwrite
    @Nullable
    public MobEntity getOwner() {
        return this.owner;
    }

    @Nullable
    @Overwrite
    public BlockPos getBounds() {
        return this.bounds;
    }

    @Overwrite
    public void setBounds(@Nullable BlockPos bounds) {
        this.bounds = bounds;
    }

    @Overwrite
    public boolean areFlagsSet(int mask) {
        byte i = this.dataTracker.get(VEX_FLAGS);
        return (i & mask) != 0;
    }

    @Overwrite
    public void setVexFlag(int mask, boolean value) {
        int i = this.dataTracker.get(VEX_FLAGS);
        if (value) {
            i |= mask;
        } else {
            i &= ~mask;
        }
        this.dataTracker.set(VEX_FLAGS, (byte)(i & 0xFF));
    }

    @Overwrite
    public boolean isCharging() {
        return this.areFlagsSet(CHARGING_FLAG);
    }

    @Overwrite
    public void setCharging(boolean charging) {
        this.setVexFlag(CHARGING_FLAG, charging);
    }

    @Overwrite
    public void setOwner(@Nullable MobEntity owner) {
        this.owner = owner;
    }

    @Overwrite
    public void setLifeTicks(int lifeTicks) {
        this.alive = true;
        this.lifeTicks = lifeTicks;
    }

    @Overwrite
    public SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_VEX_AMBIENT;
    }

    @Overwrite
    public SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_VEX_DEATH;
    }

    @Overwrite
    public SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_VEX_HURT;
    }

    @Overwrite
    public float getBrightnessAtEyes() {
        return 1.0f;
    }

    @Overwrite
    @Nullable
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        Random random = world.getRandom();
        this.initEquipment(random, difficulty);
        this.updateEnchantments(random, difficulty);
        return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
    }

    @Overwrite
    public void initEquipment(Random random, LocalDifficulty localDifficulty) {
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        this.setEquipmentDropChance(EquipmentSlot.MAINHAND, 100.0f);
    }

    @Overwrite
    public float getUnscaledRidingOffset(Entity vehicle) {
        return 0.04f;
    }

    @Overwrite
    public Vector3f getPassengerAttachmentPos(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
        return new Vector3f(0.0f, dimensions.height - 0.0625f * scaleFactor, 0.0f);
    }
}
