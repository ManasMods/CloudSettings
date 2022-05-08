package com.github.manasmods.cloudsettings.handler;

import com.github.manasmods.cloudsettings.CloudSettings;
import com.github.manasmods.cloudsettings.lwjgl.AuthenticationWindow;
import com.github.manasmods.cloudsettings.mixin.forge.OptionsAccessor;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

public class SettingsLoadingHandler {
    private static boolean initialLoadCompleted = false;
    @Getter
    private static AtomicReference<File> loginKeyFile = null;
    public static AtomicReference<String> apiToken = new AtomicReference<>("");

    public static void checkForLoad(Options options) {
        if (initialLoadCompleted) return;
        final File optionsFile = ((OptionsAccessor) options).getOptionsFile();

        CloudSettings.getLogger().info("Requesting for authentication...");

        loginKeyFile = new AtomicReference<>(new File(System.getProperty("user.home"))
            .toPath()
            .resolve(".minecraft")
            .resolve("cloudsettings")
            .resolve("login.key")
            .toFile());
        //create required dirs
        AuthenticationWindow window = new AuthenticationWindow(Minecraft.getInstance().getUser().getUuid());
        window.show();

        if(apiToken.get().isEmpty()) return;

        //TODO Load known configs

        if (!optionsFile.exists()) {
            CloudSettings.getLogger().info("No options file found. Creating a new one...");
            //TODO generate new config file
            return;
        } else {
            CloudSettings.getLogger().info("Options file found. ");
            //TODO write patches into file
            //TODO skip blacklisted settings
        }


        initialLoadCompleted = true;
    }

    public static void checkForUpdate(Options options) {
        if (!initialLoadCompleted) return;
        //TODO check for update
        //TODO update
    }
}
