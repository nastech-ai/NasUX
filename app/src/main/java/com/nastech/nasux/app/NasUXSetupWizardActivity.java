package com.nastech.nasux.app;

import com.nastech.nasux.R;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

/**
 * NasTech AI Setup Wizard — shown on first run.
 * Guides the user through the 4-step NasTech AI setup:
 *   Step 1: Welcome
 *   Step 2: About NasTech AI
 *   Step 3: Install instructions
 *   Step 4: Done — Launch NasTech AI
 */
public class NasUXSetupWizardActivity extends AppCompatActivity {

    private int mCurrentStep = 1;
    private static final int TOTAL_STEPS = 4;

    private TextView mStepIndicator;
    private TextView mStepIcon;
    private TextView mStepTitle;
    private TextView mStepSubtitle;
    private TextView mStepBody;
    private TextView mLogOutput;
    private ScrollView mLogScroll;
    private View mProgressLayout;
    private Button mNextBtn;
    private Button mSkipBtn;
    private View[] mStepBars;

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        setContentView(R.layout.activity_nasux_setup_wizard);

        mStepIndicator = findViewById(R.id.wizard_step_indicator);
        mStepIcon = findViewById(R.id.wizard_step_icon);
        mStepTitle = findViewById(R.id.wizard_step_title);
        mStepSubtitle = findViewById(R.id.wizard_step_subtitle);
        mStepBody = findViewById(R.id.wizard_step_body);
        mLogOutput = findViewById(R.id.wizard_log_output);
        mLogScroll = findViewById(R.id.wizard_log_scroll);
        mProgressLayout = findViewById(R.id.wizard_progress_layout);
        mNextBtn = findViewById(R.id.wizard_next_btn);
        mSkipBtn = findViewById(R.id.wizard_skip_btn);
        mStepBars = new View[]{
            findViewById(R.id.step_bar_1),
            findViewById(R.id.step_bar_2),
            findViewById(R.id.step_bar_3),
            findViewById(R.id.step_bar_4)
        };

        mNextBtn.setOnClickListener(v -> onNextClicked());
        mSkipBtn.setOnClickListener(v -> finishSetup());

        showStep(1);
    }

    private void onNextClicked() {
        if (mCurrentStep < TOTAL_STEPS) {
            showStep(mCurrentStep + 1);
        } else {
            finishSetup();
        }
    }

    private void showStep(int step) {
        mCurrentStep = step;
        mStepIndicator.setText(step + " / " + TOTAL_STEPS);

        int accentColor = ContextCompat.getColor(this, R.color.nastech_accent);
        int dividerColor = ContextCompat.getColor(this, R.color.divider);
        for (int i = 0; i < mStepBars.length; i++) {
            mStepBars[i].setBackgroundColor(i < step ? accentColor : dividerColor);
        }

        mLogScroll.setVisibility(View.GONE);
        mProgressLayout.setVisibility(View.GONE);

        switch (step) {
            case 1:
                mStepIcon.setText("◈");
                mStepTitle.setText("Welcome to NasUX");
                mStepSubtitle.setText("Full Linux · Full AI · Any Android");
                mStepBody.setText(
                    "NasUX gives you a complete Linux terminal on your Android device, " +
                    "powered by the NasTech AI Agent.\n\n" +
                    "• Full Bash/Zsh terminal environment\n" +
                    "• NasTech AI Agent pre-installed\n" +
                    "• Python 3.11 + Node.js + 200+ packages\n" +
                    "• Auto-updates from GitHub\n" +
                    "• Type naturally to the AI — it runs commands for you\n\n" +
                    "This wizard will help you set everything up."
                );
                mNextBtn.setText("Let's go →");
                mSkipBtn.setVisibility(View.VISIBLE);
                break;

            case 2:
                mStepIcon.setText("⬡");
                mStepTitle.setText("NasTech AI Agent");
                mStepSubtitle.setText("The self-improving AI that runs anywhere");
                mStepBody.setText(
                    "NasTech AI is an advanced agent that:\n\n" +
                    "• Understands natural language commands\n" +
                    "• Runs terminal commands in the background\n" +
                    "• Browses the web, writes code, manages files\n" +
                    "• Improves itself from experience\n" +
                    "• Supports 26+ tools: browser, code execution,\n" +
                    "  file management, memory, scheduling, and more\n\n" +
                    "Supports Claude, GPT-4, Gemini, and OpenRouter models."
                );
                mNextBtn.setText("Next →");
                mSkipBtn.setVisibility(View.VISIBLE);
                break;

            case 3:
                mStepIcon.setText("⚙");
                mStepTitle.setText("Install Dependencies");
                mStepSubtitle.setText("Python 3.11 · Node.js · All packages");
                mStepBody.setText(
                    "Run these commands in your NasUX terminal to install everything:\n"
                );
                mLogScroll.setVisibility(View.VISIBLE);
                mLogOutput.setText(
                    "# Step 1: Install system packages\n" +
                    "pkg install python nodejs git curl wget openssh -y\n\n" +
                    "# Step 2: Install NasTech AI\n" +
                    "bash ~/nastech-agent/install.sh\n\n" +
                    "# Step 3: Configure your AI API key\n" +
                    "nano ~/nastech-agent/.env\n" +
                    "# Add: OPENAI_API_KEY=your-key-here\n" +
                    "# Or:   OPENROUTER_API_KEY=your-key-here\n\n" +
                    "# Step 4: Launch NasTech AI\n" +
                    "nastech\n\n" +
                    "# Auto-update anytime:\n" +
                    "bash ~/nastech-agent/update.sh\n"
                );
                mNextBtn.setText("Got it →");
                mSkipBtn.setVisibility(View.VISIBLE);
                break;

            case 4:
                mStepIcon.setText("✓");
                mStepTitle.setText("Ready to Launch!");
                mStepSubtitle.setText("NasUX + NasTech AI configured");
                mStepBody.setText(
                    "You're all set! Here's how to use NasTech AI:\n\n" +
                    "1. Open the left sidebar (swipe from left)\n" +
                    "2. Tap '▶ Start AI Agent' to launch\n" +
                    "3. Type any message or command to the AI\n\n" +
                    "Quick tips:\n" +
                    "• /help  — see all AI commands\n" +
                    "• Type naturally: 'install nginx and start it'\n" +
                    "• The AI runs commands in background for you\n\n" +
                    "Tap 'Launch NasUX' to open your terminal."
                );
                mNextBtn.setText("Launch NasUX →");
                mSkipBtn.setVisibility(View.GONE);
                break;

            default:
                break;
        }
    }

    private void finishSetup() {
        NasUXSplashActivity.markFirstRunComplete(this);
        Intent intent = new Intent(this, NasUXActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (mCurrentStep > 1) {
            showStep(mCurrentStep - 1);
        } else {
            finishSetup();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
    }
}
