package net.ua.mixin.entity.mob;

import net.minecraft.entity.*;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.IllagerEntity;
import net.minecraft.entity.mob.IllusionerEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.ua.ips.spellI2;
import net.ua.ips.spellI3;
import net.ua.mixin.entity.abst.si;
import net.ua.mixin2.inner.goals.illusionor.GiveInvisibilityGoal2;
import net.ua.mixin2.inner.goals.SpellcastingIllagerEntity.LookAtTargetGoal2;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IllusionerEntity.class)
public class ie extends si implements RangedAttackMob, spellI3 {
    @Shadow
    private int mirrorSpellTimer;
    @Mutable
    @Final
    @Shadow
    private Vec3d[][] mirrorCopyOffsets;

    public ie(EntityType<? extends ie> entityType, World world) {
        super(entityType, world);
    }
    @Unique
    public Vec3d[][] mirrorCopyOffsets() {
        return mirrorCopyOffsets;
    }
    @Inject(method = "<init>", at = @At("TAIL"))
    private void ie2(EntityType<? extends ie> entityType, World world, CallbackInfo ci) {
        this.experiencePoints = 5;
        this.mirrorCopyOffsets = new Vec3d[2][75];
        for (int i = 0; i < 75; ++i) {
            this.mirrorCopyOffsets[0][i] = new Vec3d((int)(Math.random() * 5) - 2, (int)(Math.random() * 2), (int)(Math.random() * 5) - 2);
            this.mirrorCopyOffsets[1][i] = new Vec3d((int)(Math.random() * 5) - 2, (int)(Math.random() * 2), (int)(Math.random() * 5) - 2);
        }
        this.mirrorCopyOffsets[1][74] = Vec3d.ZERO;
    }

    @Overwrite
    public void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new LookAtTargetGoal2(this));
        this.goalSelector.add(4, new GiveInvisibilityGoal2(this));
        // this.goalSelector.add(5, new BlindTargetGoal2(this));
        this.goalSelector.add(6, new BowAttackGoal<>(this, 0.5, 20, 15.0f));
        this.goalSelector.add(8, new WanderAroundGoal(this, 0.6));
        this.goalSelector.add(9, new LookAtEntityGoal(this, PlayerEntity.class, 3.0f, 1.0f));
        this.goalSelector.add(10, new LookAtEntityGoal(this, MobEntity.class, 8.0f));
        this.targetSelector.add(1, new RevengeGoal(this, RaiderEntity.class).setGroupRevenge());
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true).setMaxTimeWithoutVisibility(300));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, MerchantEntity.class, false).setMaxTimeWithoutVisibility(300));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, IronGolemEntity.class, false).setMaxTimeWithoutVisibility(300));
    }

    @Overwrite
    public static DefaultAttributeContainer.Builder createIllusionerAttributes() {
        return HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.5).add(EntityAttributes.GENERIC_FOLLOW_RANGE, 2048.0f).add(EntityAttributes.GENERIC_MAX_HEALTH, 32.0);
    }

    @Overwrite
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
    }

    @Overwrite
    public void initDataTracker() {
        super.initDataTracker();
    }

    @Overwrite
    public Box getVisibilityBoundingBox() {
        return this.getBoundingBox().expand(5.0, 0.0, 5.0);
    }

    @Overwrite
    public void tickMovement() {
        super.tickMovement();
        if (this.getWorld().isClient && this.isInvisible()) {
            --this.mirrorSpellTimer;
            if (this.mirrorSpellTimer < 0) {
                this.mirrorSpellTimer = 0;
            }
            if (this.hurtTime == 1 || this.age % 1200 == 0) {
                int j;
                this.mirrorSpellTimer = 3;
                // for (j = 0; j < 4; ++j) {
                //     this.mirrorCopyOffsets[0][j] = this.mirrorCopyOffsets[1][j];
                //     this.mirrorCopyOffsets[1][j] = new Vec3d((double)(-6.0f + (float)this.random.nextInt(13)) * 0.5, Math.max(0, this.random.nextInt(6) - 4), (double)(-6.0f + (float)this.random.nextInt(13)) * 0.5);
                // }
                for (j = 0; j < 16; ++j) {
                    this.getWorld().addParticle(ParticleTypes.CLOUD, this.getParticleX(0.5), this.getRandomBodyY(), this.offsetZ(0.5), 0.0, 0.0, 0.0);
                }
                this.getWorld().playSound(this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, this.getSoundCategory(), 1.0f, 1.0f, false);
            } else if (this.hurtTime == this.maxHurtTime - 1) {
                this.mirrorSpellTimer = 3;
                // for (int k = 0; k < 4; ++k) {
                //     this.mirrorCopyOffsets[0][k] = this.mirrorCopyOffsets[1][k];
                //     this.mirrorCopyOffsets[1][k] = new Vec3d(0.0, 0.0, 0.0);
                // }
            }
        }
    }

    @Overwrite
    public SoundEvent getCelebratingSound() {
        return SoundEvents.ENTITY_ILLUSIONER_AMBIENT;
    }

    // @Overwrite
    // public Vec3d[] getMirrorCopyOffsets(float tickDelta) {
    //     if (this.mirrorSpellTimer <= 0) {
    //         return this.mirrorCopyOffsets[1];
    //     }
    //     double d = ((float)this.mirrorSpellTimer - tickDelta) / 3.0f;
    //     d = Math.pow(d, 0.25);
    //     Vec3d[] vec3ds = new Vec3d[4];
    //     for (int i = 0; i < 4; ++i) {
    //         vec3ds[i] = this.mirrorCopyOffsets[1][i].multiply(1.0 - d).add(this.mirrorCopyOffsets[0][i].multiply(d));
    //     }
    //     return vec3ds;
    // }
    @Overwrite
    public Vec3d[] getMirrorCopyOffsets(float tickDelta) {
        Vec3d[] vec3ds = new Vec3d[150];
        int k = 0;
        for (int j = 0; j < 75; j++) {
            vec3ds[k] = mirrorCopyOffsets[0][j];
            k++;
        }
        for (int j = 0; j < 75; j++) {
            vec3ds[k] = mirrorCopyOffsets[1][j];
            k++;
        }
        return vec3ds;
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
    public SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_VILLAGER_AMBIENT;
    }

    @Overwrite
    public SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_ILLUSIONER_DEATH;
    }

    @Overwrite
    public SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_ILLUSIONER_HURT;
    }

    @Override
    public void spellTicks(int tick) {
        super.spellTicks(tick);
    }

    @Override
    public void setSpell(spellI2 spell) {
        super.setSpell(spell);
    }

    @Override
    public int getSpellTicks() {
        return super.getSpellTicks();
    }

    @Override
    public EntityNavigation navigation() {
        return super.navigation();
    }

    @Override
    public int age() {
        return super.age();
    }

    @Overwrite
    public SoundEvent getCastSpellSound() {
        return SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL;
    }

    @Overwrite
    public void addBonusForWave(int wave, boolean unused) {
    }

    @Overwrite
    public void shootAt(LivingEntity target, float pullProgress) {
        ItemStack itemStack = this.getProjectileType(this.getStackInHand(ProjectileUtil.getHandPossiblyHolding(this, Items.BOW)));
        PersistentProjectileEntity persistentProjectileEntity = ProjectileUtil.createArrowProjectile(this, itemStack, pullProgress);
        double d = target.getX() - this.getX();
        double e = target.getBodyY(0.3333333333333333) - persistentProjectileEntity.getY();
        double f = target.getZ() - this.getZ();
        double g = Math.sqrt(d * d + f * f);
        persistentProjectileEntity.setVelocity(d, e + g * (double)0.2f, f, 1.6f, 14 - this.getWorld().getDifficulty().getId() * 4);
        this.playSound(SoundEvents.ENTITY_SKELETON_SHOOT, 1.0f, 1.0f / (this.getRandom().nextFloat() * 0.4f + 0.8f));
        this.getWorld().spawnEntity(persistentProjectileEntity);
    }

    @Overwrite
    public IllagerEntity.State getState() {
        if (this.isSpellcasting()) {
            return IllagerEntity.State.SPELLCASTING;
        }
        if (this.isAttacking()) {
            return IllagerEntity.State.BOW_AND_ARROW;
        }
        return IllagerEntity.State.CROSSED;
    }

    @Unique
    public BlockPos getBlockPos() {
        return super.getBlockPos();
    }
    @Unique
    public World getWorld() {
        return super.getWorld();
    }
    @Unique
    public boolean hasStatusEffect(StatusEffect effect) {
        return super.hasStatusEffect(effect);
    }
    @Unique
    public boolean addStatusEffect2(StatusEffectInstance effect) {
        return this.addStatusEffect(effect, null);
    }
    @Unique
    public boolean isInvisible() {
        return super.isInvisible();
    }
    @Unique
    public boolean isAttacking() {
        return super.isAttacking();
    }
    @Unique
    public LivingEntity getTarget() {
        return super.getTarget();
    }
}
