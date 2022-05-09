package com.github.manasmods.cloudsettings.util;

import dev.architectury.injectables.annotations.ExpectPlatform;
import org.apache.logging.log4j.Logger;

public class LogHelper {
    @ExpectPlatform
    public static Logger getLogger() {
        throw new AssertionError();
    }
}
