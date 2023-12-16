package net.ua.mixin.entity.mob;

import com.google.common.collect.Maps;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.NavigationConditions;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.raid.Raid;
import net.minecraft.world.Difficulty;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.ua.mixin2.inner.goals.vindicator.TargetGoal;
import net.ua.ips.ivi;
import net.ua.mixin2.inner.goals.vindicator.BreakDoorGoal;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;

@Mixin(VindicatorEntity.class)
public class vi extends IllagerEntity implements ivi {
    @Final
    @Shadow
    private static final String JOHNNY_KEY = "Johnny";

    @Shadow
    boolean johnny;
    public vi(EntityType<? extends VindicatorEntity> entityType, World world) {
        super(entityType, world);
    }

    @Overwrite
    public void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new BreakDoorGoal(this));
        this.goalSelector.add(2, new IllagerEntity.LongDoorInteractGoal(this));
        this.goalSelector.add(3, new RaiderEntity.PatrolApproachGoal(this, 10.0f));
        this.goalSelector.add(4, new MeleeAttackGoal(this, 1.0, false));
        this.targetSelector.add(1, new RevengeGoal(this, RaiderEntity.class).setGroupRevenge());
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, 1, true, false, null));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, SkeletonEntity.class, true));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, WitherSkeletonEntity.class, true));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, StrayEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, MerchantEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, IronGolemEntity.class, true));
        this.targetSelector.add(4, new TargetGoal(this));
        this.goalSelector.add(8, new WanderAroundGoal(this, 0.6));
        this.goalSelector.add(9, new LookAtEntityGoal(this, PlayerEntity.class, 3.0f, 1.0f));
        this.goalSelector.add(10, new LookAtEntityGoal(this, MobEntity.class, 8.0f));
    }

    @Overwrite
    public void mobTick() {
        if (!this.isAiDisabled() && NavigationConditions.hasMobNavigation(this)) {
            boolean bl = ((ServerWorld)this.getWorld()).hasRaidAt(this.getBlockPos());
            ((MobNavigation)this.getNavigation()).setCanPathThroughDoors(bl);
        }
        super.mobTick();
    }

    @Inject(method = "createVindicatorAttributes", at = @At("RETURN"), cancellable = true)
    private static void createVindicatorAttributes(CallbackInfoReturnable<DefaultAttributeContainer.Builder> cir) {
        cir.setReturnValue(HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35f).add(EntityAttributes.GENERIC_FOLLOW_RANGE, 12.0).add(EntityAttributes.GENERIC_MAX_HEALTH, 24.0).add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 10.0)
                .add(EntityAttributes.GENERIC_ATTACK_SPEED, 2147483647));
    }

    @Overwrite
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (this.johnny) {
            nbt.putBoolean(JOHNNY_KEY, true);
        }
    }

    @Overwrite
    public IllagerEntity.State getState() {
        if (this.isAttacking()) {
            return IllagerEntity.State.ATTACKING;
        }
        if (this.isCelebrating()) {
            return IllagerEntity.State.CELEBRATING;
        }
        return IllagerEntity.State.CROSSED;
    }

    @Overwrite
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains(JOHNNY_KEY, NbtElement.NUMBER_TYPE)) {
            this.johnny = nbt.getBoolean(JOHNNY_KEY);
        }
    }

    @Overwrite
    public SoundEvent getCelebratingSound() {
        return SoundEvents.ENTITY_VINDICATOR_CELEBRATE;
    }

    @Overwrite
    @Nullable
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        EntityData entityData2 = super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
        ((MobNavigation)this.getNavigation()).setCanPathThroughDoors(true);
        Random random = world.getRandom();
        this.initEquipment(random, difficulty);
        this.updateEnchantments(random, difficulty);
        return entityData2;
    }

    @Overwrite
    public void initEquipment(Random random, LocalDifficulty localDifficulty) {
        if (this.getRaid() == null) {
            this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        }
    }

    @Overwrite
    public boolean isTeammate(Entity other) {
        if (super.isTeammate(other)) {
            return true;
        }
        if (other instanceof LivingEntity && ((LivingEntity)other).getGroup() == EntityGroup.ILLAGER) {
            return this.getScoreboardTeam() == null && other.getScoreboardTeam() == null;
        }
        return false;
    }

    @Overwrite
    public void setCustomName(@Nullable Text name) {
        super.setCustomName(name);
        if (!this.johnny && name != null && name.getString().equals(JOHNNY_KEY)) {
            this.johnny = true;
        }
    }

    @Overwrite
    public SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_VINDICATOR_AMBIENT;
    }

    @Overwrite
    public SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_VINDICATOR_DEATH;
    }

    @Overwrite
    public SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_VINDICATOR_HURT;
    }

    @Overwrite
    public void addBonusForWave(int wave, boolean unused) {
        boolean bl;
        ItemStack itemStack = new ItemStack(Items.IRON_AXE);
        Raid raid = this.getRaid();
        int i = 1;
        assert raid != null;
        if (wave > raid.getMaxWaves(Difficulty.NORMAL)) {
            i = 2;
        }
        bl = this.random.nextFloat() <= raid.getEnchantmentChance();
        if (bl) {
            HashMap<Enchantment, Integer> map = Maps.newHashMap();
            map.put(Enchantments.SHARPNESS, i);
            EnchantmentHelper.set(map, itemStack);
        }
        this.equipStack(EquipmentSlot.MAINHAND, itemStack);
    }
    @Override
    public boolean hasActiveRaid() {
        super.hasActiveRaid();
        return this.getRaid() != null && this.getRaid().isActive();
    }
    @Override
    public Random random() {
        return this.random;
    }
    @Override
    public boolean johnny() {
        return johnny;
    }
    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean result = super.damage(source, amount);
        if (this.isDead()) {
            for (int i = 0; i < 30; i++) {
                LightningEntity l = new LightningEntity(EntityType.LIGHTNING_BOLT, this.getWorld());
                if (this.getTarget() != null)
                    l.setPos(this.getTarget().getX(), this.getTarget().getY(), this.getTarget().getZ());
                else
                    l.setPos(this.getX(), this.getY(), this.getZ());
                this.getWorld().spawnEntity(l);
            }
        }
        return result;
    }
    @Unique
    @Nullable
    public LivingEntity getTarget() {
        return super.getTarget();
    }
}
