package net.ua.commands;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class helper {
    public static LiteralArgumentBuilder<ServerCommandSource> lit(String arg) {
        return CommandManager.literal(arg);
    }
    public static <T> RequiredArgumentBuilder<ServerCommandSource, T> arg(String arg, ArgumentType<T> type) {
        return CommandManager.argument(arg, type);
    }
}
