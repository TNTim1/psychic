package net.tntim1.psychic.block.entity;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

import java.util.*;

/**
 * LaserMirrorGame – a Minecraft MiniGame ported from the Python "Laser Game".
 *
 * <h3>Piece types</h3>
 * <ul>
 *   <li><b>emitter</b>  – fires a laser in a fixed direction (always locked/static)</li>
 *   <li><b>mirror</b>   – reflects 90°; vertical dir (\) or horizontal dir (/)</li>
 *   <li><b>target</b>   – L-shaped corner piece; absorbs the beam from its valid-entry
 *                         side (win condition), bends and re-emits from the bend side</li>
 *   <li><b>splitter</b> – passes the beam straight through AND reflects a copy 90°</li>
 *   <li><b>arch</b>     – two parallel rails; lets the beam pass if aligned with its axis</li>
 *   <li><b>blocker</b>  – solid obstacle; lets nothing through (used as wall segments)</li>
 * </ul>
 *
 * <h3>Direction encoding</h3>
 * <pre>
 *   UP    = { 0, -1 }   RIGHT = { 1,  0 }
 *   DOWN  = { 0,  1 }   LEFT  = {-1,  0 }
 * </pre>
 *
 * <h3>Difficulties</h3>
 * <pre>
 *   0 – Easy   : mirrors + 1 locked target, open grid, 1 emitter
 *   1 – Medium : mirrors + splitter + arch + locked target, central walls, 1 emitter
 *   2 – Hard   : all piece types, 3 locked targets cross-wired, 3 emitters, maze walls
 * </pre>
 *
 * <h3>Win condition (mirrors Python check_win)</h3>
 * <ul>
 *   <li>All locked targets activated</li>
 *   <li>All arches activated</li>
 *   <li>Entire inventory spent (nothing left to place)</li>
 *   <li>Every non-emitter piece on the board activated</li>
 *   <li>Total targets hit ≥ target goal</li>
 * </ul>
 */
public class LaserMirrorGame extends MiniGame {

    // =========================================================================
    // Direction constants
    // =========================================================================

    private static final int[] UP    = { 0, -1};
    private static final int[] RIGHT = { 1,  0};
    private static final int[] DOWN  = { 0,  1};
    private static final int[] LEFT  = {-1,  0};

    /** Clockwise rotation order used by Piece.rotate(). */
    private static final int[][] DIR_ORDER = {UP, RIGHT, DOWN, LEFT};

    // =========================================================================
    // Piece
    // =========================================================================

    private static class Piece {
        final String type;
        int[]   dir;
        boolean isStatic;   // drawn in gold; cannot be rotated
        boolean isLocked;   // cannot be moved or rotated
        Set<Integer> hitColors;  // beam visited this cell this frame
        Set<Integer> activatedColors;  // beam used this piece in the intended way
        Set<Integer> expectedColors;
        int color;

        Piece(String type, int[] dir, boolean isStatic, boolean isLocked, int color) {
            this.type     = type;
            this.dir      = dir;
            this.isStatic = isStatic;
            this.isLocked = isLocked;
            this.color = color;
            this.hitColors = new HashSet<>();
            this.activatedColors = new HashSet<>();
            this.expectedColors = new HashSet<>();
        }

        /** Rotate 90° clockwise. No-op for static or locked pieces. */
        void rotate() {
            // Only "locked" pieces are truly frozen in place.
            // "Static" pieces can now be rotated by the player.
            if (isLocked) return;

            for (int i = 0; i < DIR_ORDER.length; i++) {
                if (Arrays.equals(DIR_ORDER[i], dir)) {
                    dir = DIR_ORDER[(i + 1) % 4];
                    return;
                }
            }
        }

        /**
         * Returns all outgoing beam directions given {@code incoming}, and updates
         * {@link #hit} / {@link #activated} flags.  Empty list = beam absorbed.
         */
        List<int[]> getOutputs(int[] incoming, int color) {
            hitColors.add(color);
            List<int[]> out = new ArrayList<>();

            switch (type) {

                // ── Mirror ────────────────────────────────────────────────
                case "mirror": {
                    boolean vertical = Arrays.equals(dir, UP) || Arrays.equals(dir, DOWN);
                    int[] reflected = mirrorReflect(incoming, vertical);
                    if (reflected != null) { activatedColors.add(color); out.add(reflected); }
                    break;
                }

                // ── Target ────────────────────────────────────────────────
                case "target": {
                    // 1. Check for Target Activation (Frontal Hit)
                    if (Arrays.equals(incoming, dir)) {
                        activatedColors.add(color);
                        // Beam absorbed by the target bullseye
                    } else {
                        // 2. Corner Mirror Logic
                        // We define the two reflecting sides based on the 'back corner' of the piece.
                        int[] mirrorSideA, mirrorSideB;

                        if (Arrays.equals(dir, RIGHT)) {
                            mirrorSideA = UP;    mirrorSideB = LEFT;
                        } else if (Arrays.equals(dir, DOWN)) {
                            mirrorSideA = RIGHT;  mirrorSideB = UP;
                        } else if (Arrays.equals(dir, LEFT)) {
                            mirrorSideA = DOWN;  mirrorSideB = RIGHT;
                        } else { // UP
                            mirrorSideA = LEFT; mirrorSideB = DOWN;
                        }

                        // 3. Perform the Reflective Swap
                        if (Arrays.equals(incoming, mirrorSideA)) {
                            // Laser enters from Side A, exits through Side B
                            out.add(new int[]{-mirrorSideB[0], -mirrorSideB[1]});
                        } else if (Arrays.equals(incoming, mirrorSideB)) {
                            // Laser enters from Side B, exits through Side A
                            out.add(new int[]{-mirrorSideA[0], -mirrorSideA[1]});
                        }
                        // If it hits the 4th side (the 'dead' side), out remains empty.
                    }
                    break;
                }
                // ── Splitter ──────────────────────────────────────────────
                case "splitter": {
                    activatedColors.add(color);
                    out.add(incoming);   // straight-through copy
                    boolean vertical = Arrays.equals(dir, UP) || Arrays.equals(dir, DOWN);
                    int[] reflected = mirrorReflect(incoming, vertical);
                    if (reflected != null) out.add(reflected);   // reflected copy
                    break;
                }

                // ── Arch ──────────────────────────────────────────────────
                case "arch": {
                    int[] opposite = {-dir[0], -dir[1]};
                    if (Arrays.equals(incoming, dir) || Arrays.equals(incoming, opposite)) {
                        activatedColors.add(color);
                        out.add(incoming);   // passes aligned beams unchanged
                    }
                    // misaligned beams are blocked (out stays empty)
                    break;
                }

                // ── Blocker ───────────────────────────────────────────────
                // Inside Piece.getOutputs(int[] incoming) switch statement:

                case "blocker": {
                    activatedColors.add(color);
                    // Changed: instead of absorbing, pass the beam straight through
                    out.add(incoming);
                    break;
                }
                case "emitter": {

                    // Only activate if beam enters from the FRONT (emitting side)
                    if (Arrays.equals(incoming, dir)) {
                        activatedColors.add(color);
                    }
                    out.add(incoming); // still pass through
                    break;
                }
                // ── Emitter (beam passes through the source cell) ─────────
                default:
                    out.add(incoming);
            }
            return out;
        }

        /**
         * Core mirror reflection.
         * <ul>
         *   <li>vertical (true)  → "\" : RIGHT↔UP,  LEFT↔DOWN</li>
         *   <li>vertical (false) → "/" : RIGHT↔DOWN, LEFT↔UP</li>
         * </ul>
         */
        private static int[] mirrorReflect(int[] in, boolean vertical) {
            if (vertical) {
                if (Arrays.equals(in, RIGHT)) return UP;
                if (Arrays.equals(in, UP))    return RIGHT;
                if (Arrays.equals(in, LEFT))  return DOWN;
                if (Arrays.equals(in, DOWN))  return LEFT;
            } else {
                if (Arrays.equals(in, RIGHT)) return DOWN;
                if (Arrays.equals(in, DOWN))  return RIGHT;
                if (Arrays.equals(in, LEFT))  return UP;
                if (Arrays.equals(in, UP))    return LEFT;
            }
            return null;
        }
    }

    // =========================================================================
    // Level definition helpers
    // =========================================================================

 // =========================================================================
    // Game state fields
    // =========================================================================

    /** grid[col][row], null = empty cell. */
    private Piece[][] grid;
    private int gridCols, gridRows;

    /** Inventory of pieces available to place. */
    private final Map<String, Integer> inventory = new LinkedHashMap<>();

    /** Which piece type the player has selected in the HUD. */
    private String selectedType = "mirror";

    /** Laser segments collected this frame: { x1, y1, x2, y2, argb }. */
    private final List<int[]> laserSegments = new ArrayList<>();

    // Win-condition counters (updated by checkWin each frame)
    private int  targetsHit;
    private int  archesHit;
    private int  archesTotal;
    private int  targetGoal;
    private boolean won;

    // =========================================================================
    // MiniGame — init
    // =========================================================================

    @Override
    public void init(int difficulty) {
        this.gridCols = SIZE;
        this.gridRows = SIZE;
        this.grid = new Piece[SIZE][SIZE];

        this.won = false;

        emitters.clear();
        goalConnections.clear();

        // Colors (ARGB)
        int[] colors = {
                0xFFFF3232, // 1
                0xFFFFA500, // 2
                0xFFFFFF32, // 3
                0xFF32FF32, // 4
                0xFF32FFFF, // 5
                0xFF3264FF, // 6
                0xFFAA32FF, // 7
                0xFFFF32AA  // 8
        };
        inventory.clear();
        inventory.put("mirror", 99);
        inventory.put("splitter", 99);

        int id = 1;

        // LEFT SIDE (bottom → top)
        for (int y = SIZE - 1; y >= 0; y -= 2) {
            emitters.add(new LaserNode(0, y, RIGHT, colors[id - 1], id));
            grid[0][y] = new Piece("emitter", RIGHT, true, true, colors[id - 1]);
            id++;
        }

        // RIGHT SIDE (top → bottom)
        for (int y = 0; y < SIZE; y += 2) {
            emitters.add(new LaserNode(SIZE - 1, y, LEFT, colors[id - 1], id));
            grid[SIZE - 1][y] = new Piece("emitter", LEFT, true, true, colors[id - 1]);
            id++;
        }

        // Example goal:
        goalConnections.add(Arrays.asList(1, 6));
        goalConnections.add(Arrays.asList(2, 5));
    }



    // =========================================================================
    // MiniGame — render
    // =========================================================================

    /** Pixels per grid cell. */
    private static final int CELL = 16;

    @Override
    public void render(GuiGraphics g, Font font, int mouseX, int mouseY, int startX, int startY) {
        int gX = startX + 4;
        int gY = startY + 24;

        // Trace lasers first so hit/activated flags are set before drawing pieces
        traceLaser(gX, gY);

        // ── Grid ──────────────────────────────────────────────────────────────
        for (int x = 0; x < gridCols; x++) {
            for (int y = 0; y < gridRows; y++) {
                int px = gX + x * CELL;
                int py = gY + y * CELL;

                // Faint cell background
                g.fill(px, py, px + CELL - 1, py + CELL - 1, 0x1AFFFFFF);

                Piece p = grid[x][y];
                if (p != null) drawPiece(g, p, px, py);
            }
        }

        // ── Laser beams (drawn on top of pieces) ──────────────────────────────
        for (int[] seg : laserSegments) {
            drawGradientLine(g,
                    seg[0], seg[1], seg[2], seg[3],
                    seg[4], seg[4],
                    1.8f, 0f, 1.0f, 0.3f, 3);
        }

        // ── HUD panel (right of grid) ─────────────────────────────────────────
        int uiX = gX + gridCols * CELL + 6;
        int uiY = gY;

        // Targets
        g.drawString(font, "Connections:", uiX, uiY, 0xFFAAAAAA);

        int cy = uiY + 10;

        for (List<Integer> group : goalConnections) {
            StringBuilder line = new StringBuilder();

            for (int i = 0; i < group.size(); i++) {
                line.append(group.get(i));
                if (i < group.size() - 1) line.append(" ↔ ");
            }

            // Check if this group is satisfied
            boolean ok = true;
            for (int i = 0; i < group.size(); i++) {
                for (int j = i + 1; j < group.size(); j++) {
                    if (!activeConnections.contains(connectionKey(group.get(i), group.get(j)))) {
                        ok = false;
                        break;
                    }
                }
            }

            int col = ok ? 0xFF32FF96 : 0xFFFF5555;
            g.drawString(font, line.toString(), uiX, cy, col);
            cy += 10;
        }

        // Inventory list — clickable rows
        int iy = uiY + 48;
        for (Map.Entry<String, Integer> e : inventory.entrySet()) {
            boolean sel  = e.getKey().equals(selectedType);
            int     col  = sel ? 0xFFFFFFFF : 0xFF7888A0;
            String  name = e.getKey();
            String  abbr = name.substring(0, Math.min(4, name.length())).toUpperCase();
            if (sel) g.fill(uiX - 2, iy - 1, uiX + 70, iy + 8, 0x33FFFFFF);
            g.drawString(font, abbr + ": " + e.getValue(), uiX, iy, col);
            iy += 11;
        }

        // Win banner
        if (won) g.drawString(font, "LEVEL CLEAR!", uiX, uiY + gridRows * CELL - 10, 0xFF32FF96);

        // Mirrors-left hint (convenience display)
        int totalLeft = inventory.values().stream().mapToInt(Integer::intValue).sum();
        g.drawString(font, "Left: " + totalLeft,
                gX, gY - 13,
                totalLeft == 0 ? 0xFFFF5555 : 0xFFFFFFFF);
    }

    // =========================================================================
    // Piece rendering  (mirrors Python draw_piece())
    // =========================================================================

    private static final int COL_STATIC = 0xFFFFBE00;  // gold
    private static final int COL_PIECE  = 0xFFC8D2E6;  // light blue-grey
    private static final int COL_ACTIVE = 0xFF32FF96;  // green
    private static final int COL_LASER  = 0xFFFF3232;  // red

    private static final int SIZE = 8;

    // Laser endpoints
    private static class LaserNode {
        int x, y;
        int[] dir;
        int color;
        int id;

        LaserNode(int x, int y, int[] dir, int color, int id) {
            this.x = x;
            this.y = y;
            this.dir = dir;
            this.color = color;
            this.id = id;
        }
    }

    private final List<LaserNode> emitters = new ArrayList<>();

    // Required goal connections
    private final List<List<Integer>> goalConnections = new ArrayList<>();

    // Actual connections found this frame
    private final Set<String> activeConnections = new HashSet<>();

    private void drawPiece(GuiGraphics g, Piece p, int px, int py) {
        int cx = px + CELL / 2;
        int cy = py + CELL / 2;
        int s  = CELL / 4;            // "small" offset used by several pieces

        // Glow rule: only targets and arches change colour when activated
        boolean glowing = ("target".equals(p.type) || "arch".equals(p.type)) && !p.activatedColors.isEmpty();
        int baseCol = p.isStatic ? COL_STATIC : COL_PIECE;
        int drawCol = glowing ? COL_ACTIVE : baseCol;

        // Lock badge (small dark square bottom-right corner)
        if (p.isLocked) g.fill(px + CELL - 5, py + CELL - 4, px + CELL - 1, py + CELL - 1, 0xFF646478);

        switch (p.type) {

            // ── Emitter ───────────────────────────────────────────────────
            case "emitter": {
                int emitterColor = p.color;
                g.fill(px + 3, py + 3, px + CELL - 3, py + CELL - 3, emitterColor);

                g.fill(px + 3, py + 3, px + CELL - 3, py + CELL - 3, emitterColor);
                // Direction arrow

                int ax = cx + p.dir[0] * (CELL / 2 - 2);
                int ay = cy + p.dir[1] * (CELL / 2 - 2);
                drawGradientLine(g, cx, cy, ax, ay, 0xFFFFAAAA, 0xFFFFFFFF, 1.2f, 0f, 1f, 0f, 1);
                break;
            }

            // ── Mirror ────────────────────────────────────────────────────
            case "mirror": {
                // Vertical dir (\) or horizontal dir (/)
                boolean bslash = Arrays.equals(p.dir, UP) || Arrays.equals(p.dir, DOWN);
                if (bslash)
                    drawGradientLine(g, px+2, py+CELL-3, px+CELL-3, py+2,
                            drawCol, brighten(drawCol), 1.4f, 0f, 1f, 0f, 1);
                else
                    drawGradientLine(g, px+2, py+2, px+CELL-3, py+CELL-3,
                            drawCol, brighten(drawCol), 1.4f, 0f, 1f, 0f, 1);
                break;
            }

            // ── Target ────────────────────────────────────────────────────
            case "target": {
                // Two arms of the "L" meeting at the centre
                int[] arm1, arm2, nodeSide;
                if      (Arrays.equals(p.dir, RIGHT)) { arm1=LEFT;  arm2=DOWN;  nodeSide=RIGHT; }
                else if (Arrays.equals(p.dir, DOWN))  { arm1=UP;    arm2=LEFT;  nodeSide=DOWN;  }
                else if (Arrays.equals(p.dir, LEFT))  { arm1=RIGHT; arm2=UP;    nodeSide=LEFT;  }
                else                                   { arm1=DOWN;  arm2=RIGHT; nodeSide=UP;    }

                drawGradientLine(g, cx, cy, cx-arm1[0]*s, cy-arm1[1]*s, drawCol, drawCol, 1.5f, 0f, 1f, 0f, 1);
                drawGradientLine(g, cx, cy, cx+arm2[0]*s, cy+arm2[1]*s, drawCol, drawCol, 1.5f, 0f, 1f, 0f, 1);

                // Entry-node dot
                int nx = cx - nodeSide[0] * 4;
                int ny = cy - nodeSide[1] * 4;
                if (p.activatedColors.isEmpty()) {
                    g.fill(nx - 2, ny - 2, nx + 2, ny + 2, 0xFF3C3C46);
                } else {
                    drawColorDots(g, p.activatedColors, nx, ny);
                }
                g.fill(nx - 2, ny - 2, nx + 2, ny + 2,0xFF3C3C46);
                break;
            }

            // ── Splitter ──────────────────────────────────────────────────
            case "splitter": {
                // Crosshair
                g.fill(cx - s, cy - 1, cx + s, cy + 1, drawCol);
                g.fill(cx - 1, cy - s, cx + 1, cy + s, drawCol);
                // Corner dots (circle approximation)
                g.fill(cx-s, cy-s, cx-s+2, cy-s+2, drawCol);
                g.fill(cx+s-2, cy-s, cx+s, cy-s+2, drawCol);
                g.fill(cx-s, cy+s-2, cx-s+2, cy+s, drawCol);
                g.fill(cx+s-2, cy+s-2, cx+s, cy+s, drawCol);
                // Direction pointer
                int pCol = drawCol;
                drawGradientLine(g, cx, cy, cx+p.dir[0]*s, cy+p.dir[1]*s, pCol, pCol, 2.0f, 0f, 1f, 0f, 1);
                break;
            }

            // ── Arch ──────────────────────────────────────────────────────
            case "arch": {
                boolean horiz = Arrays.equals(p.dir, RIGHT) || Arrays.equals(p.dir, LEFT);
                if (horiz) {
                    g.fill(px+2, cy-3, px+CELL-3, cy-1, drawCol);
                    g.fill(px+2, cy+1, px+CELL-3, cy+3, drawCol);
                } else {
                    g.fill(cx-3, py+2, cx-1, py+CELL-3, drawCol);
                    g.fill(cx+1, py+2, cx+3, py+CELL-3, drawCol);
                }
                break;
            }

            // ── Blocker / wall ────────────────────────────────────────────
            case "blocker": {
                g.fill(px+2, py+2, px+CELL-3, py+CELL-3, 0xFF282D41);
                // Subtle top-edge highlight to suggest depth
                g.fill(px+2, py+2, px+CELL-3, py+4, 0xFF3A3F58);
                break;
            }
        }
    }
    private void drawColorDots(GuiGraphics g, Set<Integer> colors, int cx, int cy) {
        if (colors.isEmpty()) return;

        int size = 3;   // pixel size
        int spacing = 4;

        int i = 0;
        for (int color : colors) {
            int dx = (i % 3) * spacing - spacing;
            int dy = (i / 3) * spacing - spacing;

            g.fill(cx + dx, cy + dy, cx + dx + size, cy + dy + size, color);
            i++;

            if (i >= 9) break; // limit to 9 dots (3x3 grid)
        }
    }

    // =========================================================================
    // Laser tracing  (BFS, mirrors Python trace_laser())
    // =========================================================================
    private int getEmitterId(int x, int y) {
        for (LaserNode n : emitters) {
            if (n.x == x && n.y == y) return n.id;
        }
        return -1;
    }

    private String connectionKey(int a, int b) {
        return (Math.min(a,b)) + "-" + (Math.max(a,b));
    }

    private void traceLaser(int gX, int gY) {
        laserSegments.clear();
        activeConnections.clear();

        // Reset all piece flags
        for (int x = 0; x < gridCols; x++)
            for (int y = 0; y < gridRows; y++)
                if (grid[x][y] != null) { grid[x][y].hitColors.clear(); grid[x][y].activatedColors.clear(); }

        // Seed the queue with every emitter on the board
        Queue<int[]> queue = new ArrayDeque<>();  // { cx, cy, dx, dy, argb }
        for (int x = 0; x < gridCols; x++) {
            for (int y = 0; y < gridRows; y++) {
                Piece p = grid[x][y];
                if (p != null && "emitter".equals(p.type)) {
                    int id = getEmitterId(x, y);
                    LaserNode node = emitters.stream().filter(n -> n.id == id).findFirst().orElse(null);
                    if (node != null) {
                        queue.add(new int[]{ x, y, p.dir[0], p.dir[1], node.color });
                    }
                }
            }
        }

        // Visited set prevents infinite cycles (e.g. two facing mirrors)
        Set<Long> visited = new HashSet<>();


        while (!queue.isEmpty()) {
            int[] start = queue.poll();
            int cx = start[0], cy = start[1];
            int[] d = { start[2], start[3] };
            int argb = start[4];

            // Walk straight until we leave the grid
            while (cx >= 0 && cx < gridCols && cy >= 0 && cy < gridRows) {

                long key = ((long) cx << 40) | ((long) cy << 20) | ((long)(d[0]+2) << 10) | (d[1]+2);
                if (visited.contains(key)) break;
                visited.add(key);

                Piece piece = grid[cx][cy];

                int cpx = gX + cx * CELL + CELL / 2;
                int cpy = gY + cy * CELL + CELL / 2;

                int sx = cpx - d[0] * CELL / 2;
                int sy = cpy - d[1] * CELL / 2;

                // ─────────────────────────────
                // DRAW INCOMING (edge → center)
                // ─────────────────────────────
                if (piece == null) {
                    int ex = cpx + d[0] * CELL / 2;
                    int ey = cpy + d[1] * CELL / 2;

                    // DRAW BOTH HALVES
                    laserSegments.add(new int[]{ sx, sy, cpx, cpy, argb }); // edge → center
                    laserSegments.add(new int[]{ cpx, cpy, ex, ey, argb }); // center → edge

                    cx += d[0];
                    cy += d[1];
                    continue;
                }

                // ─────────────────────────────
                // CONNECTION DETECTION (FIXED)
                // ─────────────────────────────
                if (piece != null && "emitter".equals(piece.type)) {
                    int targetId = getEmitterId(cx, cy);
                    int sourceId = getEmitterId(start[0], start[1]);

                    if (sourceId != targetId) {
                        activeConnections.add(connectionKey(sourceId, targetId));
                    }
                }
                // ─────────────────────────────
                // HANDLE PIECE
                // ─────────────────────────────
                if (piece != null) {

                    // 🟢 ALWAYS draw incoming (edge → center)
                    laserSegments.add(new int[]{ sx, sy, cpx, cpy, argb });

                    List<int[]> outputs = piece.getOutputs(d, argb);

                    // If absorbed → stop AFTER drawing incoming
                    if (outputs.isEmpty()) break;

                    for (int i = 0; i < outputs.size(); i++) {
                        int[] outDir = outputs.get(i);

                        int ex = cpx + outDir[0] * CELL / 2;
                        int ey = cpy + outDir[1] * CELL / 2;

                        // 🔵 OUTGOING (center → edge)
                        laserSegments.add(new int[]{ cpx, cpy, ex, ey, argb });

                        if (i == 0) {
                            d = outDir;
                        } else {
                            queue.add(new int[]{
                                    cx + outDir[0],
                                    cy + outDir[1],
                                    outDir[0],
                                    outDir[1],
                                    argb
                            });
                        }
                    }

                    cx += d[0];
                    cy += d[1];
                    continue;
                }

                // ─────────────────────────────
                // EMPTY TILE → continue straight
                // ─────────────────────────────
                cx += d[0];
                cy += d[1];
            }
        }

        checkWin();
    }

    /** Maps emitter direction to its beam ARGB colour. */
    private static int emitterArgb(int[] dir) {
        if (Arrays.equals(dir, RIGHT)) return 0xCCFF3232;  // red
        if (Arrays.equals(dir, LEFT))  return 0xCC3264FF;  // blue
        if (Arrays.equals(dir, DOWN))  return 0xCC32C832;  // green
        return                                0xCCFFBE00;  // yellow (UP)
    }

    // =========================================================================
    // Win / loss  (mirrors Python check_win())
    // =========================================================================

    private void checkWin() {
        Set<String> required = new HashSet<>();

        // Build required connection set
        for (List<Integer> group : goalConnections) {
            for (int i = 0; i < group.size(); i++) {
                for (int j = i + 1; j < group.size(); j++) {
                    required.add(connectionKey(group.get(i), group.get(j)));
                }
            }
        }

        // 1. Check all required connections exist
        for (String key : required) {
            if (!activeConnections.contains(key)) {
                won = false;
                return;
            }
        }

        // 2. Check NO extra connections exist
        for (String key : activeConnections) {
            if (!required.contains(key)) {
                won = false;
                return;
            }
        }

        won = true;
    }

    @Override
    public boolean isWon() { return won; }

    /**
     * No hard-fail state: the player can always rotate or pick up unlocked
     * pieces to try a different configuration.
     */
    @Override
    public boolean isLost() { return false; }

    // =========================================================================
    // MiniGame — handleInput
    // =========================================================================

    /**
     * Input handling mirrors Python handle_click():
     * <ul>
     *   <li>Click inside HUD panel (right of grid)  → change selected piece type</li>
     *   <li>Left-click  (button 0) on occupied cell → rotate (if not locked)</li>
     *   <li>Left-click  (button 0) on empty cell    → place selected type from inventory</li>
     *   <li>Right-click (button 1) on occupied cell → pick up (if not locked), return to inventory</li>
     * </ul>
     */
    @Override
    public void handleInput(double mouseX, double mouseY, int button, int startX, int startY) {
        int gX = startX + 4;
        int gY = startY + 24;

        int gx = (int)(mouseX - gX) / CELL;
        int gy = (int)(mouseY - gY) / CELL;

        // ── HUD panel ─────────────────────────────────────────────────────────
        if (gx >= gridCols) {
            int uiX = gX + gridCols * CELL + 6;
            int uiY = gY + 48;
            int row = (int)(mouseY - uiY) / 11;
            List<String> keys = new ArrayList<>(inventory.keySet());
            if (row >= 0 && row < keys.size()) selectedType = keys.get(row);
            return;
        }

        if (gx < 0 || gx >= gridCols || gy < 0 || gy >= gridRows) return;

        Piece existing = grid[gx][gy];

        if (button == 0) {
            // Left-click: rotate existing unlocked piece, or place from inventory
            if (existing != null) {
                existing.rotate();
            } else {
                int stock = inventory.getOrDefault(selectedType, 0);
                if (stock > 0) {
                    grid[gx][gy] = new Piece(selectedType, RIGHT, false, false, 0xffffff);
                    inventory.put(selectedType, stock - 1);
                }
            }
        } else if (button == 1) {
            // Right-click: pick up unlocked piece and return to inventory
            if (existing != null && !existing.isLocked&& !existing.isStatic) {
                inventory.merge(existing.type, 1, Integer::sum);
                grid[gx][gy] = null;
            }
        }
    }

    // =========================================================================
    // Utilities
    // =========================================================================

    /** Mixes an ARGB colour 50% toward white (used for the beam highlight). */
    private static int brighten(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = Math.min(255, (((argb >> 16) & 0xFF) + 255) / 2);
        int g = Math.min(255, (((argb >>  8) & 0xFF) + 255) / 2);
        int b = Math.min(255, (( argb        & 0xFF) + 255) / 2);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    // =========================================================================
    // Gradient line renderer  (unchanged from original LaserMirrorGame)
    // =========================================================================

    private void drawGradientLine(GuiGraphics guiGraphics,
                                  float x1, float y1, float x2, float y2,
                                  int color1, int color2,
                                  float thicknessMult, float z,
                                  float alphaMult, float jitter, int segments) {

        if (Math.abs(x1 - x2) < 0.01f && Math.abs(y1 - y2) < 0.01f) return;

        Matrix4f       matrix   = guiGraphics.pose().last().pose();
        VertexConsumer consumer = guiGraphics.bufferSource().getBuffer(RenderType.gui());

        float r1 = ((color1 >> 16) & 0xFF) / 255f,  g1 = ((color1 >>  8) & 0xFF) / 255f;
        float b1 = ( color1        & 0xFF) / 255f,   a1 = (((color1 >> 24) & 0xFF) / 255f) * alphaMult;
        float r2 = ((color2 >> 16) & 0xFF) / 255f,  g2 = ((color2 >>  8) & 0xFF) / 255f;
        float b2 = ( color2        & 0xFF) / 255f,   a2 = (((color2 >> 24) & 0xFF) / 255f) * alphaMult;

        float sdx = x2 - x1, sdy = y2 - y1, len = (float) Math.sqrt(sdx * sdx + sdy * sdy);
        float nx = -sdy / len, ny = sdx / len;

        long frameSeed = System.currentTimeMillis() / 40;
        java.util.Random rand = new java.util.Random();

        float lastX = x1, lastY = y1, lastT = thicknessMult;
        float lastR = r1, lastG = g1, lastB = b1, lastA = a1;

        for (int i = 1; i <= segments; i++) {
            float t  = (float) i / segments;
            float nx2 = x1 + sdx * t, ny2 = y1 + sdy * t, ct = thicknessMult;
            if (i < segments && jitter > 0f) {
                rand.setSeed(frameSeed + i + (long)(x1 * 100));
                nx2 += (rand.nextFloat() - 0.5f) * jitter;
                ny2 += (rand.nextFloat() - 0.5f) * jitter;
                ct  *= (0.8f + rand.nextFloat() * 0.4f);
            }
            float cr = r1+(r2-r1)*t, cg = g1+(g2-g1)*t, cb = b1+(b2-b1)*t, ca = a1+(a2-a1)*t;
            float o1x = nx*(lastT/2f), o1y = ny*(lastT/2f);
            float o2x = nx*(ct/2f),   o2y = ny*(ct/2f);

            consumer.vertex(matrix, lastX-o1x, lastY-o1y, z).color(lastR,lastG,lastB,lastA).endVertex();
            consumer.vertex(matrix, lastX+o1x, lastY+o1y, z).color(lastR,lastG,lastB,lastA).endVertex();
            consumer.vertex(matrix, nx2 +o2x,  ny2 +o2y,  z).color(cr,   cg,   cb,   ca   ).endVertex();
            consumer.vertex(matrix, nx2 -o2x,  ny2 -o2y,  z).color(cr,   cg,   cb,   ca   ).endVertex();

            lastX=nx2; lastY=ny2; lastT=ct; lastR=cr; lastG=cg; lastB=cb; lastA=ca;
        }
    }
}