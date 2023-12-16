package net.ua.mixin.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.TimeCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(TimeCommand.class)
public class tc {
    /**
     * @author Seoggeonjin
     * @reason delete original data command
     */
    @Overwrite
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {

    }
}