package net.ua.mixin.otherwise;

import com.google.common.collect.ImmutableList;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.recipe.RecipeMatcher;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Nameable;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import org.spongepowered.asm.mixin.*;

import java.util.List;
import java.util.function.Predicate;

@Mixin(PlayerInventory.class)
public class pi implements Inventory, Nameable {
    @Final
    @Shadow
    public static final int ITEM_USAGE_COOLDOWN = 1000;
    @Final
    @Shadow
    public static final int HOTBAR_SIZE = 9;
    @Final
    @Shadow
    public static final int NOT_FOUND = -1;
    @Final
    @Shadow
    public static final int[] ARMOR_SLOTS = new int[]{0, 1, 2, 3};
    @Final
    @Shadow
    public static final int[] HELMET_SLOTS = new int[]{3};
    @Unique
    public int selectedSlot;
    @Final
    @Shadow
    public static final int MAIN_SIZE = 63;
    @Final
    @Shadow
    public static final int OFF_HAND_SLOT = 40;
    @Final
    @Shadow
    public final DefaultedList<ItemStack> main = DefaultedList.ofSize(36, ItemStack.EMPTY);
    @Final
    @Shadow
    public final DefaultedList<ItemStack> armor = DefaultedList.ofSize(4, ItemStack.EMPTY);
    @Final
    @Shadow
    public final DefaultedList<ItemStack> offHand = DefaultedList.ofSize(1, ItemStack.EMPTY);
    @Final
    @Shadow
    private final List<DefaultedList<ItemStack>> combinedInventory = ImmutableList.of(this.main, this.armor, this.offHand);
    @Shadow
    private int changeCount;
    @Mutable
    @Shadow
    @Final
    public final PlayerEntity player;
    public pi(PlayerEntity player) {
        this.player = player;
    }
    @Override
    public int size() {
        return this.main.size() + this.armor.size() + this.offHand.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemStack : this.main) {
            if (itemStack.isEmpty()) continue;
            return false;
        }
        for (ItemStack itemStack : this.armor) {
            if (itemStack.isEmpty()) continue;
            return false;
        }
        for (ItemStack itemStack : this.offHand) {
            if (itemStack.isEmpty()) continue;
            return false;
        }
        return true;
    }
    @Override
    public ItemStack getStack(int slot) {
        DefaultedList<ItemStack> list = null;
        for (DefaultedList<ItemStack> defaultedList : this.combinedInventory) {
            if (slot < defaultedList.size()) {
                list = defaultedList;
                break;
            }
            slot -= defaultedList.size();
        }
        return list == null ? ItemStack.EMPTY : list.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        DefaultedList<ItemStack> list = null;
        for (DefaultedList<ItemStack> defaultedList : this.combinedInventory) {
            if (slot < defaultedList.size()) {
                list = defaultedList;
                break;
            }
            slot -= defaultedList.size();
        }
        if (list != null && !list.get(slot).isEmpty()) {
            return Inventories.splitStack(list, slot, amount);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStack(int slot) {
        DefaultedList<ItemStack> defaultedList = null;
        for (DefaultedList<ItemStack> defaultedList2 : this.combinedInventory) {
            if (slot < defaultedList2.size()) {
                defaultedList = defaultedList2;
                break;
            }
            slot -= defaultedList2.size();
        }
        if (defaultedList != null && !defaultedList.get(slot).isEmpty()) {
            ItemStack itemStack = defaultedList.get(slot);
            defaultedList.set(slot, ItemStack.EMPTY);
            return itemStack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        DefaultedList<ItemStack> defaultedList = null;
        for (DefaultedList<ItemStack> defaultedList2 : this.combinedInventory) {
            if (slot < defaultedList2.size()) {
                defaultedList = defaultedList2;
                break;
            }
            slot -= defaultedList2.size();
        }
        if (defaultedList != null) {
            defaultedList.set(slot, stack);
        }
    }

    @Override
    public void markDirty() {
        ++this.changeCount;
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return !this.player.isRemoved();
    }

    @Override
    public void clear() {
        for (List<ItemStack> list : this.combinedInventory) {
            list.clear();
        }
    }

    @Overwrite
    public Text getName() {
        return Text.translatable("container.inventory");
    }
    @Override
    public int getMaxCountPerStack() {
        return 2147483647;
    }
    /**
     * @author Seoggeonjin
     * @reason test
     */
    @Overwrite
    private boolean canStackAddMore(ItemStack existingStack, ItemStack stack) {
        // return true;
        return !existingStack.isEmpty() && ItemStack.canCombine(existingStack, stack) && existingStack.isStackable() && existingStack.getCount() < existingStack.getMaxCount() && existingStack.getCount() < this.getMaxCountPerStack();
    }

    @Overwrite
    public int getEmptySlot() {
        for (int i = 0; i < this.main.size(); ++i) {
            if (!this.main.get(i).isEmpty()) continue;
            return i;
        }
        return -1;
    }

    @Overwrite
    public int getSlotWithStack(ItemStack stack) {
        for (int i = 0; i < this.main.size(); ++i) {
            if (this.main.get(i).isEmpty() || !ItemStack.canCombine(stack, this.main.get(i))) continue;
            return i;
        }
        return -1;
    }

    /**
     * Given the item stack to search for, returns the equivalent slot index with a matching stack that is all of:
     * not damaged, not enchanted, and not renamed.
     *
     * @return the index where a matching stack was found, or {@value #NOT_FOUND}
     */
    @Overwrite
    public int indexOf(ItemStack stack) {
        for (int i = 0; i < this.main.size(); ++i) {
            ItemStack itemStack = this.main.get(i);
            if (this.main.get(i).isEmpty() || !ItemStack.canCombine(stack, this.main.get(i)) || this.main.get(i).isDamaged() || itemStack.hasEnchantments() || itemStack.hasCustomName()) continue;
            return i;
        }
        return -1;
    }

    @Overwrite
    public int remove(Predicate<ItemStack> shouldRemove, int maxCount, Inventory craftingInventory) {
        int i = 0;
        boolean bl = maxCount == 0;
        i += Inventories.remove(this, shouldRemove, maxCount - i, bl);
        i += Inventories.remove(craftingInventory, shouldRemove, maxCount - i, bl);
        ItemStack itemStack = this.player.currentScreenHandler.getCursorStack();
        i += Inventories.remove(itemStack, shouldRemove, maxCount - i, bl);
        if (itemStack.isEmpty()) {
            this.player.currentScreenHandler.setCursorStack(ItemStack.EMPTY);
        }
        return i;
    }

    @Overwrite
    private int addStack(ItemStack stack) {
        int i = this.getOccupiedSlotWithRoomForStack(stack);
        if (i == -1) {
            i = this.getEmptySlot();
        }
        if (i == -1) {
            return stack.getCount();
        }
        return this.addStack(i, stack);
    }

    @Overwrite
    private int addStack(int slot, ItemStack stack) {
        int j;
        net.minecraft.item.Item item = stack.getItem();
        int i = stack.getCount();
        ItemStack itemStack = this.getStack(slot);
        if (itemStack.isEmpty()) {
            itemStack = new ItemStack(item, 0);
            if (stack.hasNbt()) {
                itemStack.setNbt(stack.getNbt().copy());
            }
            this.setStack(slot, itemStack);
        }
        if ((j = i) > itemStack.getMaxCount() - itemStack.getCount()) {
            j = itemStack.getMaxCount() - itemStack.getCount();
        }
        if (j > this.getMaxCountPerStack() - itemStack.getCount()) {
            j = this.getMaxCountPerStack() - itemStack.getCount();
        }
        if (j == 0) {
            return i;
        }
        itemStack.increment(j);
        itemStack.setBobbingAnimationTime(5);
        return i - j;
    }

    @Overwrite
    public int getOccupiedSlotWithRoomForStack(ItemStack stack) {
        if (this.canStackAddMore(this.getStack(this.selectedSlot), stack)) {
            return this.selectedSlot;
        }
        if (this.canStackAddMore(this.getStack(40), stack)) {
            return 40;
        }
        for (int i = 0; i < this.main.size(); ++i) {
            if (!this.canStackAddMore(this.main.get(i), stack)) continue;
            return i;
        }
        return -1;
    }

    @Overwrite
    public void updateItems() {
        for (DefaultedList<ItemStack> defaultedList : this.combinedInventory) {
            for (int i = 0; i < defaultedList.size(); ++i) {
                if (defaultedList.get(i).isEmpty()) continue;
                defaultedList.get(i).inventoryTick(this.player.getWorld(), this.player, i, this.selectedSlot == i);
            }
        }
    }

    @Overwrite
    public boolean insertStack(ItemStack stack) {
        return this.insertStack(-1, stack);
    }

    @Overwrite
    public boolean insertStack(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        try {
            if (!stack.isDamaged()) {
                int i;
                do {
                    i = stack.getCount();
                    if (slot == -1) {
                        stack.setCount(this.addStack(stack));
                        continue;
                    }
                    stack.setCount(this.addStack(slot, stack));
                } while (!stack.isEmpty() && stack.getCount() < i);
                if (stack.getCount() == i && this.player.getAbilities().creativeMode) {
                    stack.setCount(0);
                    return true;
                }
                return stack.getCount() < i;
            }
            if (slot == -1) {
                slot = this.getEmptySlot();
            }
            if (slot >= 0) {
                this.main.set(slot, stack.copyAndEmpty());
                this.main.get(slot).setBobbingAnimationTime(5);
                return true;
            }
            if (this.player.getAbilities().creativeMode) {
                stack.setCount(0);
                return true;
            }
            return false;
        } catch (Throwable throwable) {
            CrashReport crashReport = CrashReport.create(throwable, "Adding item to inventory");
            CrashReportSection crashReportSection = crashReport.addElement("Item being added");
            crashReportSection.add("Item ID", net.minecraft.item.Item.getRawId(stack.getItem()));
            crashReportSection.add("Item data", stack.getDamage());
            crashReportSection.add("Item name", () -> stack.getName().getString());
            throw new CrashException(crashReport);
        }
    }

    @Overwrite
    public void offerOrDrop(ItemStack stack) {
        this.offer(stack, true);
    }

    @Overwrite
    public void offer(ItemStack stack, boolean notifiesClient) {
        while (!stack.isEmpty()) {
            int i = this.getOccupiedSlotWithRoomForStack(stack);
            if (i == -1) {
                i = this.getEmptySlot();
            }
            if (i == -1) {
                this.player.dropItem(stack, false);
                break;
            }
            int j = stack.getMaxCount() - this.getStack(i).getCount();
            if (!this.insertStack(i, stack.split(j)) || !notifiesClient || !(this.player instanceof ServerPlayerEntity)) continue;
            ((ServerPlayerEntity)this.player).networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(-2, 0, i, this.getStack(i)));
        }
    }
    @Overwrite
    public void removeOne(ItemStack stack) {
        block0: for (DefaultedList<ItemStack> defaultedList : this.combinedInventory) {
            for (int i = 0; i < defaultedList.size(); ++i) {
                if (defaultedList.get(i) != stack) continue;
                defaultedList.set(i, ItemStack.EMPTY);
                continue block0;
            }
        }
    }

    @Overwrite
    public float getBlockBreakingSpeed(BlockState block) {
        return this.main.get(this.selectedSlot).getMiningSpeedMultiplier(block);
    }

    @Overwrite
    public NbtList writeNbt(NbtList nbtList) {
        NbtCompound nbtCompound;
        int i;
        for (i = 0; i < this.main.size(); ++i) {
            if (this.main.get(i).isEmpty()) continue;
            nbtCompound = new NbtCompound();
            nbtCompound.putByte("Slot", (byte)i);
            this.main.get(i).writeNbt(nbtCompound);
            nbtList.add(nbtCompound);
        }
        for (i = 0; i < this.armor.size(); ++i) {
            if (this.armor.get(i).isEmpty()) continue;
            nbtCompound = new NbtCompound();
            nbtCompound.putByte("Slot", (byte)(i + 100));
            this.armor.get(i).writeNbt(nbtCompound);
            nbtList.add(nbtCompound);
        }
        for (i = 0; i < this.offHand.size(); ++i) {
            if (this.offHand.get(i).isEmpty()) continue;
            nbtCompound = new NbtCompound();
            nbtCompound.putByte("Slot", (byte)(i + 150));
            this.offHand.get(i).writeNbt(nbtCompound);
            nbtList.add(nbtCompound);
        }
        return nbtList;
    }

    @Overwrite
    public void readNbt(NbtList nbtList) {
        this.main.clear();
        this.armor.clear();
        this.offHand.clear();
        for (int i = 0; i < nbtList.size(); ++i) {
            NbtCompound nbtCompound = nbtList.getCompound(i);
            int j = nbtCompound.getByte("Slot") & 0xFF;
            ItemStack itemStack = ItemStack.fromNbt(nbtCompound);
            if (itemStack.isEmpty()) continue;
            if (j >= 0 && j < this.main.size()) {
                this.main.set(j, itemStack);
                continue;
            }
            if (j >= 100 && j < this.armor.size() + 100) {
                this.armor.set(j - 100, itemStack);
                continue;
            }
            if (j < 150 || j >= this.offHand.size() + 150) continue;
            this.offHand.set(j - 150, itemStack);
        }
    }

    @Overwrite
    public ItemStack getArmorStack(int slot) {
        return this.armor.get(slot);
    }

    @Overwrite
    public void damageArmor(DamageSource damageSource, float amount, int[] slots) {
        if (amount <= 0.0f) {
            return;
        }
        if ((amount /= 4.0f) < 1.0f) {
            amount = 1.0f;
        }
        for (int i : slots) {
            ItemStack itemStack = this.armor.get(i);
            if (damageSource.isIn(DamageTypeTags.IS_FIRE) && itemStack.getItem().isFireproof() || !(itemStack.getItem() instanceof ArmorItem)) continue;
            itemStack.damage((int) amount, this.player, player -> player.sendEquipmentBreakStatus(EquipmentSlot.fromTypeIndex(EquipmentSlot.Type.ARMOR, i)));

        }
    }
    @Overwrite
    public void dropAll() {
        for (List list : this.combinedInventory) {
            for (int i = 0; i < list.size(); ++i) {
                ItemStack itemStack = (ItemStack)list.get(i);
                if (itemStack.isEmpty()) continue;
                this.player.dropItem(itemStack, true, false);
                list.set(i, ItemStack.EMPTY);
            }
        }
    }

    @Overwrite
    public int getChangeCount() {
        return this.changeCount;
    }

    @Overwrite
    public boolean contains(ItemStack stack) {
        for (DefaultedList<ItemStack> list : this.combinedInventory) {
            for (ItemStack itemStack : list) {
                if (itemStack.isEmpty() || !ItemStack.canCombine(itemStack, stack)) continue;
                return true;
            }
        }
        return false;
    }

    @Overwrite
    public boolean contains(TagKey<Item> tag) {
        for (DefaultedList<ItemStack> list : this.combinedInventory) {
            for (ItemStack itemStack : list) {
                if (itemStack.isEmpty() || !itemStack.isIn(tag)) continue;
                return true;
            }
        }
        return false;
    }

    @Overwrite
    public void clone(PlayerInventory other) {
        for (int i = 0; i < this.size(); ++i) {
            this.setStack(i, other.getStack(i));
        }
        this.selectedSlot = other.selectedSlot;
    }

    @Overwrite
    public void populateRecipeFinder(RecipeMatcher finder) {
        for (ItemStack itemStack : this.main) {
            finder.addUnenchantedInput(itemStack);
        }
    }
}
