package net.ua.mixin2.inner.otherwise;

import com.google.common.collect.ImmutableList;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.mob.BreezeBrain;
import net.minecraft.entity.mob.BreezeEntity;

import java.util.List;

public class T2 extends BreezeBrain {
    public static final List<SensorType<? extends Sensor<? super BreezeEntity>>> SENSORS = ImmutableList.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.HURT_BY, SensorType.NEAREST_PLAYERS, SensorType.BREEZE_ATTACK_ENTITY_SENSOR);
    public static final List<MemoryModuleType<?>> MEMORY_MODULES = ImmutableList.of(MemoryModuleType.LOOK_TARGET, MemoryModuleType.VISIBLE_MOBS, MemoryModuleType.NEAREST_ATTACKABLE, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryModuleType.ATTACK_TARGET, MemoryModuleType.WALK_TARGET, MemoryModuleType.BREEZE_JUMP_COOLDOWN, MemoryModuleType.BREEZE_JUMP_INHALING, MemoryModuleType.BREEZE_SHOOT, MemoryModuleType.BREEZE_SHOOT_CHARGING, MemoryModuleType.BREEZE_SHOOT_RECOVER, MemoryModuleType.BREEZE_SHOOT_COOLDOWN, MemoryModuleType.BREEZE_JUMP_TARGET, MemoryModuleType.HURT_BY, MemoryModuleType.HURT_BY_ENTITY, MemoryModuleType.PATH);

    public static Brain<?> create(Brain<BreezeEntity> brain) {
        return BreezeBrain.create(brain);
    }
}