package com.nastech.nasux.app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;

/**
 * NasUX Voice Manager — hands-free voice control for the terminal and NasTech AI.
 *
 * Modes:
 *  • TERMINAL  — Transcribed speech is typed directly into the active terminal session
 *  • AI_CHAT   — Transcribed speech is sent to NasTech AI as a prompt
 *
 * Activation:
 *  • Tap the mic FAB to start/stop listening
 *  • Long-press the main NasTech FAB to toggle voice mode
 *
 * The mic FAB glows orange/red while listening, returns to teal when idle.
 */
public class NasUXVoiceManager {

    public static final int REQUEST_RECORD_AUDIO = 2026;

    public enum VoiceMode { TERMINAL, AI_CHAT }

    public interface VoiceResultCallback {
        /** Called on the main thread with the recognised text */
        void onVoiceResult(String text, VoiceMode mode);
    }

    private final Context  mContext;
    private final Handler  mMainHandler = new Handler(Looper.getMainLooper());
    private SpeechRecognizer mRecognizer;
    private VoiceResultCallback mCallback;
    private VoiceMode mMode = VoiceMode.AI_CHAT;
    private boolean mListening = false;
    private View mMicFab;

    // ─── Colours ─────────────────────────────────────────────────────────────
    private static final int COLOR_IDLE      = 0xFF00D4AA; // NasTech teal
    private static final int COLOR_LISTENING = 0xFFFF4444; // bright red while active

    public NasUXVoiceManager(Context context) {
        mContext = context;
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    /** Attach the mic FAB so the manager can update its colour */
    public void setMicFab(View micFab) {
        mMicFab = micFab;
    }

    public VoiceMode getMode()          { return mMode; }
    public boolean   isListening()      { return mListening; }

    /** Toggle between TERMINAL and AI_CHAT modes */
    public void toggleMode() {
        mMode = (mMode == VoiceMode.AI_CHAT) ? VoiceMode.TERMINAL : VoiceMode.AI_CHAT;
        String label = mMode == VoiceMode.AI_CHAT ? "AI Chat" : "Terminal";
        Toast.makeText(mContext, "Voice mode: " + label, Toast.LENGTH_SHORT).show();
        NasUXThemeManager.logNasTechEvent(mContext, "voice_mode_changed", label);
    }

    /** Start voice listening — shows permission dialog if not yet granted */
    public void startListening(Activity activity, VoiceResultCallback callback) {
        if (!hasMicPermission()) {
            ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(mContext)) {
            Toast.makeText(mContext,
                "Speech recognition not available on this device.", Toast.LENGTH_SHORT).show();
            return;
        }
        mCallback = callback;
        doStartListening();
    }

    /** Stop listening immediately */
    public void stopListening() {
        mListening = false;
        setMicColor(COLOR_IDLE);
        if (mRecognizer != null) {
            try { mRecognizer.stopListening(); } catch (Exception ignored) {}
        }
    }

    /** Toggle start/stop */
    public void toggleListening(Activity activity, VoiceResultCallback callback) {
        if (mListening) {
            stopListening();
        } else {
            startListening(activity, callback);
        }
    }

    /** Release all resources — call from onDestroy */
    public void destroy() {
        stopListening();
        if (mRecognizer != null) {
            mRecognizer.destroy();
            mRecognizer = null;
        }
    }

    public boolean hasMicPermission() {
        return ContextCompat.checkSelfPermission(mContext, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED;
    }

    // ─── Private ─────────────────────────────────────────────────────────────

    private void doStartListening() {
        if (mRecognizer != null) {
            mRecognizer.destroy();
        }
        mRecognizer = SpeechRecognizer.createSpeechRecognizer(mContext);
        mRecognizer.setRecognitionListener(new NasUXRecognitionListener());

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 200L);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
            mMode == VoiceMode.AI_CHAT ? "Speak to NasTech AI…" : "Speak your command…");

        mListening = true;
        setMicColor(COLOR_LISTENING);
        NasUXThemeManager.logNasTechEvent(mContext, "voice_listening_started",
            mMode.name().toLowerCase());

        mRecognizer.startListening(intent);
    }

    private void setMicColor(int color) {
        if (mMicFab == null) return;
        mMainHandler.post(() -> {
            if (mMicFab instanceof FloatingActionButton) {
                ((FloatingActionButton) mMicFab).setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(color));
            } else {
                mMicFab.setBackgroundColor(color);
            }
        });
    }

    // ─── RecognitionListener ─────────────────────────────────────────────────

    private class NasUXRecognitionListener implements RecognitionListener {

        @Override
        public void onResults(Bundle results) {
            mListening = false;
            setMicColor(COLOR_IDLE);

            ArrayList<String> matches = results.getStringArrayList(
                SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches == null || matches.isEmpty()) return;

            String best = matches.get(0).trim();
            if (best.isEmpty()) return;

            NasUXThemeManager.logNasTechEvent(mContext, "voice_result",
                mMode.name().toLowerCase());

            if (mCallback != null) {
                mMainHandler.post(() -> mCallback.onVoiceResult(best, mMode));
            }
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            ArrayList<String> partial = partialResults.getStringArrayList(
                SpeechRecognizer.RESULTS_RECOGNITION);
            if (partial != null && !partial.isEmpty()) {
                // Show partial result as a brief toast so user can see what's heard
                String text = partial.get(0);
                if (text.length() > 3) {
                    mMainHandler.post(() ->
                        Toast.makeText(mContext, "🎤 " + text + "…", Toast.LENGTH_SHORT).show());
                }
            }
        }

        @Override public void onReadyForSpeech(Bundle params) {
            mMainHandler.post(() ->
                Toast.makeText(mContext, "🎤 Listening…", Toast.LENGTH_SHORT).show());
        }

        @Override public void onError(int error) {
            mListening = false;
            setMicColor(COLOR_IDLE);
            String msg = voiceErrorMessage(error);
            NasUXThemeManager.logNasTechEvent(mContext, "voice_error", String.valueOf(error));
            mMainHandler.post(() ->
                Toast.makeText(mContext, "Voice: " + msg, Toast.LENGTH_SHORT).show());
        }

        @Override public void onBeginningOfSpeech()     {}
        @Override public void onRmsChanged(float rms)   {}
        @Override public void onBufferReceived(byte[] b){}
        @Override public void onEndOfSpeech()           {}
        @Override public void onEvent(int t, Bundle b)  {}
    }

    private static String voiceErrorMessage(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:            return "Audio error";
            case SpeechRecognizer.ERROR_CLIENT:           return "Client error";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: return "Mic permission needed";
            case SpeechRecognizer.ERROR_NETWORK:          return "Network error";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:  return "Network timeout";
            case SpeechRecognizer.ERROR_NO_MATCH:         return "No speech detected";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:  return "Recogniser busy";
            case SpeechRecognizer.ERROR_SERVER:           return "Server error";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:   return "No speech — timed out";
            default:                                       return "Unknown error " + error;
        }
    }
}
