package net.ua.custom.items;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public class fix extends Item {
    public static final int fixAmount = 50;
    public fix() {
        super(new FabricItemSettings());
    }
    @Override
    public TypedActionResult<ItemStack> use(World world, @NotNull PlayerEntity p, Hand hand) {
        PlayerInventory pi = p.getInventory();
        ItemStack is;
        if (hand == Hand.MAIN_HAND) {
            is = pi.getMainHandStack();
        } else {
            is = pi.offHand.get(0);
        }
        is.setCount(is.getCount() - 1);
        p.setCurrentHand(hand);
        for (ItemStack i: pi.main) {
            i.setDamage(i.getDamage() < fixAmount ? 0 : i.getDamage() - fixAmount);
        }
        for (ItemStack i: pi.armor) {
            i.setDamage(i.getDamage() < fixAmount ? 0 : i.getDamage() - fixAmount);
        }
        for (ItemStack i: pi.offHand) {
            i.setDamage(i.getDamage() < fixAmount ? 0 : i.getDamage() - fixAmount);
        }
        return super.use(world, p, hand);
    }
}