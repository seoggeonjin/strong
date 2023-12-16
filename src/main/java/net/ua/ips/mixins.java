package net.ua.ips;

import net.minecraft.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public interface mixins {
    @Nullable
    LivingEntity getTarget();
}
