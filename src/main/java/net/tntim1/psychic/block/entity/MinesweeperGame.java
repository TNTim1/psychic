package net.tntim1.psychic.block.entity;


import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import java.util.Random;

public class MinesweeperGame extends MiniGame {
    private int rows, cols, totalMines;
    private int[][] grid;
    private boolean[][] revealed, flagged;
    private boolean gameOver = false;

    @Override
    public void init(int difficulty) {
        if (difficulty == 0) { cols = 9; rows = 9; totalMines = 10; }
        else if (difficulty == 1) { cols = 16; rows = 16; totalMines = 40; }
        else { cols = 20; rows = 16; totalMines = 70; }

        grid = new int[cols][rows];
        revealed = new boolean[cols][rows];
        flagged = new boolean[cols][rows];
        gameOver = false;
        generateMines();
    }

    private void generateMines() {
        Random rand = new Random();
        int placed = 0;
        while (placed < totalMines) {
            int rx = rand.nextInt(cols), ry = rand.nextInt(rows);
            if (grid[rx][ry] != -1) { grid[rx][ry] = -1; placed++; }
        }
        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                if (grid[x][y] == -1) continue;
                grid[x][y] = countNeighbors(x, y);
            }
        }
    }

    private int countNeighbors(int cx, int cy) {
        int count = 0;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int nx = cx + i, ny = cy + j;
                if (nx >= 0 && nx < cols && ny >= 0 && ny < rows && grid[nx][ny] == -1) count++;
            }
        }
        return count;
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, int startX, int startY) {
        int gridStartX = startX + (250 - (cols * 10)) / 2;
        int gridStartY = startY + (220 - (rows * 10)) / 2;

        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                int dx = gridStartX + (x * 10), dy = gridStartY + (y * 10);
                if (!revealed[x][y]) {
                    graphics.fill(dx, dy, dx + 9, dy + 9, flagged[x][y] ? 0xFFFF0000 : 0xFFAAAAAA);
                } else {
                    graphics.fill(dx, dy, dx + 9, dy + 9, 0xFF444444);
                    if (grid[x][y] == -1) graphics.drawString(font, "M", dx + 2, dy + 1, 0xFF0000, false);
                    else if (grid[x][y] > 0) graphics.drawString(font, String.valueOf(grid[x][y]), dx + 3, dy + 1, 0xFFFFFF, false);
                }
            }
        }
    }

    @Override
    public void handleInput(double mouseX, double mouseY, int button, int startX, int startY) {
        int gridStartX = startX + (250 - (cols * 10)) / 2;
        int gridStartY = startY + (220 - (rows * 10)) / 2;
        int gx = (int) (mouseX - gridStartX) / 10, gy = (int) (mouseY - gridStartY) / 10;

        if (gx >= 0 && gx < cols && gy >= 0 && gy < rows) {
            if (button == 0) {
                if (grid[gx][gy] == -1) { gameOver = true; revealAll(); }
                else revealCell(gx, gy);
            } else if (button == 1) flagged[gx][gy] = !flagged[gx][gy];
        }
    }

    private void revealCell(int x, int y) {
        if (x < 0 || x >= cols || y < 0 || y >= rows || revealed[x][y] || flagged[x][y]) return;
        revealed[x][y] = true;
        if (grid[x][y] == 0) {
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) revealCell(x + i, y + j);
            }
        }
    }

    private void revealAll() {
        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) if (grid[x][y] == -1) revealed[x][y] = true;
        }
    }

    @Override
    public boolean isWon() {
        int count = 0;
        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) if (grid[x][y] != -1 && !revealed[x][y]) count++;
        }
        return count == 0;
    }

    @Override
    public boolean isLost() { return gameOver; }
}