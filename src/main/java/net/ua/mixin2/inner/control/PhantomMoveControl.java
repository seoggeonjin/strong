package net.ua.mixin2.inner.control;

import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.ua.ips.phi;

public class PhantomMoveControl extends MoveControl {
    private float targetSpeed;
    private final phi p;

    public PhantomMoveControl(phi owner) {
        super((MobEntity) owner);
        this.targetSpeed = 0.1f;
        this.p = owner;
    }

    @Override
    public void tick() {
        if (p.horizontalCollision()) {
            p.setYaw(p.getYaw() + 180.0f);
            this.targetSpeed = 0.1f;
        }
        double d = p.targetPosition().x - p.getX2();
        double e = p.targetPosition().y - p.getY2();
        double f = p.targetPosition().z - p.getZ2();
        double g = Math.sqrt(d * d + f * f);
        if (Math.abs(g) > (double)1.0E-5f) {
            double h = 1.0 - Math.abs(e * (double)0.7f) / g;
            g = Math.sqrt((d *= h) * d + (f *= h) * f);
            double i = Math.sqrt(d * d + f * f + e * e);
            float j = p.getYaw();
            float k = (float) MathHelper.atan2(f, d);
            float l = MathHelper.wrapDegrees(p.getYaw() + 90.0f);
            float m = MathHelper.wrapDegrees(k * 57.295776f);
            p.setYaw(MathHelper.stepUnwrappedAngleTowards(l, m, 4.0f) - 90.0f);
            p.bodyYaw(p.getYaw());
            this.targetSpeed = MathHelper.angleBetween(j, p.getYaw()) < 3.0f ? MathHelper.stepTowards(this.targetSpeed, 1.8f, 0.005f * (1.8f / this.targetSpeed)) : MathHelper.stepTowards(this.targetSpeed, 0.2f, 0.025f);
            float n = (float)(-(MathHelper.atan2(-e, g) * 57.2957763671875));
            p.setPitch(n);
            float o = p.getYaw() + 90.0f;
            double p2 = (double)(this.targetSpeed * MathHelper.cos(o * ((float)Math.PI / 180))) * Math.abs(d / i);
            double q = (double)(this.targetSpeed * MathHelper.sin(o * ((float)Math.PI / 180))) * Math.abs(f / i);
            double r = (double)(this.targetSpeed * MathHelper.sin(n * ((float)Math.PI / 180))) * Math.abs(e / i);
            Vec3d vec3d = p.getVelocity();
            p.setVelocity(vec3d.add(new Vec3d(p2, r, q).subtract(vec3d).multiply(0.2)));
        }
    }
}
