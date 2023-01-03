package com.github.manasmods.cloudsettings;

import com.github.manasmods.cloudsettings.cloudservice.CloudSettingsApi;
import com.github.manasmods.cloudsettings.mixin.AccessorOptions;
import com.github.manasmods.cloudsettings.util.Constants;
import com.github.manasmods.cloudsettings.util.State;
import com.github.manasmods.cloudsettings.util.Utils;
import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class CloudSettings {
    public static final AuthHandler AUTH_HANDLER = new AuthHandler();
    private static State connectivity = State.PENDING;
    @Getter(onMethod_ = {@Synchronized})
    @Setter(onMethod_ = {@Synchronized})
    private static boolean initialized = false;
    private static final ConcurrentHashMap<String, String> settingsMap = new ConcurrentHashMap<>();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public static File getDisableModFile() {
        return Paths.get("").resolve("cloudsettings.disable").toFile();
    }

    public static File getLoginKeyFile() {
        return new File(System.getProperty("user.home"))
            .toPath()
            .resolve(".minecraft")
            .resolve("cloudsettings")
            .resolve("login.key")
            .toFile();
    }

    @Synchronized
    public static boolean isEnabled() {
        if (State.PENDING.equals(connectivity)) checkConnectionToCloud();
        // Check for Connectivity
        if (State.FALSE.equals(connectivity)) return false;
        // Check for disable file
        return !getDisableModFile().exists();
    }

    private static void checkConnectionToCloud() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(Constants.CLOUD_SERVER_HOSTNAME, Constants.CLOUD_SERVER_PORT), 5000);
            connectivity = State.TRUE;
            Constants.logger.info("Successfully connected to cloud");
        } catch (SocketTimeoutException e) {
            connectivity = State.FALSE;
            Constants.logger.error("Time out on connection test");
        } catch (IOException e) {
            Constants.logger.error("Exception while testing connection to cloud", e);
        }
    }

    @Synchronized
    public static void disableMod() {
        if (!getDisableModFile().exists()) {
            try {
                Files.write(getDisableModFile().toPath(), Lists.newArrayList(" "), StandardCharsets.UTF_8);
            } catch (IOException e) {
                Constants.logger.error("Error on writing key file.", e);
            }
        }
    }

    @Synchronized
    public static void enableMod() {
        if (getDisableModFile().exists()) {
            if (getDisableModFile().delete()) {
                Constants.logger.info("Enabled Cloud Settings");
            }
        }
    }

    public static void loadSettings(Options options) {
        setInitialized(true);
        final File optionsFile = ((AccessorOptions) options).getOptionsFile();
        AUTH_HANDLER.login(getUserId());

        for (String settingsLine : CloudSettingsApi.getUserSettings(getUserId(), AUTH_HANDLER)) {
            settingsMap.put(Utils.getKeyFromOptionLine(settingsLine), settingsLine);
        }

        if (!optionsFile.exists()) {
            Constants.logger.info("No options file found. Trying to create a new one...");
            //generate new config file
            try {
                Files.write(optionsFile.toPath(), settingsMap.values(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                Constants.logger.trace("Exception while trying to create a new options.txt file.", e);
            }
            return;
        }

        Constants.logger.info("Options file found. Trying up update settings...");
        //write patches into file
        try {
            Files.write(optionsFile.toPath(), settingsMap.values(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Constants.logger.trace("Exception while updating options.txt file.", e);
        }
    }

    public static void updateSettings(Options options) {
        final File optionsFile = ((AccessorOptions) options).getOptionsFile();
        Constants.logger.info("Checking options.txt for updates...");
        try (Stream<String> lines = Files.lines(optionsFile.toPath())) {
            AtomicInteger added = new AtomicInteger(0);
            AtomicInteger updated = new AtomicInteger(0);

            lines.forEach(settingsLine -> {
                String key = Utils.getKeyFromOptionLine(settingsLine);

                if (settingsMap.containsKey(key)) {
                    // check for change
                    String value = settingsMap.get(key);
                    if (!value.equals(settingsLine)) {
                        //update
                        updateOption(key, settingsLine);
                        updated.incrementAndGet();
                    }
                } else {
                    //Add new entry
                    updateOption(key, settingsLine);
                    added.incrementAndGet();
                }
            });

            Constants.logger.info("Added {} and updated {} options", added.get(), updated.get());
        } catch (IOException e) {
            Constants.logger.trace("Exception while checking the options.txt file", e);
        }
    }

    private static void updateOption(String key, String value) {
        final String userId = getUserId();
        EXECUTOR.submit(() -> {
            // Update local state
            settingsMap.put(key, value);
            if (CloudSettingsApi.sendSetting(AUTH_HANDLER, userId, key, value)) {
                Constants.logger.info("Updated {} with value {} in Cloud.", key, value);
            } else {
                Constants.logger.info("Failed to update {} with value {} in Cloud.", key, value);
            }
        });
    }

    @Synchronized
    private static String getUserId() {
        return Minecraft.getInstance().getUser().getUuid();
    }
}
