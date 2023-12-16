package net.ua.mixin.entity.animal;

import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CowEntity.class)
public class Cow extends PathAwareEntity implements Monster, Targeter {
    public Cow(EntityType<? extends CowEntity> entityType, World world) {
        super(entityType, world);
    }

    @Overwrite
    public void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 1.0, 0.0f));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 1.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.targetSelector.add(2, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true, false));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, SkeletonEntity.class, true));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, WitherSkeletonEntity.class, true));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, StrayEntity.class, true));
    }

    @Inject(method = "createCowAttributes", at = @At("RETURN"), cancellable = true)
    private static void createCowAttributes(CallbackInfoReturnable<DefaultAttributeContainer.Builder> cir) {
        cir.setReturnValue(MobEntity.createMobAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 60.0).add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.20000000298023224).add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 15)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 0));
    }

    @Overwrite
    public SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_COW_AMBIENT;
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
    public void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.ENTITY_COW_STEP, 0.15F, 1.0F);
    }

    @Overwrite
    public float getSoundVolume() {
        return 0.4F;
    }

    @Overwrite
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        return super.interactMob(player, hand);
    }

    @Nullable
    @Overwrite
    public CowEntity createChild(ServerWorld serverWorld, PassiveEntity passiveEntity) {
        return EntityType.COW.create(serverWorld);
    }

    @Overwrite
    public float getActiveEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return this.isBaby() ? dimensions.height * 0.95F : 1.3F;
    }

    @Overwrite
    public Vector3f getPassengerAttachmentPos(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
        return new Vector3f(0.0F, dimensions.height - 0.03125F * scaleFactor, 0.0F);
    }
    @Override
    public boolean tryAttack(Entity target) {
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
                ((LivingEntity)target).takeKnockback(g * 0.5f, MathHelper.sin(this.getYaw() * ((float)Math.PI / 180)), -MathHelper.cos(this.getYaw() * ((float)Math.PI / 180)));
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
}
