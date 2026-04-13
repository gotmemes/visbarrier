package io.github.gotmemes.visbarrier.compat.v1_9;

import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class CTMEventHandler {

    private static final int TILE_COUNT = 5;

    private final TextureAtlasSprite[] ctmSprites = new TextureAtlasSprite[TILE_COUNT];

    @SubscribeEvent
    public void onTextureStitch(TextureStitchEvent.Pre event) {
        event.getMap().registerSprite(new ResourceLocation("minecraft", "blocks/barrier"));
        for (int i = 0; i < TILE_COUNT; i++) {
            ctmSprites[i] = event.getMap().registerSprite(
                    new ResourceLocation("visbarrier", "blocks/ctm/" + i));
        }
    }

    @SubscribeEvent
    public void onModelBake(ModelBakeEvent event) {
        ModelResourceLocation barrierMrl = new ModelResourceLocation("minecraft:barrier", "normal");
        IBakedModel original = event.getModelRegistry().getObject(barrierMrl);
        if (original == null) return;

        event.getModelRegistry().putObject(barrierMrl, new BarrierCTMModel(original, ctmSprites));
    }
}
