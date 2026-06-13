package com.nastech.nasux.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.view.View;

import com.nastech.nasux.shared.nasux.NasUXConstants;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * NasUXThemeManager — manages all user theme preferences for NasUX.
 *
 * Handles:
 *  • Background color (any solid color)
 *  • Custom wallpaper (any image from device gallery)
 *  • Updating ~/.nasux/colors.properties when background changes
 *  • Logging ALL user preference changes to ~/nastech-agent/nasux-events.log
 *    so NasTech AI always knows what the user has changed.
 */
public class NasUXThemeManager {

    private static final String PREFS_NAME = "nasux_theme";
    private static final String KEY_BG_TYPE     = "bg_type";       // "color" | "wallpaper"
    private static final String KEY_BG_COLOR    = "bg_color";      // int ARGB
    private static final String KEY_WALLPAPER   = "wallpaper_uri"; // content URI string
    private static final String KEY_OVERLAY_ALPHA = "overlay_alpha"; // 0..255

    /** 12 built-in AMOLED/dark preset background colors */
    public static final int[] PRESET_COLORS = {
        0xFF000000, // True AMOLED Black          ★ default
        0xFF0D1117, // GitHub Dark Navy
        0xFF0A0F1A, // NasTech Dark
        0xFF07111F, // Ocean Depth
        0xFF17000A, // Dark Crimson
        0xFF001508, // Deep Forest
        0xFF00071A, // Midnight Blue
        0xFF160018, // Deep Violet
        0xFF0E0A00, // Dark Amber
        0xFF00161A, // Deep Teal
        0xFF1A0E00, // Dark Copper
        0xFF100010, // Deep Magenta
    };

    public static final String[] PRESET_NAMES = {
        "Pure Black", "GitHub Dark", "NasTech Dark", "Ocean",
        "Crimson", "Forest", "Midnight", "Violet",
        "Amber", "Deep Teal", "Copper", "Magenta",
    };

    /** 8 normal ANSI highlight colors (NasTech super-vivid palette) */
    public static final int[] ANSI_NORMAL = {
        0xFF1C1C1C, // Black
        0xFFFF3333, // Red
        0xFF00D4AA, // Green (NasTech teal)
        0xFFFFD700, // Yellow
        0xFF3DC3FF, // Blue
        0xFFFF00FF, // Magenta
        0xFF00FFDD, // Cyan
        0xFFD0D8E0, // White
    };

    /** 8 bright ANSI highlight colors */
    public static final int[] ANSI_BRIGHT = {
        0xFF666666, // Bright Black
        0xFFFF6E6E, // Bright Red
        0xFF4DFFD4, // Bright Green
        0xFFFFFF4D, // Bright Yellow
        0xFF80DAFF, // Bright Blue
        0xFFFF80FF, // Bright Magenta
        0xFF80FFE8, // Bright Cyan
        0xFFFFFFFF, // Bright White
    };

    public static final String[] ANSI_NAMES = {
        "Black", "Red", "Green", "Yellow",
        "Blue", "Magenta", "Cyan", "White",
    };

    // ───────────────────────────────────────────────────────────────────────────
    // Getters
    // ───────────────────────────────────────────────────────────────────────────

    public static int getBackgroundColor(Context ctx) {
        return prefs(ctx).getInt(KEY_BG_COLOR, 0xFF000000);
    }

    public static String getWallpaperUri(Context ctx) {
        return prefs(ctx).getString(KEY_WALLPAPER, null);
    }

    public static boolean isWallpaperMode(Context ctx) {
        return "wallpaper".equals(prefs(ctx).getString(KEY_BG_TYPE, "color"));
    }

    public static int getOverlayAlpha(Context ctx) {
        return prefs(ctx).getInt(KEY_OVERLAY_ALPHA, 0);
    }

    // ───────────────────────────────────────────────────────────────────────────
    // Setters — each one logs to NasTech event log
    // ───────────────────────────────────────────────────────────────────────────

    public static void setBackgroundColor(Context ctx, int color) {
        prefs(ctx).edit()
            .putString(KEY_BG_TYPE, "color")
            .putInt(KEY_BG_COLOR, color)
            .apply();
        updateColorsProperties(ctx, color);
        logNasTechEvent(ctx, "theme_bg_color",
            String.format("#%06X", 0xFFFFFF & color));
    }

    public static void setWallpaper(Context ctx, String uri) {
        prefs(ctx).edit()
            .putString(KEY_BG_TYPE, "wallpaper")
            .putString(KEY_WALLPAPER, uri)
            .apply();
        logNasTechEvent(ctx, "theme_wallpaper", uri);
    }

    public static void setOverlayAlpha(Context ctx, int alpha) {
        prefs(ctx).edit().putInt(KEY_OVERLAY_ALPHA, alpha).apply();
        logNasTechEvent(ctx, "theme_overlay_alpha", String.valueOf(alpha));
    }

    public static void resetToDefaults(Context ctx) {
        prefs(ctx).edit()
            .putString(KEY_BG_TYPE, "color")
            .putInt(KEY_BG_COLOR, 0xFF000000)
            .remove(KEY_WALLPAPER)
            .putInt(KEY_OVERLAY_ALPHA, 0)
            .apply();
        updateColorsProperties(ctx, 0xFF000000);
        logNasTechEvent(ctx, "theme_reset", "defaults");
    }

    // ───────────────────────────────────────────────────────────────────────────
    // Apply theme to a View
    // ───────────────────────────────────────────────────────────────────────────

    /**
     * Applies the current theme to the given root view.
     * Called from NasUXActivity.onResume() every time the user returns to the app.
     */
    public static void applyBackground(Context ctx, View rootView) {
        if (isWallpaperMode(ctx)) {
            String uriStr = getWallpaperUri(ctx);
            if (uriStr != null) {
                try {
                    Uri uri = Uri.parse(uriStr);
                    InputStream is = ctx.getContentResolver().openInputStream(uri);
                    if (is != null) {
                        Bitmap bmp = BitmapFactory.decodeStream(is);
                        is.close();
                        if (bmp != null) {
                            BitmapDrawable drawable = new BitmapDrawable(
                                ctx.getResources(), bmp);
                            drawable.setAlpha(255 - getOverlayAlpha(ctx));
                            rootView.setBackground(drawable);
                            return;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        rootView.setBackgroundColor(getBackgroundColor(ctx));
    }

    // ───────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ───────────────────────────────────────────────────────────────────────────

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Updates the background entry in ~/.nasux/colors.properties so the terminal
     * picks it up on next session start.
     */
    static void updateColorsProperties(Context ctx, int color) {
        try {
            String homePath = NasUXConstants.NASUX_HOME_DIR_PATH;
            File configDir = new File(homePath, ".nasux");
            configDir.mkdirs();
            File colorsFile = new File(configDir, "colors.properties");

            StringBuilder sb = new StringBuilder();
            if (colorsFile.exists()) {
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.FileReader(colorsFile));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("background=")) {
                        sb.append(line).append('\n');
                    }
                }
                reader.close();
            }
            sb.append("background=").append(String.format("#%06X", 0xFFFFFF & color))
              .append('\n');
            FileWriter fw = new FileWriter(colorsFile);
            fw.write(sb.toString());
            fw.close();
        } catch (Exception ignored) {}
    }

    /**
     * Appends an event line to ~/nastech-agent/nasux-events.log.
     * NasTech AI monitors this file and knows everything the user changes.
     */
    public static void logNasTechEvent(Context ctx, String event, String value) {
        try {
            String logPath = NasUXConstants.NASUX_HOME_DIR_PATH
                + "/nastech-agent/nasux-events.log";
            File logFile = new File(logPath);
            if (logFile.getParentFile() != null) logFile.getParentFile().mkdirs();
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                .format(new Date());
            FileWriter fw = new FileWriter(logFile, true);
            fw.write("[" + timestamp + "] event=" + event
                + " value=" + value + "\n");
            fw.close();
        } catch (Exception ignored) {}
    }
}
