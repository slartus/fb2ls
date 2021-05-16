package org.softeg.fb2tools;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;

import java.io.File;

/*
 * Created by slinkin on 25.05.2015.
 */
public class AppPreferences {
    public static String getOutputPath() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(App.getInstance());
        return prefs.getString("OutputDir", getDefaultOutputDir().toString());
    }

    private static File getDefaultOutputDir() {
        String folder = "Documents";
        if (Build.VERSION.SDK_INT >= 19)
            folder = Environment.DIRECTORY_DOCUMENTS;
        return new File(Environment.getExternalStoragePublicDirectory(folder), "Books");
    }

    public static Boolean trySetOutputPath(String path) {
        try {
            File f = new File(path);
            if (!f.exists() && f.mkdirs()) {
                throw new AppLog.NotReportException("Не могу создать указанный путь");
            }
            File testFile = new File(f, "fb2ls.test");
            if (testFile.exists())
                testFile.delete();

            if (!testFile.createNewFile())
                throw new AppLog.NotReportException("Не могу создать файл по указанному пути");
            testFile.delete();

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(App.getInstance());
            prefs.edit().putString("OutputDir", path).apply();
            return true;
        } catch (Throwable ex) {
            AppLog.e(App.getInstance(), new AppLog.NotReportException(ex.getMessage()));
            return false;
        }
    }
}
