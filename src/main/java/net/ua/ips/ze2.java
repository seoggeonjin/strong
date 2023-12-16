package net.ua.ips;

import net.minecraft.client.render.entity.feature.SkinOverlayOwner;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.mob.MobVisibilityCache;

public interface ze2 extends SkinOverlayOwner, mixins {
    int getFuseSpeed();
    EntityNavigation getNavigation();
    void setFuseSpeed(int fuseSpeed);
    MobVisibilityCache getVisibilityCache();
    double squaredDistanceTo(Entity entity);
}
