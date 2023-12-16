package net.ua.mixin.entity.abst;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.control.LookControl;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.IllagerEntity;
import net.minecraft.entity.mob.SpellcastingIllagerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.ua.ips.spellI;
import net.ua.ips.spellI2;
import net.ua.mixin2.inner.Spell;
import org.spongepowered.asm.mixin.*;

@Mixin(SpellcastingIllagerEntity.class)
public abstract class si extends IllagerEntity implements spellI {
    @Final
    @Shadow
    private static final TrackedData<Byte> SPELL = DataTracker.registerData(si.class, TrackedDataHandlerRegistry.BYTE);
    @Shadow
    protected int spellTicks;

    @Unique
    private spellI2 spell = Spell.NONE;

    public si(EntityType<? extends si> entityType, World world) {
        super(entityType, world);
    }
    @Unique
    public void spellTicks(int tick) {
        this.spellTicks = tick;
    }

    @Overwrite
    public void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(SPELL, (byte)0);
    }

    @Overwrite
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.spellTicks = nbt.getInt("SpellTicks");
    }

    @Overwrite
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("SpellTicks", this.spellTicks);
    }

    @Overwrite
    public IllagerEntity.State getState() {
        if (this.isSpellcasting()) {
            return IllagerEntity.State.SPELLCASTING;
        }
        if (this.isCelebrating()) {
            return IllagerEntity.State.CELEBRATING;
        }
        return IllagerEntity.State.CROSSED;
    }

    @Overwrite
    public boolean isSpellcasting() {
        if (this.getWorld().isClient) {
            return this.dataTracker.get(SPELL) > 0;
        }
        return this.spellTicks > 0;
    }

    @Unique
    public void setSpell(spellI2 spell) {
        this.spell = spell;
        this.dataTracker.set(SPELL, (byte)spell.id());
    }

    @Unique
    public spellI2 getSpell() {
        if (!this.getWorld().isClient) {
            return this.spell;
        }
        return Spell.byId(this.dataTracker.get(SPELL));
    }

    @Overwrite
    public void mobTick() {
        super.mobTick();
        if (this.spellTicks > 0) {
            --this.spellTicks;
        }
    }

    @Overwrite
    public void tick() {
        super.tick();
        if (this.getWorld().isClient && this.isSpellcasting()) {
            spellI2 spell = this.getSpell();
            double d = spell.particleVelocity(0);
            double e = spell.particleVelocity(1);
            double f = spell.particleVelocity(2);
            float g = this.bodyYaw * ((float)Math.PI / 180) + MathHelper.cos((float)this.age * 0.6662f) * 0.25f;
            float h = MathHelper.cos(g);
            float i = MathHelper.sin(g);
            this.getWorld().addParticle(ParticleTypes.ENTITY_EFFECT, this.getX() + (double)h * 0.6, this.getY() + 1.8, this.getZ() + (double)i * 0.6, d, e, f);
            this.getWorld().addParticle(ParticleTypes.ENTITY_EFFECT, this.getX() - (double)h * 0.6, this.getY() + 1.8, this.getZ() - (double)i * 0.6, d, e, f);
        }
    }

    @Overwrite
    public int getSpellTicks() {
        return this.spellTicks;
    }

    public abstract SoundEvent getCastSpellSound();
    @Unique
    public LivingEntity getTarget() {
        return super.getTarget();
    }
    @Unique
    public EntityNavigation navigation() {
        return this.navigation;
    }
    @Unique
    public LookControl getLookControl() {
        return super.getLookControl();
    }
    @Unique
    public int getMaxHeadRotation() {
        return super.getMaxHeadRotation();
    }
    @Unique
    public int getMaxLookPitchChange() {
        return super.getMaxLookPitchChange();
    }
    @Unique
    public int age() {
        return this.age;
    }
    @Unique
    public void playSound(SoundEvent sound, float volume, float pitch) {
        super.playSound(sound, volume, pitch);
    }
}