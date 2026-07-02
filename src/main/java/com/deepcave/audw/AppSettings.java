package com.deepcave.audw;

import java.util.prefs.Preferences;

public class AppSettings {
    private static final Preferences prefs = Preferences.userNodeForPackage(AppSettings.class);
    private static final String KEY_DOWNLOAD_DIR = "downloadDir";
    private static final String KEY_DIVIDED = "divided";

    private AppSettings() {}

    public static String getDownloadDir() {
        return prefs.get(KEY_DOWNLOAD_DIR, null);
    }

    public static void setDownloadDir(String path) {
        if (path != null) prefs.put(KEY_DOWNLOAD_DIR, path);
        else prefs.remove(KEY_DOWNLOAD_DIR);
    }

    public static boolean isDivided() {
        return prefs.getBoolean(KEY_DIVIDED, true);
    }

    public static void setDivided(boolean divided) {
        prefs.putBoolean(KEY_DIVIDED, divided);
    }
}
