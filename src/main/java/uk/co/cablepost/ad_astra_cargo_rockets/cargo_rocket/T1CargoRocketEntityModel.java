package uk.co.cablepost.ad_astra_cargo_rockets.cargo_rocket;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import uk.co.cablepost.ad_astra_cargo_rockets.AdAstraCargoRockets;

public class T1CargoRocketEntityModel<T extends CargoRocketEntity> extends EntityModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(AdAstraCargoRockets.MOD_ID, "cargo_rocket"), "main");

    private final ModelPart rocketbody;
    private final ModelPart rockethead;
    private final ModelPart rings;
    private final ModelPart rings2;
    private final ModelPart bb_main;

    public T1CargoRocketEntityModel(ModelPart root) {
        this.rocketbody = root.getChild("rocketbody");
        this.rockethead = root.getChild("rockethead");
        this.rings      = root.getChild("rings");
        this.rings2     = root.getChild("rings2");
        this.bb_main    = root.getChild("bb_main");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition rocketbody = root.addOrReplaceChild("rocketbody",
            CubeListBuilder.create()
                .texOffs(99, 32).addBox(-1f, -3f, -1f, 2, 3, 3, new CubeDeformation(0))
                .texOffs(99, 0).addBox(-3f, 0f, 20f, 6, 9, 4, new CubeDeformation(0))
                .texOffs(99, 13).addBox(-3f, 0f, -2f, 6, 9, 4, new CubeDeformation(0))
                .texOffs(99, 26).addBox(-1f, -3f, 20f, 2, 3, 3, new CubeDeformation(0))
                .texOffs(0, 0).addBox(-9f, -34f, 2f, 18, 44, 18, new CubeDeformation(0))
                .texOffs(0, 83).addBox(-6f, 10f, 5f, 12, 3, 12, new CubeDeformation(0))
                .texOffs(1, 63).addBox(-8f, 13f, 3f, 16, 4, 16, new CubeDeformation(0)),
            PartPose.offset(0f, 7f, -11f));

        rocketbody.addOrReplaceChild("cube_r1", CubeListBuilder.create().mirror()
            .texOffs(82, 40).addBox(-6f, -3.5f, -1f, 16, 9, 2, new CubeDeformation(0)),
            PartPose.offsetAndRotation(9.0503f, 3.5f, 20.0502f, -0.3295f, -0.7268f, 0.4754f));
        rocketbody.addOrReplaceChild("cube_r2", CubeListBuilder.create().mirror()
            .texOffs(82, 40).addBox(-8f, -4.5f, -1f, 16, 9, 2, new CubeDeformation(0)),
            PartPose.offsetAndRotation(10.1373f, 5.1237f, 1.1373f, 0.3295f, 0.7268f, 0.4754f));
        rocketbody.addOrReplaceChild("cube_r3", CubeListBuilder.create()
            .texOffs(82, 40).addBox(-8f, -4.5f, -1f, 16, 9, 2, new CubeDeformation(0)),
            PartPose.offsetAndRotation(-10.1373f, 5.1237f, 1.1373f, 0.3295f, -0.7268f, -0.4754f));
        rocketbody.addOrReplaceChild("cube_r4", CubeListBuilder.create()
            .texOffs(82, 40).addBox(-10f, -3.5f, -1f, 16, 9, 2, new CubeDeformation(0)),
            PartPose.offsetAndRotation(-9.0503f, 3.5f, 20.0502f, -0.3295f, 0.7268f, -0.4754f));
        rocketbody.addOrReplaceChild("cube_r5", CubeListBuilder.create().mirror()
            .texOffs(83, 0).addBox(-2f, -8f, -2f, 4, 16, 4, new CubeDeformation(0)),
            PartPose.offsetAndRotation(-16f, 9f, -4f, 0f, 0.7854f, 0f));
        rocketbody.addOrReplaceChild("cube_r6", CubeListBuilder.create().mirror()
            .texOffs(83, 0).addBox(-2f, -8f, -2f, 4, 16, 4, new CubeDeformation(0)),
            PartPose.offsetAndRotation(-16f, 9f, 27f, 0f, 0.7854f, 0f));
        rocketbody.addOrReplaceChild("cube_r7", CubeListBuilder.create()
            .texOffs(83, 0).addBox(-2f, -8f, -2f, 4, 16, 4, new CubeDeformation(0)),
            PartPose.offsetAndRotation(16f, 9f, 27f, 0f, -0.7854f, 0f));
        rocketbody.addOrReplaceChild("cube_r8", CubeListBuilder.create()
            .texOffs(83, 0).addBox(-2f, -8f, -2f, 4, 16, 4, new CubeDeformation(0)),
            PartPose.offsetAndRotation(16f, 9f, -4f, 0f, -0.7854f, 0f));

        PartDefinition rockethead = root.addOrReplaceChild("rockethead",
            CubeListBuilder.create()
                .texOffs(104, 114).addBox(-12f, -24f, -3f, 6, 8, 6, new CubeDeformation(0))
                .texOffs(88, 122).addBox(-11f, -26f, -2f, 4, 2, 4, new CubeDeformation(0))
                .texOffs(80, 113).addBox(-10f, -37f, -1f, 2, 13, 2, new CubeDeformation(0))
                .texOffs(88, 114).addBox(-11f, -40f, -2f, 4, 4, 4, new CubeDeformation(0)),
            PartPose.offset(9f, -27f, 0f));

        rockethead.addOrReplaceChild("cube_r9", CubeListBuilder.create().mirror()
            .texOffs(0, 102).addBox(-1f, -24f, -1f, 2, 24, 2, new CubeDeformation(0)),
            PartPose.offsetAndRotation(-18f, 0f, -9f, -0.48f, 0.7854f, 0f));
        rockethead.addOrReplaceChild("cube_r10", CubeListBuilder.create().mirror()
            .texOffs(0, 102).addBox(-1f, -24f, -1f, 2, 24, 2, new CubeDeformation(0)),
            PartPose.offsetAndRotation(-18f, 0f, 9f, 0.4326f, 0.678f, 0.6346f));
        rockethead.addOrReplaceChild("cube_r11", CubeListBuilder.create()
            .texOffs(0, 102).addBox(-1f, -24f, -1f, 2, 24, 2, new CubeDeformation(0)),
            PartPose.offsetAndRotation(0f, 0f, 9f, 0.4326f, -0.678f, -0.6346f));
        rockethead.addOrReplaceChild("cube_r12", CubeListBuilder.create()
            .texOffs(0, 102).addBox(-1f, -24f, -1f, 2, 24, 2, new CubeDeformation(0)),
            PartPose.offsetAndRotation(0f, 0f, -9f, -0.48f, -0.7854f, 0f));
        rockethead.addOrReplaceChild("cube_r13", CubeListBuilder.create()
            .texOffs(83, 68).addBox(-8f, -24f, 0f, 16, 24, 0, new CubeDeformation(0)),
            PartPose.offsetAndRotation(-9f, 0f, 9f, 2.7925f, 0f, -3.1416f));
        rockethead.addOrReplaceChild("cube_r14", CubeListBuilder.create()
            .texOffs(83, 68).addBox(-8f, -24f, 0f, 16, 24, 0, new CubeDeformation(0)),
            PartPose.offsetAndRotation(-9f, 0f, -9f, 2.7925f, 3.1416f, 3.1416f));
        rockethead.addOrReplaceChild("cube_r15", CubeListBuilder.create()
            .texOffs(83, 68).addBox(-8f, -24f, 0f, 16, 24, 0, new CubeDeformation(0)),
            PartPose.offsetAndRotation(-18f, 0f, 0f, 0f, 1.5708f, 0.3491f));
        rockethead.addOrReplaceChild("cube_r16", CubeListBuilder.create()
            .texOffs(83, 68).addBox(-8f, -24f, 0f, 16, 24, 0, new CubeDeformation(0)),
            PartPose.offsetAndRotation(0f, 0f, 0f, 0f, -1.5708f, -0.3491f));

        PartDefinition rings3 = rockethead.addOrReplaceChild("rings3",
            CubeListBuilder.create(), PartPose.offset(0.5f, 0f, 0f));
        rings3.addOrReplaceChild("cube_r17", CubeListBuilder.create()
            .texOffs(19, 121).addBox(-10.5f, -1f, 0f, 21, 2, 0, new CubeDeformation(0)),
            PartPose.offsetAndRotation(-9.499f, 0f, -10.5f, 0f, 3.1416f, 0f));
        rings3.addOrReplaceChild("cube_r18", CubeListBuilder.create()
            .texOffs(19, 121).addBox(-10.5f, -1f, 0f, 21, 2, 0, new CubeDeformation(0)),
            PartPose.offsetAndRotation(-9.499f, 0f, 10.5f, 0f, 3.1416f, 0f));
        rings3.addOrReplaceChild("cube_r19", CubeListBuilder.create()
            .texOffs(19, 121).addBox(-10.5f, -1f, 0f, 21, 2, 0, new CubeDeformation(0)),
            PartPose.offsetAndRotation(-20f, 0f, 0f, 0f, 1.5708f, 0f));
        rings3.addOrReplaceChild("cube_r20", CubeListBuilder.create()
            .texOffs(19, 121).addBox(-10.5f, -1f, 0f, 21, 2, 0, new CubeDeformation(0)),
            PartPose.offsetAndRotation(1f, 0f, 0f, 0f, 1.5708f, 0f));

        PartDefinition rings = root.addOrReplaceChild("rings",
            CubeListBuilder.create(), PartPose.offset(9.5f, 13f, 0f));
        rings.addOrReplaceChild("cube_r21", CubeListBuilder.create()
            .texOffs(21, 115).addBox(-9.5f, -1f, 0f, 19, 2, 0, new CubeDeformation(0)),
            PartPose.offsetAndRotation(-9.499f, 0f, -9.5f, 0f, 3.1416f, 0f));
        rings.addOrReplaceChild("cube_r22", CubeListBuilder.create()
            .texOffs(21, 115).addBox(-9.5f, -1f, 0f, 19, 2, 0, new CubeDeformation(0)),
            PartPose.offsetAndRotation(-9.499f, 0f, 9.5f, 0f, 3.1416f, 0f));
        rings.addOrReplaceChild("cube_r23", CubeListBuilder.create()
            .texOffs(21, 115).addBox(-9.5f, -1f, 0f, 19, 2, 0, new CubeDeformation(0)),
            PartPose.offsetAndRotation(-19f, 0f, 0f, 0f, 1.5708f, 0f));
        rings.addOrReplaceChild("cube_r24", CubeListBuilder.create()
            .texOffs(21, 115).addBox(-9.5f, -1f, 0f, 19, 2, 0, new CubeDeformation(0)),
            PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 1.5708f, 0f));

        PartDefinition rings2 = root.addOrReplaceChild("rings2",
            CubeListBuilder.create(), PartPose.offset(9.5f, 10f, 0f));
        rings2.addOrReplaceChild("cube_r25", CubeListBuilder.create()
            .texOffs(22, 118).addBox(-9.5f, -1f, 0f, 19, 2, 0, new CubeDeformation(0)),
            PartPose.offsetAndRotation(-9.499f, 0f, -9.5f, 0f, 3.1416f, 0f));
        rings2.addOrReplaceChild("cube_r26", CubeListBuilder.create()
            .texOffs(22, 118).addBox(-9.5f, -1f, 0f, 19, 2, 0, new CubeDeformation(0)),
            PartPose.offsetAndRotation(-9.499f, 0f, 9.5f, 0f, 3.1416f, 0f));
        rings2.addOrReplaceChild("cube_r27", CubeListBuilder.create()
            .texOffs(22, 118).addBox(-9.5f, -1f, 0f, 19, 2, 0, new CubeDeformation(0)),
            PartPose.offsetAndRotation(-19f, 0f, 0f, 0f, 1.5708f, 0f));
        rings2.addOrReplaceChild("cube_r28", CubeListBuilder.create()
            .texOffs(22, 118).addBox(-9.5f, -1f, 0f, 19, 2, 0, new CubeDeformation(0)),
            PartPose.offsetAndRotation(0f, 0f, 0f, 0f, 1.5708f, 0f));

        root.addOrReplaceChild("bb_main",
            CubeListBuilder.create()
                .texOffs(100, 52).addBox(-4f, -40f, -10f, 8, 8, 2, new CubeDeformation(0)),
            PartPose.offset(0f, 24f, 0f));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {}

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                                float red, float green, float blue, float alpha) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(180f));
        poseStack.translate(0f, -1.5f, 0f);

        rocketbody.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        rockethead.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        rings.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        rings2.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        bb_main.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);

        poseStack.popPose();
    }
}
