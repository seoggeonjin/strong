package net.ua.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.argument.ScoreHolderArgumentType;
import net.minecraft.command.argument.ScoreboardObjectiveArgumentType;
import net.minecraft.command.argument.TimeArgumentType;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

public class tm1 {
    private static final SimpleCommandExceptionType NOT_CORRECT_TIME = new SimpleCommandExceptionType(Text.translatable("command.time.scoreboard.not.time"));
    private static final Dynamic2CommandExceptionType PLAYERS_GET_NULL_EXCEPTION = new Dynamic2CommandExceptionType((objective, target) -> Text.stringifiedTranslatable("commands.scoreboard.players.get.null", objective, target));
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder) ((LiteralArgumentBuilder)CommandManager.literal("time").requires(source -> source.hasPermissionLevel(2)))
                .then(CommandManager.literal("set")
                        .then(CommandManager.literal("day").executes(context -> executeSet(context.getSource(), 1000)))
                        .then(CommandManager.literal("noon").executes(context -> executeSet(context.getSource(), 6000)))
                        .then(CommandManager.literal("night").executes(context -> executeSet(context.getSource(), 13000)))
                        .then(CommandManager.literal("midnight").executes(context -> executeSet(context.getSource(), 18000)))
                        .then(CommandManager.argument("time", TimeArgumentType.time())
                                .executes(context -> executeSet(context.getSource(), IntegerArgumentType.getInteger(context, "time"))))
                        .then(CommandManager.argument("time_object", ScoreHolderArgumentType.scoreHolder())
                                .then(CommandManager.argument("objective", ScoreboardObjectiveArgumentType.scoreboardObjective())
                                        .executes(context -> {
                                            ServerCommandSource source = context.getSource();
                                            ScoreHolder scoreHolder = ScoreHolderArgumentType.getScoreHolder(context, "time_object");
                                            ScoreboardObjective objective = ScoreboardObjectiveArgumentType.getObjective(context, "objective");
                                            ServerScoreboard scoreboard = source.getServer().getScoreboard();
                                            ReadableScoreboardScore readableScoreboardScore = scoreboard.getScore(scoreHolder, objective);
                                            if (readableScoreboardScore == null) {
                                                throw PLAYERS_GET_NULL_EXCEPTION.create(objective.getName(), scoreHolder.getStyledDisplayName());
                                            } else if (!(readableScoreboardScore.getScore() <= 24000 && readableScoreboardScore.getScore() >= 0)) {
                                                throw NOT_CORRECT_TIME.create();
                                            }
                                            executeSet(source, readableScoreboardScore.getScore());
                                            return readableScoreboardScore.getScore();
                                        })))
                                )
                .then(CommandManager.literal("add").then(CommandManager.argument("time", TimeArgumentType.time())
                        .executes(context -> executeAdd(context.getSource(), IntegerArgumentType.getInteger(context, "time")))))
                .then(((LiteralArgumentBuilder)CommandManager.literal("query").then(CommandManager.literal("daytime")
                        .executes(context -> executeQuery(context.getSource(), getDayTime(context.getSource().getWorld())))))
                        .then(CommandManager.literal("gametime")
                                .executes(context -> executeQuery(context.getSource(), (int)(context.getSource().getWorld().getTime() % Integer.MAX_VALUE))))
                        .then(CommandManager.literal("day").executes(context -> executeQuery(context.getSource(), (int)(context.getSource().getWorld().getTimeOfDay() / 24000L % Integer.MAX_VALUE))))));
    }

    private static int getDayTime(ServerWorld world) {
        return (int)(world.getTimeOfDay() % 24000L);
    }

    private static int executeQuery(ServerCommandSource source, int time) {
        source.sendFeedback(() -> Text.translatable("commands.time.query", time), false);
        return time;
    }

    public static int executeSet(ServerCommandSource source, int time) {
        for (ServerWorld serverWorld : source.getServer().getWorlds()) {
            serverWorld.setTimeOfDay(time);
        }
        source.sendFeedback(() -> Text.translatable("commands.time.set", time), true);
        return getDayTime(source.getWorld());
    }

    public static int executeAdd(ServerCommandSource source, int time) {
        for (ServerWorld serverWorld : source.getServer().getWorlds()) {
            serverWorld.setTimeOfDay(serverWorld.getTimeOfDay() + (long)time);
        }
        int i = getDayTime(source.getWorld());
        source.sendFeedback(() -> Text.translatable("commands.time.set", i), true);
        return i;
    }
}