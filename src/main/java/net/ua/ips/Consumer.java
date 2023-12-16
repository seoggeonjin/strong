package net.ua.ips;

import net.minecraft.enchantment.Enchantment;

@FunctionalInterface
public interface Consumer {
    void accept(Enchantment var1, int var2);
}