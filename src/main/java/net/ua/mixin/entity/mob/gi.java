package net.ua.mixin.entity.mob;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Objects;

@Mixin(GiantEntity.class)
public class gi extends HostileEntity {
    public gi(EntityType<? extends gi> entityType, World world) {
        super(entityType, world);
        Objects.requireNonNull(this.getAttributes().getCustomInstance(RegistryEntry.of(EntityAttributes.GENERIC_MAX_HEALTH))).setBaseValue(2147483647);
        this.setHealth(2147483647);
    }

    @Overwrite
    public float getActiveEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return 10.440001f;
    }

    @Overwrite
    public float getUnscaledRidingOffset(Entity vehicle) {
        return -3.75f;
    }

    @Overwrite
    public static DefaultAttributeContainer.Builder createGiantAttributes() {
        return HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 2147483647).add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.5).add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 50.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 2147483647);
    }
    static {
        EntityAttributes.GENERIC_MAX_HEALTH.clamp(1);
    }

    @Overwrite
    public float getPathfindingFavor(BlockPos pos, WorldView world) {
        return world.getPhototaxisFavor(pos);
    }
    @Override
    public void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 1.0, 0.0f));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 1.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.targetSelector.add(2, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true, false));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, GuardianEntity.class, true, false));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, SkeletonEntity.class, true));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, WitherSkeletonEntity.class, true));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, StrayEntity.class, true));
    }

    @Override
    public boolean tryAttack(Entity target) {
        return super.tryAttack(target);
    }
}
