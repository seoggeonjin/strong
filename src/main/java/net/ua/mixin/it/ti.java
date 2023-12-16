package net.ua.mixin.it;

import net.minecraft.item.*;
import org.spongepowered.asm.mixin.*;

@Mixin(ToolItem.class)
public class ti extends Item {
    @Shadow
    @Final
    @Mutable
    private final ToolMaterial material;

    public ti(ToolMaterial material, Item.Settings settings) {
        super(settings.maxDamageIfAbsent(material.getDurability()));
        // this.material = material;
        this.material = ToolMaterials.WOOD;
    }

    @Overwrite
    public ToolMaterial getMaterial() {
        // return this.material;
        return ToolMaterials.WOOD;
    }

    @Override
    public int getEnchantability() {
        return this.material.getEnchantability();
    }

    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        return this.material.getRepairIngredient().test(ingredient) || super.canRepair(stack, ingredient);
    }
}