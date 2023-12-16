package net.ua.mixin2.inner.goals.phantom;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldEvents;
import net.ua.ips.phi;
import net.ua.mixin2.inner.goals.phantom.MovementGoal;
import net.ua.mixin2.inner.otherwise.PhantomMovementType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SwoopMovementGoal extends MovementGoal {
    private static final int CAT_CHECK_INTERVAL = 20;
    private boolean catsNearby;
    private int nextCatCheckAge;
    public final phi ph;
    public boolean b = false;
    public int a = 0;
    @Nullable
    public LivingEntity l = null;
    public SwoopMovementGoal(phi ph) {
        super(ph);
        this.ph = ph;
    }

    @Override
    public boolean canStart() {
        return ph.getTarget() != null && ph.movementType() == PhantomMovementType.SWOOP;
    }

    @Override
    public boolean shouldContinue() {
        LivingEntity livingEntity = ph.getTarget();
        if (livingEntity == null) {
            return false;
        }
        if (!livingEntity.isAlive()) {
            return false;
        }
        if (livingEntity instanceof PlayerEntity playerEntity) {
            if (livingEntity.isSpectator() || playerEntity.isCreative()) {
                return false;
            }
        }
        if (!this.canStart()) {
            return false;
        }
        if (ph.age() > this.nextCatCheckAge) {
            this.nextCatCheckAge = ph.age() + 20;
            List<CatEntity> list = ph.getWorld().getEntitiesByClass(CatEntity.class, ph.getBoundingBox2().expand(16.0), EntityPredicates.VALID_ENTITY);
            for (CatEntity catEntity : list) {
                catEntity.hiss();
            }
            this.catsNearby = !list.isEmpty();
        }
        return !this.catsNearby;
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
        ph.setTarget(null);
        ph.movementType(PhantomMovementType.CIRCLE);
    }

    @Override
    public void tick() {
        LivingEntity livingEntity = ph.getTarget();
        if (livingEntity == null) {
            return;
        }
        ph.targetPosition(new Vec3d(livingEntity.getX(), livingEntity.getBodyY(0.5), livingEntity.getZ()));
        if (ph.getBoundingBox2().expand(0.5f).intersects(livingEntity.getBoundingBox().expand(0.5f))) {
            ph.setG(l);
            ph.tryAttack(livingEntity);
            ph.movementType(PhantomMovementType.CIRCLE);
            if (!ph.isSilent()) {
                ph.getWorld().syncWorldEvent(WorldEvents.PHANTOM_BITES, ph.getBlockPos(), 0);
            }
        } else if (ph.horizontalCollision() || ph.hurtTime() > 0) {
            ph.movementType(PhantomMovementType.CIRCLE);
        }
    }
}
