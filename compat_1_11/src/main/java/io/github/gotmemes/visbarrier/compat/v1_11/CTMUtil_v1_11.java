package io.github.gotmemes.visbarrier.compat.v1_11;

import io.github.gotmemes.visbarrier.CTMMath;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * 1.11-specific neighbor lookup for CTM.
 * Pure tile math lives in {@link CTMMath}; this class handles MC API calls.
 */
public final class CTMUtil_v1_11 {
    private CTMUtil_v1_11() {}

    private static final EnumFacing[][] FACE_AXES = new EnumFacing[6][2];

    static {
        FACE_AXES[EnumFacing.DOWN.getIndex()]  = new EnumFacing[]{EnumFacing.EAST,  EnumFacing.NORTH};
        FACE_AXES[EnumFacing.UP.getIndex()]    = new EnumFacing[]{EnumFacing.EAST,  EnumFacing.SOUTH};
        FACE_AXES[EnumFacing.NORTH.getIndex()] = new EnumFacing[]{EnumFacing.WEST,  EnumFacing.DOWN};
        FACE_AXES[EnumFacing.SOUTH.getIndex()] = new EnumFacing[]{EnumFacing.EAST,  EnumFacing.DOWN};
        FACE_AXES[EnumFacing.WEST.getIndex()]  = new EnumFacing[]{EnumFacing.SOUTH, EnumFacing.DOWN};
        FACE_AXES[EnumFacing.EAST.getIndex()]  = new EnumFacing[]{EnumFacing.NORTH, EnumFacing.DOWN};
    }

    public static boolean[] getNeighborFlags(IBlockAccess world, BlockPos pos, EnumFacing face) {
        EnumFacing rightDir = FACE_AXES[face.getIndex()][0];
        EnumFacing downDir  = FACE_AXES[face.getIndex()][1];
        EnumFacing leftDir  = rightDir.getOpposite();
        EnumFacing upDir    = downDir.getOpposite();

        boolean[] flags = new boolean[8];
        flags[CTMMath.RIGHT]        = isBarrier(world, pos.offset(rightDir));
        flags[CTMMath.LEFT]         = isBarrier(world, pos.offset(leftDir));
        flags[CTMMath.UP]           = isBarrier(world, pos.offset(upDir));
        flags[CTMMath.DOWN]         = isBarrier(world, pos.offset(downDir));
        flags[CTMMath.TOP_RIGHT]    = isBarrier(world, pos.offset(rightDir).offset(upDir));
        flags[CTMMath.TOP_LEFT]     = isBarrier(world, pos.offset(leftDir).offset(upDir));
        flags[CTMMath.BOTTOM_RIGHT] = isBarrier(world, pos.offset(rightDir).offset(downDir));
        flags[CTMMath.BOTTOM_LEFT]  = isBarrier(world, pos.offset(leftDir).offset(downDir));
        return flags;
    }

    private static boolean isBarrier(IBlockAccess world, BlockPos pos) {
        return world.getBlockState(pos).getBlock() == Blocks.BARRIER;
    }
}
