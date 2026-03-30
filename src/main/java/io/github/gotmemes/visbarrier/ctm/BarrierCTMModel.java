package io.github.gotmemes.visbarrier.ctm;

import io.github.gotmemes.visbarrier.Visbarrier;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3i;
import net.minecraft.world.IBlockAccess;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class BarrierCTMModel implements IBakedModel {

    private final IBakedModel original;
    private final TextureAtlasSprite[] ctmSprites;
    private final boolean hasCustomBarrierTexture;

    /**
     * Cache: maps (face × tile combo) → immutable list of 4 BakedQuads.
     * Key = face.getIndex() * 625 + tl*125 + tr*25 + bl*5 + br.
     * At most 6 × 5^4 = 3750 entries. Lazily populated, thread-safe.
     */
    private final ConcurrentHashMap<Integer, List<BakedQuad>> quadCache = new ConcurrentHashMap<>();

    public BarrierCTMModel(IBakedModel original, TextureAtlasSprite[] ctmSprites,
                           boolean hasCustomBarrierTexture) {
        this.original = original;
        this.ctmSprites = ctmSprites;
        this.hasCustomBarrierTexture = hasCustomBarrierTexture;
    }

    @Override
    public List<BakedQuad> getFaceQuads(EnumFacing side) {
        if (!Visbarrier.connectedTextures || hasCustomBarrierTexture) {
            return original.getFaceQuads(side);
        }

        BlockPos pos = BlockPosCapture.getPos();
        IBlockAccess world = BlockPosCapture.getWorld();
        if (pos == null || world == null) {
            return original.getFaceQuads(side);
        }

        boolean[] neighbors = CTMUtil.getNeighborFlags(world, pos, side);
        int[] tiles = CTMUtil.getQuadrantTiles(neighbors);

        int key = side.getIndex() * 625 + tiles[0] * 125 + tiles[1] * 25 + tiles[2] * 5 + tiles[3];
        List<BakedQuad> cached = quadCache.get(key);
        if (cached != null) {
            return cached;
        }

        List<BakedQuad> quads = buildQuads(side, tiles);
        List<BakedQuad> existing = quadCache.putIfAbsent(key, quads);
        return existing != null ? existing : quads;
    }

    @Override
    public List<BakedQuad> getGeneralQuads() {
        return original.getGeneralQuads();
    }

    @Override public boolean isAmbientOcclusion()               { return original.isAmbientOcclusion(); }
    @Override public boolean isGui3d()                          { return original.isGui3d(); }
    @Override public boolean isBuiltInRenderer()                { return original.isBuiltInRenderer(); }
    @Override public TextureAtlasSprite getParticleTexture()    { return original.getParticleTexture(); }
    @Override @SuppressWarnings("deprecation")
    public ItemCameraTransforms getItemCameraTransforms()       { return original.getItemCameraTransforms(); }

    // -------------------------------------------------------------------------
    // Quad building
    // -------------------------------------------------------------------------

    private List<BakedQuad> buildQuads(EnumFacing face, int[] tiles) {
        int shadeColor = computeShadeColor(face);
        int normal     = computeNormal(face);

        List<BakedQuad> quads = new ArrayList<>(4);
        for (int qi = 0; qi < 4; qi++) {
            quads.add(buildQuadrantQuad(face, qi, ctmSprites[tiles[qi]], shadeColor, normal));
        }
        return Collections.unmodifiableList(quads);
    }

    /**
     * Builds a BakedQuad for one quadrant of a face.
     *
     * Quadrant layout:  0=TL, 1=TR, 2=BL, 3=BR
     *   uMin/uMax in [0,1] face-local space: 0 or 0.5 / 0.5 or 1.0
     *   tileU/V sample from the corresponding 8×8 corner of the tile texture
     */
    private static BakedQuad buildQuadrantQuad(EnumFacing face, int quadrant,
                                                TextureAtlasSprite sprite,
                                                int shadeColor, int normal) {
        // World-space face-local bounds for this quadrant
        float uMin = (quadrant & 1) == 0 ? 0f : 0.5f;
        float uMax = uMin + 0.5f;
        float vMin = quadrant < 2 ? 0f : 0.5f;
        float vMax = vMin + 0.5f;

        // UV sub-region of the 16×16 tile (values for sprite.getInterpolatedU/V)
        float tileUMin = (quadrant & 1) * 8f;
        float tileUMax = tileUMin + 8f;
        float tileVMin = (quadrant >> 1) * 8f;
        float tileVMax = tileVMin + 8f;

        int[] vertexData = new int[28];
        // Winding follows EnumFaceDirection: V0(uMin,vMin) V1(uMin,vMax) V2(uMax,vMax) V3(uMax,vMin)
        putVertex(vertexData, 0, face, uMin, vMin, shadeColor, sprite, tileUMin, tileVMin, normal);
        putVertex(vertexData, 1, face, uMin, vMax, shadeColor, sprite, tileUMin, tileVMax, normal);
        putVertex(vertexData, 2, face, uMax, vMax, shadeColor, sprite, tileUMax, tileVMax, normal);
        putVertex(vertexData, 3, face, uMax, vMin, shadeColor, sprite, tileUMax, tileVMin, normal);

        return new BakedQuad(vertexData, -1, face);
    }

    /**
     * Writes one vertex into the 28-int vertex data array.
     *
     * Vertex layout (7 ints): [x, y, z, shadeColor, u, v, normal]
     * All float values stored as Float.floatToRawIntBits.
     *
     * Position mapping: position = V0origin + u*uDir + v*vDir
     * Derived from EnumFaceDirection vertex data (see CTMUtil.FACE_AXES):
     *   DOWN  V0=(0,0,1) uDir=+X vDir=-Z → x=u,   y=0,   z=1-v
     *   UP    V0=(0,1,0) uDir=+X vDir=+Z → x=u,   y=1,   z=v
     *   NORTH V0=(1,1,0) uDir=-X vDir=-Y → x=1-u, y=1-v, z=0
     *   SOUTH V0=(0,1,1) uDir=+X vDir=-Y → x=u,   y=1-v, z=1
     *   WEST  V0=(0,1,0) uDir=+Z vDir=-Y → x=0,   y=1-v, z=u
     *   EAST  V0=(1,1,1) uDir=-Z vDir=-Y → x=1,   y=1-v, z=1-u
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
        int i = index * 7;
        data[i]     = Float.floatToRawIntBits(x);
        data[i + 1] = Float.floatToRawIntBits(y);
        data[i + 2] = Float.floatToRawIntBits(z);
        data[i + 3] = color;
        data[i + 4] = Float.floatToRawIntBits(sprite.getInterpolatedU(texU));
        data[i + 5] = Float.floatToRawIntBits(sprite.getInterpolatedV(texV));
        data[i + 6] = normal;
    }

    // -------------------------------------------------------------------------
    // Shade color and normal helpers
    // -------------------------------------------------------------------------

    /**
     * Computes the packed ARGB shade color for a face, matching FaceBakery.getFaceShadeColor().
     * DOWN=0.5, UP=1.0, N/S=0.8, E/W=0.6
     */
    private static int computeShadeColor(EnumFacing face) {
        float f;
        switch (face) {
            case DOWN:  f = 0.5f; break;
            case UP:    f = 1.0f; break;
            case NORTH: case SOUTH: f = 0.8f; break;
            default:    f = 0.6f; // EAST, WEST
        }
        int i = Math.min(255, (int)(f * 255.0f));
        return 0xFF000000 | (i << 16) | (i << 8) | i;
    }

    /**
     * Packs the face normal into the format used by ForgeHooksClient.fillNormal:
     * 3 signed bytes (x, y, z) scaled by 127, packed as x|(y<<8)|(z<<16).
     */
    private static int computeNormal(EnumFacing face) {
        Vec3i dir = face.getDirectionVec();
        int nx = ((byte)(dir.getX() * 127)) & 0xFF;
        int ny = ((byte)(dir.getY() * 127)) & 0xFF;
        int nz = ((byte)(dir.getZ() * 127)) & 0xFF;
        return nx | (ny << 8) | (nz << 16);
    }

}
