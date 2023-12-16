package net.ua.mixin2.inner.goals.evoker;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.event.GameEvent;
import net.ua.ips.evi;
import net.ua.ips.spellI2;
import net.ua.mixin2.inner.Spell;
import net.ua.mixin2.inner.goals.SpellcastingIllagerEntity.CastSpellGoal2;

public class SummonVexGoal extends CastSpellGoal2 {
    private final TargetPredicate closeVexPredicate = TargetPredicate.createNonAttackable().setBaseMaxDistance(16.0).ignoreVisibility().ignoreDistanceScalingFactor();
    private final evi t;
    public SummonVexGoal(evi t) {
        super(t);
        this.t = t;
    }

    @Override
    public boolean canStart() {
        if (!super.canStart()) {
            return false;
        }
        int i = t.getWorld().getTargets(VexEntity.class, this.closeVexPredicate, (LivingEntity) t, ((Entity)t).getBoundingBox().expand(16.0)).size();
        return t.random().nextInt(8) + 1 > i;
    }

    @Override
    public int getSpellTicks() {
        return 100;
    }

    @Override
    public int startTimeDelay() {
        return 340;
    }

    @Override
    public void castSpell() {
        ServerWorld serverWorld = (ServerWorld) t.getWorld();
        Team team = t.getScoreboardTeam();
        for (int i = 0; i < 3; ++i) {
            BlockPos blockPos = ((Entity)t).getBlockPos().add(-2 + t.random().nextInt(5), 1, -2 + t.random().nextInt(5));
            VexEntity vexEntity = EntityType.VEX.create(t.getWorld());
            if (vexEntity == null) continue;
            vexEntity.refreshPositionAndAngles(blockPos, 0.0f, 0.0f);
            vexEntity.initialize(serverWorld, t.getWorld().getLocalDifficulty(blockPos), SpawnReason.MOB_SUMMONED, null, null);
            vexEntity.setOwner((MobEntity) t);
            vexEntity.setBounds(blockPos);
            vexEntity.setLifeTicks(20 * (30 + t.random().nextInt(90)));
            if (team != null) {
                serverWorld.getScoreboard().addScoreHolderToTeam(vexEntity.getNameForScoreboard(), team);
            }
            serverWorld.spawnEntityAndPassengers(vexEntity);
            serverWorld.emitGameEvent(GameEvent.ENTITY_PLACE, blockPos, GameEvent.Emitter.of((Entity) t));
        }
    }

    @Override
    public SoundEvent getSoundPrepare() {
        return SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON;
    }

    @Override
    public spellI2 getSpell() {
        return Spell.SUMMON_VEX;
    }
}