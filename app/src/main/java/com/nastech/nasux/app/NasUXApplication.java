package com.nastech.nasux.app;

import android.app.Application;
import android.content.Context;

import com.nastech.nasux.BuildConfig;
import com.nastech.nasux.shared.errors.Error;
import com.nastech.nasux.shared.logger.Logger;
import com.nastech.nasux.shared.nasux.NasUXBootstrap;
import com.nastech.nasux.shared.nasux.NasUXConstants;
import com.nastech.nasux.shared.nasux.crash.NasUXCrashUtils;
import com.nastech.nasux.shared.nasux.file.NasUXFileUtils;
import com.nastech.nasux.shared.nasux.settings.preferences.NasUXAppSharedPreferences;
import com.nastech.nasux.shared.nasux.settings.properties.NasUXAppSharedProperties;
import com.nastech.nasux.shared.nasux.shell.command.environment.NasUXShellEnvironment;
import com.nastech.nasux.shared.nasux.shell.am.NasUXAmSocketServer;
import com.nastech.nasux.shared.nasux.shell.NasUXShellManager;
import com.nastech.nasux.shared.nasux.theme.NasUXThemeUtils;

public class NasUXApplication extends Application {

    private static final String LOG_TAG = "NasUXApplication";

    public void onCreate() {
        super.onCreate();

        Context context = getApplicationContext();

        // Set crash handler for the app
        NasUXCrashUtils.setDefaultCrashHandler(this);

        // Set log config for the app
        setLogConfig(context);

        Logger.logDebug("Starting Application");

        // Set NasUXBootstrap.NASUX_APP_PACKAGE_MANAGER and NasUXBootstrap.NASUX_APP_PACKAGE_VARIANT
        NasUXBootstrap.setNasUXPackageManagerAndVariant(BuildConfig.NASUX_PACKAGE_VARIANT);

        // Init app wide SharedProperties loaded from nasux.properties
        NasUXAppSharedProperties properties = NasUXAppSharedProperties.init(context);

        // Init app wide shell manager
        NasUXShellManager shellManager = NasUXShellManager.init(context);

        // Set NightMode.APP_NIGHT_MODE
        NasUXThemeUtils.setAppNightMode(properties.getNightMode());

        // Check and create nasux files directory. If failed to access it like in case of secondary
        // user or external sd card installation, then don't run files directory related code
        Error error = NasUXFileUtils.isNasUXFilesDirectoryAccessible(this, true, true);
        boolean isNasUXFilesDirectoryAccessible = error == null;
        if (isNasUXFilesDirectoryAccessible) {
            Logger.logInfo(LOG_TAG, "NasUX files directory is accessible");

            error = NasUXFileUtils.isAppsNasUXAppDirectoryAccessible(true, true);
            if (error != null) {
                Logger.logErrorExtended(LOG_TAG, "Create apps/nasux-app directory failed\n" + error);
                return;
            }

            // Setup nasux-am-socket server
            NasUXAmSocketServer.setupNasUXAmSocketServer(context);
        } else {
            Logger.logErrorExtended(LOG_TAG, "NasUX files directory is not accessible\n" + error);
        }

        // Init NasUXShellEnvironment constants and caches after everything has been setup including nasux-am-socket server
        NasUXShellEnvironment.init(this);

        if (isNasUXFilesDirectoryAccessible) {
            NasUXShellEnvironment.writeEnvironmentToFile(this);
        }
    }

    public static void setLogConfig(Context context) {
        Logger.setDefaultLogTag(NasUXConstants.NASUX_APP_NAME);

        // Load the log level from shared preferences and set it to the {@link Logger.CURRENT_LOG_LEVEL}
        NasUXAppSharedPreferences preferences = NasUXAppSharedPreferences.build(context);
        if (preferences == null) return;
        preferences.setLogLevel(null, preferences.getLogLevel());
    }

}
