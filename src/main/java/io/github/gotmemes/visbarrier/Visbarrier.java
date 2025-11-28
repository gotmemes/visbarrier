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

    public static boolean isVisible = false;

    private final KeyBinding toggleKey = new KeyBinding(
            "key.toggle_visibility",
            Keyboard.KEY_B,
            "key.category.visbarrier"
    );

    private boolean wasPressed = false;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance().bus().register(this);
        ClientRegistry.registerKeyBinding(this.toggleKey);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent event) {
        if (this.toggleKey.isKeyDown() && !this.wasPressed) {
            Visbarrier.isVisible = !Visbarrier.isVisible;
            Minecraft.getMinecraft().renderGlobal.loadRenderers();
        }
        this.wasPressed = this.toggleKey.isKeyDown();
        
    }

    private void toggleBarriers() {
        
    }
}
