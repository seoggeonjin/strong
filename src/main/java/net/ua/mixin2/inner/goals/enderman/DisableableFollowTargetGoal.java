package net.ua.mixin2.inner.goals.enderman;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.mob.HostileEntity;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class DisableableFollowTargetGoal<T extends LivingEntity> extends ActiveTargetGoal<T> {
    private boolean enabled = true;

    public DisableableFollowTargetGoal(HostileEntity actor, Class<T> targetEntityClass, int reciprocalChance, boolean checkVisibility, boolean checkCanNavigate, @Nullable Predicate<LivingEntity> targetPredicate) {
        super(actor, targetEntityClass, reciprocalChance, checkVisibility, checkCanNavigate, targetPredicate);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean canStart() {
        return this.enabled && super.canStart();
    }
}
