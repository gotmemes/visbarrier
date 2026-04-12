package io.github.gotmemes.visbarrier.compat.v1_9;

import io.github.gotmemes.visbarrier.VisbarrierConfig;
import io.github.gotmemes.visbarrier.VisbarrierState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.util.Arrays;
import java.util.List;

public class VisbarrierCommand extends CommandBase {

    @Override
    public String getCommandName() { return "visbarrier"; }

    private static final String SUBCMD_CONNECT = "connect";
    private static final String SUBCMD_CT      = "ct";
    private static final String SUBCMD_NOTIFY  = "notify";
    private static final String SUBCMD_N       = "n";

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/visbarrier " + SUBCMD_CONNECT + " (" + SUBCMD_CT + ") | " + SUBCMD_NOTIFY + " (" + SUBCMD_N + ")";
    }

    @Override
    public int getRequiredPermissionLevel() { return 0; }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 1 && (args[0].equalsIgnoreCase(SUBCMD_CONNECT) || args[0].equalsIgnoreCase(SUBCMD_CT))) {
            VisbarrierState.connectedTextures = !VisbarrierState.connectedTextures;
            VisbarrierConfig.save();

            Minecraft mc = Minecraft.getMinecraft();
            if (mc.theWorld != null && mc.thePlayer != null) {
                Compat_v1_9.markChunks(mc);
            }

            if (mc.thePlayer != null) {
                mc.thePlayer.addChatComponentMessage(new TextComponentString(
                    TextFormatting.RED + I18n.format("message.visbarrier.connectedtextures") + ": " +
                    (VisbarrierState.connectedTextures
                        ? TextFormatting.GREEN + I18n.format("message.visbarrier.on")
                        : TextFormatting.WHITE + I18n.format("message.visbarrier.off"))
                ));
            }
        } else if (args.length == 1 && (args[0].equalsIgnoreCase(SUBCMD_NOTIFY) || args[0].equalsIgnoreCase(SUBCMD_N))) {
            VisbarrierState.keybindNotifications = !VisbarrierState.keybindNotifications;
            VisbarrierConfig.save();

            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer != null) {
                mc.thePlayer.addChatComponentMessage(new TextComponentString(
                    TextFormatting.RED + I18n.format("message.visbarrier.keybindnotifications") + ": " +
                    (VisbarrierState.keybindNotifications
                        ? TextFormatting.GREEN + I18n.format("message.visbarrier.on")
                        : TextFormatting.WHITE + I18n.format("message.visbarrier.off"))
                ));
            }
        } else {
            sender.addChatMessage(new TextComponentString(
                TextFormatting.RED + I18n.format("message.visbarrier.usage") + " " + getCommandUsage(sender)
            ));
        }
    }

    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        return args.length == 1 ? Arrays.asList(SUBCMD_CONNECT, SUBCMD_CT, SUBCMD_NOTIFY, SUBCMD_N) : null;
    }
}
