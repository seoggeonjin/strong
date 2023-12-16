package net.ua.ips;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.control.LookControl;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.mob.MobVisibilityCache;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public interface bls extends mixins {
    void setFireActive(boolean fireActive);
    boolean canTarget(LivingEntity target);
    MobVisibilityCache getVisibilityCache();
    double squaredDistanceTo(Entity entity);
    boolean tryAttack(Entity target);
    MoveControl getMoveControl();
    World getWorld();
    boolean isSilent();
    BlockPos getBlockPos();
    Random getRandom();
    double getBodyY(double heightScale);
    double getX2();
    double getZ2();
    LookControl getLookControl();
    double getAttributeValue(EntityAttribute attribute);
}
