package ru.rubin.module.impl.visuals.richi;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class RichiModel {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart neck;
    private final ModelPart body;
    private final ModelPart frontLeftLeg;
    private final ModelPart frontRightLeg;
    private final ModelPart leftBackLeg;
    private final ModelPart rightBackLeg;
    private final ModelPart tail;

    public RichiModel() {
        this.root = createTexturedModelData().createModel();
        this.head = root.getChild("head");
        this.leftEar = head.getChild("left_ear");
        this.rightEar = head.getChild("right_ear");
        this.neck = root.getChild("neck");
        this.body = root.getChild("body");
        this.frontLeftLeg = root.getChild("front_left_leg");
        this.frontRightLeg = root.getChild("front_right_leg");
        this.leftBackLeg = root.getChild("left_back_leg");
        this.rightBackLeg = root.getChild("right_back_leg");
        this.tail = root.getChild("tail");
    }

    private static TexturedModelData createTexturedModelData() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();

        ModelPartData head = root.addChild("head",
                ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-3.0f, -3.0f, -4.0f, 6.0f, 6.0f, 4.0f)
                        .uv(21, 0).cuboid(-1.5f, 0.0f, -7.0f, 3.0f, 3.0f, 3.0f),
                ModelTransform.origin(0.0f, 10.5f, -6.8f)
        );

        head.addChild("left_ear",
                ModelPartBuilder.create()
                        .uv(32, 4).cuboid(0.0f, -5.0f, -1.5f, 1.0f, 3.0f, 3.0f)
                        .uv(34, 1).cuboid(0.0f, -5.5f, -0.75f, 1.0f, 1.0f, 1.5f),
                ModelTransform.origin(3.0f, 3.0f, -2.0f)
        );

        head.addChild("right_ear",
                ModelPartBuilder.create()
                        .uv(32, 4).cuboid(-1.0f, -5.0f, -1.5f, 1.0f, 3.0f, 3.0f)
                        .uv(34, 1).cuboid(-1.0f, -5.5f, -0.75f, 1.0f, 1.0f, 1.5f),
                ModelTransform.origin(-3.0f, 3.0f, -2.0f)
        );

        root.addChild("neck",
                ModelPartBuilder.create().uv(15, 7).cuboid(-2.95f, -1.0f, -4.0f, 5.9f, 5.0f, 6.0f),
                ModelTransform.of(0.0f, 10.5f, -5.0f, (float) Math.toRadians(-25.0), 0.0f, 0.0f)
        );

        ModelPartData body = root.addChild("body",
                ModelPartBuilder.create(),
                ModelTransform.origin(0.0f, 13.5f, -5.0f)
        );

        body.addChild("chest",
                ModelPartBuilder.create().uv(32, 13).cuboid(-4.0f, -3.5f, -3.0f, 8.0f, 7.0f, 6.0f),
                ModelTransform.origin(0.0f, 0.0f, 3.0f)
        );

        body.addChild("back",
                ModelPartBuilder.create().uv(3, 19).cuboid(-3.0f, -3.0f, -0.5f, 6.0f, 6.0f, 11.0f),
                ModelTransform.origin(0.0f, -0.5f, 5.5f)
        );

        root.addChild("front_left_leg",
                ModelPartBuilder.create().uv(42, 0).cuboid(-1.0f, 0.0f, -1.0f, 2.0f, 5.0f, 2.0f),
                ModelTransform.origin(1.5f, 16.0f, -3.0f)
        );

        root.addChild("front_right_leg",
                ModelPartBuilder.create().uv(42, 0).mirrored().cuboid(-1.0f, 0.0f, -1.0f, 2.0f, 5.0f, 2.0f).mirrored(false),
                ModelTransform.origin(-1.5f, 16.0f, -3.0f)
        );

        root.addChild("left_back_leg",
                ModelPartBuilder.create().uv(52, 0).cuboid(-1.0f, 0.0f, -1.0f, 2.0f, 5.0f, 2.0f),
                ModelTransform.origin(1.5f, 16.0f, 9.0f)
        );

        root.addChild("right_back_leg",
                ModelPartBuilder.create().uv(52, 0).mirrored().cuboid(-1.0f, 0.0f, -1.0f, 2.0f, 5.0f, 2.0f).mirrored(false),
                ModelTransform.origin(-1.5f, 16.0f, 9.0f)
        );

        root.addChild("tail",
                ModelPartBuilder.create().uv(2, 12).cuboid(-1.0f, 2.0f, -1.0f, 2.0f, 8.0f, 2.0f),
                ModelTransform.of(0.0f, 9.0f, 10.0f, (float) Math.toRadians(22.5), 0.0f, 0.0f)
        );

        return TexturedModelData.of(data, 60, 36);
    }

    public void setAngles(float ageInTicks, RichiBrain brain) {
        if (brain == null) return;

        head.yaw = (float) Math.toRadians(brain.getHeadYawOffset());
        head.pitch = (float) Math.toRadians(brain.getPitch());

        float limbSwing = brain.limbSwing;
        float limbSwingAmount = brain.limbSwingAmount;

        frontLeftLeg.pitch = (float) Math.cos(limbSwing * 0.6662f) * 1.4f * limbSwingAmount;
        frontRightLeg.pitch = (float) Math.cos(limbSwing * 0.6662f + Math.PI) * 1.4f * limbSwingAmount;
        leftBackLeg.pitch = (float) Math.cos(limbSwing * 0.6662f + Math.PI) * 1.4f * limbSwingAmount;
        rightBackLeg.pitch = (float) Math.cos(limbSwing * 0.6662f) * 1.4f * limbSwingAmount;

        if (brain.isLay()) {
            frontLeftLeg.pitch = (float) Math.toRadians(-90);
            frontRightLeg.pitch = (float) Math.toRadians(-90);
            leftBackLeg.pitch = (float) Math.toRadians(90);
            rightBackLeg.pitch = (float) Math.toRadians(90);

            frontLeftLeg.yaw = (float) Math.toRadians(-22);
            frontRightLeg.yaw = (float) Math.toRadians(22);
            leftBackLeg.yaw = (float) Math.toRadians(22);
            rightBackLeg.yaw = (float) Math.toRadians(-22);
        } else {
            frontLeftLeg.yaw = 0.0f;
            frontRightLeg.yaw = 0.0f;
            leftBackLeg.yaw = 0.0f;
            rightBackLeg.yaw = 0.0f;
        }

        tail.pitch = (float) Math.toRadians(brain.isLay() ? 45 : 22);
        tail.yaw = 0.0f;
        tail.roll = (float) Math.cos(ageInTicks * 0.15f) * 0.3f;

        leftEar.roll = 0.0f;
        rightEar.roll = 0.0f;
        neck.yaw = 0.0f;
        body.yaw = 0.0f;
    }

    public void render(MatrixStack matrices, VertexConsumerProvider.Immediate consumers, int light, Identifier texture) {
        Identifier finalTexture = texture == null
                ? Identifier.of("minecraft", "textures/entity/wolf/wolf.png") : texture;
        VertexConsumer buffer = consumers.getBuffer(RenderLayers.entityTranslucent(finalTexture));
        root.render(matrices, buffer, light, OverlayTexture.DEFAULT_UV);
    }
}
