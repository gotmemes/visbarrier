package io.github.gotmemes.visbarrier.ctm;

import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;

public final class CTMUtil {
    private CTMUtil() {}

    // Tile indices matching ctm_compact format:
    // 0 = no neighbors       — full border on all sides
    // 1 = all neighbors      — no border (fully connected interior)
    // 2 = vertical neighbors — borders on left + right (L/R edges exposed)
    // 3 = horizontal neighbors — borders on top + bottom (T/B edges exposed)
    // 4 = H+V neighbors, no diagonal — corner notch
    public static final int TILE_ISOLATED   = 0;
    public static final int TILE_FULL       = 1;
    public static final int TILE_VERTICAL   = 2;
    public static final int TILE_HORIZONTAL = 3;
    public static final int TILE_CORNER     = 4;

    // Neighbor flag indices (relative to face-local UV axes)
    private static final int RIGHT        = 0;
    private static final int LEFT         = 1;
    private static final int UP           = 2;
    private static final int DOWN         = 3;
    private static final int TOP_RIGHT    = 4;
    private static final int TOP_LEFT     = 5;
    private static final int BOTTOM_RIGHT = 6;
    private static final int BOTTOM_LEFT  = 7;

    /**
     * Face-local axes derived from actual EnumFaceDirection vertex ordering.
     * [0] = U+ direction (texture right), [1] = V+ direction (texture down).
     *
     * DOWN  : U+=EAST,  V+=NORTH  (V0=[0,0,1], V3=[1,0,1])
     * UP    : U+=EAST,  V+=SOUTH  (V0=[0,1,0], V3=[1,1,0])
     * NORTH : U+=WEST,  V+=DOWN   (V0=[1,1,0], V3=[0,1,0])
     * SOUTH : U+=EAST,  V+=DOWN   (V0=[0,1,1], V3=[1,1,1])
     * WEST  : U+=SOUTH, V+=DOWN   (V0=[0,1,0], V3=[0,1,1])
     * EAST  : U+=NORTH, V+=DOWN   (V0=[1,1,1], V3=[1,1,0])
     */
    private static final EnumFacing[][] FACE_AXES = new EnumFacing[6][2];

    static {
        FACE_AXES[EnumFacing.DOWN.getIndex()]  = new EnumFacing[]{EnumFacing.EAST,  EnumFacing.NORTH};
        FACE_AXES[EnumFacing.UP.getIndex()]    = new EnumFacing[]{EnumFacing.EAST,  EnumFacing.SOUTH};
        FACE_AXES[EnumFacing.NORTH.getIndex()] = new EnumFacing[]{EnumFacing.WEST,  EnumFacing.DOWN};
        FACE_AXES[EnumFacing.SOUTH.getIndex()] = new EnumFacing[]{EnumFacing.EAST,  EnumFacing.DOWN};
        FACE_AXES[EnumFacing.WEST.getIndex()]  = new EnumFacing[]{EnumFacing.SOUTH, EnumFacing.DOWN};
        FACE_AXES[EnumFacing.EAST.getIndex()]  = new EnumFacing[]{EnumFacing.NORTH, EnumFacing.DOWN};
    }

    /**
     * Checks the 8 in-plane neighbors of pos for barriers, relative to the given face.
     * Returns boolean[8] indexed by the RIGHT/LEFT/UP/DOWN/diagonal constants above.
     */
    public static boolean[] getNeighborFlags(IBlockAccess world, BlockPos pos, EnumFacing face) {
        EnumFacing rightDir = FACE_AXES[face.getIndex()][0];
        EnumFacing downDir  = FACE_AXES[face.getIndex()][1];
        EnumFacing leftDir  = rightDir.getOpposite();
        EnumFacing upDir    = downDir.getOpposite();

        boolean[] flags = new boolean[8];
        flags[RIGHT]        = isBarrier(world, pos.offset(rightDir));
        flags[LEFT]         = isBarrier(world, pos.offset(leftDir));
        flags[UP]           = isBarrier(world, pos.offset(upDir));
        flags[DOWN]         = isBarrier(world, pos.offset(downDir));
        flags[TOP_RIGHT]    = isBarrier(world, pos.offset(rightDir).offset(upDir));
        flags[TOP_LEFT]     = isBarrier(world, pos.offset(leftDir).offset(upDir));
        flags[BOTTOM_RIGHT] = isBarrier(world, pos.offset(rightDir).offset(downDir));
        flags[BOTTOM_LEFT]  = isBarrier(world, pos.offset(leftDir).offset(downDir));
        return flags;
    }

    /**
     * Returns the tile index for each of the 4 quadrants [TL, TR, BL, BR].
     *
     * Quadrant layout in UV space (u=0 is left, v=0 is top):
     *   TL: U=[0,8],  V=[0,8]  — checks LEFT  + UP   + TOP_LEFT
     *   TR: U=[8,16], V=[0,8]  — checks RIGHT + UP   + TOP_RIGHT
     *   BL: U=[0,8],  V=[8,16] — checks LEFT  + DOWN + BOTTOM_LEFT
     *   BR: U=[8,16], V=[8,16] — checks RIGHT + DOWN + BOTTOM_RIGHT
     */
    public static int[] getQuadrantTiles(boolean[] n) {
        int[] tiles = new int[4];
        // TL: horizontal=LEFT, vertical=UP
        tiles[0] = getTile(n[LEFT],  n[UP],   n[TOP_LEFT]);
        // TR: horizontal=RIGHT, vertical=UP
        tiles[1] = getTile(n[RIGHT], n[UP],   n[TOP_RIGHT]);
        // BL: horizontal=LEFT, vertical=DOWN
        tiles[2] = getTile(n[LEFT],  n[DOWN], n[BOTTOM_LEFT]);
        // BR: horizontal=RIGHT, vertical=DOWN
        tiles[3] = getTile(n[RIGHT], n[DOWN], n[BOTTOM_RIGHT]);
        return tiles;
    }

    /**
     * Picks a tile for one quadrant corner.
     *
     * @param horiz    horizontal cardinal neighbor connected (left for TL/BL, right for TR/BR)
     * @param vert     vertical cardinal neighbor connected   (up for TL/TR, down for BL/BR)
     * @param diagonal diagonal neighbor connected
     *
     * Tile semantics match ctm_compact 0-4:
     *   Neither connected         → 0 (full border: exposed both edges)
     *   Horizontal only           → 3 (T+B borders: L edge seamless, V edge exposed)
     *   Vertical only             → 2 (L+R borders: V edge seamless, H edge exposed)
     *   Both + diagonal           → 1 (no border: fully interior)
     *   Both, no diagonal         → 4 (corner notch: convex inner corner)
     */
    public static int getTile(boolean horiz, boolean vert, boolean diagonal) {
        if (!horiz && !vert)        return TILE_ISOLATED;
        if ( horiz && !vert)        return TILE_HORIZONTAL;
        if (!horiz &&  vert)        return TILE_VERTICAL;
        if (diagonal)               return TILE_FULL;
        return TILE_CORNER;
    }

    private static boolean isBarrier(IBlockAccess world, BlockPos pos) {
        return world.getBlockState(pos).getBlock() == Blocks.barrier;
    }
}
