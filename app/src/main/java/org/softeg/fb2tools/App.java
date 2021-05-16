package org.softeg.fb2tools;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import java.util.concurrent.atomic.AtomicInteger;

@ReportsCrashes(
        mailTo = "slartus@gmail.com",
        mode = ReportingInteractionMode.DIALOG,
        customReportContent = {ReportField.APP_VERSION_CODE, ReportField.APP_VERSION_NAME,
                ReportField.ANDROID_VERSION, ReportField.PHONE_MODEL,
                ReportField.CUSTOM_DATA, ReportField.STACK_TRACE, ReportField.LOGCAT},
        resToastText = R.string.crash_toast_text, // optional, displayed as soon as the crash occurs, before collecting data which can take a few seconds
        resDialogText = R.string.crash_dialog_text,
        resDialogIcon = android.R.drawable.ic_dialog_info, //optional. default is a warning sign
        resDialogTitle = R.string.crash_dialog_title, // optional. default is your application name
        resDialogCommentPrompt = R.string.crash_dialog_comment_prompt, // optional. when defined, adds a user text field input with this text resource as a label
        resDialogOkToast = R.string.crash_dialog_ok_toast // optional. displays a Toast message when the user accepts to send a report.)
)
public class App extends Application {
    private static App instance = new App();

    public App() {
        instance = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        ACRA.init(this);
    }

    public static App getInstance() {
        return instance;
    }

    private AtomicInteger m_AtomicInteger = new AtomicInteger();

    public int getUniqueIntValue() {
        return m_AtomicInteger.incrementAndGet();
    }

    public String getProgramFullName() {
        String programName = getString(R.string.app_name);
        try {
            String packageName = getPackageName();
            PackageInfo pInfo = getPackageManager().getPackageInfo(
                    packageName, PackageManager.GET_META_DATA);

            programName += " v" + pInfo.versionName + " c" + pInfo.versionCode;

        } catch (PackageManager.NameNotFoundException e1) {
            AppLog.e(null, e1);
        }
        return programName;
    }
}
