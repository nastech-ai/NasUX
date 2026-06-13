package com.nastech.nasux.shared.nasux.settings.preferences;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nastech.nasux.shared.logger.Logger;
import com.nastech.nasux.shared.android.PackageUtils;
import com.nastech.nasux.shared.settings.preferences.AppSharedPreferences;
import com.nastech.nasux.shared.settings.preferences.SharedPreferenceUtils;
import com.nastech.nasux.shared.nasux.NasUXUtils;
import com.nastech.nasux.shared.nasux.settings.preferences.NasUXPreferenceConstants.NASUX_WIDGET_APP;
import com.nastech.nasux.shared.nasux.NasUXConstants;

import java.util.UUID;

public class NasUXWidgetAppSharedPreferences extends AppSharedPreferences {

    private static final String LOG_TAG = "NasUXWidgetAppSharedPreferences";

    private NasUXWidgetAppSharedPreferences(@NonNull Context context) {
        super(context,
            SharedPreferenceUtils.getPrivateSharedPreferences(context,
                NasUXConstants.NASUX_WIDGET_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION),
            SharedPreferenceUtils.getPrivateAndMultiProcessSharedPreferences(context,
                NasUXConstants.NASUX_WIDGET_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION));
    }

    /**
     * Get {@link NasUXWidgetAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link NasUXConstants#NASUX_WIDGET_PACKAGE_NAME}.
     * @return Returns the {@link NasUXWidgetAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    @Nullable
    public static NasUXWidgetAppSharedPreferences build(@NonNull final Context context) {
        Context nasuxWidgetPackageContext = PackageUtils.getContextForPackage(context, NasUXConstants.NASUX_WIDGET_PACKAGE_NAME);
        if (nasuxWidgetPackageContext == null)
            return null;
        else
            return new NasUXWidgetAppSharedPreferences(nasuxWidgetPackageContext);
    }

    /**
     * Get the {@link NasUXWidgetAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link NasUXConstants#NASUX_WIDGET_PACKAGE_NAME}.
     * @param exitAppOnError If {@code true} and failed to get package context, then a dialog will
     *                       be shown which when dismissed will exit the app.
     * @return Returns the {@link NasUXWidgetAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    public static NasUXWidgetAppSharedPreferences build(@NonNull final Context context, final boolean exitAppOnError) {
        Context nasuxWidgetPackageContext = NasUXUtils.getContextForPackageOrExitApp(context, NasUXConstants.NASUX_WIDGET_PACKAGE_NAME, exitAppOnError);
        if (nasuxWidgetPackageContext == null)
            return null;
        else
            return new NasUXWidgetAppSharedPreferences(nasuxWidgetPackageContext);
    }



    public static String getGeneratedToken(@NonNull Context context) {
        NasUXWidgetAppSharedPreferences preferences = NasUXWidgetAppSharedPreferences.build(context, true);
        if (preferences == null) return null;
        return preferences.getGeneratedToken();
    }

    public String getGeneratedToken() {
        String token =  SharedPreferenceUtils.getString(mSharedPreferences, NASUX_WIDGET_APP.KEY_TOKEN, null, true);
        if (token == null) {
            token = UUID.randomUUID().toString();
            SharedPreferenceUtils.setString(mSharedPreferences, NASUX_WIDGET_APP.KEY_TOKEN, token, true);
        }
        return token;
    }



    public int getLogLevel(boolean readFromFile) {
        if (readFromFile)
            return SharedPreferenceUtils.getInt(mMultiProcessSharedPreferences, NASUX_WIDGET_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL);
        else
            return SharedPreferenceUtils.getInt(mSharedPreferences, NASUX_WIDGET_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL);
    }

    public void setLogLevel(Context context, int logLevel, boolean commitToFile) {
        logLevel = Logger.setLogLevel(context, logLevel);
        SharedPreferenceUtils.setInt(mSharedPreferences, NASUX_WIDGET_APP.KEY_LOG_LEVEL, logLevel, commitToFile);
    }

}
