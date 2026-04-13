package io.github.gotmemes.visbarrier.mixin.mixins;

import io.github.gotmemes.visbarrier.mixin.SupportedVersions;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(net.minecraft.client.renderer.block.statemap.BlockStateMapper.class)
@SupportedVersions("[1.9,)")
public class BlockStateMapperMixin_v1_9 {
    @Shadow private Set<Block> setBuiltInBlocks;

    @Inject(method = "registerBuiltInBlocks", at = @At("TAIL"))
    private void putStateModelLocation(CallbackInfo ci) {
        this.setBuiltInBlocks.remove(Blocks.BARRIER);
    }
}
