package com.nastech.nasux.app.terminal;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.appcompat.widget.AppCompatTextView;

/**
 * NasUXStreamingTextView — smooth word-by-word AI streaming output.
 *
 * Streams text word-by-word (not char-by-char) so output is always readable.
 * Cursor is a ● that bounces using a smooth ValueAnimator pulse.
 *
 * Usage:
 *   streamingView.startStreaming("NasTech AI response text…");
 *   streamingView.appendChunk("more text");   // for real SSE deltas
 *   streamingView.stopStreaming();             // show full text immediately
 */
public class NasUXStreamingTextView extends AppCompatTextView {

    private static final int   WORD_DELAY_MS  = 55;
    private static final int   LINE_DELAY_MS  = 90;
    private static final String CURSOR        = "●";
    private static final int   COLOR_ACCENT   = 0xFF00C8B8;
    private static final int   COLOR_NORMAL   = 0xFFDDDDDD;
    private static final int   COLOR_CURSOR   = 0xFF00C8B8;

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private String  mFullText    = "";
    private int     mWordIndex   = 0;
    private boolean mStreaming   = false;

    private ValueAnimator mCursorAnimator;
    private float         mCursorScale = 1.0f;
    private boolean       mCursorOn    = true;

    private Runnable mStreamRunnable;

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
        setTextSize(12.5f);
        setLineSpacing(3f, 1f);
        setIncludeFontPadding(false);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Stream text word-by-word with bouncing ● cursor. */
    public void startStreaming(String text) {
        stopStreaming();
        mFullText  = text == null ? "" : text;
        mWordIndex = 0;
        mStreaming = true;
        startCursorPulse();
        scheduleNextWord();
    }

    /** Append a delta chunk (SSE / real-time). Starts stream if not running. */
    public void appendChunk(String chunk) {
        if (chunk == null || chunk.isEmpty()) return;
        mFullText += chunk;
        if (!mStreaming) {
            mStreaming = true;
            startCursorPulse();
            scheduleNextWord();
        }
    }

    /** Stop animation and show complete text immediately. */
    public void stopStreaming() {
        mStreaming = false;
        cancelCursorPulse();
        if (mStreamRunnable != null) {
            mHandler.removeCallbacks(mStreamRunnable);
            mStreamRunnable = null;
        }
        if (!mFullText.isEmpty()) {
            setText(mFullText);
        }
    }

    /** Clear everything. */
    public void reset() {
        stopStreaming();
        mFullText  = "";
        mWordIndex = 0;
        setText("");
    }

    // ── Streaming logic ───────────────────────────────────────────────────────

    private void scheduleNextWord() {
        // Find next word boundary: advance to after the next whitespace run
        int start = mWordIndex;
        if (start >= mFullText.length()) {
            finishStreaming();
            return;
        }

        // Scan forward to end of next "word" (non-whitespace token)
        int i = start;
        // Skip leading whitespace if any (shouldn't happen normally, but safe)
        while (i < mFullText.length() && mFullText.charAt(i) == ' ') i++;
        // Advance to next space or newline
        while (i < mFullText.length()
                && mFullText.charAt(i) != ' '
                && mFullText.charAt(i) != '\n') {
            i++;
        }
        // Include the trailing whitespace character
        if (i < mFullText.length()) i++;

        final int nextIndex = Math.min(i, mFullText.length());
        boolean isNewline = nextIndex > 0 && nextIndex <= mFullText.length()
                && mFullText.charAt(nextIndex - 1) == '\n';

        int delay = isNewline ? LINE_DELAY_MS : WORD_DELAY_MS;

        mStreamRunnable = () -> {
            if (!mStreaming) return;
            mWordIndex = nextIndex;
            renderText(true);

            if (mWordIndex >= mFullText.length()) {
                finishStreaming();
            } else {
                scheduleNextWord();
            }
        };
        mHandler.postDelayed(mStreamRunnable, delay);
    }

    private void finishStreaming() {
        mStreaming = false;
        cancelCursorPulse();
        renderText(false);
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    private void renderText(boolean showCursor) {
        String visible = mFullText.substring(0, mWordIndex);
        SpannableStringBuilder ssb = new SpannableStringBuilder(visible);

        // Teal accent on the last word that just appeared
        if (mWordIndex > 0) {
            int wordStart = findLastWordStart(visible);
            ssb.setSpan(
                new ForegroundColorSpan(COLOR_ACCENT),
                wordStart, visible.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        if (showCursor && mCursorOn) {
            int cursorStart = ssb.length();
            ssb.append(" ").append(CURSOR);

            // Bouncing size: base px scaled by mCursorScale
            int basePx  = (int) getTextSize();
            int pulsePx = Math.max(8, (int)(basePx * mCursorScale));

            ssb.setSpan(
                new ForegroundColorSpan(COLOR_CURSOR),
                cursorStart, ssb.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            ssb.setSpan(
                new AbsoluteSizeSpan(pulsePx),
                cursorStart + 1, ssb.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        setText(ssb);
    }

    /** Find the start index of the last "word" in the string. */
    private int findLastWordStart(String s) {
        if (s.isEmpty()) return 0;
        int end = s.length() - 1;
        // skip trailing whitespace
        while (end > 0 && Character.isWhitespace(s.charAt(end))) end--;
        // walk back to start of the word
        int start = end;
        while (start > 0 && !Character.isWhitespace(s.charAt(start - 1))) start--;
        return start;
    }

    // ── Cursor pulse animation ─────────────────────────────────────────────────

    private void startCursorPulse() {
        cancelCursorPulse();
        mCursorOn = true;

        mCursorAnimator = ValueAnimator.ofFloat(0.75f, 1.45f);
        mCursorAnimator.setDuration(700);
        mCursorAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mCursorAnimator.setRepeatMode(ValueAnimator.REVERSE);
        mCursorAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mCursorAnimator.addUpdateListener(anim -> {
            mCursorScale = (float) anim.getAnimatedValue();
            // Flash off at the bottom of the bounce for a "blink-bounce" effect
            mCursorOn = (mCursorScale > 0.85f);
            if (mWordIndex > 0 || mStreaming) {
                renderText(mStreaming || mWordIndex < mFullText.length());
            }
        });
        mCursorAnimator.start();
    }

    private void cancelCursorPulse() {
        if (mCursorAnimator != null) {
            mCursorAnimator.cancel();
            mCursorAnimator = null;
        }
        mCursorOn    = false;
        mCursorScale = 1.0f;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopStreaming();
    }
}
