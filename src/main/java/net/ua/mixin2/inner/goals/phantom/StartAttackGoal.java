package net.ua.mixin2.inner.goals.phantom;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.ua.ips.phi;
import net.ua.mixin2.inner.otherwise.PhantomMovementType;

public class StartAttackGoal extends Goal {
    private int cooldown;
    private final phi ph;
    public boolean b = false;
    private int ak = 0;

    public StartAttackGoal(phi ph) {
        this.ph = ph;
    }

    @Override
    public boolean canStart() {
        LivingEntity livingEntity = ph.getTarget();
        if (livingEntity != null) {
            return ph.isTarget(livingEntity, TargetPredicate.DEFAULT);
        }
        return false;
    }

    @Override
    public void start() {
        this.cooldown = this.getTickCount(10);
        ph.movementType(PhantomMovementType.CIRCLE);
        this.startSwoop();
    }

    @Override
    public void stop() {
        ph.circlingCenter(ph.getWorld().getTopPosition(Heightmap.Type.MOTION_BLOCKING, ph.circlingCenter()).up(10 + ph.random().nextInt(20)));
    }

    @Override
    public void tick() {
        if (ph.movementType() == PhantomMovementType.CIRCLE) {
            --this.cooldown;
            if (this.cooldown <= 0) {
                b = true;
                ph.movementType(PhantomMovementType.SWOOP);
                this.startSwoop();
                this.cooldown = ak = this.getTickCount((8 + ph.random().nextInt(4)) * 20);
                ph.playSound(SoundEvents.ENTITY_PHANTOM_SWOOP, 10.0f, 0.95f + ph.random().nextFloat() * 0.1f);
            }
        }
    }

    private void startSwoop() {
        ph.circlingCenter(ph.getTarget().getBlockPos().up(20 + ph.random().nextInt(20)));
        if (ph.circlingCenter().getY() < ph.getWorld().getSeaLevel()) {
            ph.circlingCenter(new BlockPos(ph.circlingCenter().getX(), ph.getWorld().getSeaLevel() + 1, ph.circlingCenter().getZ()));
        }
    }
}