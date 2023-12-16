package net.ua.mixin.block;

import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2BooleanMap;
import it.unimi.dsi.fastutil.shorts.Short2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FluidFillable;
import net.minecraft.block.IceBlock;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(net.minecraft.fluid.FlowableFluid.class)
public abstract class FlowableFluid extends Fluid {
    @Final
    @Shadow
    public static final BooleanProperty FALLING = Properties.FALLING;
    @Final
    @Shadow
    public static final IntProperty LEVEL = Properties.LEVEL_1_8;
    @Final
    @Shadow
    private static final ThreadLocal<Object2ByteLinkedOpenHashMap<Block.NeighborGroup>> field_15901 = ThreadLocal.withInitial(() -> {
        Object2ByteLinkedOpenHashMap<Block.NeighborGroup> object2ByteLinkedOpenHashMap = new Object2ByteLinkedOpenHashMap<>(200) {

            @Override
            public void rehash(int i) {
            }
        };
        object2ByteLinkedOpenHashMap.defaultReturnValue((byte)127);
        return object2ByteLinkedOpenHashMap;
    });
    @Final
    @Shadow
    private final Map<FluidState, VoxelShape> shapeCache = Maps.newIdentityHashMap();

    @Overwrite
    public void appendProperties(StateManager.Builder<Fluid, FluidState> builder) {
        builder.add(FALLING);
    }

    @Overwrite
    public Vec3d getVelocity(BlockView world, BlockPos pos, FluidState state) {
        double d = 0.0;
        double e = 0.0;
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (Direction direction : Direction.Type.HORIZONTAL) {
            mutable.set(pos, direction);
            FluidState fluidState = world.getFluidState(mutable);
            if (!this.isEmptyOrThis(fluidState)) continue;
            float f = fluidState.getHeight();
            float g = 0.0f;
            if (f == 0.0f) {
                FluidState fluidState2;
                if (!world.getBlockState(mutable).blocksMovement() && this.isEmptyOrThis(fluidState2 = world.getFluidState(mutable.down())) && (f = fluidState2.getHeight()) > 0.0f) {
                    g = state.getHeight() - (f - 0);   // f -  - 0.8888889f
                }
            } else if (f > 0.0f) {
                g = state.getHeight() - f;
            }
            if (g == 0.0f) continue;
            d += (float)direction.getOffsetX() * g;
            e += (float)direction.getOffsetZ() * g;
        }
        Vec3d vec3d = new Vec3d(d, 0.0, e);
        if (state.get(FALLING)) {
            for (Direction direction2 : Direction.Type.HORIZONTAL) {
                mutable.set(pos, direction2);
                if (this.isFlowBlocked(world, mutable, direction2) && this.isFlowBlocked(world, mutable.up(), direction2)) continue;
                vec3d = vec3d.normalize().add(0.0, -6.0, 0.0);
                break;
            }
        }
        return vec3d.normalize();
    }

    @Overwrite
    public boolean isEmptyOrThis(FluidState state) {
        return state.isEmpty() || state.getFluid().matchesType(this);
    }

    @Overwrite
    public boolean isFlowBlocked(BlockView world, BlockPos pos, Direction direction) {
        BlockState blockState = world.getBlockState(pos);
        FluidState fluidState = world.getFluidState(pos);
        if (fluidState.getFluid().matchesType(this)) {
            return true;
        }
        if (direction == Direction.UP) {
            return false;
        }
        if (blockState.getBlock() instanceof IceBlock) {
            return true;
        }
        return !blockState.isSideSolidFullSquare(world, pos, direction);
    }

    @Overwrite
    public void tryFlow(World world, BlockPos fluidPos, FluidState state) {
        if (state.isEmpty()) {
            return;
        }
        BlockState blockState = world.getBlockState(fluidPos);
        BlockPos blockPos = fluidPos.down();
        BlockState blockState2 = world.getBlockState(blockPos);
        FluidState fluidState = this.getUpdatedState(world, blockPos, blockState2);
        if (this.canFlow(world, fluidPos, blockState, Direction.DOWN, blockPos, blockState2, world.getFluidState(blockPos), fluidState.getFluid())) {
            this.flow(world, blockPos, blockState2, Direction.DOWN, fluidState);
            if (this.countNeighboringSources(world, fluidPos) >= 3) {
                this.flowToSides(world, fluidPos, state, blockState);
            }
        } else if (state.isStill() || !this.canFlowDownTo(world, fluidState.getFluid(), fluidPos, blockState, blockPos, blockState2)) {
            this.flowToSides(world, fluidPos, state, blockState);
        }
    }

    @Overwrite
    public void flowToSides(World world, BlockPos pos, FluidState fluidState, BlockState blockState) {
        int i = fluidState.getLevel() - this.getLevelDecreasePerBlock(world);
        if (fluidState.get(FALLING)) {
            i = 7;
        }
        if (i <= 0) {
            return;
        }
        Map<Direction, FluidState> map = this.getSpread(world, pos, blockState);
        for (Map.Entry<Direction, FluidState> entry : map.entrySet()) {
            BlockState blockState2;
            Direction direction = entry.getKey();
            FluidState fluidState2 = entry.getValue();
            BlockPos blockPos = pos.offset(direction);
            if (!this.canFlow(world, pos, blockState, direction, blockPos, blockState2 = world.getBlockState(blockPos), world.getFluidState(blockPos), fluidState2.getFluid())) continue;
            this.flow(world, blockPos, blockState2, direction, fluidState2);
        }
    }

    @Overwrite
    public FluidState getUpdatedState(World world, BlockPos pos, BlockState state) {
        BlockPos blockPos2;
        BlockState blockState3;
        FluidState fluidState3;
        int i = 0;
        int j = 0;
        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos blockPos = pos.offset(direction);
            BlockState blockState = world.getBlockState(blockPos);
            FluidState fluidState = blockState.getFluidState();
            if (!fluidState.getFluid().matchesType(this) || !this.receivesFlow(direction, world, pos, state, blockPos, blockState)) continue;
            if (fluidState.isStill()) {
                ++j;
            }
            i = Math.max(i, fluidState.getLevel());
        }
        if (this.isInfinite(world) && j >= 2) {
            BlockState blockState2 = world.getBlockState(pos.down());
            FluidState fluidState2 = blockState2.getFluidState();
            if (blockState2.isSolid() || this.isMatchingAndStill(fluidState2)) {
                return this.getStill(false);
            }
        }
        if (!(fluidState3 = (blockState3 = world.getBlockState(blockPos2 = pos.up())).getFluidState()).isEmpty() && fluidState3.getFluid().matchesType(this) && this.receivesFlow(Direction.UP, world, pos, state, blockPos2, blockState3)) {
            return this.getFlowing(8, true);
        }
        int k = i - this.getLevelDecreasePerBlock(world);
        if (k <= 0) {
            return Fluids.EMPTY.getDefaultState();
        }
        return this.getFlowing(k, false);
    }

    @Overwrite
    public boolean receivesFlow(Direction face, BlockView world, BlockPos pos, BlockState state, BlockPos fromPos, BlockState fromState) {
        boolean bl;
        Block.NeighborGroup neighborGroup;
        Object2ByteLinkedOpenHashMap<Block.NeighborGroup> object2ByteLinkedOpenHashMap = state.getBlock().hasDynamicBounds() || fromState.getBlock().hasDynamicBounds() ? null : field_15901.get();
        if (object2ByteLinkedOpenHashMap != null) {
            neighborGroup = new Block.NeighborGroup(state, fromState, face);
            byte b = object2ByteLinkedOpenHashMap.getAndMoveToFirst(neighborGroup);
            if (b != 127) {
                return b != 0;
            }
        } else {
            neighborGroup = null;
        }
        bl = !VoxelShapes.adjacentSidesCoverSquare(state.getCollisionShape(world, pos), fromState.getCollisionShape(world, fromPos), face);
        if (object2ByteLinkedOpenHashMap != null) {
            if (object2ByteLinkedOpenHashMap.size() == 200) {
                object2ByteLinkedOpenHashMap.removeLastByte();
            }
            object2ByteLinkedOpenHashMap.putAndMoveToFirst(neighborGroup, (byte)(bl ? 1 : 0));
        }
        return bl;
    }

    @Overwrite
    public abstract Fluid getFlowing();

    @Overwrite
    public FluidState getFlowing(int level, boolean falling) {
        return this.getFlowing().getDefaultState().with(LEVEL, level).with(FALLING, falling);
    }

    @Overwrite
    public abstract Fluid getStill();

    @Overwrite
    public FluidState getStill(boolean falling) {
        return this.getStill().getDefaultState().with(FALLING, falling);
    }

    @Overwrite
    public abstract boolean isInfinite(World var1);

    @Overwrite
    public void flow(WorldAccess world, BlockPos pos, BlockState state, Direction direction, FluidState fluidState) {
        if (state.getBlock() instanceof FluidFillable) {
            ((FluidFillable) state.getBlock()).tryFillWithFluid(world, pos, state, fluidState);
        } else {
            if (!state.isAir()) {
                this.beforeBreakingBlock(world, pos, state);
            }
            world.setBlockState(pos, fluidState.getBlockState(), Block.NOTIFY_ALL);
        }
    }

    @Overwrite
    public abstract void beforeBreakingBlock(WorldAccess var1, BlockPos var2, BlockState var3);

    @Overwrite
    public static short packXZOffset(BlockPos from, BlockPos to) {
        int i = to.getX() - from.getX();
        int j = to.getZ() - from.getZ();
        return (short)((i + 128 & 0xFF) << 8 | j + 128 & 0xFF);
    }

    @Overwrite
    public int getFlowSpeedBetween(WorldView world, BlockPos pos, int i, Direction direction, BlockState state, BlockPos fromPos, Short2ObjectMap<Pair<BlockState, FluidState>> stateCache, Short2BooleanMap flowDownCache) {
        int j = 1000;
        for (Direction direction2 : Direction.Type.HORIZONTAL) {
            int k;
            if (direction2 == direction) continue;
            BlockPos blockPos = pos.offset(direction2);
            short s2 = packXZOffset(fromPos, blockPos);
            Pair<BlockState, FluidState> pair = stateCache.computeIfAbsent(s2, s -> {
                BlockState blockState = world.getBlockState(blockPos);
                return Pair.of(blockState, blockState.getFluidState());
            });
            BlockState blockState = pair.getFirst();
            FluidState fluidState = pair.getSecond();
            if (this.canFlowThrough(world, this.getFlowing(), pos, state, direction2, blockPos, blockState, fluidState)) continue;
            boolean bl = flowDownCache.computeIfAbsent(s2, s -> {
                BlockPos blockPos2 = blockPos.down();
                BlockState blockState2 = world.getBlockState(blockPos2);
                return this.canFlowDownTo(world, this.getFlowing(), blockPos, blockState, blockPos2, blockState2);
            });
            if (bl) {
                return i;
            }
            if (i >= this.getFlowSpeed(world) || (k = this.getFlowSpeedBetween(world, blockPos, i + 1, direction2.getOpposite(), blockState, fromPos, stateCache, flowDownCache)) >= j) continue;
            j = k;
        }
        return j;
    }

    @Overwrite
    public boolean canFlowDownTo(BlockView world, Fluid fluid, BlockPos pos, BlockState state, BlockPos fromPos, BlockState fromState) {
        if (!this.receivesFlow(Direction.DOWN, world, pos, state, fromPos, fromState)) {
            return false;
        }
        if (fromState.getFluidState().getFluid().matchesType(this)) {
            return true;
        }
        return this.canFill(world, fromPos, fromState, fluid);
    }

    @Overwrite
    public boolean canFlowThrough(BlockView world, Fluid fluid, BlockPos pos, BlockState state, Direction face, BlockPos fromPos, BlockState fromState, FluidState fluidState) {
        return this.isMatchingAndStill(fluidState) || !this.receivesFlow(face, world, pos, state, fromPos, fromState) || !this.canFill(world, fromPos, fromState, fluid);
    }

    @Overwrite
    public boolean isMatchingAndStill(FluidState state) {
        return state.getFluid().matchesType(this) && state.isStill();
    }

    @Overwrite
    public abstract int getFlowSpeed(WorldView var1);

    @Overwrite
    public int countNeighboringSources(WorldView world, BlockPos pos) {
        int i = 0;
        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos blockPos = pos.offset(direction);
            FluidState fluidState = world.getFluidState(blockPos);
            if (!this.isMatchingAndStill(fluidState)) continue;
            ++i;
        }
        return i;
    }

    @Overwrite
    public Map<Direction, FluidState> getSpread(World world, BlockPos pos, BlockState state) {
        int i = 1000;
        EnumMap<Direction, FluidState> map = Maps.newEnumMap(Direction.class);
        Short2ObjectOpenHashMap<Pair<BlockState, FluidState>> short2ObjectMap = new Short2ObjectOpenHashMap<Pair<BlockState, FluidState>>();
        Short2BooleanOpenHashMap short2BooleanMap = new Short2BooleanOpenHashMap();
        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos blockPos = pos.offset(direction);
            short s2 = packXZOffset(pos, blockPos);
            Pair<BlockState, FluidState> pair = short2ObjectMap.computeIfAbsent(s2, s -> {
                BlockState blockState = world.getBlockState(blockPos);
                return Pair.of(blockState, blockState.getFluidState());
            });
            BlockState blockState = pair.getFirst();
            FluidState fluidState = pair.getSecond();
            FluidState fluidState2 = this.getUpdatedState(world, blockPos, blockState);
            if (this.canFlowThrough(world, fluidState2.getFluid(), pos, state, direction, blockPos, blockState, fluidState)) continue;
            BlockPos blockPos2 = blockPos.down();
            boolean bl = short2BooleanMap.computeIfAbsent(s2, s -> {
                BlockState blockState2 = world.getBlockState(blockPos2);
                return this.canFlowDownTo(world, this.getFlowing(), blockPos, blockState, blockPos2, blockState2);
            });
            int j = bl ? 0 : this.getFlowSpeedBetween(world, blockPos, 1, direction.getOpposite(), blockState, pos, short2ObjectMap, short2BooleanMap);
            if (j < i) {
                map.clear();
            }
            if (j > i) continue;
            map.put(direction, fluidState2);
            i = j;
        }
        return map;
    }

    @Overwrite
    public boolean canFill(BlockView world, BlockPos pos, BlockState state, Fluid fluid) {
        Block block = state.getBlock();
        if (block instanceof FluidFillable) {
            FluidFillable fluidFillable = (FluidFillable)((Object)block);
            return fluidFillable.canFillWithFluid(null, world, pos, state, fluid);
        }
        if (block instanceof DoorBlock || state.isIn(BlockTags.SIGNS) || state.isOf(Blocks.LADDER) || state.isOf(Blocks.SUGAR_CANE) || state.isOf(Blocks.BUBBLE_COLUMN)) {
            return false;
        }
        if (state.isOf(Blocks.NETHER_PORTAL) || state.isOf(Blocks.END_PORTAL) || state.isOf(Blocks.END_GATEWAY) || state.isOf(Blocks.STRUCTURE_VOID)) {
            return false;
        }
        return !state.blocksMovement();
    }

    @Overwrite
    public boolean canFlow(BlockView world, BlockPos fluidPos, BlockState fluidBlockState, Direction flowDirection, BlockPos flowTo, BlockState flowToBlockState, FluidState fluidState, Fluid fluid) {
        return fluidState.canBeReplacedWith(world, flowTo, fluid, flowDirection) && this.receivesFlow(flowDirection, world, fluidPos, fluidBlockState, flowTo, flowToBlockState) && this.canFill(world, flowTo, flowToBlockState, fluid);
    }

    @Overwrite
    public abstract int getLevelDecreasePerBlock(WorldView var1);

    @Overwrite
    public int getNextTickDelay(World world, BlockPos pos, FluidState oldState, FluidState newState) {
        return this.getTickRate(world);
    }

    @Overwrite
    public void onScheduledTick(World world, BlockPos pos, FluidState state) {
        if (!state.isStill()) {
            FluidState fluidState = this.getUpdatedState(world, pos, world.getBlockState(pos));
            int i = this.getNextTickDelay(world, pos, state, fluidState);
            if (fluidState.isEmpty()) {
                state = fluidState;
                world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
            } else if (!fluidState.equals(state)) {
                state = fluidState;
                BlockState blockState = state.getBlockState();
                world.setBlockState(pos, blockState, Block.NOTIFY_LISTENERS);
                world.scheduleFluidTick(pos, state.getFluid(), i);
                world.updateNeighborsAlways(pos, blockState.getBlock());
            }
        }
        this.tryFlow(world, pos, state);
    }

    @Overwrite
    public static int getBlockStateLevel(FluidState state) {
        if (state.isStill()) {
            return 0;
        }
        return 8 - Math.min(state.getLevel(), 8) + (state.get(FALLING) ? 8 : 0);
    }

    @Overwrite
    public static boolean isFluidAboveEqual(FluidState state, BlockView world, BlockPos pos) {
        return state.getFluid().matchesType(world.getFluidState(pos.up()).getFluid());
    }

    @Overwrite
    public float getHeight(FluidState state, BlockView world, BlockPos pos) {
        if (isFluidAboveEqual(state, world, pos)) {
            return 1.0f;
        }
        return state.getHeight();
    }

    @Overwrite
    public float getHeight(FluidState state) {
        return (float)state.getLevel() / 9.0f;
    }

    @Overwrite
    public abstract int getLevel(FluidState var1);

    @Overwrite
    public VoxelShape getShape(FluidState state, BlockView world, BlockPos pos) {
        if (state.getLevel() == 9 && isFluidAboveEqual(state, world, pos)) {
            return VoxelShapes.fullCube();
        }
        return this.shapeCache.computeIfAbsent(state, state2 -> VoxelShapes.cuboid(0.0, 0.0, 0.0, 1.0, state2.getHeight(world, pos), 1.0));
    }
}
