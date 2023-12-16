package net.ua.mixin.it;

import java.util.function.Predicate;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.*;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BowItem.class)
public class bi extends RangedWeaponItem implements Vanishable {
    @Final
    @Shadow
    public static final int TICKS_PER_SECOND = 20;
    @Final
    @Shadow
    public static final int RANGE = 15;

    public bi(Item.Settings settings) {
        super(settings);
    }

    @Overwrite
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        boolean bl2;
        float f = 2;
        if (!(user instanceof PlayerEntity playerEntity)) {
            return;
        }
        boolean bl = playerEntity.getAbilities().creativeMode || EnchantmentHelper.getLevel(Enchantments.INFINITY, stack) > 0;
        ItemStack itemStack = playerEntity.getProjectileType(stack);
        if (itemStack.isEmpty() && !bl) {
            return;
        }
        if (itemStack.isEmpty()) {
            itemStack = new ItemStack(Items.ARROW);
        }
        bl2 = bl && itemStack.isOf(Items.ARROW);
        if (!world.isClient) {
            int k;
            int j;
            ArrowItem arrowItem = (ArrowItem)(itemStack.getItem() instanceof ArrowItem ? itemStack.getItem() : Items.ARROW);
            for (int i = 0; i < 20; i++){
                PersistentProjectileEntity persistentProjectileEntity = arrowItem.createArrow(world, itemStack, playerEntity);
                persistentProjectileEntity.setVelocity(playerEntity, playerEntity.getPitch(), playerEntity.getYaw(), 0.0f, f * 3.0f, 1.0f);
                if ((j = EnchantmentHelper.getLevel(Enchantments.POWER, stack)) > 0) {
                    persistentProjectileEntity.setDamage(persistentProjectileEntity.getDamage() + (double)j * 0.5 + 0.5);
                }
                if ((k = EnchantmentHelper.getLevel(Enchantments.PUNCH, stack)) > 0) {
                    persistentProjectileEntity.setPunch(k);
                }
                if (EnchantmentHelper.getLevel(Enchantments.FLAME, stack) > 0) {
                    persistentProjectileEntity.setOnFireFor(100);
                }
                persistentProjectileEntity.setCritical(true);
                // stack.damage(1, playerEntity, p -> p.sendToolBreakStatus(playerEntity.getActiveHand()));
                if (bl2 || playerEntity.getAbilities().creativeMode && (itemStack.isOf(Items.SPECTRAL_ARROW) || itemStack.isOf(Items.TIPPED_ARROW))) {
                    persistentProjectileEntity.pickupType = PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY;
                }
                world.spawnEntity(persistentProjectileEntity);
            }
        }
        world.playSound(null, playerEntity.getX(), playerEntity.getY(), playerEntity.getZ(), SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1.0f, 1.0f / (world.getRandom().nextFloat() * 0.4f + 1.2f) + f * 0.5f);
        if (!bl2 && !playerEntity.getAbilities().creativeMode) {
            itemStack.decrement(1);
            if (itemStack.isEmpty()) {
                playerEntity.getInventory().removeOne(itemStack);
            }
        }
        playerEntity.incrementStat(Stats.USED.getOrCreateStat(this));
    }


    @Overwrite
    public static float getPullProgress(int useTicks) {
        float f = (float)useTicks / TICKS_PER_SECOND;
        if ((f = (f * f + f * 2.0f) / 3.0f) > 1.0f) {
            f = 1.0f;
        }
        // return f;
        return 2.0f;
    }

    @Overwrite
    public int getMaxUseTime(ItemStack stack) {
        return 72000;
    }

    @Overwrite
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    @Overwrite
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        boolean bl2;
        float f = 2;
        ItemStack stack = user.getStackInHand(hand);
        boolean bl = user.getAbilities().creativeMode || EnchantmentHelper.getLevel(Enchantments.INFINITY, stack) > 0;
        ItemStack itemStack = user.getProjectileType(stack);
        if (itemStack.isEmpty()) {
            itemStack = new ItemStack(Items.ARROW);
        }
        bl2 = bl && itemStack.isOf(Items.ARROW);
        if (!world.isClient) {
            int k;
            int j;
            ArrowItem arrowItem = (ArrowItem)(itemStack.getItem() instanceof ArrowItem ? itemStack.getItem() : Items.ARROW);
            if (user.getAbilities().creativeMode) {
                for (int i = 0; i < 50; i++){
                    PersistentProjectileEntity persistentProjectileEntity = arrowItem.createArrow(world, itemStack, user);
                    persistentProjectileEntity.setVelocity(user, user.getPitch(), user.getYaw(), 0.0f, f * 3.0f, 1.0f);
                    persistentProjectileEntity.setCritical(true);
                    if ((j = EnchantmentHelper.getLevel(Enchantments.POWER, stack)) > 0) {
                        persistentProjectileEntity.setDamage(persistentProjectileEntity.getDamage() + (double)j * 0.5 + 0.5);
                    }
                    if ((k = EnchantmentHelper.getLevel(Enchantments.PUNCH, stack)) > 0) {
                        persistentProjectileEntity.setPunch(k);
                    }
                    if (EnchantmentHelper.getLevel(Enchantments.FLAME, stack) > 0) {
                        persistentProjectileEntity.setOnFireFor(100);
                    }
                    persistentProjectileEntity.setCritical(true);
                    // stack.damage(1, user, p -> p.sendToolBreakStatus(user.getActiveHand()));
                    if (bl2 || user.getAbilities().creativeMode && (itemStack.isOf(Items.SPECTRAL_ARROW) || itemStack.isOf(Items.TIPPED_ARROW))) {
                        persistentProjectileEntity.pickupType = PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY;
                    }
                    world.spawnEntity(persistentProjectileEntity);
                }
            } else {
                for (int i = 0; i < 5; i++){
                    PersistentProjectileEntity persistentProjectileEntity = arrowItem.createArrow(world, itemStack, user);
                    persistentProjectileEntity.setVelocity(user, user.getPitch(), user.getYaw(), 0.0f, (i + 1) / 2f, 1.0f);
                    persistentProjectileEntity.setCritical(true);
                    if ((j = EnchantmentHelper.getLevel(Enchantments.POWER, stack)) > 0) {
                        persistentProjectileEntity.setDamage(persistentProjectileEntity.getDamage() + (double)j * 0.5 + 0.5);
                    }
                    if ((k = EnchantmentHelper.getLevel(Enchantments.PUNCH, stack)) > 0) {
                        persistentProjectileEntity.setPunch(k);
                    }
                    if (EnchantmentHelper.getLevel(Enchantments.FLAME, stack) > 0) {
                        persistentProjectileEntity.setOnFireFor(100);
                    }
                    persistentProjectileEntity.setCritical(true);
                    // stack.damage(1, user, p -> p.sendToolBreakStatus(user.getActiveHand()));
                    if (bl2 || user.getAbilities().creativeMode && (itemStack.isOf(Items.SPECTRAL_ARROW) || itemStack.isOf(Items.TIPPED_ARROW))) {
                        persistentProjectileEntity.pickupType = PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY;
                    }
                    world.spawnEntity(persistentProjectileEntity);
                }
            }
        }
        world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1.0f, 1.0f / (world.getRandom().nextFloat() * 0.4f + 1.2f) + f * 0.5f);
        if (!bl2 && !user.getAbilities().creativeMode) {
            itemStack.decrement(1);
            if (itemStack.isEmpty()) {
                user.getInventory().removeOne(itemStack);
            }
        }
        user.incrementStat(Stats.USED.getOrCreateStat(this));
        return TypedActionResult.success(stack);
    }

    @Overwrite
    public Predicate<ItemStack> getProjectiles() {
        return BOW_PROJECTILES;
    }

    @Overwrite
    public int getRange() {
        return RANGE;
    }
}