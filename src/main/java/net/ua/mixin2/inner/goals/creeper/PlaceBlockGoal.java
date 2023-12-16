package net.ua.mixin2.inner.goals.creeper;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.ua.ips.Ee;

public class PlaceBlockGoal
        extends Goal {
    private final Ee EnderMan;

    public PlaceBlockGoal(Ee EnderMan) {
        this.EnderMan = EnderMan;
    }

    @Override
    public boolean canStart() {
        if (this.EnderMan.getCarriedBlock() == null) {
            return false;
        }
        if (!this.EnderMan.getWorld().getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
            return false;
        }
        return this.EnderMan.getRandom().nextInt(PlaceBlockGoal.toGoalTicks(2000)) == 0;
    }

    @Override
    public void tick() {
        Random random = this.EnderMan.getRandom();
        World world = this.EnderMan.getWorld();
        int i = MathHelper.floor(this.EnderMan.getX2() - 1.0 + random.nextDouble() * 2.0);
        int j = MathHelper.floor(this.EnderMan.getY2() + random.nextDouble() * 2.0);
        int k = MathHelper.floor(this.EnderMan.getZ2() - 1.0 + random.nextDouble() * 2.0);
        BlockPos blockPos = new BlockPos(i, j, k);
        BlockState blockState = world.getBlockState(blockPos);
        BlockPos blockPos2 = blockPos.down();
        BlockState blockState2 = world.getBlockState(blockPos2);
        BlockState blockState3 = this.EnderMan.getCarriedBlock();
        if (blockState3 == null) {
            return;
        }
        if (this.canPlaceOn(world, blockPos, blockState3 = Block.postProcessState(blockState3, this.EnderMan.getWorld(), blockPos), blockState, blockState2, blockPos2)) {
            world.setBlockState(blockPos, blockState3, Block.NOTIFY_ALL);
            world.emitGameEvent(GameEvent.BLOCK_PLACE, blockPos, GameEvent.Emitter.of((Entity) this.EnderMan, blockState3));
            this.EnderMan.setCarriedBlock(null);
        }
    }

    private boolean canPlaceOn(World world, BlockPos posAbove, BlockState carriedState, BlockState stateAbove, BlockState state, BlockPos pos) {
        return stateAbove.isAir() && !state.isAir() && !state.isOf(Blocks.BEDROCK) && state.isFullCube(world, pos) && carriedState.canPlaceAt(world, posAbove) && world.getOtherEntities((Entity) this.EnderMan, Box.from(Vec3d.of(posAbove))).isEmpty();
    }
}