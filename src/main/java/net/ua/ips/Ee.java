package net.ua.ips;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.control.LookControl;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface Ee extends mixins {
    boolean canTarget(LivingEntity var1);
    @Nullable
    UUID getAngryAt();
    default boolean isUniversallyAngry(World world) {
        return world.getGameRules().getBoolean(GameRules.UNIVERSAL_ANGER) && this.hasAngerTime() && this.getAngryAt() == null;
    }
    default boolean hasAngerTime() {
        return this.getAngerTime() > 0;
    }
    int getAngerTime();
    default boolean shouldAngerAt(LivingEntity entity) {
        if (!this.canTarget(entity)) {
            return false;
        }
        if (entity.getType() == EntityType.PLAYER && this.isUniversallyAngry(entity.getWorld())) {
            return true;
        }
        return entity.getUuid().equals(this.getAngryAt());
    }
    World getWorld();
    boolean isPlayerStaring(PlayerEntity player);
    EntityNavigation getNavigation();
    LookControl getLookControl();
    @Nullable
    BlockState getCarriedBlock();
    Random getRandom();
    double getX2();
    double getY2();
    double getZ2();
    void setCarriedBlock(@Nullable BlockState state);
    int getBlockX2();
    int getBlockZ2();
    boolean teleportRandomly();
    void setProvoked();
    void lookAtEntity(Entity targetEntity, float maxYawChange, float maxPitchChange);
    boolean hasPassengerDeep(Entity passenger);
    boolean hasVehicle();
    boolean teleportTo(Entity entity);
}