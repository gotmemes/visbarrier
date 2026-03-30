package io.github.gotmemes.visbarrier.command;

import io.github.gotmemes.visbarrier.Visbarrier;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.Arrays;
import java.util.List;

public class VisbarrierCommand extends CommandBase {

    @Override
    public String getCommandName() { return "visbarrier"; }

    @Override
    public String getCommandUsage(ICommandSender sender) { return "/visbarrier connect"; }

    @Override
    public int getRequiredPermissionLevel() { return 0; }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("connect")) {
            Visbarrier.connectedTextures = !Visbarrier.connectedTextures;

            Minecraft mc = Minecraft.getMinecraft();
            if (mc.theWorld != null && mc.thePlayer != null) {
                int d = mc.gameSettings.renderDistanceChunks;
                int cx = mc.thePlayer.chunkCoordX;
                int cz = mc.thePlayer.chunkCoordZ;
                mc.theWorld.markBlockRangeForRenderUpdate(
                    (cx - d) * 16, 0, (cz - d) * 16,
                    (cx + d) * 16 + 15, 255, (cz + d) * 16 + 15
                );
            }

            if (mc.thePlayer != null) {
                mc.thePlayer.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.RED + "Connected textures: " +
                    (Visbarrier.connectedTextures
                        ? EnumChatFormatting.GREEN + "ON"
                        : EnumChatFormatting.WHITE + "OFF")
                ));
            }
        } else {
            sender.addChatMessage(new ChatComponentText(
                EnumChatFormatting.RED + "Usage: " + getCommandUsage(sender)
            ));
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        return args.length == 1 ? Arrays.asList("connect") : null;
    }
}
