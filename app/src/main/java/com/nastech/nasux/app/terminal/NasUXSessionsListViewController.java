package com.nastech.nasux.app.terminal;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.nastech.nasux.R;
import com.nastech.nasux.app.NasUXActivity;
import com.nastech.nasux.shared.nasux.shell.command.runner.terminal.NasUXSession;
import com.nastech.nasux.terminal.TerminalSession;

import java.util.List;

public class NasUXSessionsListViewController extends ArrayAdapter<NasUXSession>
        implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    final NasUXActivity mActivity;

    final StyleSpan boldSpan   = new StyleSpan(Typeface.BOLD);
    final StyleSpan italicSpan = new StyleSpan(Typeface.ITALIC);

    public NasUXSessionsListViewController(NasUXActivity activity, List<NasUXSession> sessionList) {
        super(activity.getApplicationContext(), R.layout.item_terminal_sessions_list, sessionList);
        this.mActivity = activity;
    }

    @SuppressLint("SetTextI18n")
    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View rowView = convertView;
        if (rowView == null) {
            rowView = mActivity.getLayoutInflater().inflate(
                    R.layout.item_terminal_sessions_list, parent, false);
        }

        TextView sessionTitleView = rowView.findViewById(R.id.session_title);
        View     cardContainer    = rowView.findViewById(R.id.session_card_container);
        View     activeIndicator  = rowView.findViewById(R.id.session_active_indicator);

        NasUXSession item = getItem(position);
        if (item == null) {
            sessionTitleView.setText("null session");
            return rowView;
        }
        TerminalSession sessionAtRow = item.getTerminalSession();
        if (sessionAtRow == null) {
            sessionTitleView.setText("null session");
            return rowView;
        }

        TerminalSession currentSession = mActivity.getCurrentSession();
        boolean isActive = sessionAtRow == currentSession;

        if (cardContainer != null) {
            cardContainer.setBackground(ContextCompat.getDrawable(
                    mActivity,
                    isActive ? R.drawable.session_card_active : R.drawable.session_card_normal));
        }

        if (activeIndicator != null) {
            activeIndicator.setBackgroundColor(isActive
                    ? ContextCompat.getColor(mActivity, R.color.nastech_accent)
                    : Color.TRANSPARENT);
        }

        String name          = sessionAtRow.mSessionName;
        String sessionTitle  = sessionAtRow.getTitle();
        String numberPart    = "[" + (position + 1) + "] ";
        String namePart      = TextUtils.isEmpty(name) ? "" : name;
        String titlePart     = TextUtils.isEmpty(sessionTitle)
                ? "" : ((namePart.isEmpty() ? "" : "\n") + sessionTitle);
        String full          = numberPart + namePart + titlePart;

        SpannableString styled = new SpannableString(full);
        styled.setSpan(boldSpan, 0, numberPart.length() + namePart.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        styled.setSpan(italicSpan, numberPart.length() + namePart.length(), full.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sessionTitleView.setText(styled);

        boolean running = sessionAtRow.isRunning();
        if (running) {
            sessionTitleView.setPaintFlags(
                    sessionTitleView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            sessionTitleView.setPaintFlags(
                    sessionTitleView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }

        int textColor = (running || sessionAtRow.getExitStatus() == 0)
                ? ContextCompat.getColor(mActivity, R.color.session_text)
                : ContextCompat.getColor(mActivity, R.color.text_error);
        sessionTitleView.setTextColor(textColor);

        return rowView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        NasUXSession clicked = getItem(position);
        if (clicked != null) {
            mActivity.getNasUXTerminalSessionClient()
                    .setCurrentSession(clicked.getTerminalSession());
        }
        mActivity.getDrawer().closeDrawers();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        NasUXSession selected = getItem(position);
        if (selected != null) {
            mActivity.getNasUXTerminalSessionClient()
                    .renameSession(selected.getTerminalSession());
        }
        return true;
    }
}
