package com.github.manasmods.cloudsettings.forge.util;

import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

public class PathHelperImpl {
    public static Path getGameDir() {
        return FMLPaths.GAMEDIR.get();
    }
}
