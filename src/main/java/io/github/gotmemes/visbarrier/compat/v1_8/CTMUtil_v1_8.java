package io.github.gotmemes.visbarrier.compat.v1_8;

import io.github.gotmemes.visbarrier.CTMMath;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3i;
import net.minecraft.world.IBlockAccess;

/**
 * 1.8-specific neighbour lookup for CTM.
 * Pure tile math lives in {@link CTMMath}; this class handles MC API calls.
 *
 * <p>The 3x3x3 barrier neighbourhood is computed <b>once per block</b> ({@link #computeNeighbourhood},
 * 18 world lookups — the 8 pure 3D-corner cells are never queried by any face) and packed into an int.
 * Each face then reads its 8 in-plane neighbour flags out of that mask via {@link #faceFlags} with no
 * further world access, replacing the old 48-lookups-per-block / {@code boolean[8]}-per-face approach.
 */
public final class CTMUtil_v1_8 {
    private CTMUtil_v1_8() {}

    /**
     * Face-local axes derived from actual EnumFaceDirection vertex ordering.
     * [0] = U+ direction (texture right), [1] = V+ direction (texture down).
     *
     * DOWN  : U+=EAST,  V+=NORTH
     * UP    : U+=EAST,  V+=SOUTH
     * NORTH : U+=WEST,  V+=DOWN
     * SOUTH : U+=EAST,  V+=DOWN
     * WEST  : U+=SOUTH, V+=DOWN
     * EAST  : U+=NORTH, V+=DOWN
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

    /** Bit index of a relative cell (dx,dy,dz each in [-1,1]) within the packed 3x3x3 neighbourhood. */
    private static int cellIndex(int dx, int dy, int dz) {
        return (dx + 1) * 9 + (dy + 1) * 3 + (dz + 1);
    }

    /**
     * Computes the barrier-ness of the 18 face/edge neighbours around pos, once per block. The 8 pure
     * 3D-corner cells are skipped because every face's in-plane neighbours are at most 2D diagonals.
     */
    public static int computeNeighbourhood(IBlockAccess world, BlockPos pos) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        int mask = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;   // the block itself
                    if (dx != 0 && dy != 0 && dz != 0) continue;   // 3D corner: no face queries it
                    if (isBarrier(world, new BlockPos(x + dx, y + dy, z + dz))) {
                        mask |= 1 << cellIndex(dx, dy, dz);
                    }
                }
            }
        }
        return mask;
    }

    /**
     * Reads the 8 in-plane neighbour flags for one face out of the packed neighbourhood, using the
     * same face-local axes (and therefore the same world cells) the old per-face lookups used.
     * Returns them packed by the CTMMath RIGHT/LEFT/UP/DOWN/diagonal bit positions.
     */
    public static int faceFlags(int neighbourhood, EnumFacing face) {
        EnumFacing right = FACE_AXES[face.getIndex()][0];
        EnumFacing down  = FACE_AXES[face.getIndex()][1];
        EnumFacing left  = right.getOpposite();
        EnumFacing up    = down.getOpposite();

        int flags = 0;
        if (cell(neighbourhood, right))       flags |= 1 << CTMMath.RIGHT;
        if (cell(neighbourhood, left))        flags |= 1 << CTMMath.LEFT;
        if (cell(neighbourhood, up))          flags |= 1 << CTMMath.UP;
        if (cell(neighbourhood, down))        flags |= 1 << CTMMath.DOWN;
        if (cell(neighbourhood, right, up))   flags |= 1 << CTMMath.TOP_RIGHT;
        if (cell(neighbourhood, left,  up))   flags |= 1 << CTMMath.TOP_LEFT;
        if (cell(neighbourhood, right, down)) flags |= 1 << CTMMath.BOTTOM_RIGHT;
        if (cell(neighbourhood, left,  down)) flags |= 1 << CTMMath.BOTTOM_LEFT;
        return flags;
    }

    private static boolean cell(int mask, EnumFacing d) {
        Vec3i v = d.getDirectionVec();
        return (mask & (1 << cellIndex(v.getX(), v.getY(), v.getZ()))) != 0;
    }

    private static boolean cell(int mask, EnumFacing a, EnumFacing b) {
        Vec3i va = a.getDirectionVec(), vb = b.getDirectionVec();
        return (mask & (1 << cellIndex(va.getX() + vb.getX(), va.getY() + vb.getY(), va.getZ() + vb.getZ()))) != 0;
    }

    private static boolean isBarrier(IBlockAccess world, BlockPos pos) {
        return world.getBlockState(pos).getBlock() == Blocks.barrier;
    }
}
