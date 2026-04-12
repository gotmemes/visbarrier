package io.github.gotmemes.visbarrier.compat.v1_12;

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
    public String getName() { return "visbarrier"; }

    private static final String SUBCMD_CONNECT = "connect";
    private static final String SUBCMD_CT      = "ct";
    private static final String SUBCMD_NOTIFY  = "notify";
    private static final String SUBCMD_N       = "n";

    @Override
    public String getUsage(ICommandSender sender) {
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
            if (mc.world != null && mc.player != null) {
                Compat_v1_12.markChunks(mc);
            }

            if (mc.player != null) {
                mc.player.sendMessage(new TextComponentString(
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
            if (mc.player != null) {
                mc.player.sendMessage(new TextComponentString(
                    TextFormatting.RED + I18n.format("message.visbarrier.keybindnotifications") + ": " +
                    (VisbarrierState.keybindNotifications
                        ? TextFormatting.GREEN + I18n.format("message.visbarrier.on")
                        : TextFormatting.WHITE + I18n.format("message.visbarrier.off"))
                ));
            }
        } else {
            sender.sendMessage(new TextComponentString(
                TextFormatting.RED + I18n.format("message.visbarrier.usage") + " " + getUsage(sender)
            ));
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
        return args.length == 1 ? Arrays.asList(SUBCMD_CONNECT, SUBCMD_CT, SUBCMD_NOTIFY, SUBCMD_N) : null;
    }
}
