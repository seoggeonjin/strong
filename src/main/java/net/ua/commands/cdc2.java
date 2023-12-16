package net.ua.commands;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.BlockDataObject;
import net.minecraft.command.DataCommandObject;
import net.minecraft.command.EntityDataObject;
import net.minecraft.command.StorageDataObject;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.command.argument.NbtElementArgumentType;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.*;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.DataCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class cdc2 {
    private static final String t = "target";
    private static final SimpleCommandExceptionType MERGE_FAILED_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("commands.data.merge.failed"));
    private static final DynamicCommandExceptionType GET_INVALID_EXCEPTION = new DynamicCommandExceptionType(path -> Text.translatable("commands.data.get.invalid", path));
    private static final DynamicCommandExceptionType GET_UNKNOWN_EXCEPTION = new DynamicCommandExceptionType(path -> Text.translatable("commands.data.get.unknown", path));
    private static final SimpleCommandExceptionType GET_MULTIPLE_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("commands.data.get.multiple"));
    private static final DynamicCommandExceptionType MODIFY_EXPECTED_OBJECT_EXCEPTION = new DynamicCommandExceptionType(nbt -> Text.translatable("commands.data.modify.expected_object", nbt));
    private static final DynamicCommandExceptionType MODIFY_EXPECTED_VALUE_EXCEPTION = new DynamicCommandExceptionType(nbt -> Text.translatable("commands.data.modify.expected_value", nbt));
    public static final List<Function<String, DataCommand.ObjectType>> OBJECT_TYPE_FACTORIES = ImmutableList.of(EntityDataObject.TYPE_FACTORY, BlockDataObject.TYPE_FACTORY, StorageDataObject.TYPE_FACTORY);
    public static final List<DataCommand.ObjectType> TARGET_OBJECT_TYPES = OBJECT_TYPE_FACTORIES.stream().map(factory -> factory.apply("target")).collect(ImmutableList.toImmutableList());
    public static final List<DataCommand.ObjectType> SOURCE_OBJECT_TYPES = OBJECT_TYPE_FACTORIES.stream().map(factory -> factory.apply("source")).collect(ImmutableList.toImmutableList());

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> literalArgumentBuilder = CommandManager.literal("data").requires(source -> source.hasPermissionLevel(2));
        for (DataCommand.ObjectType objectType : TARGET_OBJECT_TYPES) {
            literalArgumentBuilder.then(objectType.addArgumentsToBuilder(CommandManager.literal("merge"), builder -> builder.then(CommandManager.argument("nbt", NbtCompoundArgumentType.nbtCompound()).executes(context -> executeMerge(context.getSource(), objectType, NbtCompoundArgumentType.getNbtCompound(context, "nbt"), context))))).then(objectType.addArgumentsToBuilder(CommandManager.literal("get"), builder -> builder.executes(context -> executeGet(context.getSource(), objectType.getObject(context))).then(((RequiredArgumentBuilder)CommandManager.argument("path", NbtPathArgumentType.nbtPath()).executes(context -> executeGet(context.getSource(), objectType.getObject(context), NbtPathArgumentType.getNbtPath(context, "path")))).then(CommandManager.argument("scale", DoubleArgumentType.doubleArg()).executes(context -> executeGet(context.getSource(), objectType.getObject(context), NbtPathArgumentType.getNbtPath(context, "path"), DoubleArgumentType.getDouble(context, "scale"))))))).then(objectType.addArgumentsToBuilder(CommandManager.literal("remove"), builder -> builder.then(CommandManager.argument("path", NbtPathArgumentType.nbtPath()).executes(context -> executeRemove(context.getSource(), objectType, NbtPathArgumentType.getNbtPath(context, "path"), context))))).then(addModifyArgument((builder, modifier) -> ((ArgumentBuilder)builder.then(CommandManager.literal("insert").then(CommandManager.argument("index", IntegerArgumentType.integer()).then(modifier.create((context, sourceNbt, path, elements) -> path.insert(IntegerArgumentType.getInteger(context, "index"), sourceNbt, elements)))))).then(CommandManager.literal("prepend").then(modifier.create((context, nbtCompound, path, elements) -> path.insert(0, nbtCompound, elements)))).then(CommandManager.literal("append").then(modifier.create((context, nbtCompound, path, elements) -> path.insert(-1, nbtCompound, elements)))).then(CommandManager.literal("set").then(modifier.create((context, sourceNbt, path, elements) -> path.put(sourceNbt, Iterables.getLast(elements))))).then(CommandManager.literal("merge").then(modifier.create((context, element, path, elements) -> {
                NbtCompound nbtCompound = new NbtCompound();
                for (NbtElement nbtElement : elements) {
                    if (NbtPathArgumentType.NbtPath.isTooDeep(nbtElement, 0)) {
                        throw NbtPathArgumentType.TOO_DEEP_EXCEPTION.create();
                    }
                    if (nbtElement instanceof NbtCompound nbtCompound2) {
                        nbtCompound.copyFrom(nbtCompound2);
                        continue;
                    }
                    throw MODIFY_EXPECTED_OBJECT_EXCEPTION.create(nbtElement);
                }
                List<NbtElement> collection = path.getOrInit(element, NbtCompound::new);
                int i = 0;
                for (NbtElement nbtElement2 : collection) {
                    if (!(nbtElement2 instanceof NbtCompound nbtCompound3)) {
                        throw MODIFY_EXPECTED_OBJECT_EXCEPTION.create(nbtElement2);
                    }
                    NbtCompound nbtCompound4 = nbtCompound3.copy();
                    nbtCompound3.copyFrom(nbtCompound);
                    i += nbtCompound4.equals(nbtCompound3) ? 0 : 1;
                }
                return i;
            })))));
        }
        dispatcher.register(literalArgumentBuilder);
    }

    private static String asString(NbtElement nbt) throws CommandSyntaxException {
        if (nbt.getNbtType().isImmutable()) {
            return nbt.asString();
        }
        throw MODIFY_EXPECTED_VALUE_EXCEPTION.create(nbt);
    }

    private static List<NbtElement> mapValues(List<NbtElement> list, Function<String, String> function) throws CommandSyntaxException {
        ArrayList<NbtElement> list2 = new ArrayList<>(list.size());
        for (NbtElement nbtElement : list) {
            String string = asString(nbtElement);
            list2.add(NbtString.of(function.apply(string)));
        }
        return list2;
    }

    private static ArgumentBuilder<ServerCommandSource, ?> addModifyArgument(BiConsumer<ArgumentBuilder<ServerCommandSource, ?>, ModifyArgumentCreator> subArgumentAdder) {
        LiteralArgumentBuilder<ServerCommandSource> literalArgumentBuilder = CommandManager.literal("modify");
        for (DataCommand.ObjectType objectType : TARGET_OBJECT_TYPES) {
            objectType.addArgumentsToBuilder(literalArgumentBuilder, builder -> {
                RequiredArgumentBuilder<ServerCommandSource, NbtPathArgumentType.NbtPath> argumentBuilder = CommandManager.argument("targetPath", NbtPathArgumentType.nbtPath());
                for (DataCommand.ObjectType objectType2 : SOURCE_OBJECT_TYPES) {
                    subArgumentAdder.accept(argumentBuilder, operation -> objectType2.addArgumentsToBuilder(CommandManager.literal("from"), builder2 -> ((ArgumentBuilder)builder2.executes(context -> executeModify(context, objectType, operation, getValues(context, objectType2)))).then(CommandManager.argument("sourcePath", NbtPathArgumentType.nbtPath()).executes(context -> executeModify(context, objectType, operation, getValuesByPath(context, objectType2))))));
                    subArgumentAdder.accept(argumentBuilder, operation -> objectType2.addArgumentsToBuilder(CommandManager.literal("string"), builder2 -> builder2.executes(context -> executeModify(context, objectType, operation, mapValues(getValues(context, objectType2), value -> value))).then(((RequiredArgumentBuilder)CommandManager.argument("sourcePath", NbtPathArgumentType.nbtPath()).executes(context -> executeModify(context, objectType, operation, mapValues(getValuesByPath(context, objectType2), value -> value)))).then(((RequiredArgumentBuilder)CommandManager.argument("start", IntegerArgumentType.integer(0)).executes(context -> executeModify(context, objectType, operation, mapValues(getValuesByPath(context, objectType2), value -> value.substring(IntegerArgumentType.getInteger(context, "start")))))).then(CommandManager.argument("end", IntegerArgumentType.integer(0)).executes(context -> executeModify(context, objectType, operation, mapValues(getValuesByPath(context, objectType2), value -> value.substring(IntegerArgumentType.getInteger(context, "start"), IntegerArgumentType.getInteger(context, "end"))))))))));
                }
                subArgumentAdder.accept(argumentBuilder, modifier -> CommandManager.literal("value").then(CommandManager.argument("value", NbtElementArgumentType.nbtElement()).executes(context -> {
                    List<NbtElement> list = Collections.singletonList(NbtElementArgumentType.getNbtElement(context, "value"));
                    return executeModify(context, objectType, modifier, list);
                })));
                return builder.then(argumentBuilder);
            });
        }
        return literalArgumentBuilder;
    }

    private static List<NbtElement> getValues(CommandContext<ServerCommandSource> context, DataCommand.ObjectType objectType) throws CommandSyntaxException {
        DataCommandObject dataCommandObject = objectType.getObject(context);
        return Collections.singletonList(dataCommandObject.getNbt());
    }

    private static List<NbtElement> getValuesByPath(CommandContext<ServerCommandSource> context, DataCommand.ObjectType objectType) throws CommandSyntaxException {
        DataCommandObject dataCommandObject = objectType.getObject(context);
        NbtPathArgumentType.NbtPath nbtPath = NbtPathArgumentType.getNbtPath(context, "sourcePath");
        return nbtPath.get(dataCommandObject.getNbt());
    }
    private static int executeModify(CommandContext<ServerCommandSource> context, DataCommand.ObjectType objectType, ModifyOperation modifier, List<NbtElement> elements) throws CommandSyntaxException {
        DataCommandObject dataCommandObject = objectType.getObject(context);
        NbtPathArgumentType.NbtPath nbtPath = NbtPathArgumentType.getNbtPath(context, "targetPath");
        NbtCompound nbtCompound = dataCommandObject.getNbt();
        int i = modifier.modify(context, nbtCompound, nbtPath, elements);
        if (i == 0) {
            throw MERGE_FAILED_EXCEPTION.create();
        }
        if (dataCommandObject instanceof EntityDataObject) {
            Entity e = EntityArgumentType.getEntity(context, cdc2.t);
            UUID uUID = e.getUuid();
            e.readNbt(nbtCompound);
            e.setUuid(uUID);
        } else {
            dataCommandObject.setNbt(nbtCompound);
        }
        context.getSource().sendMessage(dataCommandObject.feedbackModify());
        return i;
    }
    private static int executeRemove(ServerCommandSource source, DataCommand.ObjectType object, NbtPathArgumentType.NbtPath path, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        NbtCompound nbtCompound = object.getObject(context).getNbt();
        int i = path.remove(nbtCompound);
        if (i == 0) {
            throw MERGE_FAILED_EXCEPTION.create();
        }
        if (object.getObject(context) instanceof EntityDataObject) {
            Entity e = EntityArgumentType.getEntity(context, cdc2.t);
            UUID uUID = e.getUuid();
            e.readNbt(nbtCompound);
            e.setUuid(uUID);
        } else {
            object.getObject(context).setNbt(nbtCompound);
        }
        source.sendMessage(object.getObject(context).feedbackModify());
        return i;
    }

    private static NbtElement getNbt(NbtPathArgumentType.NbtPath path, DataCommandObject object) throws CommandSyntaxException {
        List<NbtElement> collection = path.get(object.getNbt());
        Iterator<NbtElement> iterator = collection.iterator();
        NbtElement nbtElement = iterator.next();
        if (iterator.hasNext()) {
            throw GET_MULTIPLE_EXCEPTION.create();
        }
        return nbtElement;
    }

    private static int executeGet(ServerCommandSource source, DataCommandObject object, NbtPathArgumentType.NbtPath path) throws CommandSyntaxException {
        int i;
        NbtElement nbtElement = getNbt(path, object);
        if (nbtElement instanceof AbstractNbtNumber) {
            i = MathHelper.floor(((AbstractNbtNumber)nbtElement).doubleValue());
        } else if (nbtElement instanceof AbstractNbtList) {
            i = ((AbstractNbtList<?>)nbtElement).size();
        } else if (nbtElement instanceof NbtCompound) {
            i = ((NbtCompound)nbtElement).getSize();
        } else if (nbtElement instanceof NbtString) {
            i = nbtElement.asString().length();
        } else {
            throw GET_UNKNOWN_EXCEPTION.create(path.toString());
        }
        source.sendMessage(object.feedbackQuery(nbtElement));
        return i;
    }

    private static int executeGet(ServerCommandSource source, DataCommandObject object, NbtPathArgumentType.NbtPath path, double scale) throws CommandSyntaxException {
        NbtElement nbtElement = getNbt(path, object);
        if (!(nbtElement instanceof AbstractNbtNumber)) {
            throw GET_INVALID_EXCEPTION.create(path.toString());
        }
        int i = MathHelper.floor(((AbstractNbtNumber)nbtElement).doubleValue() * scale);
        source.sendMessage(object.feedbackGet(path, scale, i));
        return i;
    }

    private static int executeGet(ServerCommandSource source, DataCommandObject object) throws CommandSyntaxException {
        source.sendMessage(object.feedbackQuery(object.getNbt()));
        return 1;
    }

    private static int executeMerge(ServerCommandSource source, DataCommand.ObjectType object, NbtCompound nbt, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        NbtCompound nbtCompound = object.getObject(context).getNbt();
        if (NbtPathArgumentType.NbtPath.isTooDeep(nbt, 0)) {
            throw NbtPathArgumentType.TOO_DEEP_EXCEPTION.create();
        }
        NbtCompound nbtCompound2 = nbtCompound.copy().copyFrom(nbt);
        if (nbtCompound.equals(nbtCompound2)) {
            throw MERGE_FAILED_EXCEPTION.create();
        }
        if (object.getObject(context) instanceof EntityDataObject) {
            Entity e = EntityArgumentType.getEntity(context, cdc2.t);
            UUID uUID = e.getUuid();
            e.readNbt(nbtCompound2);
            e.setUuid(uUID);
        } else {
            object.getObject(context).setNbt(nbtCompound2);
        }
        source.sendMessage(object.getObject(context).feedbackModify());
        return 1;
    }

    interface ModifyOperation {
        int modify(CommandContext<ServerCommandSource> var1, NbtCompound var2, NbtPathArgumentType.NbtPath var3, List<NbtElement> var4) throws CommandSyntaxException;
    }

    interface ModifyArgumentCreator {
        ArgumentBuilder<ServerCommandSource, ?> create(ModifyOperation var1);
    }
}
