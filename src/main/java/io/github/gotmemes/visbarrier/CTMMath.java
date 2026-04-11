package io.github.gotmemes.visbarrier;

/**
 * Pure CTM tile math — no Minecraft dependencies.
 * Neighbor flag indices are public so CTMUtil_vX_Y implementations can fill
 * the array using the same layout this class expects when reading it.
 */
public final class CTMMath {
    private CTMMath() {}

    // Tile indices matching ctm_compact format:
    // 0 = no neighbors       — full border on all sides
    // 1 = all neighbors      — no border (fully connected interior)
    // 2 = vertical neighbors — borders on left + right
    // 3 = horizontal neighbors — borders on top + bottom
    // 4 = H+V neighbors, no diagonal — corner notch
    public static final int TILE_ISOLATED   = 0;
    public static final int TILE_FULL       = 1;
    public static final int TILE_VERTICAL   = 2;
    public static final int TILE_HORIZONTAL = 3;
    public static final int TILE_CORNER     = 4;

    // Neighbor flag indices (relative to face-local UV axes)
    public static final int RIGHT        = 0;
    public static final int LEFT         = 1;
    public static final int UP           = 2;
    public static final int DOWN         = 3;
    public static final int TOP_RIGHT    = 4;
    public static final int TOP_LEFT     = 5;
    public static final int BOTTOM_RIGHT = 6;
    public static final int BOTTOM_LEFT  = 7;

    /**
     * Returns the tile index for each of the 4 quadrants [TL, TR, BL, BR].
     *
     * Quadrant layout in UV space (u=0 is left, v=0 is top):
     *   TL: checks LEFT  + UP   + TOP_LEFT
     *   TR: checks RIGHT + UP   + TOP_RIGHT
     *   BL: checks LEFT  + DOWN + BOTTOM_LEFT
     *   BR: checks RIGHT + DOWN + BOTTOM_RIGHT
     */
    public static int[] getQuadrantTiles(boolean[] n) {
        int[] tiles = new int[4];
        tiles[0] = getTile(n[LEFT],  n[UP],   n[TOP_LEFT]);
        tiles[1] = getTile(n[RIGHT], n[UP],   n[TOP_RIGHT]);
        tiles[2] = getTile(n[LEFT],  n[DOWN], n[BOTTOM_LEFT]);
        tiles[3] = getTile(n[RIGHT], n[DOWN], n[BOTTOM_RIGHT]);
        return tiles;
    }

    public static int getTile(boolean horiz, boolean vert, boolean diagonal) {
        if (!horiz && !vert) return TILE_ISOLATED;
        if ( horiz && !vert) return TILE_HORIZONTAL;
        if (!horiz &&  vert) return TILE_VERTICAL;
        if (diagonal)        return TILE_FULL;
        return TILE_CORNER;
    }
}
