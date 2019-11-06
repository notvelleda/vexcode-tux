package xyz.pugduddly.vexcodetux;

import java.io.File;

// Handles all SDK-related tasks
public class SDK {
    // Returns true if SDK update is available, false otherwise
    public static boolean shouldUpdate() throws java.io.IOException, java.net.MalformedURLException {
        long sdkVersion = Long.parseLong(Utils.getURL("https://content.vexrobotics.com/vexcode/v5code/catalog.txt").replaceAll("_", ""));
        String versionPath = Utils.getStorageDirectory().toString() + "/version.txt";
        boolean shouldUpdate = false;

        if (new File(versionPath).exists()) {
            long localSdkVersion = Long.parseLong(Utils.readFile(versionPath).replaceAll("_", ""));
            shouldUpdate = sdkVersion > localSdkVersion;
        } else {
            shouldUpdate = true;
        }

        return shouldUpdate;
    }

    // Updates SDK to latest version
    public static void update() throws java.io.IOException, java.net.MalformedURLException {
        String sdkVersion = Utils.getURL("https://content.vexrobotics.com/vexcode/v5code/catalog.txt");
        Utils.downloadURL("https://content.vexrobotics.com/vexcode/v5code/catalog.txt", Utils.getStorageDirectory().toString() + "/version.txt");
        Utils.downloadURL("https://content.vexrobotics.com/vexcode/v5code/" + sdkVersion + ".sdk", Utils.getStorageDirectory().toString() + "/sdk.zip");
        Utils.unzip(Utils.getStorageDirectory().toString() + "/sdk.zip", Utils.getStorageDirectory().toString());
        new File(Utils.getStorageDirectory().toString() + "/sdk.zip").delete();
    }

    // Gets installed SDK version
    public static String getVersion() {
        try {
            return Utils.readFile(Utils.getStorageDirectory().toString() + "/version.txt");
        } catch (Exception e) {
            return "";
        }
    }
}