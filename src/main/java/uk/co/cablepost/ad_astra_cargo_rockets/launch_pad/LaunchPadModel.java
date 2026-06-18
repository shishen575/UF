package uk.co.cablepost.ad_astra_cargo_rockets.launch_pad;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import uk.co.cablepost.ad_astra_cargo_rockets.AdAstraCargoRockets;

public class LaunchPadModel extends Model {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(AdAstraCargoRockets.MOD_ID, "launch_pad"), "main");

    private final ModelPart base;
    private final ModelPart mainBody;
    private final ModelPart topPlatform;

    public LaunchPadModel(ModelPart root) {
        super(RenderType::entitySolid);
        this.base        = root.getChild("base");
        this.mainBody    = this.base.getChild("main_body");
        this.topPlatform = this.base.getChild("top_platform");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition rootPart = mesh.getRoot();

        PartDefinition base = rootPart.addOrReplaceChild("base",
                CubeListBuilder.create(), PartPose.offset(0f, 24f, 0f));

        // Main body: from [-16,0,-16] to [32,13,32] → size 48x13x48
        base.addOrReplaceChild("main_body",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-16f, -24f, -16f, 48, 13, 48, new CubeDeformation(0f)),
                PartPose.offset(0f, 0f, 0f));

        // Top platform: from [-15,13,-15] to [31,15,31] → size 46x2x46
        base.addOrReplaceChild("top_platform",
                CubeListBuilder.create()
                        .texOffs(0, 61).addBox(-15f, -11f, -15f, 46, 2, 46, new CubeDeformation(0f)),
                PartPose.offset(0f, 0f, 0f));

        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer,
                                int packedLight, int packedOverlay,
                                float red, float green, float blue, float alpha) {
        base.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
