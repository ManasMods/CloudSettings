package com.github.manasmods.cloudsettings;

import com.github.manasmods.cloudsettings.lwjgl.AuthenticationWindow;
import com.github.manasmods.cloudsettings.util.Constants;
import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;
import net.minecraft.client.Minecraft;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class AuthHandler {
    @Getter(onMethod_ = {@Synchronized})
    @Setter(onMethod_ = {@Synchronized})
    private String authKey = null;

    public AuthHandler() {}

    public boolean isLoggedIn() {
        return getAuthKey() != null;
    }

    public void login() {
        // Check if we are already logged in
        if (isLoggedIn()) return;
        // Exit since mod is disabled
        if (!CloudSettings.isEnabled()) return;
        // Try to log in with stored key
        if (autoLogin()) return;
        String token = loginLoop();
        if (token == null || token.startsWith(" ")) return;
        Constants.logger.info("Login successful");
        this.authKey = token;
        writeAutoLoginFile();
    }

    private String loginLoop() {
        // Request password
        String password = AuthenticationWindow.requestPassword();
        // Exit since mod is disabled
        if (password == null) return null;
        // Try to log in
        String token = CloudSettingsApi.login(Minecraft.getInstance().getUser().getUuid(), password);
        if (token == null) loginLoop();
        return token;
    }

    private boolean autoLogin() {
        if (!CloudSettings.getLoginKeyFile().exists()) return false;
        try (BufferedReader keyFileReader = new BufferedReader(new FileReader(CloudSettings.getLoginKeyFile()))) {
            String storedToken = keyFileReader.readLine();
            Constants.logger.info("Loaded Key {} from storage", storedToken);
            String token = CloudSettingsApi.autoLogin(storedToken);
            if (token != null) {
                Constants.logger.info("Auto Login was successful");
                this.authKey = token;
                writeAutoLoginFile();
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    private void writeAutoLoginFile() {
        // Create parent dir
        if (CloudSettings.getLoginKeyFile().getParentFile().mkdirs()) Constants.logger.info("Created storage directory");
        // Remove old file
        if (CloudSettings.getLoginKeyFile().exists()) {
            if (CloudSettings.getLoginKeyFile().delete()) {
                Constants.logger.info("Deleted old auto log in file");
            }
        }
        // Create new file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CloudSettings.getLoginKeyFile()))) {
            writer.write(this.authKey);
            Constants.logger.info("Created new auto log in file");
        } catch (IOException e) {
            if (CloudSettings.getLoginKeyFile().exists()) {
                if (CloudSettings.getLoginKeyFile().delete()) {
                    Constants.logger.info("Deleted old auto login file.", e);
                }
            }
        }
    }

    public void logout() {
        setAuthKey(null);
        Constants.logger.info("Logged out");
    }
}
