import org.lwjgl.util.tinyfd.TinyFileDialogs;

public class LWJGLTest {
    public static void main(String[] args) {
        String result = TinyFileDialogs.tinyfd_inputBox("Cloud Settings - Authentication", "Please enter a password to secure your Settings.", null);
        if (result == null) {
            boolean disableMod = TinyFileDialogs.tinyfd_messageBox("Cloud Settings", "Are you sure you want to disable CloudSettings?", "yesno", "warning", false);
            if (disableMod) {
                TinyFileDialogs.tinyfd_notifyPopup("Cloud Settings", "Cloud Settings has been disabled.", "info");
            } else {
                main(args);
            }
            return;
        }

        if (result.isEmpty() || result.isBlank()) {
            main(args);
            return;
        }

        System.out.println("Password: " + result);
    }
}
