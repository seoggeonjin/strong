package net.ua.mixin2.inner.goals.vex;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.util.math.BlockPos;

import java.util.EnumSet;

public class lat extends Goal {
    VexEntity v;
    public lat(VexEntity v) {
        this.v = v;
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }

    @Override
    public boolean canStart() {
        return !v.getMoveControl().isMoving() && v.getRandom().nextInt(toGoalTicks(7)) == 0;
    }

    @Override
    public boolean shouldContinue() {
        return false;
    }

    @Override
    public void tick() {
        BlockPos blockPos = v.getBounds();
        if (blockPos == null) {
            blockPos = v.getBlockPos();
        }
        for (int i = 0; i < 3; ++i) {
            BlockPos blockPos2 = blockPos.add(v.getRandom().nextInt(15) - 7, v.getRandom().nextInt(11) - 5, v.getRandom().nextInt(15) - 7);
            if (!v.getWorld().isAir(blockPos2)) continue;
            v.getMoveControl().moveTo((double)blockPos2.getX() + 0.5, (double)blockPos2.getY() + 0.5, (double)blockPos2.getZ() + 0.5, 0.25);
            if (v.getTarget() != null) break;
            v.getLookControl().lookAt((double)blockPos2.getX() + 0.5, (double)blockPos2.getY() + 0.5, (double)blockPos2.getZ() + 0.5, 180.0f, 20.0f);
            break;
        }
    }
}
