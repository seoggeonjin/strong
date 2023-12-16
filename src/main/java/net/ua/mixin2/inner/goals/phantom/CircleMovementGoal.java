package net.ua.mixin2.inner.goals.phantom;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.ua.ips.phi;
import net.ua.mixin2.inner.goals.phantom.MovementGoal;
import net.ua.mixin2.inner.otherwise.PhantomMovementType;

public class CircleMovementGoal extends MovementGoal {
    private float angle;
    private float radius;
    private float yOffset;
    private float circlingDirection;
    private final phi ph;
    public CircleMovementGoal(phi ph) {
        super(ph);
        this.ph = ph;
    }

    @Override
    public boolean canStart() {
        return ph.getTarget() == null || ph.movementType() == PhantomMovementType.CIRCLE;
    }

    @Override
    public void start() {
        this.radius = 5.0f + ph.random().nextFloat() * 10.0f;
        this.yOffset = -4.0f + ph.random().nextFloat() * 9.0f;
        this.circlingDirection = ph.random().nextBoolean() ? 1.0f : -1.0f;
        this.adjustDirection();
    }

    @Override
    public void tick() {
        if (ph.random().nextInt(this.getTickCount(350)) == 0) {
            this.yOffset = -4.0f + ph.random().nextFloat() * 9.0f;
        }
        if (ph.random().nextInt(this.getTickCount(250)) == 0) {
            this.radius += 1.0f;
            if (this.radius > 15.0f) {
                this.radius = 5.0f;
                this.circlingDirection = -this.circlingDirection;
            }
        }
        if (ph.random().nextInt(this.getTickCount(450)) == 0) {
            this.angle = ph.random().nextFloat() * 2.0f * (float)Math.PI;
            this.adjustDirection();
        }
        if (this.isNearTarget()) {
            this.adjustDirection();
        }
        if (ph.targetPosition().y < ph.getY2() && !ph.getWorld().isAir(ph.getBlockPos().down(1))) {
            this.yOffset = Math.max(1.0f, this.yOffset);
            this.adjustDirection();
        }
        if (ph.targetPosition().y > ph.getY2() && !ph.getWorld().isAir(ph.getBlockPos().up(1))) {
            this.yOffset = Math.min(-1.0f, this.yOffset);
            this.adjustDirection();
        }
    }

    private void adjustDirection() {
        if (BlockPos.ORIGIN.equals(ph.circlingCenter())) {
            ph.circlingCenter(ph.getBlockPos());
        }
        this.angle += this.circlingDirection * 15.0f * ((float)Math.PI / 180);
        ph.targetPosition(Vec3d.of(ph.circlingCenter()).add(this.radius * MathHelper.cos(this.angle), -4.0f + this.yOffset, this.radius * MathHelper.sin(this.angle)));
    }
}