package ru.rubin.module.impl.visuals.richi;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import ru.rubin.event.EventInit;
import ru.rubin.event.lifecycle.ClientTickEvent;
import ru.rubin.event.render.WorldRenderEvent;
import ru.rubin.module.api.Category;
import ru.rubin.module.api.IModule;
import ru.rubin.module.api.Module;
import ru.rubin.module.api.setting.Setting;
import ru.rubin.module.api.setting.impl.ModeSetting;

@IModule(name = "RichiDog", description = "Собака-компаньон рядом с игроком", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class RichiDog extends Module {
    private static final Identifier DJEKRUSSEL_TEXTURE = Identifier.of("rubin", "textures/djekrussel.png");
    private static final Identifier TAKSA_TEXTURE = Identifier.of("rubin", "textures/taksapet.png");

    private final RichiModel model = new RichiModel();
    private final RichiBrain brain = new RichiBrain();

    public static ModeSetting textureMode = new ModeSetting("Текстура", "Taksa", "Djekrussel", "Taksa");

    public RichiDog() {
        this.addSettings(new Setting[]{textureMode});
    }

    @Override
    public void onDisable() {
        super.onDisable();
        brain.setEntity(null);
    }

    @EventInit
    public void onTick(ClientTickEvent event) {
        MinecraftClient client = mc;
        if (!this.enable || client == null || client.player == null || client.world == null) return;
        brain.setEntity(client.player);
        brain.tick(client);
    }

    @EventInit
    public void onWorldRender(WorldRenderEvent event) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!this.enable || mc.player == null || mc.world == null) return;

        float tickDelta = event.worldRenderer().tickDelta();
        Camera camera = event.worldRenderer().camera();
        Vec3d cameraPos = camera.getCameraPos();
        Matrix4f positionMatrix = event.positionMatrix();

        if (positionMatrix == null) return;

        Vec3d dogPos = brain.getPos(tickDelta);
        if (dogPos == null) return;

        MatrixStack matrices = new MatrixStack();
        matrices.multiplyPositionMatrix(positionMatrix);
        matrices.push();
        matrices.translate(dogPos.x - cameraPos.x, dogPos.y - cameraPos.y, dogPos.z - cameraPos.z);
        matrices.translate(0.0, 1.2 - (brain.isLay() ? 0.3 : 0.0), 0.0);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180.0f));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(brain.getBodyYaw()));

        model.setAngles(mc.player.age + tickDelta, brain);

        VertexConsumerProvider.Immediate consumers = mc.getBufferBuilders().getEntityVertexConsumers();
        model.render(matrices, consumers, LightmapTextureManager.MAX_LIGHT_COORDINATE, resolveTexture());
        consumers.draw();

        matrices.pop();
    }

    private Identifier resolveTexture() {
        if (textureMode.is("Djekrussel")) {
            return DJEKRUSSEL_TEXTURE;
        }
        return TAKSA_TEXTURE;
    }
}
