package net.ua.mixin2.inner.otherwise;

import com.mojang.serialization.Dynamic;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.mob.WardenBrain;
import net.minecraft.entity.mob.WardenEntity;

public class T1 extends WardenBrain {
    public static Brain<?> create(WardenEntity warden, Dynamic<?> dynamic) {
        return WardenBrain.create(warden, dynamic);
    }
}
