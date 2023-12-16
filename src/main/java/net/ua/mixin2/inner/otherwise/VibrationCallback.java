package net.ua.mixin2.inner.otherwise;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.WardenBrain;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.registry.tag.GameEventTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.event.EntityPositionSource;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.PositionSource;
import net.minecraft.world.event.Vibrations;
import net.ua.ips.wai;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class VibrationCallback implements Vibrations.Callback {
    private static final int RANGE = 16;
    private final PositionSource positionSource;
    private WardenEntity w;
    public VibrationCallback(WardenEntity w) {
        this.w = w;
        positionSource = new EntityPositionSource(w, w.getStandingEyeHeight());
    }

    public int getRange() {
        return 16;
    }

    public PositionSource getPositionSource() {
        return this.positionSource;
    }

    public TagKey<GameEvent> getTag() {
        return GameEventTags.WARDEN_CAN_LISTEN;
    }

    public boolean triggersAvoidCriterion() {
        return true;
    }

    public boolean accepts(ServerWorld world, BlockPos pos, GameEvent event, GameEvent.Emitter emitter) {
        if (!w.isAiDisabled() && !w.isDead() && !w.getBrain().hasMemoryModule(MemoryModuleType.VIBRATION_COOLDOWN) && !((wai) w).isDiggingOrEmerging() && world.getWorldBorder().contains(pos)) {
            Entity var6 = emitter.sourceEntity();
            boolean var10000;
            if (var6 instanceof LivingEntity livingEntity) {
                if (!w.isValidTarget(livingEntity)) {
                    var10000 = false;
                    return var10000;
                }
            }

            var10000 = true;
            return var10000;
        } else {
            return false;
        }
    }

    public void accept(ServerWorld world, BlockPos pos, GameEvent event, @Nullable Entity sourceEntity, @Nullable Entity entity, float distance) {
        if (!w.isDead()) {
            w.getBrain().remember(MemoryModuleType.VIBRATION_COOLDOWN, Unit.INSTANCE, 40L);
            world.sendEntityStatus(w, (byte)61);
            w.playSound(SoundEvents.ENTITY_WARDEN_TENDRIL_CLICKS, 5.0F, w.getSoundPitch());
            BlockPos blockPos = pos;
            if (entity != null) {
                if (w.isInRange(entity, 30.0)) {
                    if (w.getBrain().hasMemoryModule(MemoryModuleType.RECENT_PROJECTILE)) {
                        if (w.isValidTarget(entity)) {
                            blockPos = entity.getBlockPos();
                        }

                        w.increaseAngerAt(entity);
                    } else {
                        w.increaseAngerAt(entity, 10, true);
                    }
                }

                w.getBrain().remember(MemoryModuleType.RECENT_PROJECTILE, Unit.INSTANCE, 100L);
            } else {
                w.increaseAngerAt(sourceEntity);
            }

            if (!w.getAngriness().isAngry()) {
                Optional<LivingEntity> optional = w.getAngerManager().getPrimeSuspect();
                if (entity != null || optional.isEmpty() || optional.get() == sourceEntity) {
                    WardenBrain.lookAtDisturbance(w, blockPos);
                }
            }

        }
    }
}