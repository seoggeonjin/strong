package net.ua.mixin.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.DataCommand;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(DataCommand.class)
public class dc {
    /**
     * @author Seoggeonjin
     * @reason delete original data command
     */
    @Overwrite
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {

    }
}