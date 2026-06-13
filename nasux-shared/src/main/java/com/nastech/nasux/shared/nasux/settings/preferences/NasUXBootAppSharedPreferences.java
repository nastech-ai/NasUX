package com.nastech.nasux.shared.nasux.settings.preferences;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nastech.nasux.shared.logger.Logger;
import com.nastech.nasux.shared.android.PackageUtils;
import com.nastech.nasux.shared.settings.preferences.AppSharedPreferences;
import com.nastech.nasux.shared.settings.preferences.SharedPreferenceUtils;
import com.nastech.nasux.shared.nasux.NasUXUtils;
import com.nastech.nasux.shared.nasux.settings.preferences.NasUXPreferenceConstants.NASUX_BOOT_APP;
import com.nastech.nasux.shared.nasux.NasUXConstants;

public class NasUXBootAppSharedPreferences extends AppSharedPreferences {

    private static final String LOG_TAG = "NasUXBootAppSharedPreferences";

    private NasUXBootAppSharedPreferences(@NonNull Context context) {
        super(context,
            SharedPreferenceUtils.getPrivateSharedPreferences(context,
                NasUXConstants.NASUX_BOOT_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION),
            SharedPreferenceUtils.getPrivateAndMultiProcessSharedPreferences(context,
                NasUXConstants.NASUX_BOOT_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION));
    }

    /**
     * Get {@link NasUXBootAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link NasUXConstants#NASUX_BOOT_PACKAGE_NAME}.
     * @return Returns the {@link NasUXBootAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    @Nullable
    public static NasUXBootAppSharedPreferences build(@NonNull final Context context) {
        Context nasuxBootPackageContext = PackageUtils.getContextForPackage(context, NasUXConstants.NASUX_BOOT_PACKAGE_NAME);
        if (nasuxBootPackageContext == null)
            return null;
        else
            return new NasUXBootAppSharedPreferences(nasuxBootPackageContext);
    }

    /**
     * Get {@link NasUXBootAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link NasUXConstants#NASUX_BOOT_PACKAGE_NAME}.
     * @param exitAppOnError If {@code true} and failed to get package context, then a dialog will
     *                       be shown which when dismissed will exit the app.
     * @return Returns the {@link NasUXBootAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    public static NasUXBootAppSharedPreferences build(@NonNull final Context context, final boolean exitAppOnError) {
        Context nasuxBootPackageContext = NasUXUtils.getContextForPackageOrExitApp(context, NasUXConstants.NASUX_BOOT_PACKAGE_NAME, exitAppOnError);
        if (nasuxBootPackageContext == null)
            return null;
        else
            return new NasUXBootAppSharedPreferences(nasuxBootPackageContext);
    }



    public int getLogLevel(boolean readFromFile) {
        if (readFromFile)
            return SharedPreferenceUtils.getInt(mMultiProcessSharedPreferences, NASUX_BOOT_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL);
        else
            return SharedPreferenceUtils.getInt(mSharedPreferences, NASUX_BOOT_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL);
    }

    public void setLogLevel(Context context, int logLevel, boolean commitToFile) {
        logLevel = Logger.setLogLevel(context, logLevel);
        SharedPreferenceUtils.setInt(mSharedPreferences, NASUX_BOOT_APP.KEY_LOG_LEVEL, logLevel, commitToFile);
    }

}
