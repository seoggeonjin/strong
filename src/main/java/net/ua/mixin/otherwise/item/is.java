package net.ua.mixin.otherwise.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.ua.ips.isi;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class is implements isi {
    @Final
    @Shadow
    @Nullable
    private Item item;
    @Shadow
    private int count;
    @Unique
    private int i = count;

    @Unique
    public Item getItem() {
        return this.isEmpty() ? Items.AIR : this.item;
    }
    /**
     * @author Seoggeonjin
     * @reason test
     */
    @Overwrite
    public int getMaxCount() {
        return 999;
    }
    /**
     * @author Seoggeonjin
     * @reason test
     */
    @Overwrite
    public void setCount(int count) {
        this.count = count;
        i = this.count;
    }
    @Unique
    public boolean isEmpty() {
        return this.item == Items.AIR || this.count <= 0;
    }
    @Inject(method = "getCount", at = @At("RETURN"), cancellable = true)
    public void getCount(CallbackInfoReturnable<Integer> cir) {
        if (this.count < 0) {
            this.count = 0;
        }
        cir.setReturnValue(this.isEmpty() ? 0 : this.count);
    }
    @Unique
    public int getI() {
        return i;
    }
}