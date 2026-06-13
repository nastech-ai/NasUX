package com.nastech.nasux.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.nastech.nasux.R;
import com.nastech.nasux.shared.markdown.MarkdownUtils;

/**
 * NasUXMarkdownPreviewSheet — Dracula-themed bottom sheet that renders
 * pasted code or markdown text using Markwon with syntax highlighting.
 *
 * Long-press the NasTech AI FAB to open it.
 * Paste any terminal output / code block to see it beautifully formatted.
 */
public class NasUXMarkdownPreviewSheet extends BottomSheetDialogFragment {

    private static final String ARG_CONTENT   = "content";
    private static final String ARG_LANG      = "lang";
    private static final String TAG           = "NasUXMarkdownPreview";

    private String mCurrentLang = "auto";

    public static NasUXMarkdownPreviewSheet create(String initialContent) {
        NasUXMarkdownPreviewSheet sheet = new NasUXMarkdownPreviewSheet();
        Bundle args = new Bundle();
        args.putString(ARG_CONTENT, initialContent != null ? initialContent : "");
        sheet.setArguments(args);
        return sheet;
    }

    public static void show(FragmentManager fm, String initialContent) {
        create(initialContent).show(fm, TAG);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(BottomSheetDialogFragment.STYLE_NORMAL, R.style.NasUX.MarkdownSheet);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_markdown_preview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextInputEditText input        = view.findViewById(R.id.preview_input);
        TextView          output       = view.findViewById(R.id.preview_output);
        TextView          outputLabel  = view.findViewById(R.id.preview_output_label);
        TextView          empty        = view.findViewById(R.id.preview_empty);
        MaterialButton    renderBtn    = view.findViewById(R.id.preview_btn_render);
        MaterialButton    copyBtn      = view.findViewById(R.id.preview_btn_copy);
        MaterialButton    shareBtn     = view.findViewById(R.id.preview_btn_share);

        // Language chip listeners
        int[] chipIds = {
            R.id.chip_auto, R.id.chip_bash, R.id.chip_python,
            R.id.chip_js, R.id.chip_markdown
        };
        String[] chipLangs = { "auto", "bash", "python", "javascript", "markdown" };

        for (int i = 0; i < chipIds.length; i++) {
            final String lang = chipLangs[i];
            View chip = view.findViewById(chipIds[i]);
            if (chip != null) {
                chip.setOnClickListener(v -> mCurrentLang = lang);
            }
        }

        // Pre-fill with initial content if provided
        String initial = getArguments() != null ? getArguments().getString(ARG_CONTENT, "") : "";
        if (!initial.isEmpty()) {
            input.setText(initial);
        }

        // Render button
        renderBtn.setOnClickListener(v -> {
            String raw = input.getText() != null ? input.getText().toString().trim() : "";
            if (raw.isEmpty()) {
                Toast.makeText(requireContext(), "Paste some code or markdown first", Toast.LENGTH_SHORT).show();
                return;
            }
            String markdown = wrapInMarkdown(raw, mCurrentLang);
            CharSequence rendered = MarkdownUtils.getSpannedMarkdownText(requireContext(), markdown);
            if (rendered != null) {
                output.setText(rendered);
                output.setVisibility(View.VISIBLE);
                outputLabel.setVisibility(View.VISIBLE);
                empty.setVisibility(View.GONE);
            }
        });

        // Copy button — copies the input text
        copyBtn.setOnClickListener(v -> {
            String text = input.getText() != null ? input.getText().toString() : "";
            if (!text.isEmpty()) {
                ClipboardManager clipboard =
                    (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(ClipData.newPlainText("NasUX Code", text));
                    Toast.makeText(requireContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Share button
        shareBtn.setOnClickListener(v -> {
            String text = input.getText() != null ? input.getText().toString() : "";
            if (!text.isEmpty()) {
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT, text);
                startActivity(Intent.createChooser(share, "Share code"));
            }
        });

        // Expand to full height
        try {
            View parent = (View) view.getParent();
            BottomSheetBehavior<?> behavior = BottomSheetBehavior.from(parent);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        } catch (Exception ignored) {}
    }

    /**
     * Wraps raw text in a fenced code block if it doesn't already look like markdown.
     * If lang is "auto", tries to detect the language from keywords.
     */
    private String wrapInMarkdown(String raw, String lang) {
        // If it already has markdown fences, render as-is
        if (raw.contains("```")) return raw;

        // If it looks like markdown prose, render as-is
        if (raw.contains("# ") || raw.contains("**") || raw.contains("- ") || raw.contains("* ")) {
            return raw;
        }

        // Auto-detect language
        String detected = lang;
        if ("auto".equals(lang)) {
            detected = detectLanguage(raw);
        }

        return "```" + detected + "\n" + raw + "\n```";
    }

    private String detectLanguage(String code) {
        String lower = code.toLowerCase();
        if (lower.contains("#!/bin/bash") || lower.contains("#!/bin/sh") || lower.startsWith("$")) return "bash";
        if (lower.contains("def ") || lower.contains("import ") && lower.contains(":")) return "python";
        if (lower.contains("function ") || lower.contains("const ") || lower.contains("=> ")) return "javascript";
        if (lower.contains("public class") || lower.contains("void main")) return "java";
        if (lower.contains("SELECT ") || lower.contains("select ") || lower.contains("FROM ")) return "sql";
        if (lower.contains("FROM ") && lower.contains("RUN ")) return "dockerfile";
        if (lower.contains("{") && (lower.contains("\"name\"") || lower.contains("\"key\""))) return "json";
        return "bash"; // default for terminal output
    }
}
