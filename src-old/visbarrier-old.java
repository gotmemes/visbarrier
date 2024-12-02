package com.gotmemes.visbarrier;

import net.minecraft.init.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
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
    private static final long CACHE_UPDATE_INTERVAL = 1000; // Update cache every 1 second
    private static final int CHUNK_SCAN_RADIUS = 4; // Scan fewer chunks for better performance
    
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
                updateBarrierCache(); // Update cache immediately when enabled
            }
            Minecraft.getMinecraft().thePlayer.addChatMessage(
                new ChatComponentText("Barrier visibility: " + (showBarriers ? "ON" : "OFF"))
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
        
        // Scan loaded chunks in a smaller radius
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
        // Only scan from bedrock to build height in the chunk
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
    
    @SubscribeEvent
    public void onRenderWorldLastEvent(RenderWorldLastEvent event) {
        if (!showBarriers) return;
        
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) return;
        
        // Update cache periodically
        if (System.currentTimeMillis() - lastCacheUpdate > CACHE_UPDATE_INTERVAL) {
            updateBarrierCache();
        }
        
        // Setup rendering
        GlStateManager.pushMatrix();
        setupRendering();
        
        WorldRenderer worldRenderer = Tessellator.getInstance().getWorldRenderer();
        worldRenderer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        
        EntityPlayer player = mc.thePlayer;
        double dx = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.partialTicks;
        double dy = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.partialTicks;
        double dz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.partialTicks;
        
        // Render all barriers in one batch
        for (BlockPos pos : barrierCache) {
            // Only render if within reasonable distance
            if (player.getDistanceSq(pos) <= 256) { // 16 blocks squared
                drawCubeOutline(worldRenderer, pos, 1.0F, 0.0F, 0.0F, 0.8F);
            }
        }
        
        // Finish rendering
        Tessellator.getInstance().draw();
        cleanupRendering();
        GlStateManager.popMatrix();
    }
    
    private void setupRendering() {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        GlStateManager.translate(
            -player.lastTickPosX - (player.posX - player.lastTickPosX),
            -player.lastTickPosY - (player.posY - player.lastTickPosY),
            -player.lastTickPosZ - (player.posZ - player.lastTickPosZ)
        );
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableLighting();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glLineWidth(2.0F); // Slightly thicker lines for better visibility
    }
    
    private void cleanupRendering() {
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GL11.glLineWidth(1.0F);
    }
    
    private void drawCubeOutline(WorldRenderer renderer, BlockPos pos, float r, float g, float b, float a) {
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();
        
        // Combine vertices for better performance
        // Bottom face
        addLine(renderer, x, y, z, x + 1, y, z, r, g, b, a);
        addLine(renderer, x + 1, y, z, x + 1, y, z + 1, r, g, b, a);
        addLine(renderer, x + 1, y, z + 1, x, y, z + 1, r, g, b, a);
        addLine(renderer, x, y, z + 1, x, y, z, r, g, b, a);
        
        // Top face
        addLine(renderer, x, y + 1, z, x + 1, y + 1, z, r, g, b, a);
        addLine(renderer, x + 1, y + 1, z, x + 1, y + 1, z + 1, r, g, b, a);
        addLine(renderer, x + 1, y + 1, z + 1, x, y + 1, z + 1, r, g, b, a);
        addLine(renderer, x, y + 1, z + 1, x, y + 1, z, r, g, b, a);
        
        // Vertical edges
        addLine(renderer, x, y, z, x, y + 1, z, r, g, b, a);
        addLine(renderer, x + 1, y, z, x + 1, y + 1, z, r, g, b, a);
        addLine(renderer, x + 1, y, z + 1, x + 1, y + 1, z + 1, r, g, b, a);
        addLine(renderer, x, y, z + 1, x, y + 1, z + 1, r, g, b, a);
    }
    
    private void addLine(WorldRenderer renderer, double x1, double y1, double z1, 
                        double x2, double y2, double z2, float r, float g, float b, float a) {
        renderer.pos(x1, y1, z1).color(r, g, b, a).endVertex();
        renderer.pos(x2, y2, z2).color(r, g, b, a).endVertex();
    }
}