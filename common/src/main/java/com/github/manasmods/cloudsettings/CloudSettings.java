package com.github.manasmods.cloudsettings;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CloudSettings {
    public static final String MOD_ID = "cloudsettings";
    private static final Logger logger = LogManager.getLogger();

    public static void init() {
    }

    public static Logger getLogger() {
        return logger;
    }
}