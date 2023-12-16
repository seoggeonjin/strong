package net.ua.mixin2.inner.goals.evoker;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.mob.EvokerEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.DyeColor;
import net.minecraft.world.GameRules;
import net.ua.ips.evi;
import net.ua.ips.spellI2;
import net.ua.mixin2.inner.Spell;
import net.ua.mixin2.inner.goals.SpellcastingIllagerEntity.CastSpellGoal2;

import java.util.List;

public class WololoGoal extends CastSpellGoal2 {
    private final TargetPredicate convertibleSheepPredicate = TargetPredicate.createNonAttackable().setBaseMaxDistance(16.0).setPredicate(livingEntity -> ((SheepEntity)livingEntity).getColor() == DyeColor.BLUE);
    private final evi t;

    public WololoGoal(evi t) {
        super(t);
        this.t = t;
    }

    @Override
    public boolean canStart() {
        if (t.getTarget() != null) {
            return false;
        }
        if (t.isSpellcasting()) {
            return false;
        }
        if (t.age() < this.startTime) {
            return false;
        }
        if (!t.getWorld().getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
            return false;
        }
        List<SheepEntity> list = t.getWorld().getTargets(SheepEntity.class, this.convertibleSheepPredicate, (EvokerEntity)t, ((Entity)t).getBoundingBox().expand(16.0, 4.0, 16.0));
        if (list.isEmpty()) {
            return false;
        }
        t.setWololoTarget(list.get(t.random().nextInt(list.size())));
        return true;
    }

    @Override
    public boolean shouldContinue() {
        return t.getWololoTarget() != null && this.spellCooldown > 0;
    }

    @Override
    public void stop() {
        super.stop();
        t.setWololoTarget(null);
    }

    @Override
    public void castSpell() {
        SheepEntity sheepEntity = t.getWololoTarget();
        if (sheepEntity != null && sheepEntity.isAlive()) {
            sheepEntity.setColor(DyeColor.RED);
        }
    }

    @Override
    public int getInitialCooldown() {
        return 40;
    }

    @Override
    public int getSpellTicks() {
        return 60;
    }

    @Override
    public int startTimeDelay() {
        return 140;
    }

    @Override
    public SoundEvent getSoundPrepare() {
        return SoundEvents.ENTITY_EVOKER_PREPARE_WOLOLO;
    }

    @Override
    public spellI2 getSpell() {
        return Spell.WOLOLO;
    }
}