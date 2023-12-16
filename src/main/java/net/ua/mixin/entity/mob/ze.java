package net.ua.mixin.entity.mob;

import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.GoatEntity;
import net.minecraft.entity.passive.OcelotEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.ua.mixin2.inner.goals.CreeperMixin.CreeperIgniteGoal3;
import net.ua.ips.ze2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.StreamSupport;

import static net.ua.Strong.log;

// import static net.ua.ucp.mixin.ZombieEntityMixin.*;

@Mixin(ZombieEntity.class)
public class ze extends HostileEntity implements ze2 {
    private static final TrackedData<Integer> FUSE_SPEED = DataTracker.registerData(ze.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> CHARGED = DataTracker.registerData(ze.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> IGNITED = DataTracker.registerData(ze.class, TrackedDataHandlerRegistry.BOOLEAN);
    public int lastFuseTime;
    public int currentFuseTime;
    public int fuseTime = 1;
    private int explosionRadius = 3;

    protected ze(net.minecraft.entity.EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target instanceof GoatEntity) {
            return;
        }
        super.setTarget(target);
    }
    public boolean tryAttack(Entity target) {
        return true;
    }
    /**
     * @author Seoggeonjin
     * @reason test
     */
    @Overwrite
    public void tick() {
        this.lastFuseTime = this.currentFuseTime;
        if (this.isAlive()) {
            int i;
            if (this.isIgnited()) {
                this.setFuseSpeed(1);
            }
            if ((i = this.getFuseSpeed()) > 0 && this.currentFuseTime == 0) {
                this.playSound(SoundEvents.ENTITY_CREEPER_PRIMED, 1.0f, 0.5f);
                this.emitGameEvent(GameEvent.PRIME_FUSE);
            }
            this.currentFuseTime += i;
            if (this.currentFuseTime < 0) {
                this.currentFuseTime = 0;
            }
            if (this.currentFuseTime >= this.fuseTime) {
                this.currentFuseTime = this.fuseTime;
                this.explode();
            }
        }
        super.tick();
    }
    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.dataTracker.set(CHARGED, nbt.getBoolean("powered"));
        if (nbt.contains("Fuse", NbtElement.NUMBER_TYPE)) {
            this.fuseTime = nbt.getShort("Fuse");
        }
        if (nbt.contains("ExplosionRadius", NbtElement.NUMBER_TYPE)) {
            this.explosionRadius = nbt.getByte("ExplosionRadius");
        }
        if (nbt.getBoolean("ignited")) {
            this.ignite();
        }
    }
    public void ignite() {
        this.dataTracker.set(IGNITED, true);
    }
    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (this.dataTracker.get(CHARGED)) {
            nbt.putBoolean("powered", true);
        }
        nbt.putShort("Fuse", (short)this.fuseTime);
        nbt.putByte("ExplosionRadius", (byte)this.explosionRadius);
        nbt.putBoolean("ignited", this.isIgnited());
    }

    public boolean isIgnited() {
        return this.dataTracker.get(IGNITED);
    }
    private void explode() {
        if (!this.getWorld().isClient) {
            float f = this.shouldRenderOverlay() ? 2.0f : 1.0f;
            this.dead = true;
            this.getWorld().createExplosion(this, this.getX(), this.getY(), this.getZ(), (float)this.explosionRadius * f, World.ExplosionSourceType.MOB);
            this.discard();
            try {
                Iterable<ServerWorld> it = Objects.requireNonNull(getServer()).getWorlds();
                StreamSupport.stream(it.spliterator(), false).forEach(e -> e.createExplosion(this, this.getX(), this.getY(), this.getZ(), (float) this.explosionRadius * f, World.ExplosionSourceType.MOB));
            } catch (NullPointerException ignored) {
                log("NullPointerException");
            }
            this.spawnEffectsCloud();
        }
    }

    private void spawnEffectsCloud() {
        Collection<StatusEffectInstance> collection = this.getStatusEffects();
        if (!collection.isEmpty()) {
            AreaEffectCloudEntity areaEffectCloudEntity = getAreaEffectCloudEntity();
            for (StatusEffectInstance statusEffectInstance : collection) {
                areaEffectCloudEntity.addEffect(new StatusEffectInstance(statusEffectInstance));
            }
            this.getWorld().spawnEntity(areaEffectCloudEntity);
        }
    }

    @NotNull
    private AreaEffectCloudEntity getAreaEffectCloudEntity() {
        AreaEffectCloudEntity areaEffectCloudEntity = new AreaEffectCloudEntity(this.getWorld(), this.getX(), this.getY(), this.getZ());
        areaEffectCloudEntity.setRadius(2.5f);
        areaEffectCloudEntity.setRadiusOnUse(-0.5f);
        areaEffectCloudEntity.setWaitTime(10);
        areaEffectCloudEntity.setDuration(areaEffectCloudEntity.getDuration() / 2);
        areaEffectCloudEntity.setRadiusGrowth(-areaEffectCloudEntity.getRadius() / (float)areaEffectCloudEntity.getDuration());
        return areaEffectCloudEntity;
    }

    public boolean shouldRenderOverlay() {
        // return this.dataTracker.get(CHARGED);
        return true;
    }

    @Inject(method = "initDataTracker", at = @At("HEAD"), cancellable = true)
    public void initDataTracker(CallbackInfo ci) {
        this.dataTracker.startTracking(FUSE_SPEED, -1);
        this.dataTracker.startTracking(CHARGED, false);
        this.dataTracker.startTracking(IGNITED, false);
        super.initDataTracker();
        ci.cancel();
    }

    @Inject(method = "initGoals", at = @At("HEAD"), cancellable = true)
    private void initGoals(CallbackInfo ci) {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new CreeperIgniteGoal3(this));
        this.goalSelector.add(3, new FleeEntityGoal<>(this, OcelotEntity.class, 6.0f, 1.0, 1.2));
        this.goalSelector.add(3, new FleeEntityGoal<>(this, CatEntity.class, 6.0f, 1.0, 1.2));
        this.goalSelector.add(4, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(6, new LookAroundGoal(this));
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        this.targetSelector.add(2, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, SkeletonEntity.class, true));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, WitherSkeletonEntity.class, true));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, StrayEntity.class, true));
        ci.cancel();
    }

    @Override
    public void onStruckByLightning(ServerWorld world, LightningEntity lightning) {
        super.onStruckByLightning(world, lightning);
        this.dataTracker.set(CHARGED, true);
    }

    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        if (itemStack.isIn(ItemTags.CREEPER_IGNITERS)) {
            SoundEvent soundEvent = itemStack.isOf(Items.FIRE_CHARGE) ? SoundEvents.ITEM_FIRECHARGE_USE : SoundEvents.ITEM_FLINTANDSTEEL_USE;
            this.getWorld().playSound(player, this.getX(), this.getY(), this.getZ(), soundEvent, this.getSoundCategory(), 1.0f, this.random.nextFloat() * 0.4f + 0.8f);
            if (!this.getWorld().isClient) {
                this.ignite();
                if (!itemStack.isDamageable()) {
                    itemStack.decrement(1);
                } else {
                    itemStack.damage(1, player, p -> p.sendToolBreakStatus(hand));
                }
            }
            return ActionResult.success(this.getWorld().isClient);
        }
        if (itemStack.isIn(ItemTags.TOOLS) || itemStack.isIn(ItemTags.SHOVELS) || itemStack.isIn(ItemTags.AXES) || itemStack.isIn(ItemTags.PICKAXES) || itemStack.isIn(ItemTags.SWORDS)) {
            if (!this.dataTracker.get(CHARGED)){
                if (!this.getWorld().isClient) {
                    this.dataTracker.set(CHARGED, true);
                    if (!itemStack.isDamageable()) {
                        itemStack.decrement(1);
                    } else {
                        itemStack.damage(1, player, p -> p.sendToolBreakStatus(hand));
                    }
                }
                return ActionResult.success(this.getWorld().isClient);
            } else {
                return ActionResult.PASS;
            }
        }
        return super.interactMob(player, hand);
    }

    @Override
    public int getFuseSpeed() {
        return this.dataTracker.get(FUSE_SPEED);
    }

    @Override
    public void setFuseSpeed(int fuseSpeed) {
        this.dataTracker.set(FUSE_SPEED, fuseSpeed);
    }
    @Override
    public int getSafeFallDistance() {
        if (this.getTarget() == null) {
            return 3;
        }
        int h = (int) this.getHealth();
        return h + 2;
    }
    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        boolean bl = super.handleFallDamage(fallDistance, damageMultiplier, damageSource);
        this.currentFuseTime += (int)(fallDistance * 1.5f);
        if (this.currentFuseTime > this.fuseTime - 5) {
            this.currentFuseTime = this.fuseTime - 5;
        }
        return bl;
    }
    @Override
    public double squaredDistanceTo(Entity entity) {
        return super.squaredDistanceTo(entity);
    }
    @Override
    public MobVisibilityCache getVisibilityCache() {
        return super.getVisibilityCache();
    }
    @Override
    public EntityNavigation getNavigation() {
        return super.getNavigation();
    }
    @Override
    public LivingEntity getTarget() {
        return super.getTarget();
    }
    @Overwrite
    public boolean isBaby() {
        return false;
    }
    @Overwrite
    public void setBaby(boolean baby) {
    }
    @Overwrite
    public void onTrackedDataSet(TrackedData<?> data) {
        super.onTrackedDataSet(data);
    }
    @Overwrite
    public boolean isConvertingInWater() {
        return false;
    }
    @Overwrite
    private void setTicksUntilWaterConversion(int ticksUntilWaterConversion) {
    }
    @Override
    public boolean isAffectedByDaylight() {
        return false;
    }
    @Overwrite
    public static DefaultAttributeContainer.Builder createZombieAttributes() {
        return HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_FOLLOW_RANGE, 2147483647).add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.23f).add(EntityAttributes.GENERIC_ARMOR, 2.0).add(EntityAttributes.ZOMBIE_SPAWN_REINFORCEMENTS);
    }
}
