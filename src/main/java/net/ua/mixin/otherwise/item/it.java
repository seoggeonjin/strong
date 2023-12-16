package net.ua.mixin.otherwise.item;

import net.minecraft.client.render.item.ItemRenderer;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.item.Item.class)
public abstract class it {
    public it(net.minecraft.item.Item.@NotNull Settings settings) {
        settings.maxDamage(0).maxCount(999);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void setVanillaMaxCount(net.minecraft.item.Item.Settings settings, CallbackInfo ci) {
        settings.maxDamage(0).maxCount(999);
    }

    @Redirect(method = "isEnchantable", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/Item;getMaxCount()I"))
    private int isVanillaEnchantable(net.minecraft.item.Item item) {
        return 999;
    }
    @Overwrite
    public final int getMaxCount() {
        return 999;
    }
    // @Overwrite
    // public final int getMaxDamage() {
    //     return 2147483647;
    // }
}