package io.github.gotmemes.visbarrier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

@Mod(
        modid = Visbarrier.MOD_ID,
        name = Visbarrier.MOD_NAME,
        version = Visbarrier.MOD_VERSION,
        acceptedMinecraftVersions = "@ACCEPTED_MINECRAFT_VERSIONS@",
        clientSideOnly = true
)
public class Visbarrier {
    public static final String MOD_ID = "@MOD_ID@";
    public static final String MOD_NAME = "@MOD_NAME@";
    public static final String MOD_VERSION = "@MOD_VERSION@";

    public static boolean barriersVisible = false;

    private final KeyBinding toggleBarriersKey = new KeyBinding(
            "key.toggle_visibility",
            Keyboard.KEY_B,
            "key.category.visbarrier"
    );

    private boolean keyWasPressed = false;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance().bus().register(this);
        ClientRegistry.registerKeyBinding(this.toggleBarriersKey);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent event) {
        if (this.toggleBarriersKey.isKeyDown() && !this.keyWasPressed) {
            toggleBarriers();
        }
        this.keyWasPressed = this.toggleBarriersKey.isKeyDown();
    }

    private void toggleBarriers() {
        barriersVisible = !barriersVisible;
        Minecraft.getMinecraft().renderGlobal.loadRenderers();

        if (Minecraft.getMinecraft().thePlayer != null) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(
                new ChatComponentText(EnumChatFormatting.RED + "Barrier visibility: " +
                    (barriersVisible ? EnumChatFormatting.GREEN + "ON" : EnumChatFormatting.WHITE + "OFF"))
            );
        }
    }
}
