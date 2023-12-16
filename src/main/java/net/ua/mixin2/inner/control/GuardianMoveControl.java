package net.ua.mixin2.inner.control;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.control.LookControl;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.ua.ips.gui;

public class GuardianMoveControl extends MoveControl {
    private final gui guardian;

    public GuardianMoveControl(gui guardian) {
        super((MobEntity) guardian);
        this.guardian = guardian;
    }

    @Override
    public void tick() {
        if (this.state != MoveControl.State.MOVE_TO || ((GuardianEntity)this.guardian).getNavigation().isIdle()) {
            ((GuardianEntity)this.guardian).setMovementSpeed(0.0f);
            this.guardian.setSpikesRetracted(false);
            return;
        }
        Vec3d vec3d = new Vec3d(this.targetX - ((Entity)this.guardian).getX(), this.targetY - ((Entity)this.guardian).getY(), this.targetZ - ((Entity)this.guardian).getZ());
        double d = vec3d.length();
        double e = vec3d.x / d;
        double f = vec3d.y / d;
        double g = vec3d.z / d;
        float h = (float)(MathHelper.atan2(vec3d.z, vec3d.x) * 57.2957763671875) - 90.0f;
        ((Entity)this.guardian).setYaw(this.wrapDegrees(((Entity)this.guardian).getYaw(), h, 90.0f));
        ((GuardianEntity)this.guardian).bodyYaw = ((Entity)this.guardian).getYaw();
        float i = (float)(this.speed * ((GuardianEntity)this.guardian).getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED));
        float j = MathHelper.lerp(0.125f, ((GuardianEntity)this.guardian).getMovementSpeed(), i);
        ((GuardianEntity)this.guardian).setMovementSpeed(j);
        double k = Math.sin((double)(((Entity)this.guardian).age + ((Entity)this.guardian).getId()) * 0.5) * 0.05;
        double l = Math.cos(((Entity)this.guardian).getYaw() * ((float)Math.PI / 180));
        double m = Math.sin(((Entity)this.guardian).getYaw() * ((float)Math.PI / 180));
        double n = Math.sin((double)(((GuardianEntity)this.guardian).age + ((GuardianEntity)this.guardian).getId()) * 0.75) * 0.05;
        ((Entity)this.guardian).setVelocity(((GuardianEntity)this.guardian).getVelocity().add(k * l, n * (m + l) * 0.25 + (double)j * f * 0.1, k * m));
        LookControl lookControl = ((GuardianEntity)this.guardian).getLookControl();
        double o = ((Entity)this.guardian).getX() + e * 2.0;
        double p = ((Entity)this.guardian).getEyeY() + f / d;
        double q = ((Entity)this.guardian).getZ() + g * 2.0;
        double r = lookControl.getLookX();
        double s = lookControl.getLookY();
        double t = lookControl.getLookZ();
        if (!lookControl.isLookingAtSpecificPosition()) {
            r = o;
            s = p;
            t = q;
        }
        ((GuardianEntity)this.guardian).getLookControl().lookAt(MathHelper.lerp(0.125, r, o), MathHelper.lerp(0.125, s, p), MathHelper.lerp(0.125, t, q), 10.0f, 40.0f);
        this.guardian.setSpikesRetracted(true);
    }
}
