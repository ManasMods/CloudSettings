package com.github.manasmods.cloudsettings;

import com.github.manasmods.cloudsettings.util.Constants;
import com.github.manasmods.cloudsettings.util.State;
import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;
import net.minecraft.client.Options;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CloudSettings {
    public static final AuthHandler AUTH_HANDLER = new AuthHandler();
    private static State connectivity = State.PENDING;
    @Getter(onMethod_ = {@Synchronized})
    @Setter(onMethod_ = {@Synchronized})
    private static boolean initialized = false;

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
        AUTH_HANDLER.login();
    }

    public static void updateSettings(Options options) {

    }
}
