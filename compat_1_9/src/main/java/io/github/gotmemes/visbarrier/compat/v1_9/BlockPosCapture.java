package io.github.gotmemes.visbarrier.compat.v1_9;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public final class BlockPosCapture {
    private BlockPosCapture() {}

    private static final ThreadLocal<BlockPos>      POS   = new ThreadLocal<BlockPos>();
    private static final ThreadLocal<IBlockAccess>  WORLD = new ThreadLocal<IBlockAccess>();
    // Packed 3x3x3 barrier neighbourhood of the block currently being rendered, computed lazily on
    // the first face and reused by the other five. -1 means "not yet computed for this block".
    private static final ThreadLocal<int[]> NEIGHBOURHOOD = new ThreadLocal<int[]>() {
        @Override protected int[] initialValue() { return new int[]{ -1 }; }
    };

    public static void set(BlockPos pos, IBlockAccess world) {
        POS.set(pos);
        WORLD.set(world);
        NEIGHBOURHOOD.get()[0] = -1;
    }

    public static BlockPos getPos()          { return POS.get(); }
    public static IBlockAccess getWorld()    { return WORLD.get(); }

    public static int getNeighbourhood()          { return NEIGHBOURHOOD.get()[0]; }
    public static void setNeighbourhood(int mask) { NEIGHBOURHOOD.get()[0] = mask; }

    public static void clear() {
        POS.remove();
        WORLD.remove();
        NEIGHBOURHOOD.get()[0] = -1;
    }
}
