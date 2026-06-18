package uk.co.cablepost.ad_astra_cargo_rockets.cargo_rocket;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import uk.co.cablepost.ad_astra_cargo_rockets.AdAstraCargoRockets;

public class CargoRocketEntityRenderer extends EntityRenderer<CargoRocketEntity> {

    private final T1CargoRocketEntityModel<CargoRocketEntity> model;

    public CargoRocketEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new T1CargoRocketEntityModel<>(
                context.bakeLayer(T1CargoRocketEntityModel.LAYER_LOCATION));
    }

    @Override
    public ResourceLocation getTextureLocation(CargoRocketEntity entity) {
        int tier = entity.getTier();
        String path = switch (tier) {
            case 1 -> "textures/entity/cargo_rocket/t1.png";
            case 2 -> "textures/entity/cargo_rocket/t2.png";
            case 3 -> "textures/entity/cargo_rocket/t3.png";
            case 4 -> "textures/entity/cargo_rocket/t4.png";
            default -> "textures/entity/cargo_rocket/t0.png";
        };
        return new ResourceLocation(AdAstraCargoRockets.MOD_ID, path);
    }

    @Override
    public boolean shouldRender(CargoRocketEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double camX, double camY, double camZ) {
        // モデルの実寸がバウンディングボックスより大きいためカリング判定を無効化
        return true;
    }

    @Override
    public void render(CargoRocketEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutout(getTextureLocation(entity)));
        model.renderToBuffer(poseStack, vc, packedLight,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                1f, 1f, 1f, 1f);
    }
}
