package io.github.gotmemes.visbarrier.compat.v1_8;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class CTMEventHandler {

    private static final int TILE_COUNT = 5;

    private final TextureAtlasSprite[] ctmSprites = new TextureAtlasSprite[TILE_COUNT];

    @SubscribeEvent
    public void onTextureStitch(TextureStitchEvent.Pre event) {
        for (int i = 0; i < TILE_COUNT; i++) {
            ctmSprites[i] = event.map.registerSprite(
                    new ResourceLocation("visbarrier", "blocks/ctm/" + i));
        }
    }

    @SubscribeEvent
    public void onModelBake(ModelBakeEvent event) {
        ModelResourceLocation barrierMrl = new ModelResourceLocation("minecraft:barrier", "normal");
        IBakedModel original = event.modelRegistry.getObject(barrierMrl);
        if (original != null) {
            boolean customTexture = detectCustomBarrierTexture();
            event.modelRegistry.putObject(barrierMrl,
                    new BarrierCTMModel(original, ctmSprites, customTexture));
        }
    }

    private static boolean detectCustomBarrierTexture() {
        ResourceLocation barrierTex = new ResourceLocation("minecraft", "textures/blocks/barrier.png");
        ResourcePackRepository repo = Minecraft.getMinecraft().getResourcePackRepository();
        for (ResourcePackRepository.Entry entry : repo.getRepositoryEntries()) {
            if (entry.getResourcePack().resourceExists(barrierTex)) {
                return true;
            }
        }
        return false;
    }
}
