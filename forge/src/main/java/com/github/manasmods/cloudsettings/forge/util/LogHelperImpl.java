package com.github.manasmods.cloudsettings.forge.util;

import com.github.manasmods.cloudsettings.forge.CloudSettingsForge;
import org.apache.logging.log4j.Logger;

public class LogHelperImpl {
    public static Logger getLogger() {
        return CloudSettingsForge.getLogger();
    }
}
