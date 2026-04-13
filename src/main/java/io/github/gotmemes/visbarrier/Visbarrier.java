package io.github.gotmemes.visbarrier;

import io.github.gotmemes.visbarrier.compat.ICompat;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
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
    public static final String MOD_ID      = "@MOD_ID@";
    public static final String MOD_NAME    = "@MOD_NAME@";
    public static final String MOD_VERSION = "@MOD_VERSION@";

    private static ICompat compat;

    private final KeyBinding toggleBarriersKey = new KeyBinding(
            "key.visbarrier.toggle",
            Keyboard.KEY_B,
            "key.category.visbarrier"
    );

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        VisbarrierConfig.init(event.getSuggestedConfigurationFile());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance().bus().register(this);
        ClientRegistry.registerKeyBinding(this.toggleBarriersKey);

        String mcVersion = Loader.instance().getMinecraftModContainer().getVersion();
        compat = loadCompat(mcVersion);
        if (compat != null) {
            compat.init();
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (this.toggleBarriersKey.isPressed()) {
            VisbarrierState.barriersVisible = !VisbarrierState.barriersVisible;
            if (compat != null) {
                compat.onBarriersToggled();
            }
        }
    }

    private static ICompat loadCompat(String mcVersion) {
        String className;
        if (mcVersion.startsWith("1.8")) {
            className = "io.github.gotmemes.visbarrier.compat.v1_8.Compat_v1_8";
        } else if (mcVersion.startsWith("1.9") || mcVersion.startsWith("1.10")) {
            className = "io.github.gotmemes.visbarrier.compat.v1_9.Compat_v1_9";
        } else if (mcVersion.startsWith("1.11")) {
            className = "io.github.gotmemes.visbarrier.compat.v1_11.Compat_v1_11";
        } else if (mcVersion.startsWith("1.12")) {
            className = "io.github.gotmemes.visbarrier.compat.v1_12.Compat_v1_12";
        } else {
            FMLLog.warning("[Visbarrier] Unsupported Minecraft version %s — mod will load key-only with no version-specific features.", mcVersion);
            return null;
        }
        try {
            return (ICompat) Class.forName(className).newInstance();
        } catch (ClassNotFoundException e) {
            FMLLog.severe("[Visbarrier] Compat class %s not found on classpath — jar may be incomplete.", className);
            return null;
        } catch (Exception e) {
            FMLLog.log(org.apache.logging.log4j.Level.ERROR, e, "[Visbarrier] Failed to instantiate compat class %s for MC %s", className, mcVersion);
            return null;
        }
    }
}
