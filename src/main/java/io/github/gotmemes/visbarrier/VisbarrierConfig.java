package io.github.gotmemes.visbarrier;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

public final class VisbarrierConfig {
    private VisbarrierConfig() {}

    private static Configuration config;

    public static void init(File configFile) {
        config = new Configuration(configFile);
        config.load();

        VisbarrierState.connectedTextures = config.getBoolean(
                "connectedTextures", Configuration.CATEGORY_GENERAL, true,
                "Enable connected textures for adjacent barrier blocks");
        VisbarrierState.keybindNotifications = config.getBoolean(
                "keybindNotifications", Configuration.CATEGORY_GENERAL, true,
                "Show chat notification when toggling barrier visibility with the keybind");

        if (config.hasChanged()) config.save();
    }

    public static void save() {
        if (config == null) return;
        config.get(Configuration.CATEGORY_GENERAL, "connectedTextures", true)
              .set(VisbarrierState.connectedTextures);
        config.get(Configuration.CATEGORY_GENERAL, "keybindNotifications", true)
              .set(VisbarrierState.keybindNotifications);
        config.save();
    }
}
