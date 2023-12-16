package net.ua.mixin.otherwise;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.GuardianEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.ElderGuardianEntity;
import net.minecraft.entity.mob.GiantEntity;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.entity.passive.AxolotlEntity;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(net.minecraft.client.render.entity.GuardianEntityRenderer.class)
@Environment(value = EnvType.CLIENT)
public class GuardianEntityRenderer extends MobEntityRenderer<GuardianEntity, GuardianEntityModel> {
    @Final
    @Shadow
    private static final Identifier TEXTURE = new Identifier("textures/entity/guardian.png");
    @Final
    @Shadow
    private static final Identifier EXPLOSION_BEAM_TEXTURE = new Identifier("textures/entity/guardian_beam.png");
    @Final
    @Shadow
    private static final RenderLayer LAYER = RenderLayer.getEntityCutoutNoCull(EXPLOSION_BEAM_TEXTURE);

    public GuardianEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new GuardianEntityModel(context.getPart(EntityModelLayers.GUARDIAN)), 0.5f);
    }
    GuardianEntityRenderer(EntityRendererFactory.Context ctx, float shadowRadius, EntityModelLayer layer) {
        super(ctx, new GuardianEntityModel(ctx.getPart(layer)), shadowRadius);
    }

    @Override
    @Overwrite
    public boolean shouldRender(GuardianEntity guardianEntity, Frustum frustum, double d, double e, double f) {
        LivingEntity livingEntity;
        if (super.shouldRender(guardianEntity, frustum, d, e, f)) {
            return true;
        }
        if (guardianEntity.hasBeamTarget() && (livingEntity = guardianEntity.getBeamTarget()) != null) {
            Vec3d vec3d = this.fromLerpedPosition(livingEntity, (double)livingEntity.getHeight() * 0.5, 1.0f);
            Vec3d vec3d2 = this.fromLerpedPosition(guardianEntity, guardianEntity.getStandingEyeHeight(), 1.0f);
            return frustum.isVisible(new Box(vec3d2.x, vec3d2.y, vec3d2.z, vec3d.x, vec3d.y, vec3d.z));
        }
        return false;
    }

    @Overwrite
    private Vec3d fromLerpedPosition(LivingEntity entity, double yOffset, float delta) {
        double d = MathHelper.lerp(delta, entity.lastRenderX, entity.getX());
        double e = MathHelper.lerp(delta, entity.lastRenderY, entity.getY()) + yOffset;
        double f = MathHelper.lerp(delta, entity.lastRenderZ, entity.getZ());
        return new Vec3d(d, e, f);
    }

    @Override
    @Overwrite
    public void render(GuardianEntity guardianEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        super.render(guardianEntity, f, g, matrixStack, vertexConsumerProvider, i);
        LivingEntity livingEntity = guardianEntity.getBeamTarget();
        if (livingEntity != null) {
            if (guardianEntity instanceof ElderGuardianEntity) {
                livingEntity.getWorld().getOtherEntities(guardianEntity, Box.of(guardianEntity.getPos(), 30, 30, 30)).forEach(e -> {
                    if (!(e instanceof LivingEntity)) return;
                    if (e instanceof PlayerEntity) {
                        if (((PlayerEntity) e).getAbilities().invulnerable) {
                            return;
                        }
                    }
                    if ((e instanceof PlayerEntity || e instanceof SquidEntity || e instanceof AxolotlEntity || e instanceof GiantEntity
                            || e instanceof AbstractSkeletonEntity) && e.squaredDistanceTo(guardianEntity) > 4.0) {
                        float h = guardianEntity.getBeamProgress(g);
                        float j = guardianEntity.getBeamTicks() + g;
                        float k = j * 0.5f % 1.0f;
                        float l = guardianEntity.getStandingEyeHeight();
                        matrixStack.push();
                        matrixStack.translate(0.0f, l, 0.0f);
                        Vec3d vec3d = this.fromLerpedPosition((LivingEntity) e, (double) e.getHeight() * 0.5, g);
                        Vec3d vec3d2 = this.fromLerpedPosition(guardianEntity, l, g);
                        Vec3d vec3d3 = vec3d.subtract(vec3d2);
                        float m = (float) (vec3d3.length() + 1.0);
                        vec3d3 = vec3d3.normalize();
                        float n = (float) Math.acos(vec3d3.y);
                        float o = (float) Math.atan2(vec3d3.z, vec3d3.x);
                        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((1.5707964f - o) * 57.295776f));
                        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(n * 57.295776f));
                        float q = j * 0.05f * -1.5f;
                        float r = h * h;
                        int s = 64 + (int) (r * 191);
                        int t = 32 + (int) (r * 191);
                        int u = 128 - (int) (r * 64);
                        float x = MathHelper.cos(q + 2.3561945f) * 0.282f;
                        float y = MathHelper.sin(q + 2.3561945f) * 0.282f;
                        float z = MathHelper.cos(q + 0.7853982f) * 0.282f;
                        float aa = MathHelper.sin(q + 0.7853982f) * 0.282f;
                        float ab = MathHelper.cos(q + 3.926991f) * 0.282f;
                        float ac = MathHelper.sin(q + 3.926991f) * 0.282f;
                        float ad = MathHelper.cos(q + 5.4977875f) * 0.282f;
                        float ae = MathHelper.sin(q + 5.4977875f) * 0.282f;
                        float af = MathHelper.cos(q + (float) Math.PI) * 0.2f;
                        float ag = MathHelper.sin(q + (float) Math.PI) * 0.2f;
                        float ah = MathHelper.cos(q + 0.0f) * 0.2f;
                        float ai = MathHelper.sin(q + 0.0f) * 0.2f;
                        float aj = MathHelper.cos(q + 1.5707964f) * 0.2f;
                        float ak = MathHelper.sin(q + 1.5707964f) * 0.2f;
                        float al = MathHelper.cos(q + 4.712389f) * 0.2f;
                        float am = MathHelper.sin(q + 4.712389f) * 0.2f;
                        float aq = -1.0f + k;
                        float ar = m * 2.5f + aq;
                        VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(LAYER);
                        MatrixStack.Entry entry = matrixStack.peek();
                        Matrix4f matrix4f = entry.getPositionMatrix();
                        Matrix3f matrix3f = entry.getNormalMatrix();
                        vertex(vertexConsumer, matrix4f, matrix3f, af, m, ag, s, t, u, 0.4999f, ar);
                        vertex(vertexConsumer, matrix4f, matrix3f, af, 0.0f, ag, s, t, u, 0.4999f, aq);
                        vertex(vertexConsumer, matrix4f, matrix3f, ah, 0.0f, ai, s, t, u, 0.0f, aq);
                        vertex(vertexConsumer, matrix4f, matrix3f, ah, m, ai, s, t, u, 0.0f, ar);
                        vertex(vertexConsumer, matrix4f, matrix3f, aj, m, ak, s, t, u, 0.4999f, ar);
                        vertex(vertexConsumer, matrix4f, matrix3f, aj, 0.0f, ak, s, t, u, 0.4999f, aq);
                        vertex(vertexConsumer, matrix4f, matrix3f, al, 0.0f, am, s, t, u, 0.0f, aq);
                        vertex(vertexConsumer, matrix4f, matrix3f, al, m, am, s, t, u, 0.0f, ar);
                        float as = 0.0f;
                        if (guardianEntity.age % 2 == 0) {
                            as = 0.5f;
                        }
                        vertex(vertexConsumer, matrix4f, matrix3f, x, m, y, s, t, u, 0.5f, as + 0.5f);
                        vertex(vertexConsumer, matrix4f, matrix3f, z, m, aa, s, t, u, 1.0f, as + 0.5f);
                        vertex(vertexConsumer, matrix4f, matrix3f, ad, m, ae, s, t, u, 1.0f, as);
                        vertex(vertexConsumer, matrix4f, matrix3f, ab, m, ac, s, t, u, 0.5f, as);
                        matrixStack.pop();
                    }

                });
            } else {
                float h = guardianEntity.getBeamProgress(g);
                float j = guardianEntity.getBeamTicks() + g;
                float k = j * 0.5f % 1.0f;
                float l = guardianEntity.getStandingEyeHeight();
                matrixStack.push();
                matrixStack.translate(0.0f, l, 0.0f);
                Vec3d vec3d = this.fromLerpedPosition(livingEntity, (double)livingEntity.getHeight() * 0.5, g);
                Vec3d vec3d2 = this.fromLerpedPosition(guardianEntity, l, g);
                Vec3d vec3d3 = vec3d.subtract(vec3d2);
                float m = (float)(vec3d3.length() + 1.0);
                vec3d3 = vec3d3.normalize();
                float n = (float)Math.acos(vec3d3.y);
                float o = (float)Math.atan2(vec3d3.z, vec3d3.x);
                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((1.5707964f - o) * 57.295776f));
                matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(n * 57.295776f));
                float q = j * 0.05f * -1.5f;
                float r = h * h;
                int s = 64 + (int)(r * 191);
                int t = 32 + (int)(r * 191);
                int u = 128 - (int)(r * 64.0f);
                float x = MathHelper.cos(q + 2.3561945f) * 0.282f;
                float y = MathHelper.sin(q + 2.3561945f) * 0.282f;
                float z = MathHelper.cos(q + 0.7853982f) * 0.282f;
                float aa = MathHelper.sin(q + 0.7853982f) * 0.282f;
                float ab = MathHelper.cos(q + 3.926991f) * 0.282f;
                float ac = MathHelper.sin(q + 3.926991f) * 0.282f;
                float ad = MathHelper.cos(q + 5.4977875f) * 0.282f;
                float ae = MathHelper.sin(q + 5.4977875f) * 0.282f;
                float af = MathHelper.cos(q + (float)Math.PI) * 0.2f;
                float ag = MathHelper.sin(q + (float)Math.PI) * 0.2f;
                float ah = MathHelper.cos(q + 0.0f) * 0.2f;
                float ai = MathHelper.sin(q + 0.0f) * 0.2f;
                float aj = MathHelper.cos(q + 1.5707964f) * 0.2f;
                float ak = MathHelper.sin(q + 1.5707964f) * 0.2f;
                float al = MathHelper.cos(q + 4.712389f) * 0.2f;
                float am = MathHelper.sin(q + 4.712389f) * 0.2f;
                float aq = -1.0f + k;
                float ar = m * 2.5f + aq;
                VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(LAYER);
                MatrixStack.Entry entry = matrixStack.peek();
                Matrix4f matrix4f = entry.getPositionMatrix();
                Matrix3f matrix3f = entry.getNormalMatrix();
                vertex(vertexConsumer, matrix4f, matrix3f, af, m, ag, s, t, u, 0.4999f, ar);
                vertex(vertexConsumer, matrix4f, matrix3f, af, 0.0f, ag, s, t, u, 0.4999f, aq);
                vertex(vertexConsumer, matrix4f, matrix3f, ah, 0.0f, ai, s, t, u, 0.0f, aq);
                vertex(vertexConsumer, matrix4f, matrix3f, ah, m, ai, s, t, u, 0.0f, ar);
                vertex(vertexConsumer, matrix4f, matrix3f, aj, m, ak, s, t, u, 0.4999f, ar);
                vertex(vertexConsumer, matrix4f, matrix3f, aj, 0.0f, ak, s, t, u, 0.4999f, aq);
                vertex(vertexConsumer, matrix4f, matrix3f, al, 0.0f, am, s, t, u, 0.0f, aq);
                vertex(vertexConsumer, matrix4f, matrix3f, al, m, am, s, t, u, 0.0f, ar);
                float as = 0.0f;
                if (guardianEntity.age % 2 == 0) {
                    as = 0.5f;
                }
                vertex(vertexConsumer, matrix4f, matrix3f, x, m, y, s, t, u, 0.5f, as + 0.5f);
                vertex(vertexConsumer, matrix4f, matrix3f, z, m, aa, s, t, u, 1.0f, as + 0.5f);
                vertex(vertexConsumer, matrix4f, matrix3f, ad, m, ae, s, t, u, 1.0f, as);
                vertex(vertexConsumer, matrix4f, matrix3f, ab, m, ac, s, t, u, 0.5f, as);
                matrixStack.pop();
            }
        }
    }

    @Overwrite
    private static void vertex(VertexConsumer vertexConsumer, Matrix4f positionMatrix, Matrix3f normalMatrix, float x, float y, float z, int red, int green, int blue, float u, float v) {
        vertexConsumer.vertex(positionMatrix, x, y, z).color(red, green, blue, 255).texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(normalMatrix, 0.0f, 1.0f, 0.0f).next();
    }

    @Override
    @Overwrite
    public Identifier getTexture(GuardianEntity guardianEntity) {
        return TEXTURE;
    }
}
