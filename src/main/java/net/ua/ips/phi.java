package net.ua.ips;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.ua.mixin2.inner.otherwise.PhantomMovementType;
import org.jetbrains.annotations.Nullable;

public interface phi extends mixins {
    boolean horizontalCollision();
    BlockPos circlingCenter();
    Vec3d targetPosition();
    void targetPosition(Vec3d v);
    void circlingCenter(BlockPos b);
    World getWorld();
    BlockPos getBlockPos();
    double getY2();
    Random random();
    double getX2();
    double getZ2();
    void setYaw(float yaw);
    float getYaw();
    void setPitch(float pitch);
    Vec3d getVelocity();
    void setVelocity(Vec3d velocity);
    void bodyYaw(float yaw);
    float bodyYaw();
    int age();
    Box getBoundingBox2();
    void setTarget(@Nullable LivingEntity target);
    void movementType(PhantomMovementType movementType);
    PhantomMovementType movementType();
    boolean tryAttack(Entity target);
    boolean isSilent();
    int hurtTime();
    boolean isTarget(LivingEntity entity, TargetPredicate predicate);
    void headYaw(float headYaw);
    void playSound(SoundEvent sound, float volume, float pitch);
    void setG(LivingEntity l);
}
