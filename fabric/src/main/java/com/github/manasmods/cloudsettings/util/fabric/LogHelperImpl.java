package com.github.manasmods.cloudsettings.util.fabric;

import com.github.manasmods.cloudsettings.fabric.CloudSettingsFabric;
import org.apache.logging.log4j.Logger;

public class LogHelperImpl {
    public static Logger getLogger() {
        return CloudSettingsFabric.getLogger();
    }
}
