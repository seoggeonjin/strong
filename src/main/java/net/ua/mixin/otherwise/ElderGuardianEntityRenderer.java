package net.ua.mixin.otherwise;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.ElderGuardianEntity;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(net.minecraft.client.render.entity.ElderGuardianEntityRenderer.class)
@Environment(value=EnvType.CLIENT)
public class ElderGuardianEntityRenderer extends GuardianEntityRenderer {
    @Final
    @Shadow
    public static final Identifier TEXTURE = new Identifier("textures/entity/guardian_elder.png");

    public ElderGuardianEntityRenderer(EntityRendererFactory.Context context) {
        super(context, 1.2f, EntityModelLayers.ELDER_GUARDIAN);
    }

    @Overwrite
    public void scale(GuardianEntity guardianEntity, MatrixStack matrixStack, float f) {
        matrixStack.scale(ElderGuardianEntity.SCALE, ElderGuardianEntity.SCALE, ElderGuardianEntity.SCALE);
    }

    @Overwrite
    public Identifier getTexture(GuardianEntity guardianEntity) {
        return TEXTURE;
    }
}