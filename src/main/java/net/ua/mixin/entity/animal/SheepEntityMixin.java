package net.ua.mixin.entity.animal;

import com.google.common.collect.Maps;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.*;
import net.minecraft.loot.LootTables;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeType;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.*;
import java.util.stream.Collectors;

@Mixin(SheepEntity.class)
public class SheepEntityMixin extends AnimalEntity implements Shearable {
    @Unique
    private static final TrackedData<Byte> COLOR;
    @Unique
    private static final Map<DyeColor, ItemConvertible> DROPS;
    @Unique
    private EatGrassGoal eatGrassGoal;
    @Unique
    private static final EnumMap COLORS;
    @Unique
    private int eatGrassTimer;
    /**
     * @author Seoggeonjin
     * @reason test
     */
    @Overwrite
    public void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(COLOR, (byte)0);
    }
    public SheepEntityMixin(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }
    /**
     * @author Seoggeonjin
     * @reason test
     */
    @Overwrite
    public boolean isSheared() {
        return (this.dataTracker.get(COLOR) & 16) != 0;
    }
    /**
     * @author Seoggeonjin
     * @reason test
     */
    @Overwrite
    public static float[] getRgbColor(DyeColor dyeColor) {
        return (float[])COLORS.get(dyeColor);
    }
    /**
     * @author Seoggeonjin
     * @reason test
     */
    @Overwrite
    public Identifier getLootTableId() {
        if (this.isSheared()) {
            return this.getType().getLootTableId();
        } else {
            return switch (this.getColor()) {
                case WHITE, BLACK, GREEN, RED, ORANGE, LIGHT_BLUE, LIME, MAGENTA, GRAY, PINK, LIGHT_GRAY, CYAN, PURPLE, YELLOW, BLUE, BROWN -> LootTables.RED_SHEEP_ENTITY;
            };
        }
    }
    /**
     * @author Seoggeonjin
     * @reason test
     */
    @Overwrite
    public void mobTick() {
        this.eatGrassTimer = this.eatGrassGoal.getTimer();
        super.mobTick();
    }
    /**
     * @author Seoggeonjin
     * @reason test
     */
    @Overwrite
    public void tickMovement() {
        if (this.getWorld().isClient) {
            this.eatGrassTimer = Math.max(0, this.eatGrassTimer - 10);
        }

        super.tickMovement();
    }
    /**
     * @author Seoggeonjin
     * @reason test
     */
    @Overwrite
    public void handleStatus(byte status) {
        if (status == 10) {
            this.eatGrassTimer = 40;
        } else {
            super.handleStatus(status);
        }

    }
    /**
     * @author Seoggeonjin
     * @reason test
     */
    @Overwrite
    public float getHeadAngle(float delta) {
        if (this.eatGrassTimer > 4 && this.eatGrassTimer <= 36) {
            float f = ((float)(this.eatGrassTimer - 4) - delta) / 32.0F;
            return 0.62831855F + 0.21991149F * MathHelper.sin(f * 28.7F);
        } else {
            return this.eatGrassTimer > 0 ? 0.62831855F : this.getPitch() * 0.017453292F;
        }
    }
    /**
     * @author Seoggeonjin
     * @reason test
     */
    @Overwrite
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        if (itemStack.isOf(Items.SHEARS)) {
            if (!this.getWorld().isClient && this.isShearable()) {
                this.sheared(SoundCategory.PLAYERS);
                this.emitGameEvent(GameEvent.SHEAR, player);
                itemStack.damage(1, player, (playerx) -> {
                    playerx.sendToolBreakStatus(hand);
                });
                return ActionResult.SUCCESS;
            } else {
                return ActionResult.CONSUME;
            }
        } else {
            return super.interactMob(player, hand);
        }
    }
    /**
     * @author Seoggeonjin
     * @reason test
     */
    @Overwrite
    public float getNeckAngle(float delta) {
        if (this.eatGrassTimer <= 0) {
            return 0.0F;
        } else if (this.eatGrassTimer >= 4 && this.eatGrassTimer <= 36) {
            return 1.0F;
        } else {
            return this.eatGrassTimer < 4 ? ((float)this.eatGrassTimer - delta) / 4.0F : -((float)(this.eatGrassTimer - 40) - delta) / 4.0F;
        }
    }
    /**
     * @author Seoggeonjin
     * @reason test
     */
    @Overwrite
    public void initGoals() {
        this.eatGrassGoal = new EatGrassGoal(this);
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new EscapeDangerGoal(this, 1.25));
        this.goalSelector.add(2, new AnimalMateGoal(this, 1.0));
        this.goalSelector.add(3, new TemptGoal(this, 1.1, Ingredient.ofItems(Items.WHEAT), false));
        this.goalSelector.add(4, new FollowParentGoal(this, 1.1));
        this.goalSelector.add(5, this.eatGrassGoal);
        this.goalSelector.add(6, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.add(8, new LookAroundGoal(this));
    }

    @Override
    public void sheared(SoundCategory shearedSoundCategory) {
        this.getWorld().playSoundFromEntity(null, this, SoundEvents.ENTITY_SHEEP_SHEAR, shearedSoundCategory, 1.0F, 1.0F);
        this.setSheared(true);
        int i = 1 + this.random.nextInt(3);

        for(int j = 0; j < i; ++j) {
            ItemEntity itemEntity = this.dropItem(DROPS.get(this.getColor()), 1);
            if (itemEntity != null) {
                itemEntity.setVelocity(itemEntity.getVelocity().add((this.random.nextFloat() - this.random.nextFloat()) * 0.1F, (double)(this.random.nextFloat() * 0.05F), (double)((this.random.nextFloat() - this.random.nextFloat()) * 0.1F)));
            }
        }
    }
    /**
     * @author Seoggeonjin
     * @reason test
     */
    @Overwrite
    public DyeColor getColor() {
        return DyeColor.byId(this.dataTracker.get(COLOR) & 15);
    }

    /**
     * @author Seoggeonjin
     * @reason test
     */
    @Overwrite
    public void setColor(DyeColor color) {
        byte b = this.dataTracker.get(COLOR);
        this.dataTracker.set(COLOR, (byte)(b & 240 | color.getId() & 15));
    }

    /**
     * @author Seoggeonjin
     * @reason test
     */
    @Overwrite
    public boolean isShearable() {
        return false;
    }
    /**
     * @author Seoggeonjin
     * @reason test
     */
    @Overwrite
    private static float[] getDyedColor(DyeColor color) {
        if (color == DyeColor.WHITE) {
            return new float[]{0.9019608F, 0.9019608F, 0.9019608F};
        } else {
            float[] fs = color.getColorComponents();
            return new float[]{fs[0] * 0.75F, fs[1] * 0.75F, fs[2] * 0.75F};
        }
    }
    /**
     * @author Seoggeonjin
     * @reason test
     */
    @Overwrite
    public void setSheared(boolean sheared) {
        byte b = this.dataTracker.get(COLOR);
        if (sheared) {
            this.dataTracker.set(COLOR, (byte)(b | 16));
        } else {
            this.dataTracker.set(COLOR, (byte)(b & -17));
        }

    }

    /**
     * @author Seoggeonjin
     * @reason test
     */
    @Overwrite
    @Nullable
    public SheepEntity createChild(ServerWorld serverWorld, PassiveEntity passiveEntity) {
        SheepEntity sheepEntity = EntityType.SHEEP.create(serverWorld);
        if (sheepEntity != null) {
            sheepEntity.setColor(this.getChildColor(this, (SheepEntity)passiveEntity));
        }

        return sheepEntity;
    }
    /**
     * @author Seoggeonjin
     * @reason test
     */
    @Overwrite
    private DyeColor getChildColor(AnimalEntity firstParent, AnimalEntity secondParent) {
        DyeColor dyeColor = ((SheepEntity)firstParent).getColor();
        DyeColor dyeColor2 = ((SheepEntity)secondParent).getColor();
        RecipeInputInventory recipeInputInventory = createDyeMixingCraftingInventory(dyeColor, dyeColor2);
        return this.getWorld().getRecipeManager().getFirstMatch(RecipeType.CRAFTING, recipeInputInventory, this.getWorld()).map(recipeEntry -> ((CraftingRecipe)recipeEntry.value()).craft(recipeInputInventory, this.getWorld().getRegistryManager())).map(ItemStack::getItem).filter(DyeItem.class::isInstance).map(DyeItem.class::cast).map(DyeItem::getColor).orElseGet(() -> this.getWorld().random.nextBoolean() ? dyeColor : dyeColor2);
    }
    /**
     * @author Seoggeonjin
     * @reason test
     */
    @Overwrite
    @Nullable
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        this.setColor(generateDefaultColor(world.getRandom()));
        return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
    }
    /**
     * @author Seoggeonjin
     * @reason test
     */
    @Overwrite
    public static DyeColor generateDefaultColor(Random random) {
        int i = random.nextInt(100);
        if (i < 5) {
            return DyeColor.BLACK;
        } else if (i < 10) {
            return DyeColor.GRAY;
        } else if (i < 15) {
            return DyeColor.LIGHT_GRAY;
        } else if (i < 18) {
            return DyeColor.BROWN;
        } else {
            return random.nextInt(500) == 0 ? DyeColor.PINK : DyeColor.WHITE;
        }
    }
    @Unique
    private static CraftingInventory createDyeMixingCraftingInventory(DyeColor firstColor, DyeColor secondColor) {
        CraftingInventory craftingInventory = new CraftingInventory(new ScreenHandler(null, -1) {
            public ItemStack quickMove(PlayerEntity player, int slot) {
                return ItemStack.EMPTY;
            }

            public boolean canUse(PlayerEntity player) {
                return false;
            }
        }, 2, 1);
        craftingInventory.setStack(0, new ItemStack(DyeItem.byColor(firstColor)));
        craftingInventory.setStack(1, new ItemStack(DyeItem.byColor(secondColor)));
        return craftingInventory;
    }
    /**
     * @author Seoggeonjin
     * @reason test
     */
    @Overwrite
    public float getActiveEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return 0.95F * dimensions.height;
    }
    public void playStepSound(BlockPos pos, BlockState state) {
        try {
            this.playSound(SoundEvents.ENTITY_COW_AMBIENT, 0.15F, 10.0F);
        } catch (Exception ignored) {
            this.playSound(SoundEvents.ENTITY_COW_AMBIENT, 0.15F, 0.0F);
        }
    }
    public SoundEvent getAmbientSound() {
        return null;
    }

    public SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_COW_HURT;
    }

    public SoundEvent getDeathSound() {
        return SoundEvents.BLOCK_ANVIL_USE;
    }
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("Sheared", this.isSheared());
        nbt.putByte("Color", (byte)this.getColor().getId());
    }

    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.setSheared(nbt.getBoolean("Sheared"));
        this.setColor(DyeColor.byId(nbt.getByte("Color")));
    }
    @Override
    public boolean isBaby() {
        return false;
    }

    static {
        COLOR = DataTracker.registerData(SheepEntityMixin.class, TrackedDataHandlerRegistry.BYTE);
        DROPS = Util.make(Maps.newEnumMap(DyeColor.class), (map) -> {
            map.put(DyeColor.WHITE, Blocks.WHITE_WOOL);
            map.put(DyeColor.ORANGE, Blocks.ORANGE_WOOL);
            map.put(DyeColor.MAGENTA, Blocks.MAGENTA_WOOL);
            map.put(DyeColor.LIGHT_BLUE, Blocks.LIGHT_BLUE_WOOL);
            map.put(DyeColor.YELLOW, Blocks.YELLOW_WOOL);
            map.put(DyeColor.LIME, Blocks.LIME_WOOL);
            map.put(DyeColor.PINK, Blocks.PINK_WOOL);
            map.put(DyeColor.GRAY, Blocks.GRAY_WOOL);
            map.put(DyeColor.LIGHT_GRAY, Blocks.LIGHT_GRAY_WOOL);
            map.put(DyeColor.CYAN, Blocks.CYAN_WOOL);
            map.put(DyeColor.PURPLE, Blocks.PURPLE_WOOL);
            map.put(DyeColor.BLUE, Blocks.BLUE_WOOL);
            map.put(DyeColor.BROWN, Blocks.BROWN_WOOL);
            map.put(DyeColor.GREEN, Blocks.GREEN_WOOL);
            map.put(DyeColor.RED, Blocks.RED_WOOL);
            map.put(DyeColor.BLACK, Blocks.BLACK_WOOL);
        });
        COLORS = Maps.newEnumMap((Map) Arrays.stream(DyeColor.values()).collect(Collectors.toMap((color) -> color, SheepEntityMixin::getDyedColor)));
    }
}