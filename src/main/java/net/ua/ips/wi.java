package net.ua.ips;

import net.minecraft.util.math.Box;

public interface wi extends mixins {
    int getInvulnerableTimer();
    void refreshPositionAndAngles(double x, double y, double z, float yaw, float pitch);
    void onSummoned();
    Box getBoundingBox2();
    void sdy(float d);
}
