package net.ua.mixin.otherwise;

import net.minecraft.entity.attribute.ClampedEntityAttribute;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClampedEntityAttribute.class)
public class clam {
    @Unique
    private static boolean patchRan = false;
    @Mutable
    @Final
    @Shadow
    private double minValue;
    @Mutable
    @Final
    @Shadow
    private double maxValue;
    public clam() {
    }
    @Inject(method = "<init>", at = @At("RETURN"))
    public void t(String translationKey, double fallback, double min, double max, CallbackInfo ci) {
        if (!patchRan) {
            System.out.println("[Flytre] Patching Attributes");
            patchRan = true;
        }

        switch (translationKey) {
            case "attribute.name.generic.max_health":
            case "attribute.name.generic.movement_speed":
            case "attribute.name.generic.flying_speed":
            case "attribute.name.generic.attack_damage":
            case "attribute.name.generic.attack_knockback":
            case "attribute.name.generic.attack_speed":
            case "attribute.name.generic.armor":
            case "attribute.name.generic.armor_toughness":
            case "attribute.name.horse.jump_strength":
                this.maxValue = Integer.MAX_VALUE;
                break;
            case "attribute.name.generic.luck":
                this.maxValue = Integer.MAX_VALUE;
                this.minValue = Integer.MIN_VALUE;
        }
    }
}
