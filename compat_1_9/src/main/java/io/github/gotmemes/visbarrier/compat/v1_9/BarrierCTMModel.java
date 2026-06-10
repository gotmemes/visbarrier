package io.github.gotmemes.visbarrier.compat.v1_9;

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
            return original.getQuads(state, side, rand);
        }

        int neighbourhood = BlockPosCapture.getNeighbourhood();
        if (neighbourhood == -1) {
            neighbourhood = CTMUtil_v1_9.computeNeighbourhood(world, pos);
            BlockPosCapture.setNeighbourhood(neighbourhood);
        }
        int n = CTMUtil_v1_9.faceFlags(neighbourhood, side);

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

    // -------------------------------------------------------------------------
    // Quad building
    // -------------------------------------------------------------------------

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

    /**
     * Builds a BakedQuad for one quadrant of a face.
     * Quadrant layout: 0=TL, 1=TR, 2=BL, 3=BR
     *
     * Vertex format in 1.9+: DefaultVertexFormats.BLOCK = 8 ints/vertex
     *   [x, y, z, color, u, v, lightmap, normal]
     */
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

        int[] vertexData = new int[32]; // 8 ints per vertex × 4 vertices
        putVertex(vertexData, 0, face, uMin, vMin, shadeColor, sprite, tileUMin, tileVMin, normal);
        putVertex(vertexData, 1, face, uMin, vMax, shadeColor, sprite, tileUMin, tileVMax, normal);
        putVertex(vertexData, 2, face, uMax, vMax, shadeColor, sprite, tileUMax, tileVMax, normal);
        putVertex(vertexData, 3, face, uMax, vMin, shadeColor, sprite, tileUMax, tileVMin, normal);

        // applyDiffuseLighting=false: the 0.5/0.6/0.8/1.0 face shade is already baked into the vertex
        // colour by computeShadeColor() (matching the working 1.8 path). Letting Forge's lighting
        // pipeline ALSO apply diffuse would shade each face twice (e.g. bottom 0.5*0.5=0.25 -> too dark).
        return new BakedQuad(vertexData, -1, face, sprite, false, DefaultVertexFormats.BLOCK);
    }

    /**
     * Writes one vertex into the 32-int vertex data array (8 ints per vertex for 1.9+).
     * Layout: [x, y, z, shadeColor, u, v, lightmap, normal]
     */
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
        data[i + 6] = 0; // lightmap: not baked, will be computed per-vertex at render time
        data[i + 7] = normal;
    }

    // -------------------------------------------------------------------------
    // Shade color and normal helpers
    // -------------------------------------------------------------------------

    /** Matches FaceBakery.getFaceShadeColor(): DOWN=0.5, UP=1.0, N/S=0.8, E/W=0.6 */
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

    /** Packs the face normal as 3 signed bytes (x,y,z) × 127, packed x|(y<<8)|(z<<16). */
    private static int computeNormal(EnumFacing face) {
        int nx = ((byte)(face.getFrontOffsetX() * 127)) & 0xFF;
        int ny = ((byte)(face.getFrontOffsetY() * 127)) & 0xFF;
        int nz = ((byte)(face.getFrontOffsetZ() * 127)) & 0xFF;
        return nx | (ny << 8) | (nz << 16);
    }
}
