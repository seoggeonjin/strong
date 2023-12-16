package net.ua.mixin.otherwise.item;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.enchantment.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.collection.Weighting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.ua.ips.Consumer;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.lang3.mutable.MutableInt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.function.Predicate;

@Mixin(EnchantmentHelper.class)
public class em {
    @Unique
    private static final String ID_KEY = "id";
    @Unique
    private static final String LEVEL_KEY = "lvl";

    @Inject(method = "createNbt", at = @At("RETURN"), cancellable = true)
    private static void createNbt(Identifier id, int lvl, CallbackInfoReturnable<NbtCompound> cir) {
        NbtCompound nbtCompound = new NbtCompound();
        nbtCompound.putString(ID_KEY, String.valueOf(id));
        nbtCompound.putShort(LEVEL_KEY, (short)lvl);
        cir.setReturnValue(nbtCompound);
    }
    @Unique
    private static NbtCompound createNbt(Identifier id, int lvl) {
        NbtCompound nbtCompound = new NbtCompound();
        nbtCompound.putString(ID_KEY, String.valueOf(id));
        nbtCompound.putShort(LEVEL_KEY, (short)lvl);
        return nbtCompound;
    }

    @Inject(method = "writeLevelToNbt", at = @At("HEAD"))
    private static void writeLevelToNbt(NbtCompound nbt, int lvl, CallbackInfo ci) {
        nbt.putShort(LEVEL_KEY, (short)lvl);
    }

    @Inject(method = "getLevelFromNbt", at = @At("RETURN"), cancellable = true)
    private static void getLevelFromNbt(NbtCompound nbt, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(MathHelper.clamp(nbt.getInt(LEVEL_KEY), 0, 2147483647));
    }
    @Unique
    private static int getLevelFromNbt(NbtCompound nbt) {
        return MathHelper.clamp(nbt.getInt(LEVEL_KEY), 0, 2147483647);
    }

    @Inject(method = "getIdFromNbt", at = @At("RETURN"), cancellable = true)
    private static void getIdFromNbt(NbtCompound nbt, CallbackInfoReturnable<Identifier> cir) {
        cir.setReturnValue(Identifier.tryParse(nbt.getString(ID_KEY)));
    }
    @Unique
    private static Identifier getIdFromNbt(NbtCompound nbt) {
        return Identifier.tryParse(nbt.getString(ID_KEY));
    }

    @Inject(method = "getEnchantmentId", at = @At("RETURN"), cancellable = true)
    private static void getEnchantmentId(Enchantment enchantment, CallbackInfoReturnable<Identifier> cir) {
        cir.setReturnValue(Registries.ENCHANTMENT.getId(enchantment));
    }
    @Unique
    private static Identifier getEnchantmentId(Enchantment enchantment) {
        return Registries.ENCHANTMENT.getId(enchantment);
    }

    @Inject(method = "getLevel", at = @At("RETURN"), cancellable = true)
    private static void getLevel(Enchantment enchantment, ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (stack.isEmpty()) {
            cir.setReturnValue(0);
        }
        Identifier identifier = getEnchantmentId(enchantment);
        NbtList nbtList = stack.getEnchantments();
        for (int i = 0; i < nbtList.size(); ++i) {
            NbtCompound nbtCompound = nbtList.getCompound(i);
            Identifier identifier2 = getIdFromNbt(nbtCompound);
            if (identifier2 == null || !identifier2.equals(identifier)) continue;
            cir.setReturnValue(getLevelFromNbt(nbtCompound));
        }
        cir.setReturnValue(0);
    }
    @Unique
    private static int getLevel(Enchantment enchantment, ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        Identifier identifier = getEnchantmentId(enchantment);
        NbtList nbtList = stack.getEnchantments();
        for (int i = 0; i < nbtList.size(); ++i) {
            NbtCompound nbtCompound = nbtList.getCompound(i);
            Identifier identifier2 = getIdFromNbt(nbtCompound);
            if (identifier2 == null || !identifier2.equals(identifier)) continue;
            return getLevelFromNbt(nbtCompound);
        }
        return 0;
    }
    @Inject(method = "get", at = @At("RETURN"), cancellable = true)
    private static void get(ItemStack stack, CallbackInfoReturnable<Map<Enchantment, Integer>> cir) {
        NbtList nbtList = stack.isOf(Items.ENCHANTED_BOOK) ? EnchantedBookItem.getEnchantmentNbt(stack) : stack.getEnchantments();
        cir.setReturnValue(fromNbt(nbtList));
    }
    @Inject(method = "fromNbt", at = @At("RETURN"), cancellable = true)
    private static void fromNbt(NbtList list, CallbackInfoReturnable<Map<Enchantment, Integer>> cir) {
        LinkedHashMap<Enchantment, Integer> map = Maps.newLinkedHashMap();
        for (int i = 0; i < list.size(); ++i) {
            NbtCompound nbtCompound = list.getCompound(i);
            Registries.ENCHANTMENT.getOrEmpty(getIdFromNbt(nbtCompound)).ifPresent(enchantment -> map.put(enchantment, getLevelFromNbt(nbtCompound)));
        }
        cir.setReturnValue(map);
    }
    @Unique
    private static Map<Enchantment, Integer> fromNbt(NbtList list) {
        LinkedHashMap<Enchantment, Integer> map = Maps.newLinkedHashMap();
        for (int i = 0; i < list.size(); ++i) {
            NbtCompound nbtCompound = list.getCompound(i);
            Registries.ENCHANTMENT.getOrEmpty(getIdFromNbt(nbtCompound)).ifPresent(enchantment -> map.put(enchantment, getLevelFromNbt(nbtCompound)));
        }
        return map;
    }
    @Inject(method = "set", at = @At("HEAD"))
    private static void set(Map<Enchantment, Integer> enchantments, ItemStack stack, CallbackInfo ci) {
        NbtList nbtList = new NbtList();
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            Enchantment enchantment = entry.getKey();
            if (enchantment == null) continue;
            int i = entry.getValue();
            nbtList.add(createNbt(getEnchantmentId(enchantment), i));
            if (!stack.isOf(Items.ENCHANTED_BOOK)) continue;
            EnchantedBookItem.addEnchantment(stack, new EnchantmentLevelEntry(enchantment, i));
        }
        if (nbtList.isEmpty()) {
            stack.removeSubNbt("Enchantments");
        } else if (!stack.isOf(Items.ENCHANTED_BOOK)) {
            stack.setSubNbt("Enchantments", nbtList);
        }
    }

    @Unique
    private static void forEachEnchantment(Consumer consumer, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        NbtList nbtList = stack.getEnchantments();
        for (int i = 0; i < nbtList.size(); ++i) {
            NbtCompound nbtCompound = nbtList.getCompound(i);
            Registries.ENCHANTMENT.getOrEmpty(getIdFromNbt(nbtCompound)).ifPresent(enchantment -> consumer.accept(enchantment, getLevelFromNbt(nbtCompound)));
        }
    }

    @Unique
    private static void forEachEnchantment(Consumer consumer, Iterable<ItemStack> stacks) {
        for (ItemStack itemStack : stacks) {
            forEachEnchantment(consumer, itemStack);
        }
    }
    @Inject(method = "getProtectionAmount", at = @At("RETURN"), cancellable = true)
    private static void getProtectionAmount(Iterable<ItemStack> equipment, DamageSource source, CallbackInfoReturnable<Integer> cir) {
        MutableInt mutableInt = new MutableInt();
        forEachEnchantment((Enchantment enchantment, int level) -> mutableInt.add(enchantment.getProtectionAmount(level, source)), equipment);
        cir.setReturnValue(mutableInt.intValue());
    }

    @Inject(method = "getAttackDamage", at = @At("RETURN"), cancellable = true)
    private static void getAttackDamage(ItemStack stack, EntityGroup group, CallbackInfoReturnable<Float> cir) {
        MutableFloat mutableFloat = new MutableFloat();
        forEachEnchantment((Enchantment enchantment, int level) -> mutableFloat.add(enchantment.getAttackDamage(level, group)), stack);
        cir.setReturnValue(mutableFloat.floatValue());
    }

    @Inject(method = "getSweepingMultiplier", at = @At("RETURN"), cancellable = true)
    private static void getSweepingMultiplier(LivingEntity entity, CallbackInfoReturnable<Float> cir) {
        int i = getEquipmentLevel(Enchantments.SWEEPING, entity);
        cir.setReturnValue(i > 0? SweepingEnchantment.getMultiplier(i): 0.0F);
    }

    @Inject(method = "onUserDamaged", at = @At("HEAD"))
    private static void onUserDamaged(LivingEntity user, Entity attacker, CallbackInfo ci) {
        Consumer consumer = (enchantment, level) -> enchantment.onUserDamaged(user, attacker, level);
        if (user != null) {
            forEachEnchantment(consumer, user.getItemsEquipped());
        }
        if (attacker instanceof PlayerEntity) {
            assert user != null;
            forEachEnchantment(consumer, user.getMainHandStack());
        }
    }

    @Inject(method = "onTargetDamaged", at = @At("HEAD"))
    private static void onTargetDamaged(LivingEntity user, Entity target, CallbackInfo ci) {
        Consumer consumer = (enchantment, level) -> enchantment.onTargetDamaged(user, target, level);
        if (user != null) {
            forEachEnchantment(consumer, user.getItemsEquipped());
        }
        if (user instanceof PlayerEntity) {
            forEachEnchantment(consumer, user.getMainHandStack());
        }
    }
    @Inject(method = "getEquipmentLevel", at = @At("RETURN"), cancellable = true)
    private static void getEquipmentLevel(Enchantment enchantment, LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        Collection<ItemStack> iterable = enchantment.getEquipment(entity).values();
        int i = 0;
        for (ItemStack itemStack : iterable) {
            int j = getLevel(enchantment, itemStack);
            if (j <= i) continue;
            i = j;
        }
        cir.setReturnValue(i);
    }
    @Unique
    private static int getEquipmentLevel(Enchantment enchantment, LivingEntity entity) {
        Collection<ItemStack> iterable = enchantment.getEquipment(entity).values();
        int i = 0;
        for (ItemStack itemStack : iterable) {
            int j = getLevel(enchantment, itemStack);
            if (j <= i) continue;
            i = j;
        }
        return i;
    }

    @Inject(method = "getSwiftSneakSpeedBoost", at = @At("RETURN"), cancellable = true)
    private static void getSwiftSneakSpeedBoost(LivingEntity entity, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue((float) ((float) getEquipmentLevel(Enchantments.SWIFT_SNEAK, entity) * 3.402823466E38));
    }

    @Inject(method = "getKnockback", at = @At("RETURN"), cancellable = true)
    private static void getKnockback(LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(getEquipmentLevel(Enchantments.KNOCKBACK, entity));
    }

    @Inject(method = "getFireAspect", at = @At("RETURN"), cancellable = true)
    private static void getFireAspect(LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(getEquipmentLevel(Enchantments.FIRE_ASPECT, entity));
    }

    @Inject(method = "getRespiration", at = @At("RETURN"), cancellable = true)
    private static void getRespiration(LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(getEquipmentLevel(Enchantments.RESPIRATION, entity));
    }

    @Inject(method = "getDepthStrider", at = @At("RETURN"), cancellable = true)
    private static void getDepthStrider(LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(getEquipmentLevel(Enchantments.DEPTH_STRIDER, entity));
    }
    @Inject(method = "getEfficiency", at = @At("RETURN"), cancellable = true)
    private static void getEfficiency(LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(getEquipmentLevel(Enchantments.EFFICIENCY, entity) + 10);
    }

    @Inject(method = "getLuckOfTheSea", at = @At("RETURN"), cancellable = true)
    private static void getLuckOfTheSea(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(getLevel(Enchantments.LUCK_OF_THE_SEA, stack));
    }

    @Inject(method = "getLure", at = @At("RETURN"), cancellable = true)
    private static void getLure(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(getLevel(Enchantments.LURE, stack));
    }

    @Inject(method = "getLooting", at = @At("RETURN"), cancellable = true)
    private static void getLooting(LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(getEquipmentLevel(Enchantments.LOOTING, entity));
    }

    @Inject(method = "hasAquaAffinity", at = @At("RETURN"), cancellable = true)
    private static void hasAquaAffinity(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(getEquipmentLevel(Enchantments.AQUA_AFFINITY, entity) > 0);
    }

    @Inject(method = "hasFrostWalker", at = @At("RETURN"), cancellable = true)
    private static void hasFrostWalker(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(getEquipmentLevel(Enchantments.FROST_WALKER, entity) > 0);
    }

    @Inject(method = "hasSoulSpeed", at = @At("RETURN"), cancellable = true)
    private static void hasSoulSpeed(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(getEquipmentLevel(Enchantments.SOUL_SPEED, entity) > 0);
    }

    @Inject(method = "hasBindingCurse", at = @At("RETURN"), cancellable = true)
    private static void hasBindingCurse(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(getLevel(Enchantments.BINDING_CURSE, stack) > 0);
    }

    @Inject(method = "hasVanishingCurse", at = @At("RETURN"), cancellable = true)
    private static void hasVanishingCurse(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(getLevel(Enchantments.VANISHING_CURSE, stack) > 0);
    }

    @Inject(method = "hasSilkTouch", at = @At("RETURN"), cancellable = true)
    private static void hasSilkTouch(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(getLevel(Enchantments.SILK_TOUCH, stack) > 0);
    }

    @Inject(method = "getLoyalty", at = @At("RETURN"), cancellable = true)
    private static void getLoyalty(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(getLevel(Enchantments.LOYALTY, stack));
    }

    @Inject(method = "getRiptide", at = @At("RETURN"), cancellable = true)
    private static void getRiptide(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(getLevel(Enchantments.RIPTIDE, stack));
    }

    @Inject(method = "hasChanneling", at = @At("RETURN"), cancellable = true)
    private static void hasChanneling(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(getLevel(Enchantments.CHANNELING, stack) > 0);
    }
    @Inject(method = "chooseEquipmentWith(Lnet/minecraft/enchantment/Enchantment;Lnet/minecraft/entity/LivingEntity;)Ljava/util/Map$Entry;", at = @At("RETURN"), cancellable = true)
    private static void chooseEquipmentWith(Enchantment enchantment, LivingEntity entity, CallbackInfoReturnable<Map.Entry<EquipmentSlot, ItemStack>> cir) {
        cir.setReturnValue(chooseEquipmentWith(enchantment, entity, stack -> true));
    }
    @Inject(method = "chooseEquipmentWith(Lnet/minecraft/enchantment/Enchantment;Lnet/minecraft/entity/LivingEntity;Ljava/util/function/Predicate;)Ljava/util/Map$Entry;", at = @At("RETURN"), cancellable = true)
    private static void chooseEquipmentWith(Enchantment enchantment, LivingEntity entity, Predicate<ItemStack> condition, CallbackInfoReturnable<Map.Entry<EquipmentSlot, ItemStack>> cir) {
        Map<EquipmentSlot, ItemStack> map = enchantment.getEquipment(entity);
        if (map.isEmpty()) {
            cir.setReturnValue(null);
        }
        ArrayList<Map.Entry<EquipmentSlot, ItemStack>> list = Lists.newArrayList();
        for (Map.Entry<EquipmentSlot, ItemStack> entry : map.entrySet()) {
            ItemStack itemStack = entry.getValue();
            if (itemStack.isEmpty() || getLevel(enchantment, itemStack) <= 0 || !condition.test(itemStack)) continue;
            list.add(entry);
        }
        cir.setReturnValue(list.isEmpty() ? null : list.get(entity.getRandom().nextInt(list.size())));
    }
    @Unique
    private static Map.Entry<EquipmentSlot, ItemStack> chooseEquipmentWith(Enchantment enchantment, LivingEntity entity, Predicate<ItemStack> condition) {
        Map<EquipmentSlot, ItemStack> map = enchantment.getEquipment(entity);
        if (map.isEmpty()) {
            return null;
        }
        ArrayList<Map.Entry<EquipmentSlot, ItemStack>> list = Lists.newArrayList();
        for (Map.Entry<EquipmentSlot, ItemStack> entry : map.entrySet()) {
            ItemStack itemStack = entry.getValue();
            if (itemStack.isEmpty() || getLevel(enchantment, itemStack) <= 0 || !condition.test(itemStack)) continue;
            list.add(entry);
        }
        return list.isEmpty() ? null : list.get(entity.getRandom().nextInt(list.size()));
    }
    @Inject(method = "calculateRequiredExperienceLevel", at = @At("RETURN"), cancellable = true)
    private static void calculateRequiredExperienceLevel(Random random, int slotIndex, int bookshelfCount, ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        Item item = stack.getItem();
        int i = item.getEnchantability();
        if (i <= 0) {
            cir.setReturnValue(0);
        }
        if (bookshelfCount > 15) {
            bookshelfCount = 15;
        }
        int j = random.nextInt(8) + 1 + (bookshelfCount >> 1) + random.nextInt(bookshelfCount + 1);
        if (slotIndex == 0) {
            cir.setReturnValue(Math.max(j / 3, 1));
        }
        if (slotIndex == 1) {
            cir.setReturnValue(j * 2 / 3 + 1);
        }
        cir.setReturnValue(Math.max(j, bookshelfCount * 2));
    }
    @Inject(method = "enchant", at = @At("RETURN"), cancellable = true)
    private static void enchant(Random random, ItemStack target, int level, boolean treasureAllowed, CallbackInfoReturnable<ItemStack> cir) {
        List<EnchantmentLevelEntry> list = generateEnchantments(random, target, level, treasureAllowed);
        boolean bl = target.isOf(Items.BOOK);
        if (bl) {
            target = new ItemStack(Items.ENCHANTED_BOOK);
        }
        for (EnchantmentLevelEntry enchantmentLevelEntry : list) {
            if (bl) {
                EnchantedBookItem.addEnchantment(target, enchantmentLevelEntry);
                continue;
            }
            target.addEnchantment(enchantmentLevelEntry.enchantment, enchantmentLevelEntry.level);
        }
        cir.setReturnValue(target);
    }
    @Inject(method = "generateEnchantments", at = @At("RETURN"), cancellable = true)
    private static void generateEnchantments(Random random, ItemStack stack, int level, boolean treasureAllowed, CallbackInfoReturnable<List<EnchantmentLevelEntry>> cir) {
        ArrayList<EnchantmentLevelEntry> list = Lists.newArrayList();
        Item item = stack.getItem();
        int i = item.getEnchantability();
        if (i <= 0) {
            cir.setReturnValue(list);
        }
        level += 1 + random.nextInt(i / 4 + 1) + random.nextInt(i / 4 + 1);
        float f = (random.nextFloat() + random.nextFloat() - 1.0f) * 0.15f;
        List<EnchantmentLevelEntry> list2 = getPossibleEntries(level = MathHelper.clamp(Math.round((float)level + (float)level * f), 1, Integer.MAX_VALUE), stack, treasureAllowed);
        if (!list2.isEmpty()) {
            Weighting.getRandom(random, list2).ifPresent(list::add);
            while (random.nextInt(50) <= level) {
                if (!list.isEmpty()) {
                    removeConflicts(list2, Util.getLast(list));
                }
                if (list2.isEmpty()) break;
                Weighting.getRandom(random, list2).ifPresent(list::add);
                level /= 2;
            }
        }
        cir.setReturnValue(list);
    }
    @Unique
    private static List<EnchantmentLevelEntry> generateEnchantments(Random random, ItemStack stack, int level, boolean treasureAllowed) {
        ArrayList<EnchantmentLevelEntry> list = Lists.newArrayList();
        Item item = stack.getItem();
        int i = item.getEnchantability();
        if (i <= 0) {
            return list;
        }
        level += 1 + random.nextInt(i / 4 + 1) + random.nextInt(i / 4 + 1);
        float f = (random.nextFloat() + random.nextFloat() - 1.0f) * 0.15f;
        List<EnchantmentLevelEntry> list2 = getPossibleEntries(level = MathHelper.clamp(Math.round((float)level + (float)level * f), 1, Integer.MAX_VALUE), stack, treasureAllowed);
        if (!list2.isEmpty()) {
            Weighting.getRandom(random, list2).ifPresent(list::add);
            while (random.nextInt(50) <= level) {
                if (!list.isEmpty()) {
                    removeConflicts(list2, Util.getLast(list));
                }
                if (list2.isEmpty()) break;
                Weighting.getRandom(random, list2).ifPresent(list::add);
                level /= 2;
            }
        }
        return list;
    }
    @Inject(method = "removeConflicts", at = @At("HEAD"))
    private static void removeConflicts(List<EnchantmentLevelEntry> possibleEntries, EnchantmentLevelEntry pickedEntry, CallbackInfo ci) {
        Iterator<EnchantmentLevelEntry> iterator = possibleEntries.iterator();
        while (iterator.hasNext()) {
            if (pickedEntry.enchantment.canCombine(iterator.next().enchantment)) continue;
            iterator.remove();
        }
    }
    @Unique
    private static void removeConflicts(List<EnchantmentLevelEntry> possibleEntries, EnchantmentLevelEntry pickedEntry) {
        Iterator<EnchantmentLevelEntry> iterator = possibleEntries.iterator();
        while (iterator.hasNext()) {
            if (pickedEntry.enchantment.canCombine(iterator.next().enchantment)) continue;
            iterator.remove();
        }
    }
    @Inject(method = "isCompatible", at = @At("RETURN"), cancellable = true)
    private static void isCompatible(Collection<Enchantment> existing, Enchantment candidate, CallbackInfoReturnable<Boolean> cir) {
        for (Enchantment enchantment : existing) {
            if (enchantment.canCombine(candidate)) continue;
            cir.setReturnValue(false);
        }
        cir.setReturnValue(true);
    }
    @Inject(method = "getPossibleEntries", at = @At("RETURN"), cancellable = true)
    private static void getPossibleEntries(int power, ItemStack stack, boolean treasureAllowed, CallbackInfoReturnable<List<EnchantmentLevelEntry>> cir) {
        ArrayList<EnchantmentLevelEntry> list = Lists.newArrayList();
        Item item = stack.getItem();
        boolean bl = stack.isOf(Items.BOOK);
        block0: for (Enchantment enchantment : Registries.ENCHANTMENT) {
            if (enchantment.isTreasure() && !treasureAllowed || !enchantment.isAvailableForRandomSelection() || !enchantment.target.isAcceptableItem(item) && !bl) continue;
            for (int i = enchantment.getMaxLevel(); i > enchantment.getMinLevel() - 1; --i) {
                if (power < enchantment.getMinPower(i) || power > enchantment.getMaxPower(i)) continue;
                list.add(new EnchantmentLevelEntry(enchantment, i));
                continue block0;
            }
        }
        cir.setReturnValue(list);
    }
    @Unique
    private static List<EnchantmentLevelEntry> getPossibleEntries(int power, ItemStack stack, boolean treasureAllowed) {
        ArrayList<EnchantmentLevelEntry> list = Lists.newArrayList();
        Item item = stack.getItem();
        boolean bl = stack.isOf(Items.BOOK);
        block0: for (Enchantment enchantment : Registries.ENCHANTMENT) {
            if (enchantment.isTreasure() && !treasureAllowed || !enchantment.isAvailableForRandomSelection() || !enchantment.target.isAcceptableItem(item) && !bl) continue;
            for (int i = enchantment.getMaxLevel(); i > enchantment.getMinLevel() - 1; --i) {
                if (power < enchantment.getMinPower(i) || power > enchantment.getMaxPower(i)) continue;
                list.add(new EnchantmentLevelEntry(enchantment, i));
                continue block0;
            }
        }
        return list;
    }
}