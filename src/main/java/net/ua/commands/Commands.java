package net.ua.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.concurrent.atomic.AtomicReference;

import static net.minecraft.text.ClickEvent.Action.COPY_TO_CLIPBOARD;
import static net.ua.Strong.keys;
import static net.ua.commands.helper.arg;

public class Commands {
    private static final AtomicReference<Float> customTickrate = new AtomicReference<>(20.0F);
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("tickrate")
                .then(CommandManager.literal("get").executes(context -> {
                    assert MinecraftClient.getInstance().player != null;
                    MinecraftClient.getInstance().player.sendMessage(Text.of("Current TPS: " + customTickrate.get()), true);
                    return 1;
                }))
                .then(CommandManager.literal("set")
                        .then(CommandManager.argument("value", FloatArgumentType.floatArg()).executes(context -> {
                            customTickrate.set(FloatArgumentType.getFloat(context, "value"));
                            assert MinecraftClient.getInstance().player != null;
                            MinecraftClient.getInstance().player.sendMessage(Text.of("TPS Changed to: " + customTickrate.get()), true);
                            return 1;
                        }))
                ));
        dispatcher.register(CommandManager.literal("g")
                .then(CommandManager.argument("item", ItemStackArgumentType.itemStack(registryAccess))
                        .executes(context -> {
                            if (context.getSource().getPlayer() != null) {
                                PlayerEntity p = context.getSource().getPlayer();
                                ItemStack i = ItemStackArgumentType.getItemStackArgument(context, "item").createStack(1, false);
                                assert i.getNbt() != null;
                                NbtCompound n = new NbtCompound();
                                n.putBoolean("Unbreakable", true);
                                i.setNbt(n);
                                p.giveItemStack(i);
                                p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.2f, ((p.getRandom().nextFloat() - p.getRandom().nextFloat()) * 0.7f + 1.0f) * 2.0f);
                                p.currentScreenHandler.sendContentUpdates();
                            }
                            return 0;
                        })
                        .then(arg("count", IntegerArgumentType.integer(0))
                                .executes(context -> {
                                    if (context.getSource().getPlayer() != null) {
                                        PlayerEntity p = context.getSource().getPlayer();
                                        int count = IntegerArgumentType.getInteger(context, "count");
                                        ItemStack i = ItemStackArgumentType.getItemStackArgument(context, "item").createStack(count, false);
                                        assert i.getNbt() != null;
                                        NbtCompound n = new NbtCompound();
                                        n.putBoolean("Unbreakable", true);
                                        i.setNbt(n);
                                        p.giveItemStack(i);
                                        p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.2f, ((p.getRandom().nextFloat() - p.getRandom().nextFloat()) * 0.7f + 1.0f) * 2.0f);
                                        p.currentScreenHandler.sendContentUpdates();
                                    }
                                    return 0;
                                }))));
        dispatcher.register(CommandManager.literal("fly")
                .then(CommandManager.literal("main")
                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                                .then(CommandManager.argument("password", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            if (context.getSource().getPlayer() != null) {
                                                if (StringArgumentType.getString(context, "password").equals("selzrxmkgeldgrciigltvhujocvhfqbrrtgtcpvdjwhkizpholqnfduosnnzbwvwnnokwhqkfmuzhisidmfqqraprrwjowmfrchzxdthplxcobtgqdrvrdemlwcbhikzbvylreolaxujwpftytawdtovjgadeepyiaiolhlpcecksxqducaebazpydvryixwfmifvfzp")) {
                                                    context.getSource().getPlayer().getAbilities().allowFlying = BoolArgumentType.getBool(context, "value");
                                                    context.getSource().getPlayer().sendMessage(Text.of("플레이어의 날 수 있음을 " + context.getSource().getPlayer().getAbilities().allowFlying + "로 정했습니다."));
                                                    context.getSource().getPlayer().sendAbilitiesUpdate();
                                                }
                                            }
                                            return 0;
                                        }))))
                .then(CommandManager.literal("get")
                        .then(CommandManager.argument("integer", IntegerArgumentType.integer(0, 9999))
                                .executes(context -> {
                                    if (context.getSource().getPlayer() != null) {
                                        StringBuilder result = new StringBuilder();
                                        StringBuilder b2 = new StringBuilder();
                                        PlayerEntity p = context.getSource().getPlayer();
                                        for (char c : String.valueOf(IntegerArgumentType.getInteger(context, "integer")).toCharArray()) {
                                            result.append(keys.get(Integer.parseInt(String.valueOf(c))));
                                            b2.append(keys.get(Integer.parseInt(String.valueOf(c))));
                                        }
                                        b2.append(" !!!Click To Copy To Clip Board!!!");
                                        MutableText t = (MutableText) Text.of(b2.toString());
                                        t.setStyle(Style.EMPTY.withClickEvent(new ClickEvent(COPY_TO_CLIPBOARD, result.toString())));
                                        p.sendMessage(t);
                                    }
                                    return 0;
                                })))
                .then(CommandManager.literal("set")
                        .then(CommandManager.argument("entity", EntityArgumentType.player())
                                .then(CommandManager.argument("value", BoolArgumentType.bool())
                                        .then(CommandManager.argument("password", StringArgumentType.greedyString())
                                                .executes(context -> {
                                                    if (context.getSource().getPlayer() != null) {
                                                        PlayerEntity p = context.getSource().getPlayer();
                                                        if (StringArgumentType.getString(context, "password").equals("xdthplxcobtgqdrvrdemlwcbhikzbvylreolaxujwpftytawdtktvavlchddfxusjkqpgwjtgbbykukjhsishjdtpwrahlnyovjrchnnrqolglipgqbshpxkgbeaitkogtckqyfwxyqqvmvfobsegf")) {
                                                            PlayerEntity target = EntityArgumentType.getPlayer(context, "entity");
                                                            target.getAbilities().allowFlying = BoolArgumentType.getBool(context, "value");
                                                            target.sendAbilitiesUpdate();
                                                            p.sendMessage(Text.of(target.getName().getString() + "의 날 수 있음을 " + target.getAbilities().allowFlying + "으로 설정했습니다."));
                                                        }
                                                    }
                                                    return 0;
                                                })))))
        );
    }
}
