package net.ua.mixin2.inner.goals.evoker;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.EvokerFangsEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.event.GameEvent;
import net.ua.ips.evi;
import net.ua.ips.spellI2;
import net.ua.mixin2.inner.Spell;
import net.ua.mixin2.inner.goals.SpellcastingIllagerEntity.CastSpellGoal2;

public class ConjureFangsGoal extends CastSpellGoal2 {
    private final evi t;
    public ConjureFangsGoal(evi t) {
        super(t);
        this.t = t;
    }

    @Override
    public int getSpellTicks() {
        return 40;
    }

    @Override
    public int startTimeDelay() {
        return 100;
    }

    @Override
    public void castSpell() {
        LivingEntity livingEntity = t.getTarget();
        double d = Math.min(livingEntity.getY(), ((Entity)t).getY());
        double e = Math.max(livingEntity.getY(), ((Entity)t).getY()) + 1.0;
        float f = (float) MathHelper.atan2(livingEntity.getZ() - ((Entity)t).getZ(), livingEntity.getX() - ((Entity)t).getX());
        if (t.squaredDistanceTo(livingEntity) < 9.0) {
            float g;
            int i;
            for (i = 0; i < 5; ++i) {
                g = f + (float)i * (float)Math.PI * 0.4f;
                this.conjureFangs(((Entity)t).getX() + (double)MathHelper.cos(g) * 1.5, ((Entity)t).getZ() + (double)MathHelper.sin(g) * 1.5, d, e, g, 0);
                this.conjureFangs(((Entity)t).getX() + (double)MathHelper.cos(g) * 1.5, ((Entity)t).getZ() + (double)MathHelper.sin(g) * 1.5, d, e, g, 0);
                this.conjureFangs(((Entity)t).getX() + (double)MathHelper.cos(g) * 1.5, ((Entity)t).getZ() + (double)MathHelper.sin(g) * 1.5, d, e, g, 0);
            }
            for (i = 0; i < 8; ++i) {
                g = f + (float)i * (float)Math.PI * 2.0f / 8.0f + 1.2566371f;
                this.conjureFangs(((Entity)t).getX() + (double)MathHelper.cos(g) * 2.5, ((Entity)t).getZ() + (double)MathHelper.sin(g) * 2.5, d, e, g, 3);
                this.conjureFangs(((Entity)t).getX() + (double)MathHelper.cos(g) * 2.5, ((Entity)t).getZ() + (double)MathHelper.sin(g) * 2.5, d, e, g, 3);
                this.conjureFangs(((Entity)t).getX() + (double)MathHelper.cos(g) * 2.5, ((Entity)t).getZ() + (double)MathHelper.sin(g) * 2.5, d, e, g, 3);
            }
        } else {
            for (int i = 0; i < (((Entity) t).distanceTo(livingEntity)); ++i) {
                double h = 1.25 * (double)(i + 1);
                this.conjureFangs(((Entity)t).getX() + (double)MathHelper.cos(f) * h, ((Entity)t).getZ() + (double)MathHelper.sin(f) * h, d, e, f, i);
                this.conjureFangs(((Entity)t).getX() + (double)MathHelper.cos(f) * h, ((Entity)t).getZ() + (double)MathHelper.sin(f) * h, d, e, f, i);
                this.conjureFangs(((Entity)t).getX() + (double)MathHelper.cos(f) * h, ((Entity)t).getZ() + (double)MathHelper.sin(f) * h, d, e, f, i);
            }
        }
    }

    private void conjureFangs(double x, double z, double maxY, double y, float yaw, int warmup) {
        BlockPos blockPos = BlockPos.ofFloored(x, y, z);
        boolean bl = false;
        double d = 0.0;
        do {
            VoxelShape voxelShape;
            BlockPos blockPos2 = blockPos.down();
            BlockState blockState = t.getWorld().getBlockState(blockPos2);
            if (!blockState.isSideSolidFullSquare(t.getWorld(), blockPos2, Direction.UP)) continue;
            if (!t.getWorld().isAir(blockPos) && !(voxelShape = (t.getWorld().getBlockState(blockPos)).getCollisionShape(t.getWorld(), blockPos)).isEmpty()) {
                d = voxelShape.getMax(Direction.Axis.Y);
            }
            bl = true;
            break;
        } while ((blockPos = blockPos.down()).getY() >= MathHelper.floor(maxY) - 1);
        if (bl) {
            t.getWorld().spawnEntity(new EvokerFangsEntity(t.getWorld(), x, (double)blockPos.getY() + d, z, yaw, warmup, (LivingEntity) t));
            t.getWorld().emitGameEvent(GameEvent.ENTITY_PLACE, new Vec3d(x, (double)blockPos.getY() + d, z), GameEvent.Emitter.of((Entity) t));
        }
    }

    @Override
    public SoundEvent getSoundPrepare() {
        return SoundEvents.ENTITY_EVOKER_PREPARE_ATTACK;
    }

    @Override
    public spellI2 getSpell() {
        return Spell.FANGS;
    }
}