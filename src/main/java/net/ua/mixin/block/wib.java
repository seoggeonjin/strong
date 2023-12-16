package net.ua.mixin.block;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.block.pattern.BlockPatternBuilder;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.predicate.block.BlockStatePredicate;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.ua.ips.wi;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WitherSkullBlock.class)
public class wib extends SkullBlock {
    @Unique
    @Nullable
    private static BlockPattern witherBossPattern;
    @Unique
    @Nullable
    private static BlockPattern witherDispenserPattern;

    public wib(AbstractBlock.Settings settings) {
        super(SkullBlock.Type.WITHER_SKELETON, settings);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof SkullBlockEntity) {
            WitherSkullBlock.onPlaced(world, pos, (SkullBlockEntity)blockEntity);
        }
    }

    @Inject(method = "onPlaced(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/entity/SkullBlockEntity;)V", at = @At("HEAD"))
    private static void onPlaced(World world, BlockPos pos, SkullBlockEntity blockEntity, CallbackInfo ci) {
        boolean bl;
        if (world.isClient) {
            return;
        }
        BlockState blockState = blockEntity.getCachedState();
        boolean bl2 = bl = blockState.isOf(Blocks.WITHER_SKELETON_SKULL) || blockState.isOf(Blocks.WITHER_SKELETON_WALL_SKULL);
        if (!bl || pos.getY() < world.getBottomY() || world.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        BlockPattern.Result result = getWitherBossPattern().searchAround(world, pos);
        if (result == null) {
            return;
        }
        wi witherEntity = (wi) EntityType.WITHER.create(world);
        if (witherEntity != null) {
            CarvedPumpkinBlock.breakPatternBlocks(world, result);
            BlockPos blockPos = result.translate(1, 2, 0).getBlockPos();
            witherEntity.refreshPositionAndAngles((double)blockPos.getX() + 0.5, (double)blockPos.getY() + 0.55, (double)blockPos.getZ() + 0.5, result.getForwards().getAxis() == Direction.Axis.X ? 0.0f : 90.0f, 0.0f);
            witherEntity.sdy(result.getForwards().getAxis() == Direction.Axis.X ? 0.0f : 90.0f);
            witherEntity.onSummoned();
            for (ServerPlayerEntity serverPlayerEntity : world.getNonSpectatingEntities(ServerPlayerEntity.class, witherEntity.getBoundingBox2().expand(50.0))) {
                Criteria.SUMMONED_ENTITY.trigger(serverPlayerEntity, (Entity) witherEntity);
            }
            world.spawnEntity((Entity) witherEntity);
            CarvedPumpkinBlock.updatePatternBlocks(world, result);
        }
    }

    @Inject(method = "canDispense", at = @At("RETURN"), cancellable = true)
    private static void canDispense(World world, BlockPos pos, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.isOf(Items.WITHER_SKELETON_SKULL) && pos.getY() >= world.getBottomY() + 2 && world.getDifficulty() != Difficulty.PEACEFUL && !world.isClient) {
            cir.setReturnValue(getWitherDispenserPattern().searchAround(world, pos) != null);
        }
        cir.setReturnValue(false);
    }

    @Unique
    private static BlockPattern getWitherBossPattern() {
        if (witherBossPattern == null) {
            witherBossPattern = BlockPatternBuilder.start().aisle("^^^", "###", "~#~").where('#', pos -> pos.getBlockState().isIn(BlockTags.WITHER_SUMMON_BASE_BLOCKS)).where('^', CachedBlockPosition.matchesBlockState(BlockStatePredicate.forBlock(Blocks.WITHER_SKELETON_SKULL).or(BlockStatePredicate.forBlock(Blocks.WITHER_SKELETON_WALL_SKULL)))).where('~', pos -> pos.getBlockState().isAir()).build();
        }
        return witherBossPattern;
    }

    @Unique
    private static BlockPattern getWitherDispenserPattern() {
        if (witherDispenserPattern == null) {
            witherDispenserPattern = BlockPatternBuilder.start().aisle("   ", "###", "~#~").where('#', pos -> pos.getBlockState().isIn(BlockTags.WITHER_SUMMON_BASE_BLOCKS)).where('~', pos -> pos.getBlockState().isAir()).build();
        }
        return witherDispenserPattern;
    }
}
