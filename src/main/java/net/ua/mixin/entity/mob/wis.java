package net.ua.mixin.entity.mob;

import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.ua.mixin.entity.abst.AbstractSk;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Objects;

@Mixin(WitherSkeletonEntity.class)
public class wis extends AbstractSk {
    public wis(EntityType<? extends WitherSkeletonEntity> entityType, World world) {
        super(entityType, world);
        this.setPathfindingPenalty(PathNodeType.LAVA, 8.0f);
    }

    @Overwrite
    public void initGoals() {
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.0, false));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, AbstractPiglinEntity.class, true));
        super.initGoals();
    }

    @Overwrite
    public SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_WITHER_AMBIENT;
    }

    @Overwrite
    public SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_WITHER_HURT;
    }

    @Overwrite
    public SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_WITHER_DEATH;
    }

    @Overwrite
    public SoundEvent getStepSound() {
        return SoundEvents.ENTITY_WITHER_SPAWN;
    }

    @Overwrite
    public void dropEquipment(DamageSource source, int lootingMultiplier, boolean allowDrops) {
        super.dropEquipment(source, lootingMultiplier, allowDrops);
        this.dropItem(Items.WITHER_SKELETON_SKULL);
    }

    @Overwrite
    public void initEquipment(Random random, LocalDifficulty localDifficulty) {
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
    }

    @Overwrite
    public void updateEnchantments(Random random, LocalDifficulty localDifficulty) {
    }

    @Overwrite
    @Nullable
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        EntityData entityData2 = super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
        Objects.requireNonNull(this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE)).setBaseValue(16.0);
        this.updateAttackType();
        return entityData2;
    }

    @Overwrite
    public float getActiveEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return 2.1f;
    }

    @Overwrite
    public float getUnscaledRidingOffset(Entity vehicle) {
        return -0.875f;
    }

    @Overwrite
    public boolean tryAttack(Entity target) {
        if (!super.tryAttack(target)) {
            return false;
        }
        if (target instanceof LivingEntity) {
            ((LivingEntity)target).addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 200), this);
        }
        return true;
    }

    @Overwrite
    public PersistentProjectileEntity createArrowProjectile(ItemStack arrow, float damageModifier) {
        PersistentProjectileEntity persistentProjectileEntity = super.createArrowProjectile(arrow, damageModifier);
        persistentProjectileEntity.setOnFireFor(3000);
        return persistentProjectileEntity;
    }

    @Overwrite
    public boolean canHaveStatusEffect(StatusEffectInstance effect) {
        if (effect.getEffectType() == StatusEffects.WITHER) {
            return false;
        }
        return super.canHaveStatusEffect(effect);
    }
}
