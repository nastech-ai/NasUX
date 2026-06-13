package com.nastech.nasux.shared.nasux;

import android.annotation.SuppressLint;
import android.content.Intent;

import com.nastech.nasux.shared.shell.command.ExecutionCommand;
import com.nastech.nasux.shared.shell.command.ExecutionCommand.Runner;

import java.io.File;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;

/*
 * Version: v0.53.0
 * SPDX-License-Identifier: MIT
 *
 * Changelog
 *
 * - 0.1.0 (2021-03-08)
 *      - Initial Release.
 *
 * - 0.2.0 (2021-03-11)
 *      - Added `_DIR` and `_FILE` substrings to paths.
 *      - Added `INTERNAL_PRIVATE_APP_DATA_DIR*`, `NASUX_CACHE_DIR*`, `NASUX_DATABASES_DIR*`,
 *          `NASUX_SHARED_PREFERENCES_DIR*`, `NASUX_BIN_PREFIX_DIR*`, `NASUX_ETC_DIR*`,
 *          `NASUX_INCLUDE_DIR*`, `NASUX_LIB_DIR*`, `NASUX_LIBEXEC_DIR*`, `NASUX_SHARE_DIR*`,
 *          `NASUX_TMP_DIR*`, `NASUX_VAR_DIR*`, `NASUX_STAGING_PREFIX_DIR*`,
 *          `NASUX_STORAGE_HOME_DIR*`, `NASUX_DEFAULT_PREFERENCES_FILE_BASENAME*`,
 *          `NASUX_DEFAULT_PREFERENCES_FILE`.
 *      - Renamed `DATA_HOME_PATH` to `NASUX_DATA_HOME_DIR_PATH`.
 *      - Renamed `CONFIG_HOME_PATH` to `NASUX_CONFIG_HOME_DIR_PATH`.
 *      - Updated javadocs and spacing.
 *
 * - 0.3.0 (2021-03-12)
 *      - Remove `NASUX_CACHE_DIR_PATH*`, `NASUX_DATABASES_DIR_PATH*`,
 *          `NASUX_SHARED_PREFERENCES_DIR_PATH*` since they may not be consistent on all devices.
 *      - Renamed `NASUX_DEFAULT_PREFERENCES_FILE_BASENAME` to
 *          `NASUX_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION`. This should be used for
 *           accessing shared preferences between NasUX app and its plugins if ever needed by first
 *           getting shared package context with {@link Context.createPackageContext(String,int}).
 *
 * - 0.4.0 (2021-03-16)
 *      - Added `BROADCAST_NASUX_OPENED`,
 *          `NASUX_API_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION`
 *          `NASUX_BOOT_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION`,
 *          `NASUX_FLOAT_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION`,
 *          `NASUX_STYLING_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION`,
 *          `NASUX_TASKER_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION`,
 *          `NASUX_WIDGET_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION`.
 *
 * - 0.5.0 (2021-03-16)
 *      - Renamed "NasUX Plugin app" labels to "NasUX:Tasker app".
 *
 * - 0.6.0 (2021-03-16)
 *      - Added `NASUX_FILE_SHARE_URI_AUTHORITY`.
 *
 * - 0.7.0 (2021-03-17)
 *      - Fixed javadocs.
 *
 * - 0.8.0 (2021-03-18)
 *      - Fixed Intent extra types javadocs.
 *      - Added following to `NASUX_SERVICE`:
 *          `EXTRA_PENDING_INTENT`, `EXTRA_RESULT_BUNDLE`,
 *          `EXTRA_STDOUT`, `EXTRA_STDERR`, `EXTRA_EXIT_CODE`,
 *          `EXTRA_ERR`, `EXTRA_ERRMSG`.
 *
 * - 0.9.0 (2021-03-18)
 *      - Fixed javadocs.
 *
 * - 0.10.0 (2021-03-19)
 *      - Added following to `NASUX_SERVICE`:
 *          `EXTRA_SESSION_ACTION`,
 *          `VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_OPEN_ACTIVITY`,
 *          `VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_OPEN_ACTIVITY`,
 *          `VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_DONT_OPEN_ACTIVITY`
 *          `VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_DONT_OPEN_ACTIVITY`.
 *      - Added following to `RUN_COMMAND_SERVICE`:
 *          `EXTRA_SESSION_ACTION`.
 *
 * - 0.11.0 (2021-03-24)
 *      - Added following to `NASUX_SERVICE`:
 *          `EXTRA_COMMAND_LABEL`, `EXTRA_COMMAND_DESCRIPTION`, `EXTRA_COMMAND_HELP`, `EXTRA_PLUGIN_API_HELP`.
 *      - Added following to `RUN_COMMAND_SERVICE`:
 *          `EXTRA_COMMAND_LABEL`, `EXTRA_COMMAND_DESCRIPTION`, `EXTRA_COMMAND_HELP`.
 *      - Updated `RESULT_BUNDLE` related extras with `PLUGIN_RESULT_BUNDLE` prefixes.
 *
 * - 0.12.0 (2021-03-25)
 *      - Added following to `NASUX_SERVICE`:
 *          `EXTRA_PLUGIN_RESULT_BUNDLE_STDOUT_ORIGINAL_LENGTH`,
 *          `EXTRA_PLUGIN_RESULT_BUNDLE_STDERR_ORIGINAL_LENGTH`.
 *
 * - 0.13.0 (2021-03-25)
 *      - Added following to `RUN_COMMAND_SERVICE`:
 *          `EXTRA_PENDING_INTENT`.
 *
 * - 0.14.0 (2021-03-25)
 *      - Added `FDROID_PACKAGES_BASE_URL`,
 *          `NASUX_GITHUB_ORGANIZATION_NAME`, `NASUX_GITHUB_ORGANIZATION_URL`,
 *          `NASUX_GITHUB_REPO_NAME`, `NASUX_GITHUB_REPO_URL`, `NASUX_FDROID_PACKAGE_URL`,
 *          `NASUX_API_GITHUB_REPO_NAME`,`NASUX_API_GITHUB_REPO_URL`, `NASUX_API_FDROID_PACKAGE_URL`,
 *          `NASUX_BOOT_GITHUB_REPO_NAME`, `NASUX_BOOT_GITHUB_REPO_URL`, `NASUX_BOOT_FDROID_PACKAGE_URL`,
 *          `NASUX_FLOAT_GITHUB_REPO_NAME`, `NASUX_FLOAT_GITHUB_REPO_URL`, `NASUX_FLOAT_FDROID_PACKAGE_URL`,
 *          `NASUX_STYLING_GITHUB_REPO_NAME`, `NASUX_STYLING_GITHUB_REPO_URL`, `NASUX_STYLING_FDROID_PACKAGE_URL`,
 *          `NASUX_TASKER_GITHUB_REPO_NAME`, `NASUX_TASKER_GITHUB_REPO_URL`, `NASUX_TASKER_FDROID_PACKAGE_URL`,
 *          `NASUX_WIDGET_GITHUB_REPO_NAME`, `NASUX_WIDGET_GITHUB_REPO_URL` `NASUX_WIDGET_FDROID_PACKAGE_URL`.
 *
 * - 0.15.0 (2021-04-06)
 *      - Fixed some variables that had `PREFIX_` substring missing in their name.
 *      - Added `NASUX_CRASH_LOG_FILE_PATH`, `NASUX_CRASH_LOG_BACKUP_FILE_PATH`,
 *          `NASUX_GITHUB_ISSUES_REPO_URL`, `NASUX_API_GITHUB_ISSUES_REPO_URL`,
 *          `NASUX_BOOT_GITHUB_ISSUES_REPO_URL`, `NASUX_FLOAT_GITHUB_ISSUES_REPO_URL`,
 *          `NASUX_STYLING_GITHUB_ISSUES_REPO_URL`, `NASUX_TASKER_GITHUB_ISSUES_REPO_URL`,
 *          `NASUX_WIDGET_GITHUB_ISSUES_REPO_URL`,
 *          `NASUX_GITHUB_WIKI_REPO_URL`, `NASUX_PACKAGES_GITHUB_WIKI_REPO_URL`,
 *          `NASUX_PACKAGES_GITHUB_REPO_NAME`, `NASUX_PACKAGES_GITHUB_REPO_URL`, `NASUX_PACKAGES_GITHUB_ISSUES_REPO_URL`,
 *          `NASUX_GAME_PACKAGES_GITHUB_REPO_NAME`, `NASUX_GAME_PACKAGES_GITHUB_REPO_URL`, `NASUX_GAME_PACKAGES_GITHUB_ISSUES_REPO_URL`,
 *          `NASUX_SCIENCE_PACKAGES_GITHUB_REPO_NAME`, `NASUX_SCIENCE_PACKAGES_GITHUB_REPO_URL`, `NASUX_SCIENCE_PACKAGES_GITHUB_ISSUES_REPO_URL`,
 *          `NASUX_ROOT_PACKAGES_GITHUB_REPO_NAME`, `NASUX_ROOT_PACKAGES_GITHUB_REPO_URL`, `NASUX_ROOT_PACKAGES_GITHUB_ISSUES_REPO_URL`,
 *          `NASUX_UNSTABLE_PACKAGES_GITHUB_REPO_NAME`, `NASUX_UNSTABLE_PACKAGES_GITHUB_REPO_URL`, `NASUX_UNSTABLE_PACKAGES_GITHUB_ISSUES_REPO_URL`,
 *          `NASUX_X11_PACKAGES_GITHUB_REPO_NAME`, `NASUX_X11_PACKAGES_GITHUB_REPO_URL`, `NASUX_X11_PACKAGES_GITHUB_ISSUES_REPO_URL`.
 *      - Added following to `RUN_COMMAND_SERVICE`:
 *          `RUN_COMMAND_API_HELP_URL`.
 *
 * - 0.16.0 (2021-04-06)
 *      - Added `NASUX_SUPPORT_EMAIL`, `NASUX_SUPPORT_EMAIL_URL`, `NASUX_SUPPORT_EMAIL_MAILTO_URL`,
 *          `NASUX_REDDIT_SUBREDDIT`, `NASUX_REDDIT_SUBREDDIT_URL`.
 *      - The `NASUX_SUPPORT_EMAIL_URL` value must be fixed later when email has been set up.
 *
 * - 0.17.0 (2021-04-07)
 *      - Added `NASUX_APP_NOTIFICATION_CHANNEL_ID`, `NASUX_APP_NOTIFICATION_CHANNEL_NAME`, `NASUX_APP_NOTIFICATION_ID`,
 *          `NASUX_RUN_COMMAND_NOTIFICATION_CHANNEL_ID`, `NASUX_RUN_COMMAND_NOTIFICATION_CHANNEL_NAME`, `NASUX_RUN_COMMAND_NOTIFICATION_ID`,
 *          `NASUX_PLUGIN_COMMAND_ERRORS_NOTIFICATION_CHANNEL_ID`, `NASUX_PLUGIN_COMMAND_ERRORS_NOTIFICATION_CHANNEL_NAME`,
 *          `NASUX_CRASH_REPORTS_NOTIFICATION_CHANNEL_ID`, `NASUX_CRASH_REPORTS_NOTIFICATION_CHANNEL_NAME`.
 *      - Updated javadocs.
 *
 * - 0.18.0 (2021-04-11)
 *      - Updated `NASUX_SUPPORT_EMAIL_URL` to a valid email.
 *      - Removed `NASUX_SUPPORT_EMAIL`.
 *
 * - 0.19.0 (2021-04-12)
 *      - Added `NASUX_ACTIVITY.ACTION_REQUEST_PERMISSIONS`.
 *      - Added `NASUX_SERVICE.EXTRA_STDIN`.
 *      - Added `RUN_COMMAND_SERVICE.EXTRA_STDIN`.
 *      - Deprecated `NASUX_ACTIVITY.EXTRA_RELOAD_STYLE`.
 *
 * - 0.20.0 (2021-05-13)
 *      - Added `NASUX_WIKI`, `NASUX_WIKI_URL`, `NASUX_PLUGIN_APP_NAMES_LIST`, `NASUX_PLUGIN_APP_PACKAGE_NAMES_LIST`.
 *      - Added `NASUX_SETTINGS_ACTIVITY_NAME`.
 *
 * - 0.21.0 (2021-05-13)
 *      - Added `APK_RELEASE_FDROID`, `APK_RELEASE_FDROID_SIGNING_CERTIFICATE_SHA256_DIGEST`,
 *          `APK_RELEASE_GITHUB_DEBUG_BUILD`, `APK_RELEASE_GITHUB_DEBUG_BUILD_SIGNING_CERTIFICATE_SHA256_DIGEST`,
 *          `APK_RELEASE_GOOGLE_PLAYSTORE`, `APK_RELEASE_GOOGLE_PLAYSTORE_SIGNING_CERTIFICATE_SHA256_DIGEST`.
 *
 * - 0.22.0 (2021-05-13)
 *      - Added `NASUX_DONATE_URL`.
 *
 * - 0.23.0 (2021-06-12)
 *      - Rename `INTERNAL_PRIVATE_APP_DATA_DIR_PATH` to `NASUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH`.
 *
 * - 0.24.0 (2021-06-27)
 *      - Add `COMMA_NORMAL`, `COMMA_ALTERNATIVE`.
 *      - Added following to `NASUX_APP.NASUX_SERVICE`:
 *          `EXTRA_RESULT_DIRECTORY`, `EXTRA_RESULT_SINGLE_FILE`, `EXTRA_RESULT_FILE_BASENAME`,
 *          `EXTRA_RESULT_FILE_OUTPUT_FORMAT`, `EXTRA_RESULT_FILE_ERROR_FORMAT`, `EXTRA_RESULT_FILES_SUFFIX`.
 *      - Added following to `NASUX_APP.RUN_COMMAND_SERVICE`:
 *          `EXTRA_RESULT_DIRECTORY`, `EXTRA_RESULT_SINGLE_FILE`, `EXTRA_RESULT_FILE_BASENAME`,
 *          `EXTRA_RESULT_FILE_OUTPUT_FORMAT`, `EXTRA_RESULT_FILE_ERROR_FORMAT`, `EXTRA_RESULT_FILES_SUFFIX`,
 *          `EXTRA_REPLACE_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS`, `EXTRA_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS`.
 *      - Added following to `RESULT_SENDER`:
 *           `FORMAT_SUCCESS_STDOUT`, `FORMAT_SUCCESS_STDOUT__EXIT_CODE`, `FORMAT_SUCCESS_STDOUT__STDERR__EXIT_CODE`
 *           `FORMAT_FAILED_ERR__ERRMSG__STDOUT__STDERR__EXIT_CODE`,
 *           `RESULT_FILE_ERR_PREFIX`, `RESULT_FILE_ERRMSG_PREFIX` `RESULT_FILE_STDOUT_PREFIX`,
 *           `RESULT_FILE_STDERR_PREFIX`, `RESULT_FILE_EXIT_CODE_PREFIX`.
 *
 * - 0.25.0 (2021-08-19)
 *      - Added following to `NASUX_APP.NASUX_SERVICE`:
 *          `EXTRA_BACKGROUND_CUSTOM_LOG_LEVEL`.
 *      - Added following to `NASUX_APP.RUN_COMMAND_SERVICE`:
 *          `EXTRA_BACKGROUND_CUSTOM_LOG_LEVEL`.
 *
 * - 0.26.0 (2021-08-25)
 *      - Changed `NASUX_ACTIVITY.ACTION_FAILSAFE_SESSION` to `NASUX_ACTIVITY.EXTRA_FAILSAFE_SESSION`.
 *
 * - 0.27.0 (2021-09-02)
 *      - Added `NASUX_FLOAT_APP_NOTIFICATION_CHANNEL_ID`, `NASUX_FLOAT_APP_NOTIFICATION_CHANNEL_NAME`,
 *          `NASUX_FLOAT_APP.NASUX_FLOAT_SERVICE_NAME`.
 *      - Added following to `NASUX_FLOAT_APP.NASUX_FLOAT_SERVICE`:
 *          `ACTION_STOP_SERVICE`, `ACTION_SHOW`, `ACTION_HIDE`.
 *
 * - 0.28.0 (2021-09-02)
 *      - Added `NASUX_FLOAT_PROPERTIES_PRIMARY_FILE*` and `NASUX_FLOAT_PROPERTIES_SECONDARY_FILE*`.
 *
 * - 0.29.0 (2021-09-04)
 *      - Added `NASUX_SHORTCUT_TASKS_SCRIPTS_DIR_BASENAME`, `NASUX_SHORTCUT_SCRIPT_ICONS_DIR_BASENAME`,
 *          `NASUX_SHORTCUT_SCRIPT_ICONS_DIR_PATH`, `NASUX_SHORTCUT_SCRIPT_ICONS_DIR`.
 *      - Added following to `NASUX_WIDGET.NASUX_WIDGET_PROVIDER`:
 *          `ACTION_WIDGET_ITEM_CLICKED`, `ACTION_REFRESH_WIDGET`, `EXTRA_FILE_CLICKED`.
 *      - Changed naming convention of `NASUX_FLOAT_APP.NASUX_FLOAT_SERVICE.ACTION_*`.
 *      - Fixed wrong path set for `NASUX_SHORTCUT_SCRIPTS_DIR_PATH`.
 *
 * - 0.30.0 (2021-09-08)
 *      - Changed `APK_RELEASE_GITHUB_DEBUG_BUILD`to `APK_RELEASE_GITHUB` and
 *          `APK_RELEASE_GITHUB_DEBUG_BUILD_SIGNING_CERTIFICATE_SHA256_DIGEST` to
 *          `APK_RELEASE_GITHUB_SIGNING_CERTIFICATE_SHA256_DIGEST`.
 *
 * - 0.31.0 (2021-09-09)
 *      - Added following to `NASUX_APP.NASUX_SERVICE`:
 *          `MIN_VALUE_EXTRA_SESSION_ACTION` and `MAX_VALUE_EXTRA_SESSION_ACTION`.
 *
 * - 0.32.0 (2021-09-23)
 *      - Added `NASUX_API.NASUX_API_ACTIVITY_NAME`, `NASUX_TASKER.NASUX_TASKER_ACTIVITY_NAME`
 *          and `NASUX_WIDGET.NASUX_WIDGET_ACTIVITY_NAME`.
 *
 * - 0.33.0 (2021-10-08)
 *      - Added `NASUX_PROPERTIES_FILE_PATHS_LIST` and `NASUX_FLOAT_PROPERTIES_FILE_PATHS_LIST`.
 *
 * - 0.34.0 (2021-10-26)
 *      - Move `RESULT_SENDER` to `com.nastech.nasux.shared.shell.command.ShellCommandConstants`.
 *
 * - 0.35.0 (2022-01-28)
 *      - Add `NASUX_APP.NASUX_ACTIVITY.EXTRA_RECREATE_ACTIVITY`.
 *
 * - 0.36.0 (2022-03-10)
 *      - Added `NASUX_APP.NASUX_SERVICE.EXTRA_RUNNER` and `NASUX_APP.RUN_COMMAND_SERVICE.EXTRA_RUNNER`
 *
 * - 0.37.0 (2022-03-15)
 *  - Added `NASUX_API_APT_*`.
 *
 * - 0.38.0 (2022-03-16)
 *      - Added `NASUX_APP.NASUX_ACTIVITY.ACTION_NOTIFY_APP_CRASH`.
 *
 * - 0.39.0 (2022-03-18)
 *      - Added `NASUX_APP.NASUX_SERVICE.EXTRA_SESSION_NAME`, `NASUX_APP.RUN_COMMAND_SERVICE.EXTRA_SESSION_NAME`,
 *          `NASUX_APP.NASUX_SERVICE.EXTRA_SESSION_CREATE_MODE` and `NASUX_APP.RUN_COMMAND_SERVICE.EXTRA_SESSION_CREATE_MODE`.
 *
 * - 0.40.0 (2022-04-17)
 *      - Added `NASUX_APPS_DIR_PATH` and `NASUX_APP.APPS_DIR_PATH`.
 *
 * - 0.41.0 (2022-04-17)
 *      - Added `NASUX_APP.NASUX_AM_SOCKET_FILE_PATH`.
 *
 * - 0.42.0 (2022-04-29)
 *      - Added `APK_RELEASE_NASUX_DEVS` and `APK_RELEASE_NASUX_DEVS_SIGNING_CERTIFICATE_SHA256_DIGEST`.
 *
 * - 0.43.0 (2022-05-29)
 *      - Changed `NASUX_SUPPORT_EMAIL_URL` to support@nasux.dev.
 *
 * - 0.44.0 (2022-05-29)
 *      - Changed `NASUX_APP.APPS_DIR_PATH` basename from `nasux-app` to `com.nastech.nasux`.
 *
 * - 0.45.0 (2022-06-01)
 *      - Added `NASUX_APP.BUILD_CONFIG_CLASS_NAME`.
 *
 * - 0.46.0 (2022-06-03)
 *      - Rename `NASUX_APP.NASUX_SERVICE.EXTRA_SESSION_NAME` to `*.EXTRA_SHELL_NAME`,
 *          `NASUX_APP.RUN_COMMAND_SERVICE.EXTRA_SESSION_NAME` to `*.EXTRA_SHELL_NAME`,
 *          `NASUX_APP.NASUX_SERVICE.EXTRA_SESSION_CREATE_MODE` to `*.EXTRA_SHELL_CREATE_MODE` and
 *          `NASUX_APP.RUN_COMMAND_SERVICE.EXTRA_SESSION_CREATE_MODE` to `*.EXTRA_SHELL_CREATE_MODE`.
 *
 * - 0.47.0 (2022-06-04)
 *      - Added `NASUX_SITE` and `NASUX_SITE_URL`.
 *      - Changed `NASUX_DONATE_URL`.
 *
 * - 0.48.0 (2022-06-04)
 *      - Removed `NASUX_GAME_PACKAGES_GITHUB_*`, `NASUX_SCIENCE_PACKAGES_GITHUB_*`,
 *          `NASUX_ROOT_PACKAGES_GITHUB_*`, `NASUX_UNSTABLE_PACKAGES_GITHUB_*`
 *
 * - 0.49.0 (2022-06-11)
 *      - Added `NASUX_ENV_PREFIX_ROOT`.
 *
 * - 0.50.0 (2022-06-11)
 *      - Added `NASUX_CONFIG_PREFIX_DIR_PATH`, `NASUX_ENV_FILE_PATH` and `NASUX_ENV_TEMP_FILE_PATH`.
 *
 * - 0.51.0 (2022-06-13)
 *      - Added `NASUX_APP.FILE_SHARE_RECEIVER_ACTIVITY_CLASS_NAME` and `NASUX_APP.FILE_VIEW_RECEIVER_ACTIVITY_CLASS_NAME`.
 *
 * - 0.52.0 (2022-06-18)
 *      - Added `NASUX_PREFIX_DIR_IGNORED_SUB_FILES_PATHS_TO_CONSIDER_AS_EMPTY`.
 *
 * - 0.53.0 (2025-01-12)
 *      - Renamed `NASUX_API`, `NASUX_STYLING`, `NASUX_TASKER`, `NASUX_WIDGET` classes with `_APP` suffix added.
 *      - Added `NASUX_*_MAIN_ACTIVITY_NAME` and `NASUX_*_LAUNCHER_ACTIVITY_NAME` constants to each app class.
 */

/**
 * A class that defines shared constants of the NasUX app and its plugins.
 * This class will be hosted by nasux-shared lib and should be imported by other nasux plugin
 * apps as is instead of copying constants to random classes. The 3rd party apps can also import
 * it for interacting with nasux apps. If changes are made to this file, increment the version number
 * and add an entry in the Changelog section above.
 *
 * NasUX app default package name is "com.nastech.nasux" and is used in {@link #NASUX_PREFIX_DIR_PATH}.
 * The binaries compiled for nasux have {@link #NASUX_PREFIX_DIR_PATH} hardcoded in them but it
 * can be changed during compilation.
 *
 * The {@link #NASUX_PACKAGE_NAME} must be the same as the applicationId of nasux-app build.gradle
 * since its also used by {@link #NASUX_FILES_DIR_PATH}.
 * If {@link #NASUX_PACKAGE_NAME} is changed, then binaries, specially used in bootstrap need to be
 * compiled appropriately. Check https://github.com/nasux/nasux-packages/wiki/Building-packages
 * for more info.
 *
 * Ideally the only places where changes should be required if changing package name are the following:
 * - The {@link #NASUX_PACKAGE_NAME} in {@link NasUXConstants}.
 * - The "applicationId" in "build.gradle" of nasux-app. This is package name that android and app
 *      stores will use and is also the final package name stored in "AndroidManifest.xml".
 * - The "manifestPlaceholders" values for {@link #NASUX_PACKAGE_NAME} and *_APP_NAME in
 *      "build.gradle" of nasux-app.
 * - The "ENTITY" values for {@link #NASUX_PACKAGE_NAME} and *_APP_NAME in "strings.xml" of
 *      nasux-app and of nasux-shared.
 * - The "shortcut.xml" and "*_preferences.xml" files of nasux-app since dynamic variables don't
 *      work in it.
 * - Optionally the "package" in "AndroidManifest.xml" if modifying project structure of nasux-app.
 *      This is package name for java classes project structure and is prefixed if activity and service
 *      names use dot (.) notation. This is currently not advisable since this will break lot of
 *      stuff, including nasux-* packages.
 * - Optionally the *_PATH variables in {@link NasUXConstants} containing the string "nasux".
 *
 * Check https://developer.android.com/studio/build/application-id for info on "package" in
 * "AndroidManifest.xml" and "applicationId" in "build.gradle".
 *
 * The {@link #NASUX_PACKAGE_NAME} must be used in source code of NasUX app and its plugins instead
 * of hardcoded "com.nastech.nasux" paths.
 */
public final class NasUXConstants {


    /*
     * NasUX organization variables.
     */

    /** NasUX GitHub organization name */
    public static final String NASUX_GITHUB_ORGANIZATION_NAME = "nasux"; // Default: "nasux"
    /** NasUX GitHub organization url */
    public static final String NASUX_GITHUB_ORGANIZATION_URL = "https://github.com" + "/" + NASUX_GITHUB_ORGANIZATION_NAME; // Default: "https://github.com/nasux"

    /** F-Droid packages base url */
    public static final String FDROID_PACKAGES_BASE_URL = "https://f-droid.org/en/packages"; // Default: "https://f-droid.org/en/packages"





    /*
     * NasUX and its plugin app and package names and urls.
     */

    /** NasUX app name */
    public static final String NASUX_APP_NAME = "NasUX"; // Default: "NasUX"
    /** NasUX package name */
    public static final String NASUX_PACKAGE_NAME = "com.nastech.nasux"; // Default: "com.nastech.nasux"
    /** NasUX GitHub repo name */
    public static final String NASUX_GITHUB_REPO_NAME = "nasux-app"; // Default: "nasux-app"
    /** NasUX GitHub repo url */
    public static final String NASUX_GITHUB_REPO_URL = NASUX_GITHUB_ORGANIZATION_URL + "/" + NASUX_GITHUB_REPO_NAME; // Default: "https://github.com/nasux/nasux-app"
    /** NasUX GitHub issues repo url */
    public static final String NASUX_GITHUB_ISSUES_REPO_URL = NASUX_GITHUB_REPO_URL + "/issues"; // Default: "https://github.com/nasux/nasux-app/issues"
    /** NasUX F-Droid package url */
    public static final String NASUX_FDROID_PACKAGE_URL = FDROID_PACKAGES_BASE_URL + "/" + NASUX_PACKAGE_NAME; // Default: "https://f-droid.org/en/packages/com.nastech.nasux"


    /** NasUX:API app name */
    public static final String NASUX_API_APP_NAME = "NasUX:API"; // Default: "NasUX:API"
    /** NasUX:API app package name */
    public static final String NASUX_API_PACKAGE_NAME = NASUX_PACKAGE_NAME + ".api"; // Default: "com.nastech.nasux.api"
    /** NasUX:API GitHub repo name */
    public static final String NASUX_API_GITHUB_REPO_NAME = "nasux-api"; // Default: "nasux-api"
    /** NasUX:API GitHub repo url */
    public static final String NASUX_API_GITHUB_REPO_URL = NASUX_GITHUB_ORGANIZATION_URL + "/" + NASUX_API_GITHUB_REPO_NAME; // Default: "https://github.com/nasux/nasux-api"
    /** NasUX:API GitHub issues repo url */
    public static final String NASUX_API_GITHUB_ISSUES_REPO_URL = NASUX_API_GITHUB_REPO_URL + "/issues"; // Default: "https://github.com/nasux/nasux-api/issues"
    /** NasUX:API F-Droid package url */
    public static final String NASUX_API_FDROID_PACKAGE_URL = FDROID_PACKAGES_BASE_URL + "/" + NASUX_API_PACKAGE_NAME; // Default: "https://f-droid.org/en/packages/com.nastech.nasux.api"


    /** NasUX:Boot app name */
    public static final String NASUX_BOOT_APP_NAME = "NasUX:Boot"; // Default: "NasUX:Boot"
    /** NasUX:Boot app package name */
    public static final String NASUX_BOOT_PACKAGE_NAME = NASUX_PACKAGE_NAME + ".boot"; // Default: "com.nastech.nasux.boot"
    /** NasUX:Boot GitHub repo name */
    public static final String NASUX_BOOT_GITHUB_REPO_NAME = "nasux-boot"; // Default: "nasux-boot"
    /** NasUX:Boot GitHub repo url */
    public static final String NASUX_BOOT_GITHUB_REPO_URL = NASUX_GITHUB_ORGANIZATION_URL + "/" + NASUX_BOOT_GITHUB_REPO_NAME; // Default: "https://github.com/nasux/nasux-boot"
    /** NasUX:Boot GitHub issues repo url */
    public static final String NASUX_BOOT_GITHUB_ISSUES_REPO_URL = NASUX_BOOT_GITHUB_REPO_URL + "/issues"; // Default: "https://github.com/nasux/nasux-boot/issues"
    /** NasUX:Boot F-Droid package url */
    public static final String NASUX_BOOT_FDROID_PACKAGE_URL = FDROID_PACKAGES_BASE_URL + "/" + NASUX_BOOT_PACKAGE_NAME; // Default: "https://f-droid.org/en/packages/com.nastech.nasux.boot"


    /** NasUX:Float app name */
    public static final String NASUX_FLOAT_APP_NAME = "NasUX:Float"; // Default: "NasUX:Float"
    /** NasUX:Float app package name */
    public static final String NASUX_FLOAT_PACKAGE_NAME = NASUX_PACKAGE_NAME + ".window"; // Default: "com.nastech.nasux.window"
    /** NasUX:Float GitHub repo name */
    public static final String NASUX_FLOAT_GITHUB_REPO_NAME = "nasux-float"; // Default: "nasux-float"
    /** NasUX:Float GitHub repo url */
    public static final String NASUX_FLOAT_GITHUB_REPO_URL = NASUX_GITHUB_ORGANIZATION_URL + "/" + NASUX_FLOAT_GITHUB_REPO_NAME; // Default: "https://github.com/nasux/nasux-float"
    /** NasUX:Float GitHub issues repo url */
    public static final String NASUX_FLOAT_GITHUB_ISSUES_REPO_URL = NASUX_FLOAT_GITHUB_REPO_URL + "/issues"; // Default: "https://github.com/nasux/nasux-float/issues"
    /** NasUX:Float F-Droid package url */
    public static final String NASUX_FLOAT_FDROID_PACKAGE_URL = FDROID_PACKAGES_BASE_URL + "/" + NASUX_FLOAT_PACKAGE_NAME; // Default: "https://f-droid.org/en/packages/com.nastech.nasux.window"


    /** NasUX:Styling app name */
    public static final String NASUX_STYLING_APP_NAME = "NasUX:Styling"; // Default: "NasUX:Styling"
    /** NasUX:Styling app package name */
    public static final String NASUX_STYLING_PACKAGE_NAME = NASUX_PACKAGE_NAME + ".styling"; // Default: "com.nastech.nasux.styling"
    /** NasUX:Styling GitHub repo name */
    public static final String NASUX_STYLING_GITHUB_REPO_NAME = "nasux-styling"; // Default: "nasux-styling"
    /** NasUX:Styling GitHub repo url */
    public static final String NASUX_STYLING_GITHUB_REPO_URL = NASUX_GITHUB_ORGANIZATION_URL + "/" + NASUX_STYLING_GITHUB_REPO_NAME; // Default: "https://github.com/nasux/nasux-styling"
    /** NasUX:Styling GitHub issues repo url */
    public static final String NASUX_STYLING_GITHUB_ISSUES_REPO_URL = NASUX_STYLING_GITHUB_REPO_URL + "/issues"; // Default: "https://github.com/nasux/nasux-styling/issues"
    /** NasUX:Styling F-Droid package url */
    public static final String NASUX_STYLING_FDROID_PACKAGE_URL = FDROID_PACKAGES_BASE_URL + "/" + NASUX_STYLING_PACKAGE_NAME; // Default: "https://f-droid.org/en/packages/com.nastech.nasux.styling"


    /** NasUX:Tasker app name */
    public static final String NASUX_TASKER_APP_NAME = "NasUX:Tasker"; // Default: "NasUX:Tasker"
    /** NasUX:Tasker app package name */
    public static final String NASUX_TASKER_PACKAGE_NAME = NASUX_PACKAGE_NAME + ".tasker"; // Default: "com.nastech.nasux.tasker"
    /** NasUX:Tasker GitHub repo name */
    public static final String NASUX_TASKER_GITHUB_REPO_NAME = "nasux-tasker"; // Default: "nasux-tasker"
    /** NasUX:Tasker GitHub repo url */
    public static final String NASUX_TASKER_GITHUB_REPO_URL = NASUX_GITHUB_ORGANIZATION_URL + "/" + NASUX_TASKER_GITHUB_REPO_NAME; // Default: "https://github.com/nasux/nasux-tasker"
    /** NasUX:Tasker GitHub issues repo url */
    public static final String NASUX_TASKER_GITHUB_ISSUES_REPO_URL = NASUX_TASKER_GITHUB_REPO_URL + "/issues"; // Default: "https://github.com/nasux/nasux-tasker/issues"
    /** NasUX:Tasker F-Droid package url */
    public static final String NASUX_TASKER_FDROID_PACKAGE_URL = FDROID_PACKAGES_BASE_URL + "/" + NASUX_TASKER_PACKAGE_NAME; // Default: "https://f-droid.org/en/packages/com.nastech.nasux.tasker"


    /** NasUX:Widget app name */
    public static final String NASUX_WIDGET_APP_NAME = "NasUX:Widget"; // Default: "NasUX:Widget"
    /** NasUX:Widget app package name */
    public static final String NASUX_WIDGET_PACKAGE_NAME = NASUX_PACKAGE_NAME + ".widget"; // Default: "com.nastech.nasux.widget"
    /** NasUX:Widget GitHub repo name */
    public static final String NASUX_WIDGET_GITHUB_REPO_NAME = "nasux-widget"; // Default: "nasux-widget"
    /** NasUX:Widget GitHub repo url */
    public static final String NASUX_WIDGET_GITHUB_REPO_URL = NASUX_GITHUB_ORGANIZATION_URL + "/" + NASUX_WIDGET_GITHUB_REPO_NAME; // Default: "https://github.com/nasux/nasux-widget"
    /** NasUX:Widget GitHub issues repo url */
    public static final String NASUX_WIDGET_GITHUB_ISSUES_REPO_URL = NASUX_WIDGET_GITHUB_REPO_URL + "/issues"; // Default: "https://github.com/nasux/nasux-widget/issues"
    /** NasUX:Widget F-Droid package url */
    public static final String NASUX_WIDGET_FDROID_PACKAGE_URL = FDROID_PACKAGES_BASE_URL + "/" + NASUX_WIDGET_PACKAGE_NAME; // Default: "https://f-droid.org/en/packages/com.nastech.nasux.widget"





    /*
     * NasUX plugin apps lists.
     */

    public static final List<String> NASUX_PLUGIN_APP_NAMES_LIST = Arrays.asList(
        NASUX_API_APP_NAME,
        NASUX_BOOT_APP_NAME,
        NASUX_FLOAT_APP_NAME,
        NASUX_STYLING_APP_NAME,
        NASUX_TASKER_APP_NAME,
        NASUX_WIDGET_APP_NAME);

    public static final List<String> NASUX_PLUGIN_APP_PACKAGE_NAMES_LIST = Arrays.asList(
        NASUX_API_PACKAGE_NAME,
        NASUX_BOOT_PACKAGE_NAME,
        NASUX_FLOAT_PACKAGE_NAME,
        NASUX_STYLING_PACKAGE_NAME,
        NASUX_TASKER_PACKAGE_NAME,
        NASUX_WIDGET_PACKAGE_NAME);





    /*
     * NasUX APK releases.
     */

    /** F-Droid APK release */
    public static final String APK_RELEASE_FDROID = "F-Droid"; // Default: "F-Droid"

    /** F-Droid APK release signing certificate SHA-256 digest */
    public static final String APK_RELEASE_FDROID_SIGNING_CERTIFICATE_SHA256_DIGEST = "228FB2CFE90831C1499EC3CCAF61E96E8E1CE70766B9474672CE427334D41C42"; // Default: "228FB2CFE90831C1499EC3CCAF61E96E8E1CE70766B9474672CE427334D41C42"

    /** GitHub APK release */
    public static final String APK_RELEASE_GITHUB = "Github"; // Default: "Github"

    /** GitHub APK release signing certificate SHA-256 digest */
    public static final String APK_RELEASE_GITHUB_SIGNING_CERTIFICATE_SHA256_DIGEST = "B6DA01480EEFD5FBF2CD3771B8D1021EC791304BDD6C4BF41D3FAABAD48EE5E1"; // Default: "B6DA01480EEFD5FBF2CD3771B8D1021EC791304BDD6C4BF41D3FAABAD48EE5E1"

    /** Google Play Store APK release */
    public static final String APK_RELEASE_GOOGLE_PLAYSTORE = "Google Play Store"; // Default: "Google Play Store"

    /** Google Play Store APK release signing certificate SHA-256 digest */
    public static final String APK_RELEASE_GOOGLE_PLAYSTORE_SIGNING_CERTIFICATE_SHA256_DIGEST = "738F0A30A04D3C8A1BE304AF18D0779BCF3EA88FB60808F657A3521861C2EBF9"; // Default: "738F0A30A04D3C8A1BE304AF18D0779BCF3EA88FB60808F657A3521861C2EBF9"

    /** NasUX Devs APK release */
    public static final String APK_RELEASE_NASUX_DEVS = "NasUX Devs"; // Default: "NasUX Devs"

    /** NasUX Devs APK release signing certificate SHA-256 digest */
    public static final String APK_RELEASE_NASUX_DEVS_SIGNING_CERTIFICATE_SHA256_DIGEST = "F7A038EB551F1BE8FDF388686B784ABAB4552A5D82DF423E3D8F1B5CBE1C69AE"; // Default: "F7A038EB551F1BE8FDF388686B784ABAB4552A5D82DF423E3D8F1B5CBE1C69AE"





    /*
     * NasUX packages urls.
     */

    /** NasUX Packages GitHub repo name */
    public static final String NASUX_PACKAGES_GITHUB_REPO_NAME = "nasux-packages"; // Default: "nasux-packages"
    /** NasUX Packages GitHub repo url */
    public static final String NASUX_PACKAGES_GITHUB_REPO_URL = NASUX_GITHUB_ORGANIZATION_URL + "/" + NASUX_PACKAGES_GITHUB_REPO_NAME; // Default: "https://github.com/nasux/nasux-packages"
    /** NasUX Packages GitHub issues repo url */
    public static final String NASUX_PACKAGES_GITHUB_ISSUES_REPO_URL = NASUX_PACKAGES_GITHUB_REPO_URL + "/issues"; // Default: "https://github.com/nasux/nasux-packages/issues"


    /** NasUX API apt package name */
    public static final String NASUX_API_APT_PACKAGE_NAME = "nasux-api"; // Default: "nasux-api"
    /** NasUX API apt GitHub repo name */
    public static final String NASUX_API_APT_GITHUB_REPO_NAME = "nasux-api-package"; // Default: "nasux-api-package"
    /** NasUX API apt GitHub repo url */
    public static final String NASUX_API_APT_GITHUB_REPO_URL = NASUX_GITHUB_ORGANIZATION_URL + "/" + NASUX_API_APT_GITHUB_REPO_NAME; // Default: "https://github.com/nasux/nasux-api-package"
    /** NasUX API apt GitHub issues repo url */
    public static final String NASUX_API_APT_GITHUB_ISSUES_REPO_URL = NASUX_API_APT_GITHUB_REPO_URL + "/issues"; // Default: "https://github.com/nasux/nasux-api-package/issues"





    /*
     * NasUX miscellaneous urls.
     */

    /** NasUX Site */
    public static final String NASUX_SITE = NASUX_APP_NAME + " Site"; // Default: "NasUX Site"

    /** NasUX Site url */
    public static final String NASUX_SITE_URL = "https://nasux.dev"; // Default: "https://nasux.dev"

    /** NasUX Wiki */
    public static final String NASUX_WIKI = NASUX_APP_NAME + " Wiki"; // Default: "NasUX Wiki"

    /** NasUX Wiki url */
    public static final String NASUX_WIKI_URL = "https://wiki.nasux.com"; // Default: "https://wiki.nasux.com"

    /** NasUX GitHub wiki repo url */
    public static final String NASUX_GITHUB_WIKI_REPO_URL = NASUX_GITHUB_REPO_URL + "/wiki"; // Default: "https://github.com/nasux/nasux-app/wiki"

    /** NasUX Packages wiki repo url */
    public static final String NASUX_PACKAGES_GITHUB_WIKI_REPO_URL = NASUX_PACKAGES_GITHUB_REPO_URL + "/wiki"; // Default: "https://github.com/nasux/nasux-packages/wiki"


    /** NasUX support email url */
    public static final String NASUX_SUPPORT_EMAIL_URL = "support@nasux.dev"; // Default: "support@nasux.dev"

    /** NasUX support email mailto url */
    public static final String NASUX_SUPPORT_EMAIL_MAILTO_URL = "mailto:" + NASUX_SUPPORT_EMAIL_URL; // Default: "mailto:support@nasux.dev"


    /** NasUX Reddit subreddit */
    public static final String NASUX_REDDIT_SUBREDDIT = "r/nasux"; // Default: "r/nasux"

    /** NasUX Reddit subreddit url */
    public static final String NASUX_REDDIT_SUBREDDIT_URL = "https://www.reddit.com/r/nasux"; // Default: "https://www.reddit.com/r/nasux"


    /** NasUX donate url */
    public static final String NASUX_DONATE_URL = NASUX_SITE_URL + "/donate"; // Default: "https://nasux.dev/donate"





    /*
     * NasUX app core directory paths.
     */

    /** NasUX app internal private app data directory path */
    @SuppressLint("SdCardPath")
    public static final String NASUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH = "/data/data/" + NASUX_PACKAGE_NAME; // Default: "/data/data/com.nastech.nasux"
    /** NasUX app internal private app data directory */
    public static final File NASUX_INTERNAL_PRIVATE_APP_DATA_DIR = new File(NASUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH);



    /** NasUX app Files directory path */
    public static final String NASUX_FILES_DIR_PATH = NASUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH + "/files"; // Default: "/data/data/com.nastech.nasux/files"
    /** NasUX app Files directory */
    public static final File NASUX_FILES_DIR = new File(NASUX_FILES_DIR_PATH);



    /** NasUX app $PREFIX directory path */
    public static final String NASUX_PREFIX_DIR_PATH = NASUX_FILES_DIR_PATH + "/usr"; // Default: "/data/data/com.nastech.nasux/files/usr"
    /** NasUX app $PREFIX directory */
    public static final File NASUX_PREFIX_DIR = new File(NASUX_PREFIX_DIR_PATH);


    /** NasUX app $PREFIX/bin directory path */
    public static final String NASUX_BIN_PREFIX_DIR_PATH = NASUX_PREFIX_DIR_PATH + "/bin"; // Default: "/data/data/com.nastech.nasux/files/usr/bin"
    /** NasUX app $PREFIX/bin directory */
    public static final File NASUX_BIN_PREFIX_DIR = new File(NASUX_BIN_PREFIX_DIR_PATH);


    /** NasUX app $PREFIX/etc directory path */
    public static final String NASUX_ETC_PREFIX_DIR_PATH = NASUX_PREFIX_DIR_PATH + "/etc"; // Default: "/data/data/com.nastech.nasux/files/usr/etc"
    /** NasUX app $PREFIX/etc directory */
    public static final File NASUX_ETC_PREFIX_DIR = new File(NASUX_ETC_PREFIX_DIR_PATH);


    /** NasUX app $PREFIX/include directory path */
    public static final String NASUX_INCLUDE_PREFIX_DIR_PATH = NASUX_PREFIX_DIR_PATH + "/include"; // Default: "/data/data/com.nastech.nasux/files/usr/include"
    /** NasUX app $PREFIX/include directory */
    public static final File NASUX_INCLUDE_PREFIX_DIR = new File(NASUX_INCLUDE_PREFIX_DIR_PATH);


    /** NasUX app $PREFIX/lib directory path */
    public static final String NASUX_LIB_PREFIX_DIR_PATH = NASUX_PREFIX_DIR_PATH + "/lib"; // Default: "/data/data/com.nastech.nasux/files/usr/lib"
    /** NasUX app $PREFIX/lib directory */
    public static final File NASUX_LIB_PREFIX_DIR = new File(NASUX_LIB_PREFIX_DIR_PATH);


    /** NasUX app $PREFIX/libexec directory path */
    public static final String NASUX_LIBEXEC_PREFIX_DIR_PATH = NASUX_PREFIX_DIR_PATH + "/libexec"; // Default: "/data/data/com.nastech.nasux/files/usr/libexec"
    /** NasUX app $PREFIX/libexec directory */
    public static final File NASUX_LIBEXEC_PREFIX_DIR = new File(NASUX_LIBEXEC_PREFIX_DIR_PATH);


    /** NasUX app $PREFIX/share directory path */
    public static final String NASUX_SHARE_PREFIX_DIR_PATH = NASUX_PREFIX_DIR_PATH + "/share"; // Default: "/data/data/com.nastech.nasux/files/usr/share"
    /** NasUX app $PREFIX/share directory */
    public static final File NASUX_SHARE_PREFIX_DIR = new File(NASUX_SHARE_PREFIX_DIR_PATH);


    /** NasUX app $PREFIX/tmp and $TMPDIR directory path */
    public static final String NASUX_TMP_PREFIX_DIR_PATH = NASUX_PREFIX_DIR_PATH + "/tmp"; // Default: "/data/data/com.nastech.nasux/files/usr/tmp"
    /** NasUX app $PREFIX/tmp and $TMPDIR directory */
    public static final File NASUX_TMP_PREFIX_DIR = new File(NASUX_TMP_PREFIX_DIR_PATH);


    /** NasUX app $PREFIX/var directory path */
    public static final String NASUX_VAR_PREFIX_DIR_PATH = NASUX_PREFIX_DIR_PATH + "/var"; // Default: "/data/data/com.nastech.nasux/files/usr/var"
    /** NasUX app $PREFIX/var directory */
    public static final File NASUX_VAR_PREFIX_DIR = new File(NASUX_VAR_PREFIX_DIR_PATH);



    /** NasUX app usr-staging directory path */
    public static final String NASUX_STAGING_PREFIX_DIR_PATH = NASUX_FILES_DIR_PATH + "/usr-staging"; // Default: "/data/data/com.nastech.nasux/files/usr-staging"
    /** NasUX app usr-staging directory */
    public static final File NASUX_STAGING_PREFIX_DIR = new File(NASUX_STAGING_PREFIX_DIR_PATH);



    /** NasUX app $HOME directory path */
    public static final String NASUX_HOME_DIR_PATH = NASUX_FILES_DIR_PATH + "/home"; // Default: "/data/data/com.nastech.nasux/files/home"
    /** NasUX app $HOME directory */
    public static final File NASUX_HOME_DIR = new File(NASUX_HOME_DIR_PATH);


    /** NasUX app config home directory path */
    public static final String NASUX_CONFIG_HOME_DIR_PATH = NASUX_HOME_DIR_PATH + "/.config/nasux"; // Default: "/data/data/com.nastech.nasux/files/home/.config/nasux"
    /** NasUX app config home directory */
    public static final File NASUX_CONFIG_HOME_DIR = new File(NASUX_CONFIG_HOME_DIR_PATH);

    /** NasUX app config $PREFIX directory path */
    public static final String NASUX_CONFIG_PREFIX_DIR_PATH = NASUX_ETC_PREFIX_DIR_PATH + "/nasux"; // Default: "/data/data/com.nastech.nasux/files/usr/etc/nasux"
    /** NasUX app config $PREFIX directory */
    public static final File NASUX_CONFIG_PREFIX_DIR = new File(NASUX_CONFIG_PREFIX_DIR_PATH);


    /** NasUX app data home directory path */
    public static final String NASUX_DATA_HOME_DIR_PATH = NASUX_HOME_DIR_PATH + "/.nasux"; // Default: "/data/data/com.nastech.nasux/files/home/.nasux"
    /** NasUX app data home directory */
    public static final File NASUX_DATA_HOME_DIR = new File(NASUX_DATA_HOME_DIR_PATH);


    /** NasUX app storage home directory path */
    public static final String NASUX_STORAGE_HOME_DIR_PATH = NASUX_HOME_DIR_PATH + "/storage"; // Default: "/data/data/com.nastech.nasux/files/home/storage"
    /** NasUX app storage home directory */
    public static final File NASUX_STORAGE_HOME_DIR = new File(NASUX_STORAGE_HOME_DIR_PATH);



    /** NasUX and plugin apps directory path */
    public static final String NASUX_APPS_DIR_PATH = NASUX_FILES_DIR_PATH + "/apps"; // Default: "/data/data/com.nastech.nasux/files/apps"
    /** NasUX and plugin apps directory */
    public static final File NASUX_APPS_DIR = new File(NASUX_APPS_DIR_PATH);


    /** NasUX app $PREFIX directory path ignored sub file paths to consider it empty */
    public static final List<String> NASUX_PREFIX_DIR_IGNORED_SUB_FILES_PATHS_TO_CONSIDER_AS_EMPTY = Arrays.asList(
        NasUXConstants.NASUX_TMP_PREFIX_DIR_PATH, NasUXConstants.NASUX_ENV_TEMP_FILE_PATH, NasUXConstants.NASUX_ENV_FILE_PATH);



    /*
     * NasUX app and plugin preferences and properties file paths.
     */

    /** NasUX app default SharedPreferences file basename without extension */
    public static final String NASUX_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = NASUX_PACKAGE_NAME + "_preferences"; // Default: "com.nastech.nasux_preferences"

    /** NasUX:API app default SharedPreferences file basename without extension */
    public static final String NASUX_API_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = NASUX_API_PACKAGE_NAME + "_preferences"; // Default: "com.nastech.nasux.api_preferences"

    /** NasUX:Boot app default SharedPreferences file basename without extension */
    public static final String NASUX_BOOT_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = NASUX_BOOT_PACKAGE_NAME + "_preferences"; // Default: "com.nastech.nasux.boot_preferences"

    /** NasUX:Float app default SharedPreferences file basename without extension */
    public static final String NASUX_FLOAT_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = NASUX_FLOAT_PACKAGE_NAME + "_preferences"; // Default: "com.nastech.nasux.window_preferences"

    /** NasUX:Styling app default SharedPreferences file basename without extension */
    public static final String NASUX_STYLING_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = NASUX_STYLING_PACKAGE_NAME + "_preferences"; // Default: "com.nastech.nasux.styling_preferences"

    /** NasUX:Tasker app default SharedPreferences file basename without extension */
    public static final String NASUX_TASKER_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = NASUX_TASKER_PACKAGE_NAME + "_preferences"; // Default: "com.nastech.nasux.tasker_preferences"

    /** NasUX:Widget app default SharedPreferences file basename without extension */
    public static final String NASUX_WIDGET_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = NASUX_WIDGET_PACKAGE_NAME + "_preferences"; // Default: "com.nastech.nasux.widget_preferences"



    /** NasUX app properties primary file path */
    public static final String NASUX_PROPERTIES_PRIMARY_FILE_PATH = NASUX_DATA_HOME_DIR_PATH + "/nasux.properties"; // Default: "/data/data/com.nastech.nasux/files/home/.nasux/nasux.properties"
    /** NasUX app properties primary file */
    public static final File NASUX_PROPERTIES_PRIMARY_FILE = new File(NASUX_PROPERTIES_PRIMARY_FILE_PATH);

    /** NasUX app properties secondary file path */
    public static final String NASUX_PROPERTIES_SECONDARY_FILE_PATH = NASUX_CONFIG_HOME_DIR_PATH + "/nasux.properties"; // Default: "/data/data/com.nastech.nasux/files/home/.config/nasux/nasux.properties"
    /** NasUX app properties secondary file */
    public static final File NASUX_PROPERTIES_SECONDARY_FILE = new File(NASUX_PROPERTIES_SECONDARY_FILE_PATH);

    /** NasUX app properties file paths list. **DO NOT** allow these files to be modified by
     * {@link android.content.ContentProvider} exposed to external apps, since they may silently
     * modify the values for security properties like {@link #PROP_ALLOW_EXTERNAL_APPS} set by users
     * without their explicit consent. */
    public static final List<String> NASUX_PROPERTIES_FILE_PATHS_LIST = Arrays.asList(
        NASUX_PROPERTIES_PRIMARY_FILE_PATH,
        NASUX_PROPERTIES_SECONDARY_FILE_PATH);



    /** NasUX:Float app properties primary file path */
    public static final String NASUX_FLOAT_PROPERTIES_PRIMARY_FILE_PATH = NASUX_DATA_HOME_DIR_PATH + "/nasux.float.properties"; // Default: "/data/data/com.nastech.nasux/files/home/.nasux/nasux.float.properties"
    /** NasUX:Float app properties primary file */
    public static final File NASUX_FLOAT_PROPERTIES_PRIMARY_FILE = new File(NASUX_FLOAT_PROPERTIES_PRIMARY_FILE_PATH);

    /** NasUX:Float app properties secondary file path */
    public static final String NASUX_FLOAT_PROPERTIES_SECONDARY_FILE_PATH = NASUX_CONFIG_HOME_DIR_PATH + "/nasux.float.properties"; // Default: "/data/data/com.nastech.nasux/files/home/.config/nasux/nasux.float.properties"
    /** NasUX:Float app properties secondary file */
    public static final File NASUX_FLOAT_PROPERTIES_SECONDARY_FILE = new File(NASUX_FLOAT_PROPERTIES_SECONDARY_FILE_PATH);

    /** NasUX:Float app properties file paths list. **DO NOT** allow these files to be modified by
     * {@link android.content.ContentProvider} exposed to external apps, since they may silently
     * modify the values for security properties like {@link #PROP_ALLOW_EXTERNAL_APPS} set by users
     * without their explicit consent. */
    public static final List<String> NASUX_FLOAT_PROPERTIES_FILE_PATHS_LIST = Arrays.asList(
        NASUX_FLOAT_PROPERTIES_PRIMARY_FILE_PATH,
        NASUX_FLOAT_PROPERTIES_SECONDARY_FILE_PATH);



    /** NasUX app and NasUX:Styling colors.properties file path */
    public static final String NASUX_COLOR_PROPERTIES_FILE_PATH = NASUX_DATA_HOME_DIR_PATH + "/colors.properties"; // Default: "/data/data/com.nastech.nasux/files/home/.nasux/colors.properties"
    /** NasUX app and NasUX:Styling colors.properties file */
    public static final File NASUX_COLOR_PROPERTIES_FILE = new File(NASUX_COLOR_PROPERTIES_FILE_PATH);

    /** NasUX app and NasUX:Styling font.ttf file path */
    public static final String NASUX_FONT_FILE_PATH = NASUX_DATA_HOME_DIR_PATH + "/font.ttf"; // Default: "/data/data/com.nastech.nasux/files/home/.nasux/font.ttf"
    /** NasUX app and NasUX:Styling font.ttf file */
    public static final File NASUX_FONT_FILE = new File(NASUX_FONT_FILE_PATH);


    /** NasUX app and plugins crash log file path */
    public static final String NASUX_CRASH_LOG_FILE_PATH = NASUX_HOME_DIR_PATH + "/crash_log.md"; // Default: "/data/data/com.nastech.nasux/files/home/crash_log.md"

    /** NasUX app and plugins crash log backup file path */
    public static final String NASUX_CRASH_LOG_BACKUP_FILE_PATH = NASUX_HOME_DIR_PATH + "/crash_log_backup.md"; // Default: "/data/data/com.nastech.nasux/files/home/crash_log_backup.md"


    /** NasUX app environment file path */
    public static final String NASUX_ENV_FILE_PATH = NASUX_CONFIG_PREFIX_DIR_PATH + "/nasux.env"; // Default: "/data/data/com.nastech.nasux/files/usr/etc/nasux/nasux.env"

    /** NasUX app environment temp file path */
    public static final String NASUX_ENV_TEMP_FILE_PATH = NASUX_CONFIG_PREFIX_DIR_PATH + "/nasux.env.tmp"; // Default: "/data/data/com.nastech.nasux/files/usr/etc/nasux/nasux.env.tmp"




    /*
     * NasUX app plugin specific paths.
     */

    /** NasUX app directory path to store scripts to be run at boot by NasUX:Boot */
    public static final String NASUX_BOOT_SCRIPTS_DIR_PATH = NASUX_DATA_HOME_DIR_PATH + "/boot"; // Default: "/data/data/com.nastech.nasux/files/home/.nasux/boot"
    /** NasUX app directory to store scripts to be run at boot by NasUX:Boot */
    public static final File NASUX_BOOT_SCRIPTS_DIR = new File(NASUX_BOOT_SCRIPTS_DIR_PATH);


    /** NasUX app directory path to store foreground scripts that can be run by the nasux launcher
     * widget provided by NasUX:Widget */
    public static final String NASUX_SHORTCUT_SCRIPTS_DIR_PATH = NASUX_HOME_DIR_PATH + "/.shortcuts"; // Default: "/data/data/com.nastech.nasux/files/home/.shortcuts"
    /** NasUX app directory to store foreground scripts that can be run by the nasux launcher widget provided by NasUX:Widget */
    public static final File NASUX_SHORTCUT_SCRIPTS_DIR = new File(NASUX_SHORTCUT_SCRIPTS_DIR_PATH);


    /** NasUX app directory basename that stores background scripts that can be run by the nasux
     * launcher widget provided by NasUX:Widget */
    public static final String NASUX_SHORTCUT_TASKS_SCRIPTS_DIR_BASENAME =  "tasks"; // Default: "tasks"
    /** NasUX app directory path to store background scripts that can be run by the nasux launcher
     * widget provided by NasUX:Widget */
    public static final String NASUX_SHORTCUT_TASKS_SCRIPTS_DIR_PATH = NASUX_SHORTCUT_SCRIPTS_DIR_PATH + "/" + NASUX_SHORTCUT_TASKS_SCRIPTS_DIR_BASENAME; // Default: "/data/data/com.nastech.nasux/files/home/.shortcuts/tasks"
    /** NasUX app directory to store background scripts that can be run by the nasux launcher widget provided by NasUX:Widget */
    public static final File NASUX_SHORTCUT_TASKS_SCRIPTS_DIR = new File(NASUX_SHORTCUT_TASKS_SCRIPTS_DIR_PATH);


    /** NasUX app directory basename that stores icons for the foreground and background scripts
     * that can be run by the nasux launcher widget provided by NasUX:Widget */
    public static final String NASUX_SHORTCUT_SCRIPT_ICONS_DIR_BASENAME =  "icons"; // Default: "icons"
    /** NasUX app directory path to store icons for the foreground and background scripts that can
     * be run by the nasux launcher widget provided by NasUX:Widget */
    public static final String NASUX_SHORTCUT_SCRIPT_ICONS_DIR_PATH = NASUX_SHORTCUT_SCRIPTS_DIR_PATH + "/" + NASUX_SHORTCUT_SCRIPT_ICONS_DIR_BASENAME; // Default: "/data/data/com.nastech.nasux/files/home/.shortcuts/icons"
    /** NasUX app directory to store icons for the foreground and background scripts that can be
     * run by the nasux launcher widget provided by NasUX:Widget */
    public static final File NASUX_SHORTCUT_SCRIPT_ICONS_DIR = new File(NASUX_SHORTCUT_SCRIPT_ICONS_DIR_PATH);


    /** NasUX app directory path to store scripts to be run by 3rd party twofortyfouram locale plugin
     * host apps like Tasker app via the NasUX:Tasker plugin client */
    public static final String NASUX_TASKER_SCRIPTS_DIR_PATH = NASUX_DATA_HOME_DIR_PATH + "/tasker"; // Default: "/data/data/com.nastech.nasux/files/home/.nasux/tasker"
    /** NasUX app directory to store scripts to be run by 3rd party twofortyfouram locale plugin host apps like Tasker app via the NasUX:Tasker plugin client */
    public static final File NASUX_TASKER_SCRIPTS_DIR = new File(NASUX_TASKER_SCRIPTS_DIR_PATH);





    /*
     * NasUX app and plugins notification variables.
     */

    /** NasUX app notification channel id used by {@link NASUX_APP.NASUX_SERVICE} */
    public static final String NASUX_APP_NOTIFICATION_CHANNEL_ID = "nasux_notification_channel";
    /** NasUX app notification channel name used by {@link NASUX_APP.NASUX_SERVICE} */
    public static final String NASUX_APP_NOTIFICATION_CHANNEL_NAME = NasUXConstants.NASUX_APP_NAME + " App";
    /** NasUX app unique notification id used by {@link NASUX_APP.NASUX_SERVICE} */
    public static final int NASUX_APP_NOTIFICATION_ID = 1337;

    /** NasUX app notification channel id used by {@link NASUX_APP.RUN_COMMAND_SERVICE} */
    public static final String NASUX_RUN_COMMAND_NOTIFICATION_CHANNEL_ID = "nasux_run_command_notification_channel";
    /** NasUX app notification channel name used by {@link NASUX_APP.RUN_COMMAND_SERVICE} */
    public static final String NASUX_RUN_COMMAND_NOTIFICATION_CHANNEL_NAME = NasUXConstants.NASUX_APP_NAME + " RunCommandService";
    /** NasUX app unique notification id used by {@link NASUX_APP.RUN_COMMAND_SERVICE} */
    public static final int NASUX_RUN_COMMAND_NOTIFICATION_ID = 1338;

    /** NasUX app notification channel id used for plugin command errors */
    public static final String NASUX_PLUGIN_COMMAND_ERRORS_NOTIFICATION_CHANNEL_ID = "nasux_plugin_command_errors_notification_channel";
    /** NasUX app notification channel name used for plugin command errors */
    public static final String NASUX_PLUGIN_COMMAND_ERRORS_NOTIFICATION_CHANNEL_NAME = NasUXConstants.NASUX_APP_NAME + " Plugin Commands Errors";

    /** NasUX app notification channel id used for crash reports */
    public static final String NASUX_CRASH_REPORTS_NOTIFICATION_CHANNEL_ID = "nasux_crash_reports_notification_channel";
    /** NasUX app notification channel name used for crash reports */
    public static final String NASUX_CRASH_REPORTS_NOTIFICATION_CHANNEL_NAME = NasUXConstants.NASUX_APP_NAME + " Crash Reports";


    /** NasUX app notification channel id used by {@link NASUX_FLOAT_APP.NASUX_FLOAT_SERVICE} */
    public static final String NASUX_FLOAT_APP_NOTIFICATION_CHANNEL_ID = "nasux_float_notification_channel";
    /** NasUX app notification channel name used by {@link NASUX_FLOAT_APP.NASUX_FLOAT_SERVICE} */
    public static final String NASUX_FLOAT_APP_NOTIFICATION_CHANNEL_NAME = NasUXConstants.NASUX_FLOAT_APP_NAME + " App";
    /** NasUX app unique notification id used by {@link NASUX_APP.NASUX_SERVICE} */
    public static final int NASUX_FLOAT_APP_NOTIFICATION_ID = 1339;





    /*
     * NasUX app and plugins miscellaneous variables.
     */

    /** Android OS permission declared by NasUX app in AndroidManifest.xml which can be requested by
     * 3rd party apps to run various commands in NasUX app context */
    public static final String PERMISSION_RUN_COMMAND = NASUX_PACKAGE_NAME + ".permission.RUN_COMMAND"; // Default: "com.nastech.nasux.permission.RUN_COMMAND"

    /** NasUX property defined in nasux.properties file as a secondary check to PERMISSION_RUN_COMMAND
     * to allow 3rd party apps to run various commands in NasUX app context */
    public static final String PROP_ALLOW_EXTERNAL_APPS = "allow-external-apps"; // Default: "allow-external-apps"
    /** Default value for {@link #PROP_ALLOW_EXTERNAL_APPS} */
    public static final String PROP_DEFAULT_VALUE_ALLOW_EXTERNAL_APPS = "false"; // Default: "false"

    /** The broadcast action sent when NasUX App opens */
    public static final String BROADCAST_NASUX_OPENED = NASUX_PACKAGE_NAME + ".app.OPENED";

    /** The Uri authority for NasUX app file shares */
    public static final String NASUX_FILE_SHARE_URI_AUTHORITY = NASUX_PACKAGE_NAME + ".files"; // Default: "com.nastech.nasux.files"

    /** The normal comma character (U+002C, &comma;, &#44;, comma) */
    public static final String COMMA_NORMAL = ","; // Default: ","

    /** The alternate comma character (U+201A, &sbquo;, &#8218;, single low-9 quotation mark) that
     * may be used instead of {@link #COMMA_NORMAL} */
    public static final String COMMA_ALTERNATIVE = "‚"; // Default: "‚"

    /** Environment variable prefix root for the NasUX app. */
    public static final String NASUX_ENV_PREFIX_ROOT = "NASUX";






    /**
     * NasUX app constants.
     */
    public static final class NASUX_APP {

        /** NasUX apps directory path */
        public static final String APPS_DIR_PATH = NASUX_APPS_DIR_PATH + "/" + NASUX_PACKAGE_NAME; // Default: "/data/data/com.nastech.nasux/files/apps/com.nastech.nasux"

        /** nasux-am socket file path */
        public static final String NASUX_AM_SOCKET_FILE_PATH = APPS_DIR_PATH + "/nasux-am/am.sock"; // Default: "/data/data/com.nastech.nasux/files/apps/com.nastech.nasux/nasux-am/am.sock"


        /** NasUX app BuildConfig class name */
        public static final String BUILD_CONFIG_CLASS_NAME = NASUX_PACKAGE_NAME + ".BuildConfig"; // Default: "com.nastech.nasux.BuildConfig"

        /** NasUX app FileShareReceiverActivity class name */
        public static final String FILE_SHARE_RECEIVER_ACTIVITY_CLASS_NAME = NASUX_PACKAGE_NAME + ".app.api.file.FileShareReceiverActivity"; // Default: "com.nastech.nasux.app.api.file.FileShareReceiverActivity"

        /** NasUX app FileViewReceiverActivity class name */
        public static final String FILE_VIEW_RECEIVER_ACTIVITY_CLASS_NAME = NASUX_PACKAGE_NAME + ".app.api.file.FileViewReceiverActivity"; // Default: "com.nastech.nasux.app.api.file.FileViewReceiverActivity"


        /** NasUX app core activity name. */
        public static final String NASUX_ACTIVITY_NAME = NASUX_PACKAGE_NAME + ".app.NasUXActivity"; // Default: "com.nastech.nasux.app.NasUXActivity"

        /**
         * NasUX app core activity.
         */
        public static final class NASUX_ACTIVITY {

            /** Intent extra for if nasux failsafe session needs to be started and is used by {@link NASUX_ACTIVITY} and {@link NASUX_SERVICE#ACTION_STOP_SERVICE} */
            public static final String EXTRA_FAILSAFE_SESSION = NasUXConstants.NASUX_PACKAGE_NAME + ".app.failsafe_session"; // Default: "com.nastech.nasux.app.failsafe_session"


            /** Intent action to make nasux app notify user that a crash happened. */
            public static final String ACTION_NOTIFY_APP_CRASH = NasUXConstants.NASUX_PACKAGE_NAME + ".app.notify_app_crash"; // Default: "com.nastech.nasux.app.notify_app_crash"


            /** Intent action to make nasux reload its nasux session styling */
            public static final String ACTION_RELOAD_STYLE = NasUXConstants.NASUX_PACKAGE_NAME + ".app.reload_style"; // Default: "com.nastech.nasux.app.reload_style"
            /** Intent {@code String} extra for what to reload for the NASUX_ACTIVITY.ACTION_RELOAD_STYLE intent. This has been deperecated. */
            @Deprecated
            public static final String EXTRA_RELOAD_STYLE = NasUXConstants.NASUX_PACKAGE_NAME + ".app.reload_style"; // Default: "com.nastech.nasux.app.reload_style"

            /**  Intent {@code boolean} extra for whether to recreate activity for the NASUX_ACTIVITY.ACTION_RELOAD_STYLE intent. */
            public static final String EXTRA_RECREATE_ACTIVITY = NASUX_APP.NASUX_ACTIVITY_NAME + ".EXTRA_RECREATE_ACTIVITY"; // Default: "com.nastech.nasux.app.NasUXActivity.EXTRA_RECREATE_ACTIVITY"


            /** Intent action to make nasux request storage permissions */
            public static final String ACTION_REQUEST_PERMISSIONS = NasUXConstants.NASUX_PACKAGE_NAME + ".app.request_storage_permissions"; // Default: "com.nastech.nasux.app.request_storage_permissions"
        }





        /** NasUX app settings activity name. */
        public static final String NASUX_SETTINGS_ACTIVITY_NAME = NASUX_PACKAGE_NAME + ".app.activities.SettingsActivity"; // Default: "com.nastech.nasux.app.activities.SettingsActivity"





        /** NasUX app core service name. */
        public static final String NASUX_SERVICE_NAME = NASUX_PACKAGE_NAME + ".app.NasUXService"; // Default: "com.nastech.nasux.app.NasUXService"

        /**
         * NasUX app core service.
         */
        public static final class NASUX_SERVICE {

            /** Intent action to stop NASUX_SERVICE */
            public static final String ACTION_STOP_SERVICE = NASUX_PACKAGE_NAME + ".service_stop"; // Default: "com.nastech.nasux.service_stop"


            /** Intent action to make NASUX_SERVICE acquire a wakelock */
            public static final String ACTION_WAKE_LOCK = NASUX_PACKAGE_NAME + ".service_wake_lock"; // Default: "com.nastech.nasux.service_wake_lock"


            /** Intent action to make NASUX_SERVICE release wakelock */
            public static final String ACTION_WAKE_UNLOCK = NASUX_PACKAGE_NAME + ".service_wake_unlock"; // Default: "com.nastech.nasux.service_wake_unlock"


            /** Intent action to execute command with NASUX_SERVICE */
            public static final String ACTION_SERVICE_EXECUTE = NASUX_PACKAGE_NAME + ".service_execute"; // Default: "com.nastech.nasux.service_execute"

            /** Uri scheme for paths sent via intent to NASUX_SERVICE */
            public static final String URI_SCHEME_SERVICE_EXECUTE = NASUX_PACKAGE_NAME + ".file"; // Default: "com.nastech.nasux.file"
            /** Intent {@code String[]} extra for arguments to the executable of the command for the NASUX_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_ARGUMENTS = NASUX_PACKAGE_NAME + ".execute.arguments"; // Default: "com.nastech.nasux.execute.arguments"
            /** Intent {@code String} extra for stdin of the command for the NASUX_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_STDIN = NASUX_PACKAGE_NAME + ".execute.stdin"; // Default: "com.nastech.nasux.execute.stdin"
            /** Intent {@code String} extra for command current working directory for the NASUX_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_WORKDIR = NASUX_PACKAGE_NAME + ".execute.cwd"; // Default: "com.nastech.nasux.execute.cwd"
            /** Intent {@code boolean} extra for whether to run command in background {@link Runner#APP_SHELL} or foreground {@link Runner#TERMINAL_SESSION} for the NASUX_SERVICE.ACTION_SERVICE_EXECUTE intent */
            @Deprecated
            public static final String EXTRA_BACKGROUND = NASUX_PACKAGE_NAME + ".execute.background"; // Default: "com.nastech.nasux.execute.background"
            /** Intent {@code String} extra for command the {@link Runner} for the NASUX_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_RUNNER = NASUX_PACKAGE_NAME + ".execute.runner"; // Default: "com.nastech.nasux.execute.runner"
            /** Intent {@code String} extra for custom log level for background commands defined by {@link com.nastech.nasux.shared.logger.Logger} for the NASUX_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_BACKGROUND_CUSTOM_LOG_LEVEL = NASUX_PACKAGE_NAME + ".execute.background_custom_log_level"; // Default: "com.nastech.nasux.execute.background_custom_log_level"
            /** Intent {@code String} extra for session action for {@link Runner#TERMINAL_SESSION} commands for the NASUX_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_SESSION_ACTION = NASUX_PACKAGE_NAME + ".execute.session_action"; // Default: "com.nastech.nasux.execute.session_action"
            /** Intent {@code String} extra for shell name for commands for the NASUX_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_SHELL_NAME = NASUX_PACKAGE_NAME + ".execute.shell_name"; // Default: "com.nastech.nasux.execute.shell_name"
            /** Intent {@code String} extra for the {@link ExecutionCommand.ShellCreateMode}  for the NASUX_SERVICE.ACTION_SERVICE_EXECUTE intent. */
            public static final String EXTRA_SHELL_CREATE_MODE = NASUX_PACKAGE_NAME + ".execute.shell_create_mode"; // Default: "com.nastech.nasux.execute.shell_create_mode"
            /** Intent {@code String} extra for label of the command for the NASUX_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_COMMAND_LABEL = NASUX_PACKAGE_NAME + ".execute.command_label"; // Default: "com.nastech.nasux.execute.command_label"
            /** Intent markdown {@code String} extra for description of the command for the NASUX_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_COMMAND_DESCRIPTION = NASUX_PACKAGE_NAME + ".execute.command_description"; // Default: "com.nastech.nasux.execute.command_description"
            /** Intent markdown {@code String} extra for help of the command for the NASUX_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_COMMAND_HELP = NASUX_PACKAGE_NAME + ".execute.command_help"; // Default: "com.nastech.nasux.execute.command_help"
            /** Intent markdown {@code String} extra for help of the plugin API for the NASUX_SERVICE.ACTION_SERVICE_EXECUTE intent (Internal Use Only) */
            public static final String EXTRA_PLUGIN_API_HELP = NASUX_PACKAGE_NAME + ".execute.plugin_api_help"; // Default: "com.nastech.nasux.execute.plugin_help"
            /** Intent {@code Parcelable} extra for the pending intent that should be sent with the
             * result of the execution command to the execute command caller for the NASUX_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_PENDING_INTENT = "pendingIntent"; // Default: "pendingIntent"
            /** Intent {@code String} extra for the directory path in which to write the result of the
             * execution command for the execute command caller for the NASUX_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_RESULT_DIRECTORY = NASUX_PACKAGE_NAME + ".execute.result_directory"; // Default: "com.nastech.nasux.execute.result_directory"
            /** Intent {@code boolean} extra for whether the result should be written to a single file
             * or multiple files (err, errmsg, stdout, stderr, exit_code) in
             * {@link #EXTRA_RESULT_DIRECTORY} for the NASUX_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_RESULT_SINGLE_FILE = NASUX_PACKAGE_NAME + ".execute.result_single_file"; // Default: "com.nastech.nasux.execute.result_single_file"
            /** Intent {@code String} extra for the basename of the result file that should be created
             * in {@link #EXTRA_RESULT_DIRECTORY} if {@link #EXTRA_RESULT_SINGLE_FILE} is {@code true}
             * for the NASUX_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_RESULT_FILE_BASENAME = NASUX_PACKAGE_NAME + ".execute.result_file_basename"; // Default: "com.nastech.nasux.execute.result_file_basename"
            /** Intent {@code String} extra for the output {@link Formatter} format of the
             * {@link #EXTRA_RESULT_FILE_BASENAME} result file for the NASUX_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_RESULT_FILE_OUTPUT_FORMAT = NASUX_PACKAGE_NAME + ".execute.result_file_output_format"; // Default: "com.nastech.nasux.execute.result_file_output_format"
            /** Intent {@code String} extra for the error {@link Formatter} format of the
             * {@link #EXTRA_RESULT_FILE_BASENAME} result file for the NASUX_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_RESULT_FILE_ERROR_FORMAT = NASUX_PACKAGE_NAME + ".execute.result_file_error_format"; // Default: "com.nastech.nasux.execute.result_file_error_format"
            /** Intent {@code String} extra for the optional suffix of the result files that should
             * be created in {@link #EXTRA_RESULT_DIRECTORY} if {@link #EXTRA_RESULT_SINGLE_FILE} is
             * {@code false} for the NASUX_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_RESULT_FILES_SUFFIX = NASUX_PACKAGE_NAME + ".execute.result_files_suffix"; // Default: "com.nastech.nasux.execute.result_files_suffix"



            /**
             * The value for {@link #EXTRA_SESSION_ACTION} extra that will set the new session as
             * the current session and will start {@link NASUX_ACTIVITY} if its not running to bring
             * the new session to foreground.
             */
            public static final int VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_OPEN_ACTIVITY = 0;

            /**
             * The value for {@link #EXTRA_SESSION_ACTION} extra that will keep any existing session
             * as the current session and will start {@link NASUX_ACTIVITY} if its not running to
             * bring the existing session to foreground. The new session will be added to the left
             * sidebar in the sessions list.
             */
            public static final int VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_OPEN_ACTIVITY = 1;

            /**
             * The value for {@link #EXTRA_SESSION_ACTION} extra that will set the new session as
             * the current session but will not start {@link NASUX_ACTIVITY} if its not running
             * and session(s) will be seen in NasUX notification and can be clicked to bring new
             * session to foreground. If the {@link NASUX_ACTIVITY} is already running, then this
             * will behave like {@link #VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_OPEN_ACTIVITY}.
             */
            public static final int VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_DONT_OPEN_ACTIVITY = 2;

            /**
             * The value for {@link #EXTRA_SESSION_ACTION} extra that will keep any existing session
             * as the current session but will not start {@link NASUX_ACTIVITY} if its not running
             * and session(s) will be seen in NasUX notification and can be clicked to bring
             * existing session to foreground. If the {@link NASUX_ACTIVITY} is already running,
             * then this will behave like {@link #VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_OPEN_ACTIVITY}.
             */
            public static final int VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_DONT_OPEN_ACTIVITY = 3;

            /** The minimum allowed value for {@link #EXTRA_SESSION_ACTION}. */
            public static final int MIN_VALUE_EXTRA_SESSION_ACTION = VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_OPEN_ACTIVITY;

            /** The maximum allowed value for {@link #EXTRA_SESSION_ACTION}. */
            public static final int MAX_VALUE_EXTRA_SESSION_ACTION = VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_DONT_OPEN_ACTIVITY;


            /** Intent {@code Bundle} extra to store result of execute command that is sent back for the
             * NASUX_SERVICE.ACTION_SERVICE_EXECUTE intent if the {@link #EXTRA_PENDING_INTENT} is not
             * {@code null} */
            public static final String EXTRA_PLUGIN_RESULT_BUNDLE = "result"; // Default: "result"
            /** Intent {@code String} extra for stdout value of execute command of the {@link #EXTRA_PLUGIN_RESULT_BUNDLE} */
            public static final String EXTRA_PLUGIN_RESULT_BUNDLE_STDOUT = "stdout"; // Default: "stdout"
            /** Intent {@code String} extra for original length of stdout value of execute command of the {@link #EXTRA_PLUGIN_RESULT_BUNDLE} */
            public static final String EXTRA_PLUGIN_RESULT_BUNDLE_STDOUT_ORIGINAL_LENGTH = "stdout_original_length"; // Default: "stdout_original_length"
            /** Intent {@code String} extra for stderr value of execute command of the {@link #EXTRA_PLUGIN_RESULT_BUNDLE} */
            public static final String EXTRA_PLUGIN_RESULT_BUNDLE_STDERR = "stderr"; // Default: "stderr"
            /** Intent {@code String} extra for original length of stderr value of execute command of the {@link #EXTRA_PLUGIN_RESULT_BUNDLE} */
            public static final String EXTRA_PLUGIN_RESULT_BUNDLE_STDERR_ORIGINAL_LENGTH = "stderr_original_length"; // Default: "stderr_original_length"
            /** Intent {@code int} extra for exit code value of execute command of the {@link #EXTRA_PLUGIN_RESULT_BUNDLE} */
            public static final String EXTRA_PLUGIN_RESULT_BUNDLE_EXIT_CODE = "exitCode"; // Default: "exitCode"
            /** Intent {@code int} extra for err value of execute command of the {@link #EXTRA_PLUGIN_RESULT_BUNDLE} */
            public static final String EXTRA_PLUGIN_RESULT_BUNDLE_ERR = "err"; // Default: "err"
            /** Intent {@code String} extra for errmsg value of execute command of the {@link #EXTRA_PLUGIN_RESULT_BUNDLE} */
            public static final String EXTRA_PLUGIN_RESULT_BUNDLE_ERRMSG = "errmsg"; // Default: "errmsg"

        }





        /** NasUX app run command service name. */
        public static final String RUN_COMMAND_SERVICE_NAME = NASUX_PACKAGE_NAME + ".app.RunCommandService"; // NasUX app service to receive commands from 3rd party apps "com.nastech.nasux.app.RunCommandService"

        /**
         * NasUX app run command service to receive commands sent by 3rd party apps.
         */
        public static final class RUN_COMMAND_SERVICE {

            /** NasUX RUN_COMMAND Intent help url */
            public static final String RUN_COMMAND_API_HELP_URL = NASUX_GITHUB_WIKI_REPO_URL + "/RUN_COMMAND-Intent"; // Default: "https://github.com/nasux/nasux-app/wiki/RUN_COMMAND-Intent"


            /** Intent action to execute command with RUN_COMMAND_SERVICE */
            public static final String ACTION_RUN_COMMAND = NASUX_PACKAGE_NAME + ".RUN_COMMAND"; // Default: "com.nastech.nasux.RUN_COMMAND"

            /** Intent {@code String} extra for absolute path of command for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_COMMAND_PATH = NASUX_PACKAGE_NAME + ".RUN_COMMAND_PATH"; // Default: "com.nastech.nasux.RUN_COMMAND_PATH"
            /** Intent {@code String[]} extra for arguments to the executable of the command for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_ARGUMENTS = NASUX_PACKAGE_NAME + ".RUN_COMMAND_ARGUMENTS"; // Default: "com.nastech.nasux.RUN_COMMAND_ARGUMENTS"
            /** Intent {@code boolean} extra for whether to replace comma alternative characters in arguments with comma characters for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_REPLACE_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS = NASUX_PACKAGE_NAME + ".RUN_COMMAND_REPLACE_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS"; // Default: "com.nastech.nasux.RUN_COMMAND_REPLACE_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS"
            /** Intent {@code String} extra for the comma alternative characters in arguments that should be replaced instead of the default {@link #COMMA_ALTERNATIVE} for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS = NASUX_PACKAGE_NAME + ".RUN_COMMAND_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS"; // Default: "com.nastech.nasux.RUN_COMMAND_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS"

            /** Intent {@code String} extra for stdin of the command for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_STDIN = NASUX_PACKAGE_NAME + ".RUN_COMMAND_STDIN"; // Default: "com.nastech.nasux.RUN_COMMAND_STDIN"
            /** Intent {@code String} extra for current working directory of command for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_WORKDIR = NASUX_PACKAGE_NAME + ".RUN_COMMAND_WORKDIR"; // Default: "com.nastech.nasux.RUN_COMMAND_WORKDIR"
            /** Intent {@code boolean} extra for whether to run command in background {@link Runner#APP_SHELL} or foreground {@link Runner#TERMINAL_SESSION} for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            @Deprecated
            public static final String EXTRA_BACKGROUND = NASUX_PACKAGE_NAME + ".RUN_COMMAND_BACKGROUND"; // Default: "com.nastech.nasux.RUN_COMMAND_BACKGROUND"
            /** Intent {@code String} extra for command the {@link Runner} for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_RUNNER = NASUX_PACKAGE_NAME + ".RUN_COMMAND_RUNNER"; // Default: "com.nastech.nasux.RUN_COMMAND_RUNNER"
            /** Intent {@code String} extra for custom log level for background commands defined by {@link com.nastech.nasux.shared.logger.Logger} for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_BACKGROUND_CUSTOM_LOG_LEVEL = NASUX_PACKAGE_NAME + ".RUN_COMMAND_BACKGROUND_CUSTOM_LOG_LEVEL"; // Default: "com.nastech.nasux.RUN_COMMAND_BACKGROUND_CUSTOM_LOG_LEVEL"
            /** Intent {@code String} extra for session action of {@link Runner#TERMINAL_SESSION} commands for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_SESSION_ACTION = NASUX_PACKAGE_NAME + ".RUN_COMMAND_SESSION_ACTION"; // Default: "com.nastech.nasux.RUN_COMMAND_SESSION_ACTION"
            /** Intent {@code String} extra for shell name of commands for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_SHELL_NAME = NASUX_PACKAGE_NAME + ".RUN_COMMAND_SHELL_NAME"; // Default: "com.nastech.nasux.RUN_COMMAND_SHELL_NAME"
            /** Intent {@code String} extra for the {@link ExecutionCommand.ShellCreateMode}  for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent. */
            public static final String EXTRA_SHELL_CREATE_MODE = NASUX_PACKAGE_NAME + ".RUN_COMMAND_SHELL_CREATE_MODE"; // Default: "com.nastech.nasux.RUN_COMMAND_SHELL_CREATE_MODE"
            /** Intent {@code String} extra for label of the command for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_COMMAND_LABEL = NASUX_PACKAGE_NAME + ".RUN_COMMAND_COMMAND_LABEL"; // Default: "com.nastech.nasux.RUN_COMMAND_COMMAND_LABEL"
            /** Intent markdown {@code String} extra for description of the command for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_COMMAND_DESCRIPTION = NASUX_PACKAGE_NAME + ".RUN_COMMAND_COMMAND_DESCRIPTION"; // Default: "com.nastech.nasux.RUN_COMMAND_COMMAND_DESCRIPTION"
            /** Intent markdown {@code String} extra for help of the command for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_COMMAND_HELP = NASUX_PACKAGE_NAME + ".RUN_COMMAND_COMMAND_HELP"; // Default: "com.nastech.nasux.RUN_COMMAND_COMMAND_HELP"
            /** Intent {@code Parcelable} extra for the pending intent that should be sent with the result of the execution command to the execute command caller for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_PENDING_INTENT = NASUX_PACKAGE_NAME + ".RUN_COMMAND_PENDING_INTENT"; // Default: "com.nastech.nasux.RUN_COMMAND_PENDING_INTENT"
            /** Intent {@code String} extra for the directory path in which to write the result of
             * the execution command for the execute command caller for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_RESULT_DIRECTORY = NASUX_PACKAGE_NAME + ".RUN_COMMAND_RESULT_DIRECTORY"; // Default: "com.nastech.nasux.RUN_COMMAND_RESULT_DIRECTORY"
            /** Intent {@code boolean} extra for whether the result should be written to a single file
             * or multiple files (err, errmsg, stdout, stderr, exit_code) in
             * {@link #EXTRA_RESULT_DIRECTORY} for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_RESULT_SINGLE_FILE = NASUX_PACKAGE_NAME + ".RUN_COMMAND_RESULT_SINGLE_FILE"; // Default: "com.nastech.nasux.RUN_COMMAND_RESULT_SINGLE_FILE"
            /** Intent {@code String} extra for the basename of the result file that should be created
             * in {@link #EXTRA_RESULT_DIRECTORY} if {@link #EXTRA_RESULT_SINGLE_FILE} is {@code true}
             * for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_RESULT_FILE_BASENAME = NASUX_PACKAGE_NAME + ".RUN_COMMAND_RESULT_FILE_BASENAME"; // Default: "com.nastech.nasux.RUN_COMMAND_RESULT_FILE_BASENAME"
            /** Intent {@code String} extra for the output {@link Formatter} format of the
             * {@link #EXTRA_RESULT_FILE_BASENAME} result file for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_RESULT_FILE_OUTPUT_FORMAT = NASUX_PACKAGE_NAME + ".RUN_COMMAND_RESULT_FILE_OUTPUT_FORMAT"; // Default: "com.nastech.nasux.RUN_COMMAND_RESULT_FILE_OUTPUT_FORMAT"
            /** Intent {@code String} extra for the error {@link Formatter} format of the
             * {@link #EXTRA_RESULT_FILE_BASENAME} result file for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_RESULT_FILE_ERROR_FORMAT = NASUX_PACKAGE_NAME + ".RUN_COMMAND_RESULT_FILE_ERROR_FORMAT"; // Default: "com.nastech.nasux.RUN_COMMAND_RESULT_FILE_ERROR_FORMAT"
            /** Intent {@code String} extra for the optional suffix of the result files that should be
             * created in {@link #EXTRA_RESULT_DIRECTORY} if {@link #EXTRA_RESULT_SINGLE_FILE} is
             * {@code false} for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_RESULT_FILES_SUFFIX = NASUX_PACKAGE_NAME + ".RUN_COMMAND_RESULT_FILES_SUFFIX"; // Default: "com.nastech.nasux.RUN_COMMAND_RESULT_FILES_SUFFIX"

        }
    }


    /**
     * NasUX:API app constants.
     */
    public static final class NASUX_API_APP {

        /** NasUX:API app main activity name. */
        public static final String NASUX_API_MAIN_ACTIVITY_NAME = NASUX_API_PACKAGE_NAME + ".activities.NasUXAPIMainActivity"; // Default: "com.nastech.nasux.api.activities.NasUXAPIMainActivity"

        /** NasUX:API app launcher activity name. This is an `activity-alias` for {@link #NASUX_API_MAIN_ACTIVITY_NAME} used for launchers with {@link Intent#CATEGORY_LAUNCHER}. */
        public static final String NASUX_API_LAUNCHER_ACTIVITY_NAME = NASUX_API_PACKAGE_NAME + ".activities.NasUXAPILauncherActivity"; // Default: "com.nastech.nasux.api.activities.NasUXAPILauncherActivity"

    }





    /**
     * NasUX:Boot app constants.
     */
    public static final class NASUX_BOOT_APP {

        /** NasUX:Boot app main activity name. */
        public static final String NASUX_BOOT_MAIN_ACTIVITY_NAME = NASUX_BOOT_PACKAGE_NAME + ".activities.NasUXBootMainActivity"; // Default: "com.nastech.nasux.boot.activities.NasUXBootMainActivity"

        /** NasUX:Boot app launcher activity name. This is an `activity-alias` for {@link #NASUX_BOOT_MAIN_ACTIVITY_NAME} used for launchers with {@link Intent#CATEGORY_LAUNCHER}. */
        public static final String NASUX_BOOT_LAUNCHER_ACTIVITY_NAME = NASUX_BOOT_PACKAGE_NAME + ".activities.NasUXBootLauncherActivity"; // Default: "com.nastech.nasux.boot.activities.NasUXBootLauncherActivity"

    }





    /**
     * NasUX:Float app constants.
     */
    public static final class NASUX_FLOAT_APP {

        /** NasUX:Float app core activity name. */
        public static final String NASUX_FLOAT_ACTIVITY_NAME = NASUX_FLOAT_PACKAGE_NAME + ".NasUXFloatActivity"; // Default: "com.nastech.nasux.window.NasUXFloatActivity"

        /** NasUX:Float app core service name. */
        public static final String NASUX_FLOAT_SERVICE_NAME = NASUX_FLOAT_PACKAGE_NAME + ".NasUXFloatService"; // Default: "com.nastech.nasux.window.NasUXFloatService"

        /**
         * NasUX:Float app core service.
         */
        public static final class NASUX_FLOAT_SERVICE {

            /** Intent action to stop NASUX_FLOAT_SERVICE. */
            public static final String ACTION_STOP_SERVICE = NASUX_FLOAT_PACKAGE_NAME + ".ACTION_STOP_SERVICE"; // Default: "com.nastech.nasux.float.ACTION_STOP_SERVICE"

            /** Intent action to show float window. */
            public static final String ACTION_SHOW = NASUX_FLOAT_PACKAGE_NAME + ".ACTION_SHOW"; // Default: "com.nastech.nasux.float.ACTION_SHOW"

            /** Intent action to hide float window. */
            public static final String ACTION_HIDE = NASUX_FLOAT_PACKAGE_NAME + ".ACTION_HIDE"; // Default: "com.nastech.nasux.float.ACTION_HIDE"

        }

    }





    /**
     * NasUX:Styling app constants.
     */
    public static final class NASUX_STYLING_APP {

        /** NasUX:Styling app core activity name. */
        public static final String NASUX_STYLING_ACTIVITY_NAME = NASUX_STYLING_PACKAGE_NAME + ".NasUXStyleActivity"; // Default: "com.nastech.nasux.styling.NasUXStyleActivity"


        /** NasUX:Styling app main activity name. */
        public static final String NASUX_STYLING_MAIN_ACTIVITY_NAME = NASUX_STYLING_PACKAGE_NAME + ".activities.NasUXStylingMainActivity"; // Default: "com.nastech.nasux.styling.activities.NasUXStylingMainActivity"

        /** NasUX:Styling app launcher activity name. This is an `activity-alias` for {@link #NASUX_STYLING_MAIN_ACTIVITY_NAME} used for launchers with {@link Intent#CATEGORY_LAUNCHER}. */
        public static final String NASUX_STYLING_LAUNCHER_ACTIVITY_NAME = NASUX_STYLING_PACKAGE_NAME + ".activities.NasUXStylingLauncherActivity"; // Default: "com.nastech.nasux.styling.activities.NasUXStylingLauncherActivity"

    }





    /**
     * NasUX:Tasker app constants.
     */
    public static final class NASUX_TASKER_APP {

        /** NasUX:Tasker app main activity name. */
        public static final String NASUX_TASKER_MAIN_ACTIVITY_NAME = NASUX_TASKER_PACKAGE_NAME + ".activities.NasUXTaskerMainActivity"; // Default: "com.nastech.nasux.tasker.activities.NasUXTaskerMainActivity"

        /** NasUX:Tasker app launcher activity name. This is an `activity-alias` for {@link #NASUX_TASKER_MAIN_ACTIVITY_NAME} used for launchers with {@link Intent#CATEGORY_LAUNCHER}. */
        public static final String NASUX_TASKER_LAUNCHER_ACTIVITY_NAME = NASUX_TASKER_PACKAGE_NAME + ".activities.NasUXTaskerLauncherActivity"; // Default: "com.nastech.nasux.tasker.activities.NasUXTaskerLauncherActivity"

    }





    /**
     * NasUX:Widget app constants.
     */
    public static final class NASUX_WIDGET_APP {

        /** NasUX:Widget app main activity name. */
        public static final String NASUX_WIDGET_MAIN_ACTIVITY_NAME = NASUX_WIDGET_PACKAGE_NAME + ".activities.NasUXWidgetMainActivity"; // Default: "com.nastech.nasux.widget.activities.NasUXWidgetMainActivity"

        /** NasUX:Widget app launcher activity name. This is an `activity-alias` for {@link #NASUX_WIDGET_MAIN_ACTIVITY_NAME} used for launchers with {@link Intent#CATEGORY_LAUNCHER}. */
        public static final String NASUX_WIDGET_LAUNCHER_ACTIVITY_NAME = NASUX_WIDGET_PACKAGE_NAME + ".activities.NasUXWidgetLauncherActivity"; // Default: "com.nastech.nasux.widget.activities.NasUXWidgetLauncherActivity"


        /**  Intent {@code String} extra for the token of the NasUX:Widget app shortcuts. */
        public static final String EXTRA_TOKEN_NAME = NASUX_PACKAGE_NAME + ".shortcut.token"; // Default: "com.nastech.nasux.shortcut.token"


        /**
         * NasUX:Widget app {@link android.appwidget.AppWidgetProvider} class.
         */
        public static final class NASUX_WIDGET_PROVIDER {

            /** Intent action for if an item is clicked in the widget. */
            public static final String ACTION_WIDGET_ITEM_CLICKED = NASUX_WIDGET_PACKAGE_NAME + ".ACTION_WIDGET_ITEM_CLICKED"; // Default: "com.nastech.nasux.widget.ACTION_WIDGET_ITEM_CLICKED"


            /** Intent action to refresh files in the widget. */
            public static final String ACTION_REFRESH_WIDGET = NASUX_WIDGET_PACKAGE_NAME + ".ACTION_REFRESH_WIDGET"; // Default: "com.nastech.nasux.widget.ACTION_REFRESH_WIDGET"


            /**  Intent {@code String} extra for the file clicked for the NASUX_WIDGET_PROVIDER.ACTION_WIDGET_ITEM_CLICKED intent. */
            public static final String EXTRA_FILE_CLICKED = NASUX_WIDGET_PACKAGE_NAME + ".EXTRA_FILE_CLICKED"; // Default: "com.nastech.nasux.widget.EXTRA_FILE_CLICKED"

        }

    }

}
