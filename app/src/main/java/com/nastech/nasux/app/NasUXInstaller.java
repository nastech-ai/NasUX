package com.nastech.nasux.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.system.Os;
import android.util.Pair;
import android.view.WindowManager;

import com.nastech.nasux.R;
import com.nastech.nasux.shared.file.FileUtils;
import com.nastech.nasux.shared.nasux.crash.NasUXCrashUtils;
import com.nastech.nasux.shared.nasux.file.NasUXFileUtils;
import com.nastech.nasux.shared.interact.MessageDialogUtils;
import com.nastech.nasux.shared.logger.Logger;
import com.nastech.nasux.shared.markdown.MarkdownUtils;
import com.nastech.nasux.shared.errors.Error;
import com.nastech.nasux.shared.android.PackageUtils;
import com.nastech.nasux.shared.nasux.NasUXConstants;
import com.nastech.nasux.shared.nasux.NasUXUtils;
import com.nastech.nasux.shared.nasux.shell.command.environment.NasUXShellEnvironment;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.nastech.nasux.shared.nasux.NasUXConstants.NASUX_PREFIX_DIR;
import static com.nastech.nasux.shared.nasux.NasUXConstants.NASUX_PREFIX_DIR_PATH;
import static com.nastech.nasux.shared.nasux.NasUXConstants.NASUX_STAGING_PREFIX_DIR;
import static com.nastech.nasux.shared.nasux.NasUXConstants.NASUX_STAGING_PREFIX_DIR_PATH;

/**
 * Install the NasUX bootstrap packages if necessary by following the below steps:
 * <p/>
 * (1) If $PREFIX already exist, assume that it is correct and be done. Note that this relies on that we do not create a
 * broken $PREFIX directory below.
 * <p/>
 * (2) A progress dialog is shown with "Installing..." message and a spinner.
 * <p/>
 * (3) A staging directory, $STAGING_PREFIX, is cleared if left over from broken installation below.
 * <p/>
 * (4) The zip file is loaded from a shared library.
 * <p/>
 * (5) The zip, containing entries relative to the $PREFIX, is is downloaded and extracted by a zip input stream
 * continuously encountering zip file entries:
 * <p/>
 * (5.1) If the zip entry encountered is SYMLINKS.txt, go through it and remember all symlinks to setup.
 * <p/>
 * (5.2) For every other zip entry, extract it into $STAGING_PREFIX and set execute permissions if necessary.
 */
final class NasUXInstaller {

    private static final String LOG_TAG = "NasUXInstaller";

    /** Performs bootstrap setup if necessary. */
    static void setupBootstrapIfNeeded(final Activity activity, final Runnable whenDone) {
        String bootstrapErrorMessage;
        Error filesDirectoryAccessibleError;

        // This will also call Context.getFilesDir(), which should ensure that nasux files directory
        // is created if it does not already exist
        filesDirectoryAccessibleError = NasUXFileUtils.isNasUXFilesDirectoryAccessible(activity, true, true);
        boolean isFilesDirectoryAccessible = filesDirectoryAccessibleError == null;

        // NasUX can only be run as the primary user (device owner) since only that
        // account has the expected file system paths. Verify that:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !PackageUtils.isCurrentUserThePrimaryUser(activity)) {
            bootstrapErrorMessage = activity.getString(R.string.bootstrap_error_not_primary_user_message,
                MarkdownUtils.getMarkdownCodeForString(NASUX_PREFIX_DIR_PATH, false));
            Logger.logError(LOG_TAG, "isFilesDirectoryAccessible: " + isFilesDirectoryAccessible);
            Logger.logError(LOG_TAG, bootstrapErrorMessage);
            sendBootstrapCrashReportNotification(activity, bootstrapErrorMessage);
            MessageDialogUtils.exitAppWithErrorMessage(activity,
                activity.getString(R.string.bootstrap_error_title),
                bootstrapErrorMessage);
            return;
        }

        if (!isFilesDirectoryAccessible) {
            bootstrapErrorMessage = Error.getMinimalErrorString(filesDirectoryAccessibleError);
            //noinspection SdCardPath
            if (PackageUtils.isAppInstalledOnExternalStorage(activity) &&
                !NasUXConstants.NASUX_FILES_DIR_PATH.equals(activity.getFilesDir().getAbsolutePath().replaceAll("^/data/user/0/", "/data/data/"))) {
                bootstrapErrorMessage += "\n\n" + activity.getString(R.string.bootstrap_error_installed_on_portable_sd,
                    MarkdownUtils.getMarkdownCodeForString(NASUX_PREFIX_DIR_PATH, false));
            }

            Logger.logError(LOG_TAG, bootstrapErrorMessage);
            sendBootstrapCrashReportNotification(activity, bootstrapErrorMessage);
            MessageDialogUtils.showMessage(activity,
                activity.getString(R.string.bootstrap_error_title),
                bootstrapErrorMessage, null);
            return;
        }

        // If prefix directory exists, even if its a symlink to a valid directory and symlink is not broken/dangling
        if (FileUtils.directoryFileExists(NASUX_PREFIX_DIR_PATH, true)) {
            if (NasUXFileUtils.isNasUXPrefixDirectoryEmpty()) {
                Logger.logInfo(LOG_TAG, "The nasux prefix directory \"" + NASUX_PREFIX_DIR_PATH + "\" exists but is empty or only contains specific unimportant files.");
            } else {
                whenDone.run();
                return;
            }
        } else if (FileUtils.fileExists(NASUX_PREFIX_DIR_PATH, false)) {
            Logger.logInfo(LOG_TAG, "The nasux prefix directory \"" + NASUX_PREFIX_DIR_PATH + "\" does not exist but another file exists at its destination.");
        }

        final ProgressDialog progress = ProgressDialog.show(activity, null, activity.getString(R.string.bootstrap_installer_body), true, false);
        new Thread() {
            @Override
            public void run() {
                try {
                    Logger.logInfo(LOG_TAG, "Installing " + NasUXConstants.NASUX_APP_NAME + " bootstrap packages.");

                    Error error;

                    // Delete prefix staging directory or any file at its destination
                    error = FileUtils.deleteFile("nasux prefix staging directory", NASUX_STAGING_PREFIX_DIR_PATH, true);
                    if (error != null) {
                        showBootstrapErrorDialog(activity, whenDone, Error.getErrorMarkdownString(error));
                        return;
                    }

                    // Delete prefix directory or any file at its destination
                    error = FileUtils.deleteFile("nasux prefix directory", NASUX_PREFIX_DIR_PATH, true);
                    if (error != null) {
                        showBootstrapErrorDialog(activity, whenDone, Error.getErrorMarkdownString(error));
                        return;
                    }

                    // Create prefix staging directory if it does not already exist and set required permissions
                    error = NasUXFileUtils.isNasUXPrefixStagingDirectoryAccessible(true, true);
                    if (error != null) {
                        showBootstrapErrorDialog(activity, whenDone, Error.getErrorMarkdownString(error));
                        return;
                    }

                    // Create prefix directory if it does not already exist and set required permissions
                    error = NasUXFileUtils.isNasUXPrefixDirectoryAccessible(true, true);
                    if (error != null) {
                        showBootstrapErrorDialog(activity, whenDone, Error.getErrorMarkdownString(error));
                        return;
                    }

                    Logger.logInfo(LOG_TAG, "Extracting bootstrap zip to prefix staging directory \"" + NASUX_STAGING_PREFIX_DIR_PATH + "\".");

                    final byte[] buffer = new byte[8096];
                    final List<Pair<String, String>> symlinks = new ArrayList<>(50);

                    final byte[] zipBytes = loadZipBytes();
                    try (ZipInputStream zipInput = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
                        ZipEntry zipEntry;
                        while ((zipEntry = zipInput.getNextEntry()) != null) {
                            if (zipEntry.getName().equals("SYMLINKS.txt")) {
                                BufferedReader symlinksReader = new BufferedReader(new InputStreamReader(zipInput));
                                String line;
                                while ((line = symlinksReader.readLine()) != null) {
                                    if (line.trim().isEmpty()) continue;
                                    String[] parts = line.split("←");
                                    if (parts.length != 2) {
                                        Logger.logWarn(LOG_TAG, "Skipping malformed symlink line: " + line);
                                        continue;
                                    }
                                    // Rewrite any absolute paths in SYMLINKS.txt to the NasUX package prefix.
                                    // Bootstrap ZIPs patched by scripts/patch-bootstrap.py already contain
                                    // NasUX paths, but this guard handles any future unpatched ZIPs.
                                    String oldPath = parts[0]
                                        .replace("/data/data/com.nastech.nasux/files/usr/share/nasux-keyring/",
                                                 NASUX_PREFIX_DIR_PATH + "/share/nasux-keyring/")
                                        .replace("/data/data/com.nastech.nasux/files/", NASUX_PREFIX_DIR_PATH + "/../");
                                    String newPath = NASUX_STAGING_PREFIX_DIR_PATH + "/" + parts[1];
                                    symlinks.add(Pair.create(oldPath, newPath));

                                    error = ensureDirectoryExists(new File(newPath).getParentFile());
                                    if (error != null) {
                                        showBootstrapErrorDialog(activity, whenDone, Error.getErrorMarkdownString(error));
                                        return;
                                    }
                                }
                            } else {
                                String zipEntryName = zipEntry.getName();
                                File targetFile = new File(NASUX_STAGING_PREFIX_DIR_PATH, zipEntryName);
                                boolean isDirectory = zipEntry.isDirectory();

                                error = ensureDirectoryExists(isDirectory ? targetFile : targetFile.getParentFile());
                                if (error != null) {
                                    showBootstrapErrorDialog(activity, whenDone, Error.getErrorMarkdownString(error));
                                    return;
                                }

                                if (!isDirectory) {
                                    try (FileOutputStream outStream = new FileOutputStream(targetFile)) {
                                        int readBytes;
                                        while ((readBytes = zipInput.read(buffer)) != -1)
                                            outStream.write(buffer, 0, readBytes);
                                    }
                                    if (zipEntryName.startsWith("bin/") || zipEntryName.startsWith("usr/bin/") ||
                                        zipEntryName.startsWith("libexec/") || zipEntryName.startsWith("usr/libexec/") ||
                                        zipEntryName.startsWith("lib/apt/apt-helper") || zipEntryName.startsWith("lib/apt/methods") ||
                                        zipEntryName.startsWith("usr/lib/apt/apt-helper") || zipEntryName.startsWith("usr/lib/apt/methods") ||
                                        zipEntryName.startsWith("lib/termux-exec") || zipEntryName.startsWith("lib/nasux-exec")) {
                                        //noinspection OctalInteger
                                        Os.chmod(targetFile.getAbsolutePath(), 0755);
                                    }
                                }
                            }
                        }
                    }

                    if (symlinks.isEmpty()) {
                        Logger.logWarn(LOG_TAG, "No SYMLINKS.txt encountered in bootstrap ZIP — skipping symlink creation.");
                    } else {
                        for (Pair<String, String> symlink : symlinks) {
                            try {
                                Os.symlink(symlink.first, symlink.second);
                            } catch (Exception symlinkError) {
                                Logger.logWarn(LOG_TAG, "Skipping symlink " + symlink.second + " → " + symlink.first + ": " + symlinkError.getMessage());
                            }
                        }
                    }

                    Logger.logInfo(LOG_TAG, "Moving nasux prefix staging to prefix directory.");

                    if (!NASUX_STAGING_PREFIX_DIR.renameTo(NASUX_PREFIX_DIR)) {
                        throw new RuntimeException("Moving nasux prefix staging to prefix directory failed");
                    }

                    Logger.logInfo(LOG_TAG, "Bootstrap packages installed successfully.");

                    // Post-rename: ensure all binaries under bin/ have execute permission.
                    // This covers cases where chmod was skipped during extraction (e.g. login, sh, bash).
                    String[] execDirs = { "/bin", "/usr/bin", "/libexec", "/usr/libexec" };
                    for (String execDir : execDirs) {
                        File dir = new File(NASUX_PREFIX_DIR_PATH + execDir);
                        if (!dir.isDirectory()) continue;
                        File[] files = dir.listFiles();
                        if (files == null) continue;
                        for (File f : files) {
                            if (f.isFile()) {
                                try { Os.chmod(f.getAbsolutePath(), 0755); } catch (Exception ignored) {}
                            }
                        }
                    }

                    // Recreate env file since nasux prefix was wiped earlier
                    NasUXShellEnvironment.writeEnvironmentToFile(activity);

                    // Write the correct APT sources so pkg install fetches from the right repo
                    fixPackageSources(activity);

                    // Permanently install NasTech Agent into the NasUX home directory
                    installNasTechAgent(activity);

                    // Install Kali Linux integration scripts (CLI only, no VNC)
                    setupKaliEnvironment(activity);

                    activity.runOnUiThread(whenDone);

                } catch (final Exception e) {
                    showBootstrapErrorDialog(activity, whenDone, Logger.getStackTracesMarkdownString(null, Logger.getStackTracesStringArray(e)));

                } finally {
                    activity.runOnUiThread(() -> {
                        try {
                            progress.dismiss();
                        } catch (RuntimeException e) {
                            // Activity already dismissed - ignore.
                        }
                    });
                }
            }
        }.start();
    }

    public static void showBootstrapErrorDialog(Activity activity, Runnable whenDone, String message) {
        Logger.logErrorExtended(LOG_TAG, "Bootstrap Error:\n" + message);

        // Send a notification with the exception so that the user knows why bootstrap setup failed
        sendBootstrapCrashReportNotification(activity, message);

        activity.runOnUiThread(() -> {
            try {
                new AlertDialog.Builder(activity).setTitle(R.string.bootstrap_error_title).setMessage(R.string.bootstrap_error_body)
                    .setNegativeButton(R.string.bootstrap_error_abort, (dialog, which) -> {
                        dialog.dismiss();
                        activity.finish();
                    })
                    .setPositiveButton(R.string.bootstrap_error_try_again, (dialog, which) -> {
                        dialog.dismiss();
                        FileUtils.deleteFile("nasux prefix directory", NASUX_PREFIX_DIR_PATH, true);
                        NasUXInstaller.setupBootstrapIfNeeded(activity, whenDone);
                    }).show();
            } catch (WindowManager.BadTokenException e1) {
                // Activity already dismissed - ignore.
            }
        });
    }

    private static void sendBootstrapCrashReportNotification(Activity activity, String message) {
        final String title = NasUXConstants.NASUX_APP_NAME + " Bootstrap Error";

        // Add info of all install NasUX plugin apps as well since their target sdk or installation
        // on external/portable sd card can affect NasUX app files directory access or exec.
        NasUXCrashUtils.sendCrashReportNotification(activity, LOG_TAG,
            title, null, "## " + title + "\n\n" + message + "\n\n" +
                NasUXUtils.getNasUXDebugMarkdownString(activity),
            true, false, NasUXUtils.AppInfoMode.NASUX_AND_PLUGIN_PACKAGES, true);
    }

    static void setupStorageSymlinks(final Context context) {
        final String LOG_TAG = "nasux-storage";
        final String title = NasUXConstants.NASUX_APP_NAME + " Setup Storage Error";

        Logger.logInfo(LOG_TAG, "Setting up storage symlinks.");

        new Thread() {
            public void run() {
                try {
                    Error error;
                    File storageDir = NasUXConstants.NASUX_STORAGE_HOME_DIR;

                    error = FileUtils.clearDirectory("~/storage", storageDir.getAbsolutePath());
                    if (error != null) {
                        Logger.logErrorAndShowToast(context, LOG_TAG, error.getMessage());
                        Logger.logErrorExtended(LOG_TAG, "Setup Storage Error\n" + error.toString());
                        NasUXCrashUtils.sendCrashReportNotification(context, LOG_TAG, title, null,
                            "## " + title + "\n\n" + Error.getErrorMarkdownString(error),
                            true, false, NasUXUtils.AppInfoMode.NASUX_PACKAGE, true);
                        return;
                    }

                    Logger.logInfo(LOG_TAG, "Setting up storage symlinks at ~/storage/shared, ~/storage/downloads, ~/storage/dcim, ~/storage/pictures, ~/storage/music and ~/storage/movies for directories in \"" + Environment.getExternalStorageDirectory().getAbsolutePath() + "\".");

                    // Get primary storage root "/storage/emulated/0" symlink
                    File sharedDir = Environment.getExternalStorageDirectory();
                    Os.symlink(sharedDir.getAbsolutePath(), new File(storageDir, "shared").getAbsolutePath());

                    File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                    Os.symlink(documentsDir.getAbsolutePath(), new File(storageDir, "documents").getAbsolutePath());

                    File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    Os.symlink(downloadsDir.getAbsolutePath(), new File(storageDir, "downloads").getAbsolutePath());

                    File dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
                    Os.symlink(dcimDir.getAbsolutePath(), new File(storageDir, "dcim").getAbsolutePath());

                    File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                    Os.symlink(picturesDir.getAbsolutePath(), new File(storageDir, "pictures").getAbsolutePath());

                    File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
                    Os.symlink(musicDir.getAbsolutePath(), new File(storageDir, "music").getAbsolutePath());

                    File moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
                    Os.symlink(moviesDir.getAbsolutePath(), new File(storageDir, "movies").getAbsolutePath());

                    File podcastsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS);
                    Os.symlink(podcastsDir.getAbsolutePath(), new File(storageDir, "podcasts").getAbsolutePath());

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        File audiobooksDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_AUDIOBOOKS);
                        Os.symlink(audiobooksDir.getAbsolutePath(), new File(storageDir, "audiobooks").getAbsolutePath());
                    }

                    // Dir 0 should ideally be for primary storage
                    // https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:frameworks/base/core/java/android/app/ContextImpl.java;l=818
                    // https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:frameworks/base/core/java/android/os/Environment.java;l=219
                    // https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:frameworks/base/core/java/android/os/Environment.java;l=181
                    // https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:frameworks/base/services/core/java/com/android/server/StorageManagerService.java;l=3796
                    // https://cs.android.com/android/platform/superproject/+/android-7.0.0_r36:frameworks/base/services/core/java/com/android/server/MountService.java;l=3053

                    // Create "Android/data/com.nastech.nasux" symlinks
                    File[] dirs = context.getExternalFilesDirs(null);
                    if (dirs != null && dirs.length > 0) {
                        for (int i = 0; i < dirs.length; i++) {
                            File dir = dirs[i];
                            if (dir == null) continue;
                            String symlinkName = "external-" + i;
                            Logger.logInfo(LOG_TAG, "Setting up storage symlinks at ~/storage/" + symlinkName + " for \"" + dir.getAbsolutePath() + "\".");
                            Os.symlink(dir.getAbsolutePath(), new File(storageDir, symlinkName).getAbsolutePath());
                        }
                    }

                    // Create "Android/media/com.nastech.nasux" symlinks
                    dirs = context.getExternalMediaDirs();
                    if (dirs != null && dirs.length > 0) {
                        for (int i = 0; i < dirs.length; i++) {
                            File dir = dirs[i];
                            if (dir == null) continue;
                            String symlinkName = "media-" + i;
                            Logger.logInfo(LOG_TAG, "Setting up storage symlinks at ~/storage/" + symlinkName + " for \"" + dir.getAbsolutePath() + "\".");
                            Os.symlink(dir.getAbsolutePath(), new File(storageDir, symlinkName).getAbsolutePath());
                        }
                    }

                    Logger.logInfo(LOG_TAG, "Storage symlinks created successfully.");
                } catch (Exception e) {
                    Logger.logErrorAndShowToast(context, LOG_TAG, e.getMessage());
                    Logger.logStackTraceWithMessage(LOG_TAG, "Setup Storage Error: Error setting up link", e);
                    NasUXCrashUtils.sendCrashReportNotification(context, LOG_TAG, title, null,
                        "## " + title + "\n\n" + Logger.getStackTracesMarkdownString(null, Logger.getStackTracesStringArray(e)),
                        true, false, NasUXUtils.AppInfoMode.NASUX_PACKAGE, true);
                }
            }
        }.start();
    }

    private static Error ensureDirectoryExists(File directory) {
        return FileUtils.createDirectoryFile(directory.getAbsolutePath());
    }

    /**
     * Writes APT sources pointing to the official Kali Linux rolling repository.
     * This replaces any Termux/NasUX CDN sources so that `apt install` inside Kali
     * always fetches real Kali packages from kali.org.
     *
     * Also writes the Kali sources into the Kali rootfs (kali-fs) if it already exists.
     */
    private static void fixPackageSources(final Context context) {
        try {
            // Bootstrap prefix sources.list — point to Kali rolling for the outer layer
            File sourcesFile = new File(NASUX_PREFIX_DIR_PATH + "/etc/apt/sources.list");
            if (!sourcesFile.getParentFile().exists()) sourcesFile.getParentFile().mkdirs();

            String sourcesContent =
                "# Kali Linux Rolling — Official Repository\n" +
                "# NasUX — Powered by NasTech AI\n" +
                "deb http://http.kali.org/kali kali-rolling main non-free contrib\n";

            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(sourcesFile)) {
                fos.write(sourcesContent.getBytes("UTF-8"));
            }
            sourcesFile.setReadable(true, false);
            Logger.logInfo(LOG_TAG, "Kali APT sources.list written: " + sourcesFile.getAbsolutePath());

            // Also write into the Kali rootfs if it already exists
            String kaliFsPath = NasUXConstants.NASUX_HOME_DIR_PATH + "/kali-fs/etc/apt";
            File kaliFsAptDir = new File(kaliFsPath);
            if (kaliFsAptDir.exists()) {
                File kaliSourcesFile = new File(kaliFsPath + "/sources.list");
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(kaliSourcesFile)) {
                    fos.write(sourcesContent.getBytes("UTF-8"));
                }
                Logger.logInfo(LOG_TAG, "Kali rootfs APT sources updated: " + kaliSourcesFile.getAbsolutePath());
            }

            // sources.list.d directory
            File sourcesDir = new File(NASUX_PREFIX_DIR_PATH + "/etc/apt/sources.list.d");
            if (!sourcesDir.exists()) sourcesDir.mkdirs();

        } catch (Exception e) {
            // Non-fatal
            Logger.logStackTraceWithMessage(LOG_TAG, "fixPackageSources: could not write sources.list", e);
        }
    }

    /**
     * Copies the Kali Linux integration scripts from app assets into the NasUX home directory
     * so they are ready to use from first launch.
     *
     * Scripts installed:
     *   ~/nastech-agent/kali-setup.sh         — downloads + installs Kali rootfs
     *   ~/nastech-agent/kali-login.sh         — proot login (becomes `kali` command)
     *   ~/nastech-agent/kali-nastech-setup.sh — installs NasTech AI inside Kali via apt
     *   ~/nastech-agent/kali-fonts.sh         — installs Source Code Pro + all coding fonts
     */
    static void setupKaliEnvironment(final Context context) {
        try {
            String agentDestDir = NasUXConstants.NASUX_HOME_DIR_PATH + "/nastech-agent";
            File destDir = new File(agentDestDir);
            if (!destDir.exists()) destDir.mkdirs();

            String[] kaliScripts = {
                "kali-setup.sh",
                "kali-login.sh",
                "kali-nastech-setup.sh",
                "kali-fonts.sh",
                "nastech-env-setup.sh"
            };

            for (String script : kaliScripts) {
                File destFile = new File(agentDestDir, script);
                try (java.io.InputStream in = context.getAssets().open(script);
                     java.io.FileOutputStream out = new java.io.FileOutputStream(destFile)) {
                    byte[] buf = new byte[4096];
                    int len;
                    while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                    destFile.setExecutable(true, false);
                    Logger.logInfo(LOG_TAG, "Kali script installed: " + destFile.getAbsolutePath());
                } catch (Exception e) {
                    Logger.logWarn(LOG_TAG, "Could not install Kali script " + script + ": " + e.getMessage());
                }
            }

            Logger.logInfo(LOG_TAG, "Kali environment scripts installed successfully.");
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "setupKaliEnvironment: failed", e);
        }
    }

    /**
     * Permanently copies the bundled NasTech Agent from app assets into the NasUX home directory
     * so it is available as a pre-installed AI assistant from first launch.
     */
    private static void installNasTechAgent(final Context context) {
        try {
            // Ensure the NasUX home directory itself exists before placing anything inside it
            File homeDir = new File(NasUXConstants.NASUX_HOME_DIR_PATH);
            if (!homeDir.exists()) homeDir.mkdirs();

            String agentDestDir = NasUXConstants.NASUX_HOME_DIR_PATH + "/nastech-agent";
            File destDir = new File(agentDestDir);
            File startScript = new File(agentDestDir, "start.sh");

            // Only skip if the directory exists AND start.sh is a non-empty file —
            // this prevents the "empty-dir" trap where a failed previous install left
            // an empty nastech-agent/ directory causing us to silently skip reinstall.
            if (destDir.exists() && startScript.exists() && startScript.length() > 0) {
                Logger.logInfo(LOG_TAG, "NasTech Agent already installed at " + agentDestDir);
                return;
            }

            if (destDir.exists()) {
                Logger.logInfo(LOG_TAG, "NasTech Agent dir exists but start.sh missing — reinstalling.");
            }
            destDir.mkdirs();

            String[] agentAssets = context.getAssets().list("nastech-agent");
            if (agentAssets == null || agentAssets.length == 0) {
                Logger.logInfo(LOG_TAG, "No NasTech Agent assets found to install.");
                return;
            }

            copyAssetDir(context, "nastech-agent", agentDestDir);

            // Copy NasUX convenience scripts into the agent dir
            String[] extraScripts = {"nasux-setup-all.sh", "nasux-update.sh", "nastech-wrappers.sh"};
            String[] scriptDests = {"setup-all.sh", "update.sh", "nastech-wrappers.sh"};
            for (int i = 0; i < extraScripts.length; i++) {
                try (java.io.InputStream in = context.getAssets().open(extraScripts[i]);
                     java.io.FileOutputStream out = new java.io.FileOutputStream(
                             new File(agentDestDir, scriptDests[i]))) {
                    byte[] buf = new byte[4096]; int len;
                    while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                    new File(agentDestDir, scriptDests[i]).setExecutable(true, false);
                } catch (Exception ignored) {}
            }

            // Copy default color scheme to ~/.nasux/colors.properties
            try {
                File nasuxConfigDir = new File(NasUXConstants.NASUX_HOME_DIR_PATH, ".nasux");
                nasuxConfigDir.mkdirs();
                File colorsFile = new File(nasuxConfigDir, "colors.properties");
                if (!colorsFile.exists()) {
                    try (java.io.InputStream in = context.getAssets().open("nasux-colors.properties");
                         java.io.FileOutputStream out = new java.io.FileOutputStream(colorsFile)) {
                        byte[] buf = new byte[4096]; int len;
                        while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                    }
                }
            } catch (Exception ignored) {}

            // Make ALL scripts in the entire agent tree executable
            makeAllScriptsExecutable(new File(agentDestDir));

            Logger.logInfo(LOG_TAG, "NasTech Agent installed successfully at " + agentDestDir);
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to install NasTech Agent", e);
        }
    }

    /**
     * Recursively walks {@code dir} and sets the executable bit on every file that:
     * <ul>
     *   <li>ends with {@code .sh}</li>
     *   <li>ends with {@code .py} and starts with a {@code #!} shebang line</li>
     *   <li>has no extension and is named {@code nastech}, {@code nastech-*}, or {@code kali}</li>
     * </ul>
     * Safe to call on a large tree — skips directories and binary blobs.
     */
    private static void makeAllScriptsExecutable(File dir) {
        if (dir == null || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                makeAllScriptsExecutable(f);
                continue;
            }
            String name = f.getName();
            boolean isScript = name.endsWith(".sh")
                    || name.equals("nastech")
                    || name.equals("kali")
                    || name.startsWith("nastech-")
                    || name.startsWith("kali-");
            if (!isScript && name.endsWith(".py")) {
                // Only chmod .py files that have a shebang (#!/...)
                try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(f, "r")) {
                    byte[] buf = new byte[2];
                    if (raf.read(buf) == 2 && buf[0] == '#' && buf[1] == '!') {
                        isScript = true;
                    }
                } catch (Exception ignored) {}
            }
            if (isScript) {
                f.setExecutable(true, false);
            }
        }
    }

    private static void copyAssetDir(Context context, String assetPath, String destPath) throws java.io.IOException {
        String[] assets = context.getAssets().list(assetPath);
        if (assets != null && assets.length > 0) {
            new File(destPath).mkdirs();
            for (String asset : assets) {
                copyAssetDir(context, assetPath + "/" + asset, destPath + "/" + asset);
            }
        } else {
            try (java.io.InputStream in = context.getAssets().open(assetPath);
                 java.io.FileOutputStream out = new java.io.FileOutputStream(destPath)) {
                byte[] buf = new byte[4096];
                int len;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            }
        }
    }

    public static byte[] loadZipBytes() {
        // Only load the shared library when necessary to save memory usage.
        System.loadLibrary("nasux-bootstrap");
        return getZip();
    }

    public static native byte[] getZip();

}
