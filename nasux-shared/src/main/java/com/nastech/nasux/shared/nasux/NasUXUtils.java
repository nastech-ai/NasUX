package com.nastech.nasux.shared.nasux;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nastech.nasux.shared.R;
import com.nastech.nasux.shared.android.AndroidUtils;
import com.nastech.nasux.shared.data.DataUtils;
import com.nastech.nasux.shared.file.FileUtils;
import com.nastech.nasux.shared.reflection.ReflectionUtils;
import com.nastech.nasux.shared.shell.command.runner.app.AppShell;
import com.nastech.nasux.shared.nasux.file.NasUXFileUtils;
import com.nastech.nasux.shared.logger.Logger;
import com.nastech.nasux.shared.markdown.MarkdownUtils;
import com.nastech.nasux.shared.shell.command.ExecutionCommand;
import com.nastech.nasux.shared.errors.Error;
import com.nastech.nasux.shared.android.PackageUtils;
import com.nastech.nasux.shared.nasux.NasUXConstants.NASUX_APP;
import com.nastech.nasux.shared.nasux.shell.command.environment.NasUXShellEnvironment;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.regex.Pattern;

public class NasUXUtils {

    /** The modes used by {@link #getAppInfoMarkdownString(Context, AppInfoMode, String)}. */
    public enum AppInfoMode {
        /** Get info for NasUX app only. */
        NASUX_PACKAGE,
        /** Get info for NasUX app and plugin app if context is of plugin app. */
        NASUX_AND_PLUGIN_PACKAGE,
        /** Get info for NasUX app and its plugins listed in {@link NasUXConstants#NASUX_PLUGIN_APP_PACKAGE_NAMES_LIST}. */
        NASUX_AND_PLUGIN_PACKAGES,
        /* Get info for all the NasUX app plugins listed in {@link NasUXConstants#NASUX_PLUGIN_APP_PACKAGE_NAMES_LIST}. */
        NASUX_PLUGIN_PACKAGES,
        /* Get info for NasUX app and the calling package that called a NasUX API. */
        NASUX_AND_CALLING_PACKAGE,
    }

    private static final String LOG_TAG = "NasUXUtils";

    /**
     * Get the {@link Context} for {@link NasUXConstants#NASUX_PACKAGE_NAME} package with the
     * {@link Context#CONTEXT_RESTRICTED} flag.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the package.
     * @return Returns the {@link Context}. This will {@code null} if an exception is raised.
     */
    public static Context getNasUXPackageContext(@NonNull Context context) {
        return PackageUtils.getContextForPackage(context, NasUXConstants.NASUX_PACKAGE_NAME);
    }

    /**
     * Get the {@link Context} for {@link NasUXConstants#NASUX_PACKAGE_NAME} package with the
     * {@link Context#CONTEXT_INCLUDE_CODE} flag.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the package.
     * @return Returns the {@link Context}. This will {@code null} if an exception is raised.
     */
    public static Context getNasUXPackageContextWithCode(@NonNull Context context) {
        return PackageUtils.getContextForPackage(context, NasUXConstants.NASUX_PACKAGE_NAME, Context.CONTEXT_INCLUDE_CODE);
    }

    /**
     * Get the {@link Context} for {@link NasUXConstants#NASUX_API_PACKAGE_NAME} package.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the package.
     * @return Returns the {@link Context}. This will {@code null} if an exception is raised.
     */
    public static Context getNasUXAPIPackageContext(@NonNull Context context) {
        return PackageUtils.getContextForPackage(context, NasUXConstants.NASUX_API_PACKAGE_NAME);
    }

    /**
     * Get the {@link Context} for {@link NasUXConstants#NASUX_BOOT_PACKAGE_NAME} package.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the package.
     * @return Returns the {@link Context}. This will {@code null} if an exception is raised.
     */
    public static Context getNasUXBootPackageContext(@NonNull Context context) {
        return PackageUtils.getContextForPackage(context, NasUXConstants.NASUX_BOOT_PACKAGE_NAME);
    }

    /**
     * Get the {@link Context} for {@link NasUXConstants#NASUX_FLOAT_PACKAGE_NAME} package.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the package.
     * @return Returns the {@link Context}. This will {@code null} if an exception is raised.
     */
    public static Context getNasUXFloatPackageContext(@NonNull Context context) {
        return PackageUtils.getContextForPackage(context, NasUXConstants.NASUX_FLOAT_PACKAGE_NAME);
    }

    /**
     * Get the {@link Context} for {@link NasUXConstants#NASUX_STYLING_PACKAGE_NAME} package.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the package.
     * @return Returns the {@link Context}. This will {@code null} if an exception is raised.
     */
    public static Context getNasUXStylingPackageContext(@NonNull Context context) {
        return PackageUtils.getContextForPackage(context, NasUXConstants.NASUX_STYLING_PACKAGE_NAME);
    }

    /**
     * Get the {@link Context} for {@link NasUXConstants#NASUX_TASKER_PACKAGE_NAME} package.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the package.
     * @return Returns the {@link Context}. This will {@code null} if an exception is raised.
     */
    public static Context getNasUXTaskerPackageContext(@NonNull Context context) {
        return PackageUtils.getContextForPackage(context, NasUXConstants.NASUX_TASKER_PACKAGE_NAME);
    }

    /**
     * Get the {@link Context} for {@link NasUXConstants#NASUX_WIDGET_PACKAGE_NAME} package.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the package.
     * @return Returns the {@link Context}. This will {@code null} if an exception is raised.
     */
    public static Context getNasUXWidgetPackageContext(@NonNull Context context) {
        return PackageUtils.getContextForPackage(context, NasUXConstants.NASUX_WIDGET_PACKAGE_NAME);
    }

    /** Wrapper for {@link PackageUtils#getContextForPackageOrExitApp(Context, String, boolean, String)}. */
    public static Context getContextForPackageOrExitApp(@NonNull Context context, String packageName,
                                                        final boolean exitAppOnError) {
        return PackageUtils.getContextForPackageOrExitApp(context, packageName, exitAppOnError, NasUXConstants.NASUX_GITHUB_REPO_URL);
    }

    /**
     * Check if NasUX app is installed and enabled. This can be used by external apps that don't
     * share `sharedUserId` with the NasUX app.
     *
     * If your third-party app is targeting sdk `30` (android `11`), then it needs to add `com.nastech.nasux`
     * package to the `queries` element or request `QUERY_ALL_PACKAGES` permission in its
     * `AndroidManifest.xml`. Otherwise it will get `PackageSetting{...... com.nastech.nasux/......} BLOCKED`
     * errors in `logcat` and `RUN_COMMAND` won't work.
     * Check [package-visibility](https://developer.android.com/training/basics/intents/package-visibility#package-name),
     * `QUERY_ALL_PACKAGES` [googleplay policy](https://support.google.com/googleplay/android-developer/answer/10158779
     * and this [article](https://medium.com/androiddevelopers/working-with-package-visibility-dc252829de2d) for more info.
     *
     * {@code
     * <manifest
     *     <queries>
     *         <package android:name="com.nastech.nasux" />
     *    </queries>
     * </manifest>
     * }
     *
     * @param context The context for operations.
     * @return Returns {@code errmsg} if {@link NasUXConstants#NASUX_PACKAGE_NAME} is not installed
     * or disabled, otherwise {@code null}.
     */
    public static String isNasUXAppInstalled(@NonNull final Context context) {
        return PackageUtils.isAppInstalled(context, NasUXConstants.NASUX_APP_NAME, NasUXConstants.NASUX_PACKAGE_NAME);
    }

    /**
     * Check if NasUX:API app is installed and enabled. This can be used by external apps that don't
     * share `sharedUserId` with the NasUX:API app.
     *
     * @param context The context for operations.
     * @return Returns {@code errmsg} if {@link NasUXConstants#NASUX_API_PACKAGE_NAME} is not installed
     * or disabled, otherwise {@code null}.
     */
    public static String isNasUXAPIAppInstalled(@NonNull final Context context) {
        return PackageUtils.isAppInstalled(context, NasUXConstants.NASUX_API_APP_NAME, NasUXConstants.NASUX_API_PACKAGE_NAME);
    }

    /**
     * Check if NasUX app is installed and accessible. This can only be used by apps that share
     * `sharedUserId` with the NasUX app.
     *
     * This is done by checking if first checking if app is installed and enabled and then if
     * {@code currentPackageContext} can be used to get the {@link Context} of the app with
     * {@link NasUXConstants#NASUX_PACKAGE_NAME} and then if
     * {@link NasUXConstants#NASUX_PREFIX_DIR_PATH} exists and has
     * {@link FileUtils#APP_WORKING_DIRECTORY_PERMISSIONS} permissions. The directory will not
     * be automatically created and neither the missing permissions automatically set.
     *
     * @param currentPackageContext The context of current package.
     * @return Returns {@code errmsg} if failed to get nasux package {@link Context} or
     * {@link NasUXConstants#NASUX_PREFIX_DIR_PATH} is accessible, otherwise {@code null}.
     */
    public static String isNasUXAppAccessible(@NonNull final Context currentPackageContext) {
        String errmsg = isNasUXAppInstalled(currentPackageContext);
        if (errmsg == null) {
            Context nasuxPackageContext = NasUXUtils.getNasUXPackageContext(currentPackageContext);
            // If failed to get NasUX app package context
            if (nasuxPackageContext == null)
                errmsg = currentPackageContext.getString(R.string.error_nasux_app_package_context_not_accessible);

            if (errmsg == null) {
                // If NasUXConstants.NASUX_PREFIX_DIR_PATH is not a directory or does not have required permissions
                Error error = NasUXFileUtils.isNasUXPrefixDirectoryAccessible(false, false);
                if (error != null)
                    errmsg = currentPackageContext.getString(R.string.error_nasux_prefix_dir_path_not_accessible,
                        PackageUtils.getAppNameForPackage(currentPackageContext));
            }
        }

        if (errmsg != null)
            return errmsg + " " + currentPackageContext.getString(R.string.msg_nasux_app_required_by_app,
                PackageUtils.getAppNameForPackage(currentPackageContext));
        else
            return null;
    }



    /**
     * Get a field value from the {@link NASUX_APP#BUILD_CONFIG_CLASS_NAME} class of the NasUX app
     * APK installed on the device.
     * This can only be used by apps that share `sharedUserId` with the NasUX app.
     *
     * This is a wrapper for {@link #getNasUXAppAPKClassField(Context, String, String)}.
     *
     * @param currentPackageContext The context of current package.
     * @param fieldName The name of the field to get.
     * @return Returns the field value, otherwise {@code null} if an exception was raised or failed
     * to get nasux app package context.
     */
    public static Object getNasUXAppAPKBuildConfigClassField(@NonNull Context currentPackageContext,
                                                              @NonNull String fieldName) {
        return getNasUXAppAPKClassField(currentPackageContext, NASUX_APP.BUILD_CONFIG_CLASS_NAME, fieldName);
    }

    /**
     * Get a field value from a class of the NasUX app APK installed on the device.
     * This can only be used by apps that share `sharedUserId` with the NasUX app.
     *
     * This is done by getting first getting nasux app package context and then getting in class
     * loader (instead of current app's) that contains nasux app class info, and then using that to
     * load the required class and then getting required field from it.
     *
     * Note that the value returned is from the APK file and not the current value loaded in NasUX
     * app process, so only default values will be returned.
     *
     * Trying to access {@code null} fields will result in {@link NoSuchFieldException}.
     *
     * @param currentPackageContext The context of current package.
     * @param clazzName The name of the class from which to get the field.
     * @param fieldName The name of the field to get.
     * @return Returns the field value, otherwise {@code null} if an exception was raised or failed
     * to get nasux app package context.
     */
    public static Object getNasUXAppAPKClassField(@NonNull Context currentPackageContext,
                                                   @NonNull String clazzName, @NonNull String fieldName) {
        try {
            Context nasuxPackageContext = NasUXUtils.getNasUXPackageContextWithCode(currentPackageContext);
            if (nasuxPackageContext == null)
                return null;

            Class<?> clazz = nasuxPackageContext.getClassLoader().loadClass(clazzName);
            return ReflectionUtils.invokeField(clazz, fieldName, null).value;
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to get \"" + fieldName + "\" value from \"" + clazzName + "\" class", e);
            return null;
        }
    }



    /** Returns {@code true} if {@link Uri} has `package:` scheme for {@link NasUXConstants#NASUX_PACKAGE_NAME} or its sub plugin package. */
    public static boolean isUriDataForNasUXOrPluginPackage(@NonNull Uri data) {
        return data.toString().equals("package:" + NasUXConstants.NASUX_PACKAGE_NAME) ||
            data.toString().startsWith("package:" + NasUXConstants.NASUX_PACKAGE_NAME + ".");
    }

    /** Returns {@code true} if {@link Uri} has `package:` scheme for {@link NasUXConstants#NASUX_PACKAGE_NAME} sub plugin package. */
    public static boolean isUriDataForNasUXPluginPackage(@NonNull Uri data) {
        return data.toString().startsWith("package:" + NasUXConstants.NASUX_PACKAGE_NAME + ".");
    }

    /**
     * Send the {@link NasUXConstants#BROADCAST_NASUX_OPENED} broadcast to notify apps that NasUX
     * app has been opened.
     *
     * @param context The Context to send the broadcast.
     */
    public static void sendNasUXOpenedBroadcast(@NonNull Context context) {
        Intent broadcast = new Intent(NasUXConstants.BROADCAST_NASUX_OPENED);
        List<ResolveInfo> matches = context.getPackageManager().queryBroadcastReceivers(broadcast, 0);

        // send broadcast to registered NasUX receivers
        // this technique is needed to work around broadcast changes that Oreo introduced
        for (ResolveInfo info : matches) {
            Intent explicitBroadcast = new Intent(broadcast);
            ComponentName cname = new ComponentName(info.activityInfo.applicationInfo.packageName,
                info.activityInfo.name);
            explicitBroadcast.setComponent(cname);
            context.sendBroadcast(explicitBroadcast);
        }
    }



    /**
     * Wrapper for {@link #getAppInfoMarkdownString(Context, AppInfoMode, String)}.
     *
     * @param currentPackageContext The context of current package.
     * @param appInfoMode The {@link AppInfoMode} to decide the app info required.
     * @return Returns the markdown {@link String}.
     */
    public static String getAppInfoMarkdownString(final Context currentPackageContext, final AppInfoMode appInfoMode) {
        return getAppInfoMarkdownString(currentPackageContext, appInfoMode, null);
    }

    /**
     * Get a markdown {@link String} for the apps info of nasux app, its installed plugin apps or
     * external apps that called a NasUX API depending on {@link AppInfoMode} passed.
     *
     * Also check {@link PackageUtils#isAppInstalled(Context, String, String) if targetting targeting
     * sdk `30` (android `11`) since {@link PackageManager.NameNotFoundException} may be thrown while
     * getting info of {@code callingPackageName} app.
     *
     * @param currentPackageContext The context of current package.
     * @param appInfoMode The {@link AppInfoMode} to decide the app info required.
     * @param callingPackageName The optional package name for a plugin or external app.
     * @return Returns the markdown {@link String}.
     */
    public static String getAppInfoMarkdownString(final Context currentPackageContext, final AppInfoMode appInfoMode, @Nullable String callingPackageName) {
        if (appInfoMode == null) return null;

        StringBuilder appInfo = new StringBuilder();
        switch (appInfoMode) {
            case NASUX_PACKAGE:
                return getAppInfoMarkdownString(currentPackageContext, false);

            case NASUX_AND_PLUGIN_PACKAGE:
                return getAppInfoMarkdownString(currentPackageContext, true);

            case NASUX_AND_PLUGIN_PACKAGES:
                appInfo.append(NasUXUtils.getAppInfoMarkdownString(currentPackageContext, false));

                String nasuxPluginAppsInfo =  NasUXUtils.getNasUXPluginAppsInfoMarkdownString(currentPackageContext);
                if (nasuxPluginAppsInfo != null)
                    appInfo.append("\n\n").append(nasuxPluginAppsInfo);
                return appInfo.toString();

            case NASUX_PLUGIN_PACKAGES:
                return NasUXUtils.getNasUXPluginAppsInfoMarkdownString(currentPackageContext);

            case NASUX_AND_CALLING_PACKAGE:
                appInfo.append(NasUXUtils.getAppInfoMarkdownString(currentPackageContext, false));
                if (!DataUtils.isNullOrEmpty(callingPackageName)) {
                    String callingPackageAppInfo = null;
                    if (NasUXConstants.NASUX_PLUGIN_APP_PACKAGE_NAMES_LIST.contains(callingPackageName)) {
                        Context nasuxPluginAppContext = PackageUtils.getContextForPackage(currentPackageContext, callingPackageName);
                        if (nasuxPluginAppContext != null)
                            appInfo.append(getAppInfoMarkdownString(nasuxPluginAppContext, false));
                        else
                            callingPackageAppInfo = AndroidUtils.getAppInfoMarkdownString(currentPackageContext, callingPackageName);
                    } else {
                        callingPackageAppInfo = AndroidUtils.getAppInfoMarkdownString(currentPackageContext, callingPackageName);
                    }

                    if (callingPackageAppInfo != null) {
                        ApplicationInfo applicationInfo = PackageUtils.getApplicationInfoForPackage(currentPackageContext, callingPackageName);
                        if (applicationInfo != null) {
                            appInfo.append("\n\n## ").append(PackageUtils.getAppNameForPackage(currentPackageContext, applicationInfo)).append(" App Info\n");
                            appInfo.append(callingPackageAppInfo);
                            appInfo.append("\n##\n");
                        }
                    }
                }
                return appInfo.toString();

            default:
                return null;
        }

    }

    /**
     * Get a markdown {@link String} for the apps info of all/any nasux plugin apps installed.
     *
     * @param currentPackageContext The context of current package.
     * @return Returns the markdown {@link String}.
     */
    public static String getNasUXPluginAppsInfoMarkdownString(final Context currentPackageContext) {
        if (currentPackageContext == null) return "null";

        StringBuilder markdownString = new StringBuilder();

        List<String> nasuxPluginAppPackageNamesList = NasUXConstants.NASUX_PLUGIN_APP_PACKAGE_NAMES_LIST;

        if (nasuxPluginAppPackageNamesList != null) {
            for (int i = 0; i < nasuxPluginAppPackageNamesList.size(); i++) {
                String nasuxPluginAppPackageName = nasuxPluginAppPackageNamesList.get(i);
                Context nasuxPluginAppContext = PackageUtils.getContextForPackage(currentPackageContext, nasuxPluginAppPackageName);
                // If the package context for the plugin app is not null, then assume its installed and get its info
                if (nasuxPluginAppContext != null) {
                    if (i != 0)
                        markdownString.append("\n\n");
                    markdownString.append(getAppInfoMarkdownString(nasuxPluginAppContext, false));
                }
            }
        }

        if (markdownString.toString().isEmpty())
            return null;

        return markdownString.toString();
    }

    /**
     * Get a markdown {@link String} for the app info. If the {@code context} passed is different
     * from the {@link NasUXConstants#NASUX_PACKAGE_NAME} package context, then this function
     * must have been called by a different package like a plugin, so we return info for both packages
     * if {@code returnNasUXPackageInfoToo} is {@code true}.
     *
     * @param currentPackageContext The context of current package.
     * @param returnNasUXPackageInfoToo If set to {@code true}, then will return info of the
     * {@link NasUXConstants#NASUX_PACKAGE_NAME} package as well if its different from current package.
     * @return Returns the markdown {@link String}.
     */
    public static String getAppInfoMarkdownString(final Context currentPackageContext, final boolean returnNasUXPackageInfoToo) {
        if (currentPackageContext == null) return "null";

        StringBuilder markdownString = new StringBuilder();

        Context nasuxPackageContext = getNasUXPackageContext(currentPackageContext);

        String nasuxPackageName = null;
        String nasuxAppName = null;
        if (nasuxPackageContext != null) {
            nasuxPackageName = PackageUtils.getPackageNameForPackage(nasuxPackageContext);
            nasuxAppName = PackageUtils.getAppNameForPackage(nasuxPackageContext);
        }

        String currentPackageName = PackageUtils.getPackageNameForPackage(currentPackageContext);
        String currentAppName = PackageUtils.getAppNameForPackage(currentPackageContext);

        boolean isNasUXPackage = (nasuxPackageName != null && nasuxPackageName.equals(currentPackageName));


        if (returnNasUXPackageInfoToo && !isNasUXPackage)
            markdownString.append("## ").append(currentAppName).append(" App Info (Current)\n");
        else
            markdownString.append("## ").append(currentAppName).append(" App Info\n");
        markdownString.append(getAppInfoMarkdownStringInner(currentPackageContext));
        markdownString.append("\n##\n");

        if (returnNasUXPackageInfoToo && nasuxPackageContext != null && !isNasUXPackage) {
            markdownString.append("\n\n## ").append(nasuxAppName).append(" App Info\n");
            markdownString.append(getAppInfoMarkdownStringInner(nasuxPackageContext));
            markdownString.append("\n##\n");
        }


        return markdownString.toString();
    }

    /**
     * Get a markdown {@link String} for the app info for the package associated with the {@code context}.
     *
     * @param context The context for operations for the package.
     * @return Returns the markdown {@link String}.
     */
    public static String getAppInfoMarkdownStringInner(@NonNull final Context context) {
        StringBuilder markdownString = new StringBuilder();

        markdownString.append((AndroidUtils.getAppInfoMarkdownString(context)));

        if (context.getPackageName().equals(NasUXConstants.NASUX_PACKAGE_NAME)) {
            AndroidUtils.appendPropertyToMarkdown(markdownString, "NASUX_APP_PACKAGE_MANAGER", NasUXBootstrap.NASUX_APP_PACKAGE_MANAGER);
            AndroidUtils.appendPropertyToMarkdown(markdownString, "NASUX_APP_PACKAGE_VARIANT", NasUXBootstrap.NASUX_APP_PACKAGE_VARIANT);
        }

        Error error;
        error = NasUXFileUtils.isNasUXFilesDirectoryAccessible(context, true, true);
        if (error != null) {
            AndroidUtils.appendPropertyToMarkdown(markdownString, "NASUX_FILES_DIR", NasUXConstants.NASUX_FILES_DIR_PATH);
            AndroidUtils.appendPropertyToMarkdown(markdownString, "IS_NASUX_FILES_DIR_ACCESSIBLE", "false - " + Error.getMinimalErrorString(error));
        }

        String signingCertificateSHA256Digest = PackageUtils.getSigningCertificateSHA256DigestForPackage(context);
        if (signingCertificateSHA256Digest != null) {
            AndroidUtils.appendPropertyToMarkdown(markdownString,"APK_RELEASE", getAPKRelease(signingCertificateSHA256Digest));
            AndroidUtils.appendPropertyToMarkdown(markdownString,"SIGNING_CERTIFICATE_SHA256_DIGEST", signingCertificateSHA256Digest);
        }

        return markdownString.toString();
    }

    /**
     * Get a markdown {@link String} for reporting an issue.
     *
     * @param context The context for operations.
     * @return Returns the markdown {@link String}.
     */
    public static String getReportIssueMarkdownString(@NonNull final Context context) {
        if (context == null) return "null";

        StringBuilder markdownString = new StringBuilder();

        markdownString.append("## Where To Report An Issue");

        markdownString.append("\n\n").append(context.getString(R.string.msg_report_issue, NasUXConstants.NASUX_WIKI_URL)).append("\n");

        markdownString.append("\n\n### Email\n");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(NasUXConstants.NASUX_SUPPORT_EMAIL_URL, NasUXConstants.NASUX_SUPPORT_EMAIL_MAILTO_URL)).append("  ");

        markdownString.append("\n\n### Reddit\n");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(NasUXConstants.NASUX_REDDIT_SUBREDDIT, NasUXConstants.NASUX_REDDIT_SUBREDDIT_URL)).append("  ");

        markdownString.append("\n\n### GitHub Issues for NasUX apps\n");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(NasUXConstants.NASUX_APP_NAME, NasUXConstants.NASUX_GITHUB_ISSUES_REPO_URL)).append("  ");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(NasUXConstants.NASUX_API_APP_NAME, NasUXConstants.NASUX_API_GITHUB_ISSUES_REPO_URL)).append("  ");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(NasUXConstants.NASUX_BOOT_APP_NAME, NasUXConstants.NASUX_BOOT_GITHUB_ISSUES_REPO_URL)).append("  ");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(NasUXConstants.NASUX_FLOAT_APP_NAME, NasUXConstants.NASUX_FLOAT_GITHUB_ISSUES_REPO_URL)).append("  ");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(NasUXConstants.NASUX_STYLING_APP_NAME, NasUXConstants.NASUX_STYLING_GITHUB_ISSUES_REPO_URL)).append("  ");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(NasUXConstants.NASUX_TASKER_APP_NAME, NasUXConstants.NASUX_TASKER_GITHUB_ISSUES_REPO_URL)).append("  ");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(NasUXConstants.NASUX_WIDGET_APP_NAME, NasUXConstants.NASUX_WIDGET_GITHUB_ISSUES_REPO_URL)).append("  ");

        markdownString.append("\n\n### GitHub Issues for NasUX packages\n");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(NasUXConstants.NASUX_PACKAGES_GITHUB_REPO_NAME, NasUXConstants.NASUX_PACKAGES_GITHUB_ISSUES_REPO_URL)).append("  ");

        markdownString.append("\n##\n");

        return markdownString.toString();
    }

    /**
     * Get a markdown {@link String} for important links.
     *
     * @param context The context for operations.
     * @return Returns the markdown {@link String}.
     */
    public static String getImportantLinksMarkdownString(@NonNull final Context context) {
        if (context == null) return "null";

        StringBuilder markdownString = new StringBuilder();

        markdownString.append("## Important Links");

        markdownString.append("\n\n### GitHub\n");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(NasUXConstants.NASUX_APP_NAME, NasUXConstants.NASUX_GITHUB_REPO_URL)).append("  ");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(NasUXConstants.NASUX_API_APP_NAME, NasUXConstants.NASUX_API_GITHUB_REPO_URL)).append("  ");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(NasUXConstants.NASUX_BOOT_APP_NAME, NasUXConstants.NASUX_BOOT_GITHUB_REPO_URL)).append("  ");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(NasUXConstants.NASUX_FLOAT_APP_NAME, NasUXConstants.NASUX_FLOAT_GITHUB_REPO_URL)).append("  ");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(NasUXConstants.NASUX_STYLING_APP_NAME, NasUXConstants.NASUX_STYLING_GITHUB_REPO_URL)).append("  ");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(NasUXConstants.NASUX_TASKER_APP_NAME, NasUXConstants.NASUX_TASKER_GITHUB_REPO_URL)).append("  ");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(NasUXConstants.NASUX_WIDGET_APP_NAME, NasUXConstants.NASUX_WIDGET_GITHUB_REPO_URL)).append("  ");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(NasUXConstants.NASUX_PACKAGES_GITHUB_REPO_NAME, NasUXConstants.NASUX_PACKAGES_GITHUB_REPO_URL)).append("  ");

        markdownString.append("\n\n### Email\n");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(NasUXConstants.NASUX_SUPPORT_EMAIL_URL, NasUXConstants.NASUX_SUPPORT_EMAIL_MAILTO_URL)).append("  ");

        markdownString.append("\n\n### Reddit\n");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(NasUXConstants.NASUX_REDDIT_SUBREDDIT, NasUXConstants.NASUX_REDDIT_SUBREDDIT_URL)).append("  ");

        markdownString.append("\n\n### Wiki\n");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(NasUXConstants.NASUX_WIKI, NasUXConstants.NASUX_WIKI_URL)).append("  ");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(NasUXConstants.NASUX_APP_NAME, NasUXConstants.NASUX_GITHUB_WIKI_REPO_URL)).append("  ");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(NasUXConstants.NASUX_PACKAGES_GITHUB_REPO_NAME, NasUXConstants.NASUX_PACKAGES_GITHUB_WIKI_REPO_URL)).append("  ");

        markdownString.append("\n##\n");

        return markdownString.toString();
    }



    /**
     * Get a markdown {@link String} for APT info of the app.
     *
     * This will take a few seconds to run due to running {@code apt update} command.
     *
     * @param context The context for operations.
     * @return Returns the markdown {@link String}.
     */
    public static String geAPTInfoMarkdownString(@NonNull final Context context) {

        String aptInfoScript;
        InputStream inputStream = context.getResources().openRawResource(com.nastech.nasux.shared.R.raw.apt_info_script);
        try {
            aptInfoScript = IOUtils.toString(inputStream, Charset.defaultCharset());
        } catch (IOException e) {
            Logger.logError(LOG_TAG, "Failed to get APT info script: " + e.getMessage());
            return null;
        }

        IOUtils.closeQuietly(inputStream);

        if (aptInfoScript == null || aptInfoScript.isEmpty()) {
            Logger.logError(LOG_TAG, "The APT info script is null or empty");
            return null;
        }

        aptInfoScript = aptInfoScript.replaceAll(Pattern.quote("@NASUX_PREFIX@"), NasUXConstants.NASUX_PREFIX_DIR_PATH);

        ExecutionCommand executionCommand = new ExecutionCommand(-1,
            NasUXConstants.NASUX_BIN_PREFIX_DIR_PATH + "/bash", null, aptInfoScript,
            null, ExecutionCommand.Runner.APP_SHELL.getName(), false);
        executionCommand.commandLabel = "APT Info Command";
        executionCommand.backgroundCustomLogLevel = Logger.LOG_LEVEL_OFF;
        AppShell appShell = AppShell.execute(context, executionCommand, null, new NasUXShellEnvironment(), null, true);
        if (appShell == null || !executionCommand.isSuccessful() || executionCommand.resultData.exitCode != 0) {
            Logger.logErrorExtended(LOG_TAG, executionCommand.toString());
            return null;
        }

        if (!executionCommand.resultData.stderr.toString().isEmpty())
            Logger.logErrorExtended(LOG_TAG, executionCommand.toString());

        StringBuilder markdownString = new StringBuilder();

        markdownString.append("## ").append(NasUXConstants.NASUX_APP_NAME).append(" APT Info\n\n");
        markdownString.append(executionCommand.resultData.stdout.toString());
        markdownString.append("\n##\n");

        return markdownString.toString();
    }

    /**
     * Get a markdown {@link String} for info for nasux debugging.
     *
     * @param context The context for operations.
     * @return Returns the markdown {@link String}.
     */
    public static String getNasUXDebugMarkdownString(@NonNull final Context context) {
        String statInfo = NasUXFileUtils.getNasUXFilesStatMarkdownString(context);
        String logcatInfo = getLogcatDumpMarkdownString(context);

        if (statInfo != null && logcatInfo != null)
            return statInfo + "\n\n" + logcatInfo;
        else if (statInfo != null)
            return statInfo;
        else
            return logcatInfo;

    }

    /**
     * Get a markdown {@link String} for logcat command dump.
     *
     * @param context The context for operations.
     * @return Returns the markdown {@link String}.
     */
    public static String getLogcatDumpMarkdownString(@NonNull final Context context) {
        // Build script
        // We need to prevent OutOfMemoryError since StreamGobbler StringBuilder + StringBuilder.toString()
        // may require lot of memory if dump is too large.
        // Putting a limit at 3000 lines. Assuming average 160 chars/line will result in 500KB usage
        // per object.
        // That many lines should be enough for debugging for recent issues anyways assuming nasux
        // has not been granted READ_LOGS permission s.
        String logcatScript = "/system/bin/logcat -d -t 3000 2>&1";

        // Run script
        // Logging must be disabled for output of logcat command itself in StreamGobbler
        ExecutionCommand executionCommand = new ExecutionCommand(-1, "/system/bin/sh",
            null, logcatScript + "\n", "/", ExecutionCommand.Runner.APP_SHELL.getName(), true);
        executionCommand.commandLabel = "Logcat dump command";
        executionCommand.backgroundCustomLogLevel = Logger.LOG_LEVEL_OFF;
        AppShell appShell = AppShell.execute(context, executionCommand, null, new NasUXShellEnvironment(), null, true);
        if (appShell == null || !executionCommand.isSuccessful()) {
            Logger.logErrorExtended(LOG_TAG, executionCommand.toString());
            return null;
        }

        // Build script output
        StringBuilder logcatOutput = new StringBuilder();
        logcatOutput.append("$ ").append(logcatScript);
        logcatOutput.append("\n").append(executionCommand.resultData.stdout.toString());

        boolean stderrSet = !executionCommand.resultData.stderr.toString().isEmpty();
        if (executionCommand.resultData.exitCode != 0 || stderrSet) {
            Logger.logErrorExtended(LOG_TAG, executionCommand.toString());
            if (stderrSet)
                logcatOutput.append("\n").append(executionCommand.resultData.stderr.toString());
            logcatOutput.append("\n").append("exit code: ").append(executionCommand.resultData.exitCode.toString());
        }

        // Build markdown output
        StringBuilder markdownString = new StringBuilder();
        markdownString.append("## Logcat Dump\n\n");
        markdownString.append("\n\n").append(MarkdownUtils.getMarkdownCodeForString(logcatOutput.toString(), true));
        markdownString.append("\n##\n");

        return markdownString.toString();
    }



    public static String getAPKRelease(String signingCertificateSHA256Digest) {
        if (signingCertificateSHA256Digest == null) return "null";

        switch (signingCertificateSHA256Digest.toUpperCase()) {
            case NasUXConstants.APK_RELEASE_FDROID_SIGNING_CERTIFICATE_SHA256_DIGEST:
                return NasUXConstants.APK_RELEASE_FDROID;
            case NasUXConstants.APK_RELEASE_GITHUB_SIGNING_CERTIFICATE_SHA256_DIGEST:
                return NasUXConstants.APK_RELEASE_GITHUB;
            case NasUXConstants.APK_RELEASE_GOOGLE_PLAYSTORE_SIGNING_CERTIFICATE_SHA256_DIGEST:
                return NasUXConstants.APK_RELEASE_GOOGLE_PLAYSTORE;
            case NasUXConstants.APK_RELEASE_NASUX_DEVS_SIGNING_CERTIFICATE_SHA256_DIGEST:
                return NasUXConstants.APK_RELEASE_NASUX_DEVS;
            default:
                return "Unknown";
        }
    }


    /**
     * Get a process id of the main app process of the {@link NasUXConstants#NASUX_PACKAGE_NAME}
     * package.
     *
     * @param context The context for operations.
     * @return Returns the process if found and running, otherwise {@code null}.
     */
    public static String getNasUXAppPID(final Context context) {
        return PackageUtils.getPackagePID(context, NasUXConstants.NASUX_PACKAGE_NAME);
    }

}
