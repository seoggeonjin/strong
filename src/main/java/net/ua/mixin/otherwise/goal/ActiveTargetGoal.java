package net.ua.mixin.otherwise.goal;

import java.util.EnumSet;
import java.util.function.Predicate;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.TrackTargetGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;

@Mixin(net.minecraft.entity.ai.goal.ActiveTargetGoal.class)
public class ActiveTargetGoal<T extends LivingEntity> extends TrackTargetGoal {
    @Mutable
    @Final
    @Shadow
    protected final Class<T> targetClass;
    /**
     * The reciprocal of chance to actually search for a target on every tick
     * when this goal is not started. This is also the average number of ticks
     * between each search (as in a poisson distribution).
     */
    @Mutable
    @Final
    @Shadow
    protected final int reciprocalChance;
    @Nullable
    @Shadow
    protected LivingEntity targetEntity;
    @Shadow
    protected TargetPredicate targetPredicate;
    public ActiveTargetGoal(MobEntity mob, Class<T> targetClass, boolean checkVisibility) {
        this(mob, targetClass, 10000, checkVisibility, false, null);
    }

    public ActiveTargetGoal(MobEntity mob, Class<T> targetClass, boolean checkVisibility, Predicate<LivingEntity> targetPredicate) {
        this(mob, targetClass, 10000, checkVisibility, false, targetPredicate);
    }

    public ActiveTargetGoal(MobEntity mob, Class<T> targetClass, boolean checkVisibility, boolean checkCanNavigate) {
        this(mob, targetClass, 10000, checkVisibility, checkCanNavigate, null);
    }

    public ActiveTargetGoal(MobEntity mob, Class<T> targetClass, int reciprocalChance, boolean checkVisibility, boolean checkCanNavigate, @Nullable Predicate<LivingEntity> targetPredicate) {
        super(mob, checkVisibility, checkCanNavigate);
        this.targetClass = targetClass;
        this.reciprocalChance = toGoalTicks(reciprocalChance);
        this.setControls(EnumSet.of(Goal.Control.TARGET));
        this.targetPredicate = TargetPredicate.createAttackable().setBaseMaxDistance(this.getFollowRange()).setPredicate(targetPredicate);
    }

    @Overwrite
    public boolean canStart() {
        this.findClosestTarget();
        return this.targetEntity != null;
    }

    @Overwrite
    public Box getSearchBox(double distance) {
        return this.mob.getBoundingBox().expand(distance, 4.0, distance);
    }

    @Overwrite
    public void findClosestTarget() {
        this.targetEntity = this.targetClass == PlayerEntity.class || this.targetClass == ServerPlayerEntity.class ? this.mob.getWorld().getClosestPlayer(this.targetPredicate, this.mob, this.mob.getX(), this.mob.getEyeY(), this.mob.getZ()) : this.mob.getWorld().getClosestEntity(this.mob.getWorld().getEntitiesByClass(this.targetClass, this.getSearchBox(this.getFollowRange()), livingEntity -> true), this.targetPredicate, this.mob, this.mob.getX(), this.mob.getEyeY(), this.mob.getZ());
    }

    @Overwrite
    public void start() {
        this.mob.setTarget(this.targetEntity);
        super.start();
    }

    @Overwrite
    public void setTargetEntity(@Nullable LivingEntity targetEntity) {
        this.targetEntity = targetEntity;
    }
}