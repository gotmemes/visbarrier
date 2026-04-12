package io.github.gotmemes.visbarrier.compat.v1_9;

import io.github.gotmemes.visbarrier.VisbarrierState;
import io.github.gotmemes.visbarrier.compat.ICompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;

public class Compat_v1_9 implements ICompat {

    @Override
    public void init() {
        ClientCommandHandler.instance.registerCommand(new VisbarrierCommand());
        MinecraftForge.EVENT_BUS.register(new CTMEventHandler());
    }

    @Override
    public void onBarriersToggled() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld != null && mc.thePlayer != null) {
            markChunks(mc);
        }
        if (VisbarrierState.keybindNotifications && mc.thePlayer != null) {
            mc.thePlayer.addChatComponentMessage(new TextComponentString(
                TextFormatting.RED + I18n.format("message.visbarrier.barriervisibility") + ": " +
                (VisbarrierState.barriersVisible
                    ? TextFormatting.GREEN + I18n.format("message.visbarrier.on")
                    : TextFormatting.WHITE + I18n.format("message.visbarrier.off"))
            ));
        }
    }

    static void markChunks(Minecraft mc) {
        int d  = mc.gameSettings.renderDistanceChunks;
        int cx = mc.thePlayer.chunkCoordX;
        int cz = mc.thePlayer.chunkCoordZ;
        mc.theWorld.markBlockRangeForRenderUpdate(
            (cx - d) * 16, 0, (cz - d) * 16,
            (cx + d) * 16 + 15, 255, (cz + d) * 16 + 15
        );
    }
}
