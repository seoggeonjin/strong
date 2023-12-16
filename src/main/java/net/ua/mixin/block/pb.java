package net.ua.mixin.block;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.block.enums.PistonType;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.RedstoneView;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.ua.mixin2.ph;
import org.spongepowered.asm.mixin.*;

@Mixin(PistonBlock.class)
public abstract class pb extends FacingBlock {
    // @Shadow
    // public static final MapCodec<PistonBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(((MapCodec) Codec.BOOL.fieldOf("sticky")).forGetter(block -> ((pb)block).sticky), PistonBlock.createSettingsCodec()).apply((Applicative<pb, ?>)instance, PistonBlock::new));
    @Final
    @Shadow
    public static final BooleanProperty EXTENDED = Properties.EXTENDED;
    @Final
    @Shadow
    protected static final VoxelShape EXTENDED_EAST_SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 12.0, 16.0, 16.0);
    @Final
    @Shadow
    protected static final VoxelShape EXTENDED_WEST_SHAPE = Block.createCuboidShape(4.0, 0.0, 0.0, 16.0, 16.0, 16.0);
    @Final
    @Shadow
    protected static final VoxelShape EXTENDED_SOUTH_SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 16.0, 12.0);
    @Final
    @Shadow
    protected static final VoxelShape EXTENDED_NORTH_SHAPE = Block.createCuboidShape(0.0, 0.0, 4.0, 16.0, 16.0, 16.0);
    @Final
    @Shadow
    protected static final VoxelShape EXTENDED_UP_SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 12.0, 16.0);
    @Final
    @Shadow
    protected static final VoxelShape EXTENDED_DOWN_SHAPE = Block.createCuboidShape(0.0, 4.0, 0.0, 16.0, 16.0, 16.0);
    @Mutable
    @Final
    @Shadow
    private final boolean sticky;

    public pb(boolean sticky, AbstractBlock.Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH).with(EXTENDED, false));
        this.sticky = sticky;
    }
    // @Overwrite
    // public MapCodec<PistonBlock> getCodec() {
    //     return CODEC;
    // }

    @Overwrite
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (state.get(EXTENDED)) {
            switch (state.get(FACING)) {
                case DOWN: {
                    return EXTENDED_DOWN_SHAPE;
                }
                default: {
                    return EXTENDED_UP_SHAPE;
                }
                case NORTH: {
                    return EXTENDED_NORTH_SHAPE;
                }
                case SOUTH: {
                    return EXTENDED_SOUTH_SHAPE;
                }
                case WEST: {
                    return EXTENDED_WEST_SHAPE;
                }
                case EAST:
            }
            return EXTENDED_EAST_SHAPE;
        }
        return VoxelShapes.fullCube();
    }

    @Overwrite
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        if (!world.isClient) {
            this.tryMove(world, pos, state);
        }
    }

    @Overwrite
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if (!world.isClient) {
            this.tryMove(world, pos, state);
        }
    }

    @Overwrite
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (oldState.isOf(state.getBlock())) {
            return;
        }
        if (!world.isClient && world.getBlockEntity(pos) == null) {
            this.tryMove(world, pos, state);
        }
    }

    @Overwrite
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getPlayerLookDirection().getOpposite()).with(EXTENDED, false);
    }

    @Overwrite
    public void tryMove(World world, BlockPos pos, BlockState state) {
        Direction direction = state.get(FACING);
        boolean bl = this.shouldExtend(world, pos, direction);
        if (bl && !state.get(EXTENDED)) {
            if (new ph(world, pos, direction, true).calculatePush()) {
                world.addSyncedBlockEvent(pos, this, 0, direction.getId());
            }
        } else if (!bl && state.get(EXTENDED)) {
            PistonBlockEntity pistonBlockEntity;
            BlockEntity blockEntity;
            BlockPos blockPos = pos.offset(direction, 2);
            BlockState blockState = world.getBlockState(blockPos);
            int i = 1;
            if (blockState.isOf(Blocks.MOVING_PISTON) && blockState.get(FACING) == direction && (blockEntity = world.getBlockEntity(blockPos)) instanceof PistonBlockEntity && (pistonBlockEntity = (PistonBlockEntity)blockEntity).isExtending() && (pistonBlockEntity.getProgress(0.0f) < 0.5f || world.getTime() == pistonBlockEntity.getSavedWorldTime() || ((ServerWorld)world).isInBlockTick())) {
                i = 2;
            }
            world.addSyncedBlockEvent(pos, this, i, direction.getId());
        }
    }

    @Overwrite
    public boolean shouldExtend(RedstoneView world, BlockPos pos, Direction pistonFace) {
        for (Direction direction : Direction.values()) {
            if (direction == pistonFace || !world.isEmittingRedstonePower(pos.offset(direction), direction)) continue;
            return true;
        }
        if (world.isEmittingRedstonePower(pos, Direction.DOWN)) {
            return true;
        }
        BlockPos blockPos = pos.up();
        for (Direction direction2 : Direction.values()) {
            if (direction2 == Direction.DOWN || !world.isEmittingRedstonePower(blockPos.offset(direction2), direction2)) continue;
            return true;
        }
        return false;
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    @Overwrite
    public boolean onSyncedBlockEvent(BlockState state, World world, BlockPos pos, int type, int data) {
        Direction direction = state.get(FACING);
        BlockState blockState = state.with(EXTENDED, true);
        if (!world.isClient) {
            boolean bl = this.shouldExtend(world, pos, direction);
            if (bl && (type == 1 || type == 2)) {
                world.setBlockState(pos, blockState, Block.NOTIFY_LISTENERS);
                return false;
            }
            if (!bl && type == 0) {
                return false;
            }
        }
        if (type == 0) {
            if (!this.move(world, pos, direction, true)) return false;
            world.setBlockState(pos, blockState, Block.NOTIFY_ALL | Block.MOVED);
            world.playSound(null, pos, SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.BLOCKS, 0.5f, world.random.nextFloat() * 0.25f + 0.6f);
            world.emitGameEvent(GameEvent.BLOCK_ACTIVATE, pos, GameEvent.Emitter.of(blockState));
            return true;
        } else {
            if (type != 1 && type != 2) return true;
            BlockEntity blockEntity = world.getBlockEntity(pos.offset(direction));
            if (blockEntity instanceof PistonBlockEntity) {
                ((PistonBlockEntity)blockEntity).finish();
            }
            BlockState blockState2 = Blocks.MOVING_PISTON.getDefaultState().with(PistonExtensionBlock.FACING, direction).with(PistonExtensionBlock.TYPE, this.sticky ? PistonType.STICKY : PistonType.DEFAULT);
            world.setBlockState(pos, blockState2, Block.NO_REDRAW | Block.FORCE_STATE);
            world.addBlockEntity(PistonExtensionBlock.createBlockEntityPiston(pos, blockState2, this.getDefaultState().with(FACING, Direction.byId(data & 7)), direction, false, true));
            world.updateNeighbors(pos, blockState2.getBlock());
            blockState2.updateNeighbors(world, pos, Block.NOTIFY_LISTENERS);
            if (this.sticky) {
                PistonBlockEntity pistonBlockEntity;
                BlockEntity blockEntity2;
                BlockPos blockPos = pos.add(direction.getOffsetX() * 2, direction.getOffsetY() * 2, direction.getOffsetZ() * 2);
                BlockState blockState3 = world.getBlockState(blockPos);
                boolean bl2 = false;
                if (blockState3.isOf(Blocks.MOVING_PISTON) && (blockEntity2 = world.getBlockEntity(blockPos)) instanceof PistonBlockEntity && (pistonBlockEntity = (PistonBlockEntity)blockEntity2).getFacing() == direction && pistonBlockEntity.isExtending()) {
                    pistonBlockEntity.finish();
                    bl2 = true;
                }
                if (!bl2) {
                    if (type == 1 && !blockState3.isAir() && net.minecraft.block.PistonBlock.isMovable(blockState3, world, blockPos, direction.getOpposite(), false, direction) && (blockState3.getPistonBehavior() == PistonBehavior.NORMAL || blockState3.isOf(Blocks.PISTON) || blockState3.isOf(Blocks.STICKY_PISTON))) {
                        this.move(world, pos, direction, false);
                    } else {
                        world.removeBlock(pos.offset(direction), false);
                    }
                }
            } else {
                world.removeBlock(pos.offset(direction), false);
            }
            world.playSound(null, pos, SoundEvents.BLOCK_PISTON_CONTRACT, SoundCategory.BLOCKS, 0.5f, world.random.nextFloat() * 0.15f + 0.6f);
            world.emitGameEvent(GameEvent.BLOCK_DEACTIVATE, pos, GameEvent.Emitter.of(blockState2));
        }
        return true;
    }

    @Overwrite
    public static boolean isMovable(BlockState state, World world, BlockPos pos, Direction direction, boolean canBreak, Direction pistonDir) {
        if (pos.getY() < world.getBottomY() || pos.getY() > world.getTopY() - 1 || !world.getWorldBorder().contains(pos)) {
            return false;
        }
        if (state.isAir()) {
            return true;
        }
        if (state.isOf(Blocks.OBSIDIAN) || state.isOf(Blocks.CRYING_OBSIDIAN) || state.isOf(Blocks.RESPAWN_ANCHOR) || state.isOf(Blocks.REINFORCED_DEEPSLATE)) {
            return false;
        }
        if (direction == Direction.DOWN && pos.getY() == world.getBottomY()) {
            return false;
        }
        if (direction == Direction.UP && pos.getY() == world.getTopY() - 1) {
            return false;
        }
        if (state.isOf(Blocks.PISTON) || state.isOf(Blocks.STICKY_PISTON)) {
            if (state.get(EXTENDED)) {
                return false;
            }
        } else {
            if (state.getHardness(world, pos) == -1.0f) {
                return false;
            }
            switch (state.getPistonBehavior()) {
                case BLOCK: {
                    return false;
                }
                case DESTROY: {
                    return canBreak;
                }
                case PUSH_ONLY: {
                    return direction == pistonDir;
                }
            }
        }
        return !state.hasBlockEntity();
    }
    @Overwrite
    public boolean move(World world, BlockPos pos, Direction dir, boolean retract) {
        int k;
        BlockPos blockPos5;
        BlockPos blockPos3;
        int j;
        ph ph;
        BlockPos blockPos = pos.offset(dir);
        if (!retract && world.getBlockState(blockPos).isOf(Blocks.PISTON_HEAD)) {
            world.setBlockState(blockPos, Blocks.AIR.getDefaultState(), Block.NO_REDRAW | Block.FORCE_STATE);
        }
        if (!(ph = new ph(world, pos, dir, retract)).calculatePush()) {
            return false;
        }
        HashMap<BlockPos, BlockState> map = Maps.newHashMap();
        List<BlockPos> list = ph.getMovedBlocks();
        ArrayList<BlockState> list2 = Lists.newArrayList();
        for (BlockPos blockPos2 : list) {
            BlockState blockState = world.getBlockState(blockPos2);
            list2.add(blockState);
            map.put(blockPos2, blockState);
        }
        List<BlockPos> list3 = ph.getBrokenBlocks();
        BlockState[] blockStates = new BlockState[list.size() + list3.size()];
        Direction direction = retract ? dir : dir.getOpposite();
        int i = 0;
        for (j = list3.size() - 1; j >= 0; --j) {
            blockPos3 = list3.get(j);
            BlockState blockState = world.getBlockState(blockPos3);
            BlockEntity blockEntity = blockState.hasBlockEntity() ? world.getBlockEntity(blockPos3) : null;
            net.minecraft.block.PistonBlock.dropStacks(blockState, world, blockPos3, blockEntity);
            world.setBlockState(blockPos3, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
            world.emitGameEvent(GameEvent.BLOCK_DESTROY, blockPos3, GameEvent.Emitter.of(blockState));
            if (!blockState.isIn(BlockTags.FIRE)) {
                world.addBlockBreakParticles(blockPos3, blockState);
            }
            blockStates[i++] = blockState;
        }
        for (j = list.size() - 1; j >= 0; --j) {
            blockPos3 = list.get(j);
            BlockState blockState = world.getBlockState(blockPos3);
            blockPos3 = blockPos3.offset(direction);
            map.remove(blockPos3);
            BlockState blockState3 = Blocks.MOVING_PISTON.getDefaultState().with(FACING, dir);
            world.setBlockState(blockPos3, blockState3, Block.NO_REDRAW | Block.MOVED);
            world.addBlockEntity(PistonExtensionBlock.createBlockEntityPiston(blockPos3, blockState3, list2.get(j), dir, retract, false));
            blockStates[i++] = blockState;
        }
        if (retract) {
            PistonType pistonType = this.sticky ? PistonType.STICKY : PistonType.DEFAULT;
            BlockState blockState4 = Blocks.PISTON_HEAD.getDefaultState().with(PistonHeadBlock.FACING, dir).with(PistonHeadBlock.TYPE, pistonType);
            BlockState blockState = Blocks.MOVING_PISTON.getDefaultState().with(PistonExtensionBlock.FACING, dir).with(PistonExtensionBlock.TYPE, this.sticky ? PistonType.STICKY : PistonType.DEFAULT);
            map.remove(blockPos);
            world.setBlockState(blockPos, blockState, Block.NO_REDRAW | Block.MOVED);
            world.addBlockEntity(PistonExtensionBlock.createBlockEntityPiston(blockPos, blockState, blockState4, dir, true, true));
        }
        BlockState blockState5 = Blocks.AIR.getDefaultState();
        for (BlockPos blockPos2 : map.keySet()) {
            world.setBlockState(blockPos2, blockState5, Block.NOTIFY_LISTENERS | Block.FORCE_STATE | Block.MOVED);
        }
        for (Map.Entry entry : map.entrySet()) {
            blockPos5 = (BlockPos)entry.getKey();
            BlockState blockState6 = (BlockState)entry.getValue();
            blockState6.prepare(world, blockPos5, 2);
            blockState5.updateNeighbors(world, blockPos5, Block.NOTIFY_LISTENERS);
            blockState5.prepare(world, blockPos5, 2);
        }
        i = 0;
        for (k = list3.size() - 1; k >= 0; --k) {
            BlockState blockState = blockStates[i++];
            blockPos5 = list3.get(k);
            blockState.prepare(world, blockPos5, 2);
            world.updateNeighborsAlways(blockPos5, blockState.getBlock());
        }
        for (k = list.size() - 1; k >= 0; --k) {
            world.updateNeighborsAlways(list.get(k), blockStates[i++].getBlock());
        }
        if (retract) {
            world.updateNeighborsAlways(blockPos, Blocks.PISTON_HEAD);
        }
        return true;
    }

    @Overwrite
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Overwrite
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Overwrite
    public void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, EXTENDED);
    }

    @Overwrite
    public boolean hasSidedTransparency(BlockState state) {
        return state.get(EXTENDED);
    }

    @Overwrite
    public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
        return false;
    }
}
