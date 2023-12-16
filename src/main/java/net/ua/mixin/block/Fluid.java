package net.ua.mixin.block;

import java.util.Optional;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundEvent;
import net.minecraft.state.StateManager;
import net.minecraft.util.collection.IdList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;

@Mixin(net.minecraft.fluid.Fluid.class)
public abstract class Fluid {
    @Final
    @Shadow
    public static final IdList<FluidState> STATE_IDS = new IdList<>();
    @Mutable
    @Final
    @Shadow
    protected final StateManager<net.minecraft.fluid.Fluid, FluidState> stateManager;
    @Shadow
    private FluidState defaultState;
    @Final
    @Shadow
    private final RegistryEntry.Reference<net.minecraft.fluid.Fluid> registryEntry = Registries.FLUID.createEntry((net.minecraft.fluid.Fluid) (Object)this);

    public Fluid() {
        StateManager.Builder<net.minecraft.fluid.Fluid, FluidState> builder = new StateManager.Builder<>((net.minecraft.fluid.Fluid) (Object) this);
        this.appendProperties(builder);
        this.stateManager = builder.build(net.minecraft.fluid.Fluid::getDefaultState, FluidState::new);
        this.setDefaultState(this.stateManager.getDefaultState());
    }

    @Overwrite
    public void appendProperties(StateManager.Builder<net.minecraft.fluid.Fluid, FluidState> builder) {
    }

    @Overwrite
    public StateManager<net.minecraft.fluid.Fluid, FluidState> getStateManager() {
        return this.stateManager;
    }

    @Overwrite
    public final void setDefaultState(FluidState state) {
        this.defaultState = state;
    }

    @Overwrite
    public final FluidState getDefaultState() {
        return this.defaultState;
    }
    @Overwrite
    public abstract Item getBucketItem();
    @Overwrite
    public void randomDisplayTick(World world, BlockPos pos, FluidState state, Random random) {
    }
    @Overwrite
    public void onScheduledTick(World world, BlockPos pos, FluidState state) {
    }
    @Overwrite
    public void onRandomTick(World world, BlockPos pos, FluidState state, Random random) {
    }

    @Nullable
    @Overwrite
    public ParticleEffect getParticle() {
        return null;
    }
    @Overwrite
    public abstract boolean canBeReplacedWith(FluidState var1, BlockView var2, BlockPos var3, net.minecraft.fluid.Fluid var4, Direction var5);
    @Overwrite
    public abstract Vec3d getVelocity(BlockView var1, BlockPos var2, FluidState var3);
    @Overwrite
    public abstract int getTickRate(WorldView var1);
    @Overwrite
    public boolean hasRandomTicks() {
        return false;
    }
    @Overwrite
    public boolean isEmpty() {
        return false;
    }

    @Overwrite
    public abstract float getBlastResistance();
    @Overwrite
    public abstract float getHeight(FluidState var1, BlockView var2, BlockPos var3);
    @Overwrite
    public abstract float getHeight(FluidState var1);
    @Overwrite
    public abstract BlockState toBlockState(FluidState var1);
    @Overwrite
    public abstract boolean isStill(FluidState var1);
    @Overwrite
    public abstract int getLevel(FluidState var1);
    @Overwrite
    public boolean matchesType(net.minecraft.fluid.Fluid fluid) {
        return fluid == (Object) (this);
    }

    @Deprecated
    @Overwrite
    public boolean isIn(TagKey<net.minecraft.fluid.Fluid> tag) {
        return this.registryEntry.isIn(tag);
    }
    @Overwrite
    public abstract VoxelShape getShape(FluidState var1, BlockView var2, BlockPos var3);

    @Overwrite
    public Optional<SoundEvent> getBucketFillSound() {
        return Optional.empty();
    }
    @Overwrite
    @Deprecated
    public RegistryEntry.Reference<net.minecraft.fluid.Fluid> getRegistryEntry() {
        return this.registryEntry;
    }
}