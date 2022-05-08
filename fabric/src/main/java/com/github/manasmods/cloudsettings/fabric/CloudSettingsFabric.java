package com.github.manasmods.cloudsettings.fabric;

import com.github.manasmods.cloudsettings.CloudSettings;
import net.fabricmc.api.ModInitializer;

public class CloudSettingsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        CloudSettings.init();
    }
}