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
     * Returns the tile index for one quadrant, reading the three relevant neighbour bits out of a
     * packed neighbour mask (bit positions are the RIGHT/LEFT/UP/DOWN/diagonal constants above).
     *
     * Quadrant layout in UV space (u=0 is left, v=0 is top):
     *   TL: tile(mask, LEFT,  UP,   TOP_LEFT)
     *   TR: tile(mask, RIGHT, UP,   TOP_RIGHT)
     *   BL: tile(mask, LEFT,  DOWN, BOTTOM_LEFT)
     *   BR: tile(mask, RIGHT, DOWN, BOTTOM_RIGHT)
     *
     * Replaces the old boolean[]/int[] form so the render hot path allocates nothing per face.
     */
    public static int tile(int neighbourMask, int horizBit, int vertBit, int diagBit) {
        return getTile(
                (neighbourMask & (1 << horizBit)) != 0,
                (neighbourMask & (1 << vertBit))  != 0,
                (neighbourMask & (1 << diagBit))  != 0);
    }

    public static int getTile(boolean horiz, boolean vert, boolean diagonal) {
        if (!horiz && !vert) return TILE_ISOLATED;
        if ( horiz && !vert) return TILE_HORIZONTAL;
        if (!horiz &&  vert) return TILE_VERTICAL;
        if (diagonal)        return TILE_FULL;
        return TILE_CORNER;
    }
}
