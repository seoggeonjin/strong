package net.ua.mixin2.inner.otherwise;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.GiantEntity;
import net.minecraft.entity.passive.AxolotlEntity;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.ua.ips.gui;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class GuardianTargetPredicate implements Predicate<LivingEntity> {
    private final gui owner;

    public GuardianTargetPredicate(gui owner) {
        this.owner = owner;
    }

    @Override
    public boolean test(@Nullable LivingEntity livingEntity) {
        return (livingEntity instanceof PlayerEntity || livingEntity instanceof SquidEntity || livingEntity instanceof AxolotlEntity || livingEntity instanceof GiantEntity || livingEntity instanceof AbstractSkeletonEntity) && livingEntity.squaredDistanceTo((Entity) this.owner) > 4.0;
    }
}