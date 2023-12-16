package net.ua.mixin2.inner.control;

import net.minecraft.entity.ai.control.BodyControl;
import net.minecraft.entity.mob.MobEntity;
import net.ua.ips.phi;

public class PhantomBodyControl
        extends BodyControl {
    private final phi ph;
    public PhantomBodyControl(phi ph) {
        super((MobEntity) ph);
        this.ph = ph;
    }

    @Override
    public void tick() {
        ph.headYaw(ph.bodyYaw());
        ph.bodyYaw(ph.getYaw());
    }
}