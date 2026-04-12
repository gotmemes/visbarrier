package io.github.gotmemes.visbarrier.compat.v1_9;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public final class BlockPosCapture {
    private BlockPosCapture() {}

    private static final ThreadLocal<BlockPos>      POS   = new ThreadLocal<>();
    private static final ThreadLocal<IBlockAccess>  WORLD = new ThreadLocal<>();

    public static void set(BlockPos pos, IBlockAccess world) {
        POS.set(pos);
        WORLD.set(world);
    }

    public static BlockPos getPos()          { return POS.get(); }
    public static IBlockAccess getWorld()    { return WORLD.get(); }

    public static void clear() {
        POS.remove();
        WORLD.remove();
    }
}
