package net.ua.mixin.block;

import net.minecraft.block.Block;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Block.class)
public class BlockMixin {
    /**
     * @author Seoggeonjin
     * @reason test
     */
    @Overwrite
    public void dropExperience(ServerWorld world, BlockPos pos, int size) {
        ExperienceOrbEntity.spawn(world, Vec3d.ofCenter(pos), size * 10);
    }
    // /**
    //  * @author Seoggeonjin
    //  * @reason test
    //  */
    // @Overwrite
    // public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
    //     if (state.getBlock() == Blocks.RED_CONCRETE) {
    //         entity.kill();
    //     }
    //     if (entity.getType() != EntityType.PLAYER) {
    //         Vec3d v = new Vec3d(0 , 1, 0);
    //         entity.setVelocity(v);
    //     } else {
    //     }
    // }
    /**
     * @author Seoggeonjin
     * @reason test
     */
    @Overwrite
    public MutableText getName() {
        return Text.literal("기반암");
    }
    // /**
    //  * @author Seoggeonjin
    //  * @reason test
    //  */
    // @Overwrite
    // public float getSlipperiness() {
    //     return 1.1F;
    // }
}