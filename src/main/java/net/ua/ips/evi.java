package net.ua.ips;

import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public interface evi extends spellI {
    int age();
    World getWorld();
    void setWololoTarget(@Nullable SheepEntity wololoTarget);
    Random random();
    SheepEntity getWololoTarget();
    double squaredDistanceTo(Entity entity);
    Team getScoreboardTeam();
}
