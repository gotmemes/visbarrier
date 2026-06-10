package io.github.gotmemes.visbarrier.compat.v1_12;

import io.github.gotmemes.visbarrier.CTMMath;
import io.github.gotmemes.visbarrier.VisbarrierState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class BarrierCTMModel implements IBakedModel {

    private final IBakedModel original;
    private final TextureAtlasSprite[] ctmSprites;

    /**
     * Cache indexed by (face × tile combo): key = face.getIndex() * 625 + tl*125 + tr*25 + bl*5 + br,
     * in [0, 3750). A flat lock-free array — chunk meshing runs on several worker threads, reference
     * writes are atomic, and quad lists are immutable + recomputed identically, so a benign duplicate
     * build on a race is harmless. Avoids the per-face Integer boxing + hashing of a ConcurrentHashMap.
     */
    private final AtomicReferenceArray<List<BakedQuad>> quadCache = new AtomicReferenceArray<>(6 * 625);

    // TEMP DIAGNOSTIC (remove once 1.12 CTM is verified): fire once each so the log shows whether the
    // connected-texture path actually engages, or whether BlockPosCapture is empty and we fall back.
    private static final AtomicBoolean LOGGED_ENGAGED  = new AtomicBoolean(false);
    private static final AtomicBoolean LOGGED_FALLBACK = new AtomicBoolean(false);

    public BarrierCTMModel(IBakedModel original, TextureAtlasSprite[] ctmSprites) {
        this.original = original;
        this.ctmSprites = ctmSprites;
    }

    @Override
    public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
        if (side == null || !VisbarrierState.connectedTextures) {
            return original.getQuads(state, side, rand);
        }

        BlockPos pos = BlockPosCapture.getPos();
        IBlockAccess world = BlockPosCapture.getWorld();
        if (pos == null || world == null) {
            if (LOGGED_FALLBACK.compareAndSet(false, true)) {
                System.out.println("[Visbarrier][diag] 1.12 CTM FALLBACK: BlockPosCapture empty (pos/world null) "
                        + "-> connected textures NOT applied (barrier renders plain). Capture mixin did not fire.");
            }
            return original.getQuads(state, side, rand);
        }

        if (LOGGED_ENGAGED.compareAndSet(false, true)) {
            System.out.println("[Visbarrier][diag] 1.12 CTM ENGAGED: BlockPosCapture populated "
                    + "-> building connected-texture quads.");
        }

        int neighbourhood = BlockPosCapture.getNeighbourhood();
        if (neighbourhood == -1) {
            neighbourhood = CTMUtil_v1_12.computeNeighbourhood(world, pos);
            BlockPosCapture.setNeighbourhood(neighbourhood);
        }
        int n = CTMUtil_v1_12.faceFlags(neighbourhood, side);

        int tl = CTMMath.tile(n, CTMMath.LEFT,  CTMMath.UP,   CTMMath.TOP_LEFT);
        int tr = CTMMath.tile(n, CTMMath.RIGHT, CTMMath.UP,   CTMMath.TOP_RIGHT);
        int bl = CTMMath.tile(n, CTMMath.LEFT,  CTMMath.DOWN, CTMMath.BOTTOM_LEFT);
        int br = CTMMath.tile(n, CTMMath.RIGHT, CTMMath.DOWN, CTMMath.BOTTOM_RIGHT);

        int key = side.getIndex() * 625 + tl * 125 + tr * 25 + bl * 5 + br;
        List<BakedQuad> cached = quadCache.get(key);
        if (cached != null) {
            return cached;
        }

        List<BakedQuad> quads = buildQuads(side, tl, tr, bl, br);
        return quadCache.compareAndSet(key, null, quads) ? quads : quadCache.get(key);
    }

    @Override public boolean isAmbientOcclusion()            { return original.isAmbientOcclusion(); }
    @Override public boolean isGui3d()                       { return original.isGui3d(); }
    @Override public boolean isBuiltInRenderer()             { return original.isBuiltInRenderer(); }
    @Override public TextureAtlasSprite getParticleTexture() { return original.getParticleTexture(); }
    @Override @SuppressWarnings("deprecation")
    public ItemCameraTransforms getItemCameraTransforms()    { return original.getItemCameraTransforms(); }
    @Override public ItemOverrideList getOverrides()         { return ItemOverrideList.NONE; }

    private List<BakedQuad> buildQuads(EnumFacing face, int tl, int tr, int bl, int br) {
        int shadeColor = computeShadeColor(face);
        int normal     = computeNormal(face);

        List<BakedQuad> quads = new ArrayList<>(4);
        quads.add(buildQuadrantQuad(face, 0, ctmSprites[tl], shadeColor, normal)); // TL
        quads.add(buildQuadrantQuad(face, 1, ctmSprites[tr], shadeColor, normal)); // TR
        quads.add(buildQuadrantQuad(face, 2, ctmSprites[bl], shadeColor, normal)); // BL
        quads.add(buildQuadrantQuad(face, 3, ctmSprites[br], shadeColor, normal)); // BR
        return Collections.unmodifiableList(quads);
    }

    private static BakedQuad buildQuadrantQuad(EnumFacing face, int quadrant,
                                                TextureAtlasSprite sprite,
                                                int shadeColor, int normal) {
        float uMin = (quadrant & 1) == 0 ? 0f : 0.5f;
        float uMax = uMin + 0.5f;
        float vMin = quadrant < 2 ? 0f : 0.5f;
        float vMax = vMin + 0.5f;

        float tileUMin = (quadrant & 1) * 8f;
        float tileUMax = tileUMin + 8f;
        float tileVMin = (quadrant >> 1) * 8f;
        float tileVMax = tileVMin + 8f;

        int[] vertexData = new int[32];
        putVertex(vertexData, 0, face, uMin, vMin, shadeColor, sprite, tileUMin, tileVMin, normal);
        putVertex(vertexData, 1, face, uMin, vMax, shadeColor, sprite, tileUMin, tileVMax, normal);
        putVertex(vertexData, 2, face, uMax, vMax, shadeColor, sprite, tileUMax, tileVMax, normal);
        putVertex(vertexData, 3, face, uMax, vMin, shadeColor, sprite, tileUMax, tileVMin, normal);

        // applyDiffuseLighting=false: the 0.5/0.6/0.8/1.0 face shade is already baked into the vertex
        // colour by computeShadeColor() (matching the working 1.8 path). Letting Forge's lighting
        // pipeline ALSO apply diffuse would shade each face twice (e.g. bottom 0.5*0.5=0.25 -> too dark).
        return new BakedQuad(vertexData, -1, face, sprite, false, DefaultVertexFormats.BLOCK);
    }

    private static void putVertex(int[] data, int index, EnumFacing face,
                                   float u, float v, int color,
                                   TextureAtlasSprite sprite, float texU, float texV, int normal) {
        float x, y, z;
        switch (face) {
            case DOWN:  x = u;     y = 0f;    z = 1f - v; break;
            case UP:    x = u;     y = 1f;    z = v;      break;
            case NORTH: x = 1f-u;  y = 1f-v;  z = 0f;     break;
            case SOUTH: x = u;     y = 1f-v;  z = 1f;     break;
            case WEST:  x = 0f;    y = 1f-v;  z = u;      break;
            case EAST:  x = 1f;    y = 1f-v;  z = 1f-u;   break;
            default:    x = y = z = 0f;
        }
        int i = index * 8;
        data[i]     = Float.floatToRawIntBits(x);
        data[i + 1] = Float.floatToRawIntBits(y);
        data[i + 2] = Float.floatToRawIntBits(z);
        data[i + 3] = color;
        data[i + 4] = Float.floatToRawIntBits(sprite.getInterpolatedU(texU));
        data[i + 5] = Float.floatToRawIntBits(sprite.getInterpolatedV(texV));
        data[i + 6] = 0;
        data[i + 7] = normal;
    }

    private static int computeShadeColor(EnumFacing face) {
        float f;
        switch (face) {
            case DOWN:  f = 0.5f; break;
            case UP:    f = 1.0f; break;
            case NORTH: case SOUTH: f = 0.8f; break;
            default:    f = 0.6f;
        }
        int i = Math.min(255, (int)(f * 255.0f));
        return 0xFF000000 | (i << 16) | (i << 8) | i;
    }

    private static int computeNormal(EnumFacing face) {
        int nx = ((byte)(face.getXOffset() * 127)) & 0xFF;
        int ny = ((byte)(face.getYOffset() * 127)) & 0xFF;
        int nz = ((byte)(face.getZOffset() * 127)) & 0xFF;
        return nx | (ny << 8) | (nz << 16);
    }
}
