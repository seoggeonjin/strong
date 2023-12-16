package net.ua.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.entity.attribute.ClampedEntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.Objects;

import static net.ua.commands.helper.lit;

public class t1 {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(lit("t1").executes(command -> {
            PlayerEntity p = command.getSource().getPlayer();
            if (p == null) return 1;
            p.sendMessage(Text.of(String.valueOf(((ClampedEntityAttribute) Registries.ATTRIBUTE.get(0)).getMaxValue())));
            p.sendMessage(Text.of(String.valueOf(p.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH))));
            p.setHealth(2147483647);
            return 0;
        }));
    }
    public static void register2(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(lit("setmaxhealth").then(CommandManager.argument("health", FloatArgumentType.floatArg(0, 2147483647)).executes(command -> {
            PlayerEntity p = command.getSource().getPlayer();
            if (p == null) return 1;
            Objects.requireNonNull(p.getAttributes().getCustomInstance(RegistryEntry.of(EntityAttributes.GENERIC_MAX_HEALTH))).setBaseValue(FloatArgumentType.getFloat(command, "health"));
            p.sendMessage(Text.of("당신의 최대 체력을 " + FloatArgumentType.getFloat(command, "health") + "으로 설정했습니다."));
            return 0;
        })));
    }
}
