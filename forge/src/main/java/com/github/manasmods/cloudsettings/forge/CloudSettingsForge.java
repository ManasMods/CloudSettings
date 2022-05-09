package com.github.manasmods.cloudsettings.forge;

import lombok.Getter;
import me.shedaniel.architectury.platform.forge.EventBuses;
import com.github.manasmods.cloudsettings.CloudSettings;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(CloudSettings.MOD_ID)
public class CloudSettingsForge {
    @Getter
    private static final Logger logger = LogManager.getLogger("Cloud Settings");
    public CloudSettingsForge() {
        // Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(CloudSettings.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        CloudSettings.init();
    }
}