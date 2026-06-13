package com.nastech.nasux.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * NasUX Theme Settings — lets users fully customise the terminal background.
 *
 * Features:
 *  • 12 AMOLED preset color swatches
 *  • Custom hex color input
 *  • Upload any wallpaper from the device gallery
 *  • Live preview with applied background
 *  • Full 16-color ANSI highlight swatch display
 *  • All changes are logged to ~/nastech-agent/nasux-events.log (NasTech sees everything)
 */
public class NasUXThemeSettingsActivity extends Activity {

    private static final int WALLPAPER_PICK_REQUEST = 101;

    private View mPreviewTerminal;
    private EditText mHexInput;
    private TextView mWallpaperLabel;
    private View mSelectedSwatchIndicator;

    private int mSelectedColor;
    private String mWallpaperUri;
    private boolean mWallpaperMode;
    private int mSelectedSwatchIndex = -1;
    private View[] mColorSwatches;

    // ───────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().getDecorView().setBackgroundColor(0xFF000000);

        // Load current prefs
        mSelectedColor = NasUXThemeManager.getBackgroundColor(this);
        mWallpaperUri  = NasUXThemeManager.getWallpaperUri(this);
        mWallpaperMode = NasUXThemeManager.isWallpaperMode(this);

        // Build the entire UI programmatically — clean, efficient, no XML dependency
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFF000000);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF000000);
        scroll.addView(root);
        setContentView(scroll);

        buildHeader(root);
        buildSection(root, "TERMINAL BACKGROUND");
        buildColorGrid(root);
        buildHexInput(root);
        buildWallpaperPicker(root);
        buildSection(root, "PREVIEW");
        buildPreview(root);
        buildSection(root, "HIGHLIGHT COLORS  (NasTech AI — system)");
        buildAnsiSwatches(root);
        buildButtons(root);

        // Highlight the currently-selected preset swatch
        highlightCurrentColor();
    }

    // ───────────────────────────────────────────────────────────────────────────
    // UI builders
    // ───────────────────────────────────────────────────────────────────────────

    private void buildHeader(LinearLayout parent) {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setBackgroundColor(0xFF111111);
        header.setPadding(dp(14), dp(0), dp(14), dp(0));
        header.setMinimumHeight(dp(56));
        parent.addView(header);

        ImageButton back = new ImageButton(this);
        back.setImageResource(android.R.drawable.ic_media_previous);
        back.setBackground(null);
        back.setColorFilter(0xFF8B949E);
        back.setOnClickListener(v -> finish());
        header.addView(back, dp(40), dp(40));

        TextView title = new TextView(this);
        title.setText("Theme & Colors");
        title.setTextColor(0xFF00D4AA);
        title.setTextSize(17f);
        title.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        title.setPadding(dp(10), 0, 0, 0);
        header.addView(title, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        View divider = new View(this);
        divider.setBackgroundColor(0xFF21262D);
        parent.addView(divider, ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
    }

    private void buildSection(LinearLayout parent, String label) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(0xFF00D4AA);
        tv.setTextSize(9.5f);
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setLetterSpacing(0.18f);
        tv.setPadding(dp(16), dp(14), dp(16), dp(4));
        parent.addView(tv);
    }

    private void buildColorGrid(LinearLayout parent) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(12), dp(4), dp(12), dp(4));
        parent.addView(container);

        mColorSwatches = new View[NasUXThemeManager.PRESET_COLORS.length];
        int cols = 4;
        int rows = NasUXThemeManager.PRESET_COLORS.length / cols;

        for (int row = 0; row < rows; row++) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setPadding(0, dp(4), 0, dp(4));
            container.addView(rowLayout);

            for (int col = 0; col < cols; col++) {
                int idx = row * cols + col;
                int color = NasUXThemeManager.PRESET_COLORS[idx];
                String name = NasUXThemeManager.PRESET_NAMES[idx];

                LinearLayout cell = new LinearLayout(this);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                cellParams.setMargins(dp(3), 0, dp(3), 0);
                cell.setLayoutParams(cellParams);

                View swatch = new View(this);
                swatch.setTag(idx);
                mColorSwatches[idx] = swatch;

                GradientDrawable bg = new GradientDrawable();
                bg.setColor(color);
                bg.setCornerRadius(dp(8));
                bg.setStroke(dp(2), 0xFF21262D);
                swatch.setBackground(bg);

                int finalIdx = idx;
                int finalColor = color;
                swatch.setOnClickListener(v -> onSwatchSelected(finalIdx, finalColor));

                cell.addView(swatch, dp(56), dp(56));

                TextView label = new TextView(this);
                label.setText(name);
                label.setTextColor(0xFF8B949E);
                label.setTextSize(8.5f);
                label.setTypeface(Typeface.MONOSPACE);
                label.setGravity(Gravity.CENTER);
                label.setPadding(0, dp(3), 0, 0);
                cell.addView(label);

                rowLayout.addView(cell);
            }
        }
    }

    private void buildHexInput(LinearLayout parent) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(8), dp(16), dp(4));
        parent.addView(row);

        TextView hash = new TextView(this);
        hash.setText("#");
        hash.setTextColor(0xFF00D4AA);
        hash.setTextSize(16f);
        hash.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        hash.setPadding(dp(8), 0, dp(4), 0);
        GradientDrawable hashBg = new GradientDrawable();
        hashBg.setColor(0xFF161B22);
        hashBg.setCornerRadius(dp(6));
        hash.setBackground(hashBg);
        row.addView(hash);

        mHexInput = new EditText(this);
        mHexInput.setHint("000000");
        mHexInput.setHintTextColor(0xFF3D444D);
        mHexInput.setTextColor(0xFFE6EDF3);
        mHexInput.setTextSize(15f);
        mHexInput.setTypeface(Typeface.MONOSPACE);
        mHexInput.setBackgroundColor(0xFF161B22);
        mHexInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        mHexInput.setSingleLine(true);
        mHexInput.setMaxLines(1);
        mHexInput.setText(String.format("%06X", 0xFFFFFF & mSelectedColor));

        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        inputParams.setMargins(dp(4), 0, dp(8), 0);
        mHexInput.setLayoutParams(inputParams);

        mHexInput.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) {}
            public void afterTextChanged(Editable s) {
                String hex = s.toString().trim().replaceAll("[^0-9a-fA-F]", "");
                if (hex.length() == 6) {
                    try {
                        int color = 0xFF000000 | Color.parseColor("#" + hex);
                        mSelectedColor = color;
                        mWallpaperMode = false;
                        mSelectedSwatchIndex = -1;
                        clearSwatchSelections();
                        applyPreview();
                    } catch (Exception ignored) {}
                }
            }
        });

        row.addView(mHexInput);

        View colorPreviewDot = new View(this);
        colorPreviewDot.setId(View.generateViewId());
        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setColor(mSelectedColor);
        dotBg.setCornerRadius(dp(4));
        colorPreviewDot.setBackground(dotBg);
        mHexInput.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) {}
            public void afterTextChanged(Editable s) {
                String hex = s.toString().trim();
                if (hex.length() == 6) {
                    try {
                        int color = 0xFF000000 | Color.parseColor("#" + hex);
                        dotBg.setColor(color);
                        colorPreviewDot.invalidate();
                    } catch (Exception ignored) {}
                }
            }
        });
        row.addView(colorPreviewDot, dp(36), dp(36));
    }

    private void buildWallpaperPicker(LinearLayout parent) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(16), dp(8), dp(16), dp(8));
        parent.addView(row);

        View wallpaperBtn = buildPrimaryButton("📷  Upload My Wallpaper", false);
        wallpaperBtn.setOnClickListener(v -> pickWallpaper());
        row.addView(wallpaperBtn);

        mWallpaperLabel = new TextView(this);
        mWallpaperLabel.setTextColor(0xFF8B949E);
        mWallpaperLabel.setTextSize(10f);
        mWallpaperLabel.setTypeface(Typeface.MONOSPACE);
        mWallpaperLabel.setPadding(0, dp(4), 0, 0);
        mWallpaperLabel.setGravity(Gravity.CENTER);
        if (mWallpaperMode && mWallpaperUri != null) {
            mWallpaperLabel.setText("Wallpaper: " + truncateUri(mWallpaperUri));
        } else {
            mWallpaperLabel.setText("No wallpaper selected");
        }
        row.addView(mWallpaperLabel);
    }

    private void buildPreview(LinearLayout parent) {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setPadding(dp(16), dp(4), dp(16), dp(8));
        parent.addView(wrapper);

        mPreviewTerminal = new View(this) {
            // Custom draw — just a colored view with overlaid text
        };

        LinearLayout previewBox = new LinearLayout(this);
        previewBox.setOrientation(LinearLayout.VERTICAL);
        previewBox.setPadding(dp(14), dp(12), dp(14), dp(12));
        GradientDrawable previewBorder = new GradientDrawable();
        previewBorder.setColor(mSelectedColor);
        previewBorder.setStroke(dp(1), 0xFF21262D);
        previewBorder.setCornerRadius(dp(8));
        previewBox.setBackground(previewBorder);
        previewBox.setTag("preview_border");
        wrapper.addView(previewBox);
        mPreviewTerminal = previewBox;

        String[] previewLines = {
            "\u001B[32m$ \u001B[0mnastech",
            "\u001B[36m◈ NasTech AI Agent v1.0\u001B[0m",
            "\u001B[33mLoaded 26 tools\u001B[0m",
            "\u001B[35m>\u001B[0m How can I help you?",
            "\u001B[31m✗\u001B[0m \u001B[32m✓\u001B[0m \u001B[34m●\u001B[0m \u001B[35m●\u001B[0m \u001B[33m●\u001B[0m \u001B[36m●\u001B[0m",
        };
        int[] lineColors = {
            0xFF3FB950, 0xFF00D4AA, 0xFFFFD700, 0xFFFF55FF, 0xFFE6EDF3,
        };
        for (int i = 0; i < previewLines.length; i++) {
            TextView tv = new TextView(this);
            String text = previewLines[i].replaceAll("\u001B\\[[0-9;]*m", "");
            tv.setText(text);
            tv.setTextColor(lineColors[i]);
            tv.setTextSize(11f);
            tv.setTypeface(Typeface.MONOSPACE);
            tv.setPadding(0, dp(1), 0, dp(1));
            previewBox.addView(tv);
        }
    }

    private void buildAnsiSwatches(LinearLayout parent) {
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setPadding(dp(16), dp(4), dp(16), dp(8));
        parent.addView(outer);

        TextView note = new TextView(this);
        note.setText("These colors are managed by NasTech AI and applied to all terminal output:");
        note.setTextColor(0xFF8B949E);
        note.setTextSize(10f);
        note.setTypeface(Typeface.MONOSPACE);
        note.setPadding(0, 0, 0, dp(8));
        outer.addView(note);

        outer.addView(buildAnsiRow("Normal", NasUXThemeManager.ANSI_NORMAL));
        outer.addView(buildAnsiRow("Bright", NasUXThemeManager.ANSI_BRIGHT));
    }

    private LinearLayout buildAnsiRow(String label, int[] colors) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(3), 0, dp(3));

        TextView labelTv = new TextView(this);
        labelTv.setText(label + "  ");
        labelTv.setTextColor(0xFF484F58);
        labelTv.setTextSize(9f);
        labelTv.setTypeface(Typeface.MONOSPACE);
        labelTv.setMinWidth(dp(42));
        row.addView(labelTv);

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout swatchRow = new LinearLayout(this);
        swatchRow.setOrientation(LinearLayout.HORIZONTAL);

        for (int i = 0; i < colors.length; i++) {
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            cell.setPadding(dp(3), 0, dp(3), 0);

            View dot = new View(this);
            GradientDrawable dotBg = new GradientDrawable();
            dotBg.setShape(GradientDrawable.OVAL);
            dotBg.setColor(colors[i]);
            dot.setBackground(dotBg);
            cell.addView(dot, dp(22), dp(22));

            TextView name = new TextView(this);
            name.setText(NasUXThemeManager.ANSI_NAMES[i]);
            name.setTextColor(colors[i]);
            name.setTextSize(7f);
            name.setTypeface(Typeface.MONOSPACE);
            name.setGravity(Gravity.CENTER);
            cell.addView(name);

            swatchRow.addView(cell);
        }

        hsv.addView(swatchRow);
        LinearLayout.LayoutParams hsvParams = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(hsv, hsvParams);
        return row;
    }

    private void buildButtons(LinearLayout parent) {
        View divider = new View(this);
        divider.setBackgroundColor(0xFF21262D);
        parent.addView(divider, ViewGroup.LayoutParams.MATCH_PARENT, dp(1));

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(dp(16), dp(12), dp(16), dp(24));
        btnRow.setBackgroundColor(0xFF111111);
        parent.addView(btnRow);

        View resetBtn = buildPrimaryButton("Reset Defaults", false);
        resetBtn.setOnClickListener(v -> onResetClicked());
        LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, dp(48));
        resetParams.setMargins(0, 0, dp(12), 0);
        btnRow.addView(resetBtn, resetParams);

        View saveBtn = buildPrimaryButton("Save Theme  →", true);
        saveBtn.setOnClickListener(v -> onSaveClicked());
        btnRow.addView(saveBtn, new LinearLayout.LayoutParams(
            0, dp(48), 1f));
    }

    private View buildPrimaryButton(String label, boolean primary) {
        TextView btn = new TextView(this);
        btn.setText(label);
        btn.setTextSize(13f);
        btn.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(dp(16), dp(8), dp(16), dp(8));
        GradientDrawable bg = new GradientDrawable();
        if (primary) {
            bg.setColor(0xFF00D4AA);
            btn.setTextColor(0xFF000000);
        } else {
            bg.setColor(0xFF21262D);
            bg.setStroke(dp(1), 0xFF3D444D);
            btn.setTextColor(0xFF8B949E);
        }
        bg.setCornerRadius(dp(8));
        btn.setBackground(bg);
        btn.setClickable(true);
        btn.setFocusable(true);
        return btn;
    }

    // ───────────────────────────────────────────────────────────────────────────
    // Interactions
    // ───────────────────────────────────────────────────────────────────────────

    private void onSwatchSelected(int index, int color) {
        mSelectedSwatchIndex = index;
        mSelectedColor = color;
        mWallpaperMode = false;
        mWallpaperLabel.setText("No wallpaper selected");

        // Update hex input
        mHexInput.setText(String.format("%06X", 0xFFFFFF & color));

        clearSwatchSelections();
        highlightSwatch(index);
        applyPreview();
    }

    private void clearSwatchSelections() {
        for (int i = 0; i < mColorSwatches.length; i++) {
            GradientDrawable d = new GradientDrawable();
            d.setColor(NasUXThemeManager.PRESET_COLORS[i]);
            d.setCornerRadius(dp(8));
            d.setStroke(dp(2), 0xFF21262D);
            mColorSwatches[i].setBackground(d);
        }
    }

    private void highlightSwatch(int index) {
        if (index < 0 || index >= mColorSwatches.length) return;
        GradientDrawable d = new GradientDrawable();
        d.setColor(NasUXThemeManager.PRESET_COLORS[index]);
        d.setCornerRadius(dp(8));
        d.setStroke(dp(3), 0xFF00D4AA);
        mColorSwatches[index].setBackground(d);
    }

    private void highlightCurrentColor() {
        for (int i = 0; i < NasUXThemeManager.PRESET_COLORS.length; i++) {
            if (NasUXThemeManager.PRESET_COLORS[i] == mSelectedColor) {
                mSelectedSwatchIndex = i;
                highlightSwatch(i);
                return;
            }
        }
    }

    private void applyPreview() {
        if (mPreviewTerminal != null) {
            if (mWallpaperMode && mWallpaperUri != null) {
                NasUXThemeManager.applyBackground(this, mPreviewTerminal);
            } else {
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(mSelectedColor);
                bg.setStroke(dp(1), 0xFF21262D);
                bg.setCornerRadius(dp(8));
                mPreviewTerminal.setBackground(bg);
            }
        }
    }

    private void pickWallpaper() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
            | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(
            Intent.createChooser(intent, "Choose wallpaper"),
            WALLPAPER_PICK_REQUEST
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == WALLPAPER_PICK_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                try {
                    // Persist permission so we can read it later
                    getContentResolver().takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception ignored) {}
                mWallpaperUri = uri.toString();
                mWallpaperMode = true;
                mSelectedSwatchIndex = -1;
                clearSwatchSelections();
                mWallpaperLabel.setText("✓ " + truncateUri(mWallpaperUri));
                applyPreview();
            }
        }
    }

    private void onSaveClicked() {
        if (mWallpaperMode && mWallpaperUri != null) {
            NasUXThemeManager.setWallpaper(this, mWallpaperUri);
        } else {
            NasUXThemeManager.setBackgroundColor(this, mSelectedColor);
        }
        Toast.makeText(this, "Theme saved! Restart your session to apply.",
            Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private void onResetClicked() {
        NasUXThemeManager.resetToDefaults(this);
        mSelectedColor = 0xFF000000;
        mWallpaperMode = false;
        mWallpaperUri = null;
        mHexInput.setText("000000");
        mWallpaperLabel.setText("No wallpaper selected");
        clearSwatchSelections();
        highlightSwatch(0);
        applyPreview();
        Toast.makeText(this, "Theme reset to NasTech defaults.", Toast.LENGTH_SHORT).show();
    }

    // ───────────────────────────────────────────────────────────────────────────
    // Helpers
    // ───────────────────────────────────────────────────────────────────────────

    private int dp(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private String truncateUri(String uri) {
        if (uri == null) return "";
        int slash = uri.lastIndexOf('/');
        return slash >= 0 ? "…/" + uri.substring(slash + 1) : uri;
    }
}
