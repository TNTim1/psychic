package net.tntim1.psychic.block.entity;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

import java.util.ArrayList;

public class LaserMirrorGame extends MiniGame {

    private enum Tile {
        EMPTY, WALL, MIRROR_NW, MIRROR_NE, SOURCE, TARGET
    }

    private Tile[][] grid;
    private int gridCols, gridRows;
    private int mirrorsAvailable; // Total inventory given to the player
    private int mirrorsPlaced;
    private boolean pathComplete = false;

    @Override
    public void init(int difficulty) {
        pathComplete = false;
        mirrorsPlaced = 0;

        switch (difficulty) {
            case 0 -> setupEasy();
            case 1 -> setupMedium();
            default -> setupHard();
        }
    }

    private void setupEasy() {
        gridCols = 8; gridRows = 6;
        grid = emptyGrid(gridCols, gridRows);
        grid[0][1] = Tile.SOURCE; // Fires Right
        grid[7][4] = Tile.TARGET;
        mirrorsAvailable = 2; // Requires exactly two 90-degree turns
    }

    private void setupMedium() {
        gridCols = 10; gridRows = 10;
        grid = emptyGrid(gridCols, gridRows);
        grid[0][0] = Tile.SOURCE;
        grid[9][9] = Tile.TARGET;
        // Central wall forces a perimeter route
        for(int i = 2; i < 8; i++) {
            grid[i][4] = Tile.WALL;
            grid[i][5] = Tile.WALL;
        }
        mirrorsAvailable = 4;
    }

    private void setupHard() {
        gridCols = 12; gridRows = 12;
        grid = emptyGrid(gridCols, gridRows);
        grid[1][0] = Tile.SOURCE; // Fires Down
        grid[10][11] = Tile.TARGET;

        // Maze construction: Create narrow corridors
        for(int x = 0; x < 12; x++) {
            if (x != 1 && x != 5 && x != 10) grid[x][3] = Tile.WALL;
            if (x != 3 && x != 8) grid[x][8] = Tile.WALL;
        }

        // This requires 6 mirrors to "snake" through the specific openings
        mirrorsAvailable = 6;
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, int startX, int startY) {
        int gX = startX + 20;
        int gY = startY + 40;
        pathComplete = false;

        // 1. Render Grid
        for (int x = 0; x < gridCols; x++) {
            for (int y = 0; y < gridRows; y++) {
                int px = gX + x * 16;
                int py = gY + y * 16;
                graphics.fill(px, py, px + 15, py + 15, 0x11FFFFFF);

                switch (grid[x][y]) {
                    case WALL   -> graphics.fill(px, py, px + 15, py + 15, 0xFF222222);
                    case SOURCE -> graphics.fill(px + 2, py + 2, px + 13, py + 13, 0xFFFFCC00);
                    case TARGET -> graphics.fill(px + 2, py + 2, px + 13, py + 13, pathComplete ? 0xFF55FF55 : 0xFF440000);
                    case MIRROR_NW -> drawMirrorLine(graphics, px, py, true);
                    case MIRROR_NE -> drawMirrorLine(graphics, px, py, false);
                }
            }
        }

        // 2. Trace Path
        tracePath(graphics, gX, gY);

        // 3. UI Overlay
        String text = "Mirrors: " + (mirrorsAvailable - mirrorsPlaced);
        int color = (mirrorsPlaced > mirrorsAvailable) ? 0xFFFF5555 : 0xFFFFFFFF;
        graphics.drawString(font, text, gX, gY - 15, color);

        if (pathComplete && mirrorsPlaced == mirrorsAvailable) {
            graphics.drawString(font, "CORE STABILIZED", gX, gY + (gridRows * 16) + 5, 0xFF55FF55);
        }
    }

    private void tracePath(GuiGraphics graphics, int gX, int gY) {
        int sx = -1, sy = -1;
        // Find source
        outer: for(int x=0; x<gridCols; x++)
            for(int y=0; y<gridRows; y++)
                if(grid[x][y] == Tile.SOURCE) { sx = x; sy = y; break outer; }

        // Initial vector: Down if Y=0, else Right
        float dx = (sy == 0) ? 0 : 1;
        float dy = (sy == 0) ? 1 : 0;
        float cx = sx + 0.5f, cy = sy + 0.5f;

        for (int i = 0; i < 100; i++) {
            float nx = cx + dx;
            float ny = cy + dy;

            if (nx < 0 || nx >= gridCols || ny < 0 || ny >= gridRows) break;

            // Draw Beam Segment
            drawGradientLine(graphics, gX + cx * 16, gY + cy * 16, gX + nx * 16, gY + ny * 16,
                    0xFF00FFFF, 0xFFFFFFFF, 1.2f, 0, 1.0f, 0.1f, 1);

            cx = nx; cy = ny;
            Tile hit = grid[(int)cx][(int)cy];

            if (hit == Tile.TARGET) {
                pathComplete = true;
                break;
            } else if (hit == Tile.WALL) {
                break;
            } else if (hit == Tile.MIRROR_NW) { // \ : swap and flip
                float temp = dx; dx = -dy; dy = -temp;
            } else if (hit == Tile.MIRROR_NE) { // / : swap
                float temp = dx; dx = dy; dy = temp;
            }
        }
    }

    @Override
    public void handleInput(double mouseX, double mouseY, int button, int startX, int startY) {
        int x = (int) (mouseX - (startX + 20)) / 16;
        int y = (int) (mouseY - (startY + 40)) / 16;

        if (x < 0 || x >= gridCols || y < 0 || y >= gridRows) return;

        Tile current = grid[x][y];
        if (button == 0) { // Cycle: Empty -> \ -> / -> Empty
            if (current == Tile.EMPTY) {
                grid[x][y] = Tile.MIRROR_NW;
                mirrorsPlaced++;
            } else if (current == Tile.MIRROR_NW) {
                grid[x][y] = Tile.MIRROR_NE;
            } else if (current == Tile.MIRROR_NE) {
                grid[x][y] = Tile.EMPTY;
                mirrorsPlaced--;
            }
        }
    }

    private void drawMirrorLine(GuiGraphics g, int px, int py, boolean nw) {
        // Draw a simple high-contrast line for the mirror face
        if (nw) g.fill(px + 1, py + 1, px + 14, py + 3, 0xFFAAAAAA);
        else    g.fill(px + 1, py + 12, px + 14, py + 14, 0xFFAAAAAA);
    }

    @Override
    public boolean isWon() {
        return pathComplete && mirrorsPlaced == mirrorsAvailable;
    }

    @Override
    public boolean isLost() {
        return mirrorsPlaced > mirrorsAvailable;
    }

    private Tile[][] emptyGrid(int c, int r) {
        Tile[][] g = new Tile[c][r];
        for (int i=0; i<c; i++) for (int j=0; j<r; j++) g[i][j] = Tile.EMPTY;
        return g;
    }private void drawGradientLine(GuiGraphics guiGraphics,

                                   float x1, float y1, float x2, float y2,

                                   int color1, int color2,

                                   float thicknessMult, float z,

                                   float alphaMult, float jitter, int segments) {


        if (Math.abs(x1 - x2) < 0.01f && Math.abs(y1 - y2) < 0.01f) return;


        Matrix4f matrix = guiGraphics.pose().last().pose();

        VertexConsumer consumer = guiGraphics.bufferSource().getBuffer(RenderType.gui());


// Decompose colours into normalised float channels

        float r1 = ((color1 >> 16) & 0xFF) / 255f;

        float g1 = ((color1 >> 8) & 0xFF) / 255f;

        float b1 = ( color1 & 0xFF) / 255f;

        float a1 = (((color1 >> 24) & 0xFF) / 255f) * alphaMult;


        float r2 = ((color2 >> 16) & 0xFF) / 255f;

        float g2 = ((color2 >> 8) & 0xFF) / 255f;

        float b2 = ( color2 & 0xFF) / 255f;

        float a2 = (((color2 >> 24) & 0xFF) / 255f) * alphaMult;


// Screen-space normal (perpendicular to the line direction)

        float dx = x2 - x1, dy = y2 - y1;

        float len = (float) Math.sqrt(dx * dx + dy * dy);

        float nx = -dy / len;

        float ny = dx / len;


        float baseThickness = thicknessMult;

        long frameSeed = System.currentTimeMillis() / 40;

        java.util.Random rand = new java.util.Random();


// Walk state

        float lastX = x1, lastY = y1, lastThickness = baseThickness;

        float lastR = r1, lastG = g1, lastB = b1, lastA = a1;


        for (int i = 1; i <= segments; i++) {

            float t = (float) i / segments;


            float nextX = x1 + dx * t;

            float nextY = y1 + dy * t;

            float currentThickness = baseThickness;


// Apply per-segment jitter (skip the very last point to avoid

// displacing the endpoint)

            if (i < segments && jitter > 0f) {

                rand.setSeed(frameSeed + i + (long) (x1 * 100));

                nextX += (rand.nextFloat() - 0.5f) * jitter;

                nextY += (rand.nextFloat() - 0.5f) * jitter;

                currentThickness *= (0.8f + rand.nextFloat() * 0.4f);

            }


// Interpolated colour at this segment end

            float currR = r1 + (r2 - r1) * t;

            float currG = g1 + (g2 - g1) * t;

            float currB = b1 + (b2 - b1) * t;

            float currA = a1 + (a2 - a1) * t;


// Half-widths along the normal

            float offX1 = nx * (lastThickness / 2f);

            float offY1 = ny * (lastThickness / 2f);

            float offX2 = nx * (currentThickness / 2f);

            float offY2 = ny * (currentThickness / 2f);


// Emit quad (four vertices – Minecraft's GUI layer uses quads)

            consumer.vertex(matrix, lastX - offX1, lastY - offY1, z)

                    .color(lastR, lastG, lastB, lastA).endVertex();

            consumer.vertex(matrix, lastX + offX1, lastY + offY1, z)

                    .color(lastR, lastG, lastB, lastA).endVertex();

            consumer.vertex(matrix, nextX + offX2, nextY + offY2, z)

                    .color(currR, currG, currB, currA).endVertex();

            consumer.vertex(matrix, nextX - offX2, nextY - offY2, z)

                    .color(currR, currG, currB, currA).endVertex();


// Advance walk state

            lastX = nextX;

            lastY = nextY;

            lastThickness = currentThickness;

            lastR = currR; lastG = currG; lastB = currB; lastA = currA;

        }

    }

}