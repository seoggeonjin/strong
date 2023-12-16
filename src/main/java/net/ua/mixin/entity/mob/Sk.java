package net.ua.mixin.entity.mob;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import net.ua.mixin.entity.abst.AbstractSk;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SkeletonEntity.class)
public abstract class Sk extends AbstractSk {
    public Sk(EntityType<? extends AbstractSkeletonEntity> entityType, World world) {
        super(entityType, world);
    }
    @Inject(method = "getStepSound", at = @At("RETURN"), cancellable = true)
    void getStepSound(@NotNull CallbackInfoReturnable<SoundEvent> cir) {
        cir.setReturnValue(SoundEvents.ENTITY_SKELETON_STEP);
    }

    @Final
    @Shadow
    private static final TrackedData<Boolean> CONVERTING = DataTracker.registerData(Sk.class, TrackedDataHandlerRegistry.BOOLEAN);
    @Final
    @Shadow
    public static final String STRAY_CONVERSION_TIME_KEY = "StrayConversionTime";
    @Shadow
    private int inPowderSnowTime;
    @Shadow
    private int conversionTime;

    @Overwrite
    public void initDataTracker() {
        super.initDataTracker();
        this.getDataTracker().startTracking(CONVERTING, false);
    }

    @Overwrite
    public boolean isConverting() {
        return this.getDataTracker().get(CONVERTING);
    }

    @Overwrite
    public void setConverting(boolean converting) {
        this.dataTracker.set(CONVERTING, converting);
    }

    @Overwrite
    public boolean isShaking() {
        return this.isConverting();
    }

    @Overwrite
    public void tick() {
        if (!this.getWorld().isClient && this.isAlive() && !this.isAiDisabled()) {
            if (this.inPowderSnow) {
                if (this.isConverting()) {
                    --this.conversionTime;
                    if (this.conversionTime < 0) {
                        this.convertToStray();
                    }
                } else {
                    ++this.inPowderSnowTime;
                    if (this.inPowderSnowTime >= 140) {
                        this.setConversionTime(300);
                    }
                }
            } else {
                this.inPowderSnowTime = -1;
                this.setConverting(false);
            }
        }
        super.tick();
    }

    @Overwrite
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt(STRAY_CONVERSION_TIME_KEY, this.isConverting() ? this.conversionTime : -1);
    }

    @Overwrite
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains(STRAY_CONVERSION_TIME_KEY, NbtElement.NUMBER_TYPE) && nbt.getInt(STRAY_CONVERSION_TIME_KEY) > -1) {
            this.setConversionTime(nbt.getInt(STRAY_CONVERSION_TIME_KEY));
        }
    }

    @Overwrite
    private void setConversionTime(int time) {
        this.conversionTime = time;
        this.setConverting(true);
    }

    @Overwrite
    public void convertToStray() {
        this.convertTo(EntityType.WITHER, true);
        if (!this.isSilent()) {
            this.getWorld().syncWorldEvent(null, WorldEvents.SKELETON_CONVERTS_TO_STRAY, this.getBlockPos(), 0);
        }
    }

    @Overwrite
    public boolean canFreeze() {
        return false;
    }

    @Overwrite
    public SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_PIG_AMBIENT;
    }

    @Overwrite
    public SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_PIG_HURT;
    }

    @Overwrite
    public SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_PIG_DEATH;
    }

    @Overwrite
    public void dropEquipment(DamageSource source, int lootingMultiplier, boolean allowDrops) {
        CreeperEntity creeperEntity;
        super.dropEquipment(source, lootingMultiplier, allowDrops);
        Entity entity = source.getAttacker();
        if (entity instanceof CreeperEntity && (creeperEntity = (CreeperEntity)entity).shouldDropHead()) {
            creeperEntity.onHeadDropped();
            this.dropItem(Items.SKELETON_SKULL);
        }
    }
}
