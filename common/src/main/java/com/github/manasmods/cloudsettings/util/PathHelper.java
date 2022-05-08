package com.github.manasmods.cloudsettings.util;

import dev.architectury.injectables.annotations.ExpectPlatform;

import java.nio.file.Path;

public class PathHelper {
    @ExpectPlatform
    public static Path getGameDir() {
        throw new AssertionError();
    }
}
