package com.gotmemes.visbarrier;

import net.minecraft.init.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

@Mod(
        modid = visbarrier.MODID,
        name = visbarrier.NAME,
        version = visbarrier.VERSION,
        clientSideOnly = true,
        acceptedMinecraftVersions = "[1.8.9]"
)
public class visbarrier {
    public static final String MODID = "visbarrier";
    public static final String NAME = "Visible Barriers";
    public static final String VERSION = "1.0";
    private static boolean showBarriers = false;
    private KeyBinding toggleKey;
    private List<BlockPos> barrierCache = new ArrayList<BlockPos>();
    private long lastCacheUpdate = 0;
    private static final long CACHE_UPDATE_INTERVAL = 1000;
    private static final int CHUNK_SCAN_RADIUS = 4;
    private static final ResourceLocation BARRIER_TEXTURE = new ResourceLocation(MODID, "textures/blocks/barrier.png");
    private ICamera frustum = new Frustum();
    
    @EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        toggleKey = new KeyBinding("Toggle Barrier Visibility", Keyboard.KEY_B, "Barrier Viewer");
        ClientRegistry.registerKeyBinding(toggleKey);
    }
    
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if(toggleKey.isPressed()) {
            showBarriers = !showBarriers;
            if (showBarriers) {
                updateBarrierCache();
            } else {
                barrierCache.clear();
            }
            Minecraft.getMinecraft().thePlayer.addChatMessage(
                new ChatComponentText(EnumChatFormatting.RED + "Barrier visibility: " + (showBarriers ? EnumChatFormatting.GREEN + "ON" : EnumChatFormatting.WHITE + "OFF"))
            );
        }
    }
    
    private void updateBarrierCache() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) return;
        
        barrierCache.clear();
        EntityPlayer player = mc.thePlayer;
        World world = mc.theWorld;
        
        int playerChunkX = player.getPosition().getX() >> 4;
        int playerChunkZ = player.getPosition().getZ() >> 4;
        
        for (int cx = -CHUNK_SCAN_RADIUS; cx <= CHUNK_SCAN_RADIUS; cx++) {
            for (int cz = -CHUNK_SCAN_RADIUS; cz <= CHUNK_SCAN_RADIUS; cz++) {
                int chunkX = playerChunkX + cx;
                int chunkZ = playerChunkZ + cz;
                
                if (!world.getChunkProvider().chunkExists(chunkX, chunkZ)) continue;
                
                Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
                scanChunkForBarriers(chunk, world);
            }
        }
        
        lastCacheUpdate = System.currentTimeMillis();
    }
    
    private void scanChunkForBarriers(Chunk chunk, World world) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 256; y++) {
                    if (chunk.getBlock(x, y, z) == Blocks.barrier) {
                        int worldX = chunk.xPosition * 16 + x;
                        int worldZ = chunk.zPosition * 16 + z;
                        barrierCache.add(new BlockPos(worldX, y, worldZ));
                    }
                }
            }
        }
    }

    private void renderCubeWithTexture(WorldRenderer renderer, BlockPos pos, EntityPlayer player) {
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();

        float u1 = 0f;
        float u2 = 1f;
        float v1 = 0f;
        float v2 = 1f;

        // Only render faces that are visible to the player
        double eyeX = player.posX;
        double eyeY = player.posY + player.getEyeHeight();
        double eyeZ = player.posZ;

        // Top face (y+1)
        if (eyeY > y + 1) {
            renderer.pos(x, y + 1, z).tex(u1, v1).color(1.0f, 1.0f, 1.0f, 0.8f).endVertex();
            renderer.pos(x, y + 1, z + 1).tex(u1, v2).color(1.0f, 1.0f, 1.0f, 0.8f).endVertex();
            renderer.pos(x + 1, y + 1, z + 1).tex(u2, v2).color(1.0f, 1.0f, 1.0f, 0.8f).endVertex();
            renderer.pos(x + 1, y + 1, z).tex(u2, v1).color(1.0f, 1.0f, 1.0f, 0.8f).endVertex();
        }

        // Bottom face (y-1)
        if (eyeY < y) {
            renderer.pos(x + 1, y, z).tex(u2, v1).color(1.0f, 1.0f, 1.0f, 0.8f).endVertex();
            renderer.pos(x + 1, y, z + 1).tex(u2, v2).color(1.0f, 1.0f, 1.0f, 0.8f).endVertex();
            renderer.pos(x, y, z + 1).tex(u1, v2).color(1.0f, 1.0f, 1.0f, 0.8f).endVertex();
            renderer.pos(x, y, z).tex(u1, v1).color(1.0f, 1.0f, 1.0f, 0.8f).endVertex();
        }

        // North face (z-)
        if (eyeZ < z) {
            renderer.pos(x, y, z).tex(u1, v2).color(1.0f, 1.0f, 1.0f, 0.8f).endVertex();
            renderer.pos(x, y + 1, z).tex(u1, v1).color(1.0f, 1.0f, 1.0f, 0.8f).endVertex();
            renderer.pos(x + 1, y + 1, z).tex(u2, v1).color(1.0f, 1.0f, 1.0f, 0.8f).endVertex();
            renderer.pos(x + 1, y, z).tex(u2, v2).color(1.0f, 1.0f, 1.0f, 0.8f).endVertex();
        }

        // South face (z+)
        if (eyeZ > z + 1) {
            renderer.pos(x + 1, y, z + 1).tex(u1, v2).color(1.0f, 1.0f, 1.0f, 0.8f).endVertex();
            renderer.pos(x + 1, y + 1, z + 1).tex(u1, v1).color(1.0f, 1.0f, 1.0f, 0.8f).endVertex();
            renderer.pos(x, y + 1, z + 1).tex(u2, v1).color(1.0f, 1.0f, 1.0f, 0.8f).endVertex();
            renderer.pos(x, y, z + 1).tex(u2, v2).color(1.0f, 1.0f, 1.0f, 0.8f).endVertex();
        }

        // West face (x-)
        if (eyeX < x) {
            renderer.pos(x, y, z + 1).tex(u1, v2).color(1.0f, 1.0f, 1.0f, 0.8f).endVertex();
            renderer.pos(x, y + 1, z + 1).tex(u1, v1).color(1.0f, 1.0f, 1.0f, 0.8f).endVertex();
            renderer.pos(x, y + 1, z).tex(u2, v1).color(1.0f, 1.0f, 1.0f, 0.8f).endVertex();
            renderer.pos(x, y, z).tex(u2, v2).color(1.0f, 1.0f, 1.0f, 0.8f).endVertex();
        }

        // East face (x+)
        if (eyeX > x + 1) {
            renderer.pos(x + 1, y, z).tex(u1, v2).color(1.0f, 1.0f, 1.0f, 0.8f).endVertex();
            renderer.pos(x + 1, y + 1, z).tex(u1, v1).color(1.0f, 1.0f, 1.0f, 0.8f).endVertex();
            renderer.pos(x + 1, y + 1, z + 1).tex(u2, v1).color(1.0f, 1.0f, 1.0f, 0.8f).endVertex();
            renderer.pos(x + 1, y, z + 1).tex(u2, v2).color(1.0f, 1.0f, 1.0f, 0.8f).endVertex();
        }
    }
    
    @SubscribeEvent
    public void onRenderWorldLastEvent(RenderWorldLastEvent event) {
        if (!showBarriers) return;
        
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) return;
        
        if (System.currentTimeMillis() - lastCacheUpdate > CACHE_UPDATE_INTERVAL) {
            updateBarrierCache();
        }
        
        if (barrierCache.isEmpty()) return;

        EntityPlayer player = mc.thePlayer;
        double dx = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.partialTicks;
        double dy = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.partialTicks;
        double dz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.partialTicks;

        // Update frustum for visibility culling
        frustum.setPosition(dx, dy, dz);

        // Setup rendering
        GlStateManager.pushMatrix();
        GlStateManager.translate(-dx, -dy, -dz);
        
        // Bind our specific texture
        mc.getTextureManager().bindTexture(BARRIER_TEXTURE);
        
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.enableCull();
        GlStateManager.disableLighting();
        
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

        for (BlockPos pos : barrierCache) {
            if (player.getDistanceSq(pos) <= 256) { // Distance check
                // Frustum culling check
                AxisAlignedBB boundingBox = new AxisAlignedBB(
                    pos.getX(), pos.getY(), pos.getZ(),
                    pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1
                );
                if (frustum.isBoundingBoxInFrustum(boundingBox)) {
                    renderCubeWithTexture(worldRenderer, pos, player);
                }
            }
        }

        tessellator.draw();
        
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
}