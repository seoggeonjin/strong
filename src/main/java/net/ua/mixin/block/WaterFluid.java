package net.ua.mixin.block;

import java.util.Optional;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.entity.BlockEntity;
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
import net.minecraft.world.BlockView;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(net.minecraft.fluid.WaterFluid.class)
public abstract class WaterFluid extends FlowableFluid {

    @Overwrite
    public Fluid getFlowing() {
        return Fluids.FLOWING_WATER;
    }

    @Overwrite
    public Fluid getStill() {
        return Fluids.WATER;
    }

    @Overwrite
    public Item getBucketItem() {
        return Items.WATER_BUCKET;
    }

    @Overwrite
    public void randomDisplayTick(World world, BlockPos pos, FluidState state, Random random) {
        if (!state.isStill() && !state.get(FALLING)) {
            if (random.nextInt(64) == 0) {
                world.playSound((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, SoundEvents.BLOCK_WATER_AMBIENT, SoundCategory.BLOCKS, random.nextFloat() * 0.25f + 0.75f, random.nextFloat() + 0.5f, false);
            }
        } else if (random.nextInt(10) == 0) {
            world.addParticle(ParticleTypes.UNDERWATER, (double)pos.getX() + random.nextDouble(), (double)pos.getY() + random.nextDouble(), (double)pos.getZ() + random.nextDouble(), 0.0, 0.0, 0.0);
        }
    }

    @Overwrite
    @Nullable
    public ParticleEffect getParticle() {
        return ParticleTypes.DRIPPING_WATER;
    }

    @Overwrite
    public boolean isInfinite(World world) {
        return world.getGameRules().getBoolean(GameRules.WATER_SOURCE_CONVERSION);
    }

    @Overwrite
    public void beforeBreakingBlock(WorldAccess world, BlockPos pos, BlockState state) {
        BlockEntity blockEntity = state.hasBlockEntity() ? world.getBlockEntity(pos) : null;
        Block.dropStacks(state, world, pos, blockEntity);
    }

    @Overwrite
    public int getFlowSpeed(WorldView world) {
        return 4;
    }

    @Overwrite
    public BlockState toBlockState(FluidState state) {
        return Blocks.WATER.getDefaultState().with(FluidBlock.LEVEL, getBlockStateLevel(state));
    }

    @Overwrite
    public boolean matchesType(Fluid fluid) {
        return fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER;
    }

    @Overwrite
    public int getLevelDecreasePerBlock(WorldView world) {
        return 1;
    }

    @Overwrite
    public int getTickRate(WorldView world) {
        return 5;
    }

    @Overwrite
    public boolean canBeReplacedWith(FluidState state, BlockView world, BlockPos pos, Fluid fluid, Direction direction) {
        return direction == Direction.DOWN && !fluid.isIn(FluidTags.WATER);
    }

    @Overwrite
    public float getBlastResistance() {
        return 100.0f;
    }

    @Overwrite
    public Optional<SoundEvent> getBucketFillSound() {
        return Optional.of(SoundEvents.ITEM_BUCKET_FILL);
    }

    @Mixin(targets = "net.minecraft.fluid.WaterFluid$Flowing")
    public static class Flowing extends WaterFluid {
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

    @Mixin(targets = "net.minecraft.fluid.WaterFluid$Still")
    public static class Still extends WaterFluid {
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