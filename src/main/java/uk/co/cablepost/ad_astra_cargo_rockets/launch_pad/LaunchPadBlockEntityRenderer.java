package uk.co.cablepost.ad_astra_cargo_rockets.launch_pad;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import uk.co.cablepost.ad_astra_cargo_rockets.AdAstraCargoRockets;

public class LaunchPadBlockEntityRenderer implements BlockEntityRenderer<LaunchPadBlockEntity> {

    final LaunchPadModel launchPadModel;

    public LaunchPadBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
        launchPadModel = new LaunchPadModel(ctx.bakeLayer(LaunchPadModel.LAYER_LOCATION));
    }

    @Override
    public boolean shouldRenderOffScreen(LaunchPadBlockEntity be) { return true; }

    @Override
    public int getViewDistance() { return 256; }

    @Override
    public void render(LaunchPadBlockEntity blockEntity, float tickDelta,
                       PoseStack matrices, MultiBufferSource buffers,
                       int light, int overlay) {
        matrices.pushPose();
        matrices.translate(0.5, 2.0, 0.5);
        matrices.translate(-0.5, -2.0, -0.5);
        // 180度回転 + PI回転 (元コードと同じ)
        matrices.mulPose(Axis.XP.rotationDegrees(180.0F));
        matrices.mulPose(Axis.XP.rotation((float) Math.PI));

        var vertexConsumer = buffers.getBuffer(RenderType.entityCutout(
                new ResourceLocation(AdAstraCargoRockets.MOD_ID, "textures/block/launch_pad.png")));
        launchPadModel.renderToBuffer(matrices, vertexConsumer, light, overlay, 1f, 1f, 1f, 1f);

        matrices.popPose();
    }
}
