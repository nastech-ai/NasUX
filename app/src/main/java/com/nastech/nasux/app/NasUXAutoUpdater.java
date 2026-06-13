package com.nastech.nasux.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * NasUX Auto-Updater — checks GitHub releases API for new NasUX APK versions.
 *
 * On every app launch (at most once per 24h), silently queries:
 *   https://api.github.com/repos/nastech-ai/NasUX/releases/latest
 *
 * If a newer version is available, shows a dialog offering to open the
 * GitHub release page so the user can download the new APK.
 *
 * Also drives in-terminal update via "bash ~/nastech-agent/update.sh".
 */
public class NasUXAutoUpdater {

    private static final String GITHUB_API_RELEASES =
        "https://api.github.com/repos/nastech-ai/NasUX/releases/latest";
    private static final String PREFS_NAME    = "nasux_updater";
    private static final String KEY_LAST_CHECK = "last_update_check_ms";
    private static final String KEY_SKIP_TAG   = "skip_version_tag";
    private static final long   CHECK_INTERVAL_MS = 24L * 60 * 60 * 1000; // 24h

    private final Context  mContext;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mMainHandler     = new Handler(Looper.getMainLooper());

    public NasUXAutoUpdater(Context context) {
        mContext = context.getApplicationContext();
    }

    /** Call from NasUXActivity.onCreate() — runs silently in background. */
    public void checkForUpdateAsync() {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastCheck = prefs.getLong(KEY_LAST_CHECK, 0L);
        if (System.currentTimeMillis() - lastCheck < CHECK_INTERVAL_MS) return;

        mExecutor.submit(() -> {
            try {
                ReleaseInfo release = fetchLatestRelease();
                if (release == null) return;

                prefs.edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply();

                String skipTag = prefs.getString(KEY_SKIP_TAG, "");
                if (release.tagName.equals(skipTag)) return;

                int currentCode = getCurrentVersionCode();
                if (release.versionCode > currentCode) {
                    NasUXThemeManager.logNasTechEvent(mContext, "update_available",
                        release.tagName);
                    mMainHandler.post(() -> showUpdateDialog(release));
                }
            } catch (Exception ignored) {
                // Silent — never crash on update check failure
            }
        });
    }

    // ─── Private helpers ────────────────────────────────────────────────────

    private ReleaseInfo fetchLatestRelease() throws Exception {
        URL url = new URL(GITHUB_API_RELEASES);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setRequestProperty("User-Agent", "NasUX-AutoUpdater/1.0");

        if (conn.getResponseCode() != 200) return null;

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        conn.disconnect();

        JSONObject json = new JSONObject(sb.toString());
        String tagName  = json.optString("tag_name", "");
        String name     = json.optString("name", tagName);
        String htmlUrl  = json.optString("html_url", "");
        String body     = json.optString("body", "");

        // Parse version code from tagName e.g. "v0.120.0" → 120
        int versionCode = parseVersionCode(tagName);

        // Find universal APK download URL from assets
        String apkUrl = htmlUrl; // fallback to release page
        JSONArray assets = json.optJSONArray("assets");
        if (assets != null) {
            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.getJSONObject(i);
                String assetName = asset.optString("name", "");
                if (assetName.contains("universal") && assetName.endsWith(".apk")) {
                    apkUrl = asset.optString("browser_download_url", htmlUrl);
                    break;
                }
            }
        }

        ReleaseInfo info = new ReleaseInfo();
        info.tagName     = tagName;
        info.name        = name;
        info.htmlUrl     = htmlUrl;
        info.apkUrl      = apkUrl;
        info.changelog   = body.length() > 500 ? body.substring(0, 500) + "…" : body;
        info.versionCode = versionCode;
        return info;
    }

    private int parseVersionCode(String tagName) {
        // "v0.120.0" → 120, "v1.2.3" → 10203 (major*10000 + minor*100 + patch)
        try {
            String v = tagName.replaceAll("[^0-9.]", "");
            String[] parts = v.split("\\.");
            if (parts.length >= 3) {
                return Integer.parseInt(parts[0]) * 10000
                     + Integer.parseInt(parts[1]) * 100
                     + Integer.parseInt(parts[2]);
            } else if (parts.length == 2) {
                return Integer.parseInt(parts[0]) * 10000
                     + Integer.parseInt(parts[1]) * 100;
            }
        } catch (NumberFormatException ignored) {}
        return 0;
    }

    private int getCurrentVersionCode() {
        try {
            return mContext.getPackageManager()
                .getPackageInfo(mContext.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    private void showUpdateDialog(ReleaseInfo release) {
        if (!(mContext instanceof android.app.Activity)) {
            // Context might not be an activity — skip dialog (background check)
            return;
        }
        android.app.Activity activity = (android.app.Activity) mContext;
        if (activity.isFinishing() || activity.isDestroyed()) return;

        new AlertDialog.Builder(activity)
            .setTitle("NasTech Update Available — " + release.tagName)
            .setMessage(
                "A new version of NasUX is ready!\n\n" +
                release.name + "\n\n" +
                "Changelog:\n" + release.changelog + "\n\n" +
                "Download and install to get the latest NasTech AI features.")
            .setPositiveButton("Download APK", (d, w) -> {
                NasUXThemeManager.logNasTechEvent(mContext, "update_download_tapped",
                    release.tagName);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(release.apkUrl));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
            })
            .setNeutralButton("View Release Notes", (d, w) -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(release.htmlUrl));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
            })
            .setNegativeButton("Skip This Version", (d, w) -> {
                mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit().putString(KEY_SKIP_TAG, release.tagName).apply();
                NasUXThemeManager.logNasTechEvent(mContext, "update_skipped", release.tagName);
            })
            .setCancelable(true)
            .show();
    }

    // ─── Data class ──────────────────────────────────────────────────────────

    private static class ReleaseInfo {
        String tagName    = "";
        String name       = "";
        String htmlUrl    = "";
        String apkUrl     = "";
        String changelog  = "";
        int    versionCode = 0;
    }
}
