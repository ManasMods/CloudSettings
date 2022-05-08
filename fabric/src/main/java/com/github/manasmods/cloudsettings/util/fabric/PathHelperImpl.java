package com.github.manasmods.cloudsettings.util.fabric;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class PathHelperImpl {
    public static Path getGameDir() {
        return FabricLoader.getInstance().getGameDir();
    }
}
