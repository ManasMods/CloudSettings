package com.github.manasmods.cloudsettings.lwjgl;

import com.github.manasmods.cloudsettings.CloudSettings;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import javax.annotation.Nullable;
import java.util.function.Predicate;

public class AuthenticationWindow {
    @SuppressWarnings("RedundantIfStatement")
    private static final Predicate<String> passwordCheck = s -> {
        if (s.isEmpty()) return false;
        if (s.startsWith(" ")) return false;
        return true;
    };

    @Nullable
    public static String requestPassword() {
        String enteredPassword = TinyFileDialogs.tinyfd_inputBox("Cloud Settings - Authentication",
            "Cloud Settings is a client side Minecraft mod to synchronize your options to other Modpacks you might play or to other physical machines you play Modpacks on." + System.lineSeparator() + System.lineSeparator() +
                "This Service is 100% free and the client side source code is viewable on GitHub." + System.lineSeparator() + System.lineSeparator() +
                "If you have never used CloudSettings before, please enter a password to register your Account." + System.lineSeparator() + System.lineSeparator() +
                "Otherwise please enter your password to login into your Account." + System.lineSeparator() + System.lineSeparator() +
                "Click on cancel to disable CloudSettings for this Modpack."
            , null);

        // Dialog cancel or close
        if (enteredPassword == null && !confirmDisableMod()) {
            if (confirmDisableMod()) {
                // Exit since mod is disabled
                return null;
            } else {
                // Ask for Password again
                return requestPassword();
            }
        }

        // Retry on empty input
        if (!passwordCheck.test(enteredPassword)) return requestPassword();

        return enteredPassword;
    }

    private static boolean confirmDisableMod() {
        boolean disableMod = TinyFileDialogs.tinyfd_messageBox("Cloud Settings", "Are you sure you want to disable CloudSettings?", "yesno", "warning", false);
        if (disableMod) {
            CloudSettings.disableMod();
            TinyFileDialogs.tinyfd_notifyPopup("Cloud Settings", "Cloud Settings has been disabled.", "info");
        }

        return disableMod;
    }

    public static void logInFailedNotification() {
        TinyFileDialogs.tinyfd_notifyPopup("Cloud Settings - Authentication",
            "Login failed.\nPlease enter the correct password.\nTo reset the password please message the ManasMods support team.",
            "error");
    }
}
