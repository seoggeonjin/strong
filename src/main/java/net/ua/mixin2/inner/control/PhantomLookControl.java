package net.ua.mixin2.inner.control;

import net.minecraft.entity.ai.control.LookControl;
import net.minecraft.entity.mob.MobEntity;
import net.ua.ips.phi;

public class PhantomLookControl
        extends LookControl {
    private final phi ph;
    public PhantomLookControl(phi ph) {
        super((MobEntity) ph);
        this.ph = ph;
    }

    @Override
    public void tick() {
    }
}
