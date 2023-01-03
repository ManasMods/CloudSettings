package com.github.manasmods.cloudsettings.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Constants {
    public static final Logger logger = LogManager.getLogger("CloudSettings");
    public static final String CLOUD_SERVER_HOSTNAME = "cloudservice.blutmondgilde.de";
    public static final int CLOUD_SERVER_PORT = 2053;
    public static final String CLOUD_SERVER = "https://" + CLOUD_SERVER_HOSTNAME + ":" + CLOUD_SERVER_PORT;
}
