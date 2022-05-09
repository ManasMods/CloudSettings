package com.github.manasmods.cloudsettings.handler;

import com.github.manasmods.cloudsettings.lwjgl.AuthenticationWindow;
import com.github.manasmods.cloudsettings.mixin.forge.OptionsAccessor;
import com.github.manasmods.cloudsettings.util.ApiHelper;
import com.github.manasmods.cloudsettings.util.LogHelper;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class SettingsLoadingHandler {
    private static boolean initialLoadCompleted = false;
    @Getter
    private static AtomicReference<File> loginKeyFile = null;
    public static AtomicReference<String> apiToken = new AtomicReference<>("");
    private static final Map<String, String> settingsMap = new TreeMap<>();

    public static void checkForLoad(Options options) {
        if (initialLoadCompleted) return;
        final File optionsFile = ((OptionsAccessor) options).getOptionsFile();

        LogHelper.getLogger().info("Requesting for authentication...");

        loginKeyFile = new AtomicReference<>(new File(System.getProperty("user.home"))
            .toPath()
            .resolve(".minecraft")
            .resolve("cloudsettings")
            .resolve("login.key")
            .toFile());
        //create required dirs
        AuthenticationWindow window = new AuthenticationWindow(Minecraft.getInstance().getUser().getUuid());
        window.show();

        if (apiToken.get().isEmpty()) {
            LogHelper.getLogger().warn("No API Token found. Canceling Synchronization!");
            return;
        }

        //Load known configs
        ApiHelper.loadSettings()
            .forEach(settingLine -> settingsMap.put(getKeyFromOptionLine(settingLine), settingLine));

        if (!optionsFile.exists()) {
            LogHelper.getLogger().info("No options file found. Trying to create a new one...");
            //generate new config file
            try {
                Files.write(optionsFile.toPath(), settingsMap.values(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                LogHelper.getLogger().trace("Exception while trying to create a new options.txt file.", e);
                return;
            }
        } else {
            LogHelper.getLogger().info("Options file found. Trying up update settings...");
            //write patches into file
            try {
                //TODO skip blacklisted settings
                Files.write(optionsFile.toPath(), settingsMap.values(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                LogHelper.getLogger().trace("Exception while updating options.txt file.", e);
            }
        }


        initialLoadCompleted = true;
    }

    public static void checkForUpdate(Options options) {
        if (!initialLoadCompleted) return;
        //TODO check for update
        final File optionsFile = ((OptionsAccessor) options).getOptionsFile();
        LogHelper.getLogger().info("Checking options.txt for updates...");
        try (Stream<String> lines = Files.lines(optionsFile.toPath())) {
            AtomicInteger added = new AtomicInteger(0);
            AtomicInteger updated = new AtomicInteger(0);


            lines.forEach(settingsLine -> {
                String key = getKeyFromOptionLine(settingsLine);

                if (settingsMap.containsKey(key)) {
                    // check for change
                    String value = settingsMap.get(key);
                    if (!value.equals(settingsLine)) {
                        //update
                        settingsMap.put(key, settingsLine);
                        if (ApiHelper.sendSetting(key, settingsLine)) {
                            LogHelper.getLogger().info("Updated {} in Cloud.", key);
                        } else {
                            LogHelper.getLogger().info("Failed to update {} in Cloud.", key);
                        }
                        updated.incrementAndGet();
                    }
                } else {
                    //Add new entry
                    settingsMap.put(getKeyFromOptionLine(settingsLine), settingsLine);
                    if (ApiHelper.sendSetting(key, settingsLine)) {
                        LogHelper.getLogger().info("Updated {} in Cloud.", key);
                    } else {
                        LogHelper.getLogger().info("Failed to update {} in Cloud.", key);
                    }
                    added.incrementAndGet();
                }
            });

            LogHelper.getLogger().info("Added {} and updated {} options", added.get(), updated.get());
        } catch (IOException e) {
            LogHelper.getLogger().trace("Exception while checking the options.txt file", e);
        }
    }

    private static String getKeyFromOptionLine(String line) {
        return line.substring(0, line.indexOf(':'));
    }
}
