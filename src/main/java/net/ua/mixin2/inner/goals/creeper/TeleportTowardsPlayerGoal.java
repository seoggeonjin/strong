package net.ua.mixin2.inner.goals.creeper;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.ua.ips.Ee;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class TeleportTowardsPlayerGoal extends ActiveTargetGoal<PlayerEntity> {
    private final Ee enderman;
    @Nullable
    private PlayerEntity targetPlayer;
    private int lookAtPlayerWarmup;
    private int ticksSinceUnseenTeleport;
    private final TargetPredicate staringPlayerPredicate;
    private final TargetPredicate validTargetPredicate = TargetPredicate.createAttackable().ignoreVisibility();
    private final Predicate<LivingEntity> angerPredicate;

    public TeleportTowardsPlayerGoal(Ee enderman, @Nullable Predicate<LivingEntity> targetPredicate) {
        super((MobEntity) enderman, PlayerEntity.class, 10, false, false, targetPredicate);
        this.enderman = enderman;
        this.angerPredicate = playerEntity -> (enderman.isPlayerStaring((PlayerEntity)playerEntity) || enderman.shouldAngerAt(playerEntity)) && !enderman.hasPassengerDeep(playerEntity);
        this.staringPlayerPredicate = TargetPredicate.createAttackable().setBaseMaxDistance(this.getFollowRange()).setPredicate(this.angerPredicate);
    }

    @Override
    public boolean canStart() {
        this.targetPlayer = this.enderman.getWorld().getClosestPlayer(this.staringPlayerPredicate, (LivingEntity) this.enderman);
        return this.targetPlayer != null;
    }

    @Override
    public void start() {
        this.lookAtPlayerWarmup = this.getTickCount(5);
        this.ticksSinceUnseenTeleport = 0;
        this.enderman.setProvoked();
    }

    @Override
    public void stop() {
        this.targetPlayer = null;
        super.stop();
    }

    @Override
    public boolean shouldContinue() {
        if (this.targetPlayer != null) {
            if (!this.angerPredicate.test(this.targetPlayer)) {
                return false;
            }
            this.enderman.lookAtEntity(this.targetPlayer, 10.0f, 10.0f);
            return true;
        }
        if (this.targetEntity != null) {
            if (this.enderman.hasPassengerDeep(this.targetEntity)) {
                return false;
            }
            if (this.validTargetPredicate.test((LivingEntity) this.enderman, this.targetEntity)) {
                return true;
            }
        }
        return super.shouldContinue();
    }

    @Override
    public void tick() {
        if (this.enderman.getTarget() == null) {
            super.setTargetEntity(null);
        }
        if (this.targetPlayer != null) {
            if (--this.lookAtPlayerWarmup <= 0) {
                this.targetEntity = this.targetPlayer;
                this.targetPlayer = null;
                super.start();
            }
        } else {
            if (this.targetEntity != null && !this.enderman.hasVehicle()) {
                if (this.enderman.isPlayerStaring((PlayerEntity)this.targetEntity)) {
                    if (this.targetEntity.squaredDistanceTo((Entity) this.enderman) < 5.0) {
                        this.enderman.teleportRandomly();
                    }
                    this.ticksSinceUnseenTeleport = 0;
                } else if (this.targetEntity.squaredDistanceTo((Entity) this.enderman) > 256.0 && this.ticksSinceUnseenTeleport++ >= this.getTickCount(30) && this.enderman.teleportTo(this.targetEntity)) {
                    this.ticksSinceUnseenTeleport = 0;
                }
            }
            super.tick();
        }
    }
}