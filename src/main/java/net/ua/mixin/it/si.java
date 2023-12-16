package net.ua.mixin.it;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;

@Mixin(SwordItem.class)
public class si extends ToolItem implements Vanishable {
    @Mutable
    @Final
    @Shadow
    private final float attackDamage;
    @Mutable
    @Final
    @Shadow
    private final Multimap<EntityAttribute, EntityAttributeModifier> attributeModifiers;


    public si(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Item.Settings settings) {
        super(toolMaterial, settings);
        // this.attackDamage = (float)attackDamage + toolMaterial.getAttackDamage();
        this.attackDamage = 30;
        ImmutableMultimap.Builder<EntityAttribute, EntityAttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(EntityAttributes.GENERIC_ATTACK_DAMAGE, new EntityAttributeModifier(ATTACK_DAMAGE_MODIFIER_ID, "Weapon modifier", this.attackDamage, EntityAttributeModifier.Operation.ADDITION));
        builder.put(EntityAttributes.GENERIC_ATTACK_SPEED, new EntityAttributeModifier(ATTACK_SPEED_MODIFIER_ID, "Weapon modifier", 2147483646, EntityAttributeModifier.Operation.ADDITION));
        this.attributeModifiers = builder.build();
    }

    @Overwrite
    public float getAttackDamage() {
        return 30;
    }

    @Overwrite
    public boolean canMine(BlockState state, World world, BlockPos pos, PlayerEntity miner) {
        return !miner.isCreative();
    }

    @Overwrite
    public float getMiningSpeedMultiplier(ItemStack stack, BlockState state) {
        if (state.isOf(Blocks.COBWEB)) {
            return 15.0f;
        }
        return state.isIn(BlockTags.SWORD_EFFICIENT) ? 1.5f : 1.0f;
    }

    @Overwrite
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.damage(1, attacker, e -> e.sendEquipmentBreakStatus(EquipmentSlot.MAINHAND));
        return true;
    }

    @Overwrite
    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        if (state.getHardness(world, pos) != 0.0f) {
            stack.damage(2, miner, e -> e.sendEquipmentBreakStatus(EquipmentSlot.MAINHAND));
        }
        return true;
    }

    @Overwrite
    public boolean isSuitableFor(BlockState state) {
        return state.isOf(Blocks.COBWEB);
    }

    @Overwrite
    public Multimap<EntityAttribute, EntityAttributeModifier> getAttributeModifiers(EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND) {
            return this.attributeModifiers;
        }
        return super.getAttributeModifiers(slot);
    }
}
