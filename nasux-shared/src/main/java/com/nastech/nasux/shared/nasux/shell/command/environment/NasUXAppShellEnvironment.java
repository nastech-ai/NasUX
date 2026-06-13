package com.nastech.nasux.shared.nasux.shell.command.environment;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nastech.nasux.shared.android.PackageUtils;
import com.nastech.nasux.shared.android.SELinuxUtils;
import com.nastech.nasux.shared.data.DataUtils;
import com.nastech.nasux.shared.shell.command.environment.ShellEnvironmentUtils;
import com.nastech.nasux.shared.nasux.NasUXBootstrap;
import com.nastech.nasux.shared.nasux.NasUXConstants;
import com.nastech.nasux.shared.nasux.NasUXUtils;
import com.nastech.nasux.shared.nasux.shell.am.NasUXAmSocketServer;

import java.util.HashMap;

/**
 * Environment for {@link NasUXConstants#NASUX_PACKAGE_NAME} app.
 */
public class NasUXAppShellEnvironment {

    /** NasUX app environment variables. */
    public static HashMap<String, String> nasuxAppEnvironment;

    /** Environment variable for the NasUX app version. */
    public static final String ENV_NASUX_VERSION = NasUXConstants.NASUX_ENV_PREFIX_ROOT + "_VERSION";

    /** Environment variable prefix for the NasUX app. */
    public static final String NASUX_APP_ENV_PREFIX = NasUXConstants.NASUX_ENV_PREFIX_ROOT + "_APP__";

    /** Environment variable for the NasUX app version name. */
    public static final String ENV_NASUX_APP__VERSION_NAME = NASUX_APP_ENV_PREFIX + "VERSION_NAME";
    /** Environment variable for the NasUX app version code. */
    public static final String ENV_NASUX_APP__VERSION_CODE = NASUX_APP_ENV_PREFIX + "VERSION_CODE";
    /** Environment variable for the NasUX app package name. */
    public static final String ENV_NASUX_APP__PACKAGE_NAME = NASUX_APP_ENV_PREFIX + "PACKAGE_NAME";
    /** Environment variable for the NasUX app process id. */
    public static final String ENV_NASUX_APP__PID = NASUX_APP_ENV_PREFIX + "PID";
    /** Environment variable for the NasUX app uid. */
    public static final String ENV_NASUX_APP__UID = NASUX_APP_ENV_PREFIX + "UID";
    /** Environment variable for the NasUX app targetSdkVersion. */
    public static final String ENV_NASUX_APP__TARGET_SDK = NASUX_APP_ENV_PREFIX + "TARGET_SDK";
    /** Environment variable for the NasUX app is debuggable apk build. */
    public static final String ENV_NASUX_APP__IS_DEBUGGABLE_BUILD = NASUX_APP_ENV_PREFIX + "IS_DEBUGGABLE_BUILD";
    /** Environment variable for the NasUX app {@link NasUXConstants} APK_RELEASE_*. */
    public static final String ENV_NASUX_APP__APK_RELEASE = NASUX_APP_ENV_PREFIX + "APK_RELEASE";
    /** Environment variable for the NasUX app install path. */
    public static final String ENV_NASUX_APP__APK_PATH = NASUX_APP_ENV_PREFIX + "APK_PATH";
    /** Environment variable for the NasUX app is installed on external/portable storage. */
    public static final String ENV_NASUX_APP__IS_INSTALLED_ON_EXTERNAL_STORAGE = NASUX_APP_ENV_PREFIX + "IS_INSTALLED_ON_EXTERNAL_STORAGE";

    /** Environment variable for the NasUX app process selinux context. */
    public static final String ENV_NASUX_APP__SE_PROCESS_CONTEXT = NASUX_APP_ENV_PREFIX + "SE_PROCESS_CONTEXT";
    /** Environment variable for the NasUX app data files selinux context. */
    public static final String ENV_NASUX_APP__SE_FILE_CONTEXT = NASUX_APP_ENV_PREFIX + "SE_FILE_CONTEXT";
    /** Environment variable for the NasUX app seInfo tag found in selinux policy used to set app process and app data files selinux context. */
    public static final String ENV_NASUX_APP__SE_INFO = NASUX_APP_ENV_PREFIX + "SE_INFO";
    /** Environment variable for the NasUX app user id. */
    public static final String ENV_NASUX_APP__USER_ID = NASUX_APP_ENV_PREFIX + "USER_ID";
    /** Environment variable for the NasUX app profile owner. */
    public static final String ENV_NASUX_APP__PROFILE_OWNER = NASUX_APP_ENV_PREFIX + "PROFILE_OWNER";

    /** Environment variable for the NasUX app {@link NasUXBootstrap#NASUX_APP_PACKAGE_MANAGER}. */
    public static final String ENV_NASUX_APP__PACKAGE_MANAGER = NASUX_APP_ENV_PREFIX + "PACKAGE_MANAGER";
    /** Environment variable for the NasUX app {@link NasUXBootstrap#NASUX_APP_PACKAGE_VARIANT}. */
    public static final String ENV_NASUX_APP__PACKAGE_VARIANT = NASUX_APP_ENV_PREFIX + "PACKAGE_VARIANT";
    /** Environment variable for the NasUX app files directory. */
    public static final String ENV_NASUX_APP__FILES_DIR = NASUX_APP_ENV_PREFIX + "FILES_DIR";


    /** Environment variable for the NasUX app {@link NasUXAmSocketServer#getNasUXAppAMSocketServerEnabled(Context)}. */
    public static final String ENV_NASUX_APP__AM_SOCKET_SERVER_ENABLED = NASUX_APP_ENV_PREFIX + "AM_SOCKET_SERVER_ENABLED";



    /** Get shell environment for NasUX app. */
    @Nullable
    public static HashMap<String, String> getEnvironment(@NonNull Context currentPackageContext) {
        setNasUXAppEnvironment(currentPackageContext);
        return nasuxAppEnvironment;
    }

    /** Set NasUX app environment variables in {@link #nasuxAppEnvironment}. */
    public synchronized static void setNasUXAppEnvironment(@NonNull Context currentPackageContext) {
        boolean isNasUXApp = NasUXConstants.NASUX_PACKAGE_NAME.equals(currentPackageContext.getPackageName());

        // If current package context is of nasux app and its environment is already set, then no need to set again since it won't change
        // Other apps should always set environment again since nasux app may be installed/updated/deleted in background
        if (nasuxAppEnvironment != null && isNasUXApp)
            return;

        nasuxAppEnvironment = null;

        String packageName = NasUXConstants.NASUX_PACKAGE_NAME;
        PackageInfo packageInfo = PackageUtils.getPackageInfoForPackage(currentPackageContext, packageName);
        if (packageInfo == null) return;
        ApplicationInfo applicationInfo = PackageUtils.getApplicationInfoForPackage(currentPackageContext, packageName);
        if (applicationInfo == null || !applicationInfo.enabled) return;

        HashMap<String, String> environment = new HashMap<>();

        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_NASUX_VERSION, PackageUtils.getVersionNameForPackage(packageInfo));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_NASUX_APP__VERSION_NAME, PackageUtils.getVersionNameForPackage(packageInfo));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_NASUX_APP__VERSION_CODE, String.valueOf(PackageUtils.getVersionCodeForPackage(packageInfo)));

        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_NASUX_APP__PACKAGE_NAME, packageName);
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_NASUX_APP__PID, NasUXUtils.getNasUXAppPID(currentPackageContext));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_NASUX_APP__UID, String.valueOf(PackageUtils.getUidForPackage(applicationInfo)));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_NASUX_APP__TARGET_SDK, String.valueOf(PackageUtils.getTargetSDKForPackage(applicationInfo)));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_NASUX_APP__IS_DEBUGGABLE_BUILD, PackageUtils.isAppForPackageADebuggableBuild(applicationInfo));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_NASUX_APP__APK_PATH, PackageUtils.getBaseAPKPathForPackage(applicationInfo));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_NASUX_APP__IS_INSTALLED_ON_EXTERNAL_STORAGE, PackageUtils.isAppInstalledOnExternalStorage(applicationInfo));

        putNasUXAPKSignature(currentPackageContext, environment);

        Context nasuxPackageContext = NasUXUtils.getNasUXPackageContext(currentPackageContext);
        if (nasuxPackageContext != null) {
            // An app that does not have the same sharedUserId as nasux app will not be able to get
            // get nasux context's classloader to get BuildConfig.NASUX_PACKAGE_VARIANT via reflection.
            // Check NasUXBootstrap.setNasUXPackageManagerAndVariantFromNasUXApp()
            if (NasUXBootstrap.NASUX_APP_PACKAGE_MANAGER != null)
                environment.put(ENV_NASUX_APP__PACKAGE_MANAGER, NasUXBootstrap.NASUX_APP_PACKAGE_MANAGER.getName());
            if (NasUXBootstrap.NASUX_APP_PACKAGE_VARIANT != null)
                environment.put(ENV_NASUX_APP__PACKAGE_VARIANT, NasUXBootstrap.NASUX_APP_PACKAGE_VARIANT.getName());

            // Will not be set for plugins
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_NASUX_APP__AM_SOCKET_SERVER_ENABLED,
                NasUXAmSocketServer.getNasUXAppAMSocketServerEnabled(currentPackageContext));

            String filesDirPath = currentPackageContext.getFilesDir().getAbsolutePath();
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_NASUX_APP__FILES_DIR, filesDirPath);

            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_NASUX_APP__SE_PROCESS_CONTEXT, SELinuxUtils.getContext());
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_NASUX_APP__SE_FILE_CONTEXT, SELinuxUtils.getFileContext(filesDirPath));

            String seInfoUser = PackageUtils.getApplicationInfoSeInfoUserForPackage(applicationInfo);
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_NASUX_APP__SE_INFO, PackageUtils.getApplicationInfoSeInfoForPackage(applicationInfo) +
                (DataUtils.isNullOrEmpty(seInfoUser) ? "" : seInfoUser));

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_NASUX_APP__USER_ID, String.valueOf(PackageUtils.getUserIdForPackage(currentPackageContext)));
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_NASUX_APP__PROFILE_OWNER, PackageUtils.getProfileOwnerPackageNameForUser(currentPackageContext));
        }

        nasuxAppEnvironment = environment;
    }

    /** Put {@link #ENV_NASUX_APP__APK_RELEASE} in {@code environment}. */
    public static void putNasUXAPKSignature(@NonNull Context currentPackageContext,
                                             @NonNull HashMap<String, String> environment) {
        String signingCertificateSHA256Digest = PackageUtils.getSigningCertificateSHA256DigestForPackage(currentPackageContext,
            NasUXConstants.NASUX_PACKAGE_NAME);
        if (signingCertificateSHA256Digest != null) {
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_NASUX_APP__APK_RELEASE,
                NasUXUtils.getAPKRelease(signingCertificateSHA256Digest).replaceAll("[^a-zA-Z]", "_").toUpperCase());
        }
    }

    /** Update {@link #ENV_NASUX_APP__AM_SOCKET_SERVER_ENABLED} value in {@code environment}. */
    public synchronized static void updateNasUXAppAMSocketServerEnabled(@NonNull Context currentPackageContext) {
        if (nasuxAppEnvironment == null) return;
        nasuxAppEnvironment.remove(ENV_NASUX_APP__AM_SOCKET_SERVER_ENABLED);
        ShellEnvironmentUtils.putToEnvIfSet(nasuxAppEnvironment, ENV_NASUX_APP__AM_SOCKET_SERVER_ENABLED,
            NasUXAmSocketServer.getNasUXAppAMSocketServerEnabled(currentPackageContext));
    }

}
