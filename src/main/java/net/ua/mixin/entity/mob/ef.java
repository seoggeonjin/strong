package net.ua.mixin.entity.mob;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Ownable;
import net.minecraft.entity.mob.EvokerFangsEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;

@Mixin(EvokerFangsEntity.class)
public class ef extends Entity implements Ownable {
    @Shadow
    private int warmup;
    @Shadow
    private boolean startedAttack;
    @Shadow
    private int ticksLeft;
    @Shadow
    private boolean playingAnimation;
    @Nullable
    @Shadow
    private LivingEntity owner;
    @Nullable
    @Shadow
    private UUID ownerUuid;

    public ef(EntityType<? extends EvokerFangsEntity> entityType, World world) {
        super(entityType, world);
        this.ticksLeft = 22;
    }
    @Inject(method = "<init>(Lnet/minecraft/world/World;DDDFILnet/minecraft/entity/LivingEntity;)V", at = @At("RETURN"))
    public void gef(World world, double x, double y, double z, float yaw, int warmup, LivingEntity owner, CallbackInfo ci) {
        EvokerFangsEntity f = (EvokerFangsEntity) (Object) (new ef(EntityType.EVOKER_FANGS, world));
        ((ef) (Object) f).warmup = warmup;
        f.setOwner(owner);
        f.setYaw(yaw * 57.295776F);
        f.setPosition(x, y, z);
    }

    @Overwrite
    public void initDataTracker() {
    }

    @Overwrite
    public void setOwner(@Nullable LivingEntity owner) {
        this.owner = owner;
        this.ownerUuid = owner == null ? null : owner.getUuid();
    }

    @Nullable
    @Overwrite
    public LivingEntity getOwner() {
        if (this.owner == null && this.ownerUuid != null && this.getWorld() instanceof ServerWorld) {
            Entity entity = ((ServerWorld)this.getWorld()).getEntity(this.ownerUuid);
            if (entity instanceof LivingEntity) {
                this.owner = (LivingEntity)entity;
            }
        }
        return this.owner;
    }

    @Overwrite
    public void readCustomDataFromNbt(NbtCompound nbt) {
        this.warmup = nbt.getInt("Warmup");
        if (nbt.containsUuid("Owner")) {
            this.ownerUuid = nbt.getUuid("Owner");
        }
    }

    @Overwrite
    public void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("Warmup", this.warmup);
        if (this.ownerUuid != null) {
            nbt.putUuid("Owner", this.ownerUuid);
        }
    }

    @Overwrite
    public void tick() {
        super.tick();
        if (this.getWorld().isClient) {
            if (this.playingAnimation) {
                --this.ticksLeft;
                if (this.ticksLeft == 14) {
                    for(int i = 0; i < 12; ++i) {
                        double d = this.getX() + (this.random.nextDouble() * 2.0 - 1.0) * (double)this.getWidth() * 0.5;
                        double e = this.getY() + 0.05 + this.random.nextDouble();
                        double f = this.getZ() + (this.random.nextDouble() * 2.0 - 1.0) * (double)this.getWidth() * 0.5;
                        double g = (this.random.nextDouble() * 2.0 - 1.0) * 0.3;
                        double h = 0.3 + this.random.nextDouble() * 0.3;
                        double j = (this.random.nextDouble() * 2.0 - 1.0) * 0.3;
                        this.getWorld().addParticle(ParticleTypes.CRIT, d, e + 1.0, f, g, h, j);
                    }
                }
            }
        } else if (--this.warmup < 0) {
            if (this.warmup == -8) {
                List<LivingEntity> list = this.getWorld().getNonSpectatingEntities(LivingEntity.class, this.getBoundingBox().expand(0.2, 0.0, 0.2));

                for (LivingEntity livingEntity : list) {
                    this.damage(livingEntity);
                }
            }

            if (!this.startedAttack) {
                this.getWorld().sendEntityStatus(this, (byte)4);
                this.startedAttack = true;
            }

            if (--this.ticksLeft < 0) {
                this.discard();
            }
        }
    }

    @Overwrite
    public void damage(LivingEntity target) {
        LivingEntity livingEntity = this.getOwner();
        if (target.isAlive() && !target.isInvulnerable() && target != livingEntity) {
            if (livingEntity == null) {
                target.damage(this.getDamageSources().magic(), 6.0F);
            } else {
                if (livingEntity.isTeammate(target)) {
                    return;
                }

                target.damage(this.getDamageSources().indirectMagic(this, livingEntity), 6.0F);
            }

        }
    }

    @Overwrite
    public void handleStatus(byte status) {
        super.handleStatus(status);
        if (status == 4) {
            this.playingAnimation = true;
            if (!this.isSilent()) {
                this.getWorld().playSound(this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_EVOKER_FANGS_ATTACK, this.getSoundCategory(), 1.0F, this.random.nextFloat() * 0.2F + 0.85F, false);
            }
        }

    }

    @Overwrite
    public float getAnimationProgress(float tickDelta) {
        if (!this.playingAnimation) {
            return 0.0F;
        } else {
            int i = this.ticksLeft - 2;
            return i <= 0 ? 1.0F : 1.0F - ((float)i - tickDelta) / 20.0F;
        }
    }
}
