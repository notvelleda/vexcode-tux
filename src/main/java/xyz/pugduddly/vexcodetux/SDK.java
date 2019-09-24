package xyz.pugduddly.vexcodetux;

import java.io.File;

public class SDK {
    public static boolean shouldUpdate() throws java.io.IOException, java.net.MalformedURLException {
        String sdkVersion = Utils.getURL("https://content.vexrobotics.com/vexcode/v5code/catalog.txt");
        String versionPath = Utils.getStorageDirectory().toString() + "/version.txt";
        boolean shouldUpdate = false;

        if (new File(versionPath).exists()) {
            String localSdkVersion = Utils.readFile(versionPath);
            shouldUpdate = !localSdkVersion.equals(sdkVersion);
        } else {
            shouldUpdate = true;
        }

        return shouldUpdate;
    }

    public static void update() throws java.io.IOException, java.net.MalformedURLException {
        String sdkVersion = Utils.getURL("https://content.vexrobotics.com/vexcode/v5code/catalog.txt");
        Utils.downloadURL("https://content.vexrobotics.com/vexcode/v5code/catalog.txt", Utils.getStorageDirectory().toString() + "/version.txt");
        Utils.downloadURL("https://content.vexrobotics.com/vexcode/v5code/" + sdkVersion + ".sdk", Utils.getStorageDirectory().toString() + "/sdk.zip");
        Utils.unzip(Utils.getStorageDirectory().toString() + "/sdk.zip", Utils.getStorageDirectory().toString());
        new File(Utils.getStorageDirectory().toString() + "/sdk.zip").delete();
    }

    public static String getVersion() {
        try {
            return Utils.readFile(Utils.getStorageDirectory().toString() + "/version.txt");
        } catch (Exception e) {
            return "";
        }
    }
}