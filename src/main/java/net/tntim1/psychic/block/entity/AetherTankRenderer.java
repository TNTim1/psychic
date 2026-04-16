package net.tntim1.psychic.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import org.joml.Matrix4f;

public class AetherTankRenderer implements BlockEntityRenderer<AetherTankBlockEntity> {

    private int tickCount = 0; // throttle prints to once per 60 frames

    public AetherTankRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(AetherTankBlockEntity be, float partialTicks, PoseStack pose, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        tickCount++;
        boolean shouldLog = (tickCount % 60 == 0);

        FluidStack fluidStack = be.getTank().getFluid();

        if (shouldLog) {
            System.out.println("[AetherTankRenderer] === RENDER CALLED ===");
            System.out.println("[AetherTankRenderer] FluidStack empty: " + fluidStack.isEmpty());
            System.out.println("[AetherTankRenderer] FluidStack: " + fluidStack);
            System.out.println("[AetherTankRenderer] Tank capacity: " + be.getTank().getCapacity());
            System.out.println("[AetherTankRenderer] Tank amount: " + be.getTank().getFluidAmount());
        }

        if (fluidStack.isEmpty()) {
            if (shouldLog) System.out.println("[AetherTankRenderer] EARLY EXIT: fluid is empty");
            return;
        }

        float fill = be.getTank().getFillFraction();

        if (shouldLog) {
            System.out.println("[AetherTankRenderer] Fill fraction: " + fill);
        }

        if (fill <= 0) {
            if (shouldLog) System.out.println("[AetherTankRenderer] EARLY EXIT: fill <= 0");
            return;
        }

        IClientFluidTypeExtensions fluidExt = IClientFluidTypeExtensions.of(fluidStack.getFluid());
        ResourceLocation textureLocation = fluidExt.getStillTexture(fluidStack);

        if (shouldLog) {
            System.out.println("[AetherTankRenderer] Texture location: " + textureLocation);
        }

        if (textureLocation == null) {
            if (shouldLog) System.out.println("[AetherTankRenderer] EARLY EXIT: textureLocation is null");
            return;
        }

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(textureLocation);

        if (shouldLog) {
            System.out.println("[AetherTankRenderer] Sprite: " + sprite);
            System.out.println("[AetherTankRenderer] Sprite UV: u0=" + sprite.getU0() + " u1=" + sprite.getU1() + " v0=" + sprite.getV0() + " v1=" + sprite.getV1());
        }

        int color = fluidExt.getTintColor(fluidStack);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;
        if (a == 0) a = 255;

        if (shouldLog) {
            System.out.println("[AetherTankRenderer] Raw color int: " + Integer.toHexString(color));
            System.out.println("[AetherTankRenderer] RGBA: r=" + r + " g=" + g + " b=" + b + " a=" + a);
        }

        float u0 = sprite.getU0(), u1 = sprite.getU1();
        float v0 = sprite.getV0(), v1 = sprite.getV1();

        float x1 = 3.0f / 16f, x2 = 13.0f / 16f;
        float z1 = 3.0f / 16f, z2 = 13.0f / 16f;
        float yBottom = 2.1f / 16f;
        float yTop = yBottom + fill * (13.8f / 16f - yBottom);

        if (shouldLog) {
            System.out.println("[AetherTankRenderer] yBottom=" + yBottom + " yTop=" + yTop);
            System.out.println("[AetherTankRenderer] x1=" + x1 + " x2=" + x2 + " z1=" + z1 + " z2=" + z2);
        }

        int light = 15728880;
        Matrix4f m = pose.last().pose();
        VertexConsumer vc = buffer.getBuffer(RenderType.translucent());

        if (shouldLog) {
            System.out.println("[AetherTankRenderer] About to draw vertices...");
        }

        // TOP
        vc.vertex(m, x1,yTop,z1).color(r,g,b,a).uv(u0,v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0,1,0).endVertex();
        vc.vertex(m, x1,yTop,z2).color(r,g,b,a).uv(u0,v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0,1,0).endVertex();
        vc.vertex(m, x2,yTop,z2).color(r,g,b,a).uv(u1,v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0,1,0).endVertex();
        vc.vertex(m, x2,yTop,z1).color(r,g,b,a).uv(u1,v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0,1,0).endVertex();

        // BOTTOM
        vc.vertex(m, x1,yBottom,z2).color(r,g,b,a).uv(u0,v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0,-1,0).endVertex();
        vc.vertex(m, x1,yBottom,z1).color(r,g,b,a).uv(u0,v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0,-1,0).endVertex();
        vc.vertex(m, x2,yBottom,z1).color(r,g,b,a).uv(u1,v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0,-1,0).endVertex();
        vc.vertex(m, x2,yBottom,z2).color(r,g,b,a).uv(u1,v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0,-1,0).endVertex();

        // NORTH
        vc.vertex(m, x2,yTop,z1).color(r,g,b,a).uv(u0,v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0,0,-1).endVertex();
        vc.vertex(m, x2,yBottom,z1).color(r,g,b,a).uv(u0,v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0,0,-1).endVertex();
        vc.vertex(m, x1,yBottom,z1).color(r,g,b,a).uv(u1,v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0,0,-1).endVertex();
        vc.vertex(m, x1,yTop,z1).color(r,g,b,a).uv(u1,v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0,0,-1).endVertex();

        // SOUTH
        vc.vertex(m, x1,yTop,z2).color(r,g,b,a).uv(u0,v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0,0,1).endVertex();
        vc.vertex(m, x1,yBottom,z2).color(r,g,b,a).uv(u0,v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0,0,1).endVertex();
        vc.vertex(m, x2,yBottom,z2).color(r,g,b,a).uv(u1,v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0,0,1).endVertex();
        vc.vertex(m, x2,yTop,z2).color(r,g,b,a).uv(u1,v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0,0,1).endVertex();

        // WEST
        vc.vertex(m, x1,yTop,z1).color(r,g,b,a).uv(u0,v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(-1,0,0).endVertex();
        vc.vertex(m, x1,yBottom,z1).color(r,g,b,a).uv(u0,v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(-1,0,0).endVertex();
        vc.vertex(m, x1,yBottom,z2).color(r,g,b,a).uv(u1,v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(-1,0,0).endVertex();
        vc.vertex(m, x1,yTop,z2).color(r,g,b,a).uv(u1,v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(-1,0,0).endVertex();

        // EAST
        vc.vertex(m, x2,yTop,z2).color(r,g,b,a).uv(u0,v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(1,0,0).endVertex();
        vc.vertex(m, x2,yBottom,z2).color(r,g,b,a).uv(u0,v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(1,0,0).endVertex();
        vc.vertex(m, x2,yBottom,z1).color(r,g,b,a).uv(u1,v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(1,0,0).endVertex();
        vc.vertex(m, x2,yTop,z1).color(r,g,b,a).uv(u1,v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(1,0,0).endVertex();

        if (shouldLog) {
            System.out.println("[AetherTankRenderer] Done drawing vertices.");
        }
    }
}