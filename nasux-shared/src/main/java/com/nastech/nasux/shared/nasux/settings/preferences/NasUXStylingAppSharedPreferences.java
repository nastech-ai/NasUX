package com.nastech.nasux.shared.nasux.settings.preferences;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nastech.nasux.shared.logger.Logger;
import com.nastech.nasux.shared.android.PackageUtils;
import com.nastech.nasux.shared.settings.preferences.AppSharedPreferences;
import com.nastech.nasux.shared.settings.preferences.SharedPreferenceUtils;
import com.nastech.nasux.shared.nasux.NasUXUtils;
import com.nastech.nasux.shared.nasux.settings.preferences.NasUXPreferenceConstants.NASUX_STYLING_APP;
import com.nastech.nasux.shared.nasux.NasUXConstants;

public class NasUXStylingAppSharedPreferences extends AppSharedPreferences {

    private static final String LOG_TAG = "NasUXStylingAppSharedPreferences";

    private NasUXStylingAppSharedPreferences(@NonNull Context context) {
        super(context,
            SharedPreferenceUtils.getPrivateSharedPreferences(context,
                NasUXConstants.NASUX_STYLING_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION),
            SharedPreferenceUtils.getPrivateAndMultiProcessSharedPreferences(context,
                NasUXConstants.NASUX_STYLING_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION));
    }

    /**
     * Get {@link NasUXStylingAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link NasUXConstants#NASUX_STYLING_PACKAGE_NAME}.
     * @return Returns the {@link NasUXStylingAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    @Nullable
    public static NasUXStylingAppSharedPreferences build(@NonNull final Context context) {
        Context nasuxStylingPackageContext = PackageUtils.getContextForPackage(context, NasUXConstants.NASUX_STYLING_PACKAGE_NAME);
        if (nasuxStylingPackageContext == null)
            return null;
        else
            return new NasUXStylingAppSharedPreferences(nasuxStylingPackageContext);
    }

    /**
     * Get {@link NasUXStylingAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link NasUXConstants#NASUX_STYLING_PACKAGE_NAME}.
     * @param exitAppOnError If {@code true} and failed to get package context, then a dialog will
     *                       be shown which when dismissed will exit the app.
     * @return Returns the {@link NasUXStylingAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    public static NasUXStylingAppSharedPreferences build(@NonNull final Context context, final boolean exitAppOnError) {
        Context nasuxStylingPackageContext = NasUXUtils.getContextForPackageOrExitApp(context, NasUXConstants.NASUX_STYLING_PACKAGE_NAME, exitAppOnError);
        if (nasuxStylingPackageContext == null)
            return null;
        else
            return new NasUXStylingAppSharedPreferences(nasuxStylingPackageContext);
    }



    public int getLogLevel(boolean readFromFile) {
        if (readFromFile)
            return SharedPreferenceUtils.getInt(mMultiProcessSharedPreferences, NASUX_STYLING_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL);
        else
            return SharedPreferenceUtils.getInt(mSharedPreferences, NASUX_STYLING_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL);
    }

    public void setLogLevel(Context context, int logLevel, boolean commitToFile) {
        logLevel = Logger.setLogLevel(context, logLevel);
        SharedPreferenceUtils.setInt(mSharedPreferences, NASUX_STYLING_APP.KEY_LOG_LEVEL, logLevel, commitToFile);
    }

}
