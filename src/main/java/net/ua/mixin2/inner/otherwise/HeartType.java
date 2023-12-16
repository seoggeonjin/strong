package net.ua.mixin2.inner.otherwise;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

@Environment(value= EnvType.CLIENT)
public enum HeartType {
    CONTAINER(new Identifier("hud/heart/container"), new Identifier("hud/heart/container_blinking"), new Identifier("hud/heart/container"), new Identifier("hud/heart/container_blinking"), new Identifier("hud/heart/container_hardcore"), new Identifier("hud/heart/container_hardcore_blinking"), new Identifier("hud/heart/container_hardcore"), new Identifier("hud/heart/container_hardcore_blinking")),
    NORMAL(new Identifier("hud/heart/full"), new Identifier("hud/heart/full_blinking"), new Identifier("hud/heart/half"), new Identifier("hud/heart/half_blinking"), new Identifier("hud/heart/hardcore_full"), new Identifier("hud/heart/hardcore_full_blinking"), new Identifier("hud/heart/hardcore_half"), new Identifier("hud/heart/hardcore_half_blinking")),
    POISONED(new Identifier("hud/heart/poisoned_full"), new Identifier("hud/heart/poisoned_full_blinking"), new Identifier("hud/heart/poisoned_half"), new Identifier("hud/heart/poisoned_half_blinking"), new Identifier("hud/heart/poisoned_hardcore_full"), new Identifier("hud/heart/poisoned_hardcore_full_blinking"), new Identifier("hud/heart/poisoned_hardcore_half"), new Identifier("hud/heart/poisoned_hardcore_half_blinking")),
    WITHERED(new Identifier("hud/heart/withered_full"), new Identifier("hud/heart/withered_full_blinking"), new Identifier("hud/heart/withered_half"), new Identifier("hud/heart/withered_half_blinking"), new Identifier("hud/heart/withered_hardcore_full"), new Identifier("hud/heart/withered_hardcore_full_blinking"), new Identifier("hud/heart/withered_hardcore_half"), new Identifier("hud/heart/withered_hardcore_half_blinking")),
    ABSORBING(new Identifier("hud/heart/absorbing_full"), new Identifier("hud/heart/absorbing_full_blinking"), new Identifier("hud/heart/absorbing_half"), new Identifier("hud/heart/absorbing_half_blinking"), new Identifier("hud/heart/absorbing_hardcore_full"), new Identifier("hud/heart/absorbing_hardcore_full_blinking"), new Identifier("hud/heart/absorbing_hardcore_half"), new Identifier("hud/heart/absorbing_hardcore_half_blinking")),
    FROZEN(new Identifier("hud/heart/frozen_full"), new Identifier("hud/heart/frozen_full_blinking"), new Identifier("hud/heart/frozen_half"), new Identifier("hud/heart/frozen_half_blinking"), new Identifier("hud/heart/frozen_hardcore_full"), new Identifier("hud/heart/frozen_hardcore_full_blinking"), new Identifier("hud/heart/frozen_hardcore_half"), new Identifier("hud/heart/frozen_hardcore_half_blinking"));

    private final Identifier fullTexture;
    private final Identifier fullBlinkingTexture;
    private final Identifier halfTexture;
    private final Identifier halfBlinkingTexture;
    private final Identifier hardcoreFullTexture;
    private final Identifier hardcoreFullBlinkingTexture;
    private final Identifier hardcoreHalfTexture;
    private final Identifier hardcoreHalfBlinkingTexture;

    HeartType(Identifier fullTexture, Identifier fullBlinkingTexture, Identifier halfTexture, Identifier halfBlinkingTexture, Identifier hardcoreFullTexture, Identifier hardcoreFullBlinkingTexture, Identifier hardcoreHalfTexture, Identifier hardcoreHalfBlinkingTexture) {
        this.fullTexture = fullTexture;
        this.fullBlinkingTexture = fullBlinkingTexture;
        this.halfTexture = halfTexture;
        this.halfBlinkingTexture = halfBlinkingTexture;
        this.hardcoreFullTexture = hardcoreFullTexture;
        this.hardcoreFullBlinkingTexture = hardcoreFullBlinkingTexture;
        this.hardcoreHalfTexture = hardcoreHalfTexture;
        this.hardcoreHalfBlinkingTexture = hardcoreHalfBlinkingTexture;
    }

    public Identifier getTexture(boolean hardcore, boolean half, boolean blinking) {
        if (!hardcore) {
            if (half) {
                return blinking ? this.halfBlinkingTexture : this.halfTexture;
            }
            return blinking ? this.fullBlinkingTexture : this.fullTexture;
        }
        if (half) {
            return blinking ? this.hardcoreHalfBlinkingTexture : this.hardcoreHalfTexture;
        }
        return blinking ? this.hardcoreFullBlinkingTexture : this.hardcoreFullTexture;
    }

    public static HeartType fromPlayerState(PlayerEntity player) {
        return player.hasStatusEffect(StatusEffects.POISON) ? POISONED : (player.hasStatusEffect(StatusEffects.WITHER) ? WITHERED : (player.isFrozen() ? FROZEN : NORMAL));
    }
}
