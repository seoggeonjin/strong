package net.ua.mixin2.inner.goals.vex;

import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class vmc extends MoveControl {
    private VexEntity v;
    public vmc(VexEntity owner) {
        super(owner);
        v = owner;
    }

    @Override
    public void tick() {
        if (this.state != MoveControl.State.MOVE_TO) {
            return;
        }
        Vec3d vec3d = new Vec3d(this.targetX - v.getX(), this.targetY - v.getY(), this.targetZ - v.getZ());
        double d = vec3d.length();
        if (d < v.getBoundingBox().getAverageSideLength()) {
            this.state = MoveControl.State.WAIT;
            v.setVelocity(v.getVelocity().multiply(0.5));
        } else {
            v.setVelocity(v.getVelocity().add(vec3d.multiply(this.speed * 0.05 / d)));
            if (v.getTarget() == null) {
                Vec3d vec3d2 = v.getVelocity();
                v.setYaw(-((float) MathHelper.atan2(vec3d2.x, vec3d2.z)) * 57.295776f);
            } else {
                double e = v.getTarget().getX() - v.getX();
                double f = v.getTarget().getZ() - v.getZ();
                v.setYaw(-((float)MathHelper.atan2(e, f)) * 57.295776f);
            }
            v.bodyYaw = v.getYaw();
        }
    }
}