package net.tntim1.psychic.UI;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.tntim1.psychic.Keybinds.KeyInit;
import net.tntim1.psychic.Spells.WorldSpellData;
import net.tntim1.psychic.UI.Rhythm.RhythmNote;
import net.tntim1.psychic.network.CastSpellPacket;
import net.tntim1.psychic.network.ModPackets;
import net.tntim1.psychic.network.RequestWarpPacket;
import net.tntim1.psychic.network.SpendManaPacket;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CastingUi extends Screen {
    private static final ResourceLocation TEXTURE = new ResourceLocation("psychic", "textures/gui/casting_ui.png");
    private static final ResourceLocation SHADER_LOC = new ResourceLocation("psychic", "casting_tint");

    private String currentSpellId = null;

    private final long[] feedbackExpiry = new long[8];
    private final int[] feedbackColor = new int[8];
    private record LineConnection(int startLane, int endLane, long timestamp) {}
    private final List<LineConnection> completedLines = new ArrayList<>();

    private static final int TEX_SIZE = 160;
    private static final float TEX_CENTER_X = TEX_SIZE / 2f;
    private static final float TEX_CENTER_Y = TEX_SIZE / 2f;

    private static final float[] BUTTON_TEX_X = {  54f,  23f,  21f,  56f, 103f, 135f, 137f, 104f };
    private static final float[] BUTTON_TEX_Y = { 135f, 103f,  55f,  22f,  23f,  54f, 103f, 134f };
    private static final float BUTTON_TEX_RADIUS = 12f;

    private final List<Integer> sequence = new ArrayList<>();
    private final boolean[] isPressed = new boolean[8];

    private int uiState = 0; // 0 = Dialing, 1 = Rhythm
    private final List<RhythmNote> activeNotes = new ArrayList<>();
    private long rhythmStartTime = 0;
    private float accuracy = 100.0f;
    private int earnedPoints = 0;
    private int maxPossiblePoints = 0;

    private int warpStrength = 0;

    private static final int[] BUTTON_COLORS = {
            0xFFFF0000, // 1: Red
            0xFF5555FF, // 2: blue
            0xFF55FF55, // 3: Green
            0xFF000000, // 4: black
            0xFFFF55FF, // 5: Pink
            0xFF555555, // 6: gray
            0xFFBF00FF, // 7: Orange
            0xFFFFFFFF  // 8: White
    };

    private ShaderInstance tintShader = null;

    public CastingUi() {
        super(Component.translatable("psychic.casting_screen.cast_instruction",
                KeyInit.confirmKey.getTranslatedKeyMessage()));
    }

    // --- Helpers ---
    private float getScale() { return Math.min((float) this.width / TEX_SIZE, (float) this.height / TEX_SIZE); }
    private float texOriginX() { return (this.width - TEX_SIZE * getScale()) / 2f; }
    private float texOriginY() { return (this.height - TEX_SIZE * getScale()) / 2f; }
    private int buttonScreenX(int lane) { return Math.round(texOriginX() + BUTTON_TEX_X[lane] * getScale()); }
    private int buttonScreenY(int lane) { return Math.round(texOriginY() + BUTTON_TEX_Y[lane] * getScale()); }
    private int buttonScreenRadius() { return Math.round(BUTTON_TEX_RADIUS * getScale()); }

    private ShaderInstance getShader() {
        if (tintShader == null) {
            try {
                tintShader = new ShaderInstance(Minecraft.getInstance().getResourceManager(), SHADER_LOC, DefaultVertexFormat.POSITION_TEX_COLOR);
            } catch (IOException e) { throw new RuntimeException(e); }
        }
        return tintShader;
    }

    @Override public void onClose() { super.onClose(); if (tintShader != null) tintShader.close(); }
    @Override protected void init() { super.init(); Minecraft.getInstance().mouseHandler.releaseMouse();  ModPackets.sendToServer(new RequestWarpPacket());}
    @Override public boolean isPauseScreen() { return false; }

    // --- Logic ---

    private void requestRhythmStart(String spellId) {
        this.currentSpellId = spellId;
        ModPackets.sendToServer(new SpendManaPacket(spellId));
        // UI stays in dialing state until server responds
    }

    // Called by ManaSpendResultPacket if success
    public void confirmRhythmStart() {
        this.uiState = 1;
        this.rhythmStartTime = System.currentTimeMillis();
        this.activeNotes.clear();
        for (int i = 0; i < 10; i++) {
            activeNotes.add(new RhythmNote((int)(Math.random() * 8), 2000 + (i * 800L)));
        }
    }

    // Called by ManaSpendResultPacket if failure
    public void cancelRhythmStart() {
        this.currentSpellId = null;
        clearSequence();
        Minecraft.getInstance().player.displayClientMessage(
                Component.literal("§cNot enough mana!"), true);
    }
    private void startRhythmGame(String spellName) {
        this.currentSpellId = spellName;
        this.uiState = 1;
        this.rhythmStartTime = System.currentTimeMillis();
        this.activeNotes.clear();
        for (int i = 0; i < 10; i++) {
            activeNotes.add(new RhythmNote((int)(Math.random()*8), 2000 + (i * 800L)));
        }
    }
    public void setWarpStrength(int value) {
        this.warpStrength = value;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        float scale = getScale();
        int centerX = this.width / 2;

        guiGraphics.drawString(
                this.font,
                "Warp: " + warpStrength,
                10,
                10,
                0xAA00FF
        );

        // --- 1. Render Background ---
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, -50);
        guiGraphics.blit(TEXTURE, Math.round(texOriginX()), Math.round(texOriginY()),
                Math.round(TEX_SIZE * scale), Math.round(TEX_SIZE * scale), 0, 0, TEX_SIZE, TEX_SIZE, TEX_SIZE, TEX_SIZE);
        guiGraphics.pose().popPose();

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // --- 2. Render UI Text (RESTORED) ---
        if (uiState == 0) {
            guiGraphics.drawCenteredString(this.font, this.title, centerX, 20, 0xFFFFFF);
        } else {
            String accText = String.format("Accuracy: %.1f%%", accuracy);
            guiGraphics.drawCenteredString(this.font, accText, centerX, 20, 0x00FF00);
            if (uiState == 1 && !activeNotes.isEmpty()) {
                boolean allFinished = true;
                long lastNoteEndTime = 0;

                for (RhythmNote note : activeNotes) {
                    if (!note.hit && !note.missed) {
                        allFinished = false;
                        break;
                    }
                    lastNoteEndTime = Math.max(lastNoteEndTime, rhythmStartTime + note.targetTime);
                }

                // If all notes are processed, wait 500ms after the last note's arrival time to close
                if (allFinished && System.currentTimeMillis() > lastNoteEndTime + 500) {
                    this.onClose();
                    sendCompletionMessage();
                }
            }
        }

        // --- 3. Render Game Elements ---
        renderButtons(guiGraphics);
        if (uiState == 1) renderRhythmNotes(guiGraphics);
    }
    private void sendCompletionMessage() {
        if (Minecraft.getInstance().player != null) {
            // Send to server to actually execute the spell
            if (currentSpellId != null) {
                ModPackets.sendToServer(new CastSpellPacket(currentSpellId, accuracy));
            }

            String color = accuracy >= 90 ? "§6" : (accuracy >= 70 ? "§e" : "§7");
            Minecraft.getInstance().player.displayClientMessage(
                    Component.literal(String.format("§b✨ Spell Cast! Final Accuracy: %s%.1f%%", color, accuracy)),
                    false
            );
        }
    }

    // --- Updated renderButtons Logic ---
    private void renderButtons(GuiGraphics guiGraphics) {
        int r = buttonScreenRadius();
        long now = System.currentTimeMillis();

        if (uiState == 0) {
            for (LineConnection line : completedLines) {
                float x1 = (float) buttonScreenX(line.startLane);
                float y1 = (float) buttonScreenY(line.startLane);
                float x2 = (float) buttonScreenX(line.endLane);
                float y2 = (float) buttonScreenY(line.endLane);

                int colorStart = BUTTON_COLORS[line.startLane];
                int colorEnd = BUTTON_COLORS[line.endLane];

                long elapsed = now - line.timestamp;

                // --- The Surge (Lead Tip) ---
                float leadProgress = Math.min(1.0f, elapsed / 300f);
                float leadCurX = x1 + (x2 - x1) * leadProgress;
                float leadCurY = y1 + (y2 - y1) * leadProgress;

                // Calculate segments based on length so the jitter stays consistent
                float dist = (float) Math.sqrt(Math.pow(leadCurX - x1, 2) + Math.pow(leadCurY - y1, 2));
                int segments = Math.max(2, (int)(dist / 16f));

                // Draw the white "Spark" head (more jitter, thin)
                drawGradientLine(guiGraphics, x1, y1, leadCurX, leadCurY,
                        0xFFFFFFFF, 0xFFFFFFFF, 0.8f, -4.5f, 0.5f,
                        1.0f, segments);

                // --- The Main Flow ---
                float mainProgress = Math.min(1.0f, elapsed / 600f);
                float mainCurX = x1 + (x2 - x1) * mainProgress;
                float mainCurY = y1 + (y2 - y1) * mainProgress;

                // Slightly less jitter for the main body
                drawGradientLine(guiGraphics, x1, y1, mainCurX, mainCurY,
                        colorStart, colorEnd, 2.0f, -4.0f, 0.7f,
                        0.5f, segments);
            }
        }

        // 2. Draw Buttons (Z = 0)
        for (int i = 0; i < 8; i++) {
            int buttonId = i + 1;

            // Active: The button is being pressed OR is the "waiting" start of a new line
            boolean isFirstOfPair = (sequence.size() % 2 != 0) && (sequence.get(sequence.size() - 1) == buttonId);
            boolean isActive = (uiState == 0 && (isFirstOfPair || isPressed[i]));

            // Check if this button is part of any finished connection
            boolean isConnected = false;
            for (LineConnection conn : completedLines) {
                if (conn.startLane == i || conn.endLane == i) {
                    isConnected = true;
                    break;
                }
            }

            if (isActive) {
                // "Current no glow when active": Render with original texture colors (1.0 tint)
                // Note: alpha 0.0f in your drawTintedCircle might be handled by your shader
                // to show the raw texture. Adjust if your shader uses 1.0 for "no tint".
                drawTintedCircle(guiGraphics, buttonScreenX(i), buttonScreenY(i), r, 1.0f, 1.0f, 1.0f, 0.0f);

            } else if (uiState == 1 && now < feedbackExpiry[i]) {
                // Rhythm Feedback
                drawTintedCircle(guiGraphics, buttonScreenX(i), buttonScreenY(i), r,
                        (feedbackColor[i] == 0 ? 0f : 1f), (feedbackColor[i] == 0 ? 1f : 0f), 0f, 0.8f);

            } else if (isConnected) {
                // "Strong glow when not active but in a connection"
                int color = BUTTON_COLORS[i];
                float red = ((color >> 16) & 0xFF) / 255f;
                float green = ((color >> 8) & 0xFF) / 255f;
                float blue = (color & 0xFF) / 255f;

                // Render with the button's specific color at full strength
                drawTintedCircle(guiGraphics, buttonScreenX(i), buttonScreenY(i), r, red, green, blue, 1.0f);

            } else {
                // Default "Off" state: Dimmed
                drawTintedCircle(guiGraphics, buttonScreenX(i), buttonScreenY(i), r, 0.7f, 0.7f, 0.7f, 1.0f);
            }
        }
    }

    private void drawGradientLine(GuiGraphics guiGraphics, float x1, float y1, float x2, float y2,
                                  int color1, int color2, float thicknessMult, float z, float alphaMult,
                                  float jitter, int segments) {

        if (Math.abs(x1 - x2) < 0.01f && Math.abs(y1 - y2) < 0.01f) return;

        Matrix4f matrix = guiGraphics.pose().last().pose();
        VertexConsumer consumer = guiGraphics.bufferSource().getBuffer(RenderType.gui());

        // --- Color Extraction ---
        float r1 = ((color1 >> 16) & 0xFF) / 255f;
        float g1 = ((color1 >> 8) & 0xFF) / 255f;
        float b1 = (color1 & 0xFF) / 255f;
        float a1 = (((color1 >> 24) & 0xFF) / 255f) * alphaMult;

        float r2 = ((color2 >> 16) & 0xFF) / 255f;
        float g2 = ((color2 >> 8) & 0xFF) / 255f;
        float b2 = (color2 & 0xFF) / 255f;
        float a2 = (((color2 >> 24) & 0xFF) / 255f) * alphaMult;

        // --- Math Prep ---
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);

        float nx = -dy / len;
        float ny = dx / len;
        float baseThickness = thicknessMult * getScale();

        // High-speed flicker seed
        long frameSeed = (System.currentTimeMillis() / 30);
        java.util.Random rand = new java.util.Random();

        // --- Loop State Initialization ---
        float lastX = x1;
        float lastY = y1;
        float lastThickness = baseThickness;

        // Starting colors for the very first segment
        float lastR = r1;
        float lastG = g1;
        float lastB = b1;
        float lastA = a1;

        for (int i = 1; i <= segments; i++) {
            float t = (float) i / segments;

            // Linear targets
            float nextX = x1 + dx * t;
            float nextY = y1 + dy * t;
            float currentThickness = baseThickness;

            // Apply Jitter to intermediate points
            if (i < segments) {
                rand.setSeed(frameSeed + i + (long)(x1 * 31));

                float intensity = rand.nextFloat() * jitter * getScale();
                nextX += (rand.nextFloat() - 0.5f) * intensity;
                nextY += (rand.nextFloat() - 0.5f) * intensity;

                // Width jitter for the pulsing effect
                currentThickness *= (0.7f + rand.nextFloat() * 0.6f);
            }

            // Calculate colors for the END of this segment
            float currR = r1 + (r2 - r1) * t;
            float currG = g1 + (g2 - g1) * t;
            float currB = b1 + (b2 - b1) * t;
            float currA = a1 + (a2 - a1) * t;

            // Offset positions for thickness
            float offX1 = nx * (lastThickness / 2f);
            float offY1 = ny * (lastThickness / 2f);
            float offX2 = nx * (currentThickness / 2f);
            float offY2 = ny * (currentThickness / 2f);

            // --- Render Quad Segment ---
            // By using 'last' colors for the start and 'curr' colors for the end,
            // the GPU creates a smooth gradient across the segment.
            consumer.vertex(matrix, lastX - offX1, lastY - offY1, z).color(lastR, lastG, lastB, lastA).endVertex();
            consumer.vertex(matrix, lastX + offX1, lastY + offY1, z).color(lastR, lastG, lastB, lastA).endVertex();
            consumer.vertex(matrix, nextX + offX2, nextY + offY2, z).color(currR, currG, currB, currA).endVertex();
            consumer.vertex(matrix, nextX - offX2, nextY - offY2, z).color(currR, currG, currB, currA).endVertex();

            // --- Advance State ---
            lastX = nextX;
            lastY = nextY;
            lastThickness = currentThickness;
            lastR = currR;
            lastG = currG;
            lastB = currB;
            lastA = currA;
        }
        guiGraphics.flush();
    }



    private void handleLanePress(int lane) {
        int num = lane + 1;
        if (uiState == 1) {
            long now = System.currentTimeMillis();

            boolean hit = false;

            float hitWindow = 400f; // Must match renderRhythmNotes

            for (RhythmNote note : activeNotes) {
                if (note.lane == lane && !note.hit && !note.missed) {
                    long noteArrival = rhythmStartTime + note.targetTime;
                    long diff = Math.abs(now - noteArrival);

                    if (diff < hitWindow) {
                        note.hit = hit = true;
                        feedbackExpiry[lane] = now + 200;
                        feedbackColor[lane] = 0; // Green feedback

                        // ACCURACY CALCULATION:
                        // Perfect (0ms diff) = 100 points
                        // Barely hit (400ms diff) = 0 points
                        int score = (int)(100 * (1.0f - (diff / hitWindow)));
                        updateAccuracy(score, 100);
                        break;
                    }
                }
            }

            if (!hit) { feedbackExpiry[lane] = now + 200; feedbackColor[lane] = 1; updateAccuracy(0, 50); }
        } else {

            if (!sequence.isEmpty() && sequence.size() % 2 != 0) {
                int lastSelected = sequence.get(sequence.size() - 1);
                int startLane = lastSelected - 1;
                int endLane = lane;

                // 1. Double-click to deselect
                if (lastSelected == num) {
                    sequence.remove(sequence.size() - 1);
                    return;
                }

                // 2. Check if this specific line already exists (in either direction)
                boolean exists = false;
                for (LineConnection connection : completedLines) {
                    if ((connection.startLane == startLane && connection.endLane == endLane) ||
                            (connection.startLane == endLane && connection.endLane == startLane)) {
                        exists = true;
                        break;
                    }
                }

                if (exists) {
                    // If it exists, we deselect the first button and don't add the new line
                    sequence.remove(sequence.size() - 1);
                    // Optional: Play a "fail" sound or message here
                    return;
                }

                // 3. Pair completed! Save it with the current time
                completedLines.add(new LineConnection(startLane, endLane, System.currentTimeMillis()));
            }
            sequence.add(num);
        }
    }


    // Clear lines if the spell is reset
    private void clearSequence() {
        sequence.clear();
        completedLines.clear();
    }

    private void processSpellCheck() {
        String foundSpellId = null;

        // 1. Iterate through the static Registry (Available on Client)
        for (Map.Entry<String, net.tntim1.psychic.Spells.SpellDefinition> entry : net.tntim1.psychic.Spells.SpellRegistry.SPELLS.entrySet()) {
            // We simulate the match check here on the client
            if (checkMatchLocal(entry.getValue().pattern)) {
                foundSpellId = entry.getKey();
                break;
            }
        }

        if (foundSpellId != null) {
            // 2. Check knowledge
            if (net.tntim1.psychic.player_data.ClientKnowledge.isUnlocked(foundSpellId)) {
                this.completedLines.clear();
                requestRhythmStart(foundSpellId);
                Minecraft.getInstance().player.displayClientMessage(
                        Component.literal("§b✔ Casting " + foundSpellId + "..."), true);

            } else {
                clearSequence();
                Minecraft.getInstance().player.displayClientMessage(
                        Component.literal("§6⚠ Research Required!"), true);
            }
        } else {
            clearSequence();
            Minecraft.getInstance().player.displayClientMessage(
                    Component.literal("§c✘ Invalid Pattern"), true);
        }
    }

    /**
     * Local helper to see if current sequence matches a pattern
     */
    private boolean checkMatchLocal(Set<Set<Integer>> requiredPattern) {
        if (this.sequence.size() < 2 || this.sequence.size() % 2 != 0) return false;

        Set<Set<Integer>> inputConnections = new java.util.HashSet<>();
        for (int i = 0; i < this.sequence.size() - 1; i += 2) {
            Set<Integer> pair = new java.util.HashSet<>();
            pair.add(this.sequence.get(i));
            pair.add(this.sequence.get(i + 1));
            inputConnections.add(pair);
        }
        return inputConnections.equals(requiredPattern);
    }



    private void drawTintedCircle(GuiGraphics g, int cx, int cy, int r, float tr, float tg, float tb, float ta) {
        ShaderInstance shader = getShader();
        if (shader.getUniform("TintColor") != null) shader.getUniform("TintColor").set(tr, tg, tb, ta);
        RenderSystem.enableBlend();
        RenderSystem.setShader(() -> shader);
        RenderSystem.setShaderTexture(0, TEXTURE);

        Matrix4f matrix = g.pose().last().pose();
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        float scale = getScale(), ox = texOriginX(), oy = texOriginY();

        for (int dy = -r; dy <= r; dy++) {
            int dx = (int) Math.sqrt(r * r - dy * dy);
            float sx0 = cx - dx, sx1 = cx + dx + 1, sy0 = cy + dy, sy1 = cy + dy + 1;
            float u0 = (sx0 - ox) / (TEX_SIZE * scale), u1 = (sx1 - ox) / (TEX_SIZE * scale);
            float v0 = (sy0 - oy) / (TEX_SIZE * scale), v1 = (sy1 - oy) / (TEX_SIZE * scale);
            buf.vertex(matrix, sx0, sy1, 0).uv(u0, v1).color(1f, 1f, 1f, 1f).endVertex();
            buf.vertex(matrix, sx1, sy1, 0).uv(u1, v1).color(1f, 1f, 1f, 1f).endVertex();
            buf.vertex(matrix, sx1, sy0, 0).uv(u1, v0).color(1f, 1f, 1f, 1f).endVertex();
            buf.vertex(matrix, sx0, sy0, 0).uv(u0, v0).color(1f, 1f, 1f, 1f).endVertex();
        }
        Tesselator.getInstance().end();
    }

    @Override
    public boolean keyPressed(int key, int sc, int mod) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }

        if (KeyInit.castingKey.isActiveAndMatches(InputConstants.getKey(key, sc))) {
            this.onClose();
            return true;
        }

        if (KeyInit.confirmKey.isActiveAndMatches(InputConstants.getKey(key, sc)) && uiState == 0) {
            processSpellCheck();
            return true;
        }
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_8) {
            int lane = key - GLFW.GLFW_KEY_1;
            isPressed[lane] = true;
            handleLanePress(lane);
            return false;
        }
        return false;
    }

    @Override
    public boolean keyReleased(int key, int sc, int mod) {
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_8) isPressed[key - GLFW.GLFW_KEY_1] = false;
        return false;
    }

    private void renderRhythmNotes(GuiGraphics guiGraphics) {
        long now = System.currentTimeMillis();
        float scale = getScale();

        // Increased window: 400ms (Total window is 800ms centered on target)
        float hitWindow = 400f;

        VertexConsumer consumer = guiGraphics.bufferSource().getBuffer(RenderType.gui());
        Matrix4f matrix = guiGraphics.pose().last().pose();

        for (RhythmNote note : activeNotes) {
            if (note.hit || note.missed) continue;

            long noteArrival = rhythmStartTime + note.targetTime;
            long diff = now - noteArrival; // 0 is perfect, negative is early, positive is late

            if (diff < -2000) continue;
            if (diff > hitWindow) { // Note missed
                note.missed = true;
                updateAccuracy(0, 100);
                continue;
            }

            // --- Logic: Proximity for Glow/Growth ---
            // 1.0 = Perfect timing, 0.0 = Outside hit window
            float proximity = Math.max(0, 1.0f - (Math.abs(diff) / hitWindow));

            // --- Visuals: Size & Transparency ---
            // Base size 3, grows to 8 when correct click is possible
            float currentRadius = (3.0f + (5.0f * proximity)) * scale;
            float alpha = 0.7f + (0.3f * proximity);

            float progress = Math.max(0, (noteArrival - now) / 2000f);
            float targetX = buttonScreenX(note.lane);
            float targetY = buttonScreenY(note.lane);
            float centerX = texOriginX() + (TEX_CENTER_X * scale);
            float centerY = texOriginY() + (TEX_CENTER_Y * scale);

            float curX = targetX + (centerX - targetX) * progress;
            float curY = targetY + (centerY - targetY) * progress;

            int color = BUTTON_COLORS[note.lane];
            float r = ((color >> 16) & 0xFF) / 255f;
            float g = ((color >> 8) & 0xFF) / 255f;
            float b = (color & 0xFF) / 255f;

            // Draw diamond shape for a "magical" feel
            consumer.vertex(matrix, curX, curY + currentRadius, 10).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, curX + currentRadius, curY, 10).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, curX, curY - currentRadius, 10).color(r, g, b, alpha).endVertex();
            consumer.vertex(matrix, curX - currentRadius, curY, 10).color(r, g, b, alpha).endVertex();
        }
        guiGraphics.flush();
    }
    private void updateAccuracy(int p, int m) { this.earnedPoints += p; this.maxPossiblePoints += m; if (maxPossiblePoints > 0) accuracy = (earnedPoints / (float)maxPossiblePoints) * 100; }
}