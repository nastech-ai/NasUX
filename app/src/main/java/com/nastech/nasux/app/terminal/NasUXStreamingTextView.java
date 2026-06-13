package com.nastech.nasux.app.terminal;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.animation.LinearInterpolator;

import androidx.appcompat.widget.AppCompatTextView;

/**
 * NasUXStreamingTextView — animated character-by-character streaming output.
 *
 * Designed for NasTech AI responses: text streams in one character at a time
 * with a blinking cursor at the end. The teal NasTech accent color highlights
 * the most recently streamed text, then fades to normal.
 *
 * Usage:
 *   streamingView.startStreaming("NasTech AI response text here...");
 *   streamingView.stopStreaming();
 *   streamingView.appendChunk("delta text");  // for real streaming deltas
 */
public class NasUXStreamingTextView extends AppCompatTextView {

    private static final int CHAR_DELAY_MS   = 18;
    private static final int CURSOR_BLINK_MS = 530;
    private static final String CURSOR       = "▋";
    private static final int COLOR_ACCENT    = 0xFF00C8B8;
    private static final int COLOR_NORMAL    = 0xFFDDDDDD;
    private static final int COLOR_CURSOR    = 0xFF00C8B8;

    private final Handler  mHandler    = new Handler(Looper.getMainLooper());
    private final SpannableStringBuilder mBuffer = new SpannableStringBuilder();

    private String  mFullText       = "";
    private int     mCharIndex      = 0;
    private boolean mStreaming      = false;
    private boolean mCursorVisible  = true;

    private Runnable mStreamRunnable;
    private Runnable mCursorRunnable;

    public NasUXStreamingTextView(Context context) {
        super(context);
        init();
    }

    public NasUXStreamingTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NasUXStreamingTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setTypeface(Typeface.MONOSPACE);
        setTextColor(COLOR_NORMAL);
        setTextSize(12f);
        setLineSpacing(2f, 1f);
    }

    /**
     * Start streaming the given text character-by-character.
     * Cancels any ongoing stream first.
     */
    public void startStreaming(String text) {
        stopStreaming();
        mFullText  = text == null ? "" : text;
        mCharIndex = 0;
        mBuffer.clear();
        mStreaming = true;

        scheduleNextChar();
        startCursorBlink();
    }

    /**
     * Append a delta chunk to the current stream (for SSE / real-time streaming).
     * If not currently streaming, starts the stream.
     */
    public void appendChunk(String chunk) {
        if (chunk == null || chunk.isEmpty()) return;
        mFullText += chunk;
        if (!mStreaming) {
            mStreaming = true;
            scheduleNextChar();
            startCursorBlink();
        }
    }

    /**
     * Stop streaming and show the full text immediately.
     */
    public void stopStreaming() {
        mStreaming = false;
        mHandler.removeCallbacks(mStreamRunnable != null ? mStreamRunnable : () -> {});
        mHandler.removeCallbacks(mCursorRunnable != null ? mCursorRunnable : () -> {});

        if (!mFullText.isEmpty()) {
            setText(mFullText);
        }
    }

    /**
     * Reset and clear the view.
     */
    public void reset() {
        stopStreaming();
        mFullText  = "";
        mCharIndex = 0;
        mBuffer.clear();
        setText("");
    }

    private void scheduleNextChar() {
        mStreamRunnable = this::streamNextChar;
        mHandler.postDelayed(mStreamRunnable, CHAR_DELAY_MS);
    }

    private void streamNextChar() {
        if (!mStreaming) return;
        if (mCharIndex >= mFullText.length()) {
            mStreaming = false;
            mHandler.removeCallbacks(mCursorRunnable != null ? mCursorRunnable : () -> {});
            renderText(false);
            return;
        }

        mCharIndex++;
        renderText(true);
        scheduleNextChar();
    }

    private void renderText(boolean showCursor) {
        String visible = mFullText.substring(0, mCharIndex);
        SpannableStringBuilder ssb = new SpannableStringBuilder(visible);

        if (mCharIndex > 0) {
            int lastCharStart = Math.max(0, mCharIndex - 1);
            ssb.setSpan(
                new ForegroundColorSpan(COLOR_ACCENT),
                lastCharStart, mCharIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        if (showCursor && mCursorVisible) {
            int cursorStart = ssb.length();
            ssb.append(CURSOR);
            ssb.setSpan(
                new ForegroundColorSpan(COLOR_CURSOR),
                cursorStart, ssb.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        setText(ssb);
    }

    private void startCursorBlink() {
        mCursorRunnable = new Runnable() {
            @Override
            public void run() {
                if (!mStreaming && mCharIndex >= mFullText.length()) return;
                mCursorVisible = !mCursorVisible;
                renderText(mStreaming || mCharIndex < mFullText.length());
                mHandler.postDelayed(this, CURSOR_BLINK_MS);
            }
        };
        mHandler.postDelayed(mCursorRunnable, CURSOR_BLINK_MS);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopStreaming();
    }
}
