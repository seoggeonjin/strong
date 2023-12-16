package net.ua.custom;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.ua.custom.items.fix;

import static net.ua.Strong.MOD_ID;

public class custom {
    public static final Item fixer = register("fixer", new fix());
    // public static final Block observer2 = register("observer2", new Observer2());
    private static Item register(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(MOD_ID, name), item);
    }
    private static void TabGroup(FabricItemGroupEntries entries, Item item) {
        entries.add(item);
    }
    private static void fixer(FabricItemGroupEntries entries) {
        TabGroup(entries, fixer);
    }
    public static void register() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(custom::fixer);
    }
    private static Item registerBlockItem(String name, Block block) {
        return Registry.register(Registries.ITEM, new Identifier(MOD_ID, name), new BlockItem(block, new FabricItemSettings()));
    }
    private static Block register(String name, Block block) {
        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, new Identifier(MOD_ID, name), block);
    }
}
