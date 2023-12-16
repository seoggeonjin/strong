package net.ua.ips;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.WanderAroundGoal;

public interface gui extends mixins {
    void setBeamTarget(int entityId);
    WanderAroundGoal wanderGoal();
    void setSpikesRetracted(boolean retracted);
    void setTarget(LivingEntity entity);
}
