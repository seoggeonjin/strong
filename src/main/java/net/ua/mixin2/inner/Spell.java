package net.ua.mixin2.inner;

import net.minecraft.util.function.ValueLists;
import net.ua.ips.spellI2;

import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

public enum Spell implements spellI2 {
    NONE(0, 0.0, 0.0, 0.0),
    SUMMON_VEX(1, 0.7, 0.7, 0.8),
    FANGS(2, 0.4, 0.3, 0.35),
    WOLOLO(3, 0.7, 0.5, 0.2),
    DISAPPEAR(4, 0.3, 0.3, 0.8),
    BLINDNESS(5, 0.1, 0.1, 0.2);

    private static final IntFunction<Spell> BY_ID;
    final int id;
    final double[] particleVelocity;

    Spell(int id, double particleVelocityX, double particleVelocityY, double particleVelocityZ) {
        this.id = id;
        this.particleVelocity = new double[]{particleVelocityX, particleVelocityY, particleVelocityZ};
    }
    @Override
    public double particleVelocity(int index) {
        return particleVelocity[index];
    }

    public int id() {
        return id;
    }
    public static Spell byId(int id) {
        return BY_ID.apply(id);
    }


    static {
        BY_ID = ValueLists.createIdToValueFunction((ToIntFunction<Spell>) spell -> spell.id, Spell.values(), ValueLists.OutOfBoundsHandling.ZERO);
    }
}