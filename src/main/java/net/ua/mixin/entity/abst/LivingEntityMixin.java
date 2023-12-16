package net.ua.mixin.entity.abst;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Colors;
import net.minecraft.world.World;
import net.ua.ips.Pi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class LivingEntityMixin extends LivingEntity implements Pi {
    protected LivingEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }
    @Overwrite
    public boolean damage(DamageSource source, float amount) {
        return ((PlayerEntity) (Object) this).damage(source, amount);
    }
    @Inject(method = "tick", at = @At("RETURN"))
    public void tick(CallbackInfo ci) {
    }
}
