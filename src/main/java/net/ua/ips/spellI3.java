package net.ua.ips;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public interface spellI3 extends spellI {
    World getWorld();
    BlockPos getBlockPos();
    boolean hasStatusEffect(StatusEffect effect);
    boolean addStatusEffect2(StatusEffectInstance effect);
    Vec3d[] getMirrorCopyOffsets(float tickDelta);
    boolean isInvisible();
    boolean isAttacking();
    Vec3d[][] mirrorCopyOffsets();
}
