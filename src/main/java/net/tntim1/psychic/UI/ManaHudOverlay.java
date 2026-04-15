package net.tntim1.psychic.UI;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tntim1.psychic.player_data.ClientManaStore;

@Mod.EventBusSubscriber(modid = "psychic", value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class ManaHudOverlay {

    // Sparkle frames cycling for a twinkle effect
    private static int sparkleFrame = 0;
    private static long lastFrameTime = 0;
    private static final String[] SPARKLE = { "✦", "✧", "✦", "✧" };

    @SubscribeEvent
    public static void onRenderHud(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        boolean castingUiOpen = mc.screen instanceof CastingUi;
        boolean manaNotFull = !ClientManaStore.isFull();

        if (!castingUiOpen && !manaNotFull) return;

        // Animate sparkle ~4fps
        long now = System.currentTimeMillis();
        if (now - lastFrameTime > 250) { sparkleFrame = (sparkleFrame + 1) % 4; lastFrameTime = now; }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();

        // Format: "✦ 73/100"
        String sparkle = SPARKLE[sparkleFrame];
        String manaText = String.format("%.0f/%.0f", ClientManaStore.mana, ClientManaStore.maxMana);

        int sparkleColor = 0xAA88FF; // soft purple
        int textColor    = 0xCCAEFF;
        int lowColor     = 0xFF4444; // red when very low

        boolean isLow = ClientManaStore.mana < ClientManaStore.maxMana * 0.2f;

        // Draw sparkle then number — right-aligned
        int textWidth = mc.font.width(manaText);
        int sparkleWidth = mc.font.width(sparkle + " ");
        int totalWidth = sparkleWidth + textWidth;
        int x = screenWidth - totalWidth - 6;
        int y = 6;

        guiGraphics.drawString(mc.font, sparkle, x, y, sparkleColor, false);
        guiGraphics.drawString(mc.font, manaText, x + sparkleWidth, y,
                isLow ? lowColor : textColor, false);
    }
}