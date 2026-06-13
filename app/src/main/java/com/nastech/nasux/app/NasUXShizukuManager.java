package com.nastech.nasux.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuRemoteProcess;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * NasUX Shizuku Manager — grants NasUX ADB-level elevated permissions
 * without requiring the device to be rooted.
 *
 * Shizuku lets NasUX run privileged shell commands directly through
 * a trusted service (either activated via ADB pairing or root).
 *
 * Capabilities unlocked:
 *  • WRITE_SECURE_SETTINGS — change system settings programmatically
 *  • INSTALL/DELETE_PACKAGES — silent APK install for auto-update
 *  • DUMP — read system service states
 *  • RUN_IN_BACKGROUND — bypass battery restrictions
 *  • Grant any Android permission to NasUX via `pm grant`
 *  • Run arbitrary privileged shell commands as the `shell` user
 */
public class NasUXShizukuManager {

    private static final int SHIZUKU_REQUEST_CODE = 2025;
    private static final String TAG = "NasUXShizuku";

    private final Context  mContext;
    private final Handler  mMainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    /** All privileged permissions NasUX requests via Shizuku */
    private static final String[] NASUX_ELEVATED_PERMISSIONS = {
        "android.permission.WRITE_SECURE_SETTINGS",
        "android.permission.DUMP",
        "android.permission.READ_LOGS",
        "android.permission.PACKAGE_USAGE_STATS",
        "android.permission.BATTERY_STATS",
        "android.permission.CHANGE_CONFIGURATION",
        "android.permission.REQUEST_INSTALL_PACKAGES",
    };

    public interface ShizukuResultCallback {
        void onResult(boolean success, String output);
    }

    public NasUXShizukuManager(Context context) {
        mContext = context.getApplicationContext();
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    /** @return true if Shizuku is installed and its binder is alive */
    public static boolean isShizukuAvailable() {
        try {
            return Shizuku.pingBinder();
        } catch (Throwable t) {
            return false;
        }
    }

    /** @return true if NasUX has been granted Shizuku permission */
    public static boolean hasShizukuPermission() {
        try {
            if (Shizuku.isPreV11()) {
                return false;
            }
            return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Request Shizuku permission — shows system dialog */
    public void requestShizukuPermission(Activity activity) {
        try {
            if (!isShizukuAvailable()) {
                showShizukuNotInstalledDialog(activity);
                return;
            }
            if (Shizuku.isPreV11()) {
                Toast.makeText(mContext,
                    "Shizuku version too old — please update Shizuku to v11+", Toast.LENGTH_LONG).show();
                return;
            }
            Shizuku.requestPermission(SHIZUKU_REQUEST_CODE);
            NasUXThemeManager.logNasTechEvent(mContext, "shizuku_permission_requested", "user");
        } catch (Throwable t) {
            Toast.makeText(mContext, "Shizuku error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Grant all NasUX elevated permissions via Shizuku.
     * Runs `pm grant com.nastech.nasux <permission>` for each.
     */
    public void grantAllNasUXPermissions(ShizukuResultCallback callback) {
        if (!isShizukuAvailable() || !hasShizukuPermission()) {
            mMainHandler.post(() -> callback.onResult(false,
                "Shizuku not available or permission not granted.\n" +
                "Install Shizuku from Play Store and activate it via ADB or root."));
            return;
        }

        mExecutor.submit(() -> {
            StringBuilder log = new StringBuilder();
            boolean allOk = true;
            String pkg = mContext.getPackageName();

            for (String perm : NASUX_ELEVATED_PERMISSIONS) {
                String[] cmd = {"pm", "grant", pkg, perm};
                try {
                    ShizukuRemoteProcess proc = Shizuku.newProcess(cmd, null, null);
                    proc.waitFor();
                    int code = proc.exitValue();
                    if (code == 0) {
                        log.append("✓ ").append(perm.replace("android.permission.", "")).append("\n");
                    } else {
                        log.append("⚠ ").append(perm.replace("android.permission.", ""))
                           .append(" (code ").append(code).append(")\n");
                    }
                } catch (Throwable t) {
                    log.append("✗ ").append(perm.replace("android.permission.", ""))
                       .append(": ").append(t.getMessage()).append("\n");
                    allOk = false;
                }
            }

            NasUXThemeManager.logNasTechEvent(mContext, "shizuku_permissions_granted",
                allOk ? "success" : "partial");
            final boolean success = allOk;
            final String output = log.toString();
            mMainHandler.post(() -> callback.onResult(success, output));
        });
    }

    /**
     * Run a privileged shell command via Shizuku as the `shell` user.
     * Output is returned to the callback on the main thread.
     */
    public void runPrivilegedCommand(String command, ShizukuResultCallback callback) {
        if (!isShizukuAvailable() || !hasShizukuPermission()) {
            if (callback != null)
                mMainHandler.post(() -> callback.onResult(false,
                    "Shizuku unavailable — cannot run: " + command));
            return;
        }

        mExecutor.submit(() -> {
            StringBuilder out = new StringBuilder();
            boolean success = false;
            try {
                String[] cmd = {"sh", "-c", command};
                ShizukuRemoteProcess proc = Shizuku.newProcess(cmd, null, null);

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(proc.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        out.append(line).append("\n");
                    }
                }
                proc.waitFor();
                success = proc.exitValue() == 0;

                NasUXThemeManager.logNasTechEvent(mContext, "shizuku_command_run", command);
            } catch (Throwable t) {
                out.append("Error: ").append(t.getMessage());
            }
            final boolean ok = success;
            final String result = out.toString();
            mMainHandler.post(() -> {
                if (callback != null) callback.onResult(ok, result);
            });
        });
    }

    /**
     * Show the full Shizuku status + permission dialog for the user.
     * Called from the sidebar "🔑 Shizuku Permissions" button.
     */
    public void showShizukuDialog(Activity activity) {
        boolean available  = isShizukuAvailable();
        boolean permitted  = hasShizukuPermission();

        String status = available
            ? (permitted ? "✅ Shizuku active — NasUX has elevated access"
                         : "⚠️ Shizuku found — permission not granted yet")
            : "❌ Shizuku not running";

        new AlertDialog.Builder(activity)
            .setTitle("🔑 NasUX Elevated Permissions")
            .setMessage(
                status + "\n\n" +
                "Shizuku gives NasUX ADB-level control without root:\n\n" +
                "  • Install/remove apps silently\n" +
                "  • Change system settings\n" +
                "  • Read system logs\n" +
                "  • Grant permissions automatically\n" +
                "  • Run privileged shell commands\n\n" +
                (available
                    ? (permitted
                        ? "Tap 'Grant All Permissions' to unlock everything."
                        : "Tap 'Allow NasUX' to grant permission.")
                    : "Activate Shizuku via:\n" +
                      "  ADB: adb shell sh /sdcard/Android/data/\n" +
                      "       moe.shizuku.privileged.api/files/start.sh\n" +
                      "  OR install Shizuku from the Play Store."))
            .setPositiveButton(permitted ? "Grant All Permissions" : "Allow NasUX", (d, w) -> {
                if (!available) {
                    openShizukuPlayStore(activity);
                } else if (!permitted) {
                    requestShizukuPermission(activity);
                } else {
                    grantAllNasUXPermissions((ok, output) ->
                        new AlertDialog.Builder(activity)
                            .setTitle(ok ? "✅ Permissions Granted" : "⚠️ Partial Grant")
                            .setMessage(output)
                            .setPositiveButton("OK", null)
                            .show());
                }
            })
            .setNeutralButton(available ? "Run Command" : "Get Shizuku", (d, w) -> {
                if (available && permitted) {
                    showRunCommandDialog(activity);
                } else {
                    openShizukuPlayStore(activity);
                }
            })
            .setNegativeButton("Close", null)
            .show();
    }

    // ─── Private helpers ────────────────────────────────────────────────────

    private void showShizukuNotInstalledDialog(Activity activity) {
        new AlertDialog.Builder(activity)
            .setTitle("Shizuku Not Running")
            .setMessage(
                "Shizuku is not running on this device.\n\n" +
                "Install Shizuku from the Play Store, then activate it:\n" +
                "  • On Android 11+: use Wireless ADB pairing\n" +
                "  • On rooted devices: use the root option in Shizuku\n\n" +
                "No root required for Wireless ADB activation!")
            .setPositiveButton("Get Shizuku", (d, w) -> openShizukuPlayStore(activity))
            .setNegativeButton("Close", null)
            .show();
    }

    private void showRunCommandDialog(Activity activity) {
        final android.widget.EditText input = new android.widget.EditText(activity);
        input.setHint("e.g. pm list packages | grep nastech");
        input.setSingleLine(true);
        new AlertDialog.Builder(activity)
            .setTitle("🔑 Run Privileged Command")
            .setView(input)
            .setPositiveButton("Run", (d, w) -> {
                String cmd = input.getText().toString().trim();
                if (!cmd.isEmpty()) {
                    runPrivilegedCommand(cmd, (ok, output) ->
                        new AlertDialog.Builder(activity)
                            .setTitle(ok ? "✅ Output" : "⚠️ Output")
                            .setMessage(output.isEmpty() ? "(no output)" : output)
                            .setPositiveButton("OK", null)
                            .show());
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void openShizukuPlayStore(Activity activity) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("market://details?id=moe.shizuku.privileged.api"));
            activity.startActivity(intent);
        } catch (Exception e) {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api"));
            activity.startActivity(intent);
        }
    }
}
