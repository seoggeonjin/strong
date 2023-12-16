package net.ua.mixin.entity.mob;

import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.StrayEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.ua.mixin.entity.abst.AbstractSk;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(StrayEntity.class)
public class str extends AbstractSk {
    public str(EntityType<? extends StrayEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "canSpawn", at = @At("RETURN"), cancellable = true)
    private static void canSpawn(EntityType<StrayEntity> type, ServerWorldAccess world, SpawnReason spawnReason, BlockPos pos, Random random, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }

    @Overwrite
    public SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_STRAY_AMBIENT;
    }

    @Overwrite
    public SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_STRAY_HURT;
    }

    @Overwrite
    public SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_STRAY_DEATH;
    }

    @Overwrite
    public SoundEvent getStepSound() {
        return SoundEvents.ENTITY_STRAY_STEP;
    }

    @Overwrite
    public PersistentProjectileEntity createArrowProjectile(ItemStack arrow, float damageModifier) {
        PersistentProjectileEntity persistentProjectileEntity = super.createArrowProjectile(arrow, damageModifier);
        if (persistentProjectileEntity instanceof ArrowEntity) {
            ((ArrowEntity)persistentProjectileEntity).addEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 3000));
        }
        return persistentProjectileEntity;
    }
    @Override
    public boolean tryAttack(Entity target) {
        if (!super.tryAttack(target)) {
            return false;
        }
        if (target instanceof LivingEntity) {
            ((LivingEntity)target).addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 3000), this);
        }
        return true;
    }
    @Override
    public void initEquipment(Random random, LocalDifficulty localDifficulty) {
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
    }
    @Nullable
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        EntityData entityData2 = super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
        Objects.requireNonNull(this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE)).setBaseValue(2147483647.0);
        this.updateAttackType();
        return entityData2;
    }
}
