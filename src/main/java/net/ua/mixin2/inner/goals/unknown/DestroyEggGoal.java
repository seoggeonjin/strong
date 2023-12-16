package net.ua.mixin2.inner.goals.unknown;

import net.minecraft.block.Blocks;
import net.minecraft.entity.ai.goal.StepAndDestroyBlockGoal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public class DestroyEggGoal
        extends StepAndDestroyBlockGoal {
    protected final Random random = Random.create();
    public DestroyEggGoal(PathAwareEntity mob, double speed, int maxYDifference) {
        super(Blocks.TURTLE_EGG, mob, speed, maxYDifference);
    }

    @Override
    public void tickStepping(WorldAccess world, BlockPos pos) {
        world.playSound(null, pos, SoundEvents.ENTITY_ZOMBIE_DESTROY_EGG, SoundCategory.HOSTILE, 0.5f, 0.9f + this.random.nextFloat() * 0.2f);
    }

    @Override
    public void onDestroyBlock(World world, BlockPos pos) {
        world.playSound(null, pos, SoundEvents.ENTITY_TURTLE_EGG_BREAK, SoundCategory.BLOCKS, 0.7f, 0.9f + world.random.nextFloat() * 0.2f);
    }

    @Override
    public double getDesiredDistanceToTarget() {
        return 1.14;
    }
}