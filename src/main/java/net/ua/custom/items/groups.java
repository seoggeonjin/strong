package net.ua.custom.items;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static net.ua.Strong.MOD_ID;
import static net.ua.custom.custom.*;

public class groups {
    public static final ItemGroup group = Registry.register(Registries.ITEM_GROUP,
            new Identifier(MOD_ID, MOD_ID),
            FabricItemGroup.builder().displayName(Text.translatable("itemGroup.op"))
                    .icon(() -> new ItemStack(fixer)).entries((displayContext, entries) -> {
                        entries.add(fixer);
                        // entries.add(observer2);
                    }).build());
    public static void registerGroup() {

    }
}