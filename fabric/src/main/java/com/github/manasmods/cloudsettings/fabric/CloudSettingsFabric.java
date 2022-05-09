package com.github.manasmods.cloudsettings.fabric;

import com.github.manasmods.cloudsettings.CloudSettings;
import lombok.Getter;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CloudSettingsFabric implements ModInitializer {
    @Getter
    private static final Logger logger = LogManager.getLogger("Cloud Settings");
    @Override
    public void onInitialize() {
        CloudSettings.init();
    }
}