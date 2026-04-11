package net.tntim1.psychic.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tntim1.psychic.Psychic;
import net.tntim1.psychic.player_data.ClientKnowledge;

@Mod.EventBusSubscriber(modid = Psychic.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class HudOverlay {

    private static final long DISPLAY_MS  = 4000;
    private static final long SLIDE_MS    = 300;
    private static final int  TOAST_W     = 180;
    private static final int  TOAST_H     = 36;
    private static final int  MARGIN      = 8;

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        GuiGraphics gfx = event.getGuiGraphics();
        long now = System.currentTimeMillis();
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        // ── Unlock toasts (large, gold border) ───────────────────────────────
        ClientKnowledge.UnlockToast uToast = ClientKnowledge.UNLOCK_TOASTS.peek();
        if (uToast != null) {
            long age = now - uToast.createdAt();
            if (age > DISPLAY_MS) {
                ClientKnowledge.UNLOCK_TOASTS.poll();
            } else {
                float slide = Math.min(1f, (float)age / SLIDE_MS);
                int tx = (int)(screenW - MARGIN - TOAST_W * slide);
                int ty = MARGIN;
                gfx.fill(tx, ty, tx + TOAST_W, ty + TOAST_H, 0xFF1a1208);
                drawToastBorder(gfx, tx, ty, TOAST_W, TOAST_H, 0xFFFFD700);

                // Fixed Font rendering
                gfx.drawString(mc.font, "✦ UNLOCKED", tx + 8, ty + 6, 0xFFFFD700);
                gfx.drawString(mc.font, uToast.widgetLabel(), tx + 8, ty + 18, 0xFFd4a9a2);
            }
        }

        // ── Task toasts (smaller, amber) ──────────────────────────────────────
        ClientKnowledge.TaskToast tToast = ClientKnowledge.TASK_TOASTS.peek();
        if (tToast != null) {
            long age = now - tToast.createdAt();
            if (age > DISPLAY_MS) {
                ClientKnowledge.TASK_TOASTS.poll();
            } else {
                float slide = Math.min(1f, (float)age / SLIDE_MS);
                int toastOffset = (uToast != null) ? TOAST_H + MARGIN + 4 : 0;
                int tx = (int)(screenW - MARGIN - TOAST_W * slide);
                int ty = MARGIN + toastOffset;
                gfx.fill(tx, ty, tx + TOAST_W, ty + TOAST_H - 8, 0xFF120e04);
                drawToastBorder(gfx, tx, ty, TOAST_W, TOAST_H - 8, 0xFFCC8800);

                String header = tToast.completed() ? "✔ Task complete" : "↑ Progress";
                int headerCol = tToast.completed() ? 0xFF55FF55 : 0xFFCC8800;

                // Fixed Font rendering
                gfx.drawString(mc.font, header, tx + 8, ty + 4, headerCol);
                gfx.drawString(mc.font, tToast.taskLabel(), tx + 8, ty + 14, 0xFFaaaaaa);
            }
        }
    }

    private static void drawToastBorder(GuiGraphics gfx, int x, int y, int w, int h, int col) {
        gfx.fill(x, y, x + w, y + 1, col);
        gfx.fill(x, y + h - 1, x + w, y + h, col);
        gfx.fill(x, y, x + 1, y + h, col);
        gfx.fill(x + w - 1, y, x + w, y + h, col);
    }
}