package com.nastech.nasux.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

/**
 * NasUX Splash Screen — shown on every launch.
 * On first run, presents the NasTech AI setup wizard button.
 * On subsequent runs, auto-proceeds to NasUXActivity after a brief animated display.
 */
public class NasUXSplashActivity extends Activity {

    private static final String PREFS_NAME = "nasux_prefs";
    private static final String KEY_FIRST_RUN = "first_run_complete";
    private static final int NORMAL_DELAY_MS = 1800;

    private TextView mStatusText;
    private View mProgressBar;
    private Button mSetupBtn;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );

        setContentView(R.layout.activity_nasux_splash);

        mStatusText = findViewById(R.id.splash_status);
        mProgressBar = findViewById(R.id.splash_progress);
        mSetupBtn = findViewById(R.id.splash_setup_btn);

        animateIn();

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean firstRunComplete = prefs.getBoolean(KEY_FIRST_RUN, false);

        if (!firstRunComplete) {
            showFirstRunFlow();
        } else {
            showNormalLaunch();
        }
    }

    private void animateIn() {
        View root = findViewById(android.R.id.content);
        root.setAlpha(0f);
        root.animate()
            .alpha(1f)
            .setDuration(600)
            .start();
    }

    private void showFirstRunFlow() {
        mStatusText.setText("First run detected — NasTech AI ready to install");

        mHandler.postDelayed(() -> {
            mStatusText.setText("Tap below to set up NasTech AI →");
            mProgressBar.setVisibility(View.GONE);
            mSetupBtn.setVisibility(View.VISIBLE);
            mSetupBtn.animate().alpha(1f).setDuration(400).start();
            mSetupBtn.setOnClickListener(v -> launchSetupWizard());
        }, 1200);
    }

    private void showNormalLaunch() {
        String[] statusMessages = {
            "Loading NasUX…",
            "Starting NasTech environment…",
            "Ready"
        };

        mHandler.postDelayed(() -> mStatusText.setText(statusMessages[1]), 500);
        mHandler.postDelayed(() -> mStatusText.setText(statusMessages[2]), 1100);
        mHandler.postDelayed(this::launchMain, NORMAL_DELAY_MS);
    }

    private void launchSetupWizard() {
        Intent intent = new Intent(this, NasUXSetupWizardActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void launchMain() {
        Intent intent = new Intent(this, NasUXActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    /** Called by NasUXSetupWizardActivity when setup is complete to mark first-run done. */
    public static void markFirstRunComplete(android.content.Context context) {
        context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_FIRST_RUN, true)
            .apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
    }
}
