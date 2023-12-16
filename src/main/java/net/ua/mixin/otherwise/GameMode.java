package net.ua.mixin.otherwise;

import net.minecraft.entity.player.PlayerAbilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import static net.minecraft.world.GameMode.*;

@Mixin(net.minecraft.world.GameMode.class)
public class GameMode {
    @Overwrite
    public void setAbilities(PlayerAbilities abilities) {
        if ((Object) this == CREATIVE) {
            abilities.allowFlying = true;
            abilities.creativeMode = true;
            abilities.invulnerable = true;
        } else if ((Object) this == SPECTATOR) {
            abilities.allowFlying = true;
            abilities.creativeMode = false;
            abilities.invulnerable = true;
            abilities.flying = true;
        } else {
            abilities.allowFlying = false;
            abilities.creativeMode = false;
            abilities.invulnerable = false;
            abilities.flying = false;
        }
        abilities.allowModifyWorld = !((net.minecraft.world.GameMode) (Object) this).isBlockBreakingRestricted();
    }
}
