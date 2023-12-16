package net.ua.mixin.block;

import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;

@Mixin(net.minecraft.fluid.LavaFluid.class)
public abstract class LavaFluid extends FlowableFluid {
    @Final
    @Shadow
    public static final float MIN_HEIGHT_TO_REPLACE = 0.9999999F;

    public LavaFluid() {
    }

    @Overwrite
    public Fluid getFlowing() {
        return Fluids.FLOWING_LAVA;
    }

    @Overwrite
    public Fluid getStill() {
        return Fluids.LAVA;
    }

    @Overwrite
    public Item getBucketItem() {
        return Items.WATER_BUCKET;
    }

    @Overwrite
    public void randomDisplayTick(World world, BlockPos pos, FluidState state, Random random) {
        BlockPos blockPos = pos.up();
        if (world.getBlockState(blockPos).isAir() && !world.getBlockState(blockPos).isOpaqueFullCube(world, blockPos)) {
            if (random.nextInt(100) == 0) {
                double d = (double)pos.getX() + random.nextDouble();
                double e = (double)pos.getY() + 1.0;
                double f = (double)pos.getZ() + random.nextDouble();
                world.addParticle(ParticleTypes.LAVA, d, e, f, 0.0, 0.0, 0.0);
                world.playSound(d, e, f, SoundEvents.BLOCK_LAVA_POP, SoundCategory.BLOCKS, 0.2F + random.nextFloat() * 0.2F, 0.9F + random.nextFloat() * 0.15F, false);
            }

            if (random.nextInt(200) == 0) {
                world.playSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BLOCK_LAVA_AMBIENT, SoundCategory.BLOCKS, 0.2F + random.nextFloat() * 0.2F, 0.9F + random.nextFloat() * 0.15F, false);
            }
        }

    }

    @Overwrite
    public void onRandomTick(World world, BlockPos pos, FluidState state, Random random) {
        if (world.getGameRules().getBoolean(GameRules.DO_FIRE_TICK)) {
            int i = random.nextInt(3);
            if (i > 0) {
                BlockPos blockPos = pos;

                for(int j = 0; j < i; ++j) {
                    blockPos = blockPos.add(random.nextInt(3) - 1, 1, random.nextInt(3) - 1);
                    if (!world.canSetBlock(blockPos)) {
                        return;
                    }

                    BlockState blockState = world.getBlockState(blockPos);
                    if (blockState.isAir()) {
                        if (this.canLightFire(world, blockPos)) {
                            world.setBlockState(blockPos, AbstractFireBlock.getState(world, blockPos));
                            return;
                        }
                    } else if (blockState.blocksMovement()) {
                        return;
                    }
                }
            } else {
                for(int k = 0; k < 3; ++k) {
                    BlockPos blockPos2 = pos.add(random.nextInt(3) - 1, 0, random.nextInt(3) - 1);
                    if (!world.canSetBlock(blockPos2)) {
                        return;
                    }

                    if (world.isAir(blockPos2.up()) && this.hasBurnableBlock(world, blockPos2)) {
                        world.setBlockState(blockPos2.up(), AbstractFireBlock.getState(world, blockPos2));
                    }
                }
            }

        }
    }

    @Overwrite
    public boolean canLightFire(WorldView world, BlockPos pos) {
        Direction[] var3 = Direction.values();

        for (Direction direction : var3) {
            if (this.hasBurnableBlock(world, pos.offset(direction))) {
                return true;
            }
        }

        return false;
    }

    @Overwrite
    public boolean hasBurnableBlock(WorldView world, BlockPos pos) {
        return (pos.getY() < world.getBottomY() || pos.getY() >= world.getTopY() || world.isChunkLoaded(pos)) && world.getBlockState(pos).isBurnable();
    }

    @Nullable
    @Overwrite
    public ParticleEffect getParticle() {
        return ParticleTypes.DRIPPING_LAVA;
    }

    @Overwrite
    public void beforeBreakingBlock(WorldAccess world, BlockPos pos, BlockState state) {
        this.playExtinguishEvent(world, pos);
    }

    @Overwrite
    public int getFlowSpeed(WorldView world) {
        return 4;
    }

    @Overwrite
    public BlockState toBlockState(FluidState state) {
        return Blocks.LAVA.getDefaultState().with(FluidBlock.LEVEL, getBlockStateLevel(state));
    }

    @Overwrite
    public boolean matchesType(Fluid fluid) {
        return fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA;
    }

    @Overwrite
    public int getLevelDecreasePerBlock(WorldView world) {
        return world.getDimension().ultrawarm() ? 1 : 2;
    }

    @Overwrite
    public boolean canBeReplacedWith(FluidState state, BlockView world, BlockPos pos, Fluid fluid, Direction direction) {
        return state.getHeight(world, pos) >= MIN_HEIGHT_TO_REPLACE && fluid.isIn(FluidTags.WATER);
    }

    @Overwrite
    public int getTickRate(WorldView world) {
        return world.getDimension().ultrawarm() ? 10 : 30;
    }

    // @Overwrite
    // public int getNextTickDelay(World world, BlockPos pos, FluidState oldState, FluidState newState) {
    //     int i = this.getTickRate(world);
    //     if (!oldState.isEmpty() && !newState.isEmpty() && !(Boolean)oldState.get(FALLING) && !(Boolean)newState.get(FALLING) && newState.getHeight(world, pos) > oldState.getHeight(world, pos) && world.getRandom().nextInt(4) != 0) {
    //         i *= 4;
    //     }
    //     return i;
    // }

    @Overwrite
    public void playExtinguishEvent(WorldAccess world, BlockPos pos) {
        world.syncWorldEvent(1501, pos, 0);
    }

    @Overwrite
    public boolean isInfinite(World world) {
        return world.getGameRules().getBoolean(GameRules.LAVA_SOURCE_CONVERSION);
    }

    @Overwrite
    public void flow(WorldAccess world, BlockPos pos, BlockState state, Direction direction, FluidState fluidState) {
        if (direction == Direction.DOWN) {
            FluidState fluidState2 = world.getFluidState(pos);
            if (this.isIn(FluidTags.LAVA) && fluidState2.isIn(FluidTags.WATER)) {
                if (state.getBlock() instanceof FluidBlock) {
                    world.setBlockState(pos, Blocks.STONE.getDefaultState(), 3);
                }

                this.playExtinguishEvent(world, pos);
                return;
            }
        }

        super.flow(world, pos, state, direction, fluidState);
    }

    @Overwrite
    public boolean hasRandomTicks() {
        return true;
    }

    @Overwrite
    public float getBlastResistance() {
        return 100.0F;
    }

    @Overwrite
    public Optional<SoundEvent> getBucketFillSound() {
        return Optional.of(SoundEvents.ITEM_BUCKET_FILL_LAVA);
    }

    @Mixin(targets = "net.minecraft.fluid.LavaFluid$Flowing")
    public static class Flowing extends net.minecraft.fluid.LavaFluid {
        public Flowing() {
        }

        @Overwrite
        public void appendProperties(StateManager.Builder<Fluid, FluidState> builder) {
            super.appendProperties(builder);
            builder.add(LEVEL);
        }

        @Overwrite
        public int getLevel(FluidState state) {
            return state.get(LEVEL);
        }

        @Overwrite
        public boolean isStill(FluidState state) {
            return false;
        }
    }

    @Mixin(targets = "net.minecraft.fluid.LavaFluid$Flowing")
    public static class Still extends net.minecraft.fluid.LavaFluid {
        public Still() {
        }

        @Overwrite
        public int getLevel(FluidState state) {
            return 8;
        }

        @Overwrite
        public boolean isStill(FluidState state) {
            return true;
        }
    }
}