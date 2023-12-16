package net.ua.mixin.entity.abst;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.BreezeEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.ua.Strong;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(LivingEntity.class)
public abstract class le extends Entity {
    @Shadow
    protected Brain<?> brain;
    @Final
    @Shadow
    private static final Logger LOGGER = LogUtils.getLogger();
    @Shadow
    private int lastAttackedTime;
    @Shadow
    public int deathTime;
    @Shadow
    public int hurtTime;
    @Final
    @Shadow
    private final Map<StatusEffect, StatusEffectInstance> activeStatusEffects = Maps.newHashMap();
    @Final
    @Shadow
    private static final String ACTIVE_EFFECTS_NBT_KEY = "active_effects";
    @Shadow
    private float absorptionAmount;
    @Final
    @Shadow
    private static final TrackedData<Float> HEALTH = DataTracker.registerData(le.class, TrackedDataHandlerRegistry.FLOAT);
    @Mutable
    @Shadow
    @Final
    private AttributeContainer attributes;

    @Shadow public abstract double getAttributeValue(RegistryEntry<EntityAttribute> attribute);

    public le(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
        Strong.log(entityType);
        l(entityType);
    }
    @Unique
    private void l(EntityType<? extends LivingEntity> entityType) {
        if (entityType == EntityType.BREEZE) {
            this.attributes = new AttributeContainer(HostileEntity.createMobAttributes().add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.6f).add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0).add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0).add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.0).build());
        } else {
            this.attributes = new AttributeContainer(DefaultAttributeRegistry.get(entityType));
        }
    }
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/attribute/DefaultAttributeRegistry;get(Lnet/minecraft/entity/EntityType;)Lnet/minecraft/entity/attribute/DefaultAttributeContainer;"))
    private DefaultAttributeContainer l2(EntityType<? extends LivingEntity> entityType) {
        if (entityType == EntityType.BREEZE) {
            return HostileEntity.createMobAttributes().add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.6f).add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0).add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0).add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.0).build();
        } else {
            return DefaultAttributeRegistry.get(entityType);
        }
    }
    // @Redirect(method = "initDataTracker()V", at = @At(value = "INVOKE", target = "Ljava/lang/Float;valueOf(F)Ljava/lang/Float;"))
    // private Float t1(float f) {
    //     this.dataTracker.startTracking(HEALTH, 1);
    //     return 1F;
    // }

    @Inject(method = "tick()V", at = @At("HEAD"))
    private void modifyGravity(CallbackInfo ci) {
        super.tick();
    }
    // @Inject(method = "getMaxHealth", at = @At("RETURN"), cancellable = true)
    // public void getMaxHealth(CallbackInfoReturnable<Float> cir) {
    //     cir.setReturnValue(40.0F);
    // }
    @Overwrite
    public void triggerItemPickedUpByEntityCriteria(ItemEntity item) {
        Entity entity = item.getOwner();
        if (entity instanceof ServerPlayerEntity) {
            Criteria.THROWN_ITEM_PICKED_UP_BY_ENTITY.trigger((ServerPlayerEntity)entity, item.getStack(), this);
        }
    }

    @Overwrite
    public final boolean canBreatheInWater() {
        return true;
    }
    /**
     * @author Seoggeonjin
     * @reason test
     */
    @Overwrite
    public boolean canTarget(EntityType<?> type) {
        return false;
    }
    /**
     * @author Seoggeonjin
     * @reason test
     */
    @Overwrite
    public float getJumpVelocity() {
        return 0.42f * this.getJumpVelocityMultiplier();
    }
    @Overwrite
    private boolean tryUseTotem(DamageSource source) {
        if (source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        ItemStack itemStack = null;
        for (Hand hand : Hand.values()) {
            ItemStack itemStack2 = this.getStackInHand(hand);
            if (!itemStack2.isOf(Items.TOTEM_OF_UNDYING)) continue;
            itemStack = itemStack2.copy();
            itemStack2.decrement(1);
            break;
        }
        if (itemStack != null) {
            LivingEntity livingEntity = (LivingEntity) (Object) this;
            if (livingEntity instanceof ServerPlayerEntity serverPlayerEntity) {
                serverPlayerEntity.incrementStat(Stats.USED.getOrCreateStat(Items.TOTEM_OF_UNDYING));
                Criteria.USED_TOTEM.trigger(serverPlayerEntity, itemStack);
                this.emitGameEvent(GameEvent.ITEM_INTERACT_FINISH);
            }
            livingEntity.setHealth(1.0f);
            livingEntity.clearStatusEffects();
            livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 900, 1));
            livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 100, 1));
            livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 800, 0));
            livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.BAD_OMEN, 999999, 255));
            livingEntity.getWorld().sendEntityStatus(livingEntity, EntityStatuses.USE_TOTEM_OF_UNDYING);
        }
        return itemStack != null;
    }
    @Overwrite
    public ItemStack getStackInHand(Hand hand) {
        if (hand == Hand.MAIN_HAND) {
            return this.getEquippedStack(EquipmentSlot.MAINHAND);
        }
        if (hand == Hand.OFF_HAND) {
            return this.getEquippedStack(EquipmentSlot.OFFHAND);
        }
        throw new IllegalArgumentException("Invalid hand " + hand);
    }
    @Overwrite
    public abstract ItemStack getEquippedStack(EquipmentSlot var1);
    @Overwrite
    public final float getMaxHealth() {
        if (((LivingEntity) (Object) this) instanceof BreezeEntity) {
            return 30;
        } else {
            return (float)this.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH);
        }
    }
    @Overwrite
    public double getAttributeValue(EntityAttribute attribute) {
        if (((LivingEntity) (Object) this) instanceof BreezeEntity) {
            if (attribute == EntityAttributes.GENERIC_FOLLOW_RANGE) {
                return 2147483647;
            }
        }
        return this.getAttributes().getValue(attribute);
    }
    @Overwrite
    public AttributeContainer getAttributes() {
        return this.attributes;
    }
    @Overwrite
    public double getAttributeBaseValue(EntityAttribute attribute) {
        return this.getAttributes().getBaseValue(attribute);
    }
    // @Overwrite
    // public void readCustomDataFromNbt(NbtCompound nbt) {
    //     this.setAbsorptionAmountUnclamped(nbt.getFloat("AbsorptionAmount"));
    //     if (nbt.contains("Attributes", NbtElement.LIST_TYPE) && this.getWorld() != null && !this.getWorld().isClient) {
    //         this.getAttributes().readNbt(nbt.getList("Attributes", NbtElement.COMPOUND_TYPE));
    //     }
    //     if (nbt.contains("MaxHealth", NbtElement.NUMBER_TYPE)) {
    //         NbtList n = new NbtList();
    //         n.add(nbt.get("MaxHealth"));
    //         Strong.log(nbt.get("MaxHealth"));
    //         ((LivingEntity) (Object) this).getAttributes().readNbt(n);
    //     }
    //     if (nbt.contains(ACTIVE_EFFECTS_NBT_KEY, NbtElement.LIST_TYPE)) {
    //         NbtList nbtList = nbt.getList(ACTIVE_EFFECTS_NBT_KEY, NbtElement.COMPOUND_TYPE);
    //         for (int i = 0; i < nbtList.size(); ++i) {
    //             NbtCompound nbtCompound = nbtList.getCompound(i);
    //             StatusEffectInstance statusEffectInstance = StatusEffectInstance.fromNbt(nbtCompound);
    //             if (statusEffectInstance == null) continue;
    //             this.activeStatusEffects.put(statusEffectInstance.getEffectType(), statusEffectInstance);
    //         }
    //     }
    //     if (nbt.contains("Health", NbtElement.NUMBER_TYPE)) {
    //         ((LivingEntity) (Object) this).setHealth(nbt.getFloat("Health"));
    //     }
    //     this.hurtTime = nbt.getShort("HurtTime");
    //     this.deathTime = nbt.getShort("DeathTime");
    //     this.lastAttackedTime = nbt.getInt("HurtByTimestamp");
    //     if (nbt.contains("Team", NbtElement.STRING_TYPE)) {
    //         boolean bl;
    //         String string = nbt.getString("Team");
    //         Team team = this.getWorld().getScoreboard().getTeam(string);
    //         bl = team != null && this.getWorld().getScoreboard().addScoreHolderToTeam(this.getUuidAsString(), team);
    //         if (!bl) {
    //             LOGGER.warn("Unable to add mob to team \"{}\" (that team probably doesn't exist)", (Object)string);
    //         }
    //     }
    //     if (nbt.getBoolean("FallFlying")) {
    //         this.setFlag(Entity.FALL_FLYING_FLAG_INDEX, true);
    //     }
    //     if (nbt.contains("SleepingX", NbtElement.NUMBER_TYPE) && nbt.contains("SleepingY", NbtElement.NUMBER_TYPE) && nbt.contains("SleepingZ", NbtElement.NUMBER_TYPE)) {
    //         BlockPos blockPos = new BlockPos(nbt.getInt("SleepingX"), nbt.getInt("SleepingY"), nbt.getInt("SleepingZ"));
    //         ((LivingEntity) (Object) this).setSleepingPosition(blockPos);
    //         this.dataTracker.set(POSE, EntityPose.SLEEPING);
    //         if (!this.firstUpdate) {
    //             this.setPositionInBed(blockPos);
    //         }
    //     }
    //     if (nbt.contains("Brain", NbtElement.COMPOUND_TYPE)) {
    //         this.brain = this.deserializeBrain(new Dynamic<NbtElement>(NbtOps.INSTANCE, nbt.get("Brain")));
    //     }
    // }
    @Overwrite
    public void setAbsorptionAmountUnclamped(float absorptionAmount) {
        this.absorptionAmount = absorptionAmount;
    }
    @Overwrite
    private void setPositionInBed(BlockPos pos) {
        this.setPosition((double)pos.getX() + 0.5, (double)pos.getY() + 0.6875, (double)pos.getZ() + 0.5);
    }
    @Overwrite
    public Brain<?> deserializeBrain(Dynamic<?> dynamic) {
        return this.createBrainProfile().deserialize(dynamic);
    }
    @Overwrite
    public Brain.Profile<?> createBrainProfile() {
        return Brain.createProfile(ImmutableList.of(), ImmutableList.of());
    }
    @Inject(method = "readCustomDataFromNbt", at = @At("HEAD"))
    public void s(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("MaxHealth", NbtElement.NUMBER_TYPE)) {
            NbtList n = new NbtList();
            NbtCompound n2 = new NbtCompound();
            n2.putDouble("Base", nbt.getDouble("MaxHealth"));
            n2.putString("Name", "generic.max_health");
            n.add(n2);
            Strong.log(nbt.get("MaxHealth"));
            ((LivingEntity) (Object) this).getAttributes().readNbt(n);
        }
    }
}